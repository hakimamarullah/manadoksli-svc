package com.manadoksli.exceptions;

public class EncryptionException extends ApiException{

    public EncryptionException(Throwable cause) {
        super("Error while encrypting value", cause);
    }
}
