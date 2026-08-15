//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P r o j e c t V a l i d a t o r                              //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.tool;

import org.audiveris.omr.persist.XmlConverterRegistry;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.ZipFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Validates and optionally repairs an OMR project (ZIP or directory mode).
 */
public class ProjectValidator
{
    public ValidationReport validate (Path projectPath)
            throws IOException
    {
        ValidationReport report = new ValidationReport();

        if (!Files.exists(projectPath)) {
            report.error("Project path does not exist: " + projectPath);
            return report;
        }

        Path root;
        try {
            root = ZipFileSystem.open(projectPath);
        } catch (Exception e) {
            report.error("Cannot open project: " + e.getMessage());
            return report;
        }

        try {
            Path bookXml = root.resolve("book.xml");
            if (!Files.exists(bookXml)) {
                report.error("book.xml is missing");
            } else {
                try (InputStream in = Files.newInputStream(bookXml)) {
                    try {
                        Book book = (Book) XmlConverterRegistry.fromXML(in);
                        List<SheetStub> stubs = book.getStubs();
                        if (stubs == null || stubs.isEmpty()) {
                            report.warning("No sheets defined in book.xml");
                        } else {
                            for (SheetStub stub : stubs) {
                                String sheetFileName = "sheet#" + stub.getNumber() + ".xml";
                                Path sheetFile = root.resolve(sheetFileName);
                                if (!Files.exists(sheetFile)) {
                                    report.error("Missing sheet file: " + sheetFileName);
                                } else {
                                    try (InputStream sis = Files.newInputStream(sheetFile)) {
                                        XmlConverterRegistry.fromXML(sis);
                                    } catch (Exception e) {
                                        report.error("Corrupt sheet file " + sheetFileName + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        report.error("book.xml cannot be parsed: " + e.getMessage());
                    }
                }
            }

            // Additional image checks for directory mode
            if (root.equals(projectPath)) {
                // Directory mode: check for zero-size images
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
                        String name = file.getFileName().toString().toLowerCase();
                        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".tif")) {
                            if (attrs.size() == 0) {
                                report.warning("Zero-size image file: " + root.relativize(file));
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                report.info("ZIP mode: skipping image checks (packed).");
            }
        } finally {
            ZipFileSystem.closeRoot(root, projectPath);
        }

        return report;
    }

    /**
     * Attempt to repair a project by regenerating book.xml from existing sheet files.
     * Returns true if repair was performed.
     */
    public boolean repair (Path projectPath)
            throws IOException
    {
        Path root;
        try {
            root = ZipFileSystem.open(projectPath);
        } catch (Exception e) {
            return false;
        }

        try {
            Path bookXml = root.resolve("book.xml");
            if (Files.exists(bookXml)) {
                try (InputStream in = Files.newInputStream(bookXml)) {
                    XmlConverterRegistry.fromXML(in);
                    return false; // Already valid, no repair needed
                } catch (Exception ignored) {
                }
            }

            // Collect sheet files and reconstruct book
            // Use reflection to instantiate Book (constructor is private)
            Book book;
            try {
                java.lang.reflect.Constructor<Book> ctor =
                        Book.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                book = ctor.newInstance();
            } catch (Exception ex) {
                return false;
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "sheet#*.xml")) {
                for (Path sheetFile : ds) {
                    String fileName = sheetFile.getFileName().toString();
                    String numPart = fileName.replace("sheet#", "").replace(".xml", "");
                    try {
                        int number = Integer.parseInt(numPart);
                        // Use reflection to bypass private SheetStub() constructor
                        java.lang.reflect.Constructor<SheetStub> ctor =
                                SheetStub.class.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        SheetStub stub = ctor.newInstance();
                        java.lang.reflect.Field numField =
                                SheetStub.class.getDeclaredField("number");
                        numField.setAccessible(true);
                        numField.set(stub, number);
                        book.addStub(stub);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (book.getStubs() == null || book.getStubs().isEmpty()) {
                return false;
            }

            // Write repaired book.xml
            try {
                book.storeBookInfo(root);
            } catch (Exception ignored) {
                return false;
            }
            return true;
        } finally {
            try {
                ZipFileSystem.closeRoot(root, projectPath);
            } catch (Exception ignored) {
            }
        }
    }
}
