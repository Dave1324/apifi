package dev.sanda.apifi.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import dev.sanda.apifi.ApifiStaticUtils;
import dev.sanda.apifi.annotations.ApiHooks;
import dev.sanda.apifi.service.ApiHooksAndCustomResolvers;
import dev.sanda.apifi.service.EmbeddedCollectionApiHooks;
import dev.sanda.datafi.reflection.ReflectionCache;
import dev.sanda.datafi.service.DataManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static dev.sanda.apifi.ApifiStaticUtils.apiHooksName;
import static dev.sanda.datafi.DatafiStaticUtils.toPascalCase;

/**
 * Helper class for the definition of Javapoet generated class fields
 */

@RequiredArgsConstructor
public class FieldSpecs {

    @NonNull
    private ProcessingEnvironment processingEnv;
    @NonNull
    private EntitiesInfoCache entitiesInfoCache;

    public FieldSpec reflectionCache() {
        return FieldSpec.builder(ReflectionCache.class, ApifiStaticUtils.reflectionCache, Modifier.PRIVATE)
                .addAnnotation(Autowired.class)
                .build();
    }

    public FieldSpec dataManager(TypeElement entity){
        return FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(DataManager.class), ClassName.get(entity)),
                ApifiStaticUtils.camelcaseNameOf(entity) + "DataManager")
                .addAnnotation(Autowired.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    public FieldSpec dataManager(TypeMirror entityType, String prefix){
        return FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(DataManager.class), ClassName.get(entityType)),
                prefix + toPascalCase(DataManager.class.getSimpleName()))
                .addAnnotation(Autowired.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    public FieldSpec apiHooks(TypeElement entity) {
        TypeElement apiHooks = entitiesInfoCache.getEntitiesApiHooksMap().get(entity.getSimpleName().toString());
        return FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(apiHooks), ClassName.get(entity)), apiHooksName(entity))
                .addAnnotation(Autowired.class)
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    public FieldSpec embeddedCollectionApiHooks(VariableElement field) {
        ParameterizedTypeName apiHooksType = null;
        ApiHooks apiHooks = field.getAnnotation(ApiHooks.class);
        if(apiHooks != null){
            try{
                apiHooks.value();
            }catch (MirroredTypeException mte){
                Types TypeUtils = this.processingEnv.getTypeUtils();
                apiHooksType = ParameterizedTypeName.get(ClassName.get((TypeElement)TypeUtils.asElement(mte.getTypeMirror())));
            }
        }else {
            TypeElement typeElement =
                    entitiesInfoCache
                            .getTypeElementMap()
                            .get(entitiesInfoCache.getCollectionType(field));
            apiHooksType =
                    ParameterizedTypeName.get(
                    ClassName.get(EmbeddedCollectionApiHooks.class),
                    ClassName.get(typeElement),
                    ClassName.get((TypeElement) field.getEnclosingElement()));
        }
        assert apiHooksType != null;
        return
                FieldSpec
                .builder(apiHooksType,
                        ApifiStaticUtils.camelcaseNameOf(field) +
                              EmbeddedCollectionApiHooks.class.getSimpleName(),
                        Modifier.PRIVATE)
                        .addAnnotation(Autowired.class)
                .build();
    }
}
