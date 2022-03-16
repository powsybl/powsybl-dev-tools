/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.view.diagram;

import com.google.common.io.ByteStreams;
import com.powsybl.ad.viewer.model.NadCalls;
import javafx.concurrent.Worker;
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ScrollEvent;
import netscape.javascript.JSObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class DiagramPane extends TabPane
{
    private static ContainerDiagramPane selectedTabContainer;
    private static ContainerDiagramPane checkedTabContainer;

    private static String contentSVG;

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

    public static void cleanSVG() {
        NadCalls.svgWriter = new StringWriter();
    }

    public static void addSVG(StringWriter svg) throws IOException {
        if (svg != null) {
            List<ContainerDiagramPane> ListContainerDiagramPane = new ArrayList<ContainerDiagramPane>();
            ListContainerDiagramPane.add(checkedTabContainer);
            ListContainerDiagramPane.add(selectedTabContainer);

            for (int i = 0; i < ListContainerDiagramPane.size(); i++) {

                int finalI = i;

                //
                // SVG image
                //
                String html = new String(
                        ByteStreams.toByteArray(Objects.requireNonNull(DiagramPane.class.getResourceAsStream("/svg.html")))
                );
                contentSVG = html.replace("%__JS__%", "").replace("%__SVG__%", svg.toString());
                ListContainerDiagramPane.get(finalI).getWebEngine().loadContent(contentSVG);

                // Add Zoom management
                ListContainerDiagramPane.get(finalI).getDiagramView().addEventFilter(ScrollEvent.SCROLL, (ScrollEvent e) -> {
                    if (e.isControlDown()) {
                        double deltaY = e.getDeltaY();
                        double zoom = ListContainerDiagramPane.get(finalI).getDiagramView().getZoom();
                        if (deltaY < 0) {
                            zoom /= 1.1;
                        } else if (deltaY > 0) {
                            zoom *= 1.1;
                        }
                        ListContainerDiagramPane.get(finalI).getDiagramView().setZoom(zoom);
                        e.consume();
                    }
                });

                // Avoid the useless right click on the image
                ListContainerDiagramPane.get(finalI).getDiagramView().setContextMenuEnabled(false);

                // Set up the listener on WebView changes
                // A listener has to be added as loading takes time - execute once the content is successfully loaded
                ListContainerDiagramPane.get(finalI).getWebEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                    if (Worker.State.SUCCEEDED == newValue) {
                        JSObject window = (JSObject) ListContainerDiagramPane.get(finalI).getDiagramView().getEngine().executeScript("window");
                    }
                });
                //
                // SVG string
                //
                ListContainerDiagramPane.get(finalI).setSVGText(svg.toString());

                //
                // SVG info
                //
                ListContainerDiagramPane.get(finalI).setSVGInfo("Test selected container svg info");
            }

    }

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
