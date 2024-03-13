/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.diagram.viewer.common.AbstractDiagramController;
import com.powsybl.diagram.viewer.common.ContainerResult;
import com.powsybl.iidm.network.*;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.svg.styles.StyleProvider;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * @author Thomas Adam <tadam at slicom.fr>
 */
public class SingleLineDiagramController extends AbstractDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);

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

        if (container instanceof Network) {
            return;
        }
        Service<ContainerResult> sldService = new Service<>() {
            @Override
            protected Task<ContainerResult> createTask() {
                return new Task<>() {
                    @Override
                    protected ContainerResult call() {
                        ContainerResult result = new ContainerResult();
                        try (StringWriter svgWriter = new StringWriter();
                             StringWriter metadataWriter = new StringWriter();
                             StringWriter jsonWriter = new StringWriter()) {

                            SldParameters sldParameters = new SldParameters()
                                    .setLayoutParameters(model.getLayoutParameters())
                                    .setSvgParameters(model.getSvgParameters())
                                    .setComponentLibrary(model.getComponentLibrary())
                                    .setSubstationLayoutFactory(model.getSubstationLayoutFactory())
                                    .setStyleProviderFactory(model::getStyleProvider)
                                    .setVoltageLevelLayoutFactoryCreator(voltageLevelLayoutFactoryCreator);

                            SingleLineDiagram.draw(network, container.getId(),
                                    svgWriter,
                                    metadataWriter,
                                    sldParameters);

                            svgWriter.flush();
                            metadataWriter.flush();
                            result.svgContentProperty().set(svgWriter.toString());
                            result.metadataContentProperty().set(metadataWriter.toString());
                            result.jsonContentProperty().set(jsonWriter.toString());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return result;
                    }
                };
            }
        };

        sldService.setOnSucceeded(event -> containerResult.setValue((ContainerResult) event.getSource().getValue()));
        sldService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
        });
        sldService.start();
    }
}
