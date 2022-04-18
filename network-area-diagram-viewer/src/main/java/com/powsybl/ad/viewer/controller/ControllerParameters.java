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
import com.powsybl.ad.viewer.view.diagram.containers.ContainerDiagramPane;
import com.powsybl.nad.svg.Padding;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import static com.powsybl.ad.viewer.model.NadCalls.networkProperty;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.powsybl.ad.viewer.model.NadCalls.*;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ControllerParameters
{
    private Stage primaryStage;
    private static ParamPane paramPane;

    private ChangeListener<SvgParameters> listener;

    private static StyleProvider styleProvider;  // inside it will be stored the dropdown list's selected value

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
        paramPane.setSvgXSpinner(
                addSpinner(
                        paramPane.getSvgParametersPane(), "Horizontal Padding",
                        0, 300, 5,
                        0, 0, "X",
                        svgp -> svgp.getLeft(),
                        (svgp, value) -> svgp.setDiagramPadding(
                                new Padding(value, svgp.getDiagramPadding().getTop(),
                                        value, svgp.getDiagramPadding().getBottom()))
                )
        );
        paramPane.setSvgYSpinner(
                addSpinner(
                        paramPane.getSvgParametersPane(), "Vertical Padding",
                        0, 300, 5,
                        0, 2, "Y",
                        svgp -> svgp.getBottom(),
                        (svgp, value) -> svgp.setDiagramPadding(
                                new Padding(svgp.getDiagramPadding().getLeft(), value,
                                        svgp.getDiagramPadding().getRight(), value))
                )
        );

        setSvgEdgeInfoCheckbox(paramPane.getSvgEdgeInfoCheckbox());

        // Add listener on svgParametersProperty
        listener = (observable, oldValue, newValue) -> {
            try {
                NadCalls.drawNetwork();
                ControllerDiagram.addSvgToSelectedTab();
                Util.loggerControllerParameters.info("svgParametersProperty changed succesfully.");
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
            // Only the currently selected tab's zoom should changed to fit to content
            Tab tabSelectedByUser = ControllerDiagram.getDiagramPane().getTabSelectedByUser();
            Node tabSelectedByUserNode = tabSelectedByUser.getContent();

            Node borderPaneNode = null;
            Tab selectedSubTabByUser = null;

            if (tabSelectedByUserNode != null) {
                if (tabSelectedByUserNode instanceof BorderPane) {  // that's if we in Selected (cf. createSelectedTab)
                    Util.loggerControllerParameters.info("Fit to content clicked (logger 1/2) whilst in Selected");
                    // We need to downcast tabSelectedByUserNode into a BorderPane
                    BorderPane borderPaneDowncast = (BorderPane) tabSelectedByUserNode;
                    borderPaneNode = borderPaneDowncast.getCenter();
                }
                if (tabSelectedByUserNode instanceof TabPane) {  // that's if we in Checked (cf. createCheckedTab)
                    Util.loggerControllerParameters.info("Fit to content clicked (logger 1/2) whilst in Checked");
                    // We need to downcast tabSelectedByUserNode into a TabPane
                    TabPane tabPaneDowncast = (TabPane) tabSelectedByUserNode;
                    selectedSubTabByUser = tabPaneDowncast.getSelectionModel().getSelectedItem();
                }
            }

            ContainerDiagramPane pane = null;

            if (borderPaneNode != null) {  //  that's if we in Selected
                if (borderPaneNode instanceof ContainerDiagramPane) {
                    Util.loggerControllerParameters.info("Fit to content clicked (logger 2/2) whilst in Selected");
                    // We need to downcast borderPaneNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) borderPaneNode;
                }
            }
            if (selectedSubTabByUser != null) { // that's if we in Checked
                Node selectedSubTabByUserNode = selectedSubTabByUser.getContent();
                if (selectedSubTabByUserNode instanceof ContainerDiagramPane) {
                    Util.loggerControllerParameters.info("Fit to content clicked (logger 2/2) whilst in Checked");
                    // We need to downcast selectedSubTabByUserNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) selectedSubTabByUserNode;
                }
            }

            // Now we fit to content
            if (pane != null) {
                String svgData = pane.getSvgTextArea().getText();
                Optional<String> svgLine = svgData.lines().filter(l -> l.contains("<svg")).findAny();
                if (svgLine.isPresent()) {
                    String valuePattern = "\"([^\"]*)\"";
                    Pattern pW = Pattern.compile("width=" + valuePattern);
                    Matcher mW = pW.matcher(svgLine.get());
                    Pattern pH = Pattern.compile("height=" + valuePattern);
                    Matcher mH = pH.matcher(svgLine.get());
                    if (mH.find() && mW.find()) {
                        double svgWidth = Double.parseDouble(mW.group(1));
                        double svgHeight = Double.parseDouble(mH.group(1));
                        double paneWidth = pane.getDiagramView().widthProperty().get();
                        double paneHeight = pane.getDiagramView().heightProperty().get();
                        double zoomH = paneHeight / svgHeight;
                        double zoomW = paneWidth / svgWidth;
                        pane.getDiagramView().setZoom(Math.min(zoomH, zoomW));
                    }
                }
                Util.loggerControllerParameters.info("Fit to content OK");
            }
        });
    }

    private void addListenerOnResetZoom(Button resetZoomButton)
    {
        resetZoomButton.setOnAction(event -> {

            // Only the currently selected tab's zoom should be reset
            Tab tabSelectedByUser = ControllerDiagram.getDiagramPane().getTabSelectedByUser();
            Node tabSelectedByUserNode = tabSelectedByUser.getContent();

            Node borderPaneNode = null;
            Tab selectedSubTabByUser = null;

            if (tabSelectedByUserNode != null) {
                if (tabSelectedByUserNode instanceof BorderPane) {  // that's if we in Selected (cf. createSelectedTab)
                    Util.loggerControllerParameters.info("Reset Zoom clicked (logger 1/2) whilst in Selected");
                    // We need to downcast tabSelectedByUserNode into a BorderPane
                    BorderPane borderPaneDowncast = (BorderPane) tabSelectedByUserNode;
                    borderPaneNode = borderPaneDowncast.getCenter();
                }
                if (tabSelectedByUserNode instanceof TabPane) {  // that's if we in Checked (cf. createCheckedTab)
                    Util.loggerControllerParameters.info("Reset Zoom clicked (logger 1/2) whilst in Checked");
                    // We need to downcast tabSelectedByUserNode into a TabPane
                    TabPane tabPaneDowncast = (TabPane) tabSelectedByUserNode;
                    selectedSubTabByUser = tabPaneDowncast.getSelectionModel().getSelectedItem();
                }
            }

            ContainerDiagramPane pane = null;

            if (borderPaneNode != null) {  //  that's if we in Selected
                if (borderPaneNode instanceof ContainerDiagramPane) {
                    Util.loggerControllerParameters.info("Reset Zoom clicked (logger 2/2) whilst in Selected");
                    // We need to downcast borderPaneNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) borderPaneNode;
                }
            }
            if (selectedSubTabByUser != null) { // that's if we in Checked
                Node selectedSubTabByUserNode = selectedSubTabByUser.getContent();
                if (selectedSubTabByUserNode instanceof ContainerDiagramPane) {
                    Util.loggerControllerParameters.info("Reset Zoom clicked (logger 2/2) whilst in Checked");
                    // We need to downcast selectedSubTabByUserNode into a ContainerDiagramPane
                    pane = (ContainerDiagramPane) selectedSubTabByUserNode;
                }
            }

            if (pane != null) {
                pane.getDiagramView().setZoom(1.0);
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
                if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
                    try {
                        drawNetwork();  // changes the variable svgWriter
                        ControllerDiagram.addSvgToSelectedTab();
                        ControllerDiagram.addSvgToSelectedTab();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Util.loggerControllerParameters.info("styleProvider variable successfully changed to 'NominalVoltageStyleProvider'");
            }
            else if (styleProviderChoice.getValue() == "Topological") {
                styleProvider = new TopologicalStyleProvider(NadCalls.networkProperty.get());
                if (!(getSvgWriter().toString().equals(new StringWriter().toString()))) {
                    try {
                        drawNetwork();  // changes the variable svgWriter
                        ControllerDiagram.addSvgToSelectedTab();
                        ControllerDiagram.addSvgToSelectedTab();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Util.loggerControllerParameters.info("styleProvider variable successfully changed to 'TopologicalStyleProvider'");
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

    private void setSvgEdgeInfoCheckbox(CheckBox svgEdgeInfoCheckbox)
    {
        svgEdgeInfoCheckbox.setOnAction(event ->
        {
            if (svgEdgeInfoCheckbox.isSelected()) {
                Util.loggerControllerParameters.info("SVG Edge Info Checked OK");
                setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(true));
            }
            else {
                Util.loggerControllerParameters.info("SVG Edge Info Unchecked OK");
                setParameters(svgParametersProperty.get().setEdgeInfoAlongEdge(false));
            }
        });
    }

    public static ParamPane getParamPane()
    {
        return paramPane;
    }

    public static StyleProvider getStyleProvider() {
        return styleProvider;
    }

    public static void setStyleProvider(StyleProvider styleProvider) {
        ControllerParameters.styleProvider = styleProvider;
    }

}
