/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.diagram.viewer.common.AbstractDiagramViewController;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.cgmes.dl.iidm.extensions.NetworkDiagramData;
import com.powsybl.sld.cgmes.layout.CgmesVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.SubstationLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.styles.StyleProvider;
import com.powsybl.sld.svg.styles.iidm.HighlightLineStateStyleProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SingleLineDiagramViewController extends AbstractDiagramViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramViewController.class);

    @FXML
    public ComboBox<ComponentLibrary> componentLibraryComboBox;

    @FXML
    public CheckBox basicStyleProviderCheckBox;

    @FXML
    public CheckBox nominalStyleProviderCheckBox;

    @FXML
    public CheckBox animatedStyleProviderCheckBox;

    @FXML
    public Spinner<Double> animationThreshold1Spinner;

    @FXML
    public Spinner<Double> animationThreshold2Spinner;

    @FXML
    public CheckBox highlightStyleProviderCheckBox;

    @FXML
    public CheckBox topologicalStyleProviderCheckBox;

    @FXML
    public ComboBox<SubstationLayoutFactory> substationLayoutComboBox;

    @FXML
    public ComboBox<VoltageLevelLayoutFactory> voltageLevelLayoutComboBox;

    @FXML
    public ComboBox<String> cgmesDLDiagramsComboBox;

    // Parameters
    @FXML
    public Spinner<Double> diagramPaddingTopBottomSpinner;

    @FXML
    public Spinner<Double> diagramPaddingLeftRightSpinner;

    @FXML
    public Spinner<Double> voltagePaddingTopBottomSpinner;

    @FXML
    public Spinner<Double> voltagePaddingLeftRightSpinner;

    @FXML
    public Spinner<Double> busbarVerticalSpaceSpinner;

    @FXML
    public Spinner<Double> busbarHorizontalSpaceSpinner;

    @FXML
    public Spinner<Double> cellWidthSpinner;

    @FXML
    public Spinner<Double> externCellHeightSpinner;

    @FXML
    public Spinner<Double> internCellHeightSpinner;

    @FXML
    public Spinner<Double> stackHeightSpinner;

    @FXML
    public CheckBox showGridCheckBox;

    @FXML
    public CheckBox showInternalNodesCheckBox;

    @FXML
    public CheckBox drawStraightWiresCheckBox;

    @FXML
    public CheckBox disconnectorsOnBusCheckBox;

    @FXML
    public CheckBox stackFeedersCheckBox;

    @FXML
    public CheckBox exceptionWhenPatternUnhandledCheckBox;

    @FXML
    public CheckBox handleShuntsCheckBox;

    @FXML
    public CheckBox removeFictitiousNodesCheckBox;

    @FXML
    public CheckBox substituteSingularFictitiousNodesCheckBox;

    @FXML
    public Spinner<Double> scaleFactorSpinner;

    @FXML
    public CheckBox avoidSVGComponentsDuplicationCheckBox;

    @FXML
    public CheckBox adaptCellHeightToContentCheckBox;

    @FXML
    public Spinner<Double> minSpaceBetweenComponentsSpinner;

    @FXML
    public Spinner<Double> minimumExternCellHeightSpinner;

    @FXML
    public ChoiceBox<LayoutParameters.Alignment> busBarAlignmentChoice;

    @FXML
    public CheckBox centerLabelCheckBox;

    @FXML
    public CheckBox labelDiagonalCheckBox;

    @FXML
    public Spinner<Double> angleLabelSpinner;

    @FXML
    public CheckBox addNodesInfosCheckBox;

    @FXML
    public CheckBox feederInfoSymmetryCheckBox;

    @FXML
    public Spinner<Double> spaceForFeederInfosSpinner;

    @FXML
    public Spinner<Double> feederInfosOuterMarginSpinner;

    @FXML
    public Spinner<Double> feederInfosIntraMarginSpinner;

    @FXML
    public SingleLineDiagramController selectedDiagramController;

    private SingleLineDiagramModel model;

    @FXML
    private void initialize() {
        model = new SingleLineDiagramModel(
                // Providers
                componentLibraryComboBox.valueProperty(),
                voltageLevelLayoutComboBox.valueProperty(),
                substationLayoutComboBox.valueProperty(),
                cgmesDLDiagramsComboBox.valueProperty(),
                // - Styles
                basicStyleProviderCheckBox.selectedProperty(),
                nominalStyleProviderCheckBox.selectedProperty(),
                animatedStyleProviderCheckBox.selectedProperty(),
                animationThreshold1Spinner.getValueFactory().valueProperty(),
                animationThreshold2Spinner.getValueFactory().valueProperty(),
                highlightStyleProviderCheckBox.selectedProperty(),
                topologicalStyleProviderCheckBox.selectedProperty(),
                // PositionVoltageLevelLayoutFactory
                stackFeedersCheckBox.selectedProperty(),
                exceptionWhenPatternUnhandledCheckBox.selectedProperty(),
                handleShuntsCheckBox.selectedProperty(),
                removeFictitiousNodesCheckBox.selectedProperty(),
                substituteSingularFictitiousNodesCheckBox.selectedProperty(),
                // LayoutParameters
                diagramPaddingTopBottomSpinner.getValueFactory().valueProperty(),
                diagramPaddingLeftRightSpinner.getValueFactory().valueProperty(),
                voltagePaddingTopBottomSpinner.getValueFactory().valueProperty(),
                voltagePaddingLeftRightSpinner.getValueFactory().valueProperty(),
                busbarVerticalSpaceSpinner.getValueFactory().valueProperty(),
                busbarHorizontalSpaceSpinner.getValueFactory().valueProperty(),
                cellWidthSpinner.getValueFactory().valueProperty(),
                externCellHeightSpinner.getValueFactory().valueProperty(),
                internCellHeightSpinner.getValueFactory().valueProperty(),
                stackHeightSpinner.getValueFactory().valueProperty(),
                showGridCheckBox.selectedProperty(),
                showInternalNodesCheckBox.selectedProperty(),
                drawStraightWiresCheckBox.selectedProperty(),
                disconnectorsOnBusCheckBox.selectedProperty(),
                scaleFactorSpinner.getValueFactory().valueProperty(),
                avoidSVGComponentsDuplicationCheckBox.selectedProperty(),
                adaptCellHeightToContentCheckBox.selectedProperty(),
                minSpaceBetweenComponentsSpinner.getValueFactory().valueProperty(),
                minimumExternCellHeightSpinner.getValueFactory().valueProperty(),
                busBarAlignmentChoice.valueProperty(),
                centerLabelCheckBox.selectedProperty(),
                labelDiagonalCheckBox.selectedProperty(),
                angleLabelSpinner.getValueFactory().valueProperty(),
                addNodesInfosCheckBox.selectedProperty(),
                feederInfoSymmetryCheckBox.selectedProperty(),
                spaceForFeederInfosSpinner.getValueFactory().valueProperty(),
                feederInfosOuterMarginSpinner.getValueFactory().valueProperty(),
                feederInfosIntraMarginSpinner.getValueFactory().valueProperty()
        );

        // Component library
        componentLibraryComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getComponentLibraries()));
        componentLibraryComboBox.setConverter(model.getComponentLibraryStringConverter());
        componentLibraryComboBox.getSelectionModel().selectLast(); // Flat selection
        // Style provider
        nominalStyleProviderCheckBox.setSelected(true); // Default selection without Network
        animationThreshold1Spinner.disableProperty().bind(animatedStyleProviderCheckBox.selectedProperty().not());
        animationThreshold2Spinner.disableProperty().bind(animatedStyleProviderCheckBox.selectedProperty().not());
        // Substation layout
        substationLayoutComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getSubstationLayouts()));
        substationLayoutComboBox.setConverter(model.getSubstationLayoutStringConverter());
        substationLayoutComboBox.getSelectionModel().selectFirst(); // Default selection without Network
        // VoltageLevel layout
        voltageLevelLayoutComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getVoltageLevelLayouts()));
        voltageLevelLayoutComboBox.setConverter(model.getVoltageLevelLayoutFactoryStringConverter());
        voltageLevelLayoutComboBox.getSelectionModel().selectFirst(); // Default selection without Network

        // CGMES-DL Diagrams
        cgmesDLDiagramsComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getCgmesDLDiagramNames()));
        cgmesDLDiagramsComboBox.getSelectionModel().selectFirst(); // Default selection without Network

        // PositionVoltageLevelLayoutFactory
        BooleanBinding disableBinding = Bindings.createBooleanBinding(() -> voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem() instanceof PositionVoltageLevelLayoutFactory, voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty());
        stackFeedersCheckBox.visibleProperty().bind(disableBinding);
        exceptionWhenPatternUnhandledCheckBox.visibleProperty().bind(disableBinding);
        handleShuntsCheckBox.visibleProperty().bind(disableBinding);
        removeFictitiousNodesCheckBox.visibleProperty().bind(disableBinding);
        substituteSingularFictitiousNodesCheckBox.visibleProperty().bind(disableBinding);
        // Force layout calculations when nodes are shown or hidden
        stackFeedersCheckBox.managedProperty().bind(stackFeedersCheckBox.visibleProperty());
        exceptionWhenPatternUnhandledCheckBox.managedProperty().bind(exceptionWhenPatternUnhandledCheckBox.visibleProperty());
        handleShuntsCheckBox.managedProperty().bind(handleShuntsCheckBox.visibleProperty());
        removeFictitiousNodesCheckBox.managedProperty().bind(removeFictitiousNodesCheckBox.visibleProperty());
        substituteSingularFictitiousNodesCheckBox.managedProperty().bind(substituteSingularFictitiousNodesCheckBox.visibleProperty());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        componentLibraryComboBox.valueProperty().addListener(changeListener);
        substationLayoutComboBox.valueProperty().addListener(changeListener);
        voltageLevelLayoutComboBox.valueProperty().addListener(changeListener);
        cgmesDLDiagramsComboBox.valueProperty().addListener(changeListener);

        basicStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        nominalStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        animatedStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        highlightStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        topologicalStyleProviderCheckBox.selectedProperty().addListener(changeListener);

        // PositionVoltageLevelLayoutFactory
        stackFeedersCheckBox.selectedProperty().addListener(changeListener);
        exceptionWhenPatternUnhandledCheckBox.selectedProperty().addListener(changeListener);
        handleShuntsCheckBox.selectedProperty().addListener(changeListener);
        removeFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);
        substituteSingularFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);

        // LayoutParameters
        model.addListener(changeListener);
    }

    public void updateAllDiagrams(Network network, Container<?> selectedContainer) {
        if (selectedContainer != null) {
            SingleLineDiagramController.updateDiagram(network, model, model.getSelectedContainerResult(), selectedContainer);
        }
        model.getCheckedContainerStream().forEach(container -> SingleLineDiagramController.updateDiagram(network, model, model.getCheckedContainerResult(container), container));
    }

    public void createDiagram(SingleLineDiagramJsHandler jsHandler, Network network, Container<?> container) {
        selectedDiagramController.createDiagram(jsHandler, network, model, model.getSelectedContainerResult(), container);
    }

    public void createCheckedTab(SingleLineDiagramJsHandler jsHandler,
                                 Network network,
                                 CheckBoxTreeItem<Container<?>> containerTreeItem,
                                 String tabName) {
        try {
            Container<?> container = containerTreeItem.getValue();
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent diagram = fxmlLoader.load(Objects.requireNonNull(getClass().getResourceAsStream("/sld/singleLineDiagramView.fxml")));
            SingleLineDiagramController checkedDiagramController = fxmlLoader.getController();
            checkedDiagramController.createDiagram(jsHandler,
                    network,
                    model,
                    model.getCheckedContainerResult(container),
                    container);
            super.createCheckedTab(containerTreeItem, tabName, diagram, checkedDiagramController);
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    @Override
    protected void removeCheckedDiagram(Tab tab, Container<?> container) {
        super.removeCheckedDiagram(tab, container);
        checkedDiagramControllers.remove(tab);
        model.removeCheckedContainerResult(container);
    }

    public SingleLineDiagramModel getModel() {
        return model;
    }

    public void updateFrom(final ObjectProperty<Network> networkProperty) {
        getModel().updateFrom(networkProperty.get());
        cgmesDLDiagramsComboBox.disableProperty().unbind();
        cgmesDLDiagramsComboBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            boolean cgmesSelected = voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem() instanceof CgmesVoltageLevelLayoutFactory;
            return cgmesSelected && NetworkDiagramData.checkNetworkDiagramData(networkProperty.get());
        }, voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty()).not());

        // Topology selection by default
        basicStyleProviderCheckBox.setSelected(false);
        animatedStyleProviderCheckBox.setSelected(false);
        highlightStyleProviderCheckBox.setSelected(false);
        nominalStyleProviderCheckBox.setSelected(networkProperty.get() == null);
        topologicalStyleProviderCheckBox.setSelected(networkProperty.get() != null);
        // Only available if network
        highlightStyleProviderCheckBox.disableProperty().unbind();
        highlightStyleProviderCheckBox.disableProperty().bind(Bindings.createBooleanBinding(() -> networkProperty.get() != null, networkProperty).not());
        topologicalStyleProviderCheckBox.disableProperty().unbind();
        topologicalStyleProviderCheckBox.disableProperty().bind(Bindings.createBooleanBinding(() -> networkProperty.get() != null, networkProperty).not());

        // Horizontal selection
        substationLayoutComboBox.getSelectionModel().select(1);
        // Smart selection
        voltageLevelLayoutComboBox.getSelectionModel().selectLast();
        // CGMES-DL Diagrams first selection
        cgmesDLDiagramsComboBox.getSelectionModel().selectFirst();
    }

    @Override
    protected AbstractDiagramController getSelectedDiagramController() {
        return selectedDiagramController;
    }

    @Override
    protected AbstractDiagramController getCheckedDiagramController(Tab tabInChecked) {
        return checkedDiagramControllers.get(tabInChecked);
    }
}
