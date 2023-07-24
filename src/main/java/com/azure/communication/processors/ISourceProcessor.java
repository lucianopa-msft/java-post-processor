package com.azure.communication.processors;

import com.github.javaparser.ast.CompilationUnit;

public interface ISourceProcessor {
    boolean process(CompilationUnit compilationUnit);
}
