package com.constanttherapy.share.service;

import com.constanttherapy.share.service.errors.BaseError;

import java.util.List;

public class CtStatus {
    public String success;
    public List<? extends BaseError> errors;

    private CtStatus(String success) {
        this.success = success;
    }

    public static CtStatus withSuccess(String successMessage) {
        return new CtStatus(successMessage);
    }

    private CtStatus(List<? extends BaseError> errors) {
        this.errors = errors;
    }

    public static CtStatus withErrors(List<? extends BaseError> errors) {
        return new CtStatus(errors);
    }
}
