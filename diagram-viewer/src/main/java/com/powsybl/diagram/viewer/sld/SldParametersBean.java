/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.sld.SldParameters;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.ComponentTypeName;
import com.powsybl.sld.svg.SvgParameters;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SldParametersBean {

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

    private final BooleanProperty addNodesInfos = new SimpleBooleanProperty();

    private final BooleanProperty feederInfoSymmetry = new SimpleBooleanProperty();

    private final ObjectProperty<Double>spaceForFeederInfos = new SimpleObjectProperty<>();

    private final ObjectProperty<Double>feederInfosOuterMargin = new SimpleObjectProperty<>();

    private final ObjectProperty<Double>feederInfosIntraMargin = new SimpleObjectProperty<>();

    public SldParametersBean(Property<Double> diagramPaddingTopBottom,
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
        this.addNodesInfos.bindBidirectional(addNodesInfos);
        this.feederInfoSymmetry.bindBidirectional(feederInfoSymmetry);
        this.spaceForFeederInfos.bindBidirectional(spaceForFeederInfos);
        this.feederInfosOuterMargin.bindBidirectional(feederInfosOuterMargin);
        this.feederInfosIntraMargin.bindBidirectional(feederInfosIntraMargin);

        // Initialize
        SldParameters sldParameters = new SldParameters();
        LayoutParameters defaultLayoutParameters = sldParameters.getLayoutParameters();
        SvgParameters defaultSvgParameters = sldParameters.getSvgParameters();
        defaultSvgParameters
                .setShowGrid(true);
        defaultLayoutParameters
                .setAdaptCellHeightToContent(true);
        diagramPaddingTopBottom.setValue(defaultLayoutParameters.getDiagramPadding().getTop());
        diagramPaddingLeftRight.setValue(defaultLayoutParameters.getDiagramPadding().getLeft());
        voltagePaddingTopBottom.setValue(defaultLayoutParameters.getVoltageLevelPadding().getTop());
        voltagePaddingLeftRight.setValue(defaultLayoutParameters.getVoltageLevelPadding().getLeft());
        busbarVerticalSpace.setValue(defaultLayoutParameters.getVerticalSpaceBus());
        busbarHorizontalSpace.setValue(defaultLayoutParameters.getHorizontalBusPadding());
        cellWidth.setValue(defaultLayoutParameters.getCellWidth());
        externCellHeight.setValue(defaultLayoutParameters.getExternCellHeight());
        internCellHeight.setValue(defaultLayoutParameters.getInternCellHeight());
        stackHeight.setValue(defaultLayoutParameters.getStackHeight());
        disconnectorsOnBus.setValue(defaultLayoutParameters.getComponentsOnBusbars().equals(List.of(ComponentTypeName.DISCONNECTOR)));
        adaptCellHeightToContent.setValue(defaultLayoutParameters.isAdaptCellHeightToContent());
        minSpaceBetweenComponents.setValue(defaultLayoutParameters.getMinSpaceBetweenComponents());
        minimumExternCellHeight.setValue(defaultLayoutParameters.getMinExternCellHeight());
        busBarAlignment.setValue(defaultLayoutParameters.getBusbarsAlignment());

        spaceForFeederInfos.setValue(defaultLayoutParameters.getSpaceForFeederInfos());
        scaleFactor.setValue(defaultLayoutParameters.getCgmesScaleFactor());

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
        this.addNodesInfos.addListener(changeListener);
        this.feederInfoSymmetry.addListener(changeListener);
        this.spaceForFeederInfos.addListener(changeListener);
        this.feederInfosOuterMargin.addListener(changeListener);
        this.feederInfosIntraMargin.addListener(changeListener);
    }

    public SldParameters getParameters(String diagramName) {
        SldParameters sldParameters = new SldParameters();
        sldParameters.getLayoutParameters()
                .setDiagrammPadding(diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get(),
                        diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get())
                .setVoltageLevelPadding(voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get(),
                        voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get())
                .setCgmesUseNames(useName.get())
                .setCgmesDiagramName(diagramName)
                .setVerticalSpaceBus(busbarVerticalSpace.get())
                .setHorizontalBusPadding(busbarHorizontalSpace.get())
                .setCellWidth(cellWidth.get())
                .setExternCellHeight(externCellHeight.get())
                .setInternCellHeight(internCellHeight.get())
                .setStackHeight(stackHeight.get())
                .setComponentsOnBusbars(disconnectorsOnBus.get() ? List.of(ComponentTypeName.DISCONNECTOR) : Collections.emptyList())
                .setCgmesScaleFactor(scaleFactor.get())
                .setAdaptCellHeightToContent(adaptCellHeightToContent.get())
                .setMinSpaceBetweenComponents(minSpaceBetweenComponents.get())
                .setMinExternCellHeight(minimumExternCellHeight.get())
                .setBusbarsAlignment(busBarAlignment.get())
                .setSpaceForFeederInfos(spaceForFeederInfos.get());
        sldParameters.getSvgParameters()
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
                // Forced values
                .setCssLocation(SvgParameters.CssLocation.INSERTED_IN_SVG)
                .setSvgWidthAndHeightAdded(true);
        return sldParameters;
    }
}
