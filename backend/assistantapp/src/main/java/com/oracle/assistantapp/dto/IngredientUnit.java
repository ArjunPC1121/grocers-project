package com.oracle.assistantapp.dto;

public enum IngredientUnit {
    G("g"), KG("kg"), ML("ml"), L("l"), UNIT("unit");

    private final String label;

    IngredientUnit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
