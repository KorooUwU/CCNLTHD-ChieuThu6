
package com.notification.dispatcher;

import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.NotificationRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import com.notification.dispatcher.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class NotificationDispatcherApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AuthService authService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext.getBean(AuthService.class));
        assertNotNull(applicationContext.getBean(TaiKhoanRepository.class));
        assertNotNull(applicationContext.getBean(PasswordEncoder.class));

        assertNotNull(conversationRepository);
        assertNotNull(messageRepository);
        assertNotNull(notificationRepository);
    }

    @Test
    void testRepositoriesAccess() {
        long taiKhoanCount = taiKhoanRepository.count();
        long conversationCount = conversationRepository.count();
        long messageCount = messageRepository.count();
        long notificationCount = notificationRepository.count();

        System.out.println("TaiKhoan count: " + taiKhoanCount);
        System.out.println("Conversation count: " + conversationCount);
        System.out.println("Message count: " + messageCount);
        System.out.println("Notification count: " + notificationCount);
    }
}
