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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

final class DatabaseStorageBuilder {
    private final Writer writer;
    private final Set<StorageMethod> storageMethods = new HashSet<>();
    private String className;
    private String packageName;
    private String storageInterfaceName;

    DatabaseStorageBuilder(Writer writer) {
        this.writer = writer;
    }

    public static DatabaseStorageBuilder builder(Writer writer) {
        if (writer == null) throw new IllegalArgumentException("writer is null");

        return new DatabaseStorageBuilder(writer);
    }

    public DatabaseStorageBuilder setPackage(String packageName) {
        if (packageName == null) throw new IllegalArgumentException("qualifiedName is null");

        this.packageName = packageName;
        return this;
    }

    public DatabaseStorageBuilder setClassName(String className) {
        if (className == null) throw new IllegalArgumentException("className is null");

        this.className = className;
        return this;
    }

    public DatabaseStorageBuilder setStorageInterfaceName(String storageInterfaceName) {
        if (storageInterfaceName == null)
            throw new IllegalArgumentException("storageInterfaceName is null");

        this.storageInterfaceName = storageInterfaceName;
        return this;
    }

    public DatabaseStorageBuilder addMethods(Iterable<StorageMethod> storageMethods) throws IOException {
        if(storageMethods == null) throw new IllegalArgumentException("storageMethods is null");

        for(StorageMethod method : storageMethods) {
            addMethod(method);
        }
        return this;
    }

    public DatabaseStorageBuilder addMethod(StorageMethod storageMethod) throws IOException {
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

        writePackage();
        Collection<MapperDescription> mapperDescriptions = getMapperDescriptions();

        writeImports(mapperDescriptions);
        writeClass(mapperDescriptions);
    }

    private void writeClass(Collection<MapperDescription> mapperDescriptions) throws IOException {
        writer.write("public class " + className + " implements " + storageInterfaceName + " {\n");
        writer.write("    private final SQLiteDatabase db;\n");
        for(MapperDescription description : mapperDescriptions) {
            writer.write("    private final Mapper<" + description.getEntityName() + "> " + description.getVariableName() + ";\n");
        }
        writeln();

        writer.write("    private " + className + "(Builder builder) {\n");
        writer.write("        this.db = builder.db;\n");
        for(MapperDescription description : mapperDescriptions) {
            writer.write("        this." + description.getVariableName() + " = builder." + description.getVariableName() + ";\n");
        }
        writer.write("    }\n");
        writeln();

        writeMethods();

        writer.write("    public static Builder builder() {\n");
        writer.write("        return new Builder();\n");
        writer.write("    }\n");
        writeln();

        writeStorageBuilder(mapperDescriptions);

        writer.write("}\n");
    }

    private Collection<MapperDescription> getMapperDescriptions() {
        return storageMethods.stream()
                .map(StorageMethod::getMapper)
                .distinct()
                .sorted((first, second) -> first.getVariableName().compareTo(second.getVariableName()))
                .collect(toSet());
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

    private void writeImports(Collection<MapperDescription> mapperDescriptions) throws IOException {
        Set<String> qualifiedNames = new HashSet<String>();
        for(StorageMethod storageMethod : storageMethods) {
            qualifiedNames.addAll(storageMethod.getImports());
        }

        for (MapperDescription mapperDescription : mapperDescriptions) {
            qualifiedNames.add("net.daverix.slingerorm.android.Mapper");
            qualifiedNames.add(mapperDescription.getQualifiedName());
        }

        qualifiedNames.remove("");

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

    private void writeStorageBuilder(Collection<MapperDescription> mapperDescriptions) throws IOException {
        writer.write("    public static final class Builder {\n");
        writer.write("        private SQLiteDatabase db;\n");

        for(MapperDescription description : mapperDescriptions) {
            writer.write("        private Mapper<" + description.getEntityName() + "> " + description.getVariableName() + ";\n");
        }
        writeln();
        writer.write("        private Builder() {\n");
        writer.write("        }\n");
        writeln();

        writer.write("        public Builder database(SQLiteDatabase db) {\n");
        writer.write("            if (db == null)\n");
        writer.write("                throw new IllegalArgumentException(\"db is null\");\n\n");
        writer.write("            this.db = db;\n");
        writer.write("            return this;\n");
        writer.write("        }\n");
        writeln();

        for(MapperDescription description : mapperDescriptions) {
            writer.write("        public Builder " + description.getVariableName() + "(Mapper<" + description.getEntityName() + "> " + description.getVariableName() + ") {\n");
            writer.write("            this." + description.getVariableName() + " = " + description.getVariableName() + ";\n");
            writer.write("            return this;\n");
            writer.write("        }\n");
            writeln();
        }

        writer.write("        public " + storageInterfaceName + " build() {\n");
        writer.write("            if (db == null)\n");
        writer.write("                throw new IllegalStateException(\"database must be set\");\n");
        writeln();

        for(MapperDescription description : mapperDescriptions) {
            writer.write("            if (" + description.getVariableName() + " == null)\n");
            if(description.hasEmptyConstructor()) {
                writer.write("                " + description.getVariableName() + " = new " + description.getEntityName() + "Mapper();\n");
            }
            else {
                writer.write("                throw new IllegalStateException(\"" + description.getVariableName() + " must be set using " + description.getVariableName() + "()\");\n");
            }
            writeln();
        }

        writer.write("            return new " + className + "(this);\n");
        writer.write("        }\n");

        writer.write("    }\n");
    }
}
