/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.view;

import com.powsybl.nad.viewer.util.Util;
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
public class OptionsPane extends SplitPane {
    private GridPane voltageLevelToolbar;

    private CheckBoxTreeItem fullNetworkCheck;
    private Spinner<Integer> depthSpinner;

    private Button runLoadFlowButton;
    private TextField filtersField;

    private VBox substationsPane;
    private final TreeView<Container<?>> substationsTree = new TreeView<>();

    public OptionsPane() {
        this.setPadding(new Insets(5, 5, 5, 5));

        createOptionsToolBar();
        createSubstationPane();
        this.getItems().addAll(voltageLevelToolbar, substationsPane);
        this.setOrientation(Orientation.VERTICAL);
    }

    public void createOptionsToolBar() {
        voltageLevelToolbar = new GridPane();

        fullNetworkCheck = new CheckBoxTreeItem("Full network");

        depthSpinner = new Spinner();
        Label spinnerLabel = new Label("Depth");

        runLoadFlowButton = new Button("Run Loadflow");

        filtersField = new TextField();
        Label filtersLabel = new Label("Filter :");
        filtersLabel.setMinWidth(40);

        voltageLevelToolbar.setHgap(5);
        voltageLevelToolbar.setVgap(5);
        voltageLevelToolbar.setPadding(new Insets(5, 5, 5, 5));

        voltageLevelToolbar.add(spinnerLabel, 0, 0, 2, 1);
        voltageLevelToolbar.add(depthSpinner, 0, 2, 2, 1);

        voltageLevelToolbar.add(runLoadFlowButton, 0, 4, 3, 1);
        voltageLevelToolbar.add(filtersLabel, 0, 7, 2, 1);
        voltageLevelToolbar.add(filtersField, 1, 8, 1, 1);

        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        voltageLevelToolbar.getColumnConstraints().addAll(c0, c1);
    }

    public void createSubstationPane() {
        substationsPane = new VBox();
        substationsPane.setPadding(new Insets(5, 5, 5, 5));
        substationsPane.setMinHeight(600);
    }

    public void clearSubstations() {
        substationsPane.getChildren().clear();
        substationsTree.setRoot(null);

        fullNetworkCheck.getChildren().clear();
    }

    public void displaySubstations() {
        substationsPane.getChildren().add(substationsTree);
        substationsPane.setVgrow(substationsTree, Priority.ALWAYS);
    }

    public CheckBoxTreeItem getFullNetworkCheck() {
        return fullNetworkCheck;
    }

    public Spinner getDepthSpinner() {
        return depthSpinner;
    }

    public Button getRunLoadFlowButton() {
        return runLoadFlowButton;
    }

    public TextField getFiltersField() {
        return filtersField;
    }

    public TreeView getSubstationTree() {
        return substationsTree;
    }
}
