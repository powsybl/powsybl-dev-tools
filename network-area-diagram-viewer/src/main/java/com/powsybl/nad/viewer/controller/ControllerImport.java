/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.controller;

import com.powsybl.iidm.network.Network;
import com.powsybl.nad.viewer.model.NadCalls;
import com.powsybl.nad.viewer.view.ImportBar;
import com.powsybl.nad.viewer.view.OptionsPane;
import com.powsybl.nad.viewer.view.ParamPane;
import com.powsybl.nad.viewer.view.diagram.DiagramPane;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.Preferences;

import static com.powsybl.nad.viewer.model.NadCalls.*;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerImport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerImport.class);
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final ImportBar importBar;
    private final Preferences preferences;
    private File file;
    private final OptionsPane optionsPane;
    private final DiagramPane diagramPane;
    private final ParamPane paramPane;

    public ControllerImport(Stage stage, OptionsPane optionsPane, DiagramPane diagramPane, ParamPane paramPane,
                            Preferences preferences) {
        this.optionsPane = optionsPane;
        this.diagramPane = diagramPane;
        this.paramPane = paramPane;
        this.preferences = preferences;

        this.importBar = new ImportBar();
        addListenerOnImportButton(importBar.getLoadButton(), stage);
    }

    public void loadNetworkFromPreferences() {
        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            loadFile(new File(casePathPropertyValue));
        }
    }

    private void addListenerOnImportButton(Button button, Stage primaryStage) {
        button.setOnAction(event -> {
            LOGGER.info("Import Button OK");
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = preferences.get(CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null) {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            loadFile(fileChooser.showOpenDialog(primaryStage));
        });
    }

    public void loadFile(File file) {
        if (file != null) {
            this.file = file;
            loadNetwork(file.toPath());
            handleLoadingResult(file);
            optionsPane.resetOptions();
        }
    }

    private void handleLoadingResult(File file) {
        networkService.setOnRunning(event -> {
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: yellow");
            importBar.getPathTextField().setText(file.getAbsolutePath());
            preferences.put(CASE_FOLDER_PROPERTY, file.getParent());
            LOGGER.info("Please wait while we try to import the network...");
        });

        networkService.setOnSucceeded(event -> {
            cleanNetwork();
            setNetwork((Network) event.getSource().getValue());
            importBar.getLoadingStatusButton().setStyle("-fx-background-color: green");
            preferences.put(CASE_PATH_PROPERTY, file.getAbsolutePath());
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
    public void cleanNetwork() {
        // Clean Controller Diagram
        diagramPane.resetTabContainers();

        // Clean Controller Options
        optionsPane.clearSubstations();

        // Clean Controller Parameters
        paramPane.setStyleProvider(null);

        // Clean NadCalls
        NadCalls.cleanSvgWriter();
        NadCalls.cleanNetwork();

        LOGGER.info("Cleaning diagram tabs and substations...");
    }

    protected void setNetwork(Network network) {
        networkProperty.set(network);
        optionsPane.setNodesList(network, diagramPane);
    }

    public ImportBar getImportBar() {
        return importBar;
    }

    public File getFile() {
        return file;
    }
}
