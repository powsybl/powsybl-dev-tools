package com.powsybl.ad.viewer.view.diagram;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class DiagramPane extends TabPane
{
    private Tab selectedTab;
    private Tab checkedTab;

    public DiagramPane()
    {
        createSelectedTab();
        createCheckedTab();

        this.getTabs().setAll(selectedTab, checkedTab);
    }

    public void createSelectedTab()
    {
        selectedTab = new Tab("Selected", new ContainerDiagramPane());
        selectedTab.setClosable(false);
    }

    public void createCheckedTab()
    {
        checkedTab = new Tab("Checked", new ContainerDiagramPane());
        checkedTab.setClosable(false);
    }


}
