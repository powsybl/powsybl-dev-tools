/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.view.diagram;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ContainerDiagramPane extends BorderPane
{
    // Components for diagramPane
    /* private final WebView diagramView = new WebView();
    private final TextField svgSearchField = new TextField();
    private final Button svgSearchButton = new Button("Search");
    private final TextArea svgTextArea = new TextArea();
    private AtomicReference <Integer> svgSearchStart = new AtomicReference<>(0);
    private final Button svgSaveButton = new Button("Save"); */

    private TitledPane infoPane;
    private TextArea infoArea;

    private TabPane diagramTabPane;
    private Tab diagramTab;

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
        svgTab = new Tab("SVG", svgTextArea);
        svgTab.setClosable(false);
    }

    private void createDiagramTab()
    {
        diagramTab = new Tab("Diagram");
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