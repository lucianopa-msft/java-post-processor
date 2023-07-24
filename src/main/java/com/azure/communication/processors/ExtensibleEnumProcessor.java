package com.azure.communication.processors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.google.common.collect.Streams;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtensibleEnumProcessor implements ISourceProcessor {
    private final Set<String> mExtensibleEnums;

    public ExtensibleEnumProcessor(Set<String> extensibleEnums) {
        this.mExtensibleEnums = extensibleEnums;
        System.out.println(mExtensibleEnums);
    }

    public boolean process(CompilationUnit compilationUnit) {
        List<EnumDeclaration> enumDeclarations = compilationUnit
                .findAll(EnumDeclaration.class).stream()
                .filter(enumDeclaration -> mExtensibleEnums.contains(enumDeclaration.getNameAsString()))
                .toList();

        if (enumDeclarations.isEmpty())
            return false;

        for (EnumDeclaration enumDeclaration : enumDeclarations) {
            compilationUnit.remove(enumDeclaration);
            addExtensibleEnumDeclaration(compilationUnit, enumDeclaration);
        }
        return true;
    }

    public void addExtensibleEnumDeclaration(
            CompilationUnit compilationUnit,
            EnumDeclaration enumDeclaration
    ) {
        compilationUnit.addImport("com.azure.core.util.ExpandableStringEnum");
        String className = enumDeclaration.getNameAsString();
        ClassOrInterfaceDeclaration extensibleEnumClass = compilationUnit.addClass(
                className,
                Modifier.Keyword.PUBLIC,
                Modifier.Keyword.FINAL);
        extensibleEnumClass.addExtendedType("ExpandableStringEnum<" + className + ">");
        Optional<JavadocComment> classDoc = enumDeclaration.getJavadocComment();
        classDoc.ifPresent(extensibleEnumClass::setJavadocComment);

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

            enumField.getJavadocComment()
                    .ifPresent(fieldDeclaration::setJavadocComment);
        }

        String fromStringMethodBody =
                "/**\n" +
                "* Creates or finds a {@link "+ className + "} from its string representation.\n" +
                "* @param name a name to look for\n" +
                "* @return the corresponding {@link "+ className + "}\n" +
                "*/\n" +
                "public static " + className + " fromString(String name) {\n" +
                "    int ordinal = " + className + ".findOrdinalByName(name);" +
                "    return fromString(name, " + className + ".class).setMOrdinal(ordinal);\n" +
                "}";
        ParseResult<BodyDeclaration<?>> bodyDeclaration = new JavaParser()
                .parseBodyDeclaration(fromStringMethodBody);
        bodyDeclaration.getResult()
                .ifPresent(result -> extensibleEnumClass.addMember(result.asMethodDeclaration()));

        // Private ordinal members
        String ordinalMember = "mOrdinal";
        FieldDeclaration mOrdinalField = extensibleEnumClass.addFieldWithInitializer(
                PrimitiveType.intType(),
                ordinalMember,
                new IntegerLiteralExpr("0"),
                Modifier.Keyword.PRIVATE
        );

        mOrdinalField.createSetter()
                .setType(className)
                .setModifiers(Modifier.Keyword.PRIVATE)
                .getBody()
                .ifPresent(blockStmt -> blockStmt.addStatement(new ReturnStmt(new NameExpr("this"))));

        ReturnStmt returnStmt = new ReturnStmt(new NameExpr(ordinalMember));
        BlockStmt stmt = new BlockStmt()
                .addStatement(returnStmt);
        extensibleEnumClass
                .addMethod("ordinal", Modifier.Keyword.PRIVATE)
                .setType(PrimitiveType.intType())
                .setBody(stmt);

        addFindOrdinalMethod(extensibleEnumClass, enumDeclaration);
    }

    private void addFindOrdinalMethod(
            ClassOrInterfaceDeclaration extensibleEnumClass,
            EnumDeclaration enumDeclaration
    ) {
        String nameParam = "name";
        List<EnumConstantDeclaration> fields = enumDeclaration.getEntries();
        List<SwitchEntry> switchEntries = Streams.mapWithIndex(fields.stream(), (field, index) -> {
            ReturnStmt returnStmt = new ReturnStmt(new IntegerLiteralExpr(String.valueOf(index)));
            StringLiteralExpr label = new StringLiteralExpr(field.getNameAsString());
            return new SwitchEntry()
                    .setLabels(NodeList.nodeList(label))
                    .setStatements(NodeList.nodeList(returnStmt));
        }).collect(Collectors.toList());

        // Default entry
        ObjectCreationExpr oce = new ObjectCreationExpr()
                .setType(RuntimeException.class)
                .addArgument(new StringLiteralExpr("Cannot get the ordinal from string"));
        ThrowStmt throwStmt = new ThrowStmt(oce);
        SwitchEntry defaultEntry = new SwitchEntry()
                .setStatements(NodeList.nodeList(throwStmt));
        switchEntries.add(defaultEntry);

        SwitchExpr switchExpr = new SwitchExpr()
                .setSelector(new NameExpr(nameParam))
                .setEntries(NodeList.nodeList(switchEntries));

        extensibleEnumClass
                .addMethod(
                        "findOrdinalByName",
                        Modifier.Keyword.PRIVATE,
                        Modifier.Keyword.STATIC)
                .setType(PrimitiveType.intType())
                .addParameter("String", "name")
                .setBody(new BlockStmt().addStatement(switchExpr));
    }
}
