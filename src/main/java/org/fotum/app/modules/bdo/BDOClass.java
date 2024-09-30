package org.fotum.app.modules.bdo;

import lombok.Getter;

public enum BDOClass {
    WARRIOR("Воин"),
    RANGER("Лучница"),
    SORCERESS("Колдунья"),
    BERSERKER("Варвар"),
    TAMER("Мистик"),
    MUSA("Ронин"),
    MAEHWA("Маэва"),
    VALKYRIE("Валькирия"),
    KUNOICHI("Куноичи"),
    NINJA("Ниндзя"),
    WIZARD("Волшебник"),
    WITCH("Волшебница"),
    DARK_KNIGHT("Темный рыцарь"),
    STRIKER("Страйкер"),
    MYSTIC("Фурия"),
    LAHN("Лан"),
    ARCHER("Лучник"),
    SHAI("Шай"),
    GUARDIAN("Страж"),
    HASHASHIN("Хассашин"),
    NOVA("Нова"),
    SAGE("Мудрец"),
    CORSAIR("Корсар"),
    DRAKANIA("Дракания"),
    SCHOLARIA("Сколярия"),
    MAEGU("Мэгу"),
    WOOSA("Уса"),
    OTHER("Другой класс");

    @Getter
    private final String label;

    BDOClass(String label) {
        this.label = label;
    }
}
