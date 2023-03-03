/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.nad;

import com.google.common.io.ByteStreams;
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
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAreaDiagramController.class);

    @FXML
    public WebView diagramWebView;
    @FXML
    public TextArea svgContent;
    @FXML
    public TextArea info;

    private String html;
    private String js;

    private final NetworkAreaDiagramJsHandler jsHandler = new NetworkAreaDiagramJsHandler();

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

        html = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/nad/svg.html"))));
        js = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/nad/svg.js"))));
    }

    public void createDiagram(Network network, NetworkAreaDiagramModel model, StringProperty modelSvgContent, Container<?> container) {
        modelSvgContent.addListener((obs, oldV, newV) -> {
            String embeddedSvg = html.replaceFirst("%__SVG__%", newV).replaceFirst("%__JS__%", js);
            diagramWebView.getEngine().loadContent(embeddedSvg);
        });
        svgContent.textProperty().bind(modelSvgContent);

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
}
