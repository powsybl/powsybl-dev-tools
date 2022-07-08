/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.util;

import java.util.prefs.Preferences;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public final class Util {
    public static final Preferences preferences = Preferences.userNodeForPackage(Util.class);
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private Util() {
    }
}
