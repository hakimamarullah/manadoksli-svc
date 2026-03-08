package com.manadoksli.exceptions;

public class DecryptionException extends ApiException {

    public DecryptionException(Throwable cause) {
        super("Error while decrypting value", cause);
    }
}
