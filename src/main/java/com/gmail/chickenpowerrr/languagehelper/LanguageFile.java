package com.gmail.chickenpowerrr.languagehelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageFile extends LanguageContainer {

  private final LanguageResource languageResource;
  private final File file;

  public LanguageFile(File file, LanguageResource languageResource) throws IOException {
    super(file.getName().replace(".txt", ""), new FileInputStream(file));
    this.file = file;
    this.languageResource = languageResource;

    updateTranslations();
  }

  private void updateTranslations() {
    if (this.languageResource != null) {
      addLines(this.languageResource.getTranslations().stream().filter(key -> !hasTranslation(key))
          .map(key -> new HashMap.SimpleEntry<>(key, this.languageResource.getTranslation(key)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
  }

  private void addLines(Map<String, String> translations) {
    try (FileOutputStream fileOutputStream = new FileOutputStream(this.file, true);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream,
            StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
      translations.forEach(this::addLine);

      if (translations.size() > 0) {
        bufferedWriter.write(
            translations.entrySet().stream().map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator(), System.lineSeparator(), "")));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
