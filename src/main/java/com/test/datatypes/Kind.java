package com.test.datatypes;

public enum Kind {
    MEDICAL("Équipement médical"),
    STAFF_EPI("Équipement du personnel"),
    CLIENT_EPI("Équipement du client"),
    MAINTENANCE("Équipement d'entretien"),
    OTHER("Autre");

    private final String description;

    Kind(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static Kind fromDescription(String desc) {
        for (Kind kind : Kind.values()) {
            if (kind.description.equals(desc)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown description: " + desc);
    }
}
