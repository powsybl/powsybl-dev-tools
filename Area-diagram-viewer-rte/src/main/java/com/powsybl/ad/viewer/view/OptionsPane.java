package com.powsybl.ad.viewer.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public class OptionsPane extends BorderPane
{
    private GridPane toolBar;

    private CheckBox fullNetworkCheck;
    private Spinner depthSpinner;

    private Button runLoadFlowButton;
    private TextField filtersField;

    private TreeView substationsTree;


    public OptionsPane()
    {
        this.setPadding(new Insets(5, 5, 5, 5));

        createOptionsToolBar();
        createSubstationsTree();

        this.setTop(toolBar);
        this.setCenter(substationsTree);
    }

    public void createOptionsToolBar()
    {
        toolBar = new GridPane();

        fullNetworkCheck  = new CheckBox();
        Label fullNetworkLabel = new Label("Full network");

        depthSpinner = new Spinner();
        Label spinnerLabel = new Label("Depth");

        runLoadFlowButton = new Button("Run Loadflow");

        filtersField = new TextField();
        Label filtersLabel = new Label("Filter :");

        toolBar.setPadding(new Insets(5, 5, 5, 5));
        toolBar.add(fullNetworkCheck, 1, 0);
        toolBar.add(fullNetworkLabel, 0, 0);
        toolBar.add(depthSpinner, 1, 1);
        toolBar.add(spinnerLabel, 0, 1);
        toolBar.add(runLoadFlowButton, 0, 2);
        toolBar.add(filtersField, 1, 3);
        toolBar.add(filtersLabel, 0, 3);
    }

    public void createSubstationsTree()
    {
        substationsTree = new TreeView();
    }

    public CheckBox getFullNetworkCheck()
    {
        return fullNetworkCheck;
    }

    public Spinner getDepthSpinner()
    {
        return depthSpinner;
    }

    public Button getRunLoadFlowButton()
    {
        return runLoadFlowButton;
    }

    public TextField getFiltersField()
    {
        return filtersField;
    }
}
