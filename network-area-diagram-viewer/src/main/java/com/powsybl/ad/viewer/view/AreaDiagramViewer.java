/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view;

import com.powsybl.ad.viewer.controller.ControllerImport;
import com.powsybl.ad.viewer.controller.ControllerOptions;
import com.powsybl.ad.viewer.controller.ControllerParameters;
import com.powsybl.ad.viewer.controller.ControllerDiagram;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.net.URL;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class AreaDiagramViewer extends Application
{
    private static Stage primaryStage;
    private static Scene primaryScene;

    private static ControllerDiagram    cDiagram;
    private static ControllerImport     cImport;
    private static ControllerOptions    cOptions;
    private static ControllerParameters cParameters;


    public void init() throws Exception
    {
        super.init();
    }

    public void start(Stage stage) throws Exception
    {
        primaryStage = new Stage();
        cParameters  = new ControllerParameters(primaryStage);
        cOptions  = new ControllerOptions(primaryStage);
        cDiagram  = new ControllerDiagram(primaryStage);
        cImport  = new ControllerImport(primaryStage);

        ParamPane paramPane = createParamPane();

        DiagramPane diagramPane = createDiagramPane();

        OptionsPane optionsPane = createOptionsPane();

        ImportBar importBar = createImportBar();

        SplitPane splitPane = createSplitPane(optionsPane, diagramPane, paramPane);

        BorderPane mainPane = createMainPane(splitPane, importBar);
        primaryScene = new Scene(mainPane, 1000, 800);


        URL imageURL = getClass().getResource("images/logo.png");
        primaryStage.getIcons().add(new Image(imageURL.toExternalForm()));
        primaryStage.setTitle("Area Diagram Viewers");
        primaryStage.setScene(primaryScene);
        primaryStage.show();
    }

    private ParamPane createParamPane()
    {
        cParameters.createParamPane();
        cParameters.setParamPane();

        Util.logger.info("Param pane correctly created.");
        return cParameters.getParamPane();
    }

    private OptionsPane createOptionsPane()
    {
        cOptions.createOptionsPane();
        cOptions.setParamPane();

        Util.logger.info("Options pane correctly created.");
        return cOptions.getOptionsPane();
    }

    private DiagramPane createDiagramPane()
    {
        cDiagram.createDiagramPane();

        Util.logger.info("Diagram pane correctly created.");
        return cDiagram.getDiagramPane();
    }

    private ImportBar createImportBar()
    {
        cImport.createImportBar(3);
        cImport.setImportBar(cOptions);

        Util.logger.info("Import bar correctly created.");
        return cImport.getImportBar();
    }

    private SplitPane createSplitPane(OptionsPane optionsPane, DiagramPane diagramPane, ParamPane paramPane)
    {
        SplitPane splitPane = new SplitPane(optionsPane, diagramPane, paramPane);
        splitPane.setDividerPositions(0.2, 0.7, 0.1);
        return splitPane;
    }

    private BorderPane createMainPane(SplitPane splitPane, ImportBar importBar)
    {
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(splitPane);
        mainPane.setTop(importBar);
        return mainPane;
    }

    public static Stage getPrimaryStage()
    {
        return primaryStage;
    }

}
