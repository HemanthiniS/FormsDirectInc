package com.formsdirectinc.services.account;

public class AccountCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    public AccountCreationException() {
    }

    public AccountCreationException(String message) {
        super(message);
    }

    public AccountCreationException(Throwable cause) {
        super(cause);
    }

    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}
