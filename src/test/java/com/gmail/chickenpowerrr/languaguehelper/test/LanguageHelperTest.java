package com.gmail.chickenpowerrr.languaguehelper.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import com.gmail.chickenpowerrr.languagehelper.LanguageHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LanguageHelperTest {

  private final LanguageHelper languageHelper;
  private final File outputDirectory;
  private final File incompleteInputFile;
  private final File completeInputFile;
  private final File completeOutputFile;
  private final File incompleteOutputFile;

  public LanguageHelperTest() throws IOException {
    File testResourcesDirectory = new File("src/test/resources/");

    this.incompleteInputFile = new File(
        testResourcesDirectory.getAbsolutePath() + "/test_incomplete.txt");
    this.completeInputFile = new File(
        testResourcesDirectory.getAbsolutePath() + "/language/test_complete_language.txt");

    this.completeOutputFile = new File(
        testResourcesDirectory.getAbsolutePath() + "/output/language/test_complete_language.txt");
    this.incompleteOutputFile = new File(
        testResourcesDirectory.getAbsolutePath() + "/output/language/test_incomplete_language.txt");

    this.completeOutputFile.getParentFile().mkdirs();

    for (File file : this.completeOutputFile.getParentFile().listFiles()) {
      file.delete();
    }

    Files.copy(this.incompleteInputFile.toPath(), this.incompleteOutputFile.toPath());
    Files.copy(this.completeInputFile.toPath(), this.completeOutputFile.toPath());

    this.languageHelper = new LanguageHelper(new File("src/test/resources/output"));
    this.outputDirectory = new File(testResourcesDirectory.getAbsolutePath() + "/output/language/");
  }

  @Test
  public void copyFileTest() throws IOException {
    File copyLanguageFile = new File("src/test/resources/language/test_copy_language.txt");
    File copiedLanguageFile = new File("src/test/resources/output/language/test_copy_language.txt");

    try (FileReader copyLanguageFileReader = new FileReader(copyLanguageFile);
        BufferedReader bufferedCopyLanguageFileReader = new BufferedReader(copyLanguageFileReader);
        FileReader copiedLanguageFileReader = new FileReader(copiedLanguageFile);
        BufferedReader bufferedCopiedLanguageFileReader = new BufferedReader(
            copiedLanguageFileReader)) {

      String copy = bufferedCopyLanguageFileReader.lines().collect(Collectors.joining("\n"));
      String copied = bufferedCopiedLanguageFileReader.lines().collect(Collectors.joining("\n"));

      assertThat(copy, equalTo(copied));
    }
  }

  @Test
  public void updateExistingIncompleteFileTest() throws IOException {
    try (FileReader incompleteInputReader = new FileReader(
        new File("src/test/resources/language/test_incomplete_language.txt"));
        BufferedReader bufferedIncompleteInputReader = new BufferedReader(incompleteInputReader);
        FileReader incompleteOutputReader = new FileReader(
            new File("src/test/resources/output/language/test_incomplete_language.txt"));
        BufferedReader bufferedIncompleteOutputReader = new BufferedReader(
            incompleteOutputReader)) {
      String input = bufferedIncompleteInputReader.lines().collect(Collectors.joining("\n"));
      String output = bufferedIncompleteOutputReader.lines().collect(Collectors.joining("\n"));

      assertThat(input, equalTo(output));
    }
  }

  @Test
  public void readNotExistingLanguageTest() {
    Pattern pattern = Pattern.compile("The language: \".+\" doesn't exist");
    Matcher nonExistingKey = pattern.matcher(this.languageHelper.getMessage("non_existing", "something"));
    Matcher existingKey = pattern.matcher(this.languageHelper.getMessage("non_existing", "test-one"));

    assertThat(nonExistingKey.find(), is(true));
    assertThat(existingKey.find(), is(true));
  }

  @Test
  public void readNotExistingTranslationTest() {
    assertThat(this.languageHelper.getMessage("test_copy_language", "non_existing"),
        startsWith("We couldn't find a translation"));
  }

  @Test
  public void readExistingCompleteFileTest() {
    assertThat(this.languageHelper.getMessage("test_complete_language", "test-one"),
        equalTo("Test One"));
    assertThat(this.languageHelper.getMessage("test_complete_language", "test-two"),
        equalTo("Test Two"));
    assertThat(this.languageHelper.getMessage("test_complete_language", "test-three"),
        equalTo("Test Three"));
  }
}
