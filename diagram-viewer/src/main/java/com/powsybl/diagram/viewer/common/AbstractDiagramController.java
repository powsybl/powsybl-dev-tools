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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public abstract class AbstractDiagramController {

    @FXML
    public WebView diagramWebView;

    public final TextArea svgContent = new TextArea();
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

        VBox svgArea = new VBox();
        createArea("SVG file", "*.svg", svgContent, svgArea);
        diagramTabPane.getTabs().add(createNonClosableTab("SVG", svgArea));
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

    protected void createArea(String descrSave, String extensionSave, TextArea textArea, VBox area) {
        TextField searchField = new TextField();
        double wh = 16.0;
        ImageView save = new ImageView("images/save.png");
        save.setFitWidth(wh);
        save.setFitHeight(wh);
        ImageView search = new ImageView("images/search.png");
        search.setFitWidth(wh);
        search.setFitHeight(wh);
        Button searchButton = new Button("Search", search);
        Button saveButton = new Button("Save...", save);
        searchButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        saveButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        searchButton.setTooltip(new Tooltip("Search"));
        saveButton.setTooltip(new Tooltip("Save..."));
        HBox searchBox = new HBox();
        searchBox.setSpacing(5);
        searchBox.setPadding(new Insets(5, 0, 0, 0));
        searchField.setPrefColumnCount(35);
        searchBox.getChildren().add(searchField);
        searchBox.getChildren().add(searchButton);
        searchBox.getChildren().add(saveButton);

        AtomicReference<Integer> searchStart = new AtomicReference<>(0);
        searchStart.set(0);
        searchButton.setOnAction(evh -> {
            String txtPattern = searchField.getText();
            Pattern pattern = Pattern.compile(txtPattern);
            Matcher matcher = pattern.matcher(textArea.getText());
            boolean found = matcher.find(searchStart.get());
            if (found) {
                textArea.selectRange(matcher.start(), matcher.end());
                searchStart.set(matcher.end());
            } else {
                textArea.deselect();
                searchStart.set(0);
                found = matcher.find(searchStart.get());
                if (found) {
                    textArea.selectRange(matcher.start(), matcher.end());
                    searchStart.set(matcher.end());
                }
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                searchStart.set(0)
        );

        saveButton.setOnAction(evh -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(descrSave, extensionSave);
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(area.getScene().getWindow());

            if (file != null) {
                try {
                    PrintWriter writer;
                    writer = new PrintWriter(file);
                    writer.println(textArea.getText());
                    writer.close();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        });

        area.setSpacing(5);
        area.getChildren().add(searchBox);
        area.getChildren().add(textArea);
        VBox.setVgrow(searchBox, Priority.NEVER);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setEditable(false);
    }

    protected Tab createNonClosableTab(String name, Node node) {
        Tab tab = new Tab(name, node);
        tab.setClosable(false);
        return tab;
    }
}
