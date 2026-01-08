
 CREATE TABLE categorie (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  image_path VARCHAR(255)
); 

 CREATE TABLE plat (
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


CREATE TABLE commande (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_number VARCHAR(20) UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
  total DECIMAL(10, 2) DEFAULT 0.00,
  date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
  client_id VARCHAR(50)
); 

 CREATE TABLE ligne_commande (
  id INT AUTO_INCREMENT PRIMARY KEY,
  commande_id BIGINT NOT NULL,
  plat_id INT NOT NULL,
  quantite INT NOT NULL,
  options TEXT,
  unit_price DECIMAL(10, 2) NOT NULL,
  FOREIGN KEY (commande_id) REFERENCES commande(id),
  FOREIGN KEY (plat_id) REFERENCES plat(id)
); 

