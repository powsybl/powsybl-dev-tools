/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.OptionsPane;
import com.powsybl.ad.viewer.view.diagram.containers.SelectableSubstation;
import com.powsybl.ad.viewer.view.diagram.containers.SelectableVoltageLevel;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.ad.viewer.controller.ControllerDiagram.getDiagramPane;
import static com.powsybl.ad.viewer.controller.ControllerParameters.getParamPane;
import static com.powsybl.ad.viewer.model.NadCalls.*;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerOptions
{
    private static Stage primaryStage;
    private static OptionsPane optionsPane;
    private static int depthSpinnerValue = 1;

    private static final ObservableList <SelectableSubstation> selectableSubstations = FXCollections.observableArrayList();
    private static final ObservableList <SelectableVoltageLevel> selectableVoltageLevels = FXCollections.observableArrayList();


    public ControllerOptions(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
    }

    public void createOptionsPane()
    {
        optionsPane = new OptionsPane();
        initTreeCellFactory();
    }

    public void setParamPane()
    {
        // Full Network Check
        addListenerOnFullNetworkCheck(optionsPane.getFullNetworkCheck());

        // Depth Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        optionsPane.getDepthSpinner().setValueFactory(valueFactory);
        addListenerOnDepthSpinner(optionsPane.getDepthSpinner());

        // Run loadflow button
        addListenerOnRunFlowButton(optionsPane.getRunLoadFlowButton());

        // Filters field
        addListenerOnFilterField(optionsPane.getFiltersField());
    }

    public static void setNodesList()
    {
        initSubstationsTree();
        optionsPane.displaySubstations();
        // Filters pane
    }

    public static void initSubstationsTree()
    {
        CheckBoxTreeItem rootItem = optionsPane.getFullNetworkCheck();
        rootItem.setValue(networkProperty.get());
        rootItem.setIndependent(true);
        rootItem.setExpanded(true);

        Map <String, SelectableSubstation> mapSubstations = selectableSubstations.stream()
                .collect(Collectors.toMap(SelectableSubstation::getName, Function.identity()));

        Map<String, SelectableVoltageLevel> mapVoltageLevels = selectableVoltageLevels.stream()
                .collect(Collectors.toMap(SelectableVoltageLevel::getName, Function.identity()));

        for (Substation substation : NadCalls.networkProperty.get().getSubstations())
        {
            initVoltageLevelsTree(rootItem, substation, "",
                    true, mapSubstations, mapVoltageLevels);
        }

        TreeView substationsTree = optionsPane.getSubstationTree();

        addListenerOnSelectingSubstationOrVoltageItem(substationsTree);

        if (substationsTree.getRoot() != null)
        {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(rootItem);
        substationsTree.setShowRoot(true);
    }

    private static void initVoltageLevelsTree(TreeItem substationTree,
                                              Substation substation, String filter, boolean emptyFilter,
                                              Map<String, SelectableSubstation> mapSubstations,
                                              Map<String, SelectableVoltageLevel> mapVoltageLevels) {
        boolean firstVoltageLevel = true;
        CheckBoxTreeItem substationItem = null;

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

        for (VoltageLevel voltageLevel : substation.getVoltageLevels())
        {
            voltageLevelOK = showNames && nameContainsFilter && idContainsFilter;

            // If filtered, we don't display
            if (!emptyFilter && !voltageLevelOK)
            {
                continue;
            }
            // If it is the first voltage level, we set it as root and perform other actions
            if (firstVoltageLevel && !hideSubstations)
            {
                substationItem = new CheckBoxTreeItem <> ();
                substationItem.setValue(substation);
                substationItem.setIndependent(true);
                substationItem.setExpanded(true);

                if (mapContainsSubstation && substationIsChecked)
                {
                    substationItem.setSelected(true);
                }
                substationTree.getChildren().add(substationItem);
                addListenerOnSubstationItem(substationItem);
            }
            firstVoltageLevel = false;

            if (!hideSubstations)
            {
                CheckBoxTreeItem <Container <?>> voltageItem = new CheckBoxTreeItem (voltageLevel);

                voltageItem.setIndependent(true);

                if (mapVoltageLevels.containsKey(voltageLevel.getId()) &&
                        true)//mapVoltageLevels.get(voltageLevel.getId()).checkedProperty().get())
                {
                    voltageItem.setSelected(true);
                }

                if (substationItem != null)
                {
                    substationItem.getChildren().add(voltageItem);
                }
                else
                {
                    substationTree.getChildren().add(voltageItem);
                }
                addListenerOnVoltageItem(voltageItem);

            }
        }
    }

    public static void checkvItemTree(String voltageName, boolean selected) {
        TreeView<Container<?>> substationsTree = optionsPane.getSubstationTree();
        substationsTree.getRoot().getChildren().stream().forEach(childS ->
                childS.getChildren().stream().forEach(childV -> {
                    if (childV.getValue().getName().equals(voltageName)) {
                        ((CheckBoxTreeItem) childV).setSelected(selected);
                    }
                })
        );
    }

    public static void checksItemTree(String substationName, boolean selected) {
        TreeView<Container<?>> substationsTree = optionsPane.getSubstationTree();
        substationsTree.getRoot().getChildren().stream().forEach(child -> {
            if (child.getValue().getName().equals(substationName)) {
                ((CheckBoxTreeItem) child).setSelected(selected);
            }
        });
    }

    private static void addListenerOnSubstationItem(CheckBoxTreeItem<Container<?>> substationItem)
    {
        // Handling checking Substations
        ArrayList <String> voltageIds = new ArrayList<>();
        substationItem.selectedProperty().addListener((obs, oldVal, newVal) ->
        {
            if (substationItem.isSelected())
            {
                Util.loggerControllerOptions.debug("Substation \"" + substationItem.getValue().getName() + "\" checked.");

                getParamPane().setDisabledSvgSpinners(false);
                voltageIds.clear();
                for (TreeItem <Container<?>> voltageId : substationItem.getChildren())
                    voltageIds.add(voltageId.getValue().toString());
                NadCalls.loadUniqueSubstation(voltageIds, depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToCheckedTab(
                            substationItem.getValue().getName(),  // Name
                            substationItem.getValue().toString()  // ID
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Util.loggerControllerOptions.debug("Substation \"" + substationItem.getValue().getName() + "\" unchecked.");

                // if event is unchecking the checkbox, close tab
                getDiagramPane().closeTabInCheckedDiagramPane(substationItem);
            }
        });
    }

    private static void addListenerOnVoltageItem(CheckBoxTreeItem<Container<?>> voltageItem)
    {
        // Handling checking Voltages
        voltageItem.selectedProperty().addListener((obs, oldVal, newVal) ->
        {
            if (voltageItem.isSelected()) {
                Util.loggerControllerOptions.debug("Voltage level \"" + voltageItem.getValue().getName() + "\" checked.");

                getParamPane().setDisabledSvgSpinners(false);
                NadCalls.loadSubgraph(voltageItem.getValue().toString(), depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToCheckedTab(
                            voltageItem.getValue().getName(),  // Name
                            voltageItem.getValue().toString()  // ID
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Util.loggerControllerOptions.debug("Voltage level \"" + voltageItem.getValue().getName() + "\" unchecked.");

                // if event is unchecking the checkbox, close tab and
                getDiagramPane().closeTabInCheckedDiagramPane(voltageItem);
            }
        });
    }

    private static void addListenerOnSelectingSubstationOrVoltageItem(TreeView<Container<?>> substationsTree) {
        // Handling what happens when selecting one Substation or one VoltageLevel
        substationsTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            getParamPane().setDisabledSvgSpinners(false);

            Container<?> c = newValue.getValue();
            if (c instanceof Substation) {
                Util.loggerControllerOptions.debug("Substation \"" + c.getName() + "\" selected.");

                ArrayList <String> voltageIds = new ArrayList<>();
                voltageIds.clear();
                for (TreeItem <Container<?>> voltageId : newValue.getChildren())
                    voltageIds.add(voltageId.getValue().toString());
                NadCalls.loadUniqueSubstation(voltageIds, depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToSelectedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (c instanceof VoltageLevel) {
                Util.loggerControllerOptions.info("Voltage \"" + c.getName() + "\" selected.");

                NadCalls.loadSubgraph(c.toString(), depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToSelectedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (c instanceof Network) {
                Util.loggerControllerOptions.info("Full Network \"" + c.getName() + "\" selected.");

                loadNetwork(ControllerImport.getFile().toPath());  // load network
                drawNetwork();  // changes the variable svgWriter
                try {
                    ControllerDiagram.addSvgToSelectedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /*
      Handling the display of names/id in the substations tree
    */
    private static void initTreeCellFactory()
    {
        optionsPane.getSubstationTree().setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            StringConverter <TreeItem<Container<?>>> strConvert = new StringConverter <>()
            {
                @Override
                public String toString(TreeItem<Container<?>> c)
                {
                    if (c.getParent() == null) {
                        return "Full Network";
                    }
                    if (c.getValue() != null) {
                        return c.getValue().getName();
                    } else {
                        return "";
                    }
                }

                @Override
                public TreeItem<Container<?>> fromString(String string) {
                    return null;
                }
            };
            treeCell.setConverter(strConvert);
            return treeCell;
        });
    }

    private static void addListenerOnFullNetworkCheck(CheckBoxTreeItem check)
    {
        check.selectedProperty().addListener((obs, oldVal, newVal) ->
        {
            if (check.isSelected())
            {
                Util.loggerControllerOptions.debug("Network Check Selected OK");

                getParamPane().setDisabledSvgSpinners(false);
                if (ControllerImport.getFile() == null)
                {
                    Util.loggerControllerOptions.debug("No file set. Please import a network first.");
                    return;
                }

                try {
                    loadNetwork(ControllerImport.getFile().toPath());  // load network
                    drawNetwork();  // changes the variable svgWriter

                    ControllerDiagram.addSvgToCheckedTab(
                            "Full Network", "Full Network"
                    );

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Util.loggerControllerOptions.debug("Full Network Check unselected. Cleaning diagram containers..");
                NadCalls.cleanSvgWriter();
                getDiagramPane().resetTabContainers();

                // if event is unchecking the FullNetwork checkbox, close tab
                Util.loggerControllerOptions.debug("Full Network Check unselected. Closing checked subtab..");
                List<Tab> tabList = getDiagramPane().getCheckedDiagramPane().getTabs();
                for (Tab checkedTab : tabList) {
                    if (checkedTab.getText() == "Full Network") {
                        getDiagramPane().getCheckedDiagramPane().getTabs().remove(checkedTab);
                        break;
                    }
                }
            }
        });
    }

    private void addListenerOnDepthSpinner(Spinner <Integer> spinner)
    {
        spinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {

            if (oldValue < newValue)
            {
                // Handle max depth ?
                Util.loggerControllerOptions.debug("Depth Spinner value : " + oldValue + " incremented to : " + newValue);
                depthSpinnerValue++;
            }
            else
            {
                if (depthSpinnerValue == 1)
                {
                    Util.loggerControllerOptions.error("Depth can not be less than 1.");
                    return;
                }
                Util.loggerControllerOptions.debug("Depth Spinner value : " + oldValue + " decremented to : " + newValue);
                depthSpinnerValue--;
            }
        });
    }

    private void addListenerOnRunFlowButton(Button button)
    {
        button.setOnAction(event ->
        {
            if (ControllerImport.getFile() == null)
            {
                Util.loggerControllerOptions.info("No file set. Please import a network first.");
                return;
            }
            NadCalls.runLoadFlow();
            Util.loggerControllerOptions.info("Run Loadflow OK");
        });
    }

    private void addListenerOnFilterField(TextField field)
    {
        field.setOnAction(event ->
        {
            Util.loggerControllerOptions.info("Filter field OK");
        });
    }

    public static void resetOptions()
    {
        while (depthSpinnerValue != 1)
            optionsPane.getDepthSpinner().decrement();
        optionsPane.getFullNetworkCheck().setSelected(false);
        optionsPane.getFiltersField().clear();
    }

    public static void cleanSubstations()
    {
        optionsPane.clearSubstations();
        addListenerOnFullNetworkCheck(optionsPane.getFullNetworkCheck());
    }

    public static OptionsPane getOptionsPane()
    {
        return  optionsPane;
    }
}
