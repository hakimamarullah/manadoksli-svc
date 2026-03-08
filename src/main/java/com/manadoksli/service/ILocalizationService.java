package com.manadoksli.service;

import java.util.Locale;

public interface ILocalizationService {

    String getMessage(String code, Object[] params);

    String getMessageWithLocale(String code, Object[] params, Locale locale);

    String getMessage(String code);

    String getMessageWithLocale(String code, Locale locale);
}
