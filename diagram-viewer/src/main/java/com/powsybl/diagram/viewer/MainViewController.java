/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer;

import com.powsybl.diagram.viewer.nad.NetworkAreaDiagramViewController;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class MainViewController implements ChangeListener<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewController.class);

    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final Preferences preferences = Preferences.userNodeForPackage(DiagramViewer.class);

    @FXML
    public TextField filePath;
    @FXML
    public TextField filterField;
    @FXML
    public TreeView<Container<?>> vlTree;
    @FXML
    public CheckBox hideVoltageLevels;
    @FXML
    public CheckBox hideSubstations;
    @FXML
    public CheckBox showNames;

    // Selection between sld and nad
    @FXML
    public TabPane sldOrNad;

    // Status info
    @FXML
    private Node loadingStatus;

    private Model model;

    /**
     * NetworkAreaDiagram tab content is included from nadView.fxml.
     * To get associated controller we have to use "Controller" suffix to nadView.fxml associated fx:id.
     */
    @FXML
    private NetworkAreaDiagramViewController nadViewController;

    @FXML
    private void initialize() {
        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            loadFile(new File(casePathPropertyValue));
        }

        model = new Model(showNames.selectedProperty(), nadViewController.getModel());

        model.getNetworkProperty().addListener((observableValue, oldNetwork, newNetwork) ->
                initSubstationsTree(newNetwork));

        showNames.selectedProperty().addListener(this);
        vlTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            model.setSelectedContainer(newValue.getValue());
            nadViewController.createDiagram(model.getNetwork(), newValue.getValue());
        });
        vlTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            StringConverter<TreeItem<Container<?>>> strConvert = new StringConverter<>() {
                @Override
                public String toString(TreeItem<Container<?>> c) {
                    if (c.getValue().getContainerType() == ContainerType.NETWORK) {
                        return "Full Network";
                    }
                    if (c.getValue() != null) {
                        return showNames.isSelected() ? c.getValue().getNameOrId() : c.getValue().getId();
                    }
                    return "";
                }

                @Override
                public TreeItem<Container<?>> fromString(String string) {
                    return null;
                }
            };
            treeCell.setConverter(strConvert);
            return treeCell;
        });
        nadViewController.addListener(this);
        // FIXME : SLD case is missing
    }

    @Override
    public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
        updateAllDiagrams();
    }

    private void updateAllDiagrams() {
        nadViewController.updateAllDiagrams(model.getNetwork(), model.getSelectedContainer());
        // FIXME : SLD case is missing
    }

    @FXML
    private void onClickLoadFile(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        String caseFolderPropertyValue = preferences.get(CASE_FOLDER_PROPERTY, null);
        if (caseFolderPropertyValue != null) {
            fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
        }
        fileChooser.setTitle("Open case File");
        loadFile(fileChooser.showOpenDialog(loadingStatus.getScene().getWindow()));
        event.consume();
    }

    public void loadFile(File file) {
        if (file != null) {
            Service<Network> networkService = new Service<>() {
                @Override
                protected Task<Network> createTask() {
                    return new Task<>() {
                        @Override
                        protected Network call() {
                            return Network.read(file.toPath());
                        }
                    };
                }
            };
            handleLoadingResult(file, networkService);
            networkService.start();
        }
    }

    private void handleLoadingResult(File file, Service<Network> networkService) {
        networkService.setOnRunning(event -> {
            loadingStatus.setStyle("-fx-background-color: yellow");
            filePath.setText(file.getAbsolutePath());
            preferences.put(CASE_FOLDER_PROPERTY, file.getParent());
        });

        networkService.setOnSucceeded(event -> {
            clean();
            model.setNetwork((Network) event.getSource().getValue());
            loadingStatus.setStyle("-fx-background-color: green");
            preferences.put(CASE_PATH_PROPERTY, file.getAbsolutePath());
        });

        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            filePath.setText("");
            loadingStatus.setStyle("-fx-background-color: red");
        });
    }

    private void clean() {
        // View
        filterField.setText("");
        // Controller (sld & nad)
        nadViewController.clean();
        // FIXME : SLD case is missing
        // Model (sld & nad)
        model.clean();
    }

    @FXML
    private void onClickLoadFlow(MouseEvent actionEvent) {
        LoadFlow.run(model.getNetwork());
        updateAllDiagrams(); // forces update -> better done with NetworkListener
    }

    @FXML
    private void initSubstationsTree() {
        initSubstationsTree(model.getNetwork());
    }

    private void initSubstationsTree(Network network) {
        if (network == null) {
            return;
        }

        CheckBoxTreeItem<Container<?>> rootTreeItem = (CheckBoxTreeItem<Container<?>>) vlTree.getRoot();
        if (rootTreeItem == null) {
            rootTreeItem = new CheckBoxTreeItem<>(network);
            rootTreeItem.setIndependent(true);
            rootTreeItem.setExpanded(true);
            vlTree.setRoot(rootTreeItem);
        }

        Set<String> containersChecked = rootTreeItem.getChildren().stream()
                .flatMap(s -> Stream.concat(Stream.of(s), s.getChildren().stream()))
                .filter(CheckBoxTreeItem.class::isInstance).map(ti -> (CheckBoxTreeItem<Container<?>>) ti)
                .filter(CheckBoxTreeItem::isSelected)
                .map(item -> item.getValue().getId())
                .collect(Collectors.toSet());
        TreeItem<Container<?>> selectedItem = vlTree.getSelectionModel().getSelectedItem();
        String selectedContainerId = selectedItem == null ? null : selectedItem.getValue().getId();

        rootTreeItem.getChildren().clear();
        rootTreeItem.setValue(network);

        String filter = filterField.getText();
        for (Substation s : network.getSubstations()) {
            CheckBoxTreeItem<Container<?>> sItem = null;
            boolean sFilterOk = testPassed(filter, s);
            List<VoltageLevel> voltageLevels = s.getVoltageLevelStream()
                    .filter(v -> sFilterOk || testPassed(filter, v))
                    .collect(Collectors.toList());
            if ((sFilterOk || !voltageLevels.isEmpty()) && !hideSubstations.isSelected()) {
                sItem = new CheckBoxTreeItem<>(s);
                sItem.setIndependent(true);
                sItem.setExpanded(true);
                if (containersChecked.contains(s.getId())) {
                    sItem.setSelected(true);
                }
                rootTreeItem.getChildren().add(sItem);
                addListenerOnContainerItem(sItem);
            }

            initVoltageLevelsTree(rootTreeItem, sItem, voltageLevels, containersChecked);
        }

        List<VoltageLevel> emptySubstationVoltageLevels = network.getVoltageLevelStream()
                .filter(v -> v.getSubstation().isEmpty())
                .filter(v -> testPassed(filter, v))
                .collect(Collectors.toList());
        initVoltageLevelsTree(rootTreeItem, null, emptySubstationVoltageLevels, containersChecked);

        rootTreeItem.getChildren().stream()
                .flatMap(s -> Stream.concat(Stream.of(s), s.getChildren().stream()))
                .filter(item -> item.getValue().getId().equals(selectedContainerId))
                .findFirst()
                .ifPresentOrElse(item -> vlTree.getSelectionModel().select(item),
                    () -> vlTree.getSelectionModel().clearSelection());
        vlTree.setShowRoot(true);
    }

    private void initVoltageLevelsTree(TreeItem<Container<?>> rootItem, CheckBoxTreeItem<Container<?>> sItem,
                                       Collection<VoltageLevel> voltageLevels, Set<String> checkecContainers) {

        for (VoltageLevel v : voltageLevels) {
            if (!hideVoltageLevels.isSelected()) {
                CheckBoxTreeItem<Container<?>> vItem = new CheckBoxTreeItem<>(v);
                vItem.setIndependent(true);
                if (checkecContainers.contains(v.getId())) {
                    vItem.setSelected(true);
                }
                (sItem != null ? sItem : rootItem).getChildren().add(vItem);
                addListenerOnContainerItem(vItem);
            }
        }
    }

    private Function<Identifiable<?>, String> getIdentifiableStringSupplier() {
        return showNames.isSelected() ? Identifiable::getNameOrId : Identifiable::getId;
    }

    private boolean testPassed(String filter, Identifiable<?> identifiable) {
        return StringUtils.isEmpty(filter)
                || getIdentifiableStringSupplier().apply(identifiable)
                .toLowerCase(Locale.getDefault())
                .contains(filter.toLowerCase(Locale.getDefault()));
    }

    private void addListenerOnContainerItem(CheckBoxTreeItem<Container<?>> containerTreeItem) {
        containerTreeItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                createCheckedTab(containerTreeItem);
            }
        });
    }

    public void createCheckedTab(CheckBoxTreeItem<Container<?>> containerTreeItem) {
        Container<?> container = containerTreeItem.getValue();
        String tabName = getIdentifiableStringSupplier().apply(container);
        nadViewController.createCheckedTab(model.getNetwork(), containerTreeItem, tabName);
    }
}
