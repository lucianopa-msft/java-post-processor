package com.azure.communication.processors;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Set;

public class ExtensibleEnumProcessor implements ISourceProcessor {
    private Set<String> mExtensibleEnums;

    public ExtensibleEnumProcessor(Set<String> extensibleEnums) {
        this.mExtensibleEnums = extensibleEnums;
    }

    public void process(Set<Path> javaSources, CompilationUnit compilationUnit) {

    }
}
