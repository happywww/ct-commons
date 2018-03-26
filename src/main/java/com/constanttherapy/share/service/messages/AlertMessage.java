package com.constanttherapy.share.service.messages;

import java.util.HashMap;
import java.util.Map;

public class AlertMessage extends Message {
    public String title;
    public String body;
    public Map<Button.Type, Button> buttons;

    public AlertMessage(String title, String body, Map<Button.Type, Button> buttons) {
        this.title = title;
        this.body = body;
        this.buttons = buttons;
        this.type = Type.alert;
    }

    public AlertMessage(String title, String body) {
        this.title = title;
        this.body = body;
        this.buttons = new HashMap<>();
    }

    public static class Button {
        enum Type {
            positive,
            neutral,
            negative
        }
        public Button.Type type;

        enum Action {
            dismiss,
            update,
            logout
        }
        public Action action;

        public String text;

        private Button(Type type, Action action, String text) {
            this.type = type;
            this.action = action;
            this.text = text;
        }

        public static class Builder {
            private Type type;
            private Action action;
            private String text;

            public Builder() {

            }

            public Button.Builder type(Type type) {
                this.type = type;
                return this;
            }

            public Button.Builder action(Action action) {
                this.action = action;
                return this;
            }

            public Button.Builder text(String text) {
                this.text = text;
                return this;
            }

            public Button build() {
                return new Button(type, action, text);
            }
        }
    }

    public void addButton(Button.Type type, Button button) {
        buttons.put(type, button);
    }
}
