/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ContainerResult {
    private final StringProperty svgContent = new SimpleStringProperty();

    private final StringProperty metadataContent = new SimpleStringProperty();

    private final StringProperty jsonContent = new SimpleStringProperty();

    public void clean() {
        svgContent.set("");
        metadataContent.set("");
        jsonContent.set("");
    }

    public StringProperty svgContentProperty() {
        return svgContent;
    }

    public StringProperty metadataContentProperty() {
        return metadataContent;
    }

    public StringProperty jsonContentProperty() {
        return jsonContent;
    }

    public void setValue(ContainerResult value) {
        svgContent.setValue(value.svgContent.getValue());
        metadataContent.setValue(value.metadataContent.getValue());
        jsonContent.setValue(value.jsonContent.getValue());
    }
}
