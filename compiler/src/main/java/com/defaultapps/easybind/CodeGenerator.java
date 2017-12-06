package com.defaultapps.easybind;

import com.deafaultapps.easybind.EasyBinder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.ClassName.get;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;


final class CodeGenerator {

    private static final String CLASS_SUFFIX = "Binder";

    private final String annotatedClassName;
    private final String generatedClassName;

    private MethodSpec onAttachMethodSpec;
    private MethodSpec onDetachMethodSpec;
    private MethodSpec onStartMethodSpec;
    private MethodSpec onStopMethodSpec;

    private final static String BINDING_NAME = "target";

    CodeGenerator(TypeElement annotatedClass) {
        this.annotatedClassName = annotatedClass.getSimpleName().toString();
        this.generatedClassName = annotatedClassName + CLASS_SUFFIX;
    }

    TypeSpec generateClass(TypeMirror targetMirror) {
        TypeSpec.Builder builder = classBuilder(generatedClassName)
                .addSuperinterface(EasyBinder.class)
                .addModifiers(PUBLIC, FINAL)
                .addField(TypeName.get(targetMirror), BINDING_NAME, PRIVATE, FINAL)
                .addMethod(createConstructor(targetMirror))
                .addMethod(onAttachMethodSpec)
                .addMethod(onDetachMethodSpec)
                .addMethod(onStartMethodSpec)
                .addMethod(onStopMethodSpec);
        return builder.build();
    }

    public void createOnAttachMethod(String variableName, String methodName) {
        onAttachMethodSpec = methodBuilder("onAttach")
                .addModifiers(PUBLIC)
                .addCode(BINDING_NAME + "."
                        + variableName + "."
                        + methodName + "(" + BINDING_NAME + ");\n")
                .build();
    }

    public void createOnDetachMethod() {
        onDetachMethodSpec = methodBuilder("onDetach")
                .addModifiers(PUBLIC)
                .build();
    }

    public void createOnStartmethod() {
        onStartMethodSpec = methodBuilder("onStart")
                .addModifiers(PUBLIC)
                .build();
    }

    public void createOnStopMethod() {
        onStopMethodSpec = methodBuilder("onStop")
                .addModifiers(PUBLIC)
                .build();
    }

    private MethodSpec createConstructor(TypeMirror presenterMirror) {
        return constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(TypeName.get(presenterMirror), BINDING_NAME)
                .addStatement("this.$N = $N", BINDING_NAME, BINDING_NAME)
                .build();
    }
}
