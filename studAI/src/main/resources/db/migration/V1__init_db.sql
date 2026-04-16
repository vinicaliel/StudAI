CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabela de Usuários e Controle de Plano
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    verification_code VARCHAR(100),
    email_verified BOOLEAN DEFAULT FALSE,
    
    -- Atributos de Controle de Uso e Assinatura
    plan_type VARCHAR(50) DEFAULT 'FREE',
    monthly_usage_count INT DEFAULT 0,
    last_reset_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Materiais/PDFs enviados
CREATE TABLE materials (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(1000) NOT NULL,
    total_pages INT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela dos Resumos Gerados baseada no Formato Obrigatório
CREATE TABLE summaries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    material_id UUID NOT NULL UNIQUE REFERENCES materials(id) ON DELETE CASCADE,
    
    -- Colunas do Formato Obrigatório do Rules.md
    title VARCHAR(500) NOT NULL,
    short_summary TEXT NOT NULL, -- Resumo curto (máx 5 linhas)
    main_topics TEXT NOT NULL,
    important_points TEXT NOT NULL,
    simplified_explanation TEXT NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
