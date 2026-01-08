package com.borne_audi.frontend;

import javafx.fxml.FXML;

public class AccueilController {



    @FXML
    public void onStartOrder() {
        // Passage vers l'écran menu (catégories)
        FrontendApplication.setRoot("main-view.fxml");
    }
}

