/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view.diagram;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;

import java.io.StringWriter;
import java.util.ArrayList;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class DiagramPane extends TabPane
{
    private ContainerDiagramPane selectedTabContainer;
    private ContainerDiagramPane checkedTabContainer;

    private ArrayList<String> svgList = new ArrayList<String> ();

    public DiagramPane()
    {
        Tab selectedTab = createSelectedTab();
        Tab checkedTab = createCheckedTab();

        this.getTabs().setAll(selectedTab, checkedTab);
    }

    public Tab createSelectedTab()
    {
        selectedTabContainer = new ContainerDiagramPane(true);
        Tab selectedTab = new Tab("Selected", selectedTabContainer);
        selectedTab.setClosable(false);
        return selectedTab;
    }

    public Tab createCheckedTab()
    {
        checkedTabContainer = new ContainerDiagramPane(true);
        Tab checkedTab = new Tab("Checked", checkedTabContainer);
        checkedTab.setClosable(false);
        return checkedTab;
    }

    public void addSVG(StringWriter svg)
    {
        //svgList.add(svgPath);
        //WebView webView = new WebView();
        //webView.getEngine().load(svgPath);
        // checkedTab.setContent(webView);

        checkedTabContainer.setSVGText("Test checked container svg text");
        selectedTabContainer.setSVGText("Test selected container svg text");

        checkedTabContainer.setSVGInfo("Test checked container svg info");
        selectedTabContainer.setSVGInfo("Test selected container svg info");
    }

    public ContainerDiagramPane getCheckedTab()
    {
        return checkedTabContainer;
    }

    public ContainerDiagramPane getSelectedTab()
    {
        return selectedTabContainer;
    }

}
