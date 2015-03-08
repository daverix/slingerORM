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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class StorageClassBuilder {
    private final Writer writer;
    private final Set<StorageMethod> storageMethods = new HashSet<StorageMethod>();
    private String className;
    private String packageName;
    private Serializer serializer;
    private String storageInterfaceName;

    StorageClassBuilder(Writer writer) {
        this.writer = writer;
    }

    public static StorageClassBuilder builder(Writer writer) {
        if (writer == null) throw new IllegalArgumentException("writer is null");

        return new StorageClassBuilder(writer);
    }

    public StorageClassBuilder setPackage(String packageName) {
        if (packageName == null) throw new IllegalArgumentException("qualifiedName is null");

        this.packageName = packageName;
        return this;
    }

    public StorageClassBuilder setClassName(String className) {
        if (className == null) throw new IllegalArgumentException("className is null");

        this.className = className;
        return this;
    }

    public StorageClassBuilder setStorageInterfaceName(String storageInterfaceName) {
        if (storageInterfaceName == null)
            throw new IllegalArgumentException("storageInterfaceName is null");

        this.storageInterfaceName = storageInterfaceName;
        return this;
    }

    public StorageClassBuilder setSerializer(String qualifiedName, String className, boolean emptyConstructor) {
        if (qualifiedName == null) throw new IllegalArgumentException("qualifiedName is null");
        if (className == null) throw new IllegalArgumentException("className is null");

        serializer = new Serializer(qualifiedName, className, emptyConstructor);
        return this;
    }

    public StorageClassBuilder addMethods(Iterable<StorageMethod> storageMethods) throws IOException {
        if(storageMethods == null) throw new IllegalArgumentException("storageMethods is null");

        for(StorageMethod method : storageMethods) {
            addMethod(method);
        }
        return this;
    }

    public StorageClassBuilder addMethod(StorageMethod storageMethod) throws IOException {
        if(storageMethod == null) throw new IllegalArgumentException("storageMethod is null");

        storageMethods.add(storageMethod);
        return this;
    }

    public void build() throws IOException {
        if (packageName == null)
            throw new IllegalStateException("qualifiedName must be set");
        if (className == null)
            throw new IllegalStateException("className must be set");
        if (storageInterfaceName == null)
            throw new IllegalStateException("storageInterface must be set");
        if (serializer == null)
            throw new IllegalStateException("serializer must be set");

        writePackage();
        writeImports();
        writeClass();
    }

    private void writeClass() throws IOException {
        writer.write("public class " + className + " implements " + storageInterfaceName + " {\n");
        writer.write("    private final " + serializer.getClassName() + " serializer;\n");
        writeln();
        writer.write("    private " + className + "(" + serializer.getClassName() + " serializer) {\n");
        writer.write("        this.serializer = serializer;\n");
        writer.write("    }\n");
        writeln();

        writeMethods();

        writer.write("    public static Builder builder() {\n");
        writer.write("        return new Builder();\n");
        writer.write("    }\n");
        writeln();

        writeStorageBuilder();

        writer.write("}\n");
    }

    private void writeMethods() throws IOException {
        for(StorageMethod storageMethod : storageMethods) {
            storageMethod.write(writer);
        }
    }

    private void writePackage() throws IOException {
        writer.write("package " + packageName + ";\n");
        writeln();
    }

    private void writeImports() throws IOException {
        Set<String> qualifiedNames = new HashSet<String>();
        qualifiedNames.add(serializer.getQualifiedName());
        for(StorageMethod storageMethod : storageMethods) {
            qualifiedNames.addAll(storageMethod.getImports());
        }

        List<String> sortedNames = new ArrayList<String>(qualifiedNames);
        Collections.sort(sortedNames);
        for(String qualifiedName : sortedNames) {
            writeImport(qualifiedName);
        }
        writeln();
    }

    private void writeImport(String importPackageName) throws IOException {
        writer.write(String.format("import %s;\n", importPackageName));
    }

    private void writeln() throws IOException {
        writer.write("\n");
    }

    private void writeStorageBuilder() throws IOException {
        writer.write("    public static final class Builder {\n");
        writer.write("        private " + serializer.getClassName() + " serializer;\n");
        writeln();
        writer.write("        private Builder() {\n");
        writer.write("        }\n");
        writeln();
        writer.write("        public Builder serializer(" + serializer.getClassName() + " serializer) {\n");
        writer.write("            this.serializer = serializer;\n");
        writer.write("            return this;\n");
        writer.write("        }\n");
        writeln();

        writer.write("        public " + storageInterfaceName + " build() {\n");
        writer.write("            if(serializer == null) {\n");
        if (serializer.isEmptyConstructor()) {
            writer.write("                serializer = new " + serializer.getClassName() + "();\n");
        } else {
            writer.write("                throw new IllegalArgumentException(\"serializer not set\");\n");
        }
        writer.write("            }\n");
        writeln();
        writer.write("            return new " + className + "(serializer);\n");
        writer.write("        }\n");

        writer.write("    }\n");
    }

    private static class Serializer {
        private final String qualifiedName;
        private final String className;
        private final boolean emptyConstructor;

        private Serializer(String qualifiedName, String className, boolean emptyConstructor) {
            this.qualifiedName = qualifiedName;
            this.className = className;
            this.emptyConstructor = emptyConstructor;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getClassName() {
            return className;
        }

        public boolean isEmptyConstructor() {
            return emptyConstructor;
        }
    }
}
