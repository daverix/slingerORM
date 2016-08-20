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
    private TypeElementProvider typeElementProvider;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        packageProvider = new PackageProvider();
        typeElementConverter = new TypeElementConverterImpl(processingEnv.getTypeUtils());
        typeElementProvider = new TypeElementProvider(processingEnv.getElementUtils());
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: " + e.getMessage() + " stacktrace: " + StacktraceUtils.getStackTraceString(e), e.getElement());
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Internal error: " + StacktraceUtils.getStackTraceString(e));
            }
        }
        return true;
    }

    private void createMapper(PackageProvider packageProvider, TypeElementConverter typeElementConverter, TypeElement entity) throws IOException, InvalidElementException {
        if(entity == null) throw new IllegalArgumentException("entity is null");

        DatabaseEntityModel model = new DatabaseEntityModel(entity, typeElementConverter, typeElementProvider, packageProvider);
        model.initialize();

        String packageName = model.getMapperPackageName();
        String mapperName = model.getMapperClassName();

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName + "." + mapperName);
        try (BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
            new DatabaseEntityMapperBuilder(bw).build(model);
        }
    }
}
