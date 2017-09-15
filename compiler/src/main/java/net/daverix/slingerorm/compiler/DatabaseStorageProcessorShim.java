package net.daverix.slingerorm.compiler;

import com.google.auto.service.AutoService;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "net.daverix.slingerorm.storage.DatabaseStorage"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DatabaseStorageProcessorShim extends DatabaseStorageProcessor {
}
