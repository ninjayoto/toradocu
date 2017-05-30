package org.toradocu.translator.spec;

import org.toradocu.util.Checks;

public class Precondition extends AbstractSpecification {

  public Precondition(Guard guard) {
    Checks.nonNullParameter(guard, "guard");
    this.guard = guard;
  }
}
