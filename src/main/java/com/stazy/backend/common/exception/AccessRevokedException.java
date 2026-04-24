package com.stazy.backend.common.exception;

public class AccessRevokedException extends RuntimeException {
    public AccessRevokedException(String message) {
        super(message);
    }
}
