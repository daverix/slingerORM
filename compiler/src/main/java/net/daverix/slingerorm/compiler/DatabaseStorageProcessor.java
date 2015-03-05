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

import com.google.auto.service.AutoService;

import net.daverix.slingerorm.annotation.DatabaseStorage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseStorage")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseStorageProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseStorage.class)) {
            try {
                createStorage((TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating storage class: " + e.getLocalizedMessage());
            }
        }
        return true; // no further processing of this annotation type
    }

    protected void createStorage(TypeElement entity) throws IOException {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "creating implementation of " + entity.getSimpleName(), entity);

        String qualifiedName = entity.getQualifiedName().toString();
        int lastDot = qualifiedName.lastIndexOf(".");
        String packageName = qualifiedName.substring(0, lastDot);
        String storageImplName = "Slinger_" + entity.getSimpleName();

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                packageName + "." + storageImplName);

        DatabaseStorage databaseStorage = entity.getAnnotation(DatabaseStorage.class);
        TypeElement databaseStorageElement = getMirrorForClass(databaseStorage);
        String serializerQualifiedName = databaseStorageElement.getQualifiedName().toString();

        BufferedWriter bw = new BufferedWriter(jfo.openWriter());
        bw.write("package " + packageName + ";\n" +
                 "public class " + storageImplName + " implements " + entity.getSimpleName() + " {\n" +
                "   private " + serializerQualifiedName + " serializer;\n" +
                "\n" +
                "   private " + storageImplName + "(" + serializerQualifiedName + " serializer) {\n" +
                "       this.serializer = serializer;\n" +
                "   }\n" +
                "\n" +
                "   public static Builder builder() {\n" +
                "       return new Builder();\n" +
                "   }\n" +
                "\n" +
                "   public static final class Builder {\n" +
                "       private " + serializerQualifiedName + " serializer;\n" +
                "\n" +
                "       private Builder() {\n" +
                "       }\n" +
                "\n" +
                "       public Builder serializer(" + serializerQualifiedName + " serializer) {\n" +
                "           this.serializer = serializer;\n" +
                "           return this;\n" +
                "       }\n" +
                "\n" +
                "       public " + entity.getSimpleName() + " build() {\n" +
                "           if(serializer == null) {\n" +
                "               serializer = new " + serializerQualifiedName + "();\n" +
                "           }\n" +
                "\n" +
                "           return new " + storageImplName + "(serializer);\n" +
                "       }\n" +
                "   }\n" +
                "}\n");

        bw.close();
    }

    private TypeElement getMirrorForClass(DatabaseStorage databaseStorage) {
        try {
            databaseStorage.serializer();
            return null; //should never be here, this is an ugly hack :)
        } catch (MirroredTypeException mte) {
            return asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement asTypeElement(TypeMirror typeMirror) {
        Types TypeUtils = processingEnv.getTypeUtils();
        return (TypeElement)TypeUtils.asElement(typeMirror);
    }
}
