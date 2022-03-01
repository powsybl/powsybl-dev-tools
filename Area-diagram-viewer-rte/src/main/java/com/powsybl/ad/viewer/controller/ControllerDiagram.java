package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.ad.viewer.view.diagram.ContainerDiagramPane;
import javafx.stage.Stage;

public class ControllerDiagram {
    private Stage primaryStage;

    private DiagramPane diagramPane;
    private ContainerDiagramPane infoDiagramPane;

    public ControllerDiagram(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
    }

    public void createDiagramPane()
    {
        diagramPane = new DiagramPane();
    }

    public void setDiagramPane()
    {
    }

    public void createInfoDiagramPane()
    {
        infoDiagramPane = new ContainerDiagramPane();
    }

    public void setInfoDiagramPane()
    {
    }


    public ContainerDiagramPane getInfoDiagramPane()
    {
        return infoDiagramPane;
    }

    public DiagramPane getDiagramPane()
    {
        return diagramPane;
    }
}
