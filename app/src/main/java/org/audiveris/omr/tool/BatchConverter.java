//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B a t c h C o n v e r t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.tool;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI tool to batch-convert OMR projects between ZIP (.omr) and directory format.
 * <p>
 * Usage:
 *   BatchConverter --mode directory|zip [--input &lt;path&gt;] [--output &lt;path&gt;] [--recursive]
 */
public class BatchConverter
{
    public static void main (String[] args)
            throws Exception
    {
        // ---- Parse arguments ----
        String mode = null;
        Path inputPath = Paths.get("").toAbsolutePath();
        Path outputPath = null;
        boolean recursive = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--mode":
                mode = args[++i];
                break;
            case "--input":
                inputPath = Paths.get(args[++i]);
                break;
            case "--output":
                outputPath = Paths.get(args[++i]);
                break;
            case "--recursive":
                recursive = true;
                break;
            default:
                System.err.println("Unknown: " + args[i]);
                printUsage();
                System.exit(1);
            }
        }

        if (mode == null || (!"directory".equals(mode) && !"zip".equals(mode))) {
            System.err.println("Mode must be 'directory' or 'zip'.");
            printUsage();
            System.exit(1);
        }
        if (outputPath == null) {
            outputPath = inputPath;
        }

        // ---- Collect sources ----
        final String fMode = mode;
        List<Path> sources = new ArrayList<>();
        if (Files.isDirectory(inputPath)) {
            if (recursive) {
                Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
                        if (isTarget(file, fMode)) {
                            sources.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) {
                        if (isTarget(dir, fMode)) {
                            sources.add(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(inputPath)) {
                    for (Path p : ds) {
                        if (isTarget(p, fMode)) {
                            sources.add(p);
                        }
                    }
                }
            }
        } else {
            if (isTarget(inputPath, fMode)) {
                sources.add(inputPath);
            } else {
                System.err.println("Input is not a valid source for mode " + fMode);
                System.exit(1);
            }
        }

        if (sources.isEmpty()) {
            System.out.println("No files to convert.");
            return;
        }

        // ---- Process each source ----
        boolean ok = true;
        for (Path src : sources) {
            try {
                convertOne(src, mode, outputPath);
                System.out.println("OK: " + src);
            } catch (Exception ex) {
                System.err.println("FAIL: " + src + " - " + ex.getMessage());
                ok = false;
            }
        }
        System.exit(ok ? 0 : 1);
    }

    private static boolean isTarget (Path path, String mode)
    {
        if ("directory".equals(mode)) {
            // source for directory-conversion is a .omr/.mxl file
            return Files.isRegularFile(path)
                    && (path.getFileName().toString().toLowerCase().endsWith(".omr")
                    || path.getFileName().toString().toLowerCase().endsWith(".mxl"));
        } else {
            // source for zip-conversion is a directory containing book.xml
            return Files.isDirectory(path) && Files.exists(path.resolve("book.xml"));
        }
    }

    private static void convertOne (Path source, String mode, Path outputBase)
            throws Exception
    {
        // Determine output path
        String name = source.getFileName().toString();
        Path output;
        if ("directory".equals(mode)) {
            String dirName = name.replaceAll("\\.omr$|\\.mxl$", "");
            output = outputBase.resolve(dirName);
        } else {
            output = outputBase.resolve(name + ".omr");
        }

        if (Files.exists(output)) {
            System.err.println("Output exists, skipping: " + output);
            return;
        }

        // Load book (works for both ZIP and directory, thanks to Path1/Path2)
        OMR.engine = BookManager.getInstance();
        Book book;
        if (Files.isDirectory(source)) {
            book = BookManager.getInstance().loadBook(source);
        } else {
            book = BookManager.getInstance().loadBook(source);
        }

        if (book == null) {
            throw new IOException("Failed to load: " + source);
        }

        // Set target format via system property (overrides audiveris.properties)
        System.setProperty("omr.storage.directory",
                "directory".equals(mode) ? "true" : "false");

        // Save in target format
        book.store(output, false);
        book.close(null);
        System.out.println("Converted: " + source + " -> " + output);
    }

    private static void printUsage ()
    {
        System.out.println("Usage: BatchConverter --mode directory|zip"
                + " [--input <path>] [--output <path>] [--recursive]");
    }
}
