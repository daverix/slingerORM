package net.daverix.slingerorm.compiler;

import com.google.auto.service.AutoService;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.slingerorm.entity.DatabaseEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DatabaseEntityProcessorShim extends DatabaseEntityProcessor {
}
