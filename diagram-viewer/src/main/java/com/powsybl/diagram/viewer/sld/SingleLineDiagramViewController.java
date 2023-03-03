/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.layout.SubstationLayoutFactory;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.DiagramStyleProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SingleLineDiagramViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramViewController.class);

    @FXML
    public ComboBox<ComponentLibrary> componentLibraryChoice;

    @FXML
    public ComboBox<DiagramStyleProvider> styleChoice;

    @FXML
    public ComboBox<SubstationLayoutFactory> substationLayoutChoice;

    @FXML
    public ComboBox<com.powsybl.sld.layout.VoltageLevelLayoutFactory> voltageLevelLayoutChoice;

    @FXML
    public ComboBox<String> cgmesDLDiagramsChoice;

    // Parameters
    @FXML
    public Spinner<Double> diagramPaddingTopBottomSpinner;

    @FXML
    public Spinner<Double> diagramPaddingLeftRightSpinner;

    @FXML
    public TabPane checkedTab;
    @FXML
    public BorderPane selectedDiagram;
    @FXML
    public TabPane checkedOrSelected;

    @FXML
    private SingleLineDiagramController selectedDiagramController;

    private final Map<Tab, SingleLineDiagramController> checkedDiagramControllers = new HashMap<>();

    private SingleLineDiagramModel model;

    @FXML
    private void initialize() {
        model = new SingleLineDiagramModel(componentLibraryChoice.valueProperty(),
                styleChoice.valueProperty(),
                voltageLevelLayoutChoice.valueProperty(),
                substationLayoutChoice.valueProperty(),
                cgmesDLDiagramsChoice.valueProperty(),
                diagramPaddingTopBottomSpinner.valueProperty(),
                diagramPaddingLeftRightSpinner.valueProperty());

        // Component library
        componentLibraryChoice.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getComponentLibraries()));
        componentLibraryChoice.setConverter(model.getComponentLibraryStringConverter());
        componentLibraryChoice.getSelectionModel().selectFirst();
        // Style provider
        styleChoice.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getStyleProviders()));
        styleChoice.setConverter(model.getDiagramStyleProviderStringConverter());
        styleChoice.getSelectionModel().selectFirst();
        // Substation layout
        substationLayoutChoice.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getSubstationLayouts()));
        substationLayoutChoice.setConverter(model.getSubstationLayoutStringConverter());
        substationLayoutChoice.getSelectionModel().selectFirst();
        // VoltageLevel layout
        voltageLevelLayoutChoice.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getVoltageLevelLayouts()));
        voltageLevelLayoutChoice.getSelectionModel().selectFirst();
        voltageLevelLayoutChoice.setConverter(model.getVoltageLevelLayoutFactoryStringConverter());
        // CGMES-DL Diagrams
        cgmesDLDiagramsChoice.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getCgmesDLDiagramNames()));
        cgmesDLDiagramsChoice.getSelectionModel().selectFirst();
    }

    public void addListener(ChangeListener<Object> changeListener) {
        componentLibraryChoice.valueProperty().addListener(changeListener);
        styleChoice.valueProperty().addListener(changeListener);
        substationLayoutChoice.valueProperty().addListener(changeListener);
        voltageLevelLayoutChoice.valueProperty().addListener(changeListener);
        cgmesDLDiagramsChoice.valueProperty().addListener(changeListener);

        diagramPaddingTopBottomSpinner.valueProperty().addListener(changeListener);
        diagramPaddingLeftRightSpinner.valueProperty().addListener(changeListener);
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

    @FXML
    public void onClickFitToContent(MouseEvent mouseEvent) {
        getActiveTabController().onClickFitToContent();
    }

    @FXML
    public void onClickResetZoom(MouseEvent mouseEvent) {
        getActiveTabController().onClickResetZoom();
    }

    private SingleLineDiagramController getActiveTabController() {
        Tab tab = checkedOrSelected.getSelectionModel().getSelectedItem();
        if ("Selected".equals(tab.getText())) {
            return selectedDiagramController;
        } else {
            Tab tabInChecked = checkedTab.getSelectionModel().getSelectedItem();
            return checkedDiagramControllers.get(tabInChecked);
        }
    }

    public SingleLineDiagramModel getModel() {
        return model;
    }
}
