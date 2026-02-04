/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.nad.layout.LayoutParameters;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class LayoutParametersBean {

    private final BooleanProperty textNodesIncluded = new SimpleBooleanProperty();

    private final Property<Double> springRepulsionFactor = new SimpleObjectProperty<>();
    private final Property<Integer> nbMaxSteps = new SimpleObjectProperty<>();
    private final DoubleProperty scaleFactor = new SimpleDoubleProperty();

    public LayoutParametersBean(BooleanProperty textNodesIncluded, Property<Double> springRepulsionFactor,
                                Property<Integer> nbMaxSteps, DoubleProperty scaleFactor) {
        // bind
        this.textNodesIncluded.bindBidirectional(textNodesIncluded);
        this.springRepulsionFactor.bindBidirectional(springRepulsionFactor);
        this.nbMaxSteps.bindBidirectional(nbMaxSteps);
        this.scaleFactor.bindBidirectional(scaleFactor);

        // Initialize
        LayoutParameters defaultParameters = new LayoutParameters();
        textNodesIncluded.setValue(defaultParameters.isTextNodesForceLayout());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.textNodesIncluded.addListener(changeListener);
        this.springRepulsionFactor.addListener(changeListener);
        this.nbMaxSteps.addListener(changeListener);
        this.scaleFactor.addListener(changeListener);
    }

    public LayoutParameters getLayoutParameters() {
        return new LayoutParameters()
                .setTextNodesForceLayout(textNodesIncluded.get())
                .setMaxSteps(nbMaxSteps.getValue())
                .setScaleFactor(scaleFactor.getValue());
    }
}
