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
import net.daverix.slingerorm.annotation.Delete;
import net.daverix.slingerorm.annotation.Insert;
import net.daverix.slingerorm.annotation.Replace;
import net.daverix.slingerorm.annotation.Select;
import net.daverix.slingerorm.annotation.Update;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
        String qualifiedName = entity.getQualifiedName().toString();
        String packageName = getPackage(qualifiedName);
        String storageImplName = "Slinger_" + entity.getSimpleName();

        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName + "." + storageImplName);

        DatabaseStorage databaseStorage = entity.getAnnotation(DatabaseStorage.class);
        TypeElement serializerElement = getSerializerElement(databaseStorage);
        String serializerQualifiedName = serializerElement.getQualifiedName().toString();
        String serializerClassName = serializerElement.getSimpleName().toString();
        String serializerPackageName = getPackage(serializerQualifiedName);

        List<StorageMethod> methods = getStorageMethods(entity);

        BufferedWriter bw = new BufferedWriter(jfo.openWriter());
        try {
            StorageClassBuilder.builder(bw)
                    .setPackage(packageName)
                    .setClassName(storageImplName)
                    .setStorageInterfaceName(entity.getSimpleName().toString())
                    .setSerializer(serializerPackageName, serializerClassName, hasEmptyConstructor(serializerElement))
                    .addMethods(methods)
                    .build();
        } finally {
            bw.close();
        }
    }

    private List<StorageMethod> getStorageMethods(TypeElement element) {
        List<StorageMethod> methods = new ArrayList<StorageMethod>();
        for(Element enclosedElement : element.getEnclosedElements()) {
            if(enclosedElement.getKind() == ElementKind.METHOD) {
                methods.add(createStorageMethod((ExecutableElement) enclosedElement));
            }
        }
        return methods;
    }

    private StorageMethod createStorageMethod(ExecutableElement methodElement) {
        if(methodElement.getAnnotation(Insert.class) != null) {
            return createInsertMethod(methodElement);
        } else if(methodElement.getAnnotation(Replace.class) != null) {
            return createReplaceMethod(methodElement);
        } else if(methodElement.getAnnotation(Update.class) != null) {
            return createUpdateMethod(methodElement);
        } else if(methodElement.getAnnotation(Delete.class) != null) {
            return createDeleteMethod(methodElement);
        } else if(methodElement.getAnnotation(Select.class) != null) {
            return createSelectMethod(methodElement);
        }
        return null;
    }

    private StorageMethod createSelectMethod(ExecutableElement methodElement) {
        return null;
    }

    private StorageMethod createDeleteMethod(ExecutableElement methodElement) {
        return null;
    }

    private StorageMethod createUpdateMethod(ExecutableElement methodElement) {
        return null;
    }

    private StorageMethod createReplaceMethod(ExecutableElement methodElement) {
        return null;
    }

    private StorageMethod createInsertMethod(ExecutableElement methodElement) {
        return null;
    }

    private boolean hasEmptyConstructor(TypeElement element) {
        for(Element enclosedElement : element.getEnclosedElements()) {
            if(enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                if(executableElement.getParameters().size() == 0)
                    return true;
            }
        }
        return false;
    }

    private String getPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(0, lastDot);
    }

    private TypeElement getSerializerElement(DatabaseStorage databaseStorage) {
        try {
            databaseStorage.serializer();
            return null; //should never be here, this is an ugly hack :)
        } catch (MirroredTypeException mte) {
            return asTypeElement(mte.getTypeMirror());
        }
    }

    private TypeElement asTypeElement(TypeMirror typeMirror) {
        Types TypeUtils = processingEnv.getTypeUtils();
        return (TypeElement) TypeUtils.asElement(typeMirror);
    }

}
