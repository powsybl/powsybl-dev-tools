package com.powsybl.ad.viewer.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ImportBar extends HBox
{
    private Button loadButton;
    private Button loadingStatus;

    private TextField pathTextField;

    public ImportBar()
    {
        createLoadButton("...");
        createImportTextField();
        createLoadingStatusButton("-fx-background-color: red");

        getChildren().addAll(loadingStatus, pathTextField, loadButton);
        BorderPane.setMargin(this, new Insets(3, 3, 3, 3));
    }

    private void createLoadingStatusButton(String style)
    {
        loadingStatus = new Button();
        loadingStatus.setStyle(style);
    }

    private void createLoadButton(String buttonName)
    {
        loadButton = new Button(buttonName);
    }

    private void createImportTextField()
    {
        pathTextField = new TextField();
        HBox.setHgrow(pathTextField, Priority.ALWAYS);
    }

    public Button getLoadingStatusButton()
    {
        return loadingStatus;
    }

    public Button getLoadButton()
    {
        return loadButton;
    }

    public TextField getPathTextField()
    {
        return pathTextField;
    }

}
