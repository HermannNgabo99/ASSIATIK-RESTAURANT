package com.borne_audi.frontend;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;

public class ConfirmationController {

    @FXML private Label orderNumberLabel;
    @FXML private Label orderIdLabel;

    // ⚠️ Si ton confirmation-view.fxml n’a PAS de fx:id="paidLabel", mets ce champ en commentaire
    @FXML private Label paidLabel;

    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;

    private final NumberFormat eur = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    @FXML
    public void initialize() {
        AppState.LastOrder order = AppState.getLastOrder();

        if (order == null) {
            safeSet(orderNumberLabel, "Commande N° -");
            safeSet(orderIdLabel, "ID: -");
            if (paidLabel != null) safeSet(paidLabel, "");
            safeSet(totalLabel, eur.format(0));
            if (itemsBox != null) {
                itemsBox.getChildren().clear();
                itemsBox.getChildren().add(row("Aucun article", "-"));
            }
            return;
        }

        safeSet(orderNumberLabel, "Commande N° " + nz(order.orderNumber, "-"));
        safeSet(orderIdLabel, "ID: " + nz(order.id, "-"));

        String st = nz(order.status, "");
        if (paidLabel != null) {
            paidLabel.setText(st.equalsIgnoreCase("PAID") ? "✅ Payée" : "⏳ En attente de paiement");
        }

        safeSet(totalLabel, eur.format(order.totalCents / 100.0));

        if (itemsBox != null) {
            itemsBox.getChildren().clear();

            if (order.items != null && !order.items.isEmpty()) {
                for (AppState.LastOrder.Item it : order.items) {
                    String left = nz(it.name, "Article") + "  x" + it.quantity;
                    String right = eur.format(it.lineTotalCents / 100.0);
                    itemsBox.getChildren().add(row(left, right));
                }
            } else {
                itemsBox.getChildren().add(row("Aucun article", "-"));
            }
        }
    }

    private HBox row(String left, String right) {
        Label l = new Label(left);
        l.setStyle("-fx-font-size: 16px; -fx-text-fill: #222;");
        Label r = new Label(right);
        r.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox line = new HBox(10, l, spacer, r);
        line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return line;
    }

    private static String nz(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static void safeSet(Label label, String txt) {
        if (label != null) label.setText(txt);
    }

    @FXML
    public void onNewOrder() {
        AppState.clearLastOrder();
        AppState.clearCart();
        FrontendApplication.setRoot("home-view.fxml");
    }
}