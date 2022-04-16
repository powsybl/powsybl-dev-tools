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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        TreeView substationsList = initSubstationsTree();
        optionsPane.displayNodes(substationsList);
        // Filters pane
    }

    public static TreeView initSubstationsTree()
    {
        TreeItem <Container<?>> rootItem = new TreeItem<> ();
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
        substationsTree.setShowRoot(false);
        return substationsTree;
    }

    private static void initVoltageLevelsTree(TreeItem<Container<?>> substationTree,
                                              Substation substation, String filter, boolean emptyFilter,
                                              Map<String, SelectableSubstation> mapSubstations,
                                              Map<String, SelectableVoltageLevel> mapVoltageLevels) {
        boolean firstVoltageLevel = true;
        CheckBoxTreeItem <Container<?>> substationItem = null;

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

    private static void addListenerOnSubstationItem(CheckBoxTreeItem<Container<?>> substationItem)
    {
        // Handling checking Substations
        ArrayList <String> voltageIds = new ArrayList ();
        substationItem.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), objectTreeModificationEvent ->
        {
            if (substationItem.isSelected())
            {
                Util.loggerControllerOptions.info("Substation \"" + substationItem.getValue().toString() + "\" checked.");
                voltageIds.clear();
                for (TreeItem <Container<?>> voltageId : substationItem.getChildren())
                    voltageIds.add(voltageId.getValue().toString());
                NadCalls.loadUniqueSubstation(voltageIds, depthSpinnerValue);
                //ControllerDiagram.addSvgToCheckedTab();
            } else {
                Util.loggerControllerOptions.info("Substation \"" + substationItem.getValue().toString() + "\" unchecked.");
            }
        });
    }

    private static void addListenerOnVoltageItem(CheckBoxTreeItem<Container<?>> voltageItem)
    {
        // Handling checking Voltages
        voltageItem.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), objectTreeModificationEvent ->
        {
            if (voltageItem.isSelected()) {
                Util.loggerControllerOptions.info("Voltage level \"" + voltageItem.getValue().toString() + "\" checked.");
                NadCalls.loadSubgraph(voltageItem.getValue().toString(), depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToCheckedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Util.loggerControllerOptions.info("Voltage level \"" + voltageItem.getValue().toString() + "\" unchecked.");
            }
        });
    }

    private static void addListenerOnSelectingSubstationOrVoltageItem(TreeView<Container<?>> substationsTree) {
        // Handling what happens when selecting one Substation or one VoltageLevel
        substationsTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            Container<?> c = newValue.getValue();
            if (c instanceof Substation) {
                Util.loggerControllerOptions.info("Substation \"" + getString(c) + "\" selected.");

                ArrayList <String> voltageIds = new ArrayList ();
                voltageIds.clear();
                for (TreeItem <Container<?>> voltageId : newValue.getChildren())
                    voltageIds.add(voltageId.getValue().toString());
                System.out.println(voltageIds);
                NadCalls.loadUniqueSubstation(voltageIds, depthSpinnerValue);
                try {
                    ControllerDiagram.addSvgToSelectedTab();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (c instanceof VoltageLevel) {
                Util.loggerControllerOptions.info("Voltage \"" + getString(c) + "\" selected.");

                NadCalls.loadSubgraph(c.toString(), depthSpinnerValue);
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
        TreeView substationsTree = optionsPane.getSubstationTree();
        substationsTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            StringConverter <TreeItem<Container<?>>> strConvert = new StringConverter <TreeItem<Container<?>>>()
            {
                @Override
                public String toString(TreeItem<Container<?>> c)
                {
                    if (c.getValue() != null) {
                        return getString(c.getValue());
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

    private static String getString(Container<?> value) {

        String cNameOrId = value.getName(); //showNames.isSelected() ? value.getNameOrId() : value.getId();
        /* if (value instanceof Substation && hideVoltageLevels.isSelected())
        {
            long nbVoltageLevels = ((Substation) value).getVoltageLevelStream().count();
            return cNameOrId + " [" + nbVoltageLevels + "]";
        } */
        return cNameOrId;
    }


    private void addListenerOnFullNetworkCheck(CheckBox check)
    {
        check.setOnAction(event ->
        {
            if (check.isSelected())
            {
                Util.loggerControllerOptions.info("Network Check Selected OK");
                if (ControllerImport.getFile() == null)
                {
                    Util.loggerControllerOptions.info("No file set. Please import a network first.");
                    return;
                }

                try {
                    loadNetwork(ControllerImport.getFile().toPath());  // load network
                    drawNetwork();  // changes the variable svgWriter
                    ControllerParameters.getParamPane().getSvgXSpinner().setDisable(false);
                    ControllerParameters.getParamPane().getSvgYSpinner().setDisable(false);

                    ControllerDiagram.addSvgToSelectedTab();
                    ControllerDiagram.addSvgToCheckedTab();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Util.loggerControllerOptions.info("Network Check unselected. Cleaning diagram containers..");
                NadCalls.cleanSvgWriter();
                ControllerDiagram.getDiagramPane().resetTabContainers();
                ControllerParameters.getParamPane().getSvgXSpinner().setDisable(true);
                ControllerParameters.getParamPane().getSvgYSpinner().setDisable(true);
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
                Util.loggerControllerOptions.info("Depth Spinner value :  " + oldValue + " incremented to : " + newValue);
                depthSpinnerValue++;
            }
            else
            {
                if (depthSpinnerValue == 1)
                {
                    Util.loggerControllerOptions.error("Depth can not be less than 1.");
                    return;
                }
                Util.loggerControllerOptions.info("Depth Spinner value :  " + oldValue + " decremented to : " + newValue);
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
        optionsPane.cleanNodes();
    }

    public OptionsPane getOptionsPane()
    {
        return  optionsPane;
    }
}
