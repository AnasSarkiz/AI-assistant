package com.anas.aiassistant.api;

import java.util.ArrayList;
import java.util.List;

public class GeminiRequest {
    public String model;
    public List<Message> messages;

    public GeminiRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}