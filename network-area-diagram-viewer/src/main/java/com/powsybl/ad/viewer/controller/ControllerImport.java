/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.controller;
import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.ImportBar;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.iidm.network.Network;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

import static com.powsybl.ad.viewer.model.NadCalls.*;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerImport
{
    private Stage primaryStage;
    private ImportBar importBar;
    private static File file;

    public static File getFile() {
        return file;
    }

    public ControllerImport(Stage stage)
    {
        this.primaryStage = stage;
    }

    public void createImportBar()
    {
        importBar = new ImportBar();
    }

    public void setImportBar()
    {
        addListenerOnImportButton(importBar.getLoadButton(), primaryStage);
    }

    private void addListenerOnImportButton(Button button, Stage primaryStage)
    {
        button.setOnAction(event ->
        {
            Util.loggerControllerImport.info("Import Button OK");
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = Util.preferences.get(Util.CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null)
            {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            file = fileChooser.showOpenDialog(primaryStage);
            if (file != null)
            {
                // load the network corresponding to zip
                loadNetwork(file.toPath());

                // Update loading bar
                handleLoadingResult(file);

                ControllerOptions.resetOptions();
            }
        });
    }

    private void handleLoadingResult(File file)
    {
        networkService.setOnRunning(event -> {
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: yellow");
            importBar.getPathTextField().setText(file.getAbsolutePath());
            Util.preferences.put(Util.CASE_FOLDER_PROPERTY, file.getParent());
            Util.loggerControllerImport.info("Please wait while we try to import the network...");
        });

        networkService.setOnSucceeded(event -> {
            cleanNetwork();
            setNetwork((Network) event.getSource().getValue());
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: green");
            Util.preferences.put(Util.CASE_PATH_PROPERTY, file.getAbsolutePath());
            Util.loggerControllerImport.info("Network imported successfully.");
        });

        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            Util.loggerControllerImport.error(exception.toString(), exception);
            importBar.getPathTextField().setText("");
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: red");
            Util.loggerControllerImport.error("Error when importing network. ");
        });
        networkService.start();
    }


    // Cleans all variables to prepare a new import (substations, SVG..)
    public static void cleanNetwork()
    {
        ControllerDiagram.getDiagramPane().resetTabContainers();
        ControllerOptions.cleanSubstations();
        NadCalls.cleanSvgWriter();
        ControllerParameters.getParamPane().getSvgXSpinner().setDisable(true);
        ControllerParameters.getParamPane().getSvgYSpinner().setDisable(true);
        Util.loggerControllerImport.info("Cleaning diagram tabs and substations...");
    }

    protected void setNetwork(Network network) {
        //closeAllTabs();
        //updateLayoutsFactory(network);
        //updateStylesProvider(network);
        networkProperty.set(network);
        ControllerOptions.setNodesList();
        //setDiagramsNamesContent(network, true);
    }

    public ImportBar getImportBar()
    {
        return importBar;
    }
}
