package com.test.datatypes;

import java.util.Objects;


public final class Material {
    private final String materialName;
    private final int materialId;
    private final Kind materialKind;

    public Material(String materialName, int materialId, Kind materialKind) {
        this.materialName = Objects.requireNonNull(materialName, "materialName cannot be null");
        this.materialId = materialId;
        this.materialKind = Objects.requireNonNull(materialKind, "materialKind cannot be null");
    }

    public String getMaterialName() {
        return materialName;
    }

    public int getMaterialId() {
        return materialId;
    }

    public Kind getMaterialKind() {
        return materialKind;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", materialName, materialKind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Material)) return false;
        Material material = (Material) o;
        return materialId == material.materialId &&
                materialName.equals(material.materialName) &&
                materialKind == material.materialKind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialName, materialId, materialKind);
    }
}
