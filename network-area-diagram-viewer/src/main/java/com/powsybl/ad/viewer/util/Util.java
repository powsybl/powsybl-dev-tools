package com.powsybl.ad.viewer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

public class Util
{
    //Creating the Logger object
    public static final Logger logger = LoggerFactory.getLogger("AreaDiagramViewerLogger");

    public static final Preferences preferences = Preferences.userNodeForPackage(Util.class);
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";
}
