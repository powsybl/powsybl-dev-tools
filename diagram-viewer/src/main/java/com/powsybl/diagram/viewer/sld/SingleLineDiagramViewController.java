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
import com.powsybl.sld.layout.*;
import com.powsybl.sld.layout.position.clustering.PositionByClustering;
import com.powsybl.sld.layout.position.predefined.PositionPredefined;
import com.powsybl.sld.library.ComponentLibrary;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

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
    public HBox animationHBox;

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
    public ComboBox<ZoneLayoutFactory> zoneLayoutComboBox;

    @FXML
    public ChoiceBox<SingleLineDiagramModel.VoltageLevelLayoutFactoryType> voltageLevelLayoutComboBox;

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
    public VBox positionVoltageLevelLayoutFactoryParameters;

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
    public CheckBox substituteInternalMiddle2wtByEquipmentNodesCheckBox;

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
    public CheckBox displayEquipmentNodesLabelCheckBox;

    @FXML
    public CheckBox displayConnectivityNodesIdCheckBox;

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
                substationLayoutComboBox.valueProperty(),
                zoneLayoutComboBox.valueProperty(),
                cgmesDLDiagramsComboBox.valueProperty(),
                // - Styles
                basicStyleProviderCheckBox.selectedProperty(),
                nominalStyleProviderCheckBox.selectedProperty(),
                animatedStyleProviderCheckBox.selectedProperty(),
                animationThreshold1Spinner.getValueFactory().valueProperty(),
                animationThreshold2Spinner.getValueFactory().valueProperty(),
                highlightStyleProviderCheckBox.selectedProperty(),
                topologicalStyleProviderCheckBox.selectedProperty(),
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
                disconnectorsOnBusCheckBox.selectedProperty(),
                scaleFactorSpinner.getValueFactory().valueProperty(),
                adaptCellHeightToContentCheckBox.selectedProperty(),
                minSpaceBetweenComponentsSpinner.getValueFactory().valueProperty(),
                minimumExternCellHeightSpinner.getValueFactory().valueProperty(),
                busBarAlignmentChoice.valueProperty(),
                spaceForFeederInfosSpinner.getValueFactory().valueProperty(),

                //SvgParameters
                showGridCheckBox.selectedProperty(),
                showInternalNodesCheckBox.selectedProperty(),
                drawStraightWiresCheckBox.selectedProperty(),
                centerLabelCheckBox.selectedProperty(),
                labelDiagonalCheckBox.selectedProperty(),
                displayEquipmentNodesLabelCheckBox.selectedProperty(),
                displayConnectivityNodesIdCheckBox.selectedProperty(),
                angleLabelSpinner.getValueFactory().valueProperty(),
                addNodesInfosCheckBox.selectedProperty(),
                feederInfoSymmetryCheckBox.selectedProperty(),
                avoidSVGComponentsDuplicationCheckBox.selectedProperty(),
                feederInfosOuterMarginSpinner.getValueFactory().valueProperty(),
                feederInfosIntraMarginSpinner.getValueFactory().valueProperty()
        );

        // Component library
        componentLibraryComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getComponentLibraries()));
        componentLibraryComboBox.setConverter(model.getComponentLibraryStringConverter());
        componentLibraryComboBox.getSelectionModel().selectLast(); // Flat selection
        // Style provider
        nominalStyleProviderCheckBox.setSelected(true); // Default selection without Network
        animationHBox.visibleProperty().bind(animatedStyleProviderCheckBox.selectedProperty());
        animationHBox.managedProperty().bind(animationHBox.visibleProperty());
        // Substation layout
        substationLayoutComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getSubstationLayouts()));
        substationLayoutComboBox.setConverter(model.getSubstationLayoutStringConverter());
        substationLayoutComboBox.getSelectionModel().selectFirst(); // Default selection without Network
        // Zone layout
        zoneLayoutComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getZoneLayouts()));
        zoneLayoutComboBox.setConverter(model.getZoneLayoutStringConverter());
        zoneLayoutComboBox.getSelectionModel().selectFirst(); // Default selection without Network

        // CGMES-DL Diagrams
        cgmesDLDiagramsComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getCgmesDLDiagramNames()));
        cgmesDLDiagramsComboBox.getSelectionModel().selectFirst(); // Default selection without Network

        // PositionVoltageLevelLayoutFactory
        positionVoltageLevelLayoutFactoryParameters.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> voltageLevelLayoutFactoryParametersEnabled(voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem()),
                voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty()));
        positionVoltageLevelLayoutFactoryParameters.managedProperty().bind(positionVoltageLevelLayoutFactoryParameters.visibleProperty()); // Force view layout calculations when nodes are shown or hidden
    }

    private boolean voltageLevelLayoutFactoryParametersEnabled(SingleLineDiagramModel.VoltageLevelLayoutFactoryType selectedItem) {
        return selectedItem == SingleLineDiagramModel.VoltageLevelLayoutFactoryType.POSITION_WITH_EXTENSIONS
                || selectedItem == SingleLineDiagramModel.VoltageLevelLayoutFactoryType.POSITION_BY_CLUSTERING;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        componentLibraryComboBox.valueProperty().addListener(changeListener);
        substationLayoutComboBox.valueProperty().addListener(changeListener);
        zoneLayoutComboBox.valueProperty().addListener(changeListener);
        cgmesDLDiagramsComboBox.valueProperty().addListener(changeListener);

        basicStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        nominalStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        animatedStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        highlightStyleProviderCheckBox.selectedProperty().addListener(changeListener);
        topologicalStyleProviderCheckBox.selectedProperty().addListener(changeListener);

        voltageLevelLayoutComboBox.valueProperty().addListener(changeListener);
        stackFeedersCheckBox.selectedProperty().addListener(changeListener);
        exceptionWhenPatternUnhandledCheckBox.selectedProperty().addListener(changeListener);
        handleShuntsCheckBox.selectedProperty().addListener(changeListener);
        removeFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);
        substituteSingularFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);
        substituteInternalMiddle2wtByEquipmentNodesCheckBox.selectedProperty().addListener(changeListener);

        // LayoutParameters
        model.addListener(changeListener);
    }

    public void updateAllDiagrams(Network network, Container<?> selectedContainer) {
        if (selectedContainer != null) {
            SingleLineDiagramController.updateDiagram(network, model, model.getSelectedContainerResult(), selectedContainer,
                    getVoltageLevelLayoutFactoryCreator());
        }
        model.getCheckedContainerStream().forEach(container -> SingleLineDiagramController.updateDiagram(network, model, model.getCheckedContainerResult(container), container, getVoltageLevelLayoutFactoryCreator()));
    }

    public void createDiagram(SingleLineDiagramJsHandler jsHandler, Network network, Container<?> container) {
        selectedDiagramController.createDiagram(jsHandler, network, model, model.getSelectedContainerResult(), container, getVoltageLevelLayoutFactoryCreator());
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
                    container,
                    getVoltageLevelLayoutFactoryCreator());
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
            boolean cgmesSelected = voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem() == SingleLineDiagramModel.VoltageLevelLayoutFactoryType.CGMES;
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

    public VoltageLevelLayoutFactoryCreator getVoltageLevelLayoutFactoryCreator() {
        PositionVoltageLevelLayoutFactoryParameters parameters = new PositionVoltageLevelLayoutFactoryParameters();
        parameters.setFeederStacked(stackFeedersCheckBox.isSelected())
                .setExceptionIfPatternNotHandled(exceptionWhenPatternUnhandledCheckBox.isSelected())
                .setHandleShunts(handleShuntsCheckBox.isSelected())
                .setRemoveUnnecessaryFictitiousNodes(removeFictitiousNodesCheckBox.isSelected())
                .setSubstituteSingularFictitiousByFeederNode(substituteSingularFictitiousNodesCheckBox.isSelected())
                .setSubstituteInternalMiddle2wtByEquipmentNodes(substituteInternalMiddle2wtByEquipmentNodesCheckBox.isSelected());
        SingleLineDiagramModel.VoltageLevelLayoutFactoryType type = voltageLevelLayoutComboBox.getValue();
        return switch (type) {
            case SMART -> SmartVoltageLevelLayoutFactory::new;
            case POSITION_WITH_EXTENSIONS -> network -> new PositionVoltageLevelLayoutFactory(new PositionPredefined(), parameters);
            case POSITION_BY_CLUSTERING -> network -> new PositionVoltageLevelLayoutFactory(new PositionByClustering(), parameters);
            case RANDOM -> network -> new RandomVoltageLevelLayoutFactory(500.0, 500.0);
            case CGMES -> CgmesVoltageLevelLayoutFactory::new;
        };
    }
}
