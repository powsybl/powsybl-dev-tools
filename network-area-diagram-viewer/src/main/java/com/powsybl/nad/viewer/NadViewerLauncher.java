/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer;

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class NadViewerLauncher {

    private NadViewerLauncher() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NadViewerLauncher.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Area Diagram Viewer application.");
        Application.launch(NetworkAreaDiagramViewer.class);
    }
}
