package com.powsybl.ad.viewer.model;

import com.google.common.io.ByteStreams;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.Objects;

public class DisplaySVG {

    private static String contentSVG;

    public static void loadContent(String svg) throws IOException {
        String html = new String(
                ByteStreams.toByteArray(Objects.requireNonNull(DisplaySVG.class.getResourceAsStream("/svg.html")))
        );
        String js = new String(
                ByteStreams.toByteArray(Objects.requireNonNull(DisplaySVG.class.getResourceAsStream("/svg.js")))
        );
        contentSVG = html.replace("%__JS__%", js).replace("%__SVG__%", svg);
    }

    public static String getContentSVG() {
        return contentSVG;
    }
}
