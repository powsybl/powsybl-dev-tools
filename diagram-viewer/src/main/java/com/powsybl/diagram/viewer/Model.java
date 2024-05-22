/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer;

import com.powsybl.diagram.viewer.nad.NetworkAreaDiagramModel;
import com.powsybl.diagram.viewer.sld.SingleLineDiagramModel;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class Model {
    private final ObjectProperty<Network> network = new SimpleObjectProperty<>();
    private final ObservableList<String> countriesNames = FXCollections.observableArrayList();

    private Container<?> selectedContainer = null;

    private final NetworkAreaDiagramModel nadModel;

    private final SingleLineDiagramModel sldModel;

    public Model(BooleanProperty showNames, NetworkAreaDiagramModel nadModel, SingleLineDiagramModel sldModel) {
        this.nadModel = nadModel;
        this.sldModel = sldModel;
        this.nadModel.getSvgParametersBean().bind(showNames);
        this.sldModel.getSvgParametersBean().bind(showNames);
    }

    public void setNetwork(Network network) {
        this.countriesNames.setAll(network.getCountries().stream().map(Country::toString).toList());
        this.network.setValue(network);
    }

    public Network getNetwork() {
        return network.getValue();
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

    public ObservableList<String> getCountriesNames() {
        return countriesNames;
    }
}
