package com.borne_audi.frontend;

import com.borne_audi.frontend.api.ApiClient;
import com.borne_audi.frontend.api.ApiClient.ApiResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartController {

    private static final String API_BASE = "http://localhost:9090";
    private final ApiClient api = new ApiClient(API_BASE);

    @FXML private ListView<String> cartList;
    @FXML private Label totalLabel;
    @FXML private Label statusLabel;
    @FXML private Button validateButton;
    @FXML private Button payButton;

    private String currentOrderId;
    private String currentOrderNumber;

    @FXML
    public void initialize() {
        refreshCart();
        statusLabel.setText("");
        payButton.setDisable(true);
    }

    // ✅ appelé par le bouton "← Modifier"
    @FXML
    public void onBack() {
        FrontendApplication.setRoot("menu-view.fxml");
    }

    // ✅ appelé par "⭐ Enregistrer recommandé"
    @FXML
    public void onSaveRecommended() {
        if (AppState.getCart().isEmpty()) {
            statusLabel.setText("Panier vide : rien à enregistrer.");
            return;
        }
        AppState.saveRecommendedFromCart();
        statusLabel.setText("Commande enregistrée en recommandé ✓");
    }

    // ✅ appelé par "⭐ Charger ma commande habituelle"
    @FXML
    public void onLoadRecommended() {
        if (!AppState.hasRecommended()) {
            statusLabel.setText("Aucune commande recommandée enregistrée.");
            return;
        }
        AppState.loadRecommendedIntoCart();
        refreshCart();
        statusLabel.setText("Commande recommandée chargée ✓");
    }

    // ✅ appelé par "Confirmer"
    @FXML
    public void onValidate() {
        if (AppState.getCart().isEmpty()) {
            statusLabel.setText("Panier vide.");
            return;
        }

        statusLabel.setText("Création de la commande...");
        validateButton.setDisable(true);
        payButton.setDisable(true);

        String payload = buildOrderPayload();

        api.postJson("/orders", payload)
                .thenAccept(res -> Platform.runLater(() -> handleCreateOrder(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur backend (POST /orders)");
                        validateButton.setDisable(false);
                    });
                    return null;
                });
    }

    private void handleCreateOrder(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur création commande (HTTP " + res.statusCode() + ")");
            validateButton.setDisable(false);
            return;
        }

        String body = res.body();
        currentOrderId = jsonString(body, "id");
        currentOrderNumber = jsonString(body, "orderNumber");

        // Sauvegarde pour l’écran confirmation
        AppState.LastOrder order = new AppState.LastOrder();
        order.id = currentOrderId;
        order.orderNumber = currentOrderNumber;
        order.totalCents = AppState.cartTotalCents();
        order.status = "CREATED";

        order.items = new ArrayList<>();
        for (AppState.CartItem it : AppState.getCart().values()) {
            AppState.LastOrder.Item oi = new AppState.LastOrder.Item();
            oi.name = it.name;
            oi.quantity = it.quantity;
            oi.lineTotalCents = it.lineTotal();
            order.items.add(oi);
        }

        AppState.setLastOrder(order);

        statusLabel.setText("Commande créée ✓ " + (currentOrderNumber == null ? "" : currentOrderNumber));
        payButton.setDisable(currentOrderId == null || currentOrderId.isBlank());
    }

    // ✅ appelé par "Payer"
    @FXML
    public void onPay() {
        if (currentOrderId == null || currentOrderId.isBlank()) {
            statusLabel.setText("Aucune commande à payer. Cliquez d’abord sur Confirmer.");
            return;
        }

        statusLabel.setText("Paiement en cours...");
        payButton.setDisable(true);

        api.putJson("/orders/" + currentOrderId + "/status", "{\"status\":\"PAID\"}")
                .thenAccept(res -> Platform.runLater(() -> handlePay(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur backend (PUT paiement)");
                        payButton.setDisable(false);
                    });
                    return null;
                });
    }

    private void handlePay(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur paiement (HTTP " + res.statusCode() + ")");
            payButton.setDisable(false);
            return;
        }

        AppState.LastOrder order = AppState.getLastOrder();
        if (order != null) order.status = "PAID";

        AppState.clearCart();
        refreshCart();

        statusLabel.setText("Payé ✓");
        FrontendApplication.setRoot("confirmation-view.fxml");
    }

    private void refreshCart() {
        cartList.getItems().clear();
        int total = 0;

        for (Map.Entry<Integer, AppState.CartItem> e : AppState.getCart().entrySet()) {
            AppState.CartItem it = e.getValue();
            int line = it.lineTotal();
            total += line;
            cartList.getItems().add(it.name + " x" + it.quantity + " — " + cents(line));
        }

        totalLabel.setText(cents(total));
        validateButton.setDisable(AppState.getCart().isEmpty());
    }

    private String buildOrderPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        boolean first = true;

        for (AppState.CartItem it : AppState.getCart().values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"dishId\":").append(it.dishId)
                    .append(",\"quantity\":").append(it.quantity)
                    .append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String cents(int c) {
        return String.format("%.2f €", c / 100.0);
    }

    private String jsonString(String json, String key) {
        if (json == null) return null;
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}