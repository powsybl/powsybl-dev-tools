/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.sld.layout.*;
import javafx.beans.property.*;
import javafx.beans.value.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class PositionVoltageLevelLayoutBean {

    // PositionVoltageLevelLayoutFactory
    private final BooleanProperty stackFeeders = new SimpleBooleanProperty();

    private final BooleanProperty exceptionWhenPatternUnhandled = new SimpleBooleanProperty();

    private final BooleanProperty handleShunts = new SimpleBooleanProperty();

    private final BooleanProperty removeFictitiousNodes = new SimpleBooleanProperty();

    private final BooleanProperty substituteSingularFictitiousNodes = new SimpleBooleanProperty();

    public BooleanProperty stackFeedersProperty() {
        return stackFeeders;
    }

    public BooleanProperty exceptionWhenPatternUnhandledProperty() {
        return exceptionWhenPatternUnhandled;
    }

    public BooleanProperty handleShuntsProperty() {
        return handleShunts;
    }

    public BooleanProperty removeFictitiousNodesProperty() {
        return removeFictitiousNodes;
    }

    public BooleanProperty substituteSingularFictitiousNodesProperty() {
        return substituteSingularFictitiousNodes;
    }

    public PositionVoltageLevelLayoutFactoryParameters getParameters() {
        PositionVoltageLevelLayoutFactoryParameters parameters = new PositionVoltageLevelLayoutFactoryParameters();
        parameters.setFeederStacked(stackFeeders.get())
                .setExceptionIfPatternNotHandled(exceptionWhenPatternUnhandled.get())
                .setHandleShunts(handleShunts.get())
                .setRemoveUnnecessaryFictitiousNodes(removeFictitiousNodes.get())
                .setSubstituteSingularFictitiousByFeederNode(substituteSingularFictitiousNodes.get());
        return parameters;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.stackFeeders.addListener(changeListener);
        this.exceptionWhenPatternUnhandled.addListener(changeListener);
        this.handleShunts.addListener(changeListener);
        this.removeFictitiousNodes.addListener(changeListener);
        this.substituteSingularFictitiousNodes.addListener(changeListener);
    }
}
