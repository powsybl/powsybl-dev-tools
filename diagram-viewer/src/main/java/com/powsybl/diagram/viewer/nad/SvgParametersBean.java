/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.nad.svg.SvgParameters;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SvgParametersBean {

    private final BooleanProperty idDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty edgeInfoAlongEdge = new SimpleBooleanProperty();
    private final BooleanProperty edgeNameDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty insertNameDesc = new SimpleBooleanProperty();
    private final BooleanProperty substationDescriptionDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty busLegend = new SimpleBooleanProperty();
    private final BooleanProperty vlDetails = new SimpleBooleanProperty();
    // Diagram size
    private final BooleanProperty widthHeightAdded = new SimpleBooleanProperty();
    private final ObjectProperty<SvgParameters.SizeConstraint> sizeConstraint = new SimpleObjectProperty<>(this, "sizeConstraint", SvgParameters.SizeConstraint.NONE);
    private final ObjectProperty<Double> fixedSize = new SimpleObjectProperty<>();
    private final ObjectProperty<Double> fixedScale = new SimpleObjectProperty<>();

    public SvgParametersBean(// SVG parameters
                             BooleanProperty edgeInfoAlongEdge,
                             BooleanProperty edgeNameDisplayed,
                             BooleanProperty insertNameDesc,
                             BooleanProperty substationDescriptionDisplayed,
                             BooleanProperty busLegend,
                             BooleanProperty vlDetails,
                             // Diagram size
                             BooleanProperty widthHeightAdded,
                             Property<SvgParameters.SizeConstraint> sizeConstraint,
                             Property<Double> fixedSize,
                             Property<Double> fixedScale) {
        // bind
        this.edgeInfoAlongEdge.bindBidirectional(edgeInfoAlongEdge);
        this.edgeNameDisplayed.bindBidirectional(edgeNameDisplayed);
        this.insertNameDesc.bindBidirectional(insertNameDesc);
        this.substationDescriptionDisplayed.bindBidirectional(substationDescriptionDisplayed);
        this.busLegend.bindBidirectional(busLegend);
        this.vlDetails.bindBidirectional(vlDetails);
        // Diagram size
        this.widthHeightAdded.bindBidirectional(widthHeightAdded);
        this.sizeConstraint.bindBidirectional(sizeConstraint);
        this.fixedSize.bindBidirectional(fixedSize);
        this.fixedScale.bindBidirectional(fixedScale);

        // Initialize
        SvgParameters defaultParameters = new SvgParameters();
        idDisplayed.setValue(defaultParameters.isIdDisplayed());
        edgeInfoAlongEdge.setValue(defaultParameters.isEdgeInfoAlongEdge());
        edgeNameDisplayed.setValue(defaultParameters.isEdgeNameDisplayed());
        insertNameDesc.setValue(defaultParameters.isInsertNameDesc());
        substationDescriptionDisplayed.setValue(defaultParameters.isSubstationDescriptionDisplayed());
        busLegend.setValue(defaultParameters.isBusLegend());
        vlDetails.setValue(defaultParameters.isVoltageLevelDetails());
        // Diagram size
        widthHeightAdded.setValue(defaultParameters.isSvgWidthAndHeightAdded());
        sizeConstraint.setValue(defaultParameters.getSizeConstraint());
        switch (defaultParameters.getSizeConstraint()) {
            case FIXED_HEIGHT:
                fixedSize.setValue((double) defaultParameters.getFixedHeight());
                break;
            case FIXED_WIDTH:
                fixedSize.setValue((double) defaultParameters.getFixedWidth());
                break;
            case FIXED_SCALE:
                fixedScale.setValue(defaultParameters.getFixedScale());
                break;
            default:
                break;
        }
    }

    public void bind(BooleanProperty useName) {
        this.idDisplayed.bind(useName.not());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.idDisplayed.addListener(changeListener);
        this.edgeInfoAlongEdge.addListener(changeListener);
        this.edgeNameDisplayed.addListener(changeListener);
        this.insertNameDesc.addListener(changeListener);
        this.substationDescriptionDisplayed.addListener(changeListener);
        this.busLegend.addListener(changeListener);
        this.vlDetails.addListener(changeListener);
        // Diagram size
        this.widthHeightAdded.addListener(changeListener);
        this.sizeConstraint.addListener(changeListener);
        this.fixedSize.addListener(changeListener);
        this.fixedScale.addListener(changeListener);
    }

    public SvgParameters getSvgParameters() {
        SvgParameters svgParameters = new SvgParameters()
                .setIdDisplayed(idDisplayed.get())
                .setInsertNameDesc(insertNameDesc.get())
                .setSubstationDescriptionDisplayed(substationDescriptionDisplayed.get())
                .setBusLegend(busLegend.get())
                .setVoltageLevelDetails(vlDetails.get())
                .setEdgeInfoAlongEdge(edgeInfoAlongEdge.get())
                .setEdgeNameDisplayed(edgeNameDisplayed.get())
                .setSvgWidthAndHeightAdded(widthHeightAdded.get())
                .setSizeConstraint(sizeConstraint.get());
        switch (sizeConstraint.get()) {
            case FIXED_HEIGHT:
                svgParameters.setFixedHeight((int) Math.round(fixedSize.get()));
                break;
            case FIXED_WIDTH:
                svgParameters.setFixedWidth((int) Math.round(fixedSize.get()));
                break;
            case FIXED_SCALE:
                svgParameters.setFixedScale(fixedScale.get());
                break;
            default:
                break;
        }
        return svgParameters;
    }
}
