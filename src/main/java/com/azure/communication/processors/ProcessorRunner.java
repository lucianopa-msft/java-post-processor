package com.azure.communication.processors;

import com.azure.communication.configuration.ConfigurationManager;
import com.azure.communication.configuration.ExtensibleEnumsConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ProcessorRunner {
    final private Set<Path> mJavaSources;
    final private ConfigurationManager mConfigManager;
    final private List<ISourceProcessor> mProcessors = new ArrayList<>();

    public ProcessorRunner(Set<Path> javaSources, ConfigurationManager configManager) {
        this.mJavaSources = javaSources;
        this.mConfigManager = configManager;
        this.configureProcessors();
    }

    private void configureProcessors() {
        Optional<ExtensibleEnumsConfiguration> config = mConfigManager.findConfiguration(ExtensibleEnumsConfiguration.class);
        if (config.isPresent()) {
            ExtensibleEnumProcessor enumsProcessor = new ExtensibleEnumProcessor(config.get().getConfigurationValue());
            mProcessors.add(enumsProcessor);
        }
    }

    public void run() throws IOException {

        for (Path source: mJavaSources) {
            SourceRoot sourceRoot = new SourceRoot(source.getParent());
            CompilationUnit compilationUnit = sourceRoot.parse("", source.getFileName().toString());
            for (ISourceProcessor processor : mProcessors) {
                processor.process(compilationUnit);
            }
            sourceRoot.saveAll();
        }

    }
}
