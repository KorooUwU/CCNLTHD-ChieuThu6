package com.notification.dispatcher.exception;

public class ConversationAccessDeniedException extends RuntimeException {
    public ConversationAccessDeniedException() {
        super("Bạn không có quyền truy cập hội thoại này");
    }
}
