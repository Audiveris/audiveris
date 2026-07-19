//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k C o n v e r t e r                                //
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
package org.audiveris.omr.persist.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * XStream Converter for the Book class.
 * <p>
 * NEW: Path 2 refactor — mirrors the JAXB-produced XML structure:
 * <pre>
 * &lt;book software-version="..." software-build="..." path="..." dirty="..."&gt;
 *   &lt;sheet number="1" ...&gt; ... &lt;/sheet&gt;
 *   &lt;score ...&gt; ... &lt;/score&gt;
 * &lt;/book&gt;
 * </pre>
 * Uses only existing Book API (getVersionValue, getInputPath, getStubs, etc.).
 *
 * @author Audiveris
 */
public class BookConverter
        implements Converter
{
    @Override
    public boolean canConvert (Class type)
    {
        return Book.class.isAssignableFrom(type);
    }

    @Override
    public void marshal (Object source,
                          HierarchicalStreamWriter writer,
                          MarshallingContext context)
    {
        Book book = (Book) source;

        // -- Attributes: use existing public API --
        String version = book.getVersionValue();
        if (version != null) {
            writer.addAttribute("software-version", version);
        }
        // build is not exposed via getter; skip (JAXB handled it via field annotation)
        Path inputPath = book.getInputPath();
        if (inputPath != null) {
            writer.addAttribute("path", inputPath.toString());
        }
        if (book.isDirty()) {
            writer.addAttribute("dirty", "true");
        }

        // -- Child elements: SheetStubs (delegated to XStream reflection) --
        for (SheetStub stub : book.getStubs()) {
            context.convertAnother(stub);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object unmarshal (HierarchicalStreamReader reader,
                              UnmarshallingContext context)
    {
        // Book() is private, but XStream's PureJavaReflectionProvider
        // handles instantiation via context.
        Book book = (Book) context.convertAnother(
                null, Book.class);

        // -- Attributes from XML --
        String ver = reader.getAttribute("software-version");
        if (ver != null) {
            book.setVersionValue(ver);
        }
        // build: private field, not set here — will be initialized in initTransients
        String pathAttr = reader.getAttribute("path");
        // path cannot be set after construction; it's stored in JAXB @XmlAttribute
        // and loaded via unmarshal into field directly. For XStream we skip path
        // and rely on the caller to set bookPath via setBookPath().
        String dirtyAttr = reader.getAttribute("dirty");
        if ("true".equals(dirtyAttr)) {
            book.setDirty(true);
        }

        // -- Child elements --
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            final String nodeName = reader.getNodeName();

            if ("sheet".equals(nodeName)) {
                SheetStub stub = (SheetStub) context.convertAnother(
                        book, SheetStub.class);
                book.addStub(stub);
            } else if ("sheets-selection".equals(nodeName)) {
                String val = reader.getValue();
                if (val != null && !val.trim().isEmpty()) {
                    book.setSheetsSelection(val.trim());
                }
            } else if ("parameters".equals(nodeName) || "score".equals(nodeName)) {
                // Skip — handled by initParameters() or re-computed
            }
            reader.moveUp();
        }

        return book;
    }
}
