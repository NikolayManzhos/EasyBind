package easybind;

import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;


final class FieldValidator {

  private final Set<Modifier> modifiers;

  FieldValidator(VariableElement variableElement) {
    this.modifiers = variableElement.getModifiers();
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
