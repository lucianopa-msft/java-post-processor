package com.azure.communication.processors;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Set;

public interface ISourceProcessor {
    public void process(Set<Path> javaSources, CompilationUnit compilationUnit);
}
