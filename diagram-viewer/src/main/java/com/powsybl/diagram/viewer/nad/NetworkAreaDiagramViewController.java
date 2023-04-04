/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.diagram.viewer.common.AbstractDiagramViewController;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.svg.SvgParameters;
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
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramViewController extends AbstractDiagramViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAreaDiagramViewController.class);

    @FXML
    public Spinner<Integer> depthSpinner;
    @FXML
    public ChoiceBox<String> labelProviderChoice;
    @FXML
    public ChoiceBox<String> styleProviderChoice;
    @FXML
    public ChoiceBox<String> layoutChoice;

    // Layout parameters
    @FXML
    public Spinner<Double> springRepulsionSpinner;
    @FXML
    public CheckBox layoutIncludeTextNodes;

    // SVG parameters
    @FXML
    public CheckBox infoAlongEdge;
    @FXML
    public CheckBox edgeNameDisplayed;
    @FXML
    public CheckBox insertNameDesc;
    @FXML
    public CheckBox substationDescriptionDisplayed;
    @FXML
    public CheckBox busLegend;
    @FXML
    public CheckBox vlDetails;
    // Diagram size
    @FXML
    public CheckBox widthHeightAdded;
    @FXML
    public ChoiceBox<SvgParameters.SizeConstraint> sizeConstraintChoice;
    @FXML
    public Spinner<Double> fixedSizeSpinner;

    @FXML
    public BorderPane selectedDiagram;

    @FXML
    public NetworkAreaDiagramController selectedDiagramController;

    private final Map<Tab, NetworkAreaDiagramController> checkedDiagramControllers = new HashMap<>();

    private NetworkAreaDiagramModel model;

    @FXML
    private void initialize() {
        model = new NetworkAreaDiagramModel(depthSpinner.valueProperty(),
                labelProviderChoice.valueProperty(),
                styleProviderChoice.valueProperty(),
                layoutChoice.valueProperty(),

                springRepulsionSpinner.getValueFactory().valueProperty(),
                layoutIncludeTextNodes.selectedProperty(),

                infoAlongEdge.selectedProperty(),
                edgeNameDisplayed.selectedProperty(),
                insertNameDesc.selectedProperty(),
                substationDescriptionDisplayed.selectedProperty(),
                busLegend.selectedProperty(),
                vlDetails.selectedProperty(),
                // Diagram size
                widthHeightAdded.selectedProperty(),
                sizeConstraintChoice.valueProperty(),
                fixedSizeSpinner.getValueFactory().valueProperty()
        );
        // Diagram size
        this.sizeConstraintChoice.disableProperty().bind(widthHeightAdded.selectedProperty().not());
        this.fixedSizeSpinner.disableProperty().bind(
                widthHeightAdded.selectedProperty().not()
                        .or(this.sizeConstraintChoice.valueProperty().isEqualTo(SvgParameters.SizeConstraint.NONE))
        );
    }

    public void addListener(ChangeListener<Object> changeListener) {
        depthSpinner.valueProperty().addListener(changeListener);
        labelProviderChoice.valueProperty().addListener(changeListener);
        styleProviderChoice.valueProperty().addListener(changeListener);
        layoutChoice.valueProperty().addListener(changeListener);

        layoutIncludeTextNodes.selectedProperty().addListener(changeListener);
        springRepulsionSpinner.valueProperty().addListener(changeListener);

        // SvgParameters & layoutParameters
        model.addListener(changeListener);
    }

    public void updateAllDiagrams(Network network, Container<?> selectedContainer) {
        if (selectedContainer != null) {
            NetworkAreaDiagramController.updateDiagram(network, model, model.getSvgContent(), selectedContainer);
        }
        model.getCheckedContainerStream().forEach(container -> NetworkAreaDiagramController.updateDiagram(network, model, model.getCheckedSvgContent(container), container));
    }

    public void createDiagram(Network network, Container<?> container) {
        selectedDiagramController.createDiagram(network, model, model.getSvgContent(), container);
    }

    public void clean() {
        checkedTab.getTabs().clear();
    }

    public void createCheckedTab(Network network, CheckBoxTreeItem<Container<?>> containerTreeItem, String tabName) {
        Container<?> container = containerTreeItem.getValue();
        List<Tab> tabList = checkedTab.getTabs();
        if (tabList.stream().map(Tab::getText).noneMatch(tabName::equals)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                Parent diagram = fxmlLoader.load(Objects.requireNonNull(getClass().getResourceAsStream("/nad/networkAreaDiagramView.fxml")));
                NetworkAreaDiagramController checkedDiagramController = fxmlLoader.getController();
                checkedDiagramController.createDiagram(network, model, model.getCheckedSvgContent(container), container);
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
                    model.removeSvgContent(containerTreeItem.getValue());
                    checkedDiagramControllers.remove(tab);
                }
            }
        };
    }

    public NetworkAreaDiagramModel getModel() {
        return model;
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
