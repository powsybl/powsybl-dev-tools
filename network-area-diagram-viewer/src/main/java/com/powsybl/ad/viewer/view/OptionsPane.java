/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.view;

import com.powsybl.iidm.network.Container;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class OptionsPane extends SplitPane
{
    private GridPane toolBar;

    private CheckBox fullNetworkCheck;
    private Spinner depthSpinner;

    private Button runLoadFlowButton;
    private TextField filtersField;

    private VBox nodesPane;
    private final TreeView<Container <?>> substationsTree = new TreeView<> ();

    public OptionsPane()
    {
        this.setPadding(new Insets(5, 5, 5, 5));

        createOptionsToolBar();
        createNodePane();
        this.getItems().addAll(toolBar, nodesPane);
        this.setOrientation(Orientation.VERTICAL);
    }

    public void createOptionsToolBar()
    {
        toolBar = new GridPane();

        fullNetworkCheck  = new CheckBox("Full network");

        depthSpinner = new Spinner();
        Label spinnerLabel = new Label("Depth");

        runLoadFlowButton = new Button("Run Loadflow");

        filtersField = new TextField();
        Label filtersLabel = new Label("Filter :");

        toolBar.setPadding(new Insets(5, 5, 5, 5));
        toolBar.add(fullNetworkCheck, 1, 0);
        toolBar.add(depthSpinner, 1, 1);
        toolBar.add(spinnerLabel, 0, 1);
        toolBar.add(runLoadFlowButton, 0, 2);
        toolBar.add(filtersField, 1, 3);
        toolBar.add(filtersLabel, 0, 3);
    }

    public void createNodePane()
    {
        nodesPane = new VBox();
        nodesPane.setPadding(new Insets(5, 5, 5, 5));
    }

    public void cleanNodes(){
        // TO-DO
    }

    public void displayNodes(TreeView <?> treeView)
    {
        cleanNodes(); // each time a new file is imported, previous Nodes should be deleted, otherwise
        // it could produce an exception duplicate children added or just add way too many node
        nodesPane.getChildren().add(treeView);
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

    public TreeView getSubstationTree()
    {
        return substationsTree;
    }
}
