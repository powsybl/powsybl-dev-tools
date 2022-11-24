/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.viewer;

/**
 *@author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class ContainerDiagramResult {

    private final String svgData;
    private final String metadataData;
    private final String jsonData;

    ContainerDiagramResult(String svgData, String metadataData, String jsonData) {
        this.svgData = svgData;
        this.metadataData = metadataData;
        this.jsonData = jsonData;
    }

    String getSvgData() {
        return svgData;
    }

    String getMetadataData() {
        return metadataData;
    }

    String getJsonData() {
        return jsonData;
    }
}
