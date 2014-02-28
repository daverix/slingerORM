package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.annotation.DatabaseEntity;

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
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

/**
 * This Processor creates Mappers for each class annotated with the DatabaseEntity annotation.
 */
@SupportedAnnotationTypes("net.daverix.slingerorm.annotation.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseEntityProcessor extends AbstractProcessor implements Logger {
    private final EntityMappingWriter mEntityMappingWriter = new EntityMappingWriter(this);

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element entity : roundEnvironment.getElementsAnnotatedWith(DatabaseEntity.class)) {
            try {
                if(isAbstract(entity)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Skipping abstract entity " + entity.getSimpleName());
                    continue;
                }

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating " + entity.getSimpleName() + "Mapping");
                createMapper((TypeElement) entity);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error creating mapping class: " + e.getLocalizedMessage());
            }
        }
        return true; // no further processing of this annotation type
    }

    protected void createMapper(TypeElement entity) throws IOException {
        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(entity.getQualifiedName() + "Mapping");
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

    @Override
    public void debug(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    @Override
    public void debug(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }
}
