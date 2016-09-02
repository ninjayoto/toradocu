package org.toradocu.translator;

/**
 * This class represents a conjunction between propositions.
 */
public enum Conjunction {
  AND,
  OR;

  /**
   * Returns a string representation of this enum: "&&" for AND, "||" for OR.
   *
   * @return a string representation of this conjunction
   */
  @Override
  public String toString() {
    switch (this) {
      case AND:
        return " && ";
      case OR:
        return " || ";
      default:
        return "";
    }
  }
}
