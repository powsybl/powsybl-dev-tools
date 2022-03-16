/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.view.ParamPane;
import com.powsybl.ad.viewer.view.diagram.DiagramPane;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.scene.control.*;
import javafx.stage.Stage;
import static com.powsybl.ad.viewer.model.NadCalls.networkProperty;
import java.io.IOException;
import static com.powsybl.ad.viewer.model.NadCalls.*;
import static com.powsybl.ad.viewer.view.diagram.DiagramPane.cleanSVG;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerParameters
{
    private Stage primaryStage;
    private static ParamPane paramPane;

    public static StyleProvider styleProvider;  // inside it will be stored the dropdown list's selected value

    public static void reselectDefaultChoiceBoxes()
    {
        ChoiceBox layoutChoice = paramPane.getLayoutChoice();
        layoutChoice.getSelectionModel().selectFirst();  // make ChoiceBox layoutChoice select its first element
        paramPane.setLayoutChoice(layoutChoice);

        ChoiceBox labelProviderChoice = paramPane.getLabelProviderChoice();
        labelProviderChoice.getSelectionModel().selectFirst();  // make ChoiceBox labelProviderChoice select
                                                                // its first element
        paramPane.setLabelProviderChoice(layoutChoice);

        ChoiceBox styleProviderChoice = paramPane.getStyleProviderChoice();
        styleProviderChoice.getSelectionModel().selectFirst();  // make ChoiceBox styleProviderChoice
                                                                // select its first element
        paramPane.setStyleProviderChoice(layoutChoice);
    }

    public ControllerParameters(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
    }

    public void createParamPane()
    {
        paramPane = new ParamPane();
    }

    public void setParamPane()
    {
        // Fit and Reset buttons
        addListenerOnFitToContent(paramPane.getFitContentButton());
        addListenerOnResetZoom(paramPane.getResetZoomButton());

        // Layout, Label provider and Style Provider Choices
        addListenerOnLayoutChoice(paramPane.getLayoutChoice());
        addListenerOnLabelProviderChoice(paramPane.getLabelProviderChoice());
        addListenerOnStyleProviderChoice(paramPane.getStyleProviderChoice());

        // Layout parameters spinners
        SpinnerValueFactory<Integer> layoutXSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getLayoutXSpinner().setValueFactory(layoutXSpinnerFactory);
        addListenerOnLayoutXSpinner(paramPane.getLayoutXSpinner());

        SpinnerValueFactory<Integer> layoutYSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getLayoutYSpinner().setValueFactory(layoutYSpinnerFactory);
        addListenerOnLayoutYSpinner(paramPane.getLayoutYSpinner());

        // SVG parameters spinners and checkbox
        SpinnerValueFactory<Integer> svgXSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getSvgXSpinner().setValueFactory(svgXSpinnerFactory);
        addListenerOnSVGXSpinner(paramPane.getSvgXSpinner());

        SpinnerValueFactory<Integer> svgYSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 200, 20, 20);
        paramPane.getSvgYSpinner().setValueFactory(svgYSpinnerFactory);
        addListenerOnSVGYSpinner(paramPane.getSvgYSpinner());

        addListenerOnSVGZCheck(paramPane.getSvgZCheck());
    }

    private void addListenerOnFitToContent(Button fitContentButton)
    {
        fitContentButton.setOnAction(event ->
        {
            System.out.println("Fit to content OK");
        });
    }

    private void addListenerOnResetZoom(Button resetZoomButton)
    {
        resetZoomButton.setOnAction(event ->
        {
            System.out.println("Fit to content OK");
        });
    }

    private void addListenerOnLayoutChoice(ChoiceBox layoutChoice)
    {
        layoutChoice.setOnAction(event ->
        {
            System.out.println("Layout choice : " + layoutChoice.getValue());
//            NadCalls.setLayoutParameters(toString(layoutChoice.getValue()));
        });
    }

    private void addListenerOnLabelProviderChoice(ChoiceBox labelProviderChoice)
    {
        labelProviderChoice.setOnAction(event ->
        {
            System.out.println("Label Provider : " + labelProviderChoice.getValue());
        });
    }

    private void addListenerOnStyleProviderChoice(ChoiceBox styleProviderChoice)
    {
        styleProviderChoice.setOnAction(event ->
        {
            if (styleProviderChoice.getValue() == "Nominal") {
                styleProvider = new NominalVoltageStyleProvider(networkProperty.get());
                try {
                    cleanSVG();  // clean the window and the variables
                    drawNetwork();  // changes the variable svgWriter
                    DiagramPane.addSVG(svgWriter);  // draws nad's svg
                    System.out.println("styleProvider variable successfully changed to 'NominalVoltageStyleProvider'");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (styleProviderChoice.getValue() == "Topological") {
                styleProvider = new TopologicalStyleProvider(NadCalls.networkProperty.get());
                try {
                    cleanSVG();  // clean the window and the variables
                    drawNetwork();  // changes the variable svgWriter
                    DiagramPane.addSVG(svgWriter);  // draws nad's svg
                    System.out.println("styleProvider variable successfully changed to 'TopologicalStyleProvider'");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addListenerOnLayoutXSpinner(Spinner layoutXSpinner)
    {
        layoutXSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            System.out.println("Layout X Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnLayoutYSpinner(Spinner layoutYSpinner)
    {
        layoutYSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            System.out.println("Layout Y Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGXSpinner(Spinner svgXSpinner)
    {
        svgXSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            System.out.println("SVG X Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGYSpinner(Spinner svgYSpinner)
    {
        svgYSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            System.out.println("SVG Y Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGZCheck(CheckBox svgZCheck)
    {
        svgZCheck.setOnAction(event ->
        {
            if (svgZCheck.isSelected())
                System.out.println("SVG Z Selected OK");
            else
                System.out.println("SVG Z Unselected OK");
        });
    }

    public ParamPane getParamPane()
    {
        return paramPane;
    }
}
