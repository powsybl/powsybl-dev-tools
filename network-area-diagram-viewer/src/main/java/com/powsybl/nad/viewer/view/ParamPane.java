/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.view;

import com.powsybl.nad.svg.StyleProvider;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ParamPane extends ScrollPane {
    private GridPane buttonsPane;
    private Button fitContentButton;
    private Button resetZoomButton;

    private GridPane choicePane;
    private ChoiceBox<String> layoutChoice;
    private ChoiceBox<String> labelProviderChoice;
    private ChoiceBox<String> styleProviderChoice;

    private GridPane layoutParametersPane;
    private Spinner<Double> layoutXSpinner;
    private Spinner<Double> layoutYSpinner;

    private GridPane svgParametersPane;

    private CheckBox svgEdgeInfoCheckbox;

    private int rowIndex = 0;

    // Parameters pane is a grid pane inside a scroll pane
    private GridPane contentPane;

    private StyleProvider styleProvider;  // inside it will be stored the dropdown list's selected value
    // NadCalls methods will read the value from this class when drawing diagrams

    public ParamPane() {
        createContentPane();

        createButtonPane();
        createChoiceBoxesPane();
        contentPane.add(new Text("LayoutParameter"), 0, rowIndex++);
        createLayoutParametersPane();
        contentPane.add(new Text("SvgParameters"), 0, rowIndex++);
        createSVGParametersPane();

        this.setContent(contentPane);
    }

    public void createContentPane() {
        contentPane = new GridPane();
        contentPane.setHgap(5);
        contentPane.setVgap(5);
    }

    public void createButtonPane() {
        fitContentButton = new Button("Fit to content");
        resetZoomButton = new Button("Reset zoom");

        buttonsPane = new GridPane();
        buttonsPane.add(fitContentButton, 0, 0);
        buttonsPane.add(resetZoomButton, 1, 0);
        buttonsPane.setHgap(10);

        contentPane.add(buttonsPane, 0, rowIndex++);
    }

    public void createChoiceBoxesPane() {
        layoutChoice = new ChoiceBox<>();
        layoutChoice.getItems().add("Basic");
        layoutChoice.getSelectionModel().selectFirst();
        Label layoutLabel = new Label("Layout");

        labelProviderChoice = new ChoiceBox<>();
        labelProviderChoice.getItems().add("Default");
        labelProviderChoice.getSelectionModel().selectFirst();
        Label labelProviderLabel = new Label("LabelProvider");

        styleProviderChoice = new ChoiceBox<>();
        styleProviderChoice.getItems().add("Topological");
        styleProviderChoice.getItems().add("Nominal");
        styleProviderChoice.getSelectionModel().selectFirst();  // make ChoiceBox styleProviderChoice
        // select its first element
        Label styleProviderLabel = new Label("StyleProvider");

        choicePane = new GridPane();
        choicePane.setPadding(new Insets(5, 5, 5, 5));
        choicePane.add(layoutChoice, 0, 1);
        choicePane.add(layoutLabel, 0, 0);
        choicePane.add(labelProviderChoice, 0, 3);
        choicePane.add(labelProviderLabel, 0, 2);
        choicePane.add(styleProviderChoice, 0, 5);
        choicePane.add(styleProviderLabel, 0, 4);

        contentPane.add(choicePane, 0, rowIndex++);
    }

    public void createLayoutParametersPane() {
        layoutXSpinner = new Spinner<>();
        Label layoutXLabel = new Label("Xxx");

        layoutYSpinner = new Spinner<>();
        Label layoutYLabel = new Label("Yyy");

        layoutParametersPane = new GridPane();
        layoutParametersPane.setPadding(new Insets(5, 5, 5, 5));
        layoutParametersPane.add(layoutXSpinner, 0, 1);
        layoutParametersPane.add(layoutXLabel, 0, 0);
        layoutParametersPane.add(layoutYSpinner, 0, 3);
        layoutParametersPane.add(layoutYLabel, 0, 2);

        contentPane.add(layoutParametersPane, 0, rowIndex++);
    }

    public void createSVGParametersPane() {
        svgEdgeInfoCheckbox = new CheckBox();
        svgEdgeInfoCheckbox.setSelected(true);

        svgParametersPane = new GridPane();
        svgParametersPane.setPadding(new Insets(5, 5, 5, 5));

        svgParametersPane.add(new Label("Set Info Along Edge"), 0, 4);
        svgParametersPane.add(svgEdgeInfoCheckbox, 0, 5);

        contentPane.add(svgParametersPane, 0, rowIndex++);
    }

    public Button getFitContentButton() {
        return fitContentButton;
    }

    public Button getResetZoomButton() {
        return resetZoomButton;
    }

    public GridPane getButtonsPane() {
        return buttonsPane;
    }

    public GridPane getChoicePane() {
        return choicePane;
    }

    public ChoiceBox<String> getLayoutChoice() {
        return layoutChoice;
    }

    public ChoiceBox<String> getLabelProviderChoice() {
        return labelProviderChoice;
    }

    public ChoiceBox<String> getStyleProviderChoice() {
        return styleProviderChoice;
    }

    public GridPane getLayoutParametersPane() {
        return layoutParametersPane;
    }

    public Spinner<Double> getLayoutXSpinner() {
        return layoutXSpinner;
    }

    public Spinner<Double> getLayoutYSpinner() {
        return layoutYSpinner;
    }

    public GridPane getSvgParametersPane() {
        return svgParametersPane;
    }

    public CheckBox getSvgEdgeInfoCheckbox() {
        return svgEdgeInfoCheckbox;
    }

    public StyleProvider getStyleProvider() {
        return styleProvider;
    }

    public void setStyleProvider(StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
    }
}
