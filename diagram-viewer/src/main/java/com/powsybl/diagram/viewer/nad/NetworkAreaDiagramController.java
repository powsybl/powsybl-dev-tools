/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.diagram.viewer.common.ContainerResult;
import com.powsybl.diagram.viewer.common.JsHandler;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Predicate;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramController extends AbstractDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAreaDiagramController.class);

    @FXML
    private void initialize() throws IOException {
        super.init("nad");

        setUpListenerOnWebViewChanges(new JsHandler());
    }

    public void createDiagram(Network network, NetworkAreaDiagramModel model, ContainerResult containerResult, Container<?> container) {
        super.createDiagram(container, containerResult.svgContentProperty());

        updateDiagram(network, model, containerResult, container);
    }

    public static void updateDiagram(Network network, NetworkAreaDiagramModel model, ContainerResult containerResult, Container<?> container) {
        StringWriter writer = new StringWriter();
        Service<String> nadService = new Service<>() {
            @Override
            protected Task<String> createTask() {
                return new Task<>() {
                    @Override
                    protected String call() {
                        Predicate<VoltageLevel> vls = getVoltageLevelFilter(network, model, container);
                        NadParameters nadParameters = new NadParameters();
                        nadParameters.setLayoutParameters(model.getLayoutParameters());
                        nadParameters.setSvgParameters(model.getSvgParameters());
                        NetworkAreaDiagram.draw(network, writer, nadParameters, vls);
                        return writer.toString();
                    }
                };
            }
        };

        nadService.setOnSucceeded(event -> containerResult.svgContentProperty().setValue((String) event.getSource().getValue()));
        nadService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
        });
        nadService.start();
    }

    private static Predicate<VoltageLevel> getVoltageLevelFilter(Network network, NetworkAreaDiagramModel model, Container<?> container) {
        return switch (container.getContainerType()) {
            case NETWORK -> VoltageLevelFilter.NO_FILTER;
            case SUBSTATION -> VoltageLevelFilter.createVoltageLevelsDepthFilter(network, ((Substation) container).getVoltageLevelStream().map(VoltageLevel::getId).toList(), model.getDepth());
            case VOLTAGE_LEVEL -> VoltageLevelFilter.createVoltageLevelDepthFilter(network, container.getId(), model.getDepth());
        };
    }
}
