package com.defaultapps.easybind;

import com.defaultapps.easybind.bindings.BindLayout;
import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.calls.OnAttach;
import com.defaultapps.easybind.calls.OnDetach;
import com.defaultapps.easybind.calls.OnStart;
import com.defaultapps.easybind.calls.OnStop;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

@AutoService(Processor.class)
public class EasyBindProcessor extends AbstractProcessor {

    private final Messager messager = new Messager();
    private Filer filer;
    private Types typeUtils;

    private Map<String, String> onAttachClassesToMethodsName;
    private Map<String, String> onDetachClassesToMethodsName;
    private Map<String, String> onStartClassesToMethodsName;
    private Map<String, String> onStopClassesToMethodsName;
    private Map<String, String> bindLayoutClassesToFieldsName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> allowedAnnotations = new LinkedHashSet<>();
        allowedAnnotations.add(BindNavigator.class.getCanonicalName());
        allowedAnnotations.add(BindPresenter.class.getCanonicalName());
        allowedAnnotations.add(BindLayout.class.getCanonicalName());
        return allowedAnnotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        onAttachClassesToMethodsName = getAnnotatedMethodsForClasses(OnAttach.class, roundEnvironment);
        onDetachClassesToMethodsName = getAnnotatedMethodsForClasses(OnDetach.class, roundEnvironment);
        onStartClassesToMethodsName = getAnnotatedMethodsForClasses(OnStart.class, roundEnvironment);
        onStopClassesToMethodsName = getAnnotatedMethodsForClasses(OnStop.class, roundEnvironment);
        bindLayoutClassesToFieldsName = getAnnotatedFieldsForClasses(BindLayout.class, roundEnvironment, TypeKind.INT);
        Map<String, CodeGenerator> classesToGenerate = new HashMap<>();

        for (Element presenterBindingElement : roundEnvironment.getElementsAnnotatedWith(BindPresenter.class)) {
            VariableElement variableElement = (VariableElement) presenterBindingElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindPresenter.class.getSimpleName());
                return true;
            }

            TypeElement presenterClassType = verifyClassAnnotation(PresenterClass.class, roundEnvironment, variableElement);

            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            PackageElement packageElement = (PackageElement) variableElement.getEnclosingElement().getEnclosingElement();

            buildCodeGenerator(classesToGenerate, typeElement, packageElement, presenterClassType, variableElement);
        }

        for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(BindNavigator.class)) {
            VariableElement variableElement = (VariableElement) annotatedElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindNavigator.class.getSimpleName());
                return true;
            }

            TypeElement navigatorClassType = verifyClassAnnotation(NavigatorClass.class, roundEnvironment, variableElement);

            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            PackageElement packageElement = (PackageElement) variableElement.getEnclosingElement().getEnclosingElement();

            buildCodeGenerator(classesToGenerate, typeElement, packageElement, navigatorClassType, variableElement);
        }

        for (Element annotatedElement: roundEnvironment.getElementsAnnotatedWith(Layout.class)) {
            TypeElement classTypeElement = (TypeElement) annotatedElement;
            Layout layoutAnnotation = classTypeElement.getAnnotation(Layout.class);
            PackageElement packageElement = (PackageElement) classTypeElement.getEnclosingElement();
            messager.warn(classTypeElement, "Element annotated with @Layout ", classTypeElement.getSimpleName());

            //Traverse all classes until we reach android or java packages
            TypeElement inheritedClassType = classTypeElement;
            while (true) {
                if (isInSystemPackage(inheritedClassType)) {
                    messager.error(inheritedClassType, "Unable to find class with @BindLayout field");
                    break;
                }
                if (bindLayoutClassesToFieldsName.containsKey(inheritedClassType.getQualifiedName().toString())) {
                    messager.warn(inheritedClassType, "Found BaseClass which contains @BindLayout");
                    break;
                }
                inheritedClassType = (TypeElement) typeUtils.asElement(inheritedClassType.getSuperclass());
            }

            buildCodeGenerator(classesToGenerate, classTypeElement, packageElement, inheritedClassType, String.valueOf(layoutAnnotation.id()));
        }

        //Generate code for bindings
        for (CodeGenerator generator: classesToGenerate.values()) {
            TypeSpec typeSpec = generator.generateClass();

            JavaFile javaFile = JavaFile.builder(generator.getPackageName(), typeSpec)
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                throw new RuntimeException("Error writing file");
            }
        }
        return true;
    }

    private boolean isValidField(VariableElement variableElement) {
        FieldValidator fieldValidator = new FieldValidator(variableElement);
        return fieldValidator.isDefault() || fieldValidator.isPublic();
    }

    private Map<String, String> getAnnotatedMethodsForClasses (
            Class<? extends Annotation> annotation,
            RoundEnvironment roundEnvironment) {
        Map<String, String> methodsNameMap = new HashMap<>();

        for (Element element: roundEnvironment.getElementsAnnotatedWith(annotation)) {
            ExecutableElement executableElement = (ExecutableElement) element;

            if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                messager.error(executableElement, "Element annotated with @%s should be PUBLIC.", annotation.getSimpleName());
            }

            TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
            String clsName = typeElement.getQualifiedName().toString();
            if (!methodsNameMap.containsKey(clsName)) {
                messager.warn(typeElement, "Name is: %s", clsName);
                methodsNameMap.put(clsName, executableElement.getSimpleName().toString());
            } else {
                messager.error(executableElement,
                        "Multiple @%s not allowed in a single class",
                        annotation);
            }
            messager.warn(executableElement, "%1s function name: %2s", annotation.getSimpleName(), executableElement.getSimpleName());
        }
        return methodsNameMap;
    }

    private Map<String, String> getAnnotatedFieldsForClasses(
            Class<? extends Annotation> annotation,
            RoundEnvironment roundEnvironment,
            TypeKind fieldType) {
        Map<String, String> fieldsNameMap = new HashMap<>();

        for (Element element: roundEnvironment.getElementsAnnotatedWith(annotation)) {
            VariableElement variableElement = (VariableElement) element;

            if (!variableElement.getModifiers().contains(Modifier.PUBLIC)) {
                messager.error(variableElement, "Element annotated with @%s should be PUBLIC.", annotation.getSimpleName());
            }

            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            String clsName = typeElement.getQualifiedName().toString();
            if (!fieldsNameMap.containsKey(clsName)) {
                messager.warn(typeElement, "Name is: %s", clsName);
                TypeMirror variableType = variableElement.asType();
                if (variableType.getKind() != fieldType) {
                    messager.error(variableElement, "Field must be type of: %s", fieldType.name());
                }
                fieldsNameMap.put(clsName, variableElement.getSimpleName().toString());
            } else {
                messager.error(variableElement,
                        "Multiple @%s not allowed in a single class",
                        annotation);
            }
            messager.warn(variableElement, "%1s function name: %2s", annotation.getSimpleName(), variableElement.getSimpleName());
        }
        return fieldsNameMap;
    }

    private TypeElement verifyClassAnnotation(Class<? extends Annotation> classAnnotation,
                                              RoundEnvironment roundEnvironment,
                                              VariableElement bindingField) {
        TypeElement classType = null;
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(classAnnotation);
        if (elements.size() > 1) messager.error(elements.iterator().next(), "Only single @%s annotation is allowed", classAnnotation.getSimpleName());
        else if (elements.isEmpty()) messager.error(bindingField, "Annotate Base class with @%s annotation", classAnnotation.getSimpleName());
        else {
            classType = (TypeElement) elements.iterator().next();
            messager.warn(classType, "Single annotation detected");
        }
        return classType;
    }

    private void buildCodeGenerator(Map<String, CodeGenerator> classesToGenerate,
                                    TypeElement typeElement,
                                    PackageElement packageElement,
                                    TypeElement componentClassType,
                                    VariableElement variableElement) {
        CodeGenerator codeGenerator;
        if (!classesToGenerate.containsKey(typeElement.toString())) {
            codeGenerator = new CodeGenerator(typeElement, packageElement.getQualifiedName().toString());
            codeGenerator.addConstructorParameter(typeElement.asType());
            codeGenerator.addFieldToClass(typeElement.asType());
        } else {
            codeGenerator = classesToGenerate.get(typeElement.toString());
        }

        String componentClassName = componentClassType.getQualifiedName().toString();
        String componentVarName = variableElement.getSimpleName().toString();

        String onAttachName = onAttachClassesToMethodsName.get(componentClassName);
        if (onAttachName != null) {
            codeGenerator.addInvocationToOnAttach(componentVarName, onAttachName);
        }

        String onDetachName = onDetachClassesToMethodsName.get(componentClassName);
        if (onDetachName != null) {
            codeGenerator.addInvocationToOnDetach(componentVarName, onDetachName);
        }

        String onStartName = onStartClassesToMethodsName.get(componentClassName);
        if (onStartName != null) {
            codeGenerator.addInvocationToOnStart(componentVarName, onStartName);
        }

        String onStopName = onStopClassesToMethodsName.get(componentClassName);
        if (onStopName != null) {
            codeGenerator.addInvocationToOnStop(componentVarName, onStopName);
        }
        classesToGenerate.put(typeElement.toString(), codeGenerator);
    }

    private void buildCodeGenerator(Map<String, CodeGenerator> classesToGenerate,
                                    TypeElement typeElement,
                                    PackageElement packageElement,
                                    TypeElement componentClassType,
                                    String valueToAssign) {
        CodeGenerator codeGenerator;
        if (!classesToGenerate.containsKey(typeElement.toString())) {
            codeGenerator = new CodeGenerator(typeElement, packageElement.getQualifiedName().toString());
            codeGenerator.addConstructorParameter(typeElement.asType());
            codeGenerator.addFieldToClass(typeElement.asType());
        } else {
            codeGenerator = classesToGenerate.get(typeElement.toString());
        }

        String componentClassName = componentClassType.getQualifiedName().toString();

        String layoutIdFieldName = bindLayoutClassesToFieldsName.get(componentClassName);
        if (layoutIdFieldName != null) {
            codeGenerator.addVariableAssignmentOnAttach(layoutIdFieldName, valueToAssign);
        }
        classesToGenerate.put(typeElement.toString(), codeGenerator);
    }

    private boolean isInSystemPackage(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            messager.warn(element, "Reached android. class");
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            messager.warn(element, "Reached java. class");
            return true;
        }
        return false;
    }
}

