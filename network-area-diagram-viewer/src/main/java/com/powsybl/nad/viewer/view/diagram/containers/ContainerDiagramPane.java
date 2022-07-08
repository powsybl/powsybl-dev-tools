/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.view.diagram.containers;

import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public abstract class ContainerDiagramPane extends BorderPane {
    private TitledPane infoPane;
    private TextArea infoArea;

    private TabPane tabPane;

    private Tab diagramTab;
    private final WebView diagramView = new WebView();
    private final WebEngine webEngine = diagramView.getEngine();
    // SVG source code
    private Tab svgTab;

    private TextArea svgTextArea;

    public ContainerDiagramPane() {
        createInfoPane();

        createTabPane();

        this.setCenter(tabPane);
        this.setBottom(infoPane);
    }

    private void createTabPane() {
        createDiagramTab();
        createSVGTab();
        tabPane = new TabPane(diagramTab, svgTab);
        tabPane.setSide(Side.BOTTOM);
    }

    private void createSVGTab() {
        svgTextArea = new TextArea("");
        svgTextArea.setEditable(false);
        svgTab = new Tab("SVG", svgTextArea);
        svgTab.setClosable(false);
    }

    private void createDiagramTab() {
        diagramTab = new Tab("Diagram", diagramView);
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

    public TextArea getSvgTextArea() {
        return svgTextArea;
    }

    public void setSVGText(String newSvgText) {
        svgTextArea.setText(newSvgText);
    }

    public void setSVGInfo(String newSvgInfo) {
        infoArea.setText(newSvgInfo);
    }

    public WebView getDiagramView() {
        return diagramView;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

}
