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
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.Padding;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import static com.powsybl.ad.viewer.model.NadCalls.networkProperty;
import java.io.IOException;
import java.io.StringWriter;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import static com.powsybl.ad.viewer.model.NadCalls.*;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerParameters
{
    private Stage primaryStage;
    private static ParamPane paramPane;

    private ChangeListener<SvgParameters> listener;

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
        paramPane.setSvgXSpinner(addSpinner(paramPane.getSvgParametersPane(), "Horizontal Padding",
                0, 300, 5,
                0, 0, "X",
                 svgp -> svgp.getLeft(),
                 (svgp, value) -> svgp.setDiagramPadding(
                         new Padding(value, svgp.getDiagramPadding().getTop(),
                                 value, svgp.getDiagramPadding().getBottom()))
                )
        );
        paramPane.setSvgYSpinner(addSpinner(paramPane.getSvgParametersPane(), "Vertical Padding",
                0, 300, 5,
                0, 2, "Y",
                svgp -> svgp.getBottom(),
                (svgp, value) -> svgp.setDiagramPadding(
                        new Padding(svgp.getDiagramPadding().getLeft(), value,
                                svgp.getDiagramPadding().getRight(), value))
                )
        );

        addListenerOnSVGZCheck(paramPane.getSvgZCheck());

        // Add listener on svgParametersProperty
        listener = (observable, oldValue, newValue) -> {
            try {
                NadCalls.drawNetwork();
                DiagramPane.addSVG(getSvgWriter());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        NadCalls.svgParametersProperty.addListener(listener);
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
        resetZoomButton.setOnAction(event -> {

            // only the currently selected tab's zoom should be reset
            Tab tabSelectedbyUser = ControllerDiagram.getDiagramPane().getTabSelectedByUser();
            Node node = tabSelectedbyUser.getContent();
            if (node != null) {
                if (node instanceof ContainerDiagramPane) {  // We need to downcast Node into ContainerDiagramPane
                    ContainerDiagramPane pane = (ContainerDiagramPane) node;
                    pane.getDiagramView().setZoom(1.0); // 100
                }
            }
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
                    drawNetwork();  // changes the variable svgWriter
                    DiagramPane.addSVG(getSvgWriter());  // draws nad's svg
                    Util.loggerControllerParameters.info("styleProvider variable successfully changed to 'TopologicalStyleProvider'");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setParameters(SvgParameters svgParameters){
        NadCalls.svgParametersProperty.set(new SvgParameters(svgParametersProperty.get()));
    }

    private Spinner addSpinner(GridPane paneToAddSpinnerOn,
                               String label, double min, double max, double amountToStepBy,
                               int column, int row, String direction,
                               ToDoubleFunction<Padding> initializer,
                               BiFunction<SvgParameters, Double, SvgParameters> updater) {
        Spinner<Double> spinner = new Spinner<>(
                min, max,
                initializer.applyAsDouble(svgParametersProperty.get().getDiagramPadding()), amountToStepBy
        );
        spinner.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
                        SvgParameters oldSvgParametersProperty = svgParametersProperty.get();
                        setParameters(updater.apply(oldSvgParametersProperty, newValue));
                        Util.loggerControllerParameters.info(
                                "Layout " + direction + " Spinner value :  " +
                                        oldValue + " transformed into : " + newValue
                        );
                    }
                }
        );

        spinner.setEditable(true);
        spinner.setDisable(true);
        paneToAddSpinnerOn.add(new Label(label), column, row);
        paneToAddSpinnerOn.add(spinner, column, row + 1);

        return spinner;
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

    public static ParamPane getParamPane()
    {
        return paramPane;
    }
}
