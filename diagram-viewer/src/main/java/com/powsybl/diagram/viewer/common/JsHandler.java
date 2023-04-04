/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JsHandler.class);

    // To be able to access the Java Logger from JavaScript
    public void log(String message) {
        LOG.info(message);
    }
}
