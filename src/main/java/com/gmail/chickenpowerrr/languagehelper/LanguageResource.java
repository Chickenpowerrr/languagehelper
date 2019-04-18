package com.gmail.chickenpowerrr.languagehelper;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

class LanguageResource {

    @Setter private LanguageResource fallback;
    @Getter private final String language;
    private final Map<String, String> translations;

    LanguageResource(String language, Map<String, String> translations) {
        this.language = language;
        this.translations = translations;
    }

    String getTranslation(String key) {
        String translation = this.translations.get(key);
        if(translation == null && this.fallback != null) {
            translation = this.fallback.getTranslation(key);
        }
        return translation;
    }
}
