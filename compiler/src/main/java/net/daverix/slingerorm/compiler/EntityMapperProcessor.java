package net.daverix.slingerorm.compiler;

import com.google.auto.service.AutoService;

import net.daverix.slingerorm.internal.EntityMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static net.daverix.slingerorm.compiler.ListUtils.filter;
import static net.daverix.slingerorm.compiler.StringUtils.lowerCaseFirstCharacter;

@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.slingerorm.internal.EntityMapper")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class EntityMapperProcessor extends AbstractProcessor {
    private Set<TypeElement> mappers;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mappers = new HashSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {
            for (Element entity : roundEnvironment.getElementsAnnotatedWith(EntityMapper.class)) {
                //TODO: fail when class is abstract
                //TODO: fail when class is not public
                //TODO: fail when class is not implementing Mapper interface
                mappers.add((TypeElement) entity);
            }

            if(!roundEnvironment.processingOver()) return false;

            buildClass();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapper class: " + e.getLocalizedMessage());
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Internal error: " + StacktraceUtils.getStackTraceString(e));
        }
        return true;
    }

    private void buildClass() throws IOException, InvalidElementException {
        JavaFileObject jfo = processingEnv.getFiler().createSourceFile("net.daverix.slingerorm.android.SlingerStorageBuilder");
        try (BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
            bw.write("package net.daverix.slingerorm.android;\n");
            bw.write("\n");
            bw.write("import android.database.sqlite.SQLiteDatabase;");
            bw.write("\n");
            bw.write("public class SlingerStorageBuilder {\n");
            bw.write("    private AbstractDatabaseProxy databaseProxy;\n");
            for (TypeElement typeElement : mappers) {
                bw.write("    private " + typeElement.getQualifiedName() + " " + lowerCaseFirstCharacter(typeElement.getSimpleName().toString()) + ";\n");
            }
            bw.write("\n");

            bw.write("    public SlingerStorageBuilder databaseProxy(AbstractDatabaseProxy databaseProxy) {\n");
            bw.write("        if(databaseProxy == null) throw new IllegalArgumentException(\"databaseProxy is null\");\n");
            bw.write("        this.databaseProxy = databaseProxy;\n");
            bw.write("        return this;\n");
            bw.write("    }\n");
            bw.write("\n");

            bw.write("    public SlingerStorageBuilder database(SQLiteDatabase database) {\n");
            bw.write("        if(database == null) throw new IllegalArgumentException(\"database is null\");\n");
            bw.write("        this.databaseProxy = new SQLiteDatabaseProxy(database);\n");
            bw.write("        return this;\n");
            bw.write("    }\n");
            bw.write("\n");

            for (TypeElement typeElement : mappers) {
                String lowerCaseName = lowerCaseFirstCharacter(typeElement.getSimpleName().toString());
                bw.write("    public SlingerStorageBuilder " + lowerCaseName + "(" + typeElement.getQualifiedName() + " " + lowerCaseName + ") {\n");
                bw.write("        if(" + lowerCaseName + " == null) throw new IllegalArgumentException(\"" + lowerCaseName + " is null\");\n");
                bw.write("        this." + lowerCaseName + " = " + lowerCaseName + ";\n");
                bw.write("        return this;\n");
                bw.write("    }\n");
                bw.write("\n");
            }

            bw.write("    public Storage build() {\n");
            bw.write("        if(databaseProxy == null) throw new IllegalStateException(\"required database or database proxy has not been set.\");\n");
            for (TypeElement typeElement : mappers) {
                String lowerCaseName = lowerCaseFirstCharacter(typeElement.getSimpleName().toString());
                if (hasEmptyPublicConstructor(typeElement)) {
                    bw.write("        if(" + lowerCaseName + " == null) throw new IllegalStateException(\"" + lowerCaseName + " has not been set. If the constructor would have been empty then this would not be needed.\");\n");
                } else {
                    bw.write("        if(" + lowerCaseName + " == null) " + lowerCaseName + " = new " + typeElement.getQualifiedName() + "();\n");
                }
            }
            bw.write("\n");
            bw.write("        Storage storage = new SlingerStorage(databaseProxy);\n");

            for (TypeElement typeElement : mappers) {
                String lowerCaseName = lowerCaseFirstCharacter(typeElement.getSimpleName().toString());
                String databaseEntityName = getDatabaseName(typeElement);
                bw.write("        storage.registerMapper(" + databaseEntityName + ".class, " + lowerCaseName + ");\n");
            }
            bw.write("        return storage;\n");
            bw.write("    }\n");
            bw.write("}\n");
        }
    }

    private String getDatabaseName(TypeElement mapperType) throws InvalidElementException {
        List<? extends TypeMirror> interfaces = mapperType.getInterfaces();
        for (int i = 0; i < interfaces.size(); i++) {
            TypeMirror typeMirror = interfaces.get(i);
            if(typeMirror.getKind() != TypeKind.DECLARED)
                continue;

            TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
            if("net.daverix.slingerorm.android.Mapper".equals(typeElement.getQualifiedName().toString())) {
                DeclaredType declaredType = (DeclaredType) typeMirror;
                TypeMirror databaseEntityType = declaredType.getTypeArguments().get(0);
                TypeElement databaseEntityElement = (TypeElement) processingEnv.getTypeUtils().asElement(databaseEntityType);
                return databaseEntityElement.getQualifiedName().toString();
            }
        }

        throw new InvalidElementException("Mapper doesn't implement the Mapper interface directly?", mapperType);
    }

    private boolean hasEmptyPublicConstructor(TypeElement typeElement) {
        List<Element> constructors = filter(ElementUtils.getElementsInTypeElement(typeElement), new Predicate<Element>() {
            @Override
            public boolean test(Element item) {
                return item.getKind() == ElementKind.CONSTRUCTOR &&
                        item.getModifiers().contains(Modifier.PUBLIC) &&
                        ((ExecutableElement)item).getParameters().size() == 0;
            }
        });
        return constructors.size() == 0;
    }
}
