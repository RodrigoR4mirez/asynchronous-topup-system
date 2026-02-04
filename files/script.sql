select * from balance_wallets;
select * from process_audits;
select * from recharge_requests;

-- Ponemos dinero para los operadores en Perú
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Movistar', 100.00, 'PEN');
INSERT INTO balance_wallets (operator_name, current_balance, currency) VALUES ('Claro', 50.00, 'PEN');

-- Simulamos una recarga de 20 soles
INSERT INTO recharge_requests (recharge_id, phone_number, amount, status)
VALUES ('req-777-abc', '987654321', 20.00, 'PENDING');

-- 1. Descontamos el saldo (El Consumer lo hace)
UPDATE balance_wallets
SET current_balance = current_balance - 20.00
WHERE operator_name = 'Movistar';

-- 2. Marcamos como exitosa la solicitud
UPDATE recharge_requests
SET status = 'SUCCESSFUL'
WHERE recharge_id = 'req-777-abc';

-- 3. Llenamos la auditoría con el detalle del éxito
INSERT INTO process_audits (recharge_id, error_details)
VALUES ('req-777-abc', 'Transaction processed by Kafka Consumer. Balance deducted from Movistar.');


CREATE TABLE IF NOT EXISTS recharge_requests (
                                                 recharge_id VARCHAR(36) PRIMARY KEY,
                                                 phone_number VARCHAR(15) NOT NULL,
                                                 amount DECIMAL(10,2) NOT NULL,
                                                 status VARCHAR(20) DEFAULT 'PENDING',
                                                 created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                                 updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);


CREATE TABLE IF NOT EXISTS balance_wallets (
                                               operator_id INT AUTO_INCREMENT PRIMARY KEY,
                                               operator_name VARCHAR(50) NOT NULL,
                                               current_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                                               currency VARCHAR(3) DEFAULT 'PEN'
);

-- Tabla de auditoría: Ahora el campo FK se llama igual que la PK de la tabla padre
CREATE TABLE IF NOT EXISTS process_audits (
                                              audit_id INT AUTO_INCREMENT PRIMARY KEY,
                                              recharge_id VARCHAR(36), -- Se llama exactamente igual que en recharge_requests
                                              completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              error_details TEXT,
                                              CONSTRAINT fk_recharge FOREIGN KEY (recharge_id) REFERENCES recharge_requests(recharge_id)
);



# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "SELECT * FROM balance_wallets;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "SELECT * FROM process_audits;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "SELECT * FROM recharge_requests;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "drop table balance_wallets;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "drop table process_audits;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "drop table recharge_requests;"


# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "drop table recharge_requests;"
# docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "drop table recharge_requests;"


