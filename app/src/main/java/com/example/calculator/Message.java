package com.example.calculator;

public class Message {
    public final String sender; // "user" or "assistant"
    public final String text;
    public final boolean isUser;

    public Message(String sender, String text, boolean isUser) {
        this.sender = sender;
        this.text = text;
        this.isUser = isUser;
    }

    public static Message user(String text) {
        return new Message("user", text, true);
    }

    public static Message assistant(String text) {
        return new Message("assistant", text, false);
    }
}