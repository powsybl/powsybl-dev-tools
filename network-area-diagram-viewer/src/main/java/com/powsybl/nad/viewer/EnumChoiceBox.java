/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer;

import javafx.beans.NamedArg;
import javafx.scene.control.ChoiceBox;

// From https://stackoverflow.com/questions/31325062/pass-enum-in-fxml
// Author https://stackoverflow.com/users/2189127/james-d
public class EnumChoiceBox<E extends Enum<E>> extends ChoiceBox<E> {
    public EnumChoiceBox(@NamedArg("enumType") String enumType, @NamedArg("initialValue") String initialValue) throws ClassNotFoundException {
        Class<E> enumClass = (Class<E>) Class.forName(enumType);
        E[] values = enumClass.getEnumConstants();
        getItems().setAll(values);
        int i;
        for (i = 0; i < values.length; i++) {
            if (values[i].name().equals(initialValue)) {
                break;
            }
        }
        getSelectionModel().select(i == values.length ? 0 : i);
    }
}
