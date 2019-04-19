package com.gmail.chickenpowerrr.languagehelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class LanguageHelper {

  private static final String FILE_SEPARATOR = System.getProperty("file.separator");

  private final Map<String, LanguageFile> languageResources;

  public LanguageHelper(File languageTargetDirectory) {
    this(LanguageHelper.class, languageTargetDirectory);
  }

  public LanguageHelper(Class clazz, File languageTargetDirectory) {
    languageTargetDirectory = new File(languageTargetDirectory.getPath() + "/language");
    languageTargetDirectory.mkdirs();
    this.languageResources = getLanguageFiles(getLanguageResources(clazz), languageTargetDirectory);
  }

  private Map<String, LanguageResource> getLanguageResources(Class clazz) {
    return getInputStreams(clazz.getClassLoader(), getFilesFromDirectory(clazz, "language"))
        .entrySet()
        .stream().map((entry) -> {
          String languageDirectoryName = "language" + FILE_SEPARATOR;
          String languageFileName = entry.getKey().substring(
              entry.getKey().lastIndexOf(languageDirectoryName) + languageDirectoryName.length());
          String languageName = languageFileName.substring(0, languageFileName.lastIndexOf(".txt"))
              .replaceAll("/", "");

          try {
            return new SimpleEntry<>(languageName,
                new LanguageResource(languageName, entry.getValue()));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, LanguageFile> getLanguageFiles(
      Map<String, LanguageResource> languageResources, File languageTargetDirectory) {

    languageResources.values().stream().filter(languageResource ->
        !new File(languageTargetDirectory, languageResource.getLanguage() + ".txt").exists())
        .map(languageResource -> {
          return new SimpleEntry<>(languageResource,
              getInputStream(LanguageHelper.class.getClassLoader(),
                  "language/" + languageResource.getLanguage() + ".txt"));
        })
        .forEach(entry -> {
          File languageFile = new File(languageTargetDirectory,
              entry.getKey().getLanguage() + ".txt");
          try {
            Files.copy(entry.getValue(), languageFile.toPath());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    return Arrays.stream(languageTargetDirectory.listFiles())
        .filter(file -> file.getName().endsWith(".txt"))
        .map(file -> {
          try {
            return new SimpleEntry<>(file.getName().replace(".txt", ""),
                new LanguageFile(file, languageResources.get(file.getName().replace(".txt", ""))));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public String getMessage(String language, String translationKey) {
    if (this.languageResources.containsKey(language)) {
      String translation = this.languageResources.get(language).getTranslation(translationKey);
      if (translation != null) {
        return translation;
      }
      return "We couldn't find a translation";
    }
    return "The language: \"" + language + "\" doesn't exist";
  }

  private InputStream getInputStream(ClassLoader classLoader, String resourceFile) {
    URL testUrl = classLoader.getResource("");

    if (testUrl == null || testUrl.getProtocol().equals("jar")) {
      return classLoader.getResourceAsStream(resourceFile);
    } else if (testUrl.getProtocol().equals("file")) {
      try {
        String startUri = testUrl.toURI().getPath();
        return new FileInputStream(new File(startUri + resourceFile));
      } catch (URISyntaxException | FileNotFoundException e) {
        return null;
      }
    } else {
      throw new RuntimeException(new IllegalArgumentException("A invalid protocol has been found"));
    }
  }

  private Map<String, InputStream> getInputStreams(ClassLoader classLoader,
      Collection<String> resourceFiles) {
    return resourceFiles.stream().map(name -> {
      InputStream inputStream = getInputStream(classLoader, name);
      if (inputStream != null) {
        return new HashMap.SimpleEntry<>(name, inputStream);
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Collection<String> getFilesFromDirectory(Class clazz, String directory) {
    URL directoryUrl = clazz.getClassLoader().getResource(directory);
    Collection<String> files = new HashSet<>();
    if (directoryUrl == null || directoryUrl.getProtocol().equals("jar")) {
      try {
        JarFile jarFile = new JarFile(
            clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        JarEntry jarEntry;

        while (jarEntries.hasMoreElements() && (jarEntry = jarEntries.nextElement()) != null) {
          if (!jarEntry.isDirectory() && jarEntry.getName()
              .startsWith(directory + (directory.endsWith("/") ? "" : "/"))) {
            files.add(jarEntry.getName());
          }
        }
      } catch (URISyntaxException | IOException e) {
        throw new RuntimeException(e);
      }
    } else if (directoryUrl.getProtocol().equals("file")) {
      try {
        File directoryFile = new File(directoryUrl.toURI());
        for (File file : directoryFile.listFiles()) {
          files.add(file.getPath().substring(file.getPath().indexOf(
              FILE_SEPARATOR + directory + (directory.endsWith(FILE_SEPARATOR) ? ""
                  : FILE_SEPARATOR)) + 1).replace(FILE_SEPARATOR, "/"));
        }
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException(
          new IllegalArgumentException("An invalid protocol has been found"));
    }
    return files;
  }
}
