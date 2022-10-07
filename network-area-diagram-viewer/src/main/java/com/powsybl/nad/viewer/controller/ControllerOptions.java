/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.controller;

import com.powsybl.iidm.network.Container;
import com.powsybl.nad.viewer.model.NadCalls;
import com.powsybl.nad.viewer.view.OptionsPane;
import com.powsybl.nad.viewer.view.diagram.DiagramPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerFullNetworkDiagramPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerSubstationDiagramPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerVoltageDiagramPane;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static com.powsybl.nad.viewer.model.NadCalls.getSvgWriter;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerOptions.class);
    private final OptionsPane optionsPane;
    private final DiagramPane diagramPane;

    public ControllerOptions(DiagramPane diagramPane, OptionsPane optionsPane) {
        this.optionsPane = optionsPane;
        this.diagramPane = diagramPane;
        initTreeCellFactory();
    }

    public void setParamPane() {
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

    /*
      Handling the display of names/id in the substations tree
    */
    private void initTreeCellFactory() {
        optionsPane.getSubstationTree().setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            StringConverter<TreeItem<Container<?>>> strConvert = new StringConverter<>() {
                @Override
                public String toString(TreeItem<Container<?>> c) {
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

    private void addListenerOnFullNetworkCheck(CheckBoxTreeItem<Container<?>> check) {
        check.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (check.isSelected()) {
                try {
                    NadCalls.drawNetwork(check.getValue().getNetwork());
                    diagramPane.addSvgToCheckedTab("Full Network", "Full Network",
                            diagramPane.getCheckedDiagramPane().getTabs().isEmpty() ? 0 : diagramPane.getCheckedDiagramPane().getTabs().size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // if event is unchecking the FullNetwork checkbox, close tab
                List<Tab> tabList = diagramPane.getCheckedDiagramPane().getTabs();
                for (Tab checkedTab : tabList) {
                    if (checkedTab.getText().equals("Full Network")) {
                        diagramPane.getCheckedDiagramPane().getTabs().remove(checkedTab);
                        break;
                    }
                }
            }
        });
    }

    private void addListenerOnDepthSpinner(Spinner<Integer> spinner) {
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            optionsPane.setDepthSpinnerValue(Math.max(0, newValue));
            redisplayAll();
        });
    }

    private void addListenerOnRunFlowButton(Button button) {
        button.setOnAction(event -> {
            NadCalls.runLoadFlow();

            // All SVGs need to be re-displayed (SVGs in the subtabs of CheckedPane and SVG in the SelectedPane)
            redisplayAll();
        });
    }

    private void redisplayAll() {
        if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
            // this if statement just checks whether or not the SvgWriter variable has been cleared before (or has
            // never been written). If "empty", there are no SVG to change, so there is not much to do.

            //// Storing displayed CheckedTabs (and the index to restore the checked tab selected by the user, the
            // pointer of the object will change, so we need to remember the index)
            ObservableList<Tab> listCheckedTabs = diagramPane.getListCheckedTabs();
            int indexCheckedTabSelectedByUser = diagramPane.getIndexCheckedTabSelectedByUser();

            // Let's get the diagramPane object

            // 1- The SVGs in the subtabs of CheckedPane
            for (int i = 0; i < listCheckedTabs.size(); i++) {
                Tab tab = listCheckedTabs.get(i);
                if (tab.getContent() instanceof ContainerFullNetworkDiagramPane) {
                    listCheckedTabs.remove(tab);
                    diagramPane.redrawCheckedTabSVG(tab.getText(), tab.getTooltip().getText(), i);
                } else if (tab.getContent() instanceof ContainerSubstationDiagramPane) {
                    listCheckedTabs.remove(tab);
                    diagramPane.redrawCheckedTabSVG(((ContainerSubstationDiagramPane) tab.getContent()).getVoltageLevelIds(),
                            optionsPane.getDepthSpinnerValue(), tab.getText(), tab.getTooltip().getText(), i);
                } else if (tab.getContent() instanceof ContainerVoltageDiagramPane) {
                    listCheckedTabs.remove(tab);
                    diagramPane.redrawCheckedTabSVG(((ContainerVoltageDiagramPane) tab.getContent()).getVoltageLevelId(),
                            optionsPane.getDepthSpinnerValue(), tab.getText(), tab.getTooltip().getText(), i);
                } else {
                    throw new AssertionError();
                }
            }

            // 2- The SVG in the SelectedPane

            BorderPane selectedDiagramPane = diagramPane.getSelectedDiagramPane();

            if (selectedDiagramPane.getCenter() instanceof ContainerFullNetworkDiagramPane) {
                diagramPane.redrawSelectedTabSVG();
            } else if (selectedDiagramPane.getCenter() instanceof ContainerSubstationDiagramPane) {
                diagramPane.redrawSelectedTabSVG(((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelIds(),
                        optionsPane.getDepthSpinnerValue());
            } else if (selectedDiagramPane.getCenter() instanceof ContainerVoltageDiagramPane) {
                diagramPane.redrawSelectedTabSVG(((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelId(),
                        optionsPane.getDepthSpinnerValue());
            } else {
                LOGGER.info("Unknown selectedDiagramPane.getCenter() type. Means that no Network Area Diagram was selected.");
            }

            //// Restore the former displayed tab
            diagramPane.setCheckedTabSelectedByUser(indexCheckedTabSelectedByUser);

        }
    }

    private void addListenerOnFilterField(TextField field) {
        field.setOnAction(event -> {
            LOGGER.info("Filter field not yet implemented");
        });
    }
}
