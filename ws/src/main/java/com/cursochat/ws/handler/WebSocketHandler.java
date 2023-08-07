package com.cursochat.ws.handler;

import com.cursochat.ws.data.User;
import com.cursochat.ws.dto.ChatMessage;
import com.cursochat.ws.events.Event;
import com.cursochat.ws.events.EventType;
import com.cursochat.ws.pubsub.Publisher;
import com.cursochat.ws.services.TickectService;
import com.cursochat.ws.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final static Logger logger = Logger.getLogger(WebSocketHandler.class.getName());

    private final TickectService tickectService;
    private final UserService userService;

    private final Publisher publisher;
    private final Map<String, WebSocketSession> sessions;
    private final Map<String, String> usersId;

    public WebSocketHandler(Publisher publisher, TickectService tickectService, UserService userService) {
        this.publisher = publisher;
        this.tickectService = tickectService;
        this.userService = userService;

        this.sessions = new ConcurrentHashMap<>();
        this.usersId = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("[afterConnectionEstablished] session id " + session.getId());
        Optional<String> ticket = ticketOf(session);

        if (ticket.isEmpty() || ticket.get().isBlank()) {
            logger.warning("session " + session.getId() + " without ticket");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Optional<String> userId = tickectService.getUserByTicket(ticket.get());

        if (userId.isEmpty()) {
            logger.warning("session " + session.getId() + " with invalid ticket");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(userId.get(), session);
        usersId.put(session.getId(), userId.get());
        logger.info("session " + session.getId() + " was bind to user " + userId.get());
        sendChatUsers(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("[handleTextMessage] message " + message.getPayload());

        if (message.getPayload().equals("ping")) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        MessagePayload payload = new ObjectMapper().readValue(message.getPayload(), MessagePayload.class);
        String userIdFrom = usersId.get(session.getId());

        publisher.publishChatMessage(userIdFrom, payload.to(), payload.text());
    }

    public void notify(ChatMessage chatMessage) {
        Event<ChatMessage> event = new Event<>(EventType.CHAT_MESSAGE_WAS_CREATED, chatMessage);
        List<String> userIds = List.of(chatMessage.from().id(), chatMessage.to().id());
        userIds.stream()
                .distinct()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .forEach(session -> sendEvent(session, event));

        logger.info("Chat message was notified");
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("[afterConnectionClosed] session id " + session.getId());
        String userId = usersId.get(session.getId());
        sessions.remove(userId);
        usersId.remove(session.getId());
    }


    private Optional<String> ticketOf(WebSocketSession session) {
        return Optional.ofNullable(session.getUri())
                .map(UriComponentsBuilder::fromUri)
                .map(UriComponentsBuilder::build)
                .map(UriComponents::getQueryParams)
                .map(it -> it.get("ticket"))
                .flatMap(it -> it.stream().findFirst())
                .map(String::trim);
    }

    private void sendChatUsers(WebSocketSession session) {
        List<User> chatUsers = userService.findChatUsers();
        Event<List<User>> event = new Event<>(EventType.CHAT_USERS_WERE_UPDATED, chatUsers);
        sendEvent(session, event);
    }

    private void sendEvent(WebSocketSession session, Event<?> event) {
        try {
            String eventSerialized = new ObjectMapper().writeValueAsString(event);
            session.sendMessage(new TextMessage(eventSerialized));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
