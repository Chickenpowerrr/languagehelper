package com.gmail.chickenpowerrr.languagehelper;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class LanguageHelper {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private final Map<String, LanguageResource> languageResources;

    public LanguageHelper(File languageTargetDirectory) {
        this(LanguageHelper.class, languageTargetDirectory);
    }

    public LanguageHelper(Class clazz, File languageTargetDirectory) {
        languageTargetDirectory = new File(languageTargetDirectory.getPath() + "/language");
        languageTargetDirectory.mkdirs();
        this.languageResources = getLanguageResources(clazz, languageTargetDirectory);
    }

    private void fillFile(InputStream inputStream, File file) {
        try {
            Files.copy(inputStream, file.toPath());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, LanguageResource> getLanguageResources(Class clazz, File languageTargetDirectory) throws IllegalArgumentException {
        if(languageTargetDirectory == null) {
            return getLanguageResources(getInputStreams(clazz.getClassLoader(), getFilesFromDirectory(clazz, "language")));
        } else if(languageTargetDirectory.exists() && languageTargetDirectory.isDirectory()) {
            Map<String, File> existingLanguageFiles = Arrays.stream(languageTargetDirectory.listFiles())
                    .filter(file -> file.getName().endsWith(".txt"))
                    .map(file -> new HashMap.SimpleEntry<>(file.getName(), file))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<String, InputStream> inputStreams = getInputStreams(clazz.getClassLoader(), getFilesFromDirectory(clazz, "language").stream()
                    .filter(name -> !existingLanguageFiles.containsKey(name.substring(name.lastIndexOf("/") + 1)))
                    .collect(Collectors.toSet()));

            inputStreams.forEach((file, inputStream) -> {
                File languageFile = new File(languageTargetDirectory.getPath() + "/" + file.substring(file.lastIndexOf("/") + 1));
                fillFile(inputStream, languageFile);
                try {
                    inputStream.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            });

            inputStreams = getInputStreams(clazz.getClassLoader(), getFilesFromDirectory(clazz, "language").stream()
                    .filter(name -> !existingLanguageFiles.containsKey(name.substring(name.lastIndexOf("/") + 1)))
                    .collect(Collectors.toSet()));

            Map<String, LanguageResource> languageResources = getLanguageResources(inputStreams);

            languageResources.putAll(getLanguageResources(existingLanguageFiles.entrySet().stream().map(entry -> {
                try {
                    return new HashMap.SimpleEntry<>("language" + entry.getKey(), new FileInputStream(entry.getValue()));
                } catch(FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));

            return languageResources;
        } else {
            throw new IllegalArgumentException("Illegal language directory found: " + languageTargetDirectory.getPath());
        }
    }

    private Map<String, String> getFileContent(Map<String, InputStream> inputStreams) {
        return inputStreams.entrySet().stream().map(entry -> {
            try(InputStream inputStream = entry.getValue();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                String value = bufferedReader.lines().collect(Collectors.joining("\n"));
                return new HashMap.SimpleEntry<>(entry.getKey(), value);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, LanguageResource> getLanguageResources(Map<String, InputStream> inputStreams) {
        return inputStreams.entrySet().stream().map(entry -> {
            Properties properties = new Properties();
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(entry.getValue(), StandardCharsets.UTF_8);
                properties.load(inputStreamReader);
                inputStreamReader.close();
                entry.getValue().close();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            String languageDirectoryName = "language" + FILE_SEPARATOR;
            String languageFileName = entry.getKey().substring(entry.getKey().lastIndexOf(languageDirectoryName) + languageDirectoryName.length());
            String languageName = languageFileName.substring(0, languageFileName.lastIndexOf(".txt")).replaceAll("/", "");
            return new HashMap.SimpleEntry<>(languageName, new LanguageResource(languageName, properties.entrySet().stream().map(e -> new HashMap.SimpleEntry<>((String) e.getKey(), (String) e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getMessage(String language, String translationKey) {
        if(this.languageResources.containsKey(language)) {
            String translation = this.languageResources.get(language).getTranslation(translationKey);
            if(translation != null) {
                return translation;
            }
        }
        return "We couldn't find a translation";
    }

    private Map<String, InputStream> getInputStreams(ClassLoader classLoader, Collection<String> files) {
        URL testUrl = classLoader.getResource("");

        if(testUrl == null || testUrl.getProtocol().equals("jar")) {
            return files.stream().map(name ->
                    new HashMap.SimpleEntry<>(name, classLoader.getResourceAsStream(name)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else if(testUrl.getProtocol().equals("file")) {
            try {
                String startUri = testUrl.toURI().getPath();
                return files.stream().map(name -> {
                    File file = new File(startUri + name);
                    try {
                        return new HashMap.SimpleEntry<>(name, new FileInputStream(file));
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch(URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(new IllegalArgumentException("A invalid protocol has been found"));
        }
    }

    private Map<String, String> getFileContent(ClassLoader classLoader, Collection<String> files) {
        return getInputStreams(classLoader, files).entrySet().stream().map(entry -> {
            try(InputStream inputStream = entry.getValue();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                return new HashMap.SimpleEntry<>(entry.getKey(), bufferedReader.lines().collect(Collectors.joining("\n")));
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Collection<String> getFilesFromDirectory(Class clazz, String directory) {
        URL directoryUrl = clazz.getClassLoader().getResource(directory);
        Collection<String> files = new HashSet<>();
        if(directoryUrl == null || directoryUrl.getProtocol().equals("jar")) {
            try {
                JarFile jarFile = new JarFile(clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                JarEntry jarEntry;

                while(jarEntries.hasMoreElements() && (jarEntry = jarEntries.nextElement()) != null) {
                    if(!jarEntry.isDirectory() && jarEntry.getName().startsWith(directory + (directory.endsWith("/") ? "" : "/"))) {
                        files.add(jarEntry.getName());
                    }
                }
            } catch(URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        } else if(directoryUrl.getProtocol().equals("file")) {
            try {
                File directoryFile = new File(directoryUrl.toURI());
                for(File file : directoryFile.listFiles()) {
                    files.add(file.getPath().substring(file.getPath().indexOf(FILE_SEPARATOR + directory + (directory.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)) + 1).replace(FILE_SEPARATOR, "/"));
                }
            } catch(URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(new IllegalArgumentException("An invalid protocol has been found"));
        }
        return files;
    }
}
