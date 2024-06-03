/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.diagram.viewer.DiagramViewer;
import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.diagram.viewer.common.ContainerResult;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.layout.HorizontalZoneLayoutFactory;
import com.powsybl.sld.layout.MatrixZoneLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactoryCreator;
import com.powsybl.sld.layout.ZoneLayoutFactory;
import com.powsybl.sld.svg.styles.StyleProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Thomas Adam <tadam at slicom.fr>
 */
public class SingleLineDiagramController extends AbstractDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);

    private static Service<ContainerResult> sldService;
    private StringProperty metadataContent;
    private StringProperty graphContent;

    @FXML
    private void initialize() throws IOException {
        super.init("sld");

        metadataContent = addAdditionalTab("Metadata", "JSON file", "*.json");
        graphContent = addAdditionalTab("Graph", "JSON file", "*.json");
    }

    public void createDiagram(SingleLineDiagramJsHandler jsHandler,
                              Network network,
                              SingleLineDiagramModel model,
                              ContainerResult containerResult,
                              Container<?> container,
                              VoltageLevelLayoutFactoryCreator voltageLevelLayoutFactoryCreator) {
        super.createDiagram(container, containerResult.svgContentProperty());

        // JSHandler management
        jsHandler.setOperateSwitch(swId -> {
            Switch sw = network.getSwitch(swId);
            if (sw != null) {
                sw.setOpen(!sw.isOpen());
                StyleProvider styleProvider = model.getStyleProvider(network);
                styleProvider.reset();
                updateDiagram(network, model, containerResult, container, voltageLevelLayoutFactoryCreator);
            }
        });
        setUpListenerOnWebViewChanges(jsHandler);
        // Manage cursor look above WebView
        DiagramViewer.getPrimaryStage().getScene().getRoot().cursorProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == diagramWebView.getEngine().getLoadWorker().stateProperty().get()) {
                Document doc = diagramWebView.getEngine().getDocument();
                Element body = (Element)
                        doc.getElementsByTagName("body").item(0);
                String style = body.getAttribute("style");
                body.setAttribute("style", "cursor: " + ((newValue == Cursor.WAIT) ? "wait" : "default") + ";" + style);
            }
        });

        containerResult.metadataContentProperty().addListener((obs, oldV, newV) -> jsHandler.setMetadata(containerResult.metadataContentProperty().get()));

        // Metadata & Graph binding
        metadataContent.bind(containerResult.metadataContentProperty());
        graphContent.bind(containerResult.jsonContentProperty());

        updateDiagram(network, model, containerResult, container, voltageLevelLayoutFactoryCreator);
    }

    public static void updateDiagram(Network network,
                                     SingleLineDiagramModel model,
                                     ContainerResult containerResult,
                                     Container<?> container,
                                     // PositionVoltageLevelLayoutFactory
                                     VoltageLevelLayoutFactoryCreator voltageLevelLayoutFactoryCreator) {
        if (sldService != null && sldService.isRunning()) {
            sldService.cancel();
        }
        sldService = new Service<>() {
            @Override
            protected Task<ContainerResult> createTask() {
                return new Task<>() {
                    @Override
                    protected ContainerResult call() throws IOException {
                        ContainerResult result = new ContainerResult();
                        List<String> substationList = new ArrayList<>();
                        try (StringWriter svgWriter = new StringWriter();
                             StringWriter metadataWriter = new StringWriter();
                             StringWriter jsonWriter = new StringWriter()) {
                            ZoneLayoutFactory zoneLayoutFactory = model.getZoneLayoutFactory();
                            if (model.getZoneLayoutFactory() instanceof MatrixZoneLayoutFactory) {
                                substationList = Arrays.stream(model.getMatrix())
                                        .flatMap(Arrays::stream)
                                        .filter(Predicate.not(String::isEmpty))
                                        .toList();
                                zoneLayoutFactory = substationList.isEmpty() ?
                                        new HorizontalZoneLayoutFactory() : new MatrixZoneLayoutFactory(model.getMatrix());
                            }
                            SldParameters sldParameters = new SldParameters()
                                    .setLayoutParameters(model.getLayoutParameters())
                                    .setSvgParameters(model.getSvgParameters())
                                    .setComponentLibrary(model.getComponentLibrary())
                                    .setSubstationLayoutFactory(model.getSubstationLayoutFactory())
                                    .setStyleProviderFactory(model::getStyleProvider)
                                    .setVoltageLevelLayoutFactoryCreator(voltageLevelLayoutFactoryCreator)
                                    .setZoneLayoutFactory(zoneLayoutFactory);
                            if (container instanceof Network network && !substationList.isEmpty()) {
                                SingleLineDiagram.drawMultiSubstations(network, substationList,
                                        svgWriter,
                                        metadataWriter,
                                        sldParameters);
                            } else {
                                SingleLineDiagram.draw(network, container.getId(),
                                        svgWriter,
                                        metadataWriter,
                                        sldParameters);
                            }
                            svgWriter.flush();
                            metadataWriter.flush();
                            result.svgContentProperty().set(svgWriter.toString());
                            result.metadataContentProperty().set(metadataWriter.toString());
                            result.jsonContentProperty().set(jsonWriter.toString());
                        }
                        return result;
                    }
                };
            }
        };
        // Show waiting cursor during task execution
        DiagramViewer.getPrimaryStage().getScene()
                .getRoot()
                .cursorProperty()
                .bind(Bindings.when(sldService.runningProperty())
                        .then(Cursor.WAIT)
                        .otherwise(Cursor.DEFAULT)
        );
        sldService.setOnCancelled(event -> DiagramViewer.getPrimaryStage().getScene().getRoot().setCursor(Cursor.DEFAULT));
        sldService.setOnSucceeded(event -> containerResult.setValue((ContainerResult) event.getSource().getValue()));
        sldService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            containerResult.clean();
            LOGGER.error("Error while drawing single-line diagram {}", container.getId(), exception);
        });
        sldService.start();
    }
}
