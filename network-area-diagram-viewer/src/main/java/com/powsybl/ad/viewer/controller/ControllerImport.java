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
import com.powsybl.iidm.network.Network;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.powsybl.ad.viewer.model.NadCalls.*;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerImport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerImport.class);
    private final Stage primaryStage;
    private static ImportBar importBar;
    private static File file;

    public ControllerImport(Stage stage) {
        this.primaryStage = stage;
    }

    public void createImportBar() {
        importBar = new ImportBar();
    }

    public void setImportBar() {
        addListenerOnImportButton(importBar.getLoadButton(), primaryStage);
    }

    private void addListenerOnImportButton(Button button, Stage primaryStage) {
        button.setOnAction(event -> {
            LOGGER.info("Import Button OK");
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = Util.preferences.get(Util.CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null) {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadFile();

                ControllerOptions.resetOptions();
            }
        });
    }

    public static void loadFile() {
        // load the network corresponding to zip
        loadNetwork(file.toPath());

        // Update loading bar
        handleLoadingResult(file);
    }

    private static void handleLoadingResult(File file) {
        networkService.setOnRunning(event -> {
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: yellow");
            importBar.getPathTextField().setText(file.getAbsolutePath());
            Util.preferences.put(Util.CASE_FOLDER_PROPERTY, file.getParent());
            LOGGER.info("Please wait while we try to import the network...");
        });

        networkService.setOnSucceeded(event -> {
            cleanNetwork();
            setNetwork((Network) event.getSource().getValue());
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: green");
            Util.preferences.put(Util.CASE_PATH_PROPERTY, file.getAbsolutePath());
            LOGGER.info("Network imported successfully.");
        });

        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            importBar.getPathTextField().setText("");
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: red");
            LOGGER.error("Error when importing network. ");
        });
        networkService.start();
    }

    // Cleans all variables to prepare a new import (substations, SVG..)
    public static void cleanNetwork() {
        // Clean Controller Diagram
        ControllerDiagram.getDiagramPane().resetTabContainers();

        // Clean Controller Options
        ControllerOptions.cleanSubstations();

        // Clean Controller Parameters
        ControllerParameters.setStyleProvider(null);
        ControllerParameters.getParamPane().setDisabledSvgSpinners(true);

        // Clean NadCalls
        NadCalls.cleanSvgWriter();
        NadCalls.cleanNetwork();

        LOGGER.info("Cleaning diagram tabs and substations...");
    }

    protected static void setNetwork(Network network) {
        //closeAllTabs();
        //updateLayoutsFactory(network);
        //updateStylesProvider(network);
        networkProperty.set(network);
        ControllerOptions.setNodesList();
        //setDiagramsNamesContent(network, true);
    }

    public ImportBar getImportBar() {
        return importBar;
    }

    public static void setFile(File file) {
        ControllerImport.file = file;
    }

    public static File getFile() {
        return file;
    }
}
