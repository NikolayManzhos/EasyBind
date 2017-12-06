package com.defaultapps.easybind;

import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.bindings.BindRecyclerAdapter;
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

@AutoService(Processor.class)
public class StaticLauncherProcessor extends AbstractProcessor {

    private final Messager messager = new Messager();
    private Filer filer;

    private Map<String, String> onAttachClassesToMethodsName;
    private Map<String, String> onDetachClassesToMethodsName;
    private Map<String, String> onStartClassesToMethodsName;
    private Map<String, String> onStopClassesToMethodsName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
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
        allowedAnnotations.add(BindRecyclerAdapter.class.getCanonicalName());
        return allowedAnnotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        onAttachClassesToMethodsName = getAnnotatedMethodsForClasses(OnAttach.class, roundEnvironment);
        onDetachClassesToMethodsName = getAnnotatedMethodsForClasses(OnDetach.class, roundEnvironment);
        onStartClassesToMethodsName = getAnnotatedMethodsForClasses(OnStart.class, roundEnvironment);
        onStopClassesToMethodsName = getAnnotatedMethodsForClasses(OnStop.class, roundEnvironment);
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

            generateCode(classesToGenerate, typeElement, packageElement, presenterClassType, variableElement);
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

            generateCode(classesToGenerate, typeElement, packageElement, navigatorClassType, variableElement);
        }

        for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(BindRecyclerAdapter.class)) {
            VariableElement variableElement = (VariableElement) annotatedElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindRecyclerAdapter.class.getSimpleName());
                return true;
            }

            TypeElement recyclerAdapterClassType = verifyClassAnnotation(RecyclerAdapterClass.class, roundEnvironment, variableElement);

            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            PackageElement packageElement = (PackageElement) variableElement.getEnclosingElement().getEnclosingElement();

            generateCode(classesToGenerate, typeElement, packageElement, recyclerAdapterClassType, variableElement);
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

        for (Element onAttachElement: roundEnvironment.getElementsAnnotatedWith(annotation)) {
            ExecutableElement executableElement = (ExecutableElement) onAttachElement;

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

    private void generateCode(Map<String, CodeGenerator> classesToGenerate,
                              TypeElement typeElement,
                              PackageElement packageElement,
                              TypeElement presenterClassType,
                              VariableElement variableElement) {
        CodeGenerator codeGenerator;
        if (!classesToGenerate.containsKey(typeElement.toString())) {
            codeGenerator = new CodeGenerator(typeElement, packageElement.getQualifiedName().toString());
            codeGenerator.addConstructorParameter(typeElement.asType());
            codeGenerator.addFieldToClass(typeElement.asType());
        } else {
            codeGenerator = classesToGenerate.get(typeElement.toString());
        }

        String presenterClassName = presenterClassType.getQualifiedName().toString();
        String presenterVarName = variableElement.getSimpleName().toString();

        String onAttachName = onAttachClassesToMethodsName.get(presenterClassName);
        if (onAttachName != null) {
            codeGenerator.addInvocationToOnAttach(presenterVarName, onAttachName);
        }

        String onDetachName = onDetachClassesToMethodsName.get(presenterClassName);
        if (onDetachName != null) {
            codeGenerator.addInvocationToOnDetach(presenterVarName, onDetachName);
        }

        String onStartName = onStartClassesToMethodsName.get(presenterClassName);
        if (onStartName != null) {
            codeGenerator.addInvocationToOnStart(presenterVarName, onStartName);
        }

        String onStopName = onStopClassesToMethodsName.get(presenterClassName);
        if (onStopName != null) {
            codeGenerator.addInvocationToOnStop(presenterVarName, onStopName);
        }
        classesToGenerate.put(typeElement.toString(), codeGenerator);
    }
}

