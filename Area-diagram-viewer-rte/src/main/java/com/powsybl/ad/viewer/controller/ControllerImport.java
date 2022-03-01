package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.ImportBar;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ControllerImport
{
    private Stage primaryStage;
    private ImportBar importBar;

    public ControllerImport(Stage stage)
    {
        this.primaryStage = stage;
    }

    public void createImportBar(int size)
    {
        importBar = new ImportBar();
    }

    public void setImportBar()
    {
        addListenerOnImportButton(importBar.getLoadButton(), primaryStage);
    }

    private void addListenerOnImportButton(Button button, Stage primaryStage)
    {
        button.setOnAction(event ->
        {
            System.out.print("Import Button OK");
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = Util.preferences.get(Util.CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null)
            {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null)
            {
                //loadNetwork(file.toPath());
            }
        });
    }

    public ImportBar getImportBar()
    {
        return importBar;
    }
}
