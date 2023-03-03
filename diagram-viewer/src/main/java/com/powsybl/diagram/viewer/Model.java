/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer;

import com.powsybl.diagram.viewer.nad.NetworkAreaDiagramModel;
import com.powsybl.diagram.viewer.sld.SingleLineDiagramModel;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import javafx.beans.property.*;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class Model {
    private final BooleanProperty showNames = new SimpleBooleanProperty();
    private final ObjectProperty<Network> network = new SimpleObjectProperty<>();
    private Container<?> selectedContainer = null;

    private final NetworkAreaDiagramModel nadModel;

    private final SingleLineDiagramModel sldModel;

    public Model(BooleanProperty showNames, NetworkAreaDiagramModel nadModel, SingleLineDiagramModel sldModel) {
        this.showNames.bind(showNames);
        this.nadModel = nadModel;
        this.sldModel = sldModel;
    }

    public void setNetwork(Network network) {
        this.network.setValue(network);
    }

    public Network getNetwork() {
        return network.getValue();
    }

    public BooleanProperty showNamesProperty() {
        return showNames;
    }

    public ObjectProperty<Network> networkProperty() {
        return network;
    }

    public Container<?> getSelectedContainer() {
        return selectedContainer;
    }

    public void setSelectedContainer(Container<?> container) {
        selectedContainer = container;
    }

    public void clean() {
        setSelectedContainer(null);
        nadModel.clean();
        sldModel.clean();
    }
}
