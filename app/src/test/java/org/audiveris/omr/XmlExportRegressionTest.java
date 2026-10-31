//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         X m l E x p o r t R e g r e s s i o n T e s t                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Regression test that validates Audiveris' conversion of sheet-music images (png/jpg/pdf)
 * to MusicXML, against known good reference outputs.
 * <p>
 * The reference folder ({@code data/regression}) holds pairs of files: an input
 * image ({@code file1.png}, {@code file2.pdf}, ...) and the known good MusicXML output
 * ({@code file1.xml}, {@code file2.xml}) for it. Both files share the same base name.
 * <p>
 * For every input file found in the reference folder, the test:
 * <ul>
 * <li>copies it into a temporary work folder (never touching the original),</li>
 * <li>runs the full batch pipeline ({@code -batch -transcribe -export}), with the work folder
 * as output folder,</li>
 * <li>extracts the produced MusicXML (unzipped from the generated {@code .mxl} if needed),</li>
 * <li>compares it against the reference {@code .xml} file, after normalization of a few
 * volatile fields that cannot be reproduced byte for byte:
 * <ul>
 * <li>{@code <encoding-date>} lines are dropped (they carry the export date)</li>
 * <li>{@code <software>} lines are dropped (they carry the Audiveris version)</li>
 * <li>{@code font-family} and {@code font-size} attributes are dropped</li>
 * <li>absolute input paths in {@code <source>} and in the {@code source-file}
 * miscellaneous field are reduced to the bare file name</li>
 * </ul></li>
 * </ul>
 * Every input file is tested, and a final report lists all failures (not just the first).
 * On failure the whole temp work folder is kept for inspection.
 * <p>
 * When the environment variable {@code AUDIVERIS_UPDATE_BASELINE} is set to {@code true}
 * (or {@code 1}), the produced MusicXML becomes the new reference for each input file
 * (the previous reference being kept as {@code <name>.xml.orig}).
 * Handy to generate or refresh the reference set.
 */
public class XmlExportRegressionTest
{
    //~ Static fields ///////////////////////////////////////////////////////////////////////////////

    /** Extensions accepted as input images. */
    private static final List<String> INPUT_EXTENSIONS =
            Arrays.asList("png", "jpg", "jpeg", "pdf");

    /** Timeout, in seconds, for a single file conversion. */
    private static final long TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(15);

    /** System property providing the application JVM options, comma-separated. */
    private static final String JVM_ARGS_PROPERTY = "audiveris.jvmArgs";

    /** JVM options used when {@link #JVM_ARGS_PROPERTY} is not set (e.g. run from an IDE). */
    private static final List<String> DEFAULT_JVM_ARGS = Arrays.asList(
            "--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED",
            "--enable-native-access=ALL-UNNAMED",
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8");

    /**
     * Constants imposed on Audiveris, regardless of the user configuration,
     * so that exactly one MusicXML file is produced per input.
     */
    private static final List<String> FORCED_CONSTANTS = Arrays.asList(
            "org.audiveris.omr.sheet.BookManager.useOpus=false");

    /** Maximum number of unified diff lines reported per file. */
    private static final int MAX_DIFF_LINES = 80;

    /** Pattern to match an <encoding-date> line (volatile, dropped). */
    private static final Pattern ENCODING_DATE =
            Pattern.compile("\\s*<encoding-date>.*</encoding-date>\\s*");

    /** Pattern to match a <software> line (carries the Audiveris version, dropped). */
    private static final Pattern SOFTWARE = Pattern.compile("\\s*<software>.*</software>\\s*");

    /** Pattern to match font attributes whose values vary across environments. */
    private static final Pattern FONT_ATTRIBUTES =
            Pattern.compile("\\s+font-(?:family|size)=\"[^\"]*\"");

    /** Pattern to match the <source> element that carries the absolute input path. */
    private static final Pattern SOURCE_LINE = Pattern.compile("(<source>)(.*)(</source>)");

    /** Pattern to match the source-file miscellaneous field (absolute input path). */
    private static final Pattern SOURCE_FILE_LINE = Pattern.compile(
            "(<miscellaneous-field name=\"source-file\">)(.*)(</miscellaneous-field>)");

    /** The folder holding input/reference pairs. */
    private static Path regressionFolder;

    /**
     * Folder where Audiveris must run from. In development mode (not running from a jar),
     * WellKnowns.RES_URI is the relative path "res", so the process must be launched from
     * the app sub-folder for music fonts and other resources to be found.
     */
    private static Path appFolder;

    //~ Tests //////////////////////////////////////////////////////////////////////////////////////

    //-------------//
    // locateRepo //
    //-------------//
    /**
     * Locate the needed folders once, before any test.
     */
    @BeforeClass
    public static void locateRepo ()
    {
        final Path repoRoot = findRepoRoot();
        regressionFolder = repoRoot.resolve("data/regression");
        appFolder = findAppFolder(repoRoot);
    }

    //-----------------------------//
    // testAllReferenceConversions //
    //-----------------------------//
    /**
     * Convert every input file of the reference folder and compare the result against
     * its reference MusicXML output.
     */
    @Test
    public void testAllReferenceConversions ()
    {
        System.out.println("\n+++ testAllReferenceConversions in " + regressionFolder);

        final List<Path> inputs = listInputFiles();

        if (inputs.isEmpty()) {
            fail("No input file (png/jpg/jpeg/pdf) found in " + regressionFolder
                    + ". Populate it with input/reference pairs, e.g. file1.png + file1.xml");
        }

        final List<String> failures = new ArrayList<>();
        int okCount = 0;

        for (Path input : inputs) {
            final String fileName = input.getFileName().toString();
            final int dot = fileName.lastIndexOf('.');
            final String radix = fileName.substring(0, dot);
            final Path reference = input.resolveSibling(radix + ".xml");

            if (!Files.exists(reference) && !isUpdateRequested()) {
                failures.add(fileName + ": missing reference file " + reference);
                continue;
            }

            // Work on a copy, never touch the reference folder
            final Path tempDir;
            final Path inputCopy;

            try {
                tempDir = Files.createTempDirectory("xml-export-");
                inputCopy = Files.copy(input, tempDir.resolve(fileName));
            } catch (IOException ex) {
                failures.add(fileName + ": " + ex.getMessage());
                continue;
            }

            try {
                // Full batch pipeline on the copy, with outputs in the work folder
                runAudiveris(
                        tempDir.resolve("audiveris-run.log"),
                        TIMEOUT_SECONDS,
                        tempDir,
                        inputCopy.toAbsolutePath());

                final String producedXml = readScoreXml(findSingleExportFile(tempDir));

                if (isUpdateRequested()) {
                    updateReference(reference, producedXml);
                    okCount++;
                    deleteTree(tempDir);

                    continue;
                }

                final String expectedXml =
                        new String(Files.readAllBytes(reference), StandardCharsets.UTF_8);

                final List<String> expectedLines = normalize(expectedXml);
                final List<String> actualLines = normalize(producedXml);
                final String diff = diffOf(expectedLines, actualLines);

                if (diff == null) {
                    System.out.println(fileName + ": matches reference ("
                            + expectedLines.size() + " normalized lines)");
                    okCount++;
                    deleteTree(tempDir);
                } else {
                    // Keep the whole temp folder for inspection
                    final Path kept = tempDir.resolve("produced-normalized.xml");
                    Files.write(
                            kept,
                            String.join("\n", actualLines).getBytes(StandardCharsets.UTF_8));
                    failures.add(fileName + ":\n" + diff + "\nArtifacts kept in: " + tempDir);
                }
            } catch (Exception | AssertionError ex) {
                failures.add(fileName + ":\n" + ex.getMessage() + "\nArtifacts kept in: "
                        + tempDir);
            }
        }

        System.out.println(okCount + "/" + inputs.size() + " input file(s) converted and compared");

        if (!failures.isEmpty()) {
            fail("Reference comparison failed for " + failures.size() + " file(s):\n\n"
                    + String.join("\n\n---\n\n", failures));
        }
    }

    //~ Private methods ////////////////////////////////////////////////////////////////////////////

    //------------//
    // deleteTree //
    //------------//
    /**
     * Delete the provided folder and all its content.
     * <p>
     * A cleanup failure is just reported, it must not make a successful comparison fail.
     */
    private static void deleteTree (Path root)
    {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        } catch (IOException | UncheckedIOException ex) {
            System.out.println("Could not delete work folder " + root + ": " + ex.getMessage());
        }
    }

    //--------//
    // diffOf //
    //--------//
    /**
     * Compare two normalized line lists.
     *
     * @return null if they are identical, else a unified diff of the normalized lines.
     */
    private static String diffOf (List<String> expectedLines,
                                  List<String> actualLines)
    {
        final Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);

        if (patch.getDeltas().isEmpty()) {
            return null;
        }

        final List<String> unified = UnifiedDiffUtils.generateUnifiedDiff(
                "reference (normalized)",
                "produced (normalized)",
                expectedLines,
                patch,
                3);
        final StringBuilder sb = new StringBuilder();
        sb.append(patch.getDeltas().size()).append(" difference(s)\n");

        final int shown = Math.min(unified.size(), MAX_DIFF_LINES);
        sb.append(String.join("\n", unified.subList(0, shown))).append('\n');

        if (unified.size() > shown) {
            sb.append("... and ").append(unified.size() - shown).append(" more diff line(s)\n");
        }

        return sb.toString();
    }

    //---------------//
    // findAppFolder //
    //---------------//
    /**
     * Locate the app folder (the one containing the 'res' resources sub-folder).
     */
    private static Path findAppFolder (Path repoRoot)
    {
        final Path app = repoRoot.resolve("app");

        if (Files.isDirectory(app.resolve("res"))) {
            return app;
        }

        throw new IllegalStateException("Could not locate the app/res folder from " + repoRoot);
    }

    //--------------//
    // findRepoRoot //
    //--------------//
    /**
     * Locate the repo root (the folder holding data and app/res), whether tests run
     * from the root or from the app sub-folder.
     */
    private static Path findRepoRoot ()
    {
        Path dir = Paths.get("").toAbsolutePath();

        while (dir != null) {
            if (Files.isDirectory(dir.resolve("data"))
                    && Files.isDirectory(dir.resolve("app").resolve("res"))) {
                return dir;
            }

            dir = dir.getParent();
        }

        throw new IllegalStateException("Could not locate data from "
                + Paths.get("").toAbsolutePath());
    }

    //----------------------//
    // findSingleExportFile //
    //----------------------//
    /**
     * Locate the unique MusicXML export file produced in the work folder.
     */
    private static Path findSingleExportFile (Path workDir)
    {
        final List<Path> candidates = new ArrayList<>();

        try (Stream<Path> stream = Files.list(workDir)) {
            stream.filter(Files::isRegularFile) //
                    .filter(XmlExportRegressionTest::isExportFile) //
                    .sorted() //
                    .forEach(candidates::add);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        if (candidates.size() != 1) {
            fail("Expected exactly one export file in " + workDir + " but found: " + candidates);
        }

        return candidates.get(0);
    }

    //------------//
    // getJvmArgs //
    //------------//
    /**
     * Report the JVM options for the Audiveris process: the application ones as provided
     * by Gradle, or a default set when not run by Gradle.
     */
    private static List<String> getJvmArgs ()
    {
        final String prop = System.getProperty(JVM_ARGS_PROPERTY);

        if ((prop == null) || prop.isBlank()) {
            return DEFAULT_JVM_ARGS;
        }

        final List<String> args = new ArrayList<>();

        for (String arg : prop.split(",")) {
            if (!arg.isBlank()) {
                args.add(arg.trim());
            }
        }

        return args;
    }

    //--------------//
    // isExportFile //
    //--------------//
    private static boolean isExportFile (Path file)
    {
        final String name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".mxl") || name.endsWith(".xml");
    }

    //-------------//
    // isInputFile //
    //-------------//
    private static boolean isInputFile (Path file)
    {
        final String name = file.getFileName().toString().toLowerCase();
        final int dot = name.lastIndexOf('.');

        return (dot > 0) && INPUT_EXTENSIONS.contains(name.substring(dot + 1));
    }

    //-------------------//
    // isUpdateRequested //
    //-------------------//
    /**
     * When the environment variable AUDIVERIS_UPDATE_BASELINE is set to "true" or "1",
     * the produced MusicXML becomes the new reference for each input file.
     */
    private static boolean isUpdateRequested ()
    {
        final String value = System.getenv("AUDIVERIS_UPDATE_BASELINE");

        if (value == null) {
            return false;
        }

        final String val = value.trim();

        return val.equalsIgnoreCase("true") || val.equals("1");
    }

    //----------------//
    // listInputFiles //
    //----------------//
    /**
     * Report all input image files of the reference folder, sorted by name.
     */
    private static List<Path> listInputFiles ()
    {
        final List<Path> inputs = new ArrayList<>();

        if (!Files.isDirectory(regressionFolder)) {
            return inputs;
        }

        try (Stream<Path> stream = Files.list(regressionFolder)) {
            stream.filter(Files::isRegularFile) //
                    .filter(XmlExportRegressionTest::isInputFile) //
                    .sorted() //
                    .forEach(inputs::add);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return inputs;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Split the XML text into lines and drop or rewrite the volatile fields.
     */
    private static List<String> normalize (String xml)
    {
        final List<String> lines = new ArrayList<>();

        for (String raw : xml.split("\r?\n")) {
            String line = raw.trim(); // Line endings and trailing blanks are not meaningful

            if (line.isEmpty()) {
                continue;
            }

            // Drop the export date and the software version, they cannot be reproduced
            if (ENCODING_DATE.matcher(line).matches() || SOFTWARE.matcher(line).matches()) {
                continue;
            }

            // Reduce absolute input paths to their bare file name
            line = reduceToBaseName(SOURCE_LINE, line);
            line = reduceToBaseName(SOURCE_FILE_LINE, line);

            line = FONT_ATTRIBUTES.matcher(line).replaceAll("");

            lines.add(line);
        }

        return lines;
    }

    //---------------//
    // readRootEntry //
    //---------------//
    /**
     * Report the name of the score entry within a .mxl archive, as declared by the
     * rootfile of META-INF/container.xml.
     *
     * @return the declared entry name, or null if no usable container is found
     */
    private static String readRootEntry (ZipFile zip)
    {
        final ZipEntry container = zip.getEntry("META-INF/container.xml");

        if (container == null) {
            return null;
        }

        try (InputStream is = zip.getInputStream(container)) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            final NodeList rootFiles = factory.newDocumentBuilder().parse(is)
                    .getElementsByTagName("rootfile");

            if (rootFiles.getLength() == 0) {
                return null;
            }

            final String fullPath = ((Element) rootFiles.item(0)).getAttribute("full-path");

            return fullPath.isEmpty() ? null : fullPath;
        } catch (Exception ex) {
            System.out.println("Could not read container of " + zip.getName() + ": " + ex);

            return null;
        }
    }

    //--------------//
    // readScoreXml //
    //--------------//
    /**
     * Read score XML from either a plain .xml file or a compressed .mxl archive.
     * <p>
     * In a .mxl archive, the score entry is the one declared by META-INF/container.xml,
     * with a fallback on the first .xml entry outside META-INF.
     */
    private static String readScoreXml (Path exportPath)
        throws IOException
    {
        if (exportPath.getFileName().toString().toLowerCase().endsWith(".xml")) {
            return new String(Files.readAllBytes(exportPath), StandardCharsets.UTF_8);
        }

        try (ZipFile zip = new ZipFile(exportPath.toFile())) {
            final String rootName = readRootEntry(zip);
            ZipEntry entry = (rootName != null) ? zip.getEntry(rootName) : null;

            if (entry == null) {
                entry = Collections.list(zip.entries()) //
                        .stream() //
                        .filter(e -> e.getName().toLowerCase().endsWith(".xml") && !e.getName()
                                .startsWith("META-INF")) //
                        .findFirst() //
                        .orElse(null);
            }

            assertNotNull("No XML entry in " + exportPath, entry);

            try (InputStream is = zip.getInputStream(entry)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    //------------------//
    // reduceToBaseName //
    //------------------//
    private static String reduceToBaseName (Pattern pattern,
                                            String line)
    {
        final Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            return line;
        }

        final String path = matcher.group(2);
        final int cut = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

        return matcher.group(1) + ((cut >= 0) ? path.substring(cut + 1) : path) + matcher.group(3);
    }

    //--------------//
    // runAudiveris //
    //--------------//
    /**
     * Launch Audiveris in batch, in a separate JVM using the very class path of the test JVM
     * and the application JVM options.
     * <p>
     * The process runs from the app folder (so that relative resources resolve), writes its
     * outputs to the provided output folder (regardless of the user configuration) and
     * its console output is captured in the provided log file.
     */
    private static void runAudiveris (Path logFile,
                                      long timeoutSecs,
                                      Path outputFolder,
                                      Path input)
        throws IOException, InterruptedException
    {
        final List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(getJvmArgs());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("org.audiveris.omr.Main");
        Collections.addAll(command, "-batch", "-transcribe", "-export");
        Collections.addAll(command, "-output", outputFolder.toAbsolutePath().toString());

        for (String constant : FORCED_CONSTANTS) {
            Collections.addAll(command, "-constant", constant);
        }

        command.add(input.toString());

        System.out.println("Running: " + String.join(" ", command));

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(appFolder.toFile());
        builder.redirectErrorStream(true);

        builder.redirectOutput(logFile.toFile());

        final Process process = builder.start();

        if (!process.waitFor(timeoutSecs, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Audiveris did not complete within " + timeoutSecs + " seconds");
        }

        final int exitCode = process.exitValue();

        if (exitCode != 0) {
            final String log = Files.exists(logFile)
                    ? new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8)
                    : "<no log>";

            fail("Audiveris exited with code " + exitCode + "\n--- last log lines ---\n"
                    + tailOf(log, 40));
        }
    }

    //--------//
    // tailOf //
    //--------//
    private static String tailOf (String text,
                                  int maxLines)
    {
        final String[] lines = text.split("\r?\n");
        final int from = Math.max(0, lines.length - maxLines);

        return String.join("\n", Arrays.copyOfRange(lines, from, lines.length));
    }

    //-----------------//
    // updateReference //
    //-----------------//
    /**
     * Make the produced XML the new reference, after a backup of the previous reference
     * (always refreshed, so that it holds the reference just before this update).
     */
    private static void updateReference (Path reference,
                                         String producedXml)
        throws IOException
    {
        if (Files.exists(reference)) {
            final Path backup = reference.resolveSibling(
                    reference.getFileName().toString() + ".orig");
            Files.copy(reference, backup, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.write(reference, producedXml.getBytes(StandardCharsets.UTF_8));
        System.out.println("Reference updated: " + reference);
    }
}
