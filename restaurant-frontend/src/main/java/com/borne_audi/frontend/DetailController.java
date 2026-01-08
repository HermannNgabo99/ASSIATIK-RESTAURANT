package com.borne_audi.frontend;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.text.NumberFormat;
import java.util.Locale;

public class DetailController {

    @FXML private ImageView dishImage;

    @FXML private Label nameLabel;
    @FXML private Label descLabel;
    @FXML private Label priceLabel;
    @FXML private Label qtyLabel;
    @FXML private Label infoLabel;
    @FXML private Button addButton;

    private final NumberFormat eur = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private AppState.DishSelection dish;

    private int qty = 1;

    @FXML
    public void initialize() {
        dish = AppState.getSelectedDish();

        if (dish == null) {
            nameLabel.setText("Aucun plat sélectionné");
            descLabel.setText("");
            priceLabel.setText("");
            qtyLabel.setText("1");
            infoLabel.setText("");
            addButton.setDisable(true);
            dishImage.setImage(null);
            return;
        }

        nameLabel.setText(dish.name);

        descLabel.setText(
                (dish.description == null || dish.description.isBlank())
                        ? "Aucune description."
                        : dish.description
        );

        priceLabel.setText(eur.format(dish.priceCents / 100.0));

        qty = 1;
        qtyLabel.setText(String.valueOf(qty));

        // ✅ Image (corrigé) : on utilise ImageResolver + dish.imageRel
        dishImage.setImage(null);
        if (dish.imageRel != null && !dish.imageRel.isBlank()) {
            Image img = ImageResolver.loadOrNull(dish.imageRel);
            if (img != null) dishImage.setImage(img);
        }

        if (!dish.available) {
            infoLabel.setText("Plat indisponible.");
            addButton.setDisable(true);
        } else {
            infoLabel.setText("");
            addButton.setDisable(false);
        }
    }

    @FXML
    public void onMinus() {
        if (qty > 1) qty--;
        qtyLabel.setText(String.valueOf(qty));
    }

    @FXML
    public void onPlus() {
        qty++;
        qtyLabel.setText(String.valueOf(qty));
    }

    @FXML
    public void onAddToCart() {
        if (dish == null || !dish.available) return;

        for (int i = 0; i < qty; i++) {
            AppState.addToCart(dish.id, dish.name, dish.priceCents);
        }

        infoLabel.setText("Ajouté au panier ✓");
        FrontendApplication.setRoot("menu-view.fxml");
    }

    @FXML
    public void onBack() {
        FrontendApplication.setRoot("menu-view.fxml");
    }
}