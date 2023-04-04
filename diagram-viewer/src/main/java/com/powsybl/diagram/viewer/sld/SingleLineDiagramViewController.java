/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import com.powsybl.sld.svg.DiagramStyleProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
    public ComboBox<DiagramStyleProvider> styleComboBox;

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
    public CheckBox highLightLineStateCheckBox;

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
    public BorderPane selectedDiagram;

    @FXML
    public SingleLineDiagramController selectedDiagramController;

    private final Map<Tab, SingleLineDiagramController> checkedDiagramControllers = new HashMap<>();

    private SingleLineDiagramModel model;

    @FXML
    private void initialize() {
        model = new SingleLineDiagramModel(
                // Providers
                componentLibraryComboBox.valueProperty(),
                styleComboBox.valueProperty(),
                voltageLevelLayoutComboBox.valueProperty(),
                substationLayoutComboBox.valueProperty(),
                cgmesDLDiagramsComboBox.valueProperty(),
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
                highLightLineStateCheckBox.selectedProperty(),
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
        styleComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getStyleProviders()));
        styleComboBox.setConverter(model.getDiagramStyleProviderStringConverter());
        styleComboBox.getSelectionModel().selectFirst(); // Default selection without Network
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
        styleComboBox.valueProperty().addListener(changeListener);
        substationLayoutComboBox.valueProperty().addListener(changeListener);
        voltageLevelLayoutComboBox.valueProperty().addListener(changeListener);
        cgmesDLDiagramsComboBox.valueProperty().addListener(changeListener);

        // PositionVoltageLevelLayoutFactory
        stackFeedersCheckBox.selectedProperty().addListener(changeListener);
        exceptionWhenPatternUnhandledCheckBox.selectedProperty().addListener(changeListener);
        handleShuntsCheckBox.selectedProperty().addListener(changeListener);
        removeFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);
        substituteSingularFictitiousNodesCheckBox.selectedProperty().addListener(changeListener);

        // LayoutParameters
        model.addListener(changeListener);
    }

    public void updateAllDiagrams(Network network, ReadOnlyBooleanProperty showNamesProperty, Container<?> selectedContainer) {
        if (selectedContainer != null) {
            SingleLineDiagramController.updateDiagram(network, showNamesProperty, model, model.getSelectedContainerResult(), selectedContainer);
        }
        model.getCheckedContainerStream().forEach(container -> SingleLineDiagramController.updateDiagram(network, showNamesProperty, model, model.getCheckedContainerResult(container), container));
    }

    public void createDiagram(SingleLineDiagramJsHandler jsHandler, Network network, ReadOnlyBooleanProperty showNamesProperty, Container<?> container) {
        selectedDiagramController.createDiagram(jsHandler, network, showNamesProperty, model, model.getSelectedContainerResult(), container);
    }

    public void clean() {
        checkedTab.getTabs().clear();
        selectedDiagramController.clean();
    }

    public void createCheckedTab(SingleLineDiagramJsHandler jsHandler,
                                 Network network,
                                 ReadOnlyBooleanProperty showNamesProperty,
                                 CheckBoxTreeItem<Container<?>> containerTreeItem,
                                 String tabName) {
        Container<?> container = containerTreeItem.getValue();
        List<Tab> tabList = checkedTab.getTabs();
        if (tabList.stream().map(Tab::getText).noneMatch(tabName::equals)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                Parent diagram = fxmlLoader.load(Objects.requireNonNull(getClass().getResourceAsStream("/sld/SingleLineDiagramView.fxml")));
                SingleLineDiagramController checkedDiagramController = fxmlLoader.getController();
                checkedDiagramController.createDiagram(jsHandler,
                        network,
                        showNamesProperty,
                        model,
                        model.getCheckedContainerResult(container),
                        container);
                Tab newCheckedTab = new Tab(tabName, diagram);
                checkedDiagramControllers.put(newCheckedTab, checkedDiagramController);
                newCheckedTab.setId(container.getId());
                newCheckedTab.setOnClosed(event -> {
                    containerTreeItem.setSelected(false);
                    checkedDiagramControllers.remove(newCheckedTab);
                });
                containerTreeItem.selectedProperty().addListener(getChangeListener(newCheckedTab, containerTreeItem));
                newCheckedTab.setTooltip(new Tooltip(container.getNameOrId()));
                tabList.add(newCheckedTab);
                checkedOrSelected.getSelectionModel().selectLast();
                checkedTab.getSelectionModel().selectLast();
            } catch (IOException e) {
                LOGGER.error(e.toString(), e);
            }
        }
    }

    private ChangeListener<Boolean> getChangeListener(Tab tab, CheckBoxTreeItem<Container<?>> containerTreeItem) {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (Boolean.FALSE.equals(newValue)) {
                    checkedTab.getTabs().remove(tab);
                    containerTreeItem.selectedProperty().removeListener(this);
                    model.removeCheckedContainerResult(containerTreeItem.getValue());
                    checkedDiagramControllers.remove(tab);
                }
            }
        };
    }

    public SingleLineDiagramModel getModel() {
        return model;
    }

    public void updateFrom(final Network network) {
        getModel().updateFrom(network);
        cgmesDLDiagramsComboBox.disableProperty().unbind();
        cgmesDLDiagramsComboBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            boolean cgmesSelected = voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem() instanceof CgmesVoltageLevelLayoutFactory;
            return cgmesSelected && NetworkDiagramData.checkNetworkDiagramData(network);
        }, voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty()).not());

        // Topology selection
        styleComboBox.getSelectionModel().selectLast();
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
