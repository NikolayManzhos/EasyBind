package com.defaultapps.easybind;

import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.bindings.BindRecyclerAdapter;
import com.defaultapps.easybind.calls.OnAttach;
import com.defaultapps.easybind.calls.OnDetach;
import com.google.auto.service.AutoService;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;


@AutoService(Processor.class)
public class StaticLauncherProcessor extends AbstractProcessor {

    private final Messager messager = new Messager();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager.init(processingEnvironment);
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
        for (Element presenterElement : roundEnvironment.getElementsAnnotatedWith(BindPresenter.class)) {
            VariableElement variableElement = (VariableElement) presenterElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindPresenter.class);
                return true;
            }
            for (Element onAttachElement: roundEnvironment.getElementsAnnotatedWith(OnAttach.class)) {
                ExecutableElement executableElement = (ExecutableElement) onAttachElement;
                messager.warn(executableElement, "onAttach function name: %s", executableElement.getSimpleName());
            }

            for (Element onDetachElement: roundEnvironment.getElementsAnnotatedWith(OnDetach.class)) {
                ExecutableElement executableElement = (ExecutableElement) onDetachElement;
                messager.warn(executableElement, "onDetach function name: %s", executableElement.getSimpleName());
            }

            for (Element presenterClassElement: roundEnvironment.getElementsAnnotatedWith(PresenterClass.class)) {
                if (presenterClassElement.getKind() == ElementKind.CLASS) {
                    TypeElement typeElement = (TypeElement) presenterClassElement;
                    messager.warn(typeElement, "Type element package: %s", typeElement.getQualifiedName().toString() );
                }
            }
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
}

