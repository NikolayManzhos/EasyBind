package com.defaultapps.easybind;

import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.bindings.BindRecyclerAdapter;
import com.defaultapps.easybind.calls.OnAttach;
import com.defaultapps.easybind.calls.OnDetach;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(Processor.class)
public class StaticLauncherProcessor extends AbstractProcessor {

    private final Messager messager = new Messager();
    private Filer filer;

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
        Map<String, String> onAttachClassesToMethodsName = isSingleMethodAnnotationInClass(OnAttach.class, roundEnvironment);
        Map<String, String> onDetachClassesToMethodsName = isSingleMethodAnnotationInClass(OnDetach.class, roundEnvironment);
        messager.warn(null, "OnAttach classes: " + onAttachClassesToMethodsName.keySet().toString());
        messager.warn(null, "OnDetach classes: " + onDetachClassesToMethodsName.keySet().toString());

        for (Element presenterBindingElement : roundEnvironment.getElementsAnnotatedWith(BindPresenter.class)) {
            VariableElement variableElement = (VariableElement) presenterBindingElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindPresenter.class);
                return true;
            }

            TypeElement presenterClassType = null;
            Set<? extends Element> presenterElements = roundEnvironment.getElementsAnnotatedWith(PresenterClass.class);
            if (presenterElements.size() > 1) messager.error(variableElement, "Only single @PresenterClass annotation is allowed, %d", presenterElements.size());
            else if (presenterElements.isEmpty()) messager.error(variableElement, "Annotate BasePresenter with @PresenterClass annotation");
            else {
                presenterClassType = (TypeElement) presenterElements.iterator().next();
                messager.warn(presenterClassType, "Single annotation detected");
            }

            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            messager.warn(variableElement, "Type of presenter, %s", (variableElement.asType()).toString());
            messager.warn(variableElement, "Type of class, %s", typeElement.toString());
            PackageElement packageElement = (PackageElement) variableElement.getEnclosingElement().getEnclosingElement();


            TypeSpec typeSpec = generateClasses(typeElement,
                    variableElement.getSimpleName().toString(),
                    presenterClassType,
                    onAttachClassesToMethodsName);

            messager.warn(packageElement, packageElement.getQualifiedName().toString());
            messager.warn(presenterClassType, "OnAttach Name From Map, %s",
                    onDetachClassesToMethodsName.get(presenterClassType.getQualifiedName().toString()));
            JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec)
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                throw new RuntimeException("Error writing file");
        }

            //Check if where is only single method annotated with this kind of annotations
//            messager.warn(presenterBindingElement, "Count Attach %d", roundEnvironment.getElementsAnnotatedWith(OnAttach.class).size());
//            messager.warn(presenterBindingElement, "Count Detach %d", roundEnvironment.getElementsAnnotatedWith(OnDetach.class).size());
//            isSingleMethodAnnotationInClass(OnAttach.class, roundEnvironment);
//            isSingleMethodAnnotationInClass(OnDetach.class, roundEnvironment);
        }

        for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(BindNavigator.class)) {
            VariableElement variableElement = (VariableElement) annotatedElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindRecyclerAdapter.class.getName());
                return true;
            }
        }

        for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(BindRecyclerAdapter.class)) {
            VariableElement variableElement = (VariableElement) annotatedElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindRecyclerAdapter.class.getName());
                return true;
            }
        }
        return true;
    }

    private boolean isValidField(VariableElement variableElement) {
        FieldValidator fieldValidator = new FieldValidator(variableElement);
        return fieldValidator.isDefault() || fieldValidator.isPublic();
    }

    private synchronized Map<String, String> isSingleMethodAnnotationInClass(
            Class<? extends Annotation> annotation,
            RoundEnvironment roundEnvironment) {
        Map<String, String> methodsNameMap = new HashMap<>();

        for (Element onAttachElement: roundEnvironment.getElementsAnnotatedWith(annotation)) {
            ExecutableElement executableElement = (ExecutableElement) onAttachElement;

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

    private TypeSpec generateClasses(TypeElement typeElement,
                                     String variableName,
                                     TypeElement presenterClassType,
                                     Map<String, String> methodMap) {
        CodeGenerator codeGenerator = new CodeGenerator(typeElement);
        codeGenerator.createOnAttachMethod(variableName,
                methodMap.get(presenterClassType.getQualifiedName().toString()));
        codeGenerator.createOnDetachMethod();
        codeGenerator.createOnStartmethod();
        codeGenerator.createOnStopMethod();

        return codeGenerator.generateClass(typeElement.asType());
    }
}

