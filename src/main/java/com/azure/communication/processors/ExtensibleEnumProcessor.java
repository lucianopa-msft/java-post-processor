package com.azure.communication.processors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.google.common.collect.Streams;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ExtensibleEnumProcessor implements ISourceProcessor {
    private final Set<String> mExtensibleEnums;

    public ExtensibleEnumProcessor(Set<String> extensibleEnums) {
        this.mExtensibleEnums = extensibleEnums;
        System.out.println(mExtensibleEnums);
    }

    public boolean process(CompilationUnit compilationUnit) {
        List<EnumDeclaration> enumDecls = compilationUnit
                .findAll(EnumDeclaration.class).stream()
                .filter(enumDecl -> mExtensibleEnums.contains(enumDecl.getNameAsString()))
                .toList();

        if (enumDecls.isEmpty())
            return false;

        for (EnumDeclaration enumDeclaration : enumDecls) {
            compilationUnit.remove(enumDeclaration);
            addExtensibleEnumDeclaration(compilationUnit, enumDeclaration);
        }
        return true;
    }

    public void addExtensibleEnumDeclaration(CompilationUnit compilationUnit, EnumDeclaration enumDeclaration) {
        compilationUnit.addImport("com.azure.core.util.ExpandableStringEnum");
        String className = enumDeclaration.getNameAsString();
        ClassOrInterfaceDeclaration extensibleEnumClass = compilationUnit.addClass(
                className,
                Modifier.Keyword.PUBLIC,
                Modifier.Keyword.FINAL);
        extensibleEnumClass.addExtendedType("ExpandableStringEnum<" + className + ">");
        Optional<JavadocComment> classDoc = enumDeclaration.getJavadocComment();
        if (classDoc.isPresent()) {
            extensibleEnumClass.setJavadocComment(classDoc.get());
        }

        for (EnumConstantDeclaration enumField: enumDeclaration.getEntries()) {
            StringLiteralExpr nameLiteral = new StringLiteralExpr(enumField.getNameAsString());
            MethodCallExpr fromStringCall = new MethodCallExpr("fromString", nameLiteral);

            FieldDeclaration fieldDeclaration = extensibleEnumClass.addFieldWithInitializer(
                    className,
                    enumField.getNameAsString(),
                    fromStringCall,
                    Modifier.Keyword.PUBLIC,
                    Modifier.Keyword.STATIC
            );

            Optional<JavadocComment> comment = enumField.getJavadocComment();
            if (comment.isPresent()) {
                fieldDeclaration.setJavadocComment(comment.get());
            }
        }

        String fromStringMethodBody =
                "/**\n" +
                "* Creates or finds a {@link "+ className + "} from its string representation.\n" +
                "* @param name a name to look for\n" +
                "* @return the corresponding {@link "+ className + "}\n" +
                "*/\n" +
                "public static "+ className + " fromString(String name) {\n" +
                "    return fromString(name, " + className + ".class);\n" +
                "}";
        ParseResult<BodyDeclaration<?>> bodyDeclaration = new JavaParser()
                .parseBodyDeclaration(fromStringMethodBody);
        MethodDeclaration methodDecl = bodyDeclaration.getResult().get().asMethodDeclaration();
        extensibleEnumClass.addMember(methodDecl);

        // Private ordinal members
        String ordinalMember = "mOrdinal";
        extensibleEnumClass.addFieldWithInitializer(
                PrimitiveType.intType(),
                ordinalMember,
                new IntegerLiteralExpr(0),
                Modifier.Keyword.PRIVATE
        );

        ReturnStmt returnStmt = new ReturnStmt(new NameExpr(ordinalMember));
        BlockStmt stmt = new BlockStmt()
                .addStatement(returnStmt);
        extensibleEnumClass
                .addMethod("ordinal", Modifier.Keyword.PRIVATE)
                .setType(PrimitiveType.intType())
                .setBody(stmt);


    }

    private void addFindOrdinalMethod(ClassOrInterfaceDeclaration extensibleEnumClass, EnumDeclaration enumDeclaration) {
        // TODO: Implement
        List<FieldDeclaration> fields = enumDeclaration.getFields();
        Streams.mapWithIndex(fields.stream(), (idx, field) -> {
           // new SwitchEntry().set
           return 0;
        });


        extensibleEnumClass
                .addMethod(
                        "findOrdinalByName",
                        Modifier.Keyword.PRIVATE,
                        Modifier.Keyword.STATIC);

    }
}
