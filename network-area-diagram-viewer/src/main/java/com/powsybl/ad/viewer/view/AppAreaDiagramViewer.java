/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class AppAreaDiagramViewer extends Application {
    // To Remove : https://docs.oracle.com/javase/8/javafx/api/javafx/application/Application.html
    // This function is the core of the IHM, it starts the application by calling Launch() in main
    public void start(Stage primaryStage) throws Exception {
        BorderPane mainPane = new BorderPane();

        Scene scene = new Scene(mainPane, 1000, 800);
        primaryStage.setTitle("Substation diagram viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
