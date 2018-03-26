package com.constanttherapy.share.service.errors;

public class ServiceError extends BaseError {
    String message;

    public ServiceError(String message) {
        super(Type.serviceError);
        this.message = message;
    }

}
