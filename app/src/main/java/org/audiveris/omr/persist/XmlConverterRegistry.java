//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           X m l C o n v e r t e r R e g i s t r y                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2027. All rights reserved.
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
package org.audiveris.omr.persist;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import org.audiveris.omr.persist.converters.BookConverter;
import org.audiveris.omr.persist.converters.SheetStubConverter;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central registry for XML serialization using XStream.
 * <p>
 * NEW: Path 2 refactor — replaces JAXB for Book/Sheet persistence.
 * Each persistent entity gets a custom XStream Converter registered here.
 *
 * @author Audiveris
 */
public class XmlConverterRegistry
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final XStream xstream = new XStream(new StaxDriver());

    static {
        xstream.setMode(XStream.NO_REFERENCES); // We handle cycles manually

        // Register custom converters
        xstream.registerConverter(new BookConverter());
        xstream.registerConverter(new SheetStubConverter());

        // Define aliases matching legacy JAXB root element names
        xstream.alias("book", Book.class);
        xstream.alias("sheet", Sheet.class);
        xstream.alias("stub", SheetStub.class);

        // Omit circular-reference fields (Sheet -> stub contains back-ref to Book)
        xstream.omitField(Sheet.class, "stub");
    }

    //~ Constructors -------------------------------------------------------------------------------

    private XmlConverterRegistry ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Serialize an object to XML and write to an OutputStream.
     */
    public static void toXML (Object obj,
                               OutputStream out)
    {
        xstream.toXML(obj, out);
    }

    /**
     * Deserialize XML from an InputStream into an object.
     */
    public static Object fromXML (InputStream in)
    {
        return xstream.fromXML(in);
    }

    /**
     * Convenience: store a Book to a file path.
     */
    public static void storeBook (Book book,
                                   Path filePath)
        throws Exception
    {
        try (OutputStream out = Files.newOutputStream(filePath)) {
            toXML(book, out);
        }
    }

    /**
     * Convenience: load a Book from a file path.
     */
    public static Book loadBook (Path filePath)
        throws Exception
    {
        try (InputStream in = Files.newInputStream(filePath)) {
            return (Book) fromXML(in);
        }
    }

    /**
     * Expose the underlying XStream for advanced configuration.
     */
    public static XStream getXStream ()
    {
        return xstream;
    }
}
