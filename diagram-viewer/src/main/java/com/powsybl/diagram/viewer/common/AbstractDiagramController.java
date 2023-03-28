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
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public abstract class AbstractDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDiagramController.class);

    @FXML
    public WebView diagramWebView;
    @FXML
    public TextArea svgContent;
    @FXML
    public TextArea info;

    private String html;
    private String js;

    protected abstract void setUpListenerOnWebViewChanges(java.lang.Object jsHandler);

    protected void init() throws IOException {
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

    public void createDiagram(Container<?> container,
                              StringProperty svgContentProperty) {
        info.setText(String.join(System.lineSeparator(), "id: " + container.getId(), "name: " + container.getNameOrId()));

        // SVG content listener & binding
        svgContentProperty.addListener((obs, oldV, newV) -> updateSVGContent(newV));
        svgContent.textProperty().bind(svgContentProperty);
    }

    protected void updateSVGContent(String newContent) {
        String embeddedSvg = html.replaceFirst("%__SVG__%", newContent).replaceFirst("%__JS__%", js);
        diagramWebView.getEngine().loadContent(embeddedSvg);
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
