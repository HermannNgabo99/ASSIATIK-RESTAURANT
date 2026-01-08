package com.borne_audi.frontend;

import javafx.fxml.FXML;

public class HomeController {

    @FXML
    public void onStart() {
        System.out.println("✅ onStart() appelé -> ouverture menu-view.fxml");
        FrontendApplication.setRoot("menu-view.fxml");
    }
}