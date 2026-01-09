package com.borne_audi;

import org.jdbi.v3.core.Jdbi;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Database {

    // Ceci est l'objet qui permet de parler à la DB de partout
    public static Jdbi jdbi;

    public static void connect() {
        // Connexion à H2 en mode mémoire (compatible MySQL)
        jdbi = Jdbi.create("jdbc:h2:mem:restaurant_db;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");

        System.out.println("✅ Base de données connectée !");

        // On initialise les tables tout de suite
        initTables();
    }

    private static void initTables() {
        try (InputStream is = Database.class.getResourceAsStream("/init_db.sql")) {
            if (is != null) {
                String sql = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();

                jdbi.useHandle(handle -> {
                    handle.createScript(sql).execute();
                });
                System.out.println("✅ Tables créées et données insérées !");
            } else {
                System.err.println("❌ Impossible de trouver init_db.sql ! Vérifie qu'il est dans resources.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}