/*
 * Copyright 2014 David Laurell
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

import net.daverix.slingerorm.annotation.DatabaseEntity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
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
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseEntityProcessor extends AbstractProcessor {
    private final EntityMappingWriter mEntityMappingWriter = new EntityMappingWriter();

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseEntity.class)) {
            try {
                if(isAbstract(entity)) {
                    continue;
                }

                createMapper((TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapping class: " + e.getLocalizedMessage());
            }
        }
        return true; // no further processing of this annotation type
    }

    protected void createMapper(TypeElement entity) throws IOException {
        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(entity.getQualifiedName() + "Storage");
            BufferedWriter bw = new BufferedWriter(jfo.openWriter());

        mEntityMappingWriter.writeMapper(bw, entity, processingEnv);
        bw.close();
    }

    protected boolean isAbstract(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        for(Modifier modifier : modifiers) {
            String name = modifier.name();

            if("ABSTRACT".equals(name)) {
                return true;
            }
        }

        return false;
    }
}
