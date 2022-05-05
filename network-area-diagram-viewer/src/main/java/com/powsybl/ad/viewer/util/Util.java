/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ad.viewer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public class Util
{
    //Creating the Logger object
    public static final Logger logger = LoggerFactory.getLogger("AreaDiagramViewerLogger");
    public static final Logger loggerControllerImport = LoggerFactory.getLogger("ControllerImportLogger");
    public static final Logger loggerControllerOptions = LoggerFactory.getLogger("ControllerOptionsLogger");
    public static final Logger loggerControllerParameters = LoggerFactory.getLogger("ControllerParametersLogger");
    public static final Logger loggerControllerDiagram = LoggerFactory.getLogger("ControllerDiagramLogger");


    public static final Preferences preferences = Preferences.userNodeForPackage(Util.class);
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    public Util()
    {
    }
}
