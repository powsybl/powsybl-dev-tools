/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.view.diagram;

import com.google.common.io.ByteStreams;
import com.powsybl.ad.viewer.util.JsHandler;
import com.powsybl.iidm.network.Switch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.Objects;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ContainerDiagramPane extends BorderPane
{
    // Components for diagramPane
    final WebView diagramView = new WebView();
    final WebEngine webEngine = diagramView.getEngine();
    // For communication from the Javascript engine.
//    private final JsHandler jsHandler;

    /* private final TextField svgSearchField = new TextField();
    private final Button svgSearchButton = new Button("Search");
    private final TextArea svgTextArea = new TextArea();
    private AtomicReference <Integer> svgSearchStart = new AtomicReference<>(0);
    private final Button svgSaveButton = new Button("Save"); */

    private TitledPane infoPane;
    private TextArea infoArea;

    private TabPane diagramTabPane;
    private Tab diagramTab;
    private ScrollPane scrollPane = new ScrollPane();

    private Tab svgTab;
    private TextArea svgTextArea;

    //private final ChangeListener<LayoutParameters> listener;

    public ContainerDiagramPane(boolean selected) {
        createInfoPane();

        createDiagramPane();

        this.setCenter(diagramTabPane);
        this.setBottom(infoPane);
    }

    private void createDiagramPane() {
        createDiagramTab();
        createSVGTab();

        diagramTabPane = new TabPane();
        diagramTabPane.getTabs().addAll(diagramTab, svgTab);
    }

    private void createSVGTab()
    {
        svgTextArea = new TextArea("");
        svgTextArea.setEditable(false);
        svgTab = new Tab("SVG", svgTextArea);
        svgTab.setClosable(false);
    }

    private void createDiagramTab()
    {
        diagramTab = new Tab("Diagram");
        scrollPane.setContent(diagramView);
        diagramTab.setContent(scrollPane);
        diagramTab.setClosable(false);
    }

    private void createInfoPane() {
        infoArea = new TextArea();
        infoArea.setEditable(false);

        infoPane = new TitledPane("Voltage Level Infos", infoArea);
    }

    public TextArea getInfoArea() {
        return infoArea;
    }

    public void setSVGText(String newSvgText) { svgTextArea.setText(newSvgText); }

    public void setSVGInfo(String newSvgInfo) { infoArea.setText(newSvgInfo); }
}
