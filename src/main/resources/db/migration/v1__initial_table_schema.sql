-- user_profile Table
CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    supabase_id UUID NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    nick_name VARCHAR(50),
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    date_of_birth DATE NOT NULL DEFAULT '2000-01-01',
    status VARCHAR(30) NOT NULL,-- 'ACTIVE', 'BANNED'
    role VARCHAR(30) NOT NULL;-- 'PLAYER', 'MODERATOR', 'ADMIN'
    daily_free_play_count INT DEFAULT 3,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_profile_supabase_id UNIQUE (supabase_id)
);

-- Indexes for user_profile
CREATE INDEX idx_user_profile_status ON user_profile(status);
CREATE INDEX idx_user_profile_role ON user_profile(role);


-- room Table
CREATE TABLE room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    min_players INT NOT NULL,
    entry_fee NUMERIC(12, 2) DEFAULT 0,
    pattern VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_created_by FOREIGN KEY (created_by) REFERENCES user_profile(id) ON DELETE CASCADE
);

-- Indexes for room
CREATE INDEX idx_room_status ON room(status);
CREATE INDEX idx_room_pattern ON room(pattern);
CREATE INDEX idx_room_created_by ON room(created_by);


CREATE TABLE game (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    joined_players_ids TEXT,
    drawn_numbers TEXT,
    all_card_ids TEXT,
    players_count INT NOT NULL DEFAULT 0,
    entries_count INT NOT NULL DEFAULT 0,
    prize_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    capacity INT NOT NULL,
    entry_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.0,

    started BOOLEAN NOT NULL DEFAULT FALSE,
    ended BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'READY', -- 'READY', 'PLAYING', 'COMPLETED', 'CANCELLED'
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT game_state_room FOREIGN KEY (room_id) REFERENCES room(id)
);

-- Indexes for game
CREATE INDEX idx_game_room_id ON game(room_id);
CREATE INDEX idx_game_status ON game(status);


CREATE TABLE game_transaction (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT,
    player_id BIGINT,
    txn_amount NUMERIC(19, 2) NOT NULL,
    txn_type VARCHAR(50) NOT NULL,  -- e.g., GAME_FEE, PRIZE_PAYOUT, REFUND, DISPUTE
    txn_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- e.g., SUCCESS, FAIL, AWAITING_APPROVAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_transaction_game FOREIGN KEY (game_id)
        REFERENCES game(id) ON DELETE SET NULL,
    CONSTRAINT fk_game_transaction_user FOREIGN KEY (player_id)
        REFERENCES user_profile(id) ON DELETE SET NULL
);

-- Useful indexes
CREATE INDEX idx_game_transaction_game_id ON game_transaction(game_id);
CREATE INDEX idx_game_transaction_player ON game_transaction(player_id);
CREATE INDEX idx_game_transaction_status ON game_transaction(txn_status);
CREATE INDEX idx_game_transaction_type ON game_transaction(txn_type);


-- bingo_claims Table
CREATE TABLE bingo_claims (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    card TEXT,
    marked_numbers TEXT,
    pattern VARCHAR(50),
    is_winner BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bingo_claims_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
    CONSTRAINT fk_bingo_claims_user_profile FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE CASCADE
);

-- Indexes for bingo_claims
CREATE INDEX idx_bingo_claims_game_id ON bingo_claims(game_id);
CREATE INDEX idx_bingo_claims_player_id ON bingo_claims(player_id);

-- Enum types for transactions
--CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
--CREATE TYPE transaction_status AS ENUM ('PENDING', 'AWAITING_APPROVAL', 'COMPLETED', 'FAILED');

-- payment_method Table
CREATE TABLE payment_method (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default boolean,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_default_payment_method
ON payment_method (is_default)
WHERE is_default = true;


-- transaction Table
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    txn_ref VARCHAR(100) NOT NULL,
    payment_method_id BIGINT NOT NULL,
    txn_type VARCHAR(50) NOT NULL, -- 'DEPOSIT', 'WITHDRAWAL', DISPUTE
    txn_amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', --'PENDING', 'AWAITING_APPROVAL', 'COMPLETED', 'FAILED'
    description TEXT,
    meta_data TEXT, -- JSON (bank details, etc)
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_player_id FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_payment_method_id FOREIGN KEY (payment_method_id) REFERENCES payment_method(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_approved_by FOREIGN KEY (approved_by) REFERENCES user_profile(id) ON DELETE SET NULL
);

-- Indexes for transaction
CREATE INDEX idx_transaction_player_id ON transaction(player_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_type ON transaction(txn_type);


CREATE TABLE deposit_transfer (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- SUCCESS, FAIL, AWAITING_APPROVAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deposit_transfer_sender FOREIGN KEY (sender_id)
        REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_transfer_receiver FOREIGN KEY (receiver_id)
        REFERENCES user_profile(id) ON DELETE RESTRICT
);

CREATE INDEX idx_sender_id ON deposit_transfer(sender_id);
CREATE INDEX idx_receiver_id ON deposit_transfer(receiver_id);


CREATE TABLE wallet (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    total_deposit NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    welcome_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_welcome_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    referral_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_referral_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_prize_amount NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    pending_withdrawal NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_withdrawal NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_available_balance NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_to_withdraw NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);
CREATE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);

-- system_config Table
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_system_config_name UNIQUE (name)
); --eg. daily_deposit_limit, daily_free_play

-- Indexes for system_config
CREATE INDEX idx_system_config_name ON system_config(name);

-- error_report Table
CREATE TABLE error_report (
    id BIGSERIAL PRIMARY KEY,
    reported_by BIGINT NOT NULL,
    error_about VARCHAR(255),
    error_text TEXT NOT NULL,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_error_report_reported_by FOREIGN KEY (reported_by) REFERENCES user_profile(id) ON DELETE CASCADE
);
-- Indexes for error_report
CREATE INDEX idx_error_report_reported_by ON error_report(reported_by);


--
--
---- support_request Table
--CREATE TABLE support_request (
--    id BIGSERIAL PRIMARY KEY,
--    request_reference TEXT NOT NULL UNIQUE,
--    request_title VARCHAR(255) NOT NULL,
--    request_text TEXT NOT NULL,
--    image_urls JSONB DEFAULT '[]'::jsonb,
--    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT uq_support_request_reference UNIQUE (request_reference)
--);

-- char_room Table
--CREATE TABLE chat_room (
--    id BIGSERIAL PRIMARY KEY,
--    room_name VARCHAR(100) NOT NULL UNIQUE,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT uq_char_room_name UNIQUE (room_name)
--);
--
---- chat_message Table
--CREATE TABLE chat_message (
--    id BIGSERIAL PRIMARY KEY,
--    room_id BIGINT NOT NULL,
--    sender_id BIGINT NOT NULL,
--    message TEXT NOT NULL,
--    is_deleted BOOLEAN DEFAULT FALSE,
--    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES char_room(id) ON DELETE CASCADE,
--    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user_profile(id) ON DELETE CASCADE
--);
--
---- Indexes for chat_message
--CREATE INDEX idx_chat_message_room_id ON chat_message(room_id);
--CREATE INDEX idx_chat_message_sender_id ON chat_message(sender_id);
--CREATE INDEX idx_chat_message_sent_at ON chat_message(sent_at);


