CREATE DATABASE IF NOT EXISTS taller_mecanico;

USE taller_mecanico;

CREATE TABLE IF NOT EXISTS servicios (
    id_servicio INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(150) NOT NULL,
    costo_mano_obra DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_costo_servicio
        CHECK (costo_mano_obra >= 0)
);

INSERT INTO servicios
    (id_servicio, descripcion, costo_mano_obra)
VALUES
    (1, 'Cambio de aceite', 150.00),
    (2, 'Alineacion de direccion', 200.00),
    (3, 'Revision de frenos', 175.00),
    (4, 'Cambio de pastillas de freno', 250.00),
    (5, 'Diagnostico del motor', 300.00),
    (6, 'Lavado de motor', 175.00)
ON DUPLICATE KEY UPDATE
    id_servicio = id_servicio;