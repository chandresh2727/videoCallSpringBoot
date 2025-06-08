package com.example.videocall;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();  // example: ?user=Alice
        String user = query.split("=")[1];
        sessions.put(user, session);
        System.out.println("Connection established with user: " + user);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Parse incoming message JSON into a Map
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);

        // Determine who the message is for
        String toUser = (String) payload.get("to");

        // Send message to the recipient
        WebSocketSession receiverSession = sessions.get(toUser);
        if (receiverSession != null && receiverSession.isOpen()) {
            String jsonResponse = objectMapper.writeValueAsString(payload);
            receiverSession.sendMessage(new TextMessage(jsonResponse));
        } else {
            System.out.println("User " + toUser + " not connected.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Remove the user from sessions
        sessions.values().removeIf(sess -> sess.getId().equals(session.getId()));
        System.out.println("Connection closed: " + session.getId());
    }
}
