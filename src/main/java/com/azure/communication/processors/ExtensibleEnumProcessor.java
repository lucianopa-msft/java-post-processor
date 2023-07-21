package com.azure.communication.processors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

import java.util.Set;
import java.util.stream.Stream;

public class ExtensibleEnumProcessor implements ISourceProcessor {
    private final Set<String> mExtensibleEnums;

    public ExtensibleEnumProcessor(Set<String> extensibleEnums) {
        this.mExtensibleEnums = extensibleEnums;
        System.out.println(mExtensibleEnums);
    }

    public void process(CompilationUnit compilationUnit) {
        Stream<EnumDeclaration> enumDecls = compilationUnit
                .findAll(EnumDeclaration.class).stream()
                .filter(enumDecl -> mExtensibleEnums.contains(enumDecl.getNameAsString()));

        for (EnumDeclaration enumDeclaration : enumDecls.toList()) {
            compilationUnit.remove(enumDeclaration);
        }
    }

    public ClassOrInterfaceDeclaration makeExtensibleEnumDeclaration(EnumDeclaration enumDeclaration) {
        // TODO: Implement
        return null;
    }
}
