/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import com.powsybl.iidm.network.Container;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class DiagramModel {
    private final ContainerResult selectedContainerResult = new ContainerResult();

    private final Map<Container<?>, ContainerResult> containerToResultMap = new HashMap<>();

    public DiagramModel() {
        // Nothing to do
    }

    public ContainerResult getSelectedContainerResult() {
        return selectedContainerResult;
    }

    public Stream<Container<?>> getCheckedContainerStream() {
        return containerToResultMap.keySet().stream();
    }

    public ContainerResult getCheckedContainerResult(Container<?> container) {
        return containerToResultMap.computeIfAbsent(container, c -> new ContainerResult());
    }

    public void removeCheckedContainerResult(Container<?> container) {
        containerToResultMap.remove(container);
    }

    public void clean() {
        selectedContainerResult.clean();
        containerToResultMap.clear();
    }
}
