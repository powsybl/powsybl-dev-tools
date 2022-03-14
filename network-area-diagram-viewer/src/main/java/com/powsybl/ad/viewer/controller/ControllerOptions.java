/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.view.OptionsPane;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerOptions
{
    private static Stage primaryStage;
    private static OptionsPane optionsPane;

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
        addListenerOnFullNetworkCheck(optionsPane.getFullNetworkCheck());

        // Depth Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        optionsPane.getDepthSpinner().setValueFactory(valueFactory);
        addListenerOnDepthSpinner(optionsPane.getDepthSpinner(), primaryStage);

        // Run loadflow button
        addListenerOnRunFlowButton(optionsPane.getRunLoadFlowButton(), primaryStage);

        // Filters field
        addListenerOnFilterField(optionsPane.getFiltersField(), primaryStage);
    }

    public static void setNodesList(ArrayList<String> nodesName)
    {
        optionsPane.createNodes(nodesName);
        optionsPane.displayNodes();
        // Filters pane
        addListenerOnNodesPane(optionsPane.getNodesCheckBoxes(), primaryStage);
    }
    private void addListenerOnFullNetworkCheck(CheckBox check)
    {
        check.setOnAction(event ->
        {
            if (check.isSelected() && NadCalls.svgWriter != null)
            {
                ControllerDiagram.loadNewSVG(NadCalls.svgWriter);  // calls addSVG which actually displays the svg
            }
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
            NadCalls.runLoadFlow();
        });
    }

    private void addListenerOnFilterField(TextField field, Stage primaryStage)
    {
        field.setOnAction(event ->
        {
            System.out.print("Filter field OK");
        });
    }

    private static void addListenerOnNodesPane(ArrayList<CheckBox> nodesCheckBoxes, Stage primaryStage)
    {
        for (CheckBox checkBox : nodesCheckBoxes)
        {
            checkBox.setOnAction(event ->
            {
                if (checkBox.isSelected())
                {
                    System.out.println("Node Selected OK" + checkBox.getText());
                }
                else
                {
                    System.out.println("Node Unselected OK" + checkBox.getText());
                }
            });
        }
    }

    public OptionsPane getOptionsPane()
    {
        return  optionsPane;
    }
}
