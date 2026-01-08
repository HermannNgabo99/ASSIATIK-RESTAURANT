package com.borne_audi.frontend;

import com.borne_audi.frontend.api.ApiClient;
import com.borne_audi.frontend.api.ApiClient.ApiResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {

    private static final String API_BASE = "http://localhost:9090";

    // ✅ IMPORTANT : base exacte de tes resources (d’après ta capture)
    private static final String RES_ROOT = "/com/borne_audi/frontend/";
    private static final String IMG_ROOT = RES_ROOT + "images/";

    @FXML private ListView<String> categoriesList;
    @FXML private ListView<String> cartList;

    @FXML private Label dishesTitle;
    @FXML private Label totalLabel;
    @FXML private Label statusLabel;

    @FXML private Button validateButton;
    @FXML private Button payButton;

    @FXML private TilePane dishesTile;

    private final ApiClient api = new ApiClient(API_BASE);

    private final List<Category> categories = new ArrayList<>();
    private final List<Dish> dishes = new ArrayList<>();
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();

    private String lastOrderId;

    @FXML
    public void initialize() {

        categoriesList.getSelectionModel().selectedIndexProperty()
                .addListener((obs, o, n) -> {
                    if (n == null) return;
                    int idx = n.intValue();
                    if (idx >= 0 && idx < categories.size()) {
                        loadDishes(categories.get(idx).id);
                    }
                });

        refreshCart();
        payButton.setDisable(true);
        statusLabel.setText("");
        dishesTitle.setText("Plats");
    }

    // ================= ACTIONS UI =================

    @FXML
    public void onLoadCategories() {
        statusLabel.setText("Chargement des catégories...");

        api.get("/categories")
                .thenAccept(res -> Platform.runLater(() -> handleCategoriesResponse(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Erreur backend (catégories)"));
                    return null;
                });
    }

    private void loadDishes(int categoryId) {
        dishesTitle.setText("Plats (catégorie " + categoryId + ")");
        statusLabel.setText("Chargement des plats...");

        api.get("/dishes?categoryId=" + categoryId)
                .thenAccept(res -> Platform.runLater(() -> handleDishesResponse(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Erreur backend (plats)"));
                    return null;
                });
    }

    // ================= RESPONSES =================

    private void handleCategoriesResponse(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur HTTP " + res.statusCode());
            return;
        }

        categories.clear();
        categories.addAll(parseCategories(res.body()));
        categoriesList.getItems().setAll(categories.stream().map(c -> c.name).toList());

        statusLabel.setText("Catégories chargées ✓");

        if (!categories.isEmpty()) {
            categoriesList.getSelectionModel().select(0);
        }
    }

    private void handleDishesResponse(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur HTTP " + res.statusCode());
            return;
        }

        dishes.clear();
        dishes.addAll(parseDishes(res.body()));

        dishesTile.getChildren().clear();
        for (Dish d : dishes) {
            dishesTile.getChildren().add(buildDishCard(d));
        }

        statusLabel.setText("Plats chargés ✓");
    }

    // ================= UI : CARD =================

    private VBox buildDishCard(Dish d) {

        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setPrefWidth(420);
        card.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                """);

        // image
        ImageView img = new ImageView();
        img.setFitWidth(140);
        img.setFitHeight(110);
        img.setPreserveRatio(true);
        img.setSmooth(true);

        String imgRelative = imagePathForDish(d.name); // ex: "plats/pad_thai.png"
        Image image = loadImageOrNull(imgRelative);
        if (image != null) img.setImage(image);

        VBox info = new VBox(6);

        Label name = new Label(d.name);
        name.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#111827;");

        Label price = new Label(cents(d.price));
        price.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:#111827;");

        Button add = new Button(d.available ? "Ajouter" : "Indisponible");
        add.setDisable(!d.available);
        add.setStyle("""
                -fx-font-size:14px;
                -fx-font-weight:800;
                -fx-background-radius:10;
                -fx-padding:10 14;
                """);

        add.setOnAction(e -> {
            cart.compute(d.id, (k, v) -> v == null ? new CartItem(d) : v.increment());
            refreshCart();
            statusLabel.setText("Ajouté au panier ✓");
        });

        info.getChildren().addAll(name, price, add);

        HBox row = new HBox(12, img, info);
        row.setAlignment(Pos.CENTER_LEFT);

        if (!d.available) {
            Label badge = new Label("Indisponible");
            badge.setStyle("""
                    -fx-background-color:#fee2e2;
                    -fx-text-fill:#991b1b;
                    -fx-font-weight:800;
                    -fx-padding:6 10;
                    -fx-background-radius:999;
                    """);
            badge.setTextAlignment(TextAlignment.CENTER);
            card.getChildren().addAll(row, badge);
        } else {
            card.getChildren().add(row);
        }

        // clic sur la carte => ouvrir détail
        card.setOnMouseClicked(e -> {
            if (!d.available) return;

            AppState.DishSelection sel = new AppState.DishSelection();
            sel.id = d.id;
            sel.name = d.name;
            sel.description = d.description == null ? "" : d.description;
            sel.priceCents = d.price;
            sel.available = d.available;

            // ✅ IMPORTANT : on stocke le chemin complet resource pour la vue détail
            sel.imageRel = IMG_ROOT + imgRelative; // ex: "/com/borne_audi/frontend/images/plats/pad_thai.png"

            AppState.setSelectedDish(sel);
            FrontendApplication.setRoot("detail-view.fxml");
        });

        return card;
    }

    // ✅ charge depuis /com/borne_audi/frontend/images/...
    private Image loadImageOrNull(String relativeUnderImages) {
        try {
            var url = FrontendApplication.class.getResource(IMG_ROOT + relativeUnderImages);
            if (url == null) return null;
            return new Image(url.toExternalForm(), true);
        } catch (Exception e) {
            return null;
        }
    }

    // retourne un chemin RELATIF sous images/
    private String imagePathForDish(String name) {
        String n = normalize(name);
        return switch (n) {
            case "gyozas" -> "entrees/gyozas.png";
            case "nems" -> "entrees/nems.jpg";
            case "pad thai" -> "plats/pad_thai.png";
            case "ramen" -> "plats/ramen.png";
            case "curry rouge" -> "plats/curry_rouge.jpg";
            case "bubble tea" -> "boissons/bubble_tea.png";
            case "smoothie mangue" -> "boissons/smoothie_mango.png";
            case "mochi" -> "desserts/mochi.png";
            case "mango sticky rice", "riz mangue", "mango rice" -> "desserts/mango_sticky_rice.png";
            default -> "plats/pad_thai.png";
        };
    }

    private String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = t.replace("_", " ");
        t = t.replaceAll("\\s+", " ");
        t = t.replace("é", "e").replace("è", "e").replace("ê", "e");
        t = t.replace("à", "a").replace("â", "a");
        t = t.replace("ô", "o");
        return t;
    }

    // ================= CART =================

    @FXML
    public void onValidateOrder() {
        if (cart.isEmpty()) {
            statusLabel.setText("Panier vide.");
            return;
        }

        statusLabel.setText("Création de la commande...");
        validateButton.setDisable(true);
        payButton.setDisable(true);

        api.postJson("/orders", buildOrderPayload())
                .thenAccept(res -> Platform.runLater(() -> handleCreateOrderResponse(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur backend");
                        validateButton.setDisable(false);
                    });
                    return null;
                });
    }

    private void handleCreateOrderResponse(ApiResponse res) {
        if (res.isOk()) {
            String body = res.body();
            lastOrderId = jsonString(body, "id");

            cart.clear();
            refreshCart();

            payButton.setDisable(lastOrderId == null || lastOrderId.isBlank());

            FrontendApplication.setRoot("confirmation-view.fxml");
        } else {
            statusLabel.setText("Erreur création commande");
            validateButton.setDisable(false);
        }
    }

    @FXML
    public void onPayOrder() {
        if (lastOrderId == null || lastOrderId.isBlank()) return;

        statusLabel.setText("Paiement en cours...");
        payButton.setDisable(true);

        api.putJson("/orders/" + lastOrderId + "/status", "{\"status\":\"PAID\"}")
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isOk()) statusLabel.setText("Commande payée ✓");
                    else {
                        statusLabel.setText("Erreur paiement");
                        payButton.setDisable(false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erreur paiement");
                        payButton.setDisable(false);
                    });
                    return null;
                });
    }

    private void refreshCart() {
        cartList.getItems().clear();
        int total = 0;

        for (CartItem item : cart.values()) {
            int line = item.quantity * item.unitPrice;
            total += line;
            cartList.getItems().add(item.name + " x" + item.quantity + " — " + cents(line));
        }

        totalLabel.setText("Total : " + cents(total));
        validateButton.setDisable(cart.isEmpty());
    }

    private String buildOrderPayload() {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        boolean first = true;

        for (CartItem item : cart.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"dishId\":").append(item.id)
                    .append(",\"quantity\":").append(item.quantity).append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String cents(int c) {
        return String.format("%.2f €", c / 100.0);
    }

    // ================= JSON UTILS =================

    private List<Category> parseCategories(String json) {
        List<Category> out = new ArrayList<>();
        for (String obj : splitObjects(json)) {
            out.add(new Category(jsonInt(obj, "id"), jsonString(obj, "name")));
        }
        return out;
    }

    private List<Dish> parseDishes(String json) {
        List<Dish> out = new ArrayList<>();
        for (String obj : splitObjects(json)) {
            out.add(new Dish(
                    jsonInt(obj, "id"),
                    jsonString(obj, "name"),
                    jsonString(obj, "description"),
                    jsonInt(obj, "priceCents"),
                    jsonBool(obj, "available")
            ));
        }
        return out;
    }

    private String[] splitObjects(String json) {
        if (json == null) return new String[0];
        Matcher m = Pattern.compile("\\{[^\\{\\}]*\\}").matcher(json);
        List<String> list = new ArrayList<>();
        while (m.find()) list.add(m.group());
        return list.toArray(new String[0]);
    }

    private int jsonInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private boolean jsonBool(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() && Boolean.parseBoolean(m.group(1));
    }

    private String jsonString(String json, String key) {
        if (json == null) return null;
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private record Category(int id, String name) {}
    private record Dish(int id, String name, String description, int price, boolean available) {}

    private static class CartItem {
        final int id;
        final String name;
        final int unitPrice;
        int quantity = 1;

        CartItem(Dish d) {
            id = d.id;
            name = d.name;
            unitPrice = d.price;
        }

        CartItem increment() {
            quantity++;
            return this;
        }
    }
}
