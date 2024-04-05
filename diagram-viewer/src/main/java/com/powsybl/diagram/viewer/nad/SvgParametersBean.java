/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.nad.svg.SvgParameters;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SvgParametersBean {

    private final BooleanProperty idDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty edgeInfoAlongEdge;
    private final BooleanProperty edgeNameDisplayed;
    private final BooleanProperty insertNameDesc;
    private final BooleanProperty substationDescriptionDisplayed;
    private final BooleanProperty busLegend;
    private final BooleanProperty vlDetails;
    // Diagram size
    private final BooleanProperty widthHeightAdded;
    private final Property<SvgParameters.SizeConstraint> sizeConstraint;
    private final Property<Integer> fixedSize;
    private final Property<Double> fixedScale;

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
                             Property<Integer> fixedSize,
                             Property<Double> fixedScale) {
        // bind
        this.edgeInfoAlongEdge = edgeInfoAlongEdge;
        this.edgeNameDisplayed = edgeNameDisplayed;
        this.insertNameDesc = insertNameDesc;
        this.substationDescriptionDisplayed = substationDescriptionDisplayed;
        this.busLegend = busLegend;
        this.vlDetails = vlDetails;
        // Diagram size
        this.widthHeightAdded = widthHeightAdded;
        this.sizeConstraint = sizeConstraint;
        this.fixedSize = fixedSize;
        this.fixedScale = fixedScale;
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
                .setSizeConstraint(sizeConstraint.getValue());
        switch (sizeConstraint.getValue()) {
            case FIXED_HEIGHT -> svgParameters.setFixedHeight(fixedSize.getValue());
            case FIXED_WIDTH -> svgParameters.setFixedWidth(fixedSize.getValue());
            case FIXED_SCALE -> svgParameters.setFixedScale(fixedScale.getValue());
            case NONE -> svgParameters.setSizeConstraint(SvgParameters.SizeConstraint.NONE);
        }
        return svgParameters;
    }
}
