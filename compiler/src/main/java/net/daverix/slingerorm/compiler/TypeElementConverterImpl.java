package net.daverix.slingerorm.compiler;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class TypeElementConverterImpl implements TypeElementConverter {
    private ProcessingEnvironment processingEnv;

    public TypeElementConverterImpl(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public TypeElement asTypeElement(TypeMirror typeMirror) {
        if(typeMirror == null) throw new IllegalArgumentException("typeMirror is null");

        return (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
    }
}
