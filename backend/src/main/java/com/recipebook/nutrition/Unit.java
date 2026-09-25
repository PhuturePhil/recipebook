package com.recipebook.nutrition;

public record Unit(String name, UnitKind kind, double baseFactor) {

    public static Unit other(String name) {
        return new Unit(name, UnitKind.OTHER, 1.0);
    }

    public boolean isMass() {
        return kind == UnitKind.MASS;
    }

    public boolean isVolume() {
        return kind == UnitKind.VOLUME;
    }
}
