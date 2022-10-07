/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.nad.viewer;

import com.powsybl.nad.viewer.controller.ControllerImport;
import com.powsybl.nad.viewer.controller.ControllerOptions;
import com.powsybl.nad.viewer.controller.ControllerParameters;
import com.powsybl.nad.viewer.view.ImportBar;
import com.powsybl.nad.viewer.view.OptionsPane;
import com.powsybl.nad.viewer.view.ParamPane;
import com.powsybl.nad.viewer.view.diagram.DiagramPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class NetworkAreaDiagramViewer extends Application {

    public final Preferences preferences = Preferences.userNodeForPackage(NetworkAreaDiagramViewer.class);

    public void start(Stage stage) {
        Stage primaryStage = new Stage();

        ParamPane paramPane = new ParamPane();
        OptionsPane optionsPane = new OptionsPane();
        DiagramPane diagramPane = new DiagramPane(optionsPane);
        ControllerOptions cOptions = new ControllerOptions(diagramPane, optionsPane);
        ControllerParameters cParameters = new ControllerParameters(paramPane, diagramPane);

        cParameters.setParamPane();
        cOptions.setParamPane();

        ControllerImport cImport = new ControllerImport(primaryStage, optionsPane, diagramPane, paramPane, preferences);
        cImport.loadNetworkFromPreferences();

        SplitPane splitPane = createSplitPane(optionsPane, diagramPane, paramPane);

        BorderPane mainPane = createMainPane(splitPane, cImport.getImportBar());
        Scene primaryScene = new Scene(mainPane, 1000, 800);

        primaryStage.getIcons().add(new Image("/images/logo.png"));
        primaryStage.setTitle("Area Diagram Viewers");
        primaryStage.setScene(primaryScene);

        primaryStage.show();
    }

    private SplitPane createSplitPane(OptionsPane optionsPane, DiagramPane diagramPane, ParamPane paramPane) {
        SplitPane splitPane = new SplitPane(optionsPane, diagramPane, paramPane);
        splitPane.setDividerPositions(0.2, 0.7, 0.1);
        return splitPane;
    }

    private BorderPane createMainPane(SplitPane splitPane, ImportBar importBar) {
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(splitPane);
        mainPane.setTop(importBar);
        return mainPane;
    }
}
