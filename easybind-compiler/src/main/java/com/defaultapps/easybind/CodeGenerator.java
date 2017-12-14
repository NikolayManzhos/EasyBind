package com.defaultapps.easybind;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;


final class CodeGenerator {

    private static final String CLASS_SUFFIX = "Binder";
    private static final String BINDER_PACKAGE = "com.deafaultapps.easybind";
    private static final String BINDER_SIMPLE_NAME = "EasyBinder";
    private static final String ON_ATTACH_NAME = "onAttach";
    private static final String ON_DETACH_NAME = "onDetach";
    private static final String ON_START_NAME = "onStart";
    private static final String ON_STOP_NAME = "onStop";

    private final String generatedClassName;
    private final String packageName;

    private TypeSpec.Builder classTypeSpec;
    private MethodSpec.Builder constructorMethodSpec;

    private MethodSpec.Builder onAttachMethodSpec;
    private MethodSpec.Builder onDetachMethodSpec;
    private MethodSpec.Builder onStartMethodSpec;
    private MethodSpec.Builder onStopMethodSpec;

    private final static String BINDING_NAME = "target";

    CodeGenerator(TypeElement annotatedClass, String packageName) {
        String annotatedClassName = annotatedClass.getSimpleName().toString();
        this.generatedClassName = annotatedClassName + CLASS_SUFFIX;
        this.packageName = packageName;
        initBuilders();
    }

    TypeSpec generateClass() {
        classTypeSpec.addMethod(constructorMethodSpec.build());
        if (onAttachMethodSpec != null) classTypeSpec.addMethod(onAttachMethodSpec.build());
        if (onDetachMethodSpec != null) classTypeSpec.addMethod(onDetachMethodSpec.build());
        if (onStartMethodSpec != null) classTypeSpec.addMethod(onStartMethodSpec.build());
        if (onStopMethodSpec != null) classTypeSpec.addMethod(onStopMethodSpec.build());
        return classTypeSpec.build();
    }

    void addInvocationToOnAttach(String variableName, String methodName) {
        onAttachMethodSpec
                .addCode(BINDING_NAME + "."
                        + variableName + "."
                        + methodName + "(" + BINDING_NAME + ");\n");
    }

    void addVariableAssignmentOnAttach(String variableName, String setupValue) {
        onAttachMethodSpec
                .addCode(BINDING_NAME + "."
                        + variableName + " = "
                        + setupValue + ";\n");
    }

    void addInvocationToOnDetach(String variableName, String methodName) {
        onDetachMethodSpec
                .addCode(BINDING_NAME + "."
                        + variableName + "."
                        + methodName + "();\n");
    }

    void addInvocationToOnStart(String variableName, String methodName) {
        onStartMethodSpec
                .addCode(BINDING_NAME + "."
                        + variableName + "."
                        + methodName + "(" + BINDING_NAME + ");\n");
    }

    void addInvocationToOnStop(String variableName, String methodName) {
        onStopMethodSpec
                .addCode(BINDING_NAME + "."
                        + variableName + "."
                        + methodName + "(" + BINDING_NAME + ");\n");
    }

    void addConstructorParameter(TypeMirror bindingMirror) {
        constructorMethodSpec
                .addParameter(TypeName.get(bindingMirror), BINDING_NAME)
                .addStatement("this.$N = $N", BINDING_NAME, BINDING_NAME);
    }

    void addFieldToClass(TypeMirror targetMirror) {
        classTypeSpec
                .addField(TypeName.get(targetMirror), BINDING_NAME, PRIVATE, FINAL);
    }

    String getPackageName() {
        return packageName;
    }

    private void initBuilders() {
        onAttachMethodSpec = methodBuilder(ON_ATTACH_NAME)
                .addModifiers(PUBLIC);

        onDetachMethodSpec = methodBuilder(ON_DETACH_NAME)
                .addModifiers(PUBLIC);

        onStartMethodSpec = methodBuilder(ON_START_NAME)
                .addModifiers(PUBLIC);

        onStopMethodSpec = methodBuilder(ON_STOP_NAME)
                .addModifiers(PUBLIC);

        constructorMethodSpec = constructorBuilder()
                .addModifiers(PUBLIC);

        classTypeSpec = classBuilder(generatedClassName)
                .addSuperinterface(ClassName.get(BINDER_PACKAGE, BINDER_SIMPLE_NAME))
                .addModifiers(PUBLIC, FINAL);
    }
}
