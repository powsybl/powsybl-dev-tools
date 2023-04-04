/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.ComponentTypeName;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class LayoutParametersBean {

    private final BooleanProperty useName = new SimpleBooleanProperty();

    // Layout Parameters
    private final ObjectProperty<Double> diagramPaddingTopBottom = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> diagramPaddingLeftRight = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> voltagePaddingTopBottom = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> voltagePaddingLeftRight = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> busbarVerticalSpace = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> busbarHorizontalSpace = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> cellWidth = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> externCellHeight = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> internCellHeight = new SimpleObjectProperty<>();

    private final ObjectProperty<Double> stackHeight = new SimpleObjectProperty<>();

    private final BooleanProperty showGrid = new SimpleBooleanProperty();

    private final BooleanProperty showInternalNodes = new SimpleBooleanProperty();

    private final BooleanProperty drawStraightWires = new SimpleBooleanProperty();

    private final BooleanProperty disconnectorsOnBus = new SimpleBooleanProperty();

    private final ObjectProperty<Double> scaleFactor = new SimpleObjectProperty<>();

    private final BooleanProperty avoidSVGComponentsDuplication = new SimpleBooleanProperty();

    private final BooleanProperty adaptCellHeightToContent = new SimpleBooleanProperty();

    private final ObjectProperty<Double>minSpaceBetweenComponents = new SimpleObjectProperty<>();

    private final ObjectProperty<Double>minimumExternCellHeight = new SimpleObjectProperty<>();

    private final ObjectProperty<LayoutParameters.Alignment> busBarAlignment = new SimpleObjectProperty<>();

    private final BooleanProperty centerLabel = new SimpleBooleanProperty();

    private final BooleanProperty labelDiagonal = new SimpleBooleanProperty();

    private final ObjectProperty<Double>angleLabel = new SimpleObjectProperty<>();

    private final BooleanProperty highLightLineState = new SimpleBooleanProperty();

    private final BooleanProperty addNodesInfos = new SimpleBooleanProperty();

    private final BooleanProperty feederInfoSymmetry = new SimpleBooleanProperty();

    private final ObjectProperty<Double>spaceForFeederInfos = new SimpleObjectProperty<>();

    private final ObjectProperty<Double>feederInfosOuterMargin = new SimpleObjectProperty<>();

    private final ObjectProperty<Double>feederInfosIntraMargin = new SimpleObjectProperty<>();

    public LayoutParametersBean(Property<Double> diagramPaddingTopBottom,
                                Property<Double> diagramPaddingLeftRight,
                                Property<Double> voltagePaddingTopBottom,
                                Property<Double> voltagePaddingLeftRight,
                                Property<Double> busbarVerticalSpace,
                                Property<Double> busbarHorizontalSpace,
                                Property<Double> cellWidth,
                                Property<Double> externCellHeight,
                                Property<Double> internCellHeight,
                                Property<Double> stackHeight,
                                BooleanProperty showGrid,
                                BooleanProperty showInternalNodes,
                                BooleanProperty drawStraightWires,
                                BooleanProperty disconnectorsOnBus,
                                Property<Double> scaleFactor,
                                BooleanProperty avoidSVGComponentsDuplication,
                                BooleanProperty adaptCellHeightToContent,
                                Property<Double> minSpaceBetweenComponents,
                                Property<Double> minimumExternCellHeight,
                                Property<LayoutParameters.Alignment> busBarAlignment,
                                BooleanProperty centerLabel,
                                BooleanProperty labelDiagonal,
                                Property<Double> angleLabel,
                                BooleanProperty highLightLineState,
                                BooleanProperty addNodesInfos,
                                BooleanProperty feederInfoSymmetry,
                                Property<Double> spaceForFeederInfos,
                                Property<Double> feederInfosOuterMargin,
                                Property<Double> feederInfosIntraMargin) {
        // bind
        this.diagramPaddingTopBottom.bindBidirectional(diagramPaddingTopBottom);
        this.diagramPaddingTopBottom.bindBidirectional(diagramPaddingTopBottom);
        this.diagramPaddingLeftRight.bindBidirectional(diagramPaddingLeftRight);
        this.voltagePaddingTopBottom.bindBidirectional(voltagePaddingTopBottom);
        this.voltagePaddingLeftRight.bindBidirectional(voltagePaddingLeftRight);
        this.busbarVerticalSpace.bindBidirectional(busbarVerticalSpace);
        this.busbarHorizontalSpace.bindBidirectional(busbarHorizontalSpace);
        this.cellWidth.bindBidirectional(cellWidth);
        this.externCellHeight.bindBidirectional(externCellHeight);
        this.internCellHeight.bindBidirectional(internCellHeight);
        this.stackHeight.bindBidirectional(stackHeight);
        this.showGrid.bindBidirectional(showGrid);
        this.showInternalNodes.bindBidirectional(showInternalNodes);
        this.drawStraightWires.bindBidirectional(drawStraightWires);
        this.disconnectorsOnBus.bindBidirectional(disconnectorsOnBus);
        this.scaleFactor.bindBidirectional(scaleFactor);
        this.avoidSVGComponentsDuplication.bindBidirectional(avoidSVGComponentsDuplication);
        this.adaptCellHeightToContent.bindBidirectional(adaptCellHeightToContent);
        this.minSpaceBetweenComponents.bindBidirectional(minSpaceBetweenComponents);
        this.minimumExternCellHeight.bindBidirectional(minimumExternCellHeight);
        this.busBarAlignment.bindBidirectional(busBarAlignment);
        this.centerLabel.bindBidirectional(centerLabel);
        this.labelDiagonal.bindBidirectional(labelDiagonal);
        this.angleLabel.bindBidirectional(angleLabel);
        this.highLightLineState.bindBidirectional(highLightLineState);
        this.addNodesInfos.bindBidirectional(addNodesInfos);
        this.feederInfoSymmetry.bindBidirectional(feederInfoSymmetry);
        this.spaceForFeederInfos.bindBidirectional(spaceForFeederInfos);
        this.feederInfosOuterMargin.bindBidirectional(feederInfosOuterMargin);
        this.feederInfosIntraMargin.bindBidirectional(feederInfosIntraMargin);

        // Initialize
        LayoutParameters defaultParameters = new LayoutParameters()
                .setShowGrid(true)
                .setAdaptCellHeightToContent(true);
        diagramPaddingTopBottom.setValue(defaultParameters.getDiagramPadding().getTop());
        diagramPaddingLeftRight.setValue(defaultParameters.getDiagramPadding().getLeft());
        voltagePaddingTopBottom.setValue(defaultParameters.getVoltageLevelPadding().getTop());
        voltagePaddingLeftRight.setValue(defaultParameters.getVoltageLevelPadding().getLeft());
        busbarVerticalSpace.setValue(defaultParameters.getVerticalSpaceBus());
        busbarHorizontalSpace.setValue(defaultParameters.getHorizontalBusPadding());
        cellWidth.setValue(defaultParameters.getCellWidth());
        externCellHeight.setValue(defaultParameters.getExternCellHeight());
        internCellHeight.setValue(defaultParameters.getInternCellHeight());
        stackHeight.setValue(defaultParameters.getStackHeight());
        showGrid.setValue(defaultParameters.isShowGrid());
        showInternalNodes.setValue(defaultParameters.isShowInternalNodes());
        drawStraightWires.setValue(defaultParameters.isDrawStraightWires());
        disconnectorsOnBus.setValue(defaultParameters.getComponentsOnBusbars().equals(List.of(ComponentTypeName.DISCONNECTOR)));
        scaleFactor.setValue(defaultParameters.getScaleFactor());
        avoidSVGComponentsDuplication.setValue(defaultParameters.isAvoidSVGComponentsDuplication());
        adaptCellHeightToContent.setValue(defaultParameters.isAdaptCellHeightToContent());
        minSpaceBetweenComponents.setValue(defaultParameters.getMinSpaceBetweenComponents());
        minimumExternCellHeight.setValue(defaultParameters.getMinExternCellHeight());
        busBarAlignment.setValue(defaultParameters.getBusbarsAlignment());
        centerLabel.setValue(defaultParameters.isLabelCentered());
        labelDiagonal.setValue(defaultParameters.isLabelDiagonal());
        angleLabel.setValue(defaultParameters.getAngleLabelShift());
        highLightLineState.setValue(defaultParameters.isHighlightLineState());
        addNodesInfos.setValue(defaultParameters.isAddNodesInfos());
        feederInfoSymmetry.setValue(defaultParameters.isFeederInfoSymmetry());
        spaceForFeederInfos.setValue(defaultParameters.getSpaceForFeederInfos());
        feederInfosOuterMargin.setValue(defaultParameters.getFeederInfosOuterMargin());
        feederInfosIntraMargin.setValue(defaultParameters.getFeederInfosIntraMargin());
    }

    public void bind(BooleanProperty useName) {
        this.useName.bind(useName);
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.useName.addListener(changeListener);
        this.diagramPaddingTopBottom.addListener(changeListener);
        this.diagramPaddingTopBottom.addListener(changeListener);
        this.diagramPaddingLeftRight.addListener(changeListener);
        this.voltagePaddingTopBottom.addListener(changeListener);
        this.voltagePaddingLeftRight.addListener(changeListener);
        this.busbarVerticalSpace.addListener(changeListener);
        this.busbarHorizontalSpace.addListener(changeListener);
        this.cellWidth.addListener(changeListener);
        this.externCellHeight.addListener(changeListener);
        this.internCellHeight.addListener(changeListener);
        this.stackHeight.addListener(changeListener);
        this.showGrid.addListener(changeListener);
        this.showInternalNodes.addListener(changeListener);
        this.drawStraightWires.addListener(changeListener);
        this.disconnectorsOnBus.addListener(changeListener);
        this.scaleFactor.addListener(changeListener);
        this.avoidSVGComponentsDuplication.addListener(changeListener);
        this.adaptCellHeightToContent.addListener(changeListener);
        this.minSpaceBetweenComponents.addListener(changeListener);
        this.minimumExternCellHeight.addListener(changeListener);
        this.busBarAlignment.addListener(changeListener);
        this.centerLabel.addListener(changeListener);
        this.labelDiagonal.addListener(changeListener);
        this.angleLabel.addListener(changeListener);
        this.highLightLineState.addListener(changeListener);
        this.addNodesInfos.addListener(changeListener);
        this.feederInfoSymmetry.addListener(changeListener);
        this.spaceForFeederInfos.addListener(changeListener);
        this.feederInfosOuterMargin.addListener(changeListener);
        this.feederInfosIntraMargin.addListener(changeListener);
    }

    public LayoutParameters getLayoutParameters(String diagramName) {
        return new LayoutParameters()
                .setDiagrammPadding(diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get(),
                        diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get())
                .setVoltageLevelPadding(voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get(),
                        voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get())
                .setUseName(useName.get())
                .setDiagramName(diagramName)
                .setVerticalSpaceBus(busbarVerticalSpace.get())
                .setHorizontalBusPadding(busbarHorizontalSpace.get())
                .setCellWidth(cellWidth.get())
                .setExternCellHeight(externCellHeight.get())
                .setInternCellHeight(internCellHeight.get())
                .setStackHeight(stackHeight.get())
                .setShowGrid(showGrid.get())
                .setShowInternalNodes(showInternalNodes.get())
                .setDrawStraightWires(drawStraightWires.get())
                .setComponentsOnBusbars(disconnectorsOnBus.get() ? List.of(ComponentTypeName.DISCONNECTOR) : Collections.emptyList())
                .setScaleFactor(scaleFactor.get())
                .setAvoidSVGComponentsDuplication(avoidSVGComponentsDuplication.get())
                .setAdaptCellHeightToContent(adaptCellHeightToContent.get())
                .setMinSpaceBetweenComponents(minSpaceBetweenComponents.get())
                .setMinExternCellHeight(minimumExternCellHeight.get())
                .setBusbarsAlignment(busBarAlignment.get())
                .setLabelCentered(centerLabel.get())
                .setLabelDiagonal(labelDiagonal.get())
                .setAngleLabelShift(angleLabel.get())
                .setHighlightLineState(highLightLineState.get())
                .setAddNodesInfos(addNodesInfos.get())
                .setFeederInfoSymmetry(feederInfoSymmetry.get())
                .setSpaceForFeederInfos(spaceForFeederInfos.get())
                .setFeederInfosOuterMargin(feederInfosOuterMargin.get())
                .setFeederInfosIntraMargin(feederInfosIntraMargin.get())
                // Forced values
                .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                .setSvgWidthAndHeightAdded(true);
    }
}
