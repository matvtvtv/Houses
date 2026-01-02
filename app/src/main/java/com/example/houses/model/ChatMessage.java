package com.example.houses.model;

public class ChatMessage {
    private Long id;
    private String sender;
    private String content;
    private String timestamp; // сервер отдаёт LocalDateTime; мы принимаем как String

    public ChatMessage() {}

    public ChatMessage(Long id, String sender, String content, String timestamp) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
