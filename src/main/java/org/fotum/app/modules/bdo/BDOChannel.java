package org.fotum.app.modules.bdo;

import lombok.Getter;

public enum BDOChannel {
    BALENOS("Баленос"),
    VALENCIA("Валенися"),
    SERENDIA("Серендия"),
    CALPHEON("Кальфеон"),
    MEDIAH("Медия"),
    KAMASYLVIA("Камасильвия"),
    UNKNOWN("Неизвестно");

    @Getter
    private final String label;

    BDOChannel(String label) {
        this.label = label;
    }
}
