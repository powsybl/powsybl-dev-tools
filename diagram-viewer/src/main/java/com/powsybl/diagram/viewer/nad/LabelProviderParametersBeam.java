/*
 * Copyright (c) 2026, RTE (https://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.diagram.viewer.nad;

import com.powsybl.nad.svg.LabelProviderParameters;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class LabelProviderParametersBeam {

    private final BooleanProperty busLegend;
    private final BooleanProperty idDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty substationDescriptionDisplayed;
    private final BooleanProperty voltageLevelDetails;

    public LabelProviderParametersBeam(BooleanProperty substationDescriptionDisplayed,
                                       BooleanProperty busLegend,
                                       BooleanProperty voltageLevelDetails) {
        this.substationDescriptionDisplayed = substationDescriptionDisplayed;
        this.busLegend = busLegend;
        this.voltageLevelDetails = voltageLevelDetails;
    }

    public void bind(BooleanProperty useName) {
        this.idDisplayed.bind(useName.not());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.idDisplayed.addListener(changeListener);
        this.substationDescriptionDisplayed.addListener(changeListener);
        this.busLegend.addListener(changeListener);
        this.voltageLevelDetails.addListener(changeListener);
    }

    public LabelProviderParameters getLabelProviderParameters() {
        return new LabelProviderParameters()
            .setBusLegend(busLegend.get())
            .setIdDisplayed(idDisplayed.get())
            .setSubstationDescriptionDisplayed(substationDescriptionDisplayed.get())
            .setVoltageLevelDetails(voltageLevelDetails.get());
    }
}
