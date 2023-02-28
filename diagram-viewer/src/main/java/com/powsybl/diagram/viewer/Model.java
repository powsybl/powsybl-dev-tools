/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer;

import com.powsybl.diagram.viewer.nad.NetworkAreaDiagramModel;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class Model {
    private final BooleanProperty showNames = new SimpleBooleanProperty();
    private final ObjectProperty<Network> network = new SimpleObjectProperty<>();
    private final ObjectProperty<Container<?>> selectedContainer = new SimpleObjectProperty<>();

    private final NetworkAreaDiagramModel nadModel;

    public Model(BooleanProperty showNames, NetworkAreaDiagramModel nadModel) {
        this.showNames.bind(showNames);
        this.nadModel = nadModel;
    }

    public void setNetwork(Network network) {
        this.network.setValue(network);
    }

    public Network getNetwork() {
        return network.getValue();
    }

    public ObjectProperty<Network> getNetworkProperty() {
        return network;
    }

    public Container<?> getSelectedContainer() {
        return selectedContainer.getValue();
    }
    public void setSelectedContainer(Container<?> container) {
        selectedContainer.setValue(container);
    }

    public void clean() {
        setSelectedContainer(null);
        nadModel.setSvgContent("");
    }
}
