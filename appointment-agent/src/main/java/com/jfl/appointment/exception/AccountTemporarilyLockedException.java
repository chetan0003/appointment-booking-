package com.jfl.appointment.exception;

public class AccountTemporarilyLockedException extends RuntimeException {

    public AccountTemporarilyLockedException(String msg) {
        super(msg);
    }
}
