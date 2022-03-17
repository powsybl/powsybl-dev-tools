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
import static com.powsybl.ad.viewer.view.diagram.DiagramPane.addSVG;
import static com.powsybl.ad.viewer.view.diagram.DiagramPane.cleanSVG;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerOptions
{
    private static Stage primaryStage;
    private static OptionsPane optionsPane;

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
        addListenerOnDepthSpinner(optionsPane.getDepthSpinner(), primaryStage);

        // Run loadflow button
        addListenerOnRunFlowButton(optionsPane.getRunLoadFlowButton(), primaryStage);

        // Filters field
        addListenerOnFilterField(optionsPane.getFiltersField(), primaryStage);
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
        if (optionsPane.getSubstationTree().getRoot() != null)
        {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(rootItem);
        substationsTree.setShowRoot(false);
        return substationsTree;
    }

    private static void initVoltageLevelsTree(TreeItem<Container<?>> rootItem,
                                              Substation substation, String filter, boolean emptyFilter,
                                              Map<String, SelectableSubstation> mapSubstations,
                                              Map<String, SelectableVoltageLevel> mapVoltageLevels) {
        boolean firstVoltageLevel = true;
        CheckBoxTreeItem <Container<?>> sItem = null;

        // Replace by check box showName.isSelected
        boolean showNames = true;
        // v.getName().contains(filter
        boolean nameContainsFilter = true;
        // v.getId().contains(filter)
        boolean idContainsFilter = true;
        // !hideSubstations.isSelected()
        boolean hideSubstations = false;

        boolean voltageLevelOK;

        // mapSubstations.containsKey(substation.getId()) ?? don't understand the purpose of this condition
        boolean mapContainsSubstation = true;
        // mapSubstations.get(substation.getId()).checkedProperty().get() if parent is selected ,select child
        boolean substationIsChecked = true;

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
                sItem = new CheckBoxTreeItem <> ();
                sItem.setValue(substation);
                sItem.setIndependent(true);
                sItem.setExpanded(true);

                if (mapContainsSubstation && substationIsChecked)
                {
                    sItem.setSelected(true);
                }
                rootItem.getChildren().add(sItem);
                sItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                        // checkSubstation(s, newVal)
                        Util.loggerControllerOptions.info("SELECTED ITEM")
                );
            }

            rootItem.getChildren().add(sItem);
            /*
            sItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                    checkSubstation(substation, newVal)
            ); */

            firstVoltageLevel = false;

            if (!hideSubstations)
            {
                CheckBoxTreeItem<Container<?>> vItem = new CheckBoxTreeItem<>(voltageLevel);

                vItem.setIndependent(true);

                if (mapVoltageLevels.containsKey(voltageLevel.getId()) &&
                        true)//mapVoltageLevels.get(voltageLevel.getId()).checkedProperty().get())
                {
                    vItem.setSelected(true);
                }

                if (sItem != null)
                {
                    sItem.getChildren().add(vItem);
                }
                else
                {
                    rootItem.getChildren().add(vItem);
                }

                vItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                        Util.loggerControllerOptions.info("VOLTAGE LEVEL SELECTED")
                        );// checkVoltageLevel(voltageLevel, newVal));
            }
        }
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

                try {
                    loadNetwork(ControllerImport.getFile().toPath());  // load network
                    cleanSVG();  // clean svgWriter
                    drawNetwork();  // changes the variable svgWriter
                    addSVG(getSvgWriter());  // calls addSVG which actually displays the svg
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
                Util.loggerControllerOptions.info("Network Check Unselected OK");
        });
    }

    private void addListenerOnDepthSpinner(Spinner spinner, Stage primaryStage)
    {
        spinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            Util.loggerControllerOptions.info("Depth Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnRunFlowButton(Button button, Stage primaryStage)
    {
        button.setOnAction(event ->
        {
            NadCalls.runLoadFlow();
            Util.loggerControllerOptions.info("Run Loadflow OK");
        });
    }

    private void addListenerOnFilterField(TextField field, Stage primaryStage)
    {
        field.setOnAction(event ->
        {
            Util.loggerControllerOptions.info("Filter field OK");
        });
    }

    private static void addListenerOnNodesPane(Stage primaryStage)
    {
        for (CheckBox checkBox : new ArrayList<CheckBox>())
        {
            checkBox.setOnAction(event ->
            {
                if (checkBox.isSelected())
                {
                    Util.loggerControllerOptions.info("Node Selected OK" + checkBox.getText());
                }
                else
                {
                    Util.loggerControllerOptions.info("Node Unselected OK" + checkBox.getText());
                }
            });
        }
    }

    public OptionsPane getOptionsPane()
    {
        return  optionsPane;
    }
}
