/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.DisplaySVG;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.ad.viewer.view.diagram.ContainerDiagramPane;
import javafx.stage.Stage;

import java.io.StringWriter;

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

    public void createDiagramPane()
    {
        diagramPane = new DiagramPane();
    }

    public static void loadNewSVG(StringWriter newSvg)
    {
        diagramPane.addSVG(DisplaySVG.getContentSVG(), newSvg);
    }

    public DiagramPane getDiagramPane()
    {
        return diagramPane;
    }
}
