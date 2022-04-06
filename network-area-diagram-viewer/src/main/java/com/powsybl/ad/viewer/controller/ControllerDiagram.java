/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.google.common.io.ByteStreams;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import javafx.scene.control.Tab;
import javafx.concurrent.Worker;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.Objects;

import static com.powsybl.ad.viewer.model.NadCalls.getSvgWriter;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerDiagram
{
    private Stage primaryStage;

    private static DiagramPane diagramPane;

    public ControllerDiagram(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
    }

    public static void addSvgToSelectedTab() throws IOException
    {
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerDiagramPane();
        addSVGToOneTab(selectedContainerDiagramPane);
        diagramPane.getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public static void addSvgToCheckedTab()  throws IOException
    {
        ContainerDiagramPane checkedContainerDiagramPane = new ContainerDiagramPane();
        addSVGToOneTab(checkedContainerDiagramPane);
        Tab newCheckedTab = new Tab("id", checkedContainerDiagramPane);
        diagramPane.getCheckedDiagramPane().getTabs().add(newCheckedTab);
    }

    public void createDiagramPane()
    {
        diagramPane = new DiagramPane();
    }

    private static void addSVGToOneTab(ContainerDiagramPane containerDiagramPane) throws IOException {
        //
        // SVG image
        //
        String html = new String(
                ByteStreams.toByteArray(Objects.requireNonNull(DiagramPane.class.getResourceAsStream("/svg.html")))
        );
        DiagramPane.setContentSVG(html.replace("%__JS__%", "").replace("%__SVG__%", getSvgWriter().toString()));
        containerDiagramPane.getWebEngine().loadContent(diagramPane.getContentSVG());

        // Add Zoom management
        containerDiagramPane.getDiagramView().addEventFilter(ScrollEvent.SCROLL, (ScrollEvent e) -> {
            if (e.isControlDown()) {
                double deltaY = e.getDeltaY();
                double zoom = containerDiagramPane.getDiagramView().getZoom();
                if (deltaY < 0) {
                    zoom /= 1.1;
                } else if (deltaY > 0) {
                    zoom *= 1.1;
                }
                containerDiagramPane.getDiagramView().setZoom(zoom);
                e.consume();
            }
        });

        // Avoid the useless right click on the image
        containerDiagramPane.getDiagramView().setContextMenuEnabled(false);

        // Set up the listener on WebView changes
        // A listener has to be added as loading takes time - execute once the content is successfully loaded
        containerDiagramPane.getWebEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                JSObject window = (JSObject) containerDiagramPane.getDiagramView().getEngine().executeScript("window");
            }
        });
        //
        // SVG string
        //
        containerDiagramPane.setSVGText(getSvgWriter().toString());

        //
        // SVG info
        //
        containerDiagramPane.setSVGInfo("Test selected container svg info");
    }

    public static DiagramPane getDiagramPane()
    {
        return diagramPane;
    }
}
