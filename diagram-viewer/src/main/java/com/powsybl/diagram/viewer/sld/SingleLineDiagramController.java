/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.sld;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.*;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DiagramLabelProvider;
import com.powsybl.sld.svg.DiagramStyleProvider;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Adam <tadam at slicom.fr>
 */
public class SingleLineDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);

    @FXML
    public WebView diagramWebView;

    @FXML
    public TextArea svgContent;

    @FXML
    public TextArea metadataContent;

    @FXML
    public TextArea graphContent;

    @FXML
    public TextArea info;

    private String html;

    private String js;

    @FXML
    private void initialize() throws IOException {
        // Add Zoom management
        diagramWebView.addEventFilter(ScrollEvent.SCROLL, (ScrollEvent e) -> {
            if (e.isControlDown()) {
                double deltaY = e.getDeltaY();
                double zoom = diagramWebView.getZoom();
                if (deltaY < 0) {
                    zoom /= 1.1;
                } else if (deltaY > 0) {
                    zoom *= 1.1;
                }
                diagramWebView.setZoom(zoom);
                e.consume();
            }
        });

        // Avoid the useless right click on the image
        diagramWebView.setContextMenuEnabled(false);

        html = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/sld/svg.html"))));
        js = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/sld/svg.js"))));
    }

    public void createDiagram(SingleLineDiagramJsHandler jsHandler,
                              Network network,
                              ReadOnlyBooleanProperty showNamesProperty,
                              SingleLineDiagramModel model,
                              SingleLineDiagramModel.ContainerResult containerResult,
                              Container<?> container) {
        info.setText(String.join(System.lineSeparator(), "id: " + container.getId(), "name: " + container.getNameOrId()));

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

        jsHandler.setOperateSwitch(swId -> {
            Switch sw = network.getSwitch(swId);
            if (sw != null) {
                sw.setOpen(!sw.isOpen());
                DiagramStyleProvider styleProvider = model.getStyleProvider();
                styleProvider.reset();
                updateDiagram(network, showNamesProperty, model, containerResult, container);
            }
        });

        containerResult.svgContentProperty().addListener((obs, oldV, newV) -> {
            String embeddedSvg = html.replaceFirst("%__SVG__%", newV).replaceFirst("%__JS__%", js);
            diagramWebView.getEngine().loadContent(embeddedSvg);
        });
        containerResult.metadataContentProperty().addListener((obs, oldV, newV) -> {
            jsHandler.setMetadata(containerResult.metadataContentProperty().get());
        });
        svgContent.textProperty().bind(containerResult.svgContentProperty());
        metadataContent.textProperty().bind(containerResult.metadataContentProperty());
        graphContent.textProperty().bind(containerResult.jsonContentProperty());

        updateDiagram(network, showNamesProperty, model, containerResult, container);
    }

    public static void updateDiagram(Network network,
                                     ReadOnlyBooleanProperty showNamesProperty,
                                     SingleLineDiagramModel model,
                                     SingleLineDiagramModel.ContainerResult containerResult,
                                     Container<?> container) {
        Service<SingleLineDiagramModel.ContainerResult> sldService = new Service<>() {
            @Override
            protected Task<SingleLineDiagramModel.ContainerResult> createTask() {
                return new Task<>() {
                    @Override
                    protected SingleLineDiagramModel.ContainerResult call() {
                        SingleLineDiagramModel.ContainerResult result = new SingleLineDiagramModel.ContainerResult();
                        try (StringWriter svgWriter = new StringWriter();
                             StringWriter metadataWriter = new StringWriter();
                             StringWriter jsonWriter = new StringWriter()) {

                            DiagramStyleProvider styleProvider = model.getStyleProvider();
                            LayoutParameters diagramLayoutParameters = model.getLayoutParameters(showNamesProperty.get());

                            DiagramLabelProvider initProvider = new DefaultDiagramLabelProvider(network,
                                    model.getComponentLibrary(),
                                    diagramLayoutParameters);

                            SingleLineDiagram.draw(network, container.getId(),
                                    svgWriter,
                                    metadataWriter,
                                    diagramLayoutParameters,
                                    model.getComponentLibrary(),
                                    model.getSubstationLayoutFactory(),
                                    model.getVoltageLevelLayoutFactory(),
                                    initProvider,
                                    styleProvider,
                                    "");

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

        sldService.setOnSucceeded(event -> containerResult.setValue((SingleLineDiagramModel.ContainerResult) event.getSource().getValue()));
        sldService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
        });
        sldService.start();
    }

    public void onClickFitToContent() {
        String svgData = svgContent.getText();
        Optional<String> svgLine = svgData.lines().filter(l -> l.contains("<svg")).findAny();
        if (svgLine.isPresent()) {
            String valuePattern = "\"([^\"]*)\"";
            Pattern pW = Pattern.compile("width=" + valuePattern);
            Matcher mW = pW.matcher(svgLine.get());
            Pattern pH = Pattern.compile("height=" + valuePattern);
            Matcher mH = pH.matcher(svgLine.get());
            if (mH.find() && mW.find()) {
                double svgWidth = Double.parseDouble(mW.group(1));
                double svgHeight = Double.parseDouble(mH.group(1));
                double paneWidth = diagramWebView.widthProperty().get();
                double paneHeight = diagramWebView.heightProperty().get();
                double zoomH = paneHeight / svgHeight;
                double zoomW = paneWidth / svgWidth;
                diagramWebView.setZoom(Math.min(zoomH, zoomW));
            }
        }
    }

    public void onClickResetZoom() {
        diagramWebView.setZoom(1.0);
    }

    public void clean() {
        info.setText("");
    }
}
