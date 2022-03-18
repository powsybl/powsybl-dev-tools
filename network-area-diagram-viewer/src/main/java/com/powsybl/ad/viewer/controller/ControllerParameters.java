/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.controller;

import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.util.Util;
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
            Util.loggerControllerParameters.info("Fit to content OK");
        });
    }

    private void addListenerOnResetZoom(Button resetZoomButton)
    {
        resetZoomButton.setOnAction(event ->
        {
            Util.loggerControllerParameters.info("Reset Zoom OK");
        });
    }

    private void addListenerOnLayoutChoice(ChoiceBox layoutChoice)
    {
        layoutChoice.setOnAction(event ->
        {
            Util.loggerControllerParameters.info("Layout choice : " + layoutChoice.getValue());
//            NadCalls.setLayoutParameters(toString(layoutChoice.getValue()));
        });
    }

    private void addListenerOnLabelProviderChoice(ChoiceBox labelProviderChoice)
    {
        labelProviderChoice.setOnAction(event ->
        {
            Util.loggerControllerParameters.info("Label Provider : " + labelProviderChoice.getValue());
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
                    DiagramPane.addSVG(getSvgWriter());  // draws nad's svg
                    Util.loggerControllerParameters.info("styleProvider variable successfully changed to 'NominalVoltageStyleProvider'");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (styleProviderChoice.getValue() == "Topological") {
                styleProvider = new TopologicalStyleProvider(NadCalls.networkProperty.get());
                try {
                    cleanSVG();  // clean the window and the variables
                    drawNetwork();  // changes the variable svgWriter
                    DiagramPane.addSVG(getSvgWriter());  // draws nad's svg
                    Util.loggerControllerParameters.info("styleProvider variable successfully changed to 'TopologicalStyleProvider'");
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
            Util.loggerControllerParameters.info("Layout X Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnLayoutYSpinner(Spinner layoutYSpinner)
    {
        layoutYSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            Util.loggerControllerParameters.info("Layout Y Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGXSpinner(Spinner svgXSpinner)
    {
        svgXSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            Util.loggerControllerParameters.info("SVG X Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGYSpinner(Spinner svgYSpinner)
    {
        svgYSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            Util.loggerControllerParameters.info("SVG Y Spinner value :  " + oldValue + " transformed into : " + newValue);
        });
    }

    private void addListenerOnSVGZCheck(CheckBox svgZCheck)
    {
        svgZCheck.setOnAction(event ->
        {
            if (svgZCheck.isSelected())
                Util.loggerControllerParameters.info("SVG Z Selected OK");
            else
                Util.loggerControllerParameters.info("SVG Z Unselected OK");
        });
    }

    public ParamPane getParamPane()
    {
        return paramPane;
    }
}
