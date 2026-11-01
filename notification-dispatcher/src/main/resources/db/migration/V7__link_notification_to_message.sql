-- Existing notifications remain unlinked: their originating message cannot be
-- reconstructed reliably from sender/recipient alone.
ALTER TABLE notifications ADD COLUMN message_id UUID REFERENCES messages(id) ON DELETE SET NULL;
CREATE INDEX idx_notifications_message_id ON notifications(message_id);
