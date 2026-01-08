package com.borne_audi.frontend;

import javafx.scene.image.Image;

import java.net.URL;

public class ImageResolver {

    private static final String[] IMG_ROOTS = {
            "/com/borne_audi/frontend/images/",
            "/com.borne_audi.frontend/images/"
    };

    private ImageResolver() {}

    public static URL resolve(String relative) {
        if (relative == null) return null;
        String rel = relative.startsWith("/") ? relative.substring(1) : relative;

        // si on passe déjà un chemin complet contenant /images/
        if (rel.contains("images/")) {
            // essaye direct
            URL direct = FrontendApplication.class.getResource("/" + rel);
            if (direct != null) return direct;
        }

        // sinon on colle aux roots
        for (String root : IMG_ROOTS) {
            URL url = FrontendApplication.class.getResource(root + rel);
            if (url != null) return url;
        }
        return null;
    }

    public static Image loadOrNull(String relative) {
        try {
            URL url = resolve(relative);
            if (url == null) return null;
            return new Image(url.toExternalForm(), true);
        } catch (Exception e) {
            return null;
        }
    }
}
