package com.constanttherapy.share.service.errors;

public class UserError extends BaseError {
    final String field;
    final String message;

    public UserError(String field, String message) {
        super(Type.userError);
        this.field = field;
        this.message = message;
    }
}
