/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import com.powsybl.nad.layout.LayoutParameters;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class ParamPane extends ScrollPane
{
    private GridPane buttonsPane;
    private Button fitContentButton;
    private Button resetZoomButton;

    private GridPane choicePane;
    private ChoiceBox layoutChoice;
    private ChoiceBox labelProviderChoice;
    private ChoiceBox styleProviderChoice;

    private GridPane layoutParametersPane;
    private Spinner layoutXSpinner;
    private Spinner layoutYSpinner;

    private GridPane svgParametersPane;
    private Spinner svgXSpinner;
    private Spinner svgYSpinner;
    private CheckBox svgZCheck;

    private int rowIndex = 0;

    // Parameters pane is a grid pane inside a scroll pane
    private GridPane contentPane;

    public ParamPane()
    {
        createContentPane();

        createButtonPane();
        createChoiceBoxesPane();
        contentPane.add(new Text("LayoutParameter"), 0, rowIndex++);
        createLayoutParametersPane();
        contentPane.add(new Text("SvgParameters"), 0, rowIndex++);
        createSVGParametersPane();

        this.setContent(contentPane);
    }

    public void createContentPane()
    {
        contentPane = new GridPane();
        contentPane.setHgap(5);
        contentPane.setVgap(5);
    }
    public void createButtonPane()
    {
        fitContentButton = new Button("Fit to content");
        resetZoomButton  = new Button("Reset zoom");

        buttonsPane = new GridPane();
        buttonsPane.add(fitContentButton, 0, 0);
        buttonsPane.add(resetZoomButton, 1, 0);
        buttonsPane.setHgap(10);

        contentPane.add(buttonsPane, 0, rowIndex++);
    }

    public void createChoiceBoxesPane()
    {
        layoutChoice = new ChoiceBox();
        layoutChoice.getItems().add("Horizontal");
        layoutChoice.getItems().add("Vertical");
        layoutChoice.getItems().add("Cgmes");
        layoutChoice.getItems().add("Smart");
        layoutChoice.getItems().add("Smart with horizontal compaction");
        layoutChoice.getItems().add("Smart with vertical compaction");
        Label layoutLabel = new Label("Layout");

        labelProviderChoice = new ChoiceBox();
        labelProviderChoice.getItems().add("Default");
        Label labelProviderLabel     = new Label("LabelProvider");

        styleProviderChoice = new ChoiceBox();
        styleProviderChoice.getItems().add("Default");
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

    public void createLayoutParametersPane()
    {
        layoutXSpinner = new Spinner();
        Label layoutXLabel = new Label("Xxx");

        layoutYSpinner = new Spinner();
        Label layoutYLabel = new Label("Yyy");

        layoutParametersPane = new GridPane();
        layoutParametersPane.setPadding(new Insets(5, 5, 5, 5));
        layoutParametersPane.add(layoutXSpinner, 0, 1);
        layoutParametersPane.add(layoutXLabel, 0, 0);
        layoutParametersPane.add(layoutYSpinner, 0, 3);
        layoutParametersPane.add(layoutYLabel, 0, 2);

        contentPane.add(layoutParametersPane, 0, rowIndex++);
    }

    public void createSVGParametersPane()
    {
        svgXSpinner = new Spinner();
        Label svgXLabel = new Label("Xxx");

        svgYSpinner = new Spinner();
        Label svgYLabel = new Label("Yyy");

        svgZCheck = new CheckBox();
        Label svgZLabel = new Label("Zzz");


        svgParametersPane = new GridPane();
        svgParametersPane.setPadding(new Insets(5, 5, 5, 5));
        svgParametersPane.add(svgXSpinner, 0, 1);
        svgParametersPane.add(svgXLabel, 0, 0);
        svgParametersPane.add(svgYSpinner, 0, 3);
        svgParametersPane.add(svgYLabel, 0, 2);
        svgParametersPane.add(svgZCheck, 1, 4);
        svgParametersPane.add(svgZLabel, 0, 4);

        contentPane.add(svgParametersPane, 0, rowIndex++);


    }

    public Button getFitContentButton()
    {
        return fitContentButton;
    }

    public Button getResetZoomButton()
    {
        return resetZoomButton;
    }

    public GridPane getButtonsPane()
    {
        return buttonsPane;
    }

    public GridPane getChoicePane() {
        return choicePane;
    }

    public ChoiceBox getLayoutChoice() {
        return layoutChoice;
    }

    public ChoiceBox getLabelProviderChoice() {
        return labelProviderChoice;
    }

    public ChoiceBox getStyleProviderChoice() {
        return styleProviderChoice;
    }

    public GridPane getLayoutParametersPane() {
        return layoutParametersPane;
    }

    public Spinner getLayoutXSpinner() {
        return layoutXSpinner;
    }

    public Spinner getLayoutYSpinner() {
        return layoutYSpinner;
    }

    public GridPane getSvgParametersPane() {
        return svgParametersPane;
    }

    public Spinner getSvgXSpinner() {
        return svgXSpinner;
    }

    public Spinner getSvgYSpinner() {
        return svgYSpinner;
    }

    public CheckBox getSvgZCheck() {
        return svgZCheck;
    }

}
