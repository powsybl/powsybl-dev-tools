package com.powsybl.ad.viewer.controller;
import com.powsybl.ad.viewer.model.NadCalls;
import com.powsybl.ad.viewer.view.ParamPane;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ControllerParameters
{
    private Stage primaryStage;
    private ParamPane paramPane;


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
            System.out.println("Style Provider : " + styleProviderChoice.getValue());
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
