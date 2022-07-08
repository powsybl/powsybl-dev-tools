/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.ParamPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerFullNetworkDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerSubstationDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerVoltageDiagramPane;
import com.powsybl.nad.svg.Padding;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.powsybl.ad.viewer.model.NadCalls.*;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerParameters.class);
    private static ParamPane paramPane;

    private ChangeListener<SvgParameters> listener;

    private static StyleProvider styleProvider;  // inside it will be stored the dropdown list's selected value
    // NadCalls methods will read the value from this class when drawing diagrams

    public void createParamPane() {
        paramPane = new ParamPane();
    }

    public void setParamPane() {
        // Fit and Reset buttons
        addListenerOnFitToContent(paramPane.getFitContentButton());
        addListenerOnResetZoom(paramPane.getResetZoomButton());

        // Layout, Label provider and Style Provider Choices
        addListenerOnLayoutChoice(paramPane.getLayoutChoice());
        addListenerOnLabelProviderChoice(paramPane.getLabelProviderChoice());
        addListenerOnStyleProviderChoice(paramPane.getStyleProviderChoice());

        // Layout parameters spinners
        SpinnerValueFactory<Integer> layoutXSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getLayoutXSpinner().setValueFactory(layoutXSpinnerFactory);
        addListenerOnLayoutXSpinner(paramPane.getLayoutXSpinner());

        SpinnerValueFactory<Integer> layoutYSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getLayoutYSpinner().setValueFactory(layoutYSpinnerFactory);
        addListenerOnLayoutYSpinner(paramPane.getLayoutYSpinner());

        // SVG parameters spinners and checkbox
        paramPane.setSvgXSpinner(addSpinner(paramPane.getSvgParametersPane(), "Horizontal Padding", 0, 300, 5, 0, 0, "X", Padding::getLeft, (svgp, value) -> svgp.setDiagramPadding(new Padding(value, svgp.getDiagramPadding().getTop(), value, svgp.getDiagramPadding().getBottom()))));
        paramPane.setSvgYSpinner(addSpinner(paramPane.getSvgParametersPane(), "Vertical Padding", 0, 300, 5, 0, 2, "Y", svgp -> svgp.getBottom(), (svgp, value) -> svgp.setDiagramPadding(new Padding(svgp.getDiagramPadding().getLeft(), value, svgp.getDiagramPadding().getRight(), value))));

        addListenerOnSvgEdgeInfoCheckbox(paramPane.getSvgEdgeInfoCheckbox());
    }

    private void setParameters(SvgParameters svgParameters) {
        // Full Network - Selected Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawNetwork();
            ControllerDiagram.addSvgToSelectedTab();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(SvgParameters svgParameters, String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Full Network - Checked Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawNetwork();
            ControllerDiagram.addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(SvgParameters svgParameters, List<String> voltageLevelIds, int depth) {
        // Substation - Selected Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawUniqueSubstation(voltageLevelIds, depth);
            ControllerDiagram.addSvgToSelectedTab(voltageLevelIds, depth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(SvgParameters svgParameters, List<String> voltageLevelIds, int depth, String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Substation - Checked Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawUniqueSubstation(voltageLevelIds, depth);
            ControllerDiagram.addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, voltageLevelIds, depth, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(SvgParameters svgParameters, String voltageLevelId, int depth) {
        // Voltage (= Subgraph) - Selected Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawSubgraph(voltageLevelId, depth);
            ControllerDiagram.addSvgToSelectedTab(voltageLevelId, depth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(SvgParameters svgParameters, String voltageLevelId, int depth, String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Voltage (= Subgraph) - Checked Tab
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
        try {
            NadCalls.drawSubgraph(voltageLevelId, depth);
            ControllerDiagram.addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, voltageLevelId, depth, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addListenerOnFitToContent(Button fitContentButton) {
        fitContentButton.setOnAction(event -> {
            // Only the currently selected tab's zoom should changed to fit to content
            Tab tabSelectedByUser = ControllerDiagram.getDiagramPane().getTabSelectedByUser();
            Node tabSelectedByUserNode = tabSelectedByUser.getContent();

            Node borderPaneNode = null;
            Tab selectedSubTabByUser = null;

            if (tabSelectedByUserNode != null) {
                if (tabSelectedByUserNode instanceof BorderPane) {  // that's if we in Selected (cf. createSelectedTab)
                    // We need to downcast tabSelectedByUserNode into a BorderPane
                    BorderPane borderPaneDowncast = (BorderPane) tabSelectedByUserNode;
                    borderPaneNode = borderPaneDowncast.getCenter();
                }
                if (tabSelectedByUserNode instanceof TabPane) {  // that's if we in Checked (cf. createCheckedTab)
                    // We need to downcast tabSelectedByUserNode into a TabPane
                    TabPane tabPaneDowncast = (TabPane) tabSelectedByUserNode;
                    selectedSubTabByUser = tabPaneDowncast.getSelectionModel().getSelectedItem();
                }
            }

            ContainerDiagramPane pane = null;

            if (borderPaneNode != null) {  //  that's if we in Selected
                if (borderPaneNode instanceof ContainerDiagramPane) {
                    // We need to downcast borderPaneNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) borderPaneNode;
                }
            }
            if (selectedSubTabByUser != null) { // that's if we in Checked
                Node selectedSubTabByUserNode = selectedSubTabByUser.getContent();
                if (selectedSubTabByUserNode instanceof ContainerDiagramPane) {
                    // We need to downcast selectedSubTabByUserNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) selectedSubTabByUserNode;
                }
            }

            // Now we fit to content
            if (pane != null) {
                String svgData = pane.getSvgTextArea().getText();
                Optional<String> svgLine = svgData.lines().filter(l -> l.contains("<svg")).findAny();
                if (svgLine.isPresent()) {
                    String valuePattern = "\"([^\"]*)\"";
                    Pattern pW = Pattern.compile("width=" + valuePattern);
                    Matcher mW = pW.matcher(svgLine.get());
                    Pattern pH = Pattern.compile("height=" + valuePattern);
                    Matcher mH = pH.matcher(svgLine.get());
                    if (mH.find() && mW.find()) {
                        double svgWidth = Double.parseDouble(mW.group(1));
                        double svgHeight = Double.parseDouble(mH.group(1));
                        double paneWidth = pane.getDiagramView().widthProperty().get();
                        double paneHeight = pane.getDiagramView().heightProperty().get();
                        double zoomH = paneHeight / svgHeight;
                        double zoomW = paneWidth / svgWidth;
                        pane.getDiagramView().setZoom(Math.min(zoomH, zoomW));
                    }
                }
            }
        });
    }

    private void addListenerOnResetZoom(Button resetZoomButton) {
        resetZoomButton.setOnAction(event -> {

            // Only the currently selected tab's zoom should be reset
            Tab tabSelectedByUser = ControllerDiagram.getDiagramPane().getTabSelectedByUser();
            Node tabSelectedByUserNode = tabSelectedByUser.getContent();

            Node borderPaneNode = null;
            Tab selectedSubTabByUser = null;

            if (tabSelectedByUserNode != null) {
                if (tabSelectedByUserNode instanceof BorderPane) {  // that's if we in Selected (cf. createSelectedTab)
                    // We need to downcast tabSelectedByUserNode into a BorderPane
                    BorderPane borderPaneDowncast = (BorderPane) tabSelectedByUserNode;
                    borderPaneNode = borderPaneDowncast.getCenter();
                }
                if (tabSelectedByUserNode instanceof TabPane) {  // that's if we in Checked (cf. createCheckedTab)
                    // We need to downcast tabSelectedByUserNode into a TabPane
                    TabPane tabPaneDowncast = (TabPane) tabSelectedByUserNode;
                    selectedSubTabByUser = tabPaneDowncast.getSelectionModel().getSelectedItem();
                }
            }

            ContainerDiagramPane pane = null;

            if (borderPaneNode != null) {  //  that's if we in Selected
                if (borderPaneNode instanceof ContainerDiagramPane) {
                    // We need to downcast borderPaneNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) borderPaneNode;
                }
            }
            if (selectedSubTabByUser != null) { // that's if we in Checked
                Node selectedSubTabByUserNode = selectedSubTabByUser.getContent();
                if (selectedSubTabByUserNode instanceof ContainerDiagramPane) {
                    // We need to downcast selectedSubTabByUserNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) selectedSubTabByUserNode;
                }
            }

            if (pane != null) {
                pane.getDiagramView().setZoom(1.0);
            }
        });
    }

    private void addListenerOnLayoutChoice(ChoiceBox layoutChoice) {
        layoutChoice.setOnAction(event -> {
            LOGGER.info("Layout choice not implemented");
//            NadCalls.setLayoutParameters(toString(layoutChoice.getValue()));
        });
    }

    private void addListenerOnLabelProviderChoice(ChoiceBox labelProviderChoice) {
        labelProviderChoice.setOnAction(event -> LOGGER.info("Label Provider choice not implemented"));
    }

    private void addListenerOnStyleProviderChoice(ChoiceBox styleProviderChoice) {
        styleProviderChoice.setOnAction(event -> {
            if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
                // this if statement just checks whether or not the SvgWriter variable has been cleared before (or has
                // never been written). If "empty", there are no SVG to change, so there is not much to do.

                //// Storing displayed CheckedTabs (and the index to restore the checked tab selected by the user, the
                // pointer of the object will change so we need to remember the index)
                ObservableList<Tab> listCheckedTabs = ControllerDiagram.getListCheckedTabs();
                int indexCheckedTabSelectedByUser = ControllerDiagram.getIndexCheckedTabSelectedByUser();

                if (styleProviderChoice.getValue() == "Nominal") {
                    styleProvider = new NominalVoltageStyleProvider(networkProperty.get());
                } else if (styleProviderChoice.getValue() == "Topological") {
                    styleProvider = new TopologicalStyleProvider(NadCalls.networkProperty.get());
                }

                //// If the dropdown list's value changes, it has got to affect all SVGs

                // 1- The SVGs in the subtabs of CheckedPane
                for (int i = 0; i < listCheckedTabs.size(); i++) {
                    Tab tab = listCheckedTabs.get(i);
                    if (tab.getContent() instanceof ContainerFullNetworkDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(svgParametersProperty.get(), tab.getText(), tab.getTooltip().getText(), i);
                    } else if (tab.getContent() instanceof ContainerSubstationDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(svgParametersProperty.get(), ((ContainerSubstationDiagramPane) tab.getContent()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                    } else if (tab.getContent() instanceof ContainerVoltageDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(svgParametersProperty.get(), ((ContainerVoltageDiagramPane) tab.getContent()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                    } else {
                        throw new AssertionError();
                    }
                }

                // 2- The SVG in the SelectedPane

                BorderPane selectedDiagramPane = ControllerDiagram.getDiagramPane().getSelectedDiagramPane();

                if (selectedDiagramPane.getCenter() instanceof ContainerFullNetworkDiagramPane) {
                    setParameters(svgParametersProperty.get());
                } else if (selectedDiagramPane.getCenter() instanceof ContainerSubstationDiagramPane) {
                    setParameters(svgParametersProperty.get(), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getDepth());
                } else if (selectedDiagramPane.getCenter() instanceof ContainerVoltageDiagramPane) {
                    setParameters(svgParametersProperty.get(), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getDepth());
                } else {
                    LOGGER.error("Unknown selectedDiagramPane.getCenter() type");
                }

                //// Restore the former displayed tab
                ControllerDiagram.getDiagramPane().setCheckedTabSelectedByUser(indexCheckedTabSelectedByUser);
            }
        });
    }

    private Spinner addSpinner(GridPane paneToAddSpinnerOn, String label, double min, double max, double amountToStepBy, int column, int row, String direction, ToDoubleFunction<Padding> initializer, BiFunction<SvgParameters, Double, SvgParameters> updater) {
        Spinner<Double> spinner = new Spinner<>(min, max, initializer.applyAsDouble(svgParametersProperty.get().getDiagramPadding()), amountToStepBy);
        spinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
                // this if statement just checks whether or not the SvgWriter variable has been cleared before (or has
                // never been written). If "empty", there are no SVG to change, so there is not much to do.

                //// Storing displayed CheckedTabs (and the index to restore the checked tab selected by the user, the
                // pointer of the object will change so we need to remember the index)
                ObservableList<Tab> listCheckedTabs = ControllerDiagram.getListCheckedTabs();
                int indexCheckedTabSelectedByUser = ControllerDiagram.getIndexCheckedTabSelectedByUser();

                // Storing oldSvgParametersProperty because the padding has to be changed but the rest should
                // stay the same
                SvgParameters oldSvgParametersProperty = svgParametersProperty.get();

                //// If the dropdown list's value changes, it has got to affect all SVGs

                // 1- The SVGs in the subtabs of CheckedPane
                for (int i = 0; i < listCheckedTabs.size(); i++) {
                    Tab tab = listCheckedTabs.get(i);
                    if (tab.getContent() instanceof ContainerFullNetworkDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(updater.apply(oldSvgParametersProperty, newValue), tab.getText(), tab.getTooltip().getText(), i);
                    } else if (tab.getContent() instanceof ContainerSubstationDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(updater.apply(oldSvgParametersProperty, newValue), ((ContainerSubstationDiagramPane) tab.getContent()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                    } else if (tab.getContent() instanceof ContainerVoltageDiagramPane) {
                        listCheckedTabs.remove(tab);
                        setParameters(updater.apply(oldSvgParametersProperty, newValue), ((ContainerVoltageDiagramPane) tab.getContent()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                    } else {
                        throw new AssertionError();
                    }
                }

                // 2- The SVG in the SelectedPane

                BorderPane selectedDiagramPane = ControllerDiagram.getDiagramPane().getSelectedDiagramPane();

                if (selectedDiagramPane.getCenter() instanceof ContainerFullNetworkDiagramPane) {
                    setParameters(updater.apply(oldSvgParametersProperty, newValue));
                } else if (selectedDiagramPane.getCenter() instanceof ContainerSubstationDiagramPane) {
                    setParameters(updater.apply(oldSvgParametersProperty, newValue), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getDepth());
                } else if (selectedDiagramPane.getCenter() instanceof ContainerVoltageDiagramPane) {
                    setParameters(updater.apply(oldSvgParametersProperty, newValue), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getDepth());
                } else {
                    LOGGER.error("Unknown selectedDiagramPane.getCenter() type");
                }

                //// Restore the former displayed tab
                ControllerDiagram.getDiagramPane().setCheckedTabSelectedByUser(indexCheckedTabSelectedByUser);
            }
        });

        spinner.setEditable(true);
        spinner.setDisable(true);
        paneToAddSpinnerOn.add(new Label(label), column, row);
        paneToAddSpinnerOn.add(spinner, column, row + 1);

        return spinner;
    }

    private void addListenerOnLayoutXSpinner(Spinner layoutXSpinner) {
        layoutXSpinner.valueProperty().addListener((obs, oldValue, newValue) -> LOGGER.info("Not implemented"));
    }

    private void addListenerOnLayoutYSpinner(Spinner layoutYSpinner) {
        layoutYSpinner.valueProperty().addListener((obs, oldValue, newValue) -> LOGGER.info("Not implemented"));
    }

    private void addListenerOnSvgEdgeInfoCheckbox(CheckBox svgEdgeInfoCheckbox) {
        svgEdgeInfoCheckbox.setOnAction(event -> {
            //// CheckBox State
            Boolean isCheckBoxSelected = svgEdgeInfoCheckbox.isSelected();

            //// Storing displayed CheckedTabs (and the index to restore the checked tab selected by the user, the
            // pointer of the object will change so we need to remember the index)
            ObservableList<Tab> listCheckedTabs = ControllerDiagram.getListCheckedTabs();
            int indexCheckedTabSelectedByUser = ControllerDiagram.getIndexCheckedTabSelectedByUser();

            //// If the checkbox is checked / unchecked, it has got to affect all SVGs

            // 1- The SVGs in the subtabs of CheckedPane
            for (int i = 0; i < listCheckedTabs.size(); i++) {
                Tab tab = listCheckedTabs.get(i);
                if (tab.getContent() instanceof ContainerFullNetworkDiagramPane) {
                    listCheckedTabs.remove(tab);
                    setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected), tab.getText(), tab.getTooltip().getText(), i);
                } else if (tab.getContent() instanceof ContainerSubstationDiagramPane) {
                    listCheckedTabs.remove(tab);
                    setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected), ((ContainerSubstationDiagramPane) tab.getContent()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                } else if (tab.getContent() instanceof ContainerVoltageDiagramPane) {
                    listCheckedTabs.remove(tab);
                    setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected), ((ContainerVoltageDiagramPane) tab.getContent()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) tab.getContent()).getDepth(), tab.getText(), tab.getTooltip().getText(), i);
                } else {
                    throw new AssertionError();
                }
            }

            // 2- The SVG in the SelectedPane

            BorderPane selectedDiagramPane = ControllerDiagram.getDiagramPane().getSelectedDiagramPane();

            if (selectedDiagramPane.getCenter() instanceof ContainerFullNetworkDiagramPane) {
                setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected));
            } else if (selectedDiagramPane.getCenter() instanceof ContainerSubstationDiagramPane) {
                setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelIds(), ((ContainerSubstationDiagramPane) selectedDiagramPane.getCenter()).getDepth());
            } else if (selectedDiagramPane.getCenter() instanceof ContainerVoltageDiagramPane) {
                setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(isCheckBoxSelected), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getVoltageLevelId(), ((ContainerVoltageDiagramPane) selectedDiagramPane.getCenter()).getDepth());
            } else {
                LOGGER.error("Unknown selectedDiagramPane.getCenter() type");
            }

            //// Restore the former displayed tab
            ControllerDiagram.getDiagramPane().setCheckedTabSelectedByUser(indexCheckedTabSelectedByUser);
        });
    }

    public static ParamPane getParamPane() {
        return paramPane;
    }

    public static StyleProvider getStyleProvider() {
        return styleProvider;
    }

    public static void setStyleProvider(StyleProvider styleProvider) {
        ControllerParameters.styleProvider = styleProvider;
    }

}
