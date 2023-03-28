/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.nad.NetworkAreaDiagram;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramController extends AbstractDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAreaDiagramController.class);

    @FXML
    private void initialize() throws IOException {
        super.init();

        setUpListenerOnWebViewChanges(new NetworkAreaDiagramJsHandler());
    }

    protected void setUpListenerOnWebViewChanges(java.lang.Object jsHandler) {
        // Set up the listener on WebView changes
        // A listener has to be added as loading takes time - execute once the content is successfully loaded
        diagramWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                JSObject window = (JSObject) diagramWebView.getEngine().executeScript("window");
                window.setMember("jsHandler", jsHandler);
                // For easier debugging, redirect the console.log to the jsHandler
                diagramWebView.getEngine().executeScript("console.log = function(message) " +
                        "{ jsHandler.log(message); };");
            }
        });
    }

    public void createDiagram(Network network, NetworkAreaDiagramModel model, StringProperty modelSvgContent, Container<?> container) {
        super.createDiagram(container, modelSvgContent);

        updateDiagram(network, model, modelSvgContent, container);
    }

    public static void updateDiagram(Network network, NetworkAreaDiagramModel model, StringProperty modelSvgContent, Container<?> container) {
        StringWriter writer = new StringWriter();
        Service<String> nadService = new Service<>() {
            @Override
            protected Task<String> createTask() {
                return new Task<>() {
                    @Override
                    protected String call() {
                        NetworkAreaDiagram nad = getNetworkAreaDiagram(network, model, container);
                        nad.draw(writer,
                                model.getSvgParameters(),
                                model.getLayoutParameters(),
                                model.getStyleProvider(network),
                                model.getLabelProvider(network),
                                model.getLayoutFactory());
                        return writer.toString();
                    }
                };
            }
        };

        nadService.setOnSucceeded(event -> modelSvgContent.setValue((String) event.getSource().getValue()));
        nadService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
        });
        nadService.start();
    }

    private static NetworkAreaDiagram getNetworkAreaDiagram(Network network, NetworkAreaDiagramModel model, Container<?> container) {
        switch (container.getContainerType()) {
            case NETWORK:
                return new NetworkAreaDiagram((Network) container);
            case SUBSTATION:
                List<String> vls = ((Substation) container).getVoltageLevelStream().map(VoltageLevel::getId).collect(Collectors.toList());
                return new NetworkAreaDiagram(network, vls, model.getDepth());
            case VOLTAGE_LEVEL:
                return new NetworkAreaDiagram(network, container.getId(), model.getDepth());
            default:
                throw new AssertionError();
        }
    }
}
