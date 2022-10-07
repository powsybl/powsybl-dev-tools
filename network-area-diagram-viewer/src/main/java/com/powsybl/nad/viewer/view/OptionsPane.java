/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.view;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.nad.viewer.model.NadCalls;
import com.powsybl.iidm.network.Container;
import com.powsybl.nad.viewer.view.diagram.DiagramPane;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class OptionsPane extends SplitPane {
    private GridPane voltageLevelToolbar;

    private CheckBoxTreeItem<Container<?>> fullNetworkCheck = new CheckBoxTreeItem<>();
    private Spinner<Integer> depthSpinner;

    private Button runLoadFlowButton;
    private TextField filtersField;

    private VBox substationsPane;
    private final TreeView<Container<?>> substationsTree = new TreeView<>();

    private int depthSpinnerValue = 1;

    public OptionsPane() {
        this.setPadding(new Insets(5, 5, 5, 5));

        createOptionsToolBar();
        createSubstationPane();
        this.getItems().addAll(voltageLevelToolbar, substationsPane);
        this.setOrientation(Orientation.VERTICAL);
    }

    public void createOptionsToolBar() {
        voltageLevelToolbar = new GridPane();

        depthSpinner = new Spinner<>();
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
        VBox.setVgrow(substationsTree, Priority.ALWAYS);
    }

    public void checkvItemTree(String voltageName, boolean selected) {
        substationsTree.getRoot().getChildren().stream().flatMap(childS -> childS.getChildren().stream())
                .filter(childV -> childV.getValue().getName().equals(voltageName))
                .findFirst().ifPresent(childV -> ((CheckBoxTreeItem<?>) childV).setSelected(selected));
    }

    public void checksItemTree(String substationName, boolean selected) {
        substationsTree.getRoot().getChildren().stream()
                .filter(child -> child.getValue().getName().equals(substationName))
                .forEach(child -> ((CheckBoxTreeItem<?>) child).setSelected(selected));
    }

    public CheckBoxTreeItem<Container<?>> getFullNetworkCheck() {
        return fullNetworkCheck;
    }

    public Spinner<Integer> getDepthSpinner() {
        return depthSpinner;
    }

    public Button getRunLoadFlowButton() {
        return runLoadFlowButton;
    }

    public TextField getFiltersField() {
        return filtersField;
    }

    public TreeView<Container<?>> getSubstationTree() {
        return substationsTree;
    }

    public void resetOptions() {
        fullNetworkCheck.setSelected(false);
        filtersField.clear();
    }

    public void setNodesList(Network network, DiagramPane diagramPane) {
        clearSubstations();
        initSubstationsTree(network, diagramPane);
        displaySubstations();
    }

    public void initSubstationsTree(Network network, DiagramPane diagramPane) {
        fullNetworkCheck = new CheckBoxTreeItem<>(network);
        fullNetworkCheck.setValue(network);
        fullNetworkCheck.setIndependent(true);
        fullNetworkCheck.setExpanded(true);

        for (Substation substation : network.getSubstations()) {
            initVoltageLevelsTree(fullNetworkCheck, substation, "", true, diagramPane);
        }

        addListenerOnSelectingSubstationOrVoltageItem(substationsTree, diagramPane);

        if (substationsTree.getRoot() != null) {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(fullNetworkCheck);
        substationsTree.setShowRoot(true);
    }

    private void initVoltageLevelsTree(TreeItem<Container<?>> substationTree, Substation substation, String filter, boolean emptyFilter,
                                       DiagramPane diagramPane) {
        boolean firstVoltageLevel = true;

        // Replace by check box showName.isSelected
        boolean showNames = true;
        // v.getName().contains(filter)
        boolean nameContainsFilter = true;
        // v.getId().contains(filter)
        boolean idContainsFilter = true;
        // !hideSubstations.isSelected()
        boolean hideSubstations = false;

        boolean voltageLevelOK;

        // mapSubstations.containsKey(substation.getId()) ?? don't understand the purpose of this condition
        boolean mapContainsSubstation = true;
        // mapSubstations.get(substation.getId()).checkedProperty().get()
        boolean substationIsChecked = false;

        for (VoltageLevel voltageLevel : substation.getVoltageLevels()) {
            voltageLevelOK = showNames && nameContainsFilter && idContainsFilter;

            // If filtered, we don't display
            if (!emptyFilter && !voltageLevelOK) {
                continue;
            }
            // If it is the first voltage level, we set it as root and perform other actions
            CheckBoxTreeItem<Container<?>> substationItem = new CheckBoxTreeItem<>();
            if (firstVoltageLevel && !hideSubstations) {
                substationItem.setValue(substation);
                substationItem.setIndependent(true);
                substationItem.setExpanded(true);

                if (mapContainsSubstation && substationIsChecked) {
                    substationItem.setSelected(true);
                }
                substationTree.getChildren().add(substationItem);
                addListenerOnSubstationItem(substationItem, diagramPane);
            }
            firstVoltageLevel = false;

            if (!hideSubstations) {
                CheckBoxTreeItem<Container<?>> voltageItem = new CheckBoxTreeItem<>(voltageLevel);

                voltageItem.setIndependent(true);

                if (substationItem.getValue() != null) {
                    substationItem.getChildren().add(voltageItem);
                } else {
                    substationTree.getChildren().add(voltageItem);
                }
                addListenerOnVoltageItem(voltageItem, diagramPane);
            }
        }
    }

    private void addListenerOnSubstationItem(CheckBoxTreeItem<Container<?>> substationItem, DiagramPane diagramPane) {
        // Handling checking Substations
        ArrayList<String> voltageIds = new ArrayList<>();
        substationItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (substationItem.isSelected()) {
                voltageIds.clear();
                for (TreeItem<Container<?>> voltageId : substationItem.getChildren()) {
                    voltageIds.add(voltageId.getValue().toString());
                }
                NadCalls.drawUniqueSubstation(voltageIds, depthSpinnerValue);
                try {
                    diagramPane.addSvgToCheckedTab(substationItem.getValue().getName(),  // Name
                            substationItem.getValue().toString(),  // ID
                            voltageIds,  // List of voltageLevelIds
                            depthSpinnerValue,  // Depth
                            diagramPane.getCheckedDiagramPane().getTabs().isEmpty() ? 0 : diagramPane.getCheckedDiagramPane().getTabs().size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // if event is unchecking the checkbox, close tab
                diagramPane.closeTabInCheckedDiagramPane(substationItem);
            }
        });
    }

    private void addListenerOnVoltageItem(CheckBoxTreeItem<Container<?>> voltageItem, DiagramPane diagramPane) {
        // Handling checking Voltages
        voltageItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (voltageItem.isSelected()) {
                NadCalls.drawSubgraph(voltageItem.getValue().toString(), depthSpinnerValue);
                try {
                    diagramPane.addSvgToCheckedTab(voltageItem.getValue().getName(),  // Name
                            voltageItem.getValue().toString(),  // Voltage Level (= subgraph) ID
                            voltageItem.getValue().toString(),  // Voltage Level (= subgraph) ID
                            depthSpinnerValue,  // Depth
                            diagramPane.getCheckedDiagramPane().getTabs().isEmpty() ? 0 : diagramPane.getCheckedDiagramPane().getTabs().size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // if event is unchecking the checkbox, close tab
                diagramPane.closeTabInCheckedDiagramPane(voltageItem);
            }
        });
    }

    private void addListenerOnSelectingSubstationOrVoltageItem(TreeView<Container<?>> substationsTree, DiagramPane diagramPane) {
        // Handling what happens when selecting one Substation or one VoltageLevel
        substationsTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            Container<?> c = newValue.getValue();
            if (c instanceof Substation) {
                ArrayList<String> voltageIds = new ArrayList<>();
                for (TreeItem<Container<?>> voltageId : newValue.getChildren()) {
                    voltageIds.add(voltageId.getValue().toString());
                }
                NadCalls.drawUniqueSubstation(voltageIds, depthSpinnerValue);
                try {
                    diagramPane.addSvgToSelectedTab(voltageIds, depthSpinnerValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (c instanceof VoltageLevel) {
                NadCalls.drawSubgraph(c.toString(), depthSpinnerValue);
                try {
                    diagramPane.addSvgToSelectedTab(c.toString(), depthSpinnerValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (c instanceof Network) {
                NadCalls.drawNetwork((Network) c);  // changes the variable svgWriter
                try {
                    diagramPane.addSvgToSelectedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public int getDepthSpinnerValue() {
        return depthSpinnerValue;
    }

    public void setDepthSpinnerValue(int depthSpinnerValue) {
        this.depthSpinnerValue = depthSpinnerValue;
    }
}
