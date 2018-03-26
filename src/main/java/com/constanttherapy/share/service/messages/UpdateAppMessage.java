package com.constanttherapy.share.service.messages;

public class UpdateAppMessage extends Message {
    public String description;

    public UpdateAppMessage(String description) {
        this.description = description;
    }
}
