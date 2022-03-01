package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.view.OptionsPane;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;

public class ControllerOptions
{
    private Stage primaryStage;
    private OptionsPane optionsPane;

    public ControllerOptions(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
    }

    public void createOptionsPane()
    {
        optionsPane = new OptionsPane();
    }

    public void setParamPane()
    {
        // Full Network Check
        addListenerOnNetworkCheck(optionsPane.getFullNetworkCheck(), primaryStage);

        // Depth Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        optionsPane.getDepthSpinner().setValueFactory(valueFactory);
        addListenerOnDepthSpinner(optionsPane.getDepthSpinner(), primaryStage);

        // Run loadflow button
        addListenerOnRunFlowButton(optionsPane.getRunLoadFlowButton(), primaryStage);


        // Filters field
        addListenerOnFilterField(optionsPane.getFiltersField(), primaryStage);
    }

    private void addListenerOnNetworkCheck(CheckBox check, Stage primaryStage)
    {
        check.setOnAction(event ->
        {
            if (check.isSelected())
                System.out.println("Network Check Selected OK");
            else
                System.out.println("Network Check Unselected OK");
        });
    }

    private void addListenerOnDepthSpinner(Spinner spinner, Stage primaryStage)
    {
        spinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            System.out.println("Depth Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnRunFlowButton(Button button, Stage primaryStage)
    {
        button.setOnAction(event ->
        {
            System.out.println("Run Loadflow OK");
        });
    }

    private void addListenerOnFilterField(TextField field, Stage primaryStage)
    {
        field.setOnAction(event ->
        {
            System.out.print("Filter field OK");
        });
    }

    public OptionsPane getOptionsPane()
    {
        return  optionsPane;
    }
}
