/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import com.powsybl.iidm.network.Container;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractDiagramViewController {

    @FXML
    public TabPane checkedTab;

    @FXML
    public TabPane checkedOrSelected;

    protected final Map<Tab, AbstractDiagramController> checkedDiagramControllers = new HashMap<>();

    protected abstract AbstractDiagramController getSelectedDiagramController();

    protected abstract AbstractDiagramController getCheckedDiagramController(Tab tabInChecked);

    protected void removeCheckedDiagram(Tab tab, Container<?> container) {
        Objects.requireNonNull(container);
        checkedTab.getTabs().remove(tab);
    }

    protected ChangeListener<Boolean> createChangeListener(Tab tab, CheckBoxTreeItem<Container<?>> containerTreeItem) {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (Boolean.FALSE.equals(newValue)) {
                    containerTreeItem.selectedProperty().removeListener(this);
                    removeCheckedDiagram(tab, containerTreeItem.getValue());
                }
            }
        };
    }

    protected void createCheckedTab(CheckBoxTreeItem<Container<?>> containerTreeItem,
                                    String tabName,
                                    Parent diagram,
                                    AbstractDiagramController checkedDiagramController) {
        Container<?> container = containerTreeItem.getValue();
        List<Tab> tabList = checkedTab.getTabs();
        if (tabList.stream().map(Tab::getText).noneMatch(tabName::equals)) {
            Tab newCheckedTab = new Tab(tabName, diagram);
            checkedDiagramControllers.put(newCheckedTab, checkedDiagramController);
            newCheckedTab.setId(container.getId());
            newCheckedTab.setOnClosed(event -> {
                containerTreeItem.setSelected(false);
                checkedDiagramControllers.remove(newCheckedTab);
            });
            containerTreeItem.selectedProperty().addListener(createChangeListener(newCheckedTab, containerTreeItem));
            newCheckedTab.setTooltip(new Tooltip(container.getNameOrId()));
            tabList.add(newCheckedTab);
            checkedOrSelected.getSelectionModel().selectLast();
            checkedTab.getSelectionModel().selectLast();
        }
    }

    public void clean() {
        checkedTab.getTabs().clear();
        getSelectedDiagramController().clean();
    }

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
