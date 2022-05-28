/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.google.common.io.ByteStreams;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerFullNetworkDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerSubstationDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerVoltageDiagramPane;
import javafx.scene.control.*;
import javafx.concurrent.Worker;
import javafx.scene.input.ScrollEvent;
import netscape.javascript.JSObject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.powsybl.ad.viewer.controller.ControllerOptions.getOptionsPane;
import static com.powsybl.ad.viewer.model.NadCalls.getSvgWriter;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerDiagram
{
    private static DiagramPane diagramPane;

    public static void addSvgToSelectedTab() throws IOException
    {
        // Full Network
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerFullNetworkDiagramPane();
        addSVGToOneTab(selectedContainerDiagramPane);
        diagramPane.getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public static void addSvgToSelectedTab(List<String> voltageLevelIds, int depth) throws IOException
    {
        // Substation
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerSubstationDiagramPane(voltageLevelIds, depth);
        addSVGToOneTab(selectedContainerDiagramPane);
        diagramPane.getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public static void addSvgToSelectedTab(String voltageLevelId, int depth) throws IOException
    {
        // Voltage (= Subgraph)
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerVoltageDiagramPane(voltageLevelId, depth);
        addSVGToOneTab(selectedContainerDiagramPane);
        diagramPane.getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public static void addSvgToCheckedTab(
            String tabName,
            String whatIsGonnaBeDisplayedWhenHoveringOnTabName,
            int index
    )  throws IOException  // FullNetwork SVG
    {
        List<Tab> tabList = diagramPane.getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            Util.loggerControllerDiagram.error(tabName + " already in list of opened Tabs.");
        }
        else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerFullNetworkDiagramPane();
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            diagramPane.getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
    }

    public static void addSvgToCheckedTab(
            String tabName,
            String whatIsGonnaBeDisplayedWhenHoveringOnTabName,
            List<String> voltageLevelIds,
            int depth,
            int index
    )  throws IOException  // Substation SVG
    {
        List<Tab> tabList = diagramPane.getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            Util.loggerControllerDiagram.error(tabName + " already in list of opened Tabs.");
        }
        else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerSubstationDiagramPane(voltageLevelIds, depth);
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            diagramPane.getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
    }

    public static void addSvgToCheckedTab(
            String tabName,
            String whatIsGonnaBeDisplayedWhenHoveringOnTabName,
            String voltageLevelId,
            int depth,
            int index
    )  throws IOException  // Voltage (= Subgraph) SVG
    {
        List<Tab> tabList = diagramPane.getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            Util.loggerControllerDiagram.error(tabName + " already in list of opened Tabs.");
        }
        else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerVoltageDiagramPane(voltageLevelId, depth);
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            diagramPane.getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
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

    private static void addListenerOnClosingTab(Tab tab) {
        tab.setOnClosed(arg0 -> {
            Util.loggerControllerDiagram.info("Tab " + tab.getText() + " closed.");
            // The checkbox to uncheck can either be 'Full Network', or a 'Substation' or a 'VoltageLevel'
            if (tab.getText().equals("Full Network")) {
                getOptionsPane().getFullNetworkCheck().setSelected(false);
            }
            else {
                ControllerOptions.checkvItemTree(tab.getText(), false);
                ControllerOptions.checksItemTree(tab.getText(), false);
            }
        });
    }
}
