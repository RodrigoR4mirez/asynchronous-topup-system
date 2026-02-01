CREATE TABLE IF NOT EXISTS recharge_requests (
                                                 recharge_id VARCHAR(36) PRIMARY KEY, -- Usaremos UUID para sistemas distribuidos
                                                 phone_number VARCHAR(15) NOT NULL,
                                                 amount DECIMAL(10,2) NOT NULL,
                                                 status ENUM('PENDING', 'PROCESSING', 'SUCCESSFUL', 'FAILED') DEFAULT 'PENDING',
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
