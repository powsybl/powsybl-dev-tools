/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.sld.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class VoltageLevelLayoutFactoryBean {

    private final ObjectProperty<VoltageLevelLayoutFactory> currentVoltageLevelLayoutFactory = new SimpleObjectProperty<>();

    // PositionVoltageLevelLayoutFactory
    private final BooleanProperty stackFeeders = new SimpleBooleanProperty();

    private final BooleanProperty exceptionWhenPatternUnhandled = new SimpleBooleanProperty();

    private final BooleanProperty handleShunts = new SimpleBooleanProperty();

    private final BooleanProperty removeFictitiousNodes = new SimpleBooleanProperty();

    private final BooleanProperty substituteSingularFictitiousNodes = new SimpleBooleanProperty();

    public VoltageLevelLayoutFactoryBean(ReadOnlyObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory,
                                         // PositionVoltageLevelLayoutFactory
                                         BooleanProperty stackFeeders,
                                         BooleanProperty exceptionWhenPatternUnhandled,
                                         BooleanProperty handleShunts,
                                         BooleanProperty removeFictitiousNodes,
                                         BooleanProperty substituteSingularFictitiousNodes) {
        // Current selection
        this.currentVoltageLevelLayoutFactory.bind(voltageLevelLayoutFactory);
        this.currentVoltageLevelLayoutFactory.addListener((observable, oldValue, newValue) -> {
            boolean selected = newValue instanceof PositionVoltageLevelLayoutFactory;
            if (selected) {
                this.stackFeeders.setValue(((PositionVoltageLevelLayoutFactory) newValue).isFeederStacked());
                this.exceptionWhenPatternUnhandled.setValue(((PositionVoltageLevelLayoutFactory) newValue).isExceptionIfPatternNotHandled());
                this.handleShunts.setValue(((PositionVoltageLevelLayoutFactory) newValue).isHandleShunts());
                this.removeFictitiousNodes.setValue(((PositionVoltageLevelLayoutFactory) newValue).isRemoveUnnecessaryFictitiousNodes());
                this.substituteSingularFictitiousNodes.setValue(((PositionVoltageLevelLayoutFactory) newValue).isSubstituteSingularFictitiousByFeederNode());
            }
        });

        // PositionVoltageLevelLayoutFactory
        this.stackFeeders.bindBidirectional(stackFeeders);
        this.exceptionWhenPatternUnhandled.bindBidirectional(exceptionWhenPatternUnhandled);
        this.handleShunts.bindBidirectional(handleShunts);
        this.removeFictitiousNodes.bindBidirectional(removeFictitiousNodes);
        this.substituteSingularFictitiousNodes.bindBidirectional(substituteSingularFictitiousNodes);
    }

    public void addListener(ChangeListener<Object> changeListener) {
        this.stackFeeders.addListener(changeListener);
        this.exceptionWhenPatternUnhandled.addListener(changeListener);
        this.handleShunts.addListener(changeListener);
        this.removeFictitiousNodes.addListener(changeListener);
        this.substituteSingularFictitiousNodes.addListener(changeListener);
    }

    public VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        VoltageLevelLayoutFactory layoutFactory = currentVoltageLevelLayoutFactory.get();
        if (layoutFactory instanceof PositionVoltageLevelLayoutFactory) {
            // PositionVoltageLevelLayoutFactory
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setFeederStacked(stackFeeders.get());
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setExceptionIfPatternNotHandled(exceptionWhenPatternUnhandled.get());
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setHandleShunts(handleShunts.get());
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setRemoveUnnecessaryFictitiousNodes(removeFictitiousNodes.get());
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setSubstituteSingularFictitiousByFeederNode(substituteSingularFictitiousNodes.get());
        }
        return layoutFactory;
    }
}
