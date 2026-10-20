package org.fallout2ce;

/**
 * How two values are compared.
 *
 * <p>The original's comparisons work on the values, not on their types: a
 * string is compared as a string, a float as a float and an integer as an
 * integer, and a string against a number compares the text of the number.
 */
enum Cmp {
    EQ, NE, LT, LE, GT, GE;

    boolean ints(int a, int b) {
        switch (this) {
        case EQ: return a == b;
        case NE: return a != b;
        case LT: return a < b;
        case LE: return a <= b;
        case GT: return a > b;
        default: return a >= b;
        }
    }

    boolean floats(float a, float b) {
        switch (this) {
        case EQ: return a == b;
        case NE: return a != b;
        case LT: return a < b;
        case LE: return a <= b;
        case GT: return a > b;
        default: return a >= b;
        }
    }

    /** The symbol the comparison is written with, for the error messages. */
    String symbol() {
        switch (this) {
        case EQ: return "==";
        case NE: return "!=";
        case LT: return "<";
        case LE: return "<=";
        case GT: return ">";
        default: return ">=";
        }
    }

    boolean strings(String a, String b) {
        int order = a.compareTo(b);
        switch (this) {
        case EQ: return order == 0;
        case NE: return order != 0;
        case LT: return order < 0;
        case LE: return order <= 0;
        case GT: return order > 0;
        default: return order >= 0;
        }
    }
}
