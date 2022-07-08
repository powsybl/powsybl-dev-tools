/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.main;

import com.powsybl.ad.viewer.view.AreaDiagramViewer;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class Main {

    private Main() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Area Diagram Viewer application.");
        Application.launch(AreaDiagramViewer.class);
    }
}
