package com.defaultapps.easybind;

import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;


final class FieldValidator {

  private final VariableElement annotatedField;
  private final Set<Modifier> modifiers;

  FieldValidator(VariableElement variableElement) {
    this.annotatedField = variableElement;
    this.modifiers = annotatedField.getModifiers();
  }

  boolean isPublic() {
    return modifiers.contains(Modifier.PUBLIC);
  }

  boolean isDefault() {
    return modifiers.size() == 0;
  }

  boolean isAbstract() {
    return modifiers.contains(ABSTRACT);
  }
}
