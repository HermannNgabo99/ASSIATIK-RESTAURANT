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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuController {

    private static final String API_BASE = "http://localhost:9090";

    @FXML private ListView<String> categoriesList;
    @FXML private TilePane dishesTile;

    @FXML private Label dishesTitle;
    @FXML private Label statusLabel;
    @FXML private Button cartButton;

    private final ApiClient api = new ApiClient(API_BASE);

    private final List<Category> categories = new ArrayList<>();
    private final List<Dish> dishes = new ArrayList<>();

    @FXML
    public void initialize() {
        updateCartButton();

        categoriesList.getSelectionModel().selectedIndexProperty()
                .addListener((obs, o, n) -> {
                    if (n == null) return;
                    int idx = n.intValue();
                    if (idx >= 0 && idx < categories.size()) {
                        loadDishes(categories.get(idx).id);
                    }
                });

        onLoadCategories();
    }

    @FXML public void onBackHome() { FrontendApplication.setRoot("home-view.fxml"); }
    @FXML public void onOpenCart() { FrontendApplication.setRoot("cart-view.fxml"); }

    @FXML
    public void onRecommended() {
        if (!AppState.hasRecommended()) {
            statusLabel.setText("Aucune commande recommandée enregistrée.");
            return;
        }
        AppState.loadRecommendedIntoCart();
        updateCartButton();
        statusLabel.setText("Commande recommandée chargée ✓");
    }

    @FXML
    public void onLoadCategories() {
        statusLabel.setText("Chargement des catégories...");
        api.get("/categories")
                .thenAccept(res -> Platform.runLater(() -> handleCategories(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Erreur backend (catégories)"));
                    return null;
                });
    }

    private void handleCategories(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur HTTP " + res.statusCode());
            return;
        }

        categories.clear();
        categories.addAll(parseCategories(res.body()));
        categoriesList.getItems().setAll(categories.stream().map(c -> c.name).toList());
        statusLabel.setText("Catégories chargées ✓");

        if (!categories.isEmpty()) categoriesList.getSelectionModel().select(0);
    }

    private void loadDishes(int categoryId) {
        dishesTitle.setText("Plats (catégorie " + categoryId + ")");
        statusLabel.setText("Chargement des plats...");
        api.get("/dishes?categoryId=" + categoryId)
                .thenAccept(res -> Platform.runLater(() -> handleDishes(res)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Erreur backend (plats)"));
                    return null;
                });
    }

    private void handleDishes(ApiResponse res) {
        if (!res.isOk()) {
            statusLabel.setText("Erreur HTTP " + res.statusCode());
            return;
        }

        dishes.clear();
        dishes.addAll(parseDishes(res.body()));

        dishesTile.getChildren().clear();
        for (Dish d : dishes) dishesTile.getChildren().add(buildDishCard(d));

        statusLabel.setText("Plats chargés ✓");
    }

    private VBox buildDishCard(Dish d) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dishCard");
        card.setPadding(new Insets(12));
        card.setPrefWidth(460);

        ImageView img = new ImageView();
        img.setFitWidth(190);
        img.setFitHeight(130);
        img.setPreserveRatio(true);
        img.setSmooth(true);

        String rel = imageRelativeForDish(d.name);
        Image im = ImageResolver.loadOrNull(rel);
        if (im != null) img.setImage(im);

        Label name = new Label(d.name);
        name.getStyleClass().add("dishName");

        Label price = new Label(cents(d.priceCents));
        price.getStyleClass().add("dishPrice");

        Button add = new Button(d.available ? "Ajouter" : "Indisponible");
        add.getStyleClass().add(d.available ? "btnPrimary" : "btnDisabled");
        add.setDisable(!d.available);

        add.setOnAction(e -> {
            AppState.addToCart(d.id, d.name, d.priceCents);
            updateCartButton();
            statusLabel.setText("Ajouté au panier ✓");
        });

        VBox info = new VBox(6, name, price, add);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(14, img, info);
        row.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().add(row);

        card.setOnMouseClicked((MouseEvent e) -> {
            if (!d.available) return;

            AppState.DishSelection sel = new AppState.DishSelection();
            sel.id = d.id;
            sel.name = d.name;
            sel.description = "";
            sel.priceCents = d.priceCents;
            sel.available = d.available;
            sel.imageRel = rel;

            AppState.setSelectedDish(sel);
            FrontendApplication.setRoot("detail-view.fxml");
        });

        return card;
    }

    private void updateCartButton() {
        cartButton.setText("🛒 Panier (" + cents(AppState.cartTotalCents()) + ")");
    }

    private String imageRelativeForDish(String name) {
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

    private String cents(int c) { return String.format("%.2f €", c / 100.0); }

    // ---------- parsing ----------
    private List<Category> parseCategories(String json) {
        List<Category> out = new ArrayList<>();
        for (String obj : splitObjects(json)) out.add(new Category(jsonInt(obj, "id"), jsonString(obj, "name")));
        return out;
    }

    private List<Dish> parseDishes(String json) {
        List<Dish> out = new ArrayList<>();
        for (String obj : splitObjects(json)) {
            out.add(new Dish(
                    jsonInt(obj, "id"),
                    jsonString(obj, "name"),
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
    private record Dish(int id, String name, int priceCents, boolean available) {}
}