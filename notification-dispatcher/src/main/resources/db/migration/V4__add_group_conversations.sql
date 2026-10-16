ALTER TABLE conversations
    ALTER COLUMN user1_id DROP NOT NULL,
    ALTER COLUMN user2_id DROP NOT NULL,
    ADD COLUMN conversation_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN group_name VARCHAR(100);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id)
);

INSERT INTO conversation_members (conversation_id, user_id)
SELECT id, user1_id FROM conversations WHERE user1_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO conversation_members (conversation_id, user_id)
SELECT id, user2_id FROM conversations WHERE user2_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE conversations
    ADD CONSTRAINT chk_conversations_type
    CHECK (
        (conversation_type = 'DIRECT' AND user1_id IS NOT NULL AND user2_id IS NOT NULL)
        OR (conversation_type = 'GROUP' AND group_name IS NOT NULL AND user1_id IS NULL AND user2_id IS NULL)
    );

CREATE INDEX idx_conversation_members_user
    ON conversation_members (user_id, conversation_id);
