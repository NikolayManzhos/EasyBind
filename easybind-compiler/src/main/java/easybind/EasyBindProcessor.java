package easybind;


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

import easybind.bindings.BindLayout;
import easybind.bindings.BindName;
import easybind.bindings.BindNavigator;
import easybind.bindings.BindPresenter;
import easybind.calls.OnAttach;
import easybind.calls.OnDetach;
import easybind.calls.OnDispose;
import easybind.calls.OnStart;
import easybind.calls.OnStop;

@AutoService(Processor.class)
public class EasyBindProcessor extends AbstractProcessor {

    private static final boolean DEBUG = false;

    private final Messager messager = new Messager();
    private Filer filer;
    private Types typeUtils;

    private Map<String, String> onAttachClassesToMethodsName;
    private Map<String, String> onDetachClassesToMethodsName;
    private Map<String, String> onStartClassesToMethodsName;
    private Map<String, String> onStopClassesToMethodsName;
    private Map<String, String> onDisposeClassesToMethodsName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
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
        onAttachClassesToMethodsName = getAnnotatedMethodsForClasses(OnAttach.class, roundEnvironment, true);
        onDetachClassesToMethodsName = getAnnotatedMethodsForClasses(OnDetach.class, roundEnvironment, true);
        onStartClassesToMethodsName = getAnnotatedMethodsForClasses(OnStart.class, roundEnvironment, false);
        onStopClassesToMethodsName = getAnnotatedMethodsForClasses(OnStop.class, roundEnvironment, false);
        onDisposeClassesToMethodsName = getAnnotatedMethodsForClasses(OnDispose.class, roundEnvironment, false);
        Map<String, String> bindLayoutClassesToFieldsName = getAnnotatedFieldsForClasses(BindLayout.class, roundEnvironment, TypeKind.INT);
        Map<String, String> bindNameClassesToFieldsName = getAnnotatedFieldsForClasses(BindName.class, roundEnvironment, TypeKind.DECLARED);
        Map<String, CodeGenerator> classesToGenerate = new HashMap<>();

        for (Element presenterBindingElement : roundEnvironment.getElementsAnnotatedWith(BindPresenter.class)) {
            VariableElement variableElement = (VariableElement) presenterBindingElement;
            if (!isValidField(variableElement)) {
                messager.error(variableElement,
                        "Field annotated with %s must have DEFAULT or PUBLIC access modifier, current is: ",
                        BindPresenter.class.getSimpleName());
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
            if (DEBUG) messager.warn(classTypeElement, "Element annotated with @Layout ", classTypeElement.getSimpleName());

            //Traverse all classes until we reach android or java packages
            TypeElement inheritedClassTypeLayout = searchForTypeOfClass(classTypeElement, bindLayoutClassesToFieldsName, true);
            TypeElement inheritedClassTypeName = searchForTypeOfClass(classTypeElement, bindNameClassesToFieldsName, false);

            buildCodeGenerator(classesToGenerate,
                    classTypeElement,
                    packageElement,
                    inheritedClassTypeLayout,
                    String.valueOf(layoutAnnotation.id()),
                    bindLayoutClassesToFieldsName);

            //@BindName field not found finish generation
            if (inheritedClassTypeName == null) {
                continue;
            }
            String screenName = "\"" + layoutAnnotation.name() + "\"";
            buildCodeGenerator(classesToGenerate,
                    classTypeElement,
                    packageElement,
                    inheritedClassTypeName,
                    screenName,
                    bindNameClassesToFieldsName);
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
            RoundEnvironment roundEnvironment,
            boolean hasSingleParam) {
        Map<String, String> methodsNameMap = new HashMap<>();

        for (Element element: roundEnvironment.getElementsAnnotatedWith(annotation)) {
            ExecutableElement executableElement = (ExecutableElement) element;

            if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                messager.error(executableElement, "Element annotated with @%s should be PUBLIC.", annotation.getSimpleName());
            }

            int paramsCount = executableElement.getParameters().size();
            if (hasSingleParam) {
                if (paramsCount > 1) {
                    messager.error(executableElement, "This function must have single parameter.");
                }
            } else {
                if (paramsCount != 0) {
                    messager.error(executableElement, "This function must have zero parameters.");
                }
            }

            TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
            String clsName = typeElement.getQualifiedName().toString();
            if (!methodsNameMap.containsKey(clsName)) {
                if (DEBUG) messager.warn(typeElement, "Name is: %s", clsName);
                methodsNameMap.put(clsName, executableElement.getSimpleName().toString());
            } else {
                messager.error(executableElement,
                        "Multiple @%s not allowed in a single class",
                        annotation);
            }
            if (DEBUG) messager.warn(executableElement, "%1s function name: %2s", annotation.getSimpleName(), executableElement.getSimpleName());
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
                if (DEBUG) messager.warn(typeElement, "Name is: %s", clsName);
                TypeMirror variableType = variableElement.asType();
                if (variableType.getKind() != fieldType) {
                    messager.error(variableElement, "%1s, Field must be type of: %2s", variableType.getKind().toString(), fieldType.name());
                }
                fieldsNameMap.put(clsName, variableElement.getSimpleName().toString());
            } else {
                messager.error(variableElement,
                        "Multiple @%s not allowed in a single class",
                        annotation.getSimpleName());
            }
            if (DEBUG) messager.warn(variableElement, "%1s function name: %2s", annotation.getSimpleName(), variableElement.getSimpleName());
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
            if (DEBUG) messager.warn(classType, "Single annotation detected");
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

        String onDisposeName = onDisposeClassesToMethodsName.get(componentClassName);
        if (onDisposeName != null) {
            codeGenerator.addInvocationToOnDispose(componentVarName, onDisposeName);
        }

        classesToGenerate.put(typeElement.toString(), codeGenerator);
    }

    private void buildCodeGenerator(Map<String, CodeGenerator> classesToGenerate,
                                    TypeElement typeElement,
                                    PackageElement packageElement,
                                    TypeElement componentClassType,
                                    String valueToAssign,
                                    Map<String, String> bindingMap) {
        CodeGenerator codeGenerator;
        if (!classesToGenerate.containsKey(typeElement.toString())) {
            codeGenerator = new CodeGenerator(typeElement, packageElement.getQualifiedName().toString());
            codeGenerator.addConstructorParameter(typeElement.asType());
            codeGenerator.addFieldToClass(typeElement.asType());
        } else {
            codeGenerator = classesToGenerate.get(typeElement.toString());
        }

        String componentClassName = componentClassType.getQualifiedName().toString();

        String layoutIdFieldName = bindingMap.get(componentClassName);
        if (layoutIdFieldName != null) {
            codeGenerator.addVariableAssignmentToConstructor(layoutIdFieldName, valueToAssign);
        }
        classesToGenerate.put(typeElement.toString(), codeGenerator);
    }

    private boolean isInSystemPackage(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            if (DEBUG) messager.warn(element, "Reached android. class");
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            if (DEBUG) messager.warn(element, "Reached java. class");
            return true;
        }
        return false;
    }

    private TypeElement searchForTypeOfClass(TypeElement typeElement,
                                             Map<String, String> bindingMap,
                                             boolean isStrict) {
        while (true) {
            if (isInSystemPackage(typeElement)) {
                if (isStrict) {
                    messager.error(typeElement, "Unable to find class with @BindLayout field");
                } else {
                    return null;
                }
            }
            if (bindingMap.containsKey(typeElement.getQualifiedName().toString())) {
                if (DEBUG) messager.warn(typeElement, "Found BaseClass which contains @BindLayout");
                break;
            }
            typeElement = (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
        }
        return typeElement;
    }
}

