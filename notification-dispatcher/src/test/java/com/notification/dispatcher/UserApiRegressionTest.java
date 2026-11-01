package com.notification.dispatcher;

import com.notification.dispatcher.entity.*;
import com.notification.dispatcher.repository.*;
import com.notification.dispatcher.rabbitmq.RabbitMQProducer;
import com.notification.dispatcher.security.JwtService;
import com.notification.dispatcher.DTO.TypingEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class UserApiRegressionTest {
    @Autowired MockMvc mvc;
    @Autowired TaiKhoanRepository users;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired NotificationRepository notifications;
    @Autowired JwtService tokens;
    @MockitoBean RabbitMQProducer producer;
    TaiKhoan a, b, outsider;
    Conversation direct, group;

    @BeforeEach void setup() {
        a = account(); b = account(); outsider = account();
        direct = conversations.saveAndFlush(new Conversation(a, b));
        group = conversations.saveAndFlush(Conversation.group("Test group", new LinkedHashSet<>(List.of(a, b))));
    }
    TaiKhoan account() {
        String name = UUID.randomUUID().toString();
        return users.saveAndFlush(new TaiKhoan(name, name + "@example.test", name, "test-hash"));
    }
    String bearer(TaiKhoan user) { return "Bearer " + tokens.issue(user.getId()).accessToken(); }
    String message(Conversation c, String text) {
        return "{\"conversationId\":\"" + c.getId() + "\",\"messageContent\":\"" + text + "\"}";
    }

    @Test void outsiderCannotReadOrSendDirectAndGroupAndNothingIsSaved() throws Exception {
        long before = messages.count();
        for (var c : List.of(direct, group)) {
            mvc.perform(get("/api/conversations/" + c.getId() + "/messages")
                    .header("Authorization", bearer(outsider))).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            mvc.perform(post("/api/messages").header("Authorization", bearer(outsider))
                    .contentType(MediaType.APPLICATION_JSON).content(message(c, "Forbidden")))
                    .andExpect(status().isForbidden());
        }
        assertEquals(before, messages.count());
        verifyNoInteractions(producer);
    }

    @Test void removedMemberIsDenied() throws Exception {
        group.getMembers().add(outsider); conversations.saveAndFlush(group);
        mvc.perform(get("/api/conversations/" + group.getId() + "/messages")
                .header("Authorization", bearer(outsider))).andExpect(status().isOk());
        group.getMembers().remove(outsider); conversations.saveAndFlush(group);
        mvc.perform(get("/api/conversations/" + group.getId() + "/messages")
                .header("Authorization", bearer(outsider))).andExpect(status().isForbidden());
    }

    @Test void notificationHistoryKeepsOriginalMessageContent() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mvc.perform(post("/api/chat/send").header("Authorization", bearer(a))
                    .contentType(MediaType.APPLICATION_JSON).content(message(direct, "Notification " + i)))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/notifications").header("Authorization", bearer(b)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].messageContent", org.hamcrest.Matchers.containsInAnyOrder(
                        "Notification 1", "Notification 2", "Notification 3")));
        for (var n : notifications.findByRecipientIdOrderByCreatedAtDesc(b.getId())) {
            assertNotNull(n.getMessage());
            assertEquals(direct.getId(), n.getMessage().getConversation().getId());
        }
    }

    @Test void groupNotificationReferencesCorrectMessage() throws Exception {
        mvc.perform(post("/api/messages").header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(message(group, "Group content")))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/notifications").header("Authorization", bearer(b)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].messageContent").value("Group content"))
                .andExpect(jsonPath("$[0].conversationId").value(group.getId().toString()));
    }

    @Test void bothTypingSpellingsWorkAndSenderComesFromToken() throws Exception {
        for (String spelling : List.of("typing", "isTyping")) {
            mvc.perform(post("/api/chat/typing").header("Authorization", bearer(a))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"conversationId\":\"" + direct.getId()
                            + "\",\"senderId\":\"" + outsider.getId() + "\",\"" + spelling + "\":true}"))
                    .andExpect(status().isNoContent());
        }
        verify(producer, times(2)).sendTyping(argThat((TypingEventDTO e) ->
                e.isTyping() && e.getSenderId().equals(a.getId().toString())), eq(b.getId()));
    }

    @Test void outsiderCannotTypeOrMarkRead() throws Exception {
        mvc.perform(post("/api/chat/typing").header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON).content("{\"conversationId\":\"" + direct.getId()
                        + "\",\"senderId\":\"" + a.getId() + "\",\"isTyping\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chat/conversations/" + direct.getId() + "/read")
                .header("Authorization", bearer(outsider))).andExpect(status().isForbidden());
        verifyNoInteractions(producer);
    }

    @Test void emptyMessageRemainsBadRequest() throws Exception {
        mvc.perform(post("/api/messages").header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(message(direct, "   ")))
                .andExpect(status().isBadRequest());
    }
}
