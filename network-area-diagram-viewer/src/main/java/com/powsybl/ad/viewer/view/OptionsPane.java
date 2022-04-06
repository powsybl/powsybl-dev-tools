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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class OptionsPane extends SplitPane
{
    private GridPane voltageLevelToolbar;

    private CheckBox fullNetworkCheck;
    private Spinner <Integer> depthSpinner;

    private Button runLoadFlowButton;
    private TextField filtersField;

    private VBox nodesPane;
    private final TreeView<Container <?>> substationsTree = new TreeView<> ();

    public OptionsPane()
    {
        this.setPadding(new Insets(5, 5, 5, 5));

        createOptionsToolBar();
        createNodePane();
        this.getItems().addAll(voltageLevelToolbar, nodesPane);
        this.setOrientation(Orientation.VERTICAL);
    }

    public void createOptionsToolBar()
    {
        voltageLevelToolbar = new GridPane();

        fullNetworkCheck  = new CheckBox("Full network");

        depthSpinner = new Spinner();
        Label spinnerLabel = new Label("Depth");

        runLoadFlowButton = new Button("Run Loadflow");

        filtersField = new TextField();
        Label filtersLabel = new Label("Filter :");
        filtersLabel.setMinWidth(40);

        voltageLevelToolbar.setHgap(5);
        voltageLevelToolbar.setVgap(5);
        voltageLevelToolbar.setPadding(new Insets(5, 5, 5, 5));
        voltageLevelToolbar.add(fullNetworkCheck, 0, 0, 2, 1);

        voltageLevelToolbar.add(spinnerLabel, 0, 1, 2, 1);
        voltageLevelToolbar.add(depthSpinner, 0, 3, 2, 1);

        voltageLevelToolbar.add(runLoadFlowButton, 0, 6, 3, 1);
        voltageLevelToolbar.add(filtersLabel, 0, 9, 2, 1);
        voltageLevelToolbar.add(filtersField, 1, 10, 1, 1);

        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        voltageLevelToolbar.getColumnConstraints().addAll(c0, c1);
    }

    public void createNodePane()
    {
        nodesPane = new VBox();
        nodesPane.setPadding(new Insets(5, 5, 5, 5));
        nodesPane.setMinHeight(600);
    }

    public void cleanNodes()
    {
        nodesPane.getChildren().clear();
    }

    public void displayNodes(TreeView <?> treeView)
    {
        nodesPane.getChildren().add(treeView);
        nodesPane.setVgrow(treeView, Priority.ALWAYS);
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
