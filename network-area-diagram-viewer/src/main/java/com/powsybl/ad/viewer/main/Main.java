/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.main;

import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.AreaDiagramViewer;
import javafx.application.Application;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class Main {

    public static void main(String[] args) {
        Util.logger.info("Starting Area Diagram Viewer application.");

        // Launches application
        Application.launch(AreaDiagramViewer.class);

        Util.logger.info("Ending Area Diagram Viewer application.");
    }
}
