CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE IF NOT EXISTS otp_config (
    id INT PRIMARY KEY DEFAULT 1,
    code_length INT NOT NULL DEFAULT 6,
    lifetime_seconds INT NOT NULL DEFAULT 300,
    CHECK (id = 1)
);

-- Вставка конфигурации по умолчанию, если нет
INSERT INTO otp_config (id, code_length, lifetime_seconds)
VALUES (1, 6, 300)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGSERIAL PRIMARY KEY,
    operation_id VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);