/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.sld.svg.SvgParameters;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.*;

/**
 * @author Sophie Frasnedo <sophie.frasnedo at rte-france.com>
 */
public class SvgParametersBean {

    private final BooleanProperty useName = new SimpleBooleanProperty();
    private final BooleanProperty showGrid = new SimpleBooleanProperty();
    private final BooleanProperty showInternalNodes = new SimpleBooleanProperty();
    private final BooleanProperty drawStraightWires = new SimpleBooleanProperty();
    private final BooleanProperty avoidSVGComponentsDuplication = new SimpleBooleanProperty();
    private final BooleanProperty centerLabel = new SimpleBooleanProperty();
    private final BooleanProperty labelDiagonal = new SimpleBooleanProperty();
    private final ObjectProperty<Double>angleLabel = new SimpleObjectProperty<>();
    private final BooleanProperty addNodesInfos = new SimpleBooleanProperty();
    private final BooleanProperty feederInfoSymmetry = new SimpleBooleanProperty();
    private final ObjectProperty<Double>feederInfosOuterMargin = new SimpleObjectProperty<>();
    private final ObjectProperty<Double>feederInfosIntraMargin = new SimpleObjectProperty<>();

    public SvgParametersBean(BooleanProperty showGrid,
                             BooleanProperty showInternalNodes,
                             BooleanProperty drawStraightWires,
                             BooleanProperty avoidSVGComponentsDuplication,
                             BooleanProperty centerLabel,
                             BooleanProperty labelDiagonal,
                             Property<Double> angleLabel,
                             BooleanProperty addNodesInfos,
                             BooleanProperty feederInfoSymmetry,
                             Property<Double> feederInfosOuterMargin,
                             Property<Double> feederInfosIntraMargin) {
        // bind
        this.showGrid.bindBidirectional(showGrid);
        this.showInternalNodes.bindBidirectional(showInternalNodes);
        this.drawStraightWires.bindBidirectional(drawStraightWires);
        this.avoidSVGComponentsDuplication.bindBidirectional(avoidSVGComponentsDuplication);
        this.centerLabel.bindBidirectional(centerLabel);
        this.labelDiagonal.bindBidirectional(labelDiagonal);
        this.angleLabel.bindBidirectional(angleLabel);
        this.addNodesInfos.bindBidirectional(addNodesInfos);
        this.feederInfoSymmetry.bindBidirectional(feederInfoSymmetry);
        this.feederInfosOuterMargin.bindBidirectional(feederInfosOuterMargin);
        this.feederInfosIntraMargin.bindBidirectional(feederInfosIntraMargin);

        // Initialize
        SvgParameters defaultSvgParameters = new SvgParameters()
                .setShowGrid(true);

        useName.setValue(defaultSvgParameters.isUseName());
        showGrid.setValue(defaultSvgParameters.isShowGrid());
        showInternalNodes.setValue(defaultSvgParameters.isShowInternalNodes());
        drawStraightWires.setValue(defaultSvgParameters.isDrawStraightWires());
        avoidSVGComponentsDuplication.setValue(defaultSvgParameters.isAvoidSVGComponentsDuplication());
        centerLabel.setValue(defaultSvgParameters.isLabelCentered());
        labelDiagonal.setValue(defaultSvgParameters.isLabelDiagonal());
        angleLabel.setValue(defaultSvgParameters.getAngleLabelShift());
        addNodesInfos.setValue(defaultSvgParameters.isAddNodesInfos());
        feederInfoSymmetry.setValue(defaultSvgParameters.isFeederInfoSymmetry());
        feederInfosOuterMargin.setValue(defaultSvgParameters.getFeederInfosOuterMargin());
        feederInfosIntraMargin.setValue(defaultSvgParameters.getFeederInfosIntraMargin());
    }

    public void bind(BooleanProperty useName) {
        this.useName.bindBidirectional(useName);
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.useName.addListener(changeListener);
        this.showGrid.addListener(changeListener);
        this.showInternalNodes.addListener(changeListener);
        this.drawStraightWires.addListener(changeListener);
        this.avoidSVGComponentsDuplication.addListener(changeListener);
        this.centerLabel.addListener(changeListener);
        this.labelDiagonal.addListener(changeListener);
        this.angleLabel.addListener(changeListener);
        this.addNodesInfos.addListener(changeListener);
        this.feederInfoSymmetry.addListener(changeListener);
        this.feederInfosOuterMargin.addListener(changeListener);
        this.feederInfosIntraMargin.addListener(changeListener);
    }

    public SvgParameters getSvgParameters(String diagramName) {
        return new SvgParameters()
                .setShowGrid(showGrid.get())
                .setShowInternalNodes(showInternalNodes.get())
                .setDrawStraightWires(drawStraightWires.get())
                .setAvoidSVGComponentsDuplication(avoidSVGComponentsDuplication.get())
                .setLabelCentered(centerLabel.get())
                .setLabelDiagonal(labelDiagonal.get())
                .setAngleLabelShift(angleLabel.get())
                .setAddNodesInfos(addNodesInfos.get())
                .setFeederInfoSymmetry(feederInfoSymmetry.get())
                .setFeederInfosOuterMargin(feederInfosOuterMargin.get())
                .setFeederInfosIntraMargin(feederInfosIntraMargin.get())
                .setDiagramName(diagramName)
                // Forced values
                .setCssLocation(SvgParameters.CssLocation.INSERTED_IN_SVG)
                .setSvgWidthAndHeightAdded(true);
    }
}
