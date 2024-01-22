/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.iidm.network.*;
import com.powsybl.sld.cgmes.layout.*;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.layout.positionbyclustering.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class VoltageLevelLayoutFactoryBean {

    public enum VoltageLevelLayoutFactoryType {
        AUTO_EXTENSIONS, AUTO_WITHOUT_EXTENSIONS_CLUSTERING, CGMES, RANDOM, SMART
    }

    private final ObjectProperty<VoltageLevelLayoutFactoryType> factoryType = new SimpleObjectProperty<>();

    // PositionVoltageLevelLayoutFactory
    private final EnumMap<VoltageLevelLayoutFactoryType, PositionVoltageLevelLayoutBean> positionLayoutParametersByType = new EnumMap<>(VoltageLevelLayoutFactoryType.class);

    // RandomVoltageLevelLayoutFactory
    private final ObjectProperty<Double> width = new SimpleObjectProperty<>(500.0);
    private final ObjectProperty<Double> height = new SimpleObjectProperty<>(500.0);

    public VoltageLevelLayoutFactoryBean(Property<VoltageLevelLayoutFactoryType> type,
                                         // PositionVoltageLevelLayoutFactory
                                         BooleanProperty stackFeeders,
                                         BooleanProperty exceptionWhenPatternUnhandled,
                                         BooleanProperty handleShunts,
                                         BooleanProperty removeFictitiousNodes,
                                         BooleanProperty substituteSingularFictitiousNodes,
                                         // RandomVoltageLevelLayoutFactory
                                         Property<Double> width,
                                         Property<Double> height) {
        // Current selection
        this.factoryType.bindBidirectional(type);

        // Manage history for Position VoltageLevel layouts
        this.positionLayoutParametersByType.put(VoltageLevelLayoutFactoryType.AUTO_EXTENSIONS, new PositionVoltageLevelLayoutBean());
        this.positionLayoutParametersByType.put(VoltageLevelLayoutFactoryType.AUTO_WITHOUT_EXTENSIONS_CLUSTERING, new PositionVoltageLevelLayoutBean());
        this.factoryType.addListener((observable, oldValue, newValue) -> {
            PositionVoltageLevelLayoutBean oldParameters = positionLayoutParametersByType.get(oldValue);
            if (oldParameters != null) {
                stackFeeders.unbindBidirectional(oldParameters.stackFeedersProperty());
                exceptionWhenPatternUnhandled.unbindBidirectional(oldParameters.exceptionWhenPatternUnhandledProperty());
                handleShunts.unbindBidirectional(oldParameters.handleShuntsProperty());
                removeFictitiousNodes.unbindBidirectional(oldParameters.removeFictitiousNodesProperty());
                substituteSingularFictitiousNodes.unbindBidirectional(oldParameters.substituteSingularFictitiousNodesProperty());
            }
            PositionVoltageLevelLayoutBean newParameters = positionLayoutParametersByType.get(newValue);
            if (newParameters != null) {
                stackFeeders.bindBidirectional(newParameters.stackFeedersProperty());
                exceptionWhenPatternUnhandled.bindBidirectional(newParameters.exceptionWhenPatternUnhandledProperty());
                handleShunts.bindBidirectional(newParameters.handleShuntsProperty());
                removeFictitiousNodes.bindBidirectional(newParameters.removeFictitiousNodesProperty());
                substituteSingularFictitiousNodes.bindBidirectional(newParameters.substituteSingularFictitiousNodesProperty());
            }
        });

        // RandomVoltageLevelLayoutFactory
        width.bindBidirectional(this.width); // Keep this binding order in order to initialize with Bean value
        height.bindBidirectional(this.height); // Keep this binding order in order to initialize with Bean value
    }

    public void addListener(ChangeListener<Object> changeListener) {
        // PositionVoltageLevelLayoutFactory
        this.factoryType.addListener(changeListener);
        this.positionLayoutParametersByType.forEach((k, v) -> v.addListener(changeListener));
        // RandomVoltageLevelLayoutFactory
        this.width.addListener(changeListener);
        this.height.addListener(changeListener);
    }

    public VoltageLevelLayoutFactoryCreator getVoltageLevelLayoutFactoryCreator(Network network) {
        VoltageLevelLayoutFactoryType type = factoryType.get();
        switch (type) {
            case SMART -> {
                return VoltageLevelLayoutFactoryCreator.newSmartVoltageLevelLayoutFactoryCreator();
            }
            case AUTO_EXTENSIONS -> {
                return VoltageLevelLayoutFactoryCreator.newPositionVoltageLevelLayoutFactoryCreator(new PositionByClustering(), positionLayoutParametersByType.get(type).getParameters());
            }
            case AUTO_WITHOUT_EXTENSIONS_CLUSTERING -> {
                return VoltageLevelLayoutFactoryCreator.newPositionVoltageLevelLayoutFactoryCreator(positionLayoutParametersByType.get(type).getParameters());
            }
            case RANDOM -> {
                return i -> new RandomVoltageLevelLayoutFactory(this.width.get(), this.height.get());
            }
            case CGMES -> {
                return i -> new CgmesVoltageLevelLayoutFactory(network);
            }
        }
        throw new UnsupportedOperationException("This code cannot be reach, missing case '" + type.name() + "'");
    }
}
