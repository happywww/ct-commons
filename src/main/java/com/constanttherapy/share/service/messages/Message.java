package com.constanttherapy.share.service.messages;

import java.util.ArrayList;
import java.util.List;

public abstract class Message {
    enum Type {
        alert,
        audio,
        rating,
        update,
        refreshUser,
        resourceDomain
    }
    Type type;

    static final class ListBuilder  {
        private List<Message> ctmsgs;

        ListBuilder() {
            ctmsgs = new ArrayList<>();
        }

        ListBuilder addMsg(Message msg) {
            ctmsgs.add(msg);
            return this;
        }

        List<Message> asList() {
            return ctmsgs;
        }
    }
}
