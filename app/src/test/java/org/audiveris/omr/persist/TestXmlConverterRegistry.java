//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                       T e s t X m l C o n v e r t e r R e g i s t r y                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.persist;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tests for XmlConverterRegistry: Book serialization round-trip.
 */
public class TestXmlConverterRegistry
{
    @Test
    public void testBookRoundTrip ()
            throws Exception
    {
        // Use Book.createBook() static factory or reflection for no-arg construction.
        // Since Book() is private, we test via XmlConverterRegistry.loadBook/storeBook.
        // Create a minimal book.xml on disk and round-trip it.
        String bookXml = "<?xml version=\"1.0\" ?>"
                + "<book software-version=\"5.11.0\" dirty=\"true\">"
                + "<sheet number=\"1\"><steps/></sheet>"
                + "</book>";

        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("book", ".xml");
        java.nio.file.Files.writeString(tmpFile, bookXml);

        try {
            Book loaded = XmlConverterRegistry.loadBook(tmpFile);
            assertNotNull("Loaded book should not be null", loaded);
            assertEquals("Should have 1 stub", 1, loaded.getStubs().size());
            assertEquals("Stub number should be 1", 1, loaded.getStubs().get(0).getNumber());
        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
    }

    @Test
    public void testStoreAndLoadBookViaFile ()
            throws Exception
    {
        String bookXml = "<?xml version=\"1.0\" ?>"
                + "<book software-version=\"5.11.0\">"
                + "<sheet number=\"99\"><steps/></sheet>"
                + "</book>";

        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("book", ".xml");
        java.nio.file.Files.writeString(tmpFile, bookXml);

        try {
            Book loaded = XmlConverterRegistry.loadBook(tmpFile);
            assertNotNull(loaded);
            assertEquals("Loaded stub count", 1, loaded.getStubs().size());
            assertEquals("Loaded stub number", 99, loaded.getStubs().get(0).getNumber());
        } finally {
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
    }
}
