package dev.sanda.apifi.service.graphql_subcriptions.pubsub.redis_pubsub;

import dev.sanda.apifi.utils.ConfigValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
@AllArgsConstructor(onConstructor_ = @Autowired)
@Conditional(RedisPubSubUrlEnvVarSetCondition.class)
public class DefaultRedisConfig {

  private final ConfigValues configValues;

  @Bean
  public LettuceConnectionFactory lettuceConnectionFactory() {
    validateURL(configValues.getRedisPubSubUrl());
    return new LettuceConnectionFactory(parseURLProperties());
  }

  @Bean
  public RedisMessageListenerContainer redisContainer() {
    val container = new RedisMessageListenerContainer();
    container.setConnectionFactory(lettuceConnectionFactory());
    return container;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    val template = new RedisTemplate<String, Object>();
    template.setConnectionFactory(lettuceConnectionFactory());
    return template;
  }

  private void validateURL(String url) {
    if (
      !url.matches("redis[^:]*:[/][/]([^:]*:[^:@]+@)?[^:]+:[0-9]{4}([/][0-9])?")
    ) throw new RuntimeException(
      "The redis url: \"" +
      url +
      "\" is invalid. The redis url should conform to something along the lines of: \"redis://username:password@ipaddress:port_number/db_index\""
    );
  }

  private RedisStandaloneConfiguration parseURLProperties() {
    val url = configValues.getRedisPubSubUrl();
    val username = url.contains("@")
      ? url.replaceFirst("redis[^:]*:[/][/]", "").replaceFirst(":.+", "")
      : "";

    val password = url.contains("@")
      ? url
        .replaceFirst("redis[^:]*:[/][/]", "")
        .replaceFirst(":", "")
        .replaceFirst("@.+", "")
      : "";

    val hostName = url
      .replaceFirst("redis[^:]*:[/][/]([^:]*:[^:@]+@)?", "")
      .replaceFirst(":[0-9]{4}([/][0-9])?", "");

    val port = Integer.parseInt(
      url
        .replaceFirst("redis[^:]*:[/][/]([^:]*:[^:@]+@)?[^:]+:", "")
        .replaceFirst("[/][0-9]", "")
    );

    val dbIndexStr = url
      .replaceFirst("redis[^:]*:[/][/]([^:]*:[^:@]+@)?[^:]+:[0-9]{4}", "")
      .replaceFirst("[/]", "");

    val dbIndex = dbIndexStr.matches("[0-9]")
      ? Integer.parseInt(dbIndexStr)
      : null;

    val _username = !username.equals("") ? username : null;
    val _password = !password.equals("") ? password : null;

    val redisStandAloneConfig = new RedisStandaloneConfiguration();
    redisStandAloneConfig.setHostName(hostName);
    redisStandAloneConfig.setPort(port);
    if (_username != null) redisStandAloneConfig.setUsername(_username);
    if (_password != null) redisStandAloneConfig.setPassword(_password);
    if (dbIndex != null) redisStandAloneConfig.setDatabase(dbIndex);

    return redisStandAloneConfig;
  }
}
