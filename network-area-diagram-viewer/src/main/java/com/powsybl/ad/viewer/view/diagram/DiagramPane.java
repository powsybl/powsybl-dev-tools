/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view.diagram;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerFullNetworkDiagramPane;
import com.powsybl.iidm.network.Container;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import java.util.List;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class DiagramPane extends TabPane
{
    private Tab selectedTab;
    private Tab checkedTab;

    private BorderPane selectedDiagramPane;
    private static ContainerDiagramPane selectedContainer;

    private TabPane checkedDiagramPane;

    private static String contentSVG;

    public DiagramPane()
    {
        createSelectedTab();
        createCheckedTab();

        this.getTabs().setAll(selectedTab, checkedTab);
    }

    private void createSelectedTab()
    {
        selectedContainer = new ContainerFullNetworkDiagramPane();  // will be overwritten with selectedDiagramPane.setCenter()
        selectedDiagramPane = new BorderPane(selectedContainer);
        selectedTab = new Tab("Selected", selectedDiagramPane);
        selectedTab.setClosable(false);
    }

    private void createCheckedTab()
    {
        checkedDiagramPane = new TabPane();
        checkedTab = new Tab("Checked", checkedDiagramPane);
        checkedTab.setClosable(false);
    }

    public void resetTabContainers()
    {
        resetCheckedTabs();
        resetSelectedTabs();
    }

    public void resetSelectedTabs()
    {
        selectedDiagramPane.getChildren().clear();
    }

    public void resetCheckedTabs()
    {
        while (!checkedDiagramPane.getTabs().isEmpty())
        {
            // we call the listener to remove the tab
            EventHandler<Event> handler = checkedDiagramPane.getTabs().get(0).getOnClosed();
            handler.handle(null);
        }
    }

    public void closeTabInCheckedDiagramPane(CheckBoxTreeItem<Container<?>> item) {
        List<Tab> tabList = checkedDiagramPane.getTabs();
        for (Tab checkedTab : tabList) {
            if (checkedTab.getText().compareTo(item.getValue().getName()) == 0)
            {
                // we call the listener to remove the tab
                EventHandler<Event> handler = checkedTab.getOnClosed();
                handler.handle(null);
                // we need to remove the tab because the listener will not
                checkedDiagramPane.getTabs().remove(checkedTab);
                break;
            }
        }
    }

    public TabPane getCheckedDiagramPane()
    {
        return checkedDiagramPane;
    }

    public BorderPane getSelectedDiagramPane()
    {
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

    public String getContentSVG()
    {
        return contentSVG;
    }

    public static void setContentSVG(String contentSVG) {
        DiagramPane.contentSVG = contentSVG;
    }

}
