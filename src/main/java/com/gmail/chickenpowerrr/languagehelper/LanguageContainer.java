package com.gmail.chickenpowerrr.languagehelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

public class LanguageContainer {

  @Setter private LanguageContainer fallback;
  @Getter private final String language;
  private final Properties properties;

  public LanguageContainer(String language, InputStream inputStream, LanguageContainer fallback)
      throws IOException {
    this.language = language;
    this.fallback = fallback;

    this.properties = new Properties();

    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
        StandardCharsets.UTF_8)) {
      this.properties.load(inputStreamReader);
    }
  }

  public LanguageContainer(String language, InputStream inputStream) throws IOException {
    this(language, inputStream, null);
  }

  String getTranslation(String key) {
    if (!hasTranslation(key) && this.fallback != null) {
      return this.fallback.getTranslation(key);
    }
    return this.properties.getProperty(key);
  }

  boolean hasTranslation(String key) {
    return this.properties.containsKey(key);
  }

  Collection<String> getTranslations() {
    return this.properties.keySet().stream().map(key -> (String) key).collect(Collectors.toSet());
  }

  void addLine(String key, String value) {
    this.properties.setProperty(key, value);
  }
}
