/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.nad.viewer.view.diagram;

import com.google.common.io.ByteStreams;
import com.powsybl.nad.viewer.model.NadCalls;
import com.powsybl.nad.viewer.view.OptionsPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerFullNetworkDiagramPane;
import com.powsybl.iidm.network.Container;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerSubstationDiagramPane;
import com.powsybl.nad.viewer.view.diagram.containers.ContainerVoltageDiagramPane;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.powsybl.nad.viewer.model.NadCalls.getSvgWriter;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class DiagramPane extends TabPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagramPane.class);

    private final OptionsPane optionsPane;

    private Tab selectedTab;
    private Tab checkedTab;

    private BorderPane selectedDiagramPane;
    private static ContainerDiagramPane selectedContainer;

    private TabPane checkedDiagramPane;

    private static String contentSVG;

    public DiagramPane(OptionsPane optionsPane) {
        this.optionsPane = optionsPane;
        createSelectedTab();
        createCheckedTab();

        this.getTabs().setAll(selectedTab, checkedTab);
    }

    private void createSelectedTab() {
        selectedContainer = new ContainerFullNetworkDiagramPane();  // will be overwritten with selectedDiagramPane.setCenter()
        selectedDiagramPane = new BorderPane(selectedContainer);
        selectedTab = new Tab("Selected", selectedDiagramPane);
        selectedTab.setClosable(false);
    }

    private void createCheckedTab() {
        checkedDiagramPane = new TabPane();
        checkedTab = new Tab("Checked", checkedDiagramPane);
        checkedTab.setClosable(false);
    }

    public void resetTabContainers() {
        resetCheckedTabs();
        resetSelectedTabs();
    }

    public void resetSelectedTabs() {
        selectedDiagramPane.getChildren().clear();
    }

    public void resetCheckedTabs() {
        while (!checkedDiagramPane.getTabs().isEmpty()) {
            // we call the listener to remove the tab
            EventHandler<Event> handler = checkedDiagramPane.getTabs().get(0).getOnClosed();
            handler.handle(null);
        }
    }

    public void closeTabInCheckedDiagramPane(CheckBoxTreeItem<Container<?>> item) {
        List<Tab> tabList = checkedDiagramPane.getTabs();
        for (Tab checkedTab : tabList) {
            if (checkedTab.getText().compareTo(item.getValue().getName()) == 0) {
                // we call the listener to remove the tab
                EventHandler<Event> handler = checkedTab.getOnClosed();
                handler.handle(null);
                // we need to remove the tab because the listener will not
                checkedDiagramPane.getTabs().remove(checkedTab);
                break;
            }
        }
    }

    public void redrawSelectedTabSVG() {
        // Full Network - Selected Tab
        try {
            NadCalls.drawNetwork();
            addSvgToSelectedTab();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawCheckedTabSVG(String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Full Network - Checked Tab
        try {
            NadCalls.drawNetwork();
            addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawSelectedTabSVG(List<String> voltageLevelIds, int depth) {
        // Substation - Selected Tab
        try {
            NadCalls.drawUniqueSubstation(voltageLevelIds, depth);
            addSvgToSelectedTab(voltageLevelIds, depth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawCheckedTabSVG(List<String> voltageLevelIds, int depth, String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Substation - Checked Tab
        try {
            NadCalls.drawUniqueSubstation(voltageLevelIds, depth);
            addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, voltageLevelIds, depth, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawSelectedTabSVG(String voltageLevelId, int depth) {
        // Voltage (= Subgraph) - Selected Tab
        try {
            NadCalls.drawSubgraph(voltageLevelId, depth);
            addSvgToSelectedTab(voltageLevelId, depth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawCheckedTabSVG(String voltageLevelId, int depth, String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) {
        // Voltage (= Subgraph) - Checked Tab
        try {
            NadCalls.drawSubgraph(voltageLevelId, depth);
            addSvgToCheckedTab(tabName, whatIsGonnaBeDisplayedWhenHoveringOnTabName, voltageLevelId, depth, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TabPane getCheckedDiagramPane() {
        return checkedDiagramPane;
    }

    public BorderPane getSelectedDiagramPane() {
        return selectedDiagramPane;
    }

    public Tab getTabSelectedByUser() {
        return this.getSelectionModel().getSelectedItem();
    }

    public Tab getCheckedTabSelectedByUser() {
        return checkedDiagramPane.getSelectionModel().getSelectedItem();
    }

    public void setCheckedTabSelectedByUser(int indexOfTabToSelect) {
        checkedDiagramPane.getSelectionModel().select(indexOfTabToSelect);
    }

    public String getContentSVG() {
        return contentSVG;
    }

    public static void setContentSVG(String contentSVG) {
        DiagramPane.contentSVG = contentSVG;
    }

    public void addSvgToSelectedTab() throws IOException {
        // Full Network
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerFullNetworkDiagramPane();
        addSVGToOneTab(selectedContainerDiagramPane);
        getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public void addSvgToSelectedTab(List<String> voltageLevelIds, int depth) throws IOException {
        // Substation
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerSubstationDiagramPane(voltageLevelIds, depth);
        addSVGToOneTab(selectedContainerDiagramPane);
        getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public void addSvgToSelectedTab(String voltageLevelId, int depth) throws IOException {
        // Voltage (= Subgraph)
        ContainerDiagramPane selectedContainerDiagramPane = new ContainerVoltageDiagramPane(voltageLevelId, depth);
        addSVGToOneTab(selectedContainerDiagramPane);
        getSelectedDiagramPane().setCenter(selectedContainerDiagramPane);
    }

    public void addSvgToCheckedTab(String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, int index) throws IOException {
        // FullNetwork SVG
        List<Tab> tabList = getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            LOGGER.warn("{} already in list of opened Tabs.", tabName);
        } else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerFullNetworkDiagramPane();
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
    }

    public void addSvgToCheckedTab(String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, List<String> voltageLevelIds, int depth, int index) throws IOException {
        // Substation SVG
        List<Tab> tabList = getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            LOGGER.warn("{} already in list of opened Tabs.", tabName);
        } else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerSubstationDiagramPane(voltageLevelIds, depth);
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
    }

    public void addSvgToCheckedTab(String tabName, String whatIsGonnaBeDisplayedWhenHoveringOnTabName, String voltageLevelId, int depth, int index) throws IOException {
        // Voltage (= Subgraph) SVG
        List<Tab> tabList = getCheckedDiagramPane().getTabs();
        if (tabList.stream().map(Tab::getText).collect(Collectors.toList()).contains(tabName)) {
            LOGGER.warn("{} already in list of opened Tabs.", tabName);
        } else {
            ContainerDiagramPane checkedContainerDiagramPane;
            checkedContainerDiagramPane = new ContainerVoltageDiagramPane(voltageLevelId, depth);
            addSVGToOneTab(checkedContainerDiagramPane);
            Tab newCheckedTab = new Tab(tabName, checkedContainerDiagramPane);
            getCheckedDiagramPane().getTabs().add(index, newCheckedTab);
            addListenerOnClosingTab(newCheckedTab);
            newCheckedTab.setTooltip(new Tooltip(whatIsGonnaBeDisplayedWhenHoveringOnTabName));
        }
    }

    private void addListenerOnClosingTab(Tab tab) {
        tab.setOnClosed(arg0 -> {
            // The checkbox to uncheck can either be 'Full Network', or a 'Substation' or a 'VoltageLevel'
            if (tab.getText().equals("Full Network")) {
                optionsPane.getFullNetworkCheck().setSelected(false);
            } else {
                optionsPane.checkvItemTree(tab.getText(), false);
                optionsPane.checksItemTree(tab.getText(), false);
            }
        });
    }

    private void addSVGToOneTab(ContainerDiagramPane containerDiagramPane) throws IOException {
        // SVG image
        String html = new String(ByteStreams.toByteArray(Objects.requireNonNull(DiagramPane.class.getResourceAsStream("/svg.html"))));
        DiagramPane.setContentSVG(html.replace("%__JS__%", "").replace("%__SVG__%", getSvgWriter().toString()));
        containerDiagramPane.getWebEngine().loadContent(getContentSVG());

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

        // SVG string
        containerDiagramPane.setSVGText(getSvgWriter().toString());

        // SVG info
        containerDiagramPane.setSVGInfo("Test selected container svg info");
    }

    public ObservableList<Tab> getListCheckedTabs() {
        return getCheckedDiagramPane().getTabs();
    }

    public int getIndexCheckedTabSelectedByUser() {
        ObservableList<Tab> listCheckedTabs = getListCheckedTabs();
        return listCheckedTabs.indexOf(getCheckedTabSelectedByUser());
    }
}
