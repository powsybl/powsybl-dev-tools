/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AreaViewController {

    @FXML
    private HBox searchBox;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button searchButton;

    @FXML
    private Button saveButton;

    @FXML
    private TextArea content;

    @FXML
    private void initialize() {
        AtomicReference<Integer> searchStart = new AtomicReference<>(0);
        searchStart.set(0);
        searchButton.setOnAction(evh -> {
            String txtPattern = searchTextField.getText();
            Pattern pattern = Pattern.compile(txtPattern);
            Matcher matcher = pattern.matcher(content.getText());
            boolean found = matcher.find(searchStart.get());
            if (found) {
                content.selectRange(matcher.start(), matcher.end());
                searchStart.set(matcher.end());
            } else {
                content.deselect();
                searchStart.set(0);
                found = matcher.find(searchStart.get());
                if (found) {
                    content.selectRange(matcher.start(), matcher.end());
                    searchStart.set(matcher.end());
                }
            }
        });
        searchTextField.textProperty().addListener((observable, oldValue, newValue) ->
                searchStart.set(0)
        );
        HBox.setHgrow(searchTextField, Priority.ALWAYS);
        VBox.setVgrow(searchBox, Priority.NEVER);
        VBox.setVgrow(content, Priority.ALWAYS);
        content.setEditable(false);
    }

    public StringProperty init(String descrSave, String extensionSave) {
        saveButton.setOnAction(evh -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(descrSave, extensionSave);
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(content.getScene().getWindow());
            if (file != null) {
                try {
                    PrintWriter writer;
                    writer = new PrintWriter(file);
                    writer.println(content.getText());
                    writer.close();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        });
        return content.textProperty();
    }
}
