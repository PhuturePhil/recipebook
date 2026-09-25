package com.recipebook.nutrition;

public enum SizeHint {
    NONE(1.0),
    SMALL(0.7),
    LARGE(1.3),
    HEAPED(1.5);

    private final double factor;

    SizeHint(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
