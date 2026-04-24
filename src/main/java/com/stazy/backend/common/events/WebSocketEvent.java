package com.stazy.backend.common.events;

public class WebSocketEvent {
    private String eventType;
    private Object payload;
    private long timestamp;

    public WebSocketEvent(String eventType, Object payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
