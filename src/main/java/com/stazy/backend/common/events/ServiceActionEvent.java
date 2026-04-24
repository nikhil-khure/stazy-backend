package com.stazy.backend.common.events;

public class ServiceActionEvent {
    private String service;
    private String action;

    public ServiceActionEvent(String service, String action) {
        this.service = service;
        this.action = action;
    }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
