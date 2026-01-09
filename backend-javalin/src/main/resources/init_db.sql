-- 1. Création des tables
CREATE TABLE IF NOT EXISTS categorie (
                                         id INT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL,
    image_path VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS plat (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    categorie_id INT NOT NULL,
                                    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    image_path VARCHAR(255),
    disponible BOOLEAN DEFAULT TRUE,
    tags VARCHAR(255),
    FOREIGN KEY (categorie_id) REFERENCES categorie(id)
    );

CREATE TABLE IF NOT EXISTS commande (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        order_number VARCHAR(50) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    total DECIMAL(10, 2) DEFAULT 0.00,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_id VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS ligne_commande (
                                              id INT AUTO_INCREMENT PRIMARY KEY,
                                              commande_id BIGINT NOT NULL,
                                              plat_id INT NOT NULL,
                                              quantite INT NOT NULL,
                                              options VARCHAR(255),
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (commande_id) REFERENCES commande(id),
    FOREIGN KEY (plat_id) REFERENCES plat(id)
    );

-- 2. Insertion des données (C'est ça qu'il manquait !)
-- On vide d'abord pour éviter les doublons si on relance
DELETE FROM ligne_commande;
DELETE FROM commande;
DELETE FROM plat;
DELETE FROM categorie;

-- Ajout des catégories
INSERT INTO categorie (id, name, image_path) VALUES (1, 'Entrées', 'entree.jpg');
INSERT INTO categorie (id, name, image_path) VALUES (2, 'Plats', 'plat.jpg');
INSERT INTO categorie (id, name, image_path) VALUES (3, 'Desserts', 'dessert.jpg');

-- Ajout des plats
INSERT INTO plat (categorie_id, name, description, price, available) VALUES
                                                                         (1, 'Nems Poulet', '4 pièces croustillantes', 5.50, TRUE),
                                                                         (1, 'Rouleaux de Printemps', '2 pièces fraiches', 4.90, TRUE),
                                                                         (2, 'Poulet Curry', 'Poulet tendre et sauce coco', 12.00, TRUE),
                                                                         (2, 'Boeuf Loc Lac', 'Riz tomaté et boeuf sauté', 13.50, TRUE),
                                                                         (3, 'Perles de Coco', 'Dessert chaud', 4.00, TRUE);