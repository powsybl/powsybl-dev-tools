/*
 * Copyright (c) 2026, RTE (https://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.diagram.viewer.nad;

import com.powsybl.nad.svg.iidm.DefaultLabelProvider;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class EdgeInfoParametersBeam {

    private final Property<DefaultLabelProvider.EdgeInfoEnum> infoSideExternal;
    private final Property<DefaultLabelProvider.EdgeInfoEnum> infoMiddleSide1;
    private final Property<DefaultLabelProvider.EdgeInfoEnum> infoMiddleSide2;
    private final Property<DefaultLabelProvider.EdgeInfoEnum> infoSideInternal;

    public EdgeInfoParametersBeam(Property<DefaultLabelProvider.EdgeInfoEnum> infoSideExternal,
                                  Property<DefaultLabelProvider.EdgeInfoEnum> infoMiddleSide1,
                                  Property<DefaultLabelProvider.EdgeInfoEnum> infoMiddleSide2,
                                  Property<DefaultLabelProvider.EdgeInfoEnum> infoSideInternal) {
        this.infoSideExternal = infoSideExternal;
        this.infoMiddleSide1 = infoMiddleSide1;
        this.infoMiddleSide2 = infoMiddleSide2;
        this.infoSideInternal = infoSideInternal;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.infoSideExternal.addListener(changeListener);
        this.infoMiddleSide1.addListener(changeListener);
        this.infoMiddleSide2.addListener(changeListener);
        this.infoSideInternal.addListener(changeListener);
    }

    public DefaultLabelProvider.EdgeInfoParameters getEdgeInfoParameters() {
        return new DefaultLabelProvider.EdgeInfoParameters(
            infoSideExternal.getValue(),
            infoMiddleSide1.getValue(),
            infoMiddleSide2.getValue(),
            infoSideInternal.getValue()
        );
    }
}
