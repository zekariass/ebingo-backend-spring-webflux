-- Enum types (values in uppercase)
CREATE TYPE user_role AS ENUM ('PLAYER', 'MODERATOR', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'BANNED');
-- OPEN: Room is open for players to join.
-- GAME_READY: Waiting for the game to start.
-- GAME_STARTED: Room is in progress and the game has started.
-- CLOSED: Room is closed and no longer available for players to join.

-- user_profile Table
CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    supabase_id UUID NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    nick_name VARCHAR(50),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE NOT NULL DEFAULT '2000-01-01',
    status VARCHAR(30) NOT NULL,-- DEFAULT 'ACTIVE',
    role VARCHAR(30) NOT NULL;-- DEFAULT 'PLAYER',
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

-- room_setting Table
CREATE TABLE room_setting (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    is_demo BOOLEAN DEFAULT FALSE,
    free_play_limit INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_setting_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
);

-- Indexes for room_setting
CREATE INDEX idx_room_setting_room_id ON room_setting(room_id);

--
---- card_pool Table
--CREATE TABLE card_pool (
--    id BIGSERIAL PRIMARY KEY,
--    room_id BIGINT NOT NULL,
--    numbers JSONB NOT NULL,
--    numbers_hash TEXT NOT NULL,
--    is_active BOOLEAN DEFAULT TRUE,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT uq_card_pool_numbers_hash UNIQUE (numbers_hash)
--);
--
---- Indexes for card_pool
--CREATE INDEX idx_card_pool_is_active ON card_pool(is_active);
--CREATE INDEX idx_card_pool_numbers_gin ON card_pool USING GIN(numbers);


--
---- room_entries Table
--CREATE TABLE room_entries (
--    id BIGSERIAL PRIMARY KEY,
--    user_profile_id BIGINT NOT NULL,
--    room_id BIGINT NOT NULL,
--    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT fk_room_entries_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
--    CONSTRAINT fk_room_entries_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
--);
--
---- Indexes for room_entries
--CREATE INDEX idx_room_entries_user_profile_id ON room_entries(user_profile_id);
--CREATE INDEX idx_room_entries_room_id ON room_entries(room_id);


-- TRACK player entries in memory and only persist the results to the database after the game ends.

-- game Table
CREATE TYPE game_status AS ENUM ('READY', 'PLAYING', 'COMPLETED', 'CANCELLED', 'CANCELLED_NO_MIN_PLAYERS');
--Game is started when at least min_players have started. Initial status is READY. When game starts status changes to PLAYING. When a player wins and claims bingo, status changes to COMPLETED.

CREATE TABLE game (
    id BIGSERIAL PRIMARY KEY,
    game_reference VARCHAR(30) NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    status game_status NOT NULL DEFAULT 'READY',
    called_numbers JSONB DEFAULT '[]'::jsonb,
    prize_amount NUMERIC(12,2) DEFAULT 0,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    CONSTRAINT uq_game_reference UNIQUE (game_reference)
);

-- Indexes for game
CREATE INDEX idx_game_room_id ON game(room_id);
CREATE INDEX idx_game_status ON game(status);


-- game_entries Table
CREATE TABLE game_entries (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    card JSONB NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_entries_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_entries_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE
);

-- Indexes for game_entries
CREATE INDEX idx_game_entries_game_id ON game_entries(game_id);
CREATE INDEX idx_game_entries_user_profile_id ON game_entries(user_profile_id);


---- player_cards Table
--CREATE TABLE player_cards (
--    id BIGSERIAL PRIMARY KEY,
--    user_profile_id BIGINT NOT NULL,
--    card_id BIGINT NOT NULL,
--    game_id BIGINT NOT NULL,
--    marked_numbers JSONB DEFAULT '[]'::jsonb,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT fk_player_cards_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
--    CONSTRAINT fk_player_cards_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
--    CONSTRAINT fk_player_cards_card FOREIGN KEY (card_id) REFERENCES card_pool(id) ON DELETE CASCADE
--);
--
---- Indexes for player_cards
--CREATE INDEX idx_player_cards_user_profile_id ON player_cards(user_profile_id);
--CREATE INDEX idx_player_cards_card_id ON player_cards(card_id);


-- HANDLE PLAYER CARDS IN MEMORY AND ONLY PERSIST THE RESULTS TO THE DATABASE AFTER THE GAME ENDS.

-- bingo_claims Table
CREATE TABLE bingo_claims (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    user_profile_id BIGINT NOT NULL,
    game_entry_id BIGINT NOT NULL,
    is_winner BOOLEAN DEFAULT FALSE,
    private List<Integer> claimedMarkedNumbers;
    claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bingo_claims_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
    CONSTRAINT fk_bingo_claims_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT uq_bingo_claims_per_game UNIQUE (game_id, game_entry_id)

    -- ADD MARKED NUMBERS IF NEEDED FOR AUDIT PURPOSES
);

-- Indexes for bingo_claims
CREATE INDEX idx_bingo_claims_game_id ON bingo_claims(game_id);
CREATE INDEX idx_bingo_claims_user_profile_id ON bingo_claims(user_profile_id);
Create INDEX idx_bingo_claims_game_entry_id ON bingo_claims(game_entry_id);

-- Enum types for transactions
CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'AWAITING_APPROVAL', 'COMPLETED', 'FAILED');

-- payment_method Table
CREATE TABLE payment_method (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- transaction Table
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL,
    transfer_to BIGINT,
    payment_method_id BIGINT NOT NULL,
    txn_type transaction_type NOT NULL,
    txn_amount NUMERIC(12,2) NOT NULL,
    status transaction_status NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_transfer_to FOREIGN KEY (transfer_to) REFERENCES user_profile(id) ON DELETE RESTRICT
);

-- Indexes for transaction
CREATE INDEX idx_transaction_user_profile_id ON transaction(user_profile_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_type ON transaction(type);


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


-- support_request Table
CREATE TABLE support_request (
    id BIGSERIAL PRIMARY KEY,
    request_reference TEXT NOT NULL UNIQUE,
    request_title VARCHAR(255) NOT NULL,
    request_text TEXT NOT NULL,
    image_urls JSONB DEFAULT '[]'::jsonb,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_support_request_reference UNIQUE (request_reference)
);

-- char_room Table
CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_char_room_name UNIQUE (room_name)
);

-- chat_message Table
CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES char_room(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user_profile(id) ON DELETE CASCADE
);

-- Indexes for chat_message
CREATE INDEX idx_chat_message_room_id ON chat_message(room_id);
CREATE INDEX idx_chat_message_sender_id ON chat_message(sender_id);
CREATE INDEX idx_chat_message_sent_at ON chat_message(sent_at);


-- system_config Table
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_system_config_name UNIQUE (name)
); --eg. daily_deposit_limit, daily_free_play

-- Indexes for system_config
CREATE INDEX idx_system_config_name ON system_config(name);


CREATE TABLE wallet (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL,
    available_balance NUMERIC(12, 2) NOT NULL DEFAULT 0,
    pending_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    current_total NUMERIC(12, 2) GENERATED ALWAYS AS (available_balance + pending_amount) STORED,
    overall_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    welcome_bonus NUMERIC(12, 2) NOT NULL DEFAULT 0,
    bonus_balance NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_prize_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wallet_user UNIQUE (user_profile_id),
    CONSTRAINT fk_wallet_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_created_by FOREIGN KEY (created_by) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_updated_by FOREIGN KEY (updated_by) REFERENCES user_profile(id) ON DELETE RESTRICT
);

CREATE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);

CREATE TABLE game_card_audit (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    user_profile_id BIGINT NOT NULL,
    card_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_card_audit_game FOREIGN KEY (game_id) REFERENCES game(id),
    CONSTRAINT fk_game_card_audit_user FOREIGN KEY (user_profile_id) REFERENCES user_profile(id)
);