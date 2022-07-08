/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.nad.viewer;

import com.powsybl.nad.viewer.controller.ControllerDiagram;
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

import java.io.File;
import java.net.URL;

import static com.powsybl.nad.viewer.util.Util.CASE_PATH_PROPERTY;
import static com.powsybl.nad.viewer.util.Util.preferences;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class NetworkAreaDiagramViewer extends Application {
    private static Stage primaryStage;
    private static Scene primaryScene;

    private static ControllerDiagram cDiagram;
    private static ControllerImport cImport;
    private static ControllerOptions cOptions;
    private static ControllerParameters cParameters;

    public void start(Stage stage) {
        primaryStage = new Stage();
        cParameters = new ControllerParameters();
        cOptions = new ControllerOptions();
        cDiagram = new ControllerDiagram();
        cImport = new ControllerImport(primaryStage);

        ParamPane paramPane = createParamPane();

        DiagramPane diagramPane = createDiagramPane();

        OptionsPane optionsPane = createOptionsPane();

        ImportBar importBar = createImportBar();

        SplitPane splitPane = createSplitPane(optionsPane, diagramPane, paramPane);

        BorderPane mainPane = createMainPane(splitPane, importBar);
        primaryScene = new Scene(mainPane, 1000, 800);

        URL imageURL = getClass().getResource("/images/logo.png");
        primaryStage.getIcons().add(new Image(imageURL.toExternalForm()));
        primaryStage.setTitle("Area Diagram Viewers");
        primaryStage.setScene(primaryScene);
        loadNetworkFromPreferences();
        primaryStage.show();
    }

    public static boolean loadNetworkFromPreferences() {
        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            ControllerImport.setFile(new File(casePathPropertyValue));
            ControllerImport.loadFile();
            return true;
        }
        return false;
    }

    private ParamPane createParamPane() {
        cParameters.createParamPane();
        cParameters.setParamPane();

        return cParameters.getParamPane();
    }

    private OptionsPane createOptionsPane() {
        cOptions.createOptionsPane();
        cOptions.setParamPane();

        return cOptions.getOptionsPane();
    }

    private DiagramPane createDiagramPane() {
        cDiagram.createDiagramPane();

        return cDiagram.getDiagramPane();
    }

    private ImportBar createImportBar() {
        cImport.createImportBar();
        cImport.setImportBar();

        return cImport.getImportBar();
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
