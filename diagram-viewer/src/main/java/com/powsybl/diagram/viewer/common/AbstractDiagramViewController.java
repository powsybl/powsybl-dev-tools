package com.powsybl.diagram.viewer.common;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;

public abstract class AbstractDiagramViewController {

    @FXML
    public TabPane checkedTab;

    @FXML
    public TabPane checkedOrSelected;

    protected abstract AbstractDiagramController getSelectedDiagramController();

    protected abstract AbstractDiagramController getCheckedDiagramController(Tab tabInChecked);

    private AbstractDiagramController getActiveTabController() {
        Tab tab = checkedOrSelected.getSelectionModel().getSelectedItem();
        if ("Selected".equals(tab.getText())) {
            return getSelectedDiagramController();
        } else {
            Tab tabInChecked = checkedTab.getSelectionModel().getSelectedItem();
            return getCheckedDiagramController(tabInChecked);
        }
    }

    @FXML
    public void onClickFitToContent(MouseEvent mouseEvent) {
        getActiveTabController().onClickFitToContent();
        mouseEvent.consume();
    }

    @FXML
    public void onClickResetZoom(MouseEvent mouseEvent) {
        getActiveTabController().onClickResetZoom();
        mouseEvent.consume();
    }
}
