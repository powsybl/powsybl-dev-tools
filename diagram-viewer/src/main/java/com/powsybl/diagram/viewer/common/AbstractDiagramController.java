/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.*;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public abstract class AbstractDiagramController {

    @FXML
    public WebView diagramWebView;

    private StringProperty svgContent;
    private StringProperty metadataContent;

    @FXML
    public TextArea info;
    @FXML
    protected TabPane diagramTabPane;

    private String html;
    private String js;

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

    protected void init(String prefix) throws IOException {
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

        html = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/" + prefix + "/svg.html"))));
        js = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/" + prefix + "/svg.js"))));

        svgContent = addAdditionalTab("SVG", "SVG file", "*.svg");
        metadataContent = addAdditionalTab("Metadata", "JSON file", "*.json");
    }

    public void createDiagram(Container<?> container,
                              ContainerResult containerResult) {
        info.setText(String.join(System.lineSeparator(), "id: " + container.getId(), "name: " + container.getNameOrId()));

        // SVG content listener & binding
        StringProperty svgContentProperty = containerResult.svgContentProperty();
        svgContentProperty.addListener((obs, oldV, newV) -> updateSVGContent(newV));
        svgContent.bind(svgContentProperty);

        // Metadata binding
        metadataContent.bind(containerResult.metadataContentProperty());
    }

    protected void updateSVGContent(String newContent) {
        String embeddedSvg = html.replaceFirst("%__SVG__%", newContent).replaceFirst("%__JS__%", js);
        diagramWebView.getEngine().loadContent(embeddedSvg);
    }

    public void onClickFitToContent() {
        String svgData = svgContent.get();
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

    protected StringProperty addAdditionalTab(String name, String descrSave, String extensionSave) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Objects.requireNonNull(getClass().getResource("/areaView.fxml")));
        VBox metadataArea = loader.load();
        AreaViewController viewController = loader.getController();
        StringProperty result = viewController.init(descrSave, extensionSave);
        Tab tab = new Tab(name, metadataArea);
        tab.setClosable(false);
        diagramTabPane.getTabs().add(tab);
        return result;
    }
}
