package com.azure.communication.processors;

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

/**
 * Transforms a Java enum into a extensible enum following the
 * <a href="https://azure.github.io/azure-sdk/java_introduction.html#enumerations">Azure Review Board guidelines</a>.
 */
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
        ClassOrInterfaceDeclaration extensibleEnumClass = compilationUnit
                .addClass(
                    className,
                    Modifier.Keyword.PUBLIC,
                    Modifier.Keyword.FINAL)
                .addExtendedType("ExpandableStringEnum<" + className + ">");
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

        this.addFromStringMethodDeclaration(extensibleEnumClass, enumDeclaration);

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

        this.addFindOrdinalMethod(extensibleEnumClass, enumDeclaration);
    }

    // public static EnumName fromString(String name) declaration and body.
    private void addFromStringMethodDeclaration(
            ClassOrInterfaceDeclaration extensibleEnumClass,
            EnumDeclaration enumDeclaration
    ) {
        String className = enumDeclaration.getNameAsString();
        String javadocComment =
                "* Creates or finds a {@link "+ className + "} from its string representation.\n" +
                "* @param name a name to look for.\n" +
                "* @return the corresponding {@link "+ className + "}\n";

        // int ordinal = EnumName.findOrdinalByName(name);
        MethodCallExpr callToFindOrdinal = new MethodCallExpr()
                .setScope(new NameExpr(className))
                .setName("findOrdinalByName")
                .addArgument(new NameExpr("name"));
        VariableDeclarator ordinalDeclarator = new VariableDeclarator()
                .setName("ordinal")
                .setType(PrimitiveType.intType())
                .setInitializer(callToFindOrdinal);
        VariableDeclarationExpr ordinalValueDeclaration = new VariableDeclarationExpr()
                .setVariables(NodeList.nodeList(ordinalDeclarator));

        // return fromString(name, EnumName.class).setMOrdinal(ordinal);
        MethodCallExpr callToFromString = new MethodCallExpr()
                .setName("fromString")
                .addArgument(new NameExpr("name"))
                .addArgument(new ClassExpr().setType(className));
        MethodCallExpr callToSetOrdinal = new MethodCallExpr()
                .setScope(callToFromString)
                .setName("setMOrdinal")
                .addArgument("ordinal");
        ReturnStmt returnStmt = new ReturnStmt()
                .setExpression(callToSetOrdinal);

        BlockStmt fnBody = new BlockStmt()
                .addStatement(ordinalValueDeclaration)
                .addStatement(returnStmt);

        extensibleEnumClass
                .addMethod(
                        "fromString",
                        Modifier.Keyword.PUBLIC,
                        Modifier.Keyword.STATIC)
                .setType(className)
                .addParameter(String.class, "name")
                .setBody(fnBody)
                .setJavadocComment(javadocComment);
    }

    // private static int findOrdinalByName(String name) declaration and body.
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

        SwitchStmt switchStmt = new SwitchStmt()
                .setSelector(new NameExpr(nameParam))
                .setEntries(NodeList.nodeList(switchEntries));

        extensibleEnumClass
                .addMethod(
                        "findOrdinalByName",
                        Modifier.Keyword.PRIVATE,
                        Modifier.Keyword.STATIC)
                .setType(PrimitiveType.intType())
                .addParameter(String.class, "name")
                .setBody(new BlockStmt().addStatement(switchStmt));
    }
}
