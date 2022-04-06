/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view.diagram;
import com.powsybl.ad.viewer.controller.ControllerDiagram;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import static com.powsybl.ad.viewer.model.NadCalls.getSvgWriter;

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
        selectedContainer = new ContainerDiagramPane();
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
    // resetCheckedTabs();
    // resetSelectedTabs();
    }

    public void resetSelectedTabs()
    {
        createSelectedTab();
        //contentSVG = "";
        //selectedTabContainer.getWebEngine().loadContent(contentSVG);
    }

    public void resetCheckedTabs()
    {
        createCheckedTab();
        //contentSVG = "";
        //checkedTabContainer.getWebEngine().loadContent(contentSVG);
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

    public String getContentSVG()
    {
        return contentSVG;
    }

    public static void setContentSVG(String contentSVG) {
        DiagramPane.contentSVG = contentSVG;
    }

}
