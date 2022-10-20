/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.nad.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class NetworkAreaDiagramViewer extends Application {

    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/mainView.fxml")));
        primaryStage.setTitle("Developer viewer for powsybl-network-area-diagram");
        primaryStage.getIcons().add(new Image("/images/logo.png"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
