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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

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
    public Spinner<Integer> nbMaxStepsSpinner;
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
    public NetworkAreaDiagramController selectedDiagramController;

    private NetworkAreaDiagramModel model;

    @FXML
    private void initialize() {
        model = new NetworkAreaDiagramModel(depthSpinner.valueProperty(),
                labelProviderChoice.valueProperty(),
                styleProviderChoice.valueProperty(),
                layoutChoice.valueProperty(),

                springRepulsionSpinner.getValueFactory().valueProperty(),
                nbMaxStepsSpinner.getValueFactory().valueProperty(),
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
            NetworkAreaDiagramController.updateDiagram(network, model, model.getSelectedContainerResult(), selectedContainer);
        }
        model.getCheckedContainerStream().forEach(container -> NetworkAreaDiagramController.updateDiagram(network, model, model.getCheckedContainerResult(container), container));
    }

    public void createDiagram(Network network, Container<?> container) {
        selectedDiagramController.createDiagram(network, model, model.getSelectedContainerResult(), container);
    }

    public void createCheckedTab(Network network,
                                 CheckBoxTreeItem<Container<?>> containerTreeItem,
                                 String tabName) {
        try {
            Container<?> container = containerTreeItem.getValue();
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent diagram = fxmlLoader.load(Objects.requireNonNull(getClass().getResourceAsStream("/nad/networkAreaDiagramView.fxml")));
            NetworkAreaDiagramController checkedDiagramController = fxmlLoader.getController();
            checkedDiagramController.createDiagram(network,
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
