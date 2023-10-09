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
    private final BooleanProperty disconnectorsOnBus = new SimpleBooleanProperty();
    private final ObjectProperty<Double> scaleFactor = new SimpleObjectProperty<>();
    private final BooleanProperty adaptCellHeightToContent = new SimpleBooleanProperty();
    private final ObjectProperty<Double>minSpaceBetweenComponents = new SimpleObjectProperty<>();
    private final ObjectProperty<Double>minimumExternCellHeight = new SimpleObjectProperty<>();
    private final ObjectProperty<LayoutParameters.Alignment> busBarAlignment = new SimpleObjectProperty<>();
    private final ObjectProperty<Double>spaceForFeederInfos = new SimpleObjectProperty<>();

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
                             BooleanProperty disconnectorsOnBus,
                             Property<Double> scaleFactor,
                             BooleanProperty adaptCellHeightToContent,
                             Property<Double> minSpaceBetweenComponents,
                             Property<Double> minimumExternCellHeight,
                             Property<LayoutParameters.Alignment> busBarAlignment,
                             Property<Double> spaceForFeederInfos) {
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
        this.disconnectorsOnBus.bindBidirectional(disconnectorsOnBus);
        this.scaleFactor.bindBidirectional(scaleFactor);
        this.adaptCellHeightToContent.bindBidirectional(adaptCellHeightToContent);
        this.minSpaceBetweenComponents.bindBidirectional(minSpaceBetweenComponents);
        this.minimumExternCellHeight.bindBidirectional(minimumExternCellHeight);
        this.busBarAlignment.bindBidirectional(busBarAlignment);
        this.spaceForFeederInfos.bindBidirectional(spaceForFeederInfos);

        // Initialize
        LayoutParameters defaultLayoutParameters = new LayoutParameters()
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
    }

    public void addListener(ChangeListener<Object> changeListener) {
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
        this.disconnectorsOnBus.addListener(changeListener);
        this.scaleFactor.addListener(changeListener);
        this.adaptCellHeightToContent.addListener(changeListener);
        this.minSpaceBetweenComponents.addListener(changeListener);
        this.minimumExternCellHeight.addListener(changeListener);
        this.busBarAlignment.addListener(changeListener);
        this.spaceForFeederInfos.addListener(changeListener);
    }

    public LayoutParameters getLayoutParameters() {
        return new LayoutParameters()
                .setDiagrammPadding(diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get(),
                        diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get())
                .setVoltageLevelPadding(voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get(),
                        voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get())
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
    }
}
