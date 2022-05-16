/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powerfactory.viewer;

import com.powsybl.powerfactory.model.DataObject;
import com.powsybl.powerfactory.model.Project;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DataObjectTree extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataObjectTree.class);

    private final TreeTableView<DataObject> treeTableView = new TreeTableView<>();

    private final ContextMenu menu = new ContextMenu();

    private final Menu backwardLinksMenuItem = new Menu("Backward links");

    private final SimpleObjectProperty<DataObject> selectedDataObject = new SimpleObjectProperty<>();

    private Project project;

    @SuppressWarnings("unchecked")
    public DataObjectTree() {
        treeTableView.setStyle("-fx-selection-bar: yellow; -fx-selection-bar-non-focused: lightyellow;");
        setCenter(treeTableView);
        TreeTableColumn<DataObject, DataObject> locNameCol = new TreeTableColumn<>("Name");
        locNameCol.setPrefWidth(300);
        TreeTableColumn<DataObject, String> classNameCol = new TreeTableColumn<>("Class");
        classNameCol.setPrefWidth(100);
        TreeTableColumn<DataObject, Long> idCol = new TreeTableColumn<>("ID");
        locNameCol.setPrefWidth(100);

        locNameCol.setCellFactory(new Callback<>() {
            @Override
            public TreeTableCell<DataObject, DataObject> call(TreeTableColumn<DataObject, DataObject> param) {
                return new TextFieldTreeTableCell<>() {
                    @Override
                    public void updateItem(DataObject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(item.getLocName());
                            setContextMenu(menu);
                        }
                    }
                };
            }
        });
        locNameCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
        classNameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getDataClass().getName()));
        idCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue().getId()));
        treeTableView.getColumns().addAll(locNameCol, classNameCol, idCol);
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        treeTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<DataObject>>) c -> {
            ObservableList<? extends TreeItem<DataObject>> list = c.getList();
            if (list.isEmpty()) {
                selectedDataObject.set(null);
            } else {
                DataObject dataObject = list.get(0).getValue();
                selectedDataObject.set(dataObject);

                // update backward links menu
                backwardLinksMenuItem.getItems().clear();
                List<DataObject> backwardLinks = project.getIndex().getBackwardLinks(dataObject.getId());
                if (backwardLinks.isEmpty()) {
                    backwardLinksMenuItem.setDisable(true);
                } else {
                    backwardLinksMenuItem.setDisable(false);
                    for (DataObject backwardLink : backwardLinks) {
                        Hyperlink hyperlink = new Hyperlink(backwardLink.toString());
                        hyperlink.setOnAction(event -> {
                            search(backwardLink);
                            menu.hide();
                        });
                        backwardLinksMenuItem.getItems().add(new MenuItem("", hyperlink));
                    }
                }
            }
        });
        menu.setStyle("-fx-selection-bar: yellow; -fx-selection-bar-non-focused: lightyellow;");
        menu.getItems().add(backwardLinksMenuItem);
    }

    public void setProject(Project project) {
        this.project = project;
        TreeItem<DataObject> rootItem = null;
        if (project != null) {
            rootItem = createRootItem(project.getRootObject());
        }
        treeTableView.setRoot(rootItem);
        if (rootItem != null) {
            rootItem.setExpanded(true);
            treeTableView.getSelectionModel().select(rootItem);
        }
    }

    public void search(DataObject dataObject) {
        Objects.requireNonNull(dataObject);
        LOGGER.info("Searching '{}'", dataObject);

        TreeItem<DataObject> currentTreeItem = treeTableView.getRoot();
        List<TreeItem<DataObject>> treeItemPath = new ArrayList<>(10);
        Deque<DataObject> path = new ArrayDeque<>(dataObject.getPath());
        path.pop(); // pop root
        while (!path.isEmpty()) {
            treeItemPath.add(currentTreeItem);
            DataObject lastElem = path.pop();
            currentTreeItem = currentTreeItem.getChildren().stream()
                    .filter(child -> child.getValue().getLocName().equals(lastElem.getLocName()))
                    .findFirst()
                    .orElse(null);
            if (currentTreeItem == null) {
                break;
            }
        }

        if (path.isEmpty()) { // success
            for (TreeItem<DataObject> treeItem : treeItemPath) {
                treeItem.setExpanded(true);
            }
            treeTableView.getSelectionModel().select(currentTreeItem);
            int selectedIndex = treeTableView.getSelectionModel().getSelectedIndex();
            treeTableView.scrollTo(selectedIndex);
        }
    }

    private TreeItem<DataObject> createRootItem(DataObject rootDataObject) {
        TreeItem<DataObject> rootItem = null;
        if (rootDataObject != null) {
            rootItem = new TreeItem<>(rootDataObject);
            createTree(rootItem);
        }
        return rootItem;
    }

    private void createTree(TreeItem<DataObject> item) {
        for (DataObject childDataObject : item.getValue().getChildren()) {
            TreeItem<DataObject> childItem = new TreeItem<>(childDataObject);
            item.getChildren().add(childItem);
            createTree(childItem);
        }
    }

    public SimpleObjectProperty<DataObject> selectedDataObjectProperty() {
        return selectedDataObject;
    }
}
