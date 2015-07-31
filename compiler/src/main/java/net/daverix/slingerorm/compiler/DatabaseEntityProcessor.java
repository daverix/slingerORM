/*
 * Copyright 2015 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.daverix.slingerorm.compiler;

import com.google.auto.service.AutoService;

import net.daverix.slingerorm.annotation.DatabaseEntity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseEntityProcessor extends AbstractProcessor {
    private PackageProvider packageProvider;
    private TypeElementConverter typeElementConverter;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        packageProvider = new PackageProvider();
        typeElementConverter = new TypeElementConverterImpl(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseEntity.class)) {
            if(entity.getModifiers().contains(Modifier.ABSTRACT)) continue;

            try {
                createMapper(packageProvider, typeElementConverter, (TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: " + e.getLocalizedMessage());
            } catch (InvalidElementException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: " + e.getMessage(), e.getElement());
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Internal error: " + StacktraceUtils.getStackTraceString(e));
            }
        }
        return true;
    }

    private void createMapper(PackageProvider packageProvider, TypeElementConverter typeElementConverter, TypeElement entity) throws IOException, InvalidElementException {
        if(entity == null) throw new IllegalArgumentException("entity is null");

        DatabaseEntityModel model = new DatabaseEntityModel(entity, typeElementConverter);

        String qualifiedName = entity.getQualifiedName().toString();
        String packageName = packageProvider.getPackage(qualifiedName);
        String mapperName = entity.getSimpleName() + "Mapper";
        String createTableSql = model.createTableSql();
        List<FieldMethod> setters = model.getSetters();
        List<FieldMethod> getters = model.getGetters();
        String deleteSql = model.getItemSql();
        String deleteSqlArgs = model.getItemSqlArgs();

        TypeElement serializerElement = model.getSerializerElement();
        String serializerQualifiedName = serializerElement.getQualifiedName().toString();
        String serializerSimpleName = serializerElement.getSimpleName().toString();

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName + "." + mapperName);
        BufferedWriter bw = new BufferedWriter(jfo.openWriter());
        try {
            DatabaseEntityMapperBuilder.builder(bw)
                    .setDatabaseEntityClassName(entity.getSimpleName().toString())
                    .setPackageName(packageName)
                    .setSerializerClassName(serializerSimpleName)
                    .setSerializerQualifiedName(serializerQualifiedName)
                    .setTableName(model.getTableName())
                    .setCreateTableSql(createTableSql)
                    .setColumnNames(model.getColumnNames())
                    .setSetters(setters)
                    .setGetters(getters)
                    .setItemSql(deleteSql)
                    .setItemSqlArguments(deleteSqlArgs)
                    .build();
        } finally {
            bw.close();
        }
    }
}
