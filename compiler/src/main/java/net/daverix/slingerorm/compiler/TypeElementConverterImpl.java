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
