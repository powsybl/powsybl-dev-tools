/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powerfactory.viewer;

import com.powsybl.powerfactory.model.DataAttribute;
import com.powsybl.powerfactory.model.DataObject;
import com.powsybl.powerfactory.model.DataObjectRef;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixFormat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DataAttributeTable extends BorderPane {

    private static final RealMatrixFormat MATRIX_FORMAT = new RealMatrixFormat("{", "}", "{", "}", "," + System.lineSeparator(), ",");

    private final TableView<DataAttribute> tableView;

    private DataObject dataObject;

    @SuppressWarnings("unchecked")
    public DataAttributeTable(DataObjectTree dataObjectTree) {
        Objects.requireNonNull(dataObjectTree);
        tableView = new TableView<>();
        tableView.setStyle("-fx-selection-bar: yellow; -fx-selection-bar-non-focused: lightyellow;");
        setCenter(tableView);
        TableColumn<DataAttribute, String> attrNameCol = new TableColumn<>("Name");
        TableColumn<DataAttribute, String> attrTypeCol = new TableColumn<>("Type");
        TableColumn<DataAttribute, Object> attrValueCol = new TableColumn<>("Value");
        attrNameCol.setPrefWidth(200);
        attrTypeCol.setPrefWidth(150);
        attrValueCol.setPrefWidth(700);
        attrNameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        attrTypeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().name()));
        attrValueCol.setCellValueFactory(param -> {
            Object value = getAttributeValue(param.getValue());
            return value != null ? new SimpleObjectProperty<>(value) : null;
        });
        attrValueCol.setCellFactory(param -> new TableCell<>() {

            private Node createHyperlink(DataObjectRef dataObjectRef) {
                DataObject otherDataObject = dataObjectRef.resolve().orElse(null);
                if (otherDataObject != null) {
                    Hyperlink hyperlink = new Hyperlink(otherDataObject.toString());
                    hyperlink.setTooltip(new Tooltip(otherDataObject.toString()));
                    hyperlink.setOnAction((ActionEvent event) -> {
                        dataObjectTree.search(otherDataObject);
                        tableView.requestFocus();
                    });
                    return hyperlink;
                } else {
                    return new Label(Long.toString(dataObjectRef.getId()));
                }
            }

            private Node createHyperlinkList(List<DataObjectRef> dataObjectRefs) {
                VBox vBox = new VBox();
                Iterator<DataObjectRef> it = dataObjectRefs.iterator();
                List<Node> nodes = new ArrayList<>();
                nodes.add(new Text("["));
                while (it.hasNext()) {
                    DataObjectRef dataObjectRef = it.next();
                    Node hyperlink = createHyperlink(dataObjectRef);
                    nodes.add(hyperlink);
                    if (it.hasNext()) {
                        nodes.add(new Text(","));
                    } else {
                        nodes.add(new Text("]"));
                    }
                    HBox hBox = new HBox(nodes.toArray(new Node[0]));
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    vBox.getChildren().add(hBox);
                    nodes.clear();
                }
                return vBox;
            }

            @Override
            public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (item != null) {
                        if (item instanceof DataObjectRef) {
                            Node hyperlink = createHyperlink((DataObjectRef) item);
                            setGraphic(hyperlink);
                            setText(null);
                        } else if (item instanceof List && !((List<?>) item).isEmpty() && ((List<?>) item).get(0) instanceof DataObjectRef) {
                            Node hyperlinks = createHyperlinkList((List<DataObjectRef>) item);
                            setGraphic(hyperlinks);
                            setText(null);
                        } else if (item instanceof RealMatrix) {
                            setGraphic(null);
                            setText(MATRIX_FORMAT.format((RealMatrix) item));
                        } else {
                            setGraphic(null);
                            setText(item.toString());
                        }
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });
        tableView.getColumns().addAll(attrNameCol, attrTypeCol, attrValueCol);
    }

    private Object getAttributeValue(DataAttribute attribute) {
        if (dataObject != null) {
            return dataObject.findAttributeValue(attribute.getName()).orElse(null);
        }
        return null;
    }

    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
        if (dataObject != null) {
            tableView.getItems().setAll(dataObject.getDataClass().getAttributes());
            tableView.scrollTo(0);
            tableView.getSelectionModel().select(0);
        } else {
            tableView.getItems().clear();
        }
    }
}
