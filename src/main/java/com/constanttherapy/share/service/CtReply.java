package com.constanttherapy.share.service;

import com.constanttherapy.share.service.errors.ServiceError;
import com.constanttherapy.share.service.errors.UserError;
import com.constanttherapy.share.service.messages.Message;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// TODO replace with AutoValue
public class CtReply<D> {
    private D ctdata;
    private List<? extends Message> ctmsgs;
    private CtStatus ctstatus;

    public CtReply(D ctdata, List<? extends Message> ctmsgs, CtStatus ctstatus) {
        this.ctdata = ctdata;
        this.ctmsgs = ctmsgs;
        this.ctstatus = ctstatus;
    }

    public static <D> Builder<D> builder() {
        return new Builder<>();
    }

    public static final class Builder<D> {
        private D ctdata;
        private Type dataType;
        private List<Message> ctmsgs;
        private CtStatus ctstatus;

        public Builder() {
        }

        public CtReply.Builder<D> addMsg(Message msg) {
            if (this.ctmsgs == null) {
                this.ctmsgs = new ArrayList<>();
            }
            this.ctmsgs.add(msg);
            return this;
        }

        public CtReply.Builder<D> ctmsgs(List<Message> msgs) {
            this.ctmsgs = msgs;
            return this;
        }

        public CtReply.Builder<D> ctdata(D data) {
            this.ctdata = data;
            return this;
        }

        public CtReply.Builder<D> status(CtStatus ctStatus) {
            this.ctstatus = ctStatus;
            return this;
        }

        public CtReply<D> build() {
            return new CtReply<>(ctdata, ctmsgs, ctstatus);
        }
    }

    public static CtReply<Void> withSuccess(String successMessage) {
        return new CtReply<>(null, null, CtStatus.withSuccess(successMessage));
    }

    public static CtReply<Void> withServiceError(String message) {
        return new CtReply<>(null, null, CtStatus.withErrors(ServiceError.asList(new ServiceError(""))));
    }

    public static CtReply<Void> withUserError(String field, String message) {
        return new CtReply<>(null, null, CtStatus.withErrors(UserError.asList(new UserError(field, message))));
    }
}
