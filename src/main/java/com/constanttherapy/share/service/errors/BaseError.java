package com.constanttherapy.share.service.errors;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseError {
    enum Type {
        serviceError,
        userError,
        authError,
        invalidPatientId
    }

    Type type;

    public BaseError(Type type) {
        this.type = type;
    }

    public static <T> List<T> asList(T error) {
        List<T> errors = new ArrayList<>();
        errors.add(error);
        return errors;
    }
}
