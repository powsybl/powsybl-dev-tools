/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powerfactory.viewer;

import com.powsybl.powerfactory.model.Project;
import com.powsybl.powerfactory.model.ProjectLoader;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PowerFactoryViewer extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(PowerFactoryViewer.class);

    private static final String FILE_PATH_PROPERTY = "filePath";

    private final TextField filePathTextField = new TextField();

    private final FileChooser fileChooser = new FileChooser();

    private final DataObjectTree dataObjectTree = new DataObjectTree();

    private final DataAttributeTable dataAttributeTable = new DataAttributeTable(dataObjectTree);

    private final Preferences preferences = Preferences.userNodeForPackage(DataObjectTree.class);

    private final BorderPane mainPane = new BorderPane();

    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    private final Group loader = new Group(progressIndicator);

    private Service<Project> service;

    private void loadFileFromPreferences() {
        String pathPropertyValue = preferences.get(FILE_PATH_PROPERTY, null);
        if (pathPropertyValue != null) {
            loadFile(Paths.get(pathPropertyValue));
        }
    }

    private void showLoader() {
        loader.setVisible(true);
        mainPane.setDisable(true);
    }

    private void hideLoader() {
        loader.setVisible(false);
        mainPane.setDisable(false);
    }

    private void loadFile(Path file) {
        service = new Service<Project>() {
            @Override
            protected Task<Project> createTask() {
                return new Task<Project>() {
                    @Override
                    protected Project call() {
                        return ProjectLoader.load(file).orElse(null);
                    }
                };
            }
        };

        service.setOnScheduled(event -> {
            dataObjectTree.setProject(null);
            showLoader();
        });

        service.setOnSucceeded(event -> {
            Project project = (Project) event.getSource().getValue();
            dataObjectTree.setProject(project);
            hideLoader();
        });

        service.setOnFailed(event -> LOGGER.error(event.getSource().getException().toString(), event.getSource().getException()));

        service.start();

        filePathTextField.setText(file.toAbsolutePath().toString());
    }

    @Override
    public void start(Stage primaryStage) {
        dataObjectTree.selectedDataObjectProperty().addListener((observable, oldValue, newValue) -> dataAttributeTable.setDataObject(newValue));

        fileChooser.setTitle("Open PowerFactory file");
        List<String> extensions = StreamSupport.stream(ServiceLoader.load(ProjectLoader.class).spliterator(), false)
                .map(l -> "*." + l.getExtension())
                .collect(Collectors.toList());
        String filterDescr = "PowerFactory files (" + String.join(", ", extensions) + ")";
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterDescr, extensions));
        filePathTextField.setEditable(false);
        HBox.setHgrow(filePathTextField, Priority.ALWAYS);

        Button filePathButton = new Button("...");
        filePathButton.setOnAction(event -> {
            String pathPropertyValue = preferences.get(FILE_PATH_PROPERTY, null);
            if (pathPropertyValue != null) {
                fileChooser.setInitialDirectory(Paths.get(pathPropertyValue).getParent().toFile());
            }

            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadFile(file.toPath());
                preferences.put(FILE_PATH_PROPERTY, file.getAbsolutePath());
            }
        });
        HBox filePathPane = new HBox(3, filePathTextField, filePathButton);
        Text dataObjPane = new Text();
        dataObjectTree.selectedDataObjectProperty().addListener((observable, oldValue, newValue) -> dataObjPane.setText(newValue != null ? newValue.toString() : ""));
        VBox topPane = new VBox(filePathPane, dataObjPane);
        BorderPane.setMargin(topPane, new Insets(3, 3, 3, 3));

        SplitPane dataPane = new SplitPane(dataObjectTree, dataAttributeTable);
        dataPane.setDividerPositions(0.3);
        mainPane.setCenter(dataPane);
        mainPane.setTop(topPane);

        hideLoader();
        StackPane stackPane = new StackPane(mainPane, loader);

        Scene scene = new Scene(stackPane, 1000, 800);
        primaryStage.setTitle("PowerFactory Viewer");
        primaryStage.setScene(scene);

        loadFileFromPreferences();

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
