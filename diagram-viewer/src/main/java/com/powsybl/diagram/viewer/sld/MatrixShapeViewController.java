package com.powsybl.diagram.viewer.sld;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;

public class MatrixShapeViewController {

    @FXML
    public Label substationName;

    @FXML
    public Label substationId;

    @FXML
    public Spinner<Integer> row;

    @FXML
    public Spinner<Integer> column;

    public void setSubstationNameValue(String text) {
        this.substationName.setText(text);
    }

    public String getSubstationNameValue() {
        return substationName.getText();
    }

    public void setRowValue(Integer row) {
        this.row.getValueFactory().setValue(row);
    }

    public Integer getRowValue() {
        return row.getValue();
    }

    public void setColumnValue(Integer column) {
        this.column.getValueFactory().setValue(column);
    }

    public Integer getColumnValue() {
        return column.getValue();
    }

    public void setSubstationIdValue(String substationId) {
        this.substationId.setText(substationId);
    }

    public Label getSubstationIdValue() {
        return substationId;
    }
}
