/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.DisplaySVG;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.ImportBar;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.powsybl.ad.viewer.model.NadCalls.*;
import static com.powsybl.ad.viewer.view.diagram.DiagramPane.cleanSVG;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerImport
{
    private Stage primaryStage;
    private ImportBar importBar;

    public ControllerImport(Stage stage)
    {
        this.primaryStage = stage;
    }

    public void createImportBar(int size)
    {
        importBar = new ImportBar();
    }

    public void setImportBar(ControllerOptions controllerOptions)
    {
        addListenerOnImportButton(importBar.getLoadButton(), primaryStage);
    }

    private void addListenerOnImportButton(Button button, Stage primaryStage)
    {
        button.setOnAction(event ->
        {
            System.out.print("Import Button OK");
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = Util.preferences.get(Util.CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null)
            {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null)
            {
                loadNetwork(file.toPath());
                ControllerParameters.reselectDefaultChoiceBoxes();  // select first element of parameters when we import something new
                try {
                    cleanSVG();  // clean the window and the variables
                    drawNetwork();  // changes the variable svgWriter
                    DisplaySVG.loadContent(svgWriter.toString());  // changes the variable contentSVG
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ArrayList<String> fakeTestList = new ArrayList<String>();
                fakeTestList.add("Ex1");
                fakeTestList.add("Ex2");
                fakeTestList.add("OK");
                ControllerOptions.setNodesList(fakeTestList);
                // Filters pane
            }
        });
    }

    public ImportBar getImportBar()
    {
        return importBar;
    }
}
