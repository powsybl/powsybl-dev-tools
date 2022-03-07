package com.powsybl.ad.viewer.view;

import com.powsybl.ad.viewer.controller.ControllerImport;
import com.powsybl.ad.viewer.controller.ControllerOptions;
import com.powsybl.ad.viewer.controller.ControllerParameters;
import com.powsybl.ad.viewer.controller.ControllerDiagram;
import com.powsybl.ad.viewer.main.Main;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;

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
        ImportBar importBar = createImportBar();

        OptionsPane optionsPane = createOptionsPane();

        ParamPane paramPane = createParamPane();

        DiagramPane diagramPane = createDiagramPane();

        SplitPane splitPane = createSplitPane(optionsPane, diagramPane, paramPane);

        BorderPane mainPane = createMainPane(splitPane, importBar);
        primaryScene = new Scene(mainPane, 1000, 800);
        primaryStage = new Stage();

        URL imageURL = getClass().getResource("images/logo.png");
        primaryStage.getIcons().add(new Image(imageURL.toExternalForm()));
        primaryStage.setTitle("Area Diagram Viewers");
        primaryStage.setScene(primaryScene);
        primaryStage.show();
    }

    private ParamPane createParamPane()
    {
        ControllerParameters cParam  = new ControllerParameters(primaryStage);
        cParam.createParamPane();
        cParam.setParamPane();

        Util.logger.info("Param pane correctly created.");
        return cParam.getParamPane();
    }

    private OptionsPane createOptionsPane()
    {
        ControllerOptions cOptions  = new ControllerOptions(primaryStage);
        cOptions.createOptionsPane();
        cOptions.setParamPane();

        Util.logger.info("Options pane correctly created.");
        return cOptions.getOptionsPane();
    }

    private DiagramPane createDiagramPane()
    {
        if (cDiagram == null)
            cDiagram  = new ControllerDiagram(primaryStage);

        cDiagram.createDiagramPane();
        cDiagram.setDiagramPane();

        cDiagram.createInfoDiagramPane();
        cDiagram.setInfoDiagramPane();

        Util.logger.info("Diagram pane correctly created.");
        return cDiagram.getDiagramPane();
    }

    private ImportBar createImportBar()
    {
        ControllerImport cImport  = new ControllerImport(primaryStage);
        cImport.createImportBar(3);
        cImport.setImportBar();

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
