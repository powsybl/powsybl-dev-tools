/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.diagram.viewer.nad;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public final class NetworkAreaDiagramView {

    private NetworkAreaDiagramView() {
        // Nothing to do
    }

    public static BorderPane getView() throws IOException {
        // Create the Pane and all Details
        return FXMLLoader.load(Objects.requireNonNull(NetworkAreaDiagramView.class.getResource("/networkAreaView.fxml")));
    }
}
