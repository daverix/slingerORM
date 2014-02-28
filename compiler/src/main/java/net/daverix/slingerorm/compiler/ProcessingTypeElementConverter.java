package net.daverix.slingerorm.compiler;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class ProcessingTypeElementConverter implements TypeElementConverter {

    private final ProcessingEnvironment mProcessingEnvironment;

    public ProcessingTypeElementConverter(ProcessingEnvironment processingEnvironment) {
        if(processingEnvironment == null) throw new IllegalArgumentException("processingEnvironment is null");
        mProcessingEnvironment = processingEnvironment;
    }

    @Override
    public TypeElement asTypeElement(TypeMirror typeMirror) {
        Types TypeUtils = mProcessingEnvironment.getTypeUtils();
        return (TypeElement)TypeUtils.asElement(typeMirror);
    }
}
