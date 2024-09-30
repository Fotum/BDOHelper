package org.fotum.app.modules.bdo;

import lombok.Getter;

public enum BDOChannel {
    BALENOS("Баленос"),
    VALENCIA("Валенсия"),
    SERENDIA("Серендия"),
    CALPHEON("Кальфеон"),
    MEDIAH("Медия"),
    KAMASYLVIA("Камасильвия"),
    UNKNOWN("Неизвестно"),

    // Node wars double channels
    BAL_SER("Баленос/Серендия"),
    VAL_MED("Валенсия/Медия"),
    CAL_KAM("Кальфеон/Камасильвия");

    @Getter
    private final String label;

    BDOChannel(String label) {
        this.label = label;
    }
}
