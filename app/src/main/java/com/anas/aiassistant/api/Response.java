package com.anas.aiassistant.api;

import java.util.List;

public class Response {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String content;
    }

    public String getFirstText() {
        if (choices != null && !choices.isEmpty() &&
            choices.get(0).message != null) {
            return choices.get(0).message.content;
        }
        return "No response from AI.";
    }
}