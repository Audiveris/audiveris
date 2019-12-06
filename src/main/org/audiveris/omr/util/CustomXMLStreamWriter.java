//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C u s t o m X M L S t r e a m W r i t e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Class {@code CustomXMLStreamWriter} handles indentation for XML output.
 * <p>
 * It handles indentation correctly (without JAXB default indentation limited to 8 steps).
 * <p>
 * Any element detected as empty (that is element with no content) is written as one self-closing
 * empty element rather than the pair of start + end tags.
 *
 * @author Kohsuke Kawaguchi (the author of the internal Sun implementation of class
 * IndentingXMLStreamWriter in com.sun.xml.internal.txw2.output package, this class was
 * initially derived from)
 *
 * @author Luca Basso Ricci
 * @see <a href="https://stackoverflow.com/a/27158805">Luca article</a>
 *
 * @author Hervé Bitteur (buffering of every element item as a Callable)
 */
public class CustomXMLStreamWriter
        implements XMLStreamWriter
{

    /** The actual writer, to which any real work is delegated. */
    protected final XMLStreamWriter writer;

    /** The indentation amount for one step. If null, no indentation is performed at all. */
    protected final String indentStep;

    /** Current level of element indentation. */
    protected int level;

    /** Are we closing element(s)?. */
    protected boolean closing;

    /**
     * Pending element processing with related items, such as attributes.
     * <ol>
     * <li>At index 0: the 'writeEmptyElement'
     * <li>At index 1: the 'writeStartElement'
     * <li>At following indices, depending on order of arrival, we can find:
     * <ul>
     * <li>'writeAttribute'
     * <li>'writeNameSpace'
     * <li>'setPrefix'
     * <li>'setDefaultNameSpace'
     * </ul>
     * </ol>
     */
    protected List<Item> items = new ArrayList<>();

    /**
     * Creates a new {@code IndentingXmlStreamWriter} object with default indent step of
     * 2 spaces.
     *
     * @param writer the underlying writer
     */
    public CustomXMLStreamWriter (XMLStreamWriter writer)
    {
        this(writer, "  ");
    }

    /**
     * Creates a new {@code IndentingXmlStreamWriter} object, with the specified indent
     * step value.
     *
     * @param writer     the underlying writer
     * @param indentStep the indentation string for one step. If null, no indentation is performed.
     */
    public CustomXMLStreamWriter (XMLStreamWriter writer,
                                  final String indentStep)
    {
        this.writer = writer;
        this.indentStep = indentStep;
    }

    @Override
    public void close ()
            throws XMLStreamException
    {
        flushItems();
        writer.close();
    }

    @Override
    public void flush ()
            throws XMLStreamException
    {
        if (items.isEmpty()) {
            writer.flush();
        }
    }

    @Override
    public NamespaceContext getNamespaceContext ()
    {
        return writer.getNamespaceContext();
    }

    @Override
    public void setNamespaceContext (NamespaceContext context)
            throws XMLStreamException
    {
        writer.setNamespaceContext(context);
    }

    @Override
    public String getPrefix (final String uri)
            throws XMLStreamException
    {
        return writer.getPrefix(uri);
    }

    @Override
    public Object getProperty (final String name)
            throws IllegalArgumentException
    {
        return writer.getProperty(name);
    }

    @Override
    public void setDefaultNamespace (final String uri)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.setDefaultNamespace(uri);
                    return null;
                }
            });
        } else {
            writer.setDefaultNamespace(uri);
        }
    }

    @Override
    public void setPrefix (final String prefix,
                           final String uri)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.setPrefix(prefix, uri);
                    return null;
                }
            });
        } else {
            writer.setPrefix(prefix, uri);
        }
    }

    @Override
    public void writeAttribute (final String localName,
                                final String value)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.writeAttribute(localName, value);
                    return null;
                }
            });
        } else {
            writer.writeAttribute(localName, value);
        }
    }

    @Override
    public void writeAttribute (final String prefix,
                                final String namespaceURI,
                                final String localName,
                                final String value)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.writeAttribute(prefix, namespaceURI, localName, value);
                    return null;
                }
            });
        } else {
            writer.writeAttribute(prefix, namespaceURI, localName, value);
        }
    }

    @Override
    public void writeAttribute (final String namespaceURI,
                                final String localName,
                                final String value)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.writeAttribute(namespaceURI, localName, value);
                    return null;
                }
            });
        } else {
            writer.writeAttribute(namespaceURI, localName, value);
        }

    }

    @Override
    public void writeCData (final String data)
            throws XMLStreamException
    {
        if (!data.isEmpty()) {
            flushItems();
            writer.writeCData(data);
        }
    }

    @Override
    public void writeCharacters (final String text)
            throws XMLStreamException
    {
        if (!text.isEmpty()) {
            flushItems();
            writer.writeCharacters(text);
        }
    }

    @Override
    public void writeCharacters (char[] text,
                                 int start,
                                 int len)
            throws XMLStreamException
    {
        if (len > 0) {
            flushItems();
            writer.writeCharacters(text, start, len);
        }
    }

    @Override
    public void writeComment (final String data)
            throws XMLStreamException
    {
        flushItems();
        indentComment();
        writer.writeComment(data);
    }

    @Override
    public void writeDTD (final String dtd)
            throws XMLStreamException
    {
        flushItems();
        writer.writeDTD(dtd);
    }

    @Override
    public void writeDefaultNamespace (final String namespaceURI)
            throws XMLStreamException
    {
        writer.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeEmptyElement (final String namespaceURI,
                                   final String localName)
            throws XMLStreamException
    {
        flushItems();
        writer.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement (final String prefix,
                                   final String localName,
                                   final String namespaceURI)
            throws XMLStreamException
    {
        flushItems();
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement (final String localName)
            throws XMLStreamException
    {
        flushItems();
        writer.writeEmptyElement(localName);
    }

    @Override
    public void writeEndDocument ()
            throws XMLStreamException
    {
        flushItems();
        writer.writeEndDocument();
    }

    @Override
    public void writeEndElement ()
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            try {
                // Here, element has no content (attributes don't count as content),
                // therfore we write empty element, instead of start + end
                items.get(0).call(); // At index 0 was saved the empty processing

                // Process the saved items
                for (Item item : items.subList(2, items.size())) {
                    item.call();
                }
            } catch (Exception ex) {
                throw new XMLStreamException(ex);
            }

            indentEnd();
            items.clear();
        } else {
            indentEnd();
            writer.writeEndElement();
        }
    }

    @Override
    public void writeEntityRef (final String name)
            throws XMLStreamException
    {
        flushItems();
        writer.writeEntityRef(name);
    }

    @Override
    public void writeNamespace (final String prefix,
                                final String namespaceURI)
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            items.add(new Item()
            {
                public Void call ()
                        throws XMLStreamException
                {
                    writer.writeNamespace(prefix, namespaceURI);
                    return null;
                }
            });
        } else {
            writer.writeNamespace(prefix, namespaceURI);
        }
    }

    @Override
    public void writeProcessingInstruction (final String target)
            throws XMLStreamException
    {
        flushItems();
        writer.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction (final String target,
                                            final String data)
            throws XMLStreamException
    {
        flushItems();
        writer.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeStartDocument ()
            throws XMLStreamException
    {
        writer.writeStartDocument();
    }

    @Override
    public void writeStartDocument (final String version)
            throws XMLStreamException
    {
        writer.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument (final String encoding,
                                    final String version)
            throws XMLStreamException
    {
        writer.writeStartDocument(encoding, version);
    }

    @Override
    public void writeStartElement (final String localName)
            throws XMLStreamException
    {
        flushItems();
        indentStart(localName);

        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeEmptyElement(localName); // Empty saved first
                return null;
            }
        });
        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeStartElement(localName); // Start saved second
                return null;
            }
        });
    }

    @Override
    public void writeStartElement (final String namespaceURI,
                                   final String localName)
            throws XMLStreamException
    {
        flushItems();
        indentStart(localName);

        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeEmptyElement(namespaceURI, localName); // Empty saved first
                return null;
            }
        });
        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeStartElement(namespaceURI, localName); // Start saved second
                return null;
            }
        });
    }

    @Override
    public void writeStartElement (final String prefix,
                                   final String localName,
                                   final String namespaceURI)
            throws XMLStreamException
    {
        flushItems();
        indentStart(localName);

        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeEmptyElement(prefix, localName, namespaceURI); // Empty saved first
                return null;
            }
        });
        items.add(new Item()
        {
            public Void call ()
                    throws XMLStreamException
            {
                writer.writeStartElement(prefix, localName, namespaceURI); // Start saved second
                return null;
            }
        });
    }

    //------------//
    // flushItems //
    //------------//
    /**
     * We finish the saving of current element if any, by flushing the saved items.
     *
     * @throws XMLStreamException
     */
    protected void flushItems ()
            throws XMLStreamException
    {
        if (!items.isEmpty()) {
            try {
                // Write 'start' element
                items.get(1).call(); // At index 1 was saved the 'start' processing

                // Process the saved items
                for (Item item : items.subList(2, items.size())) {
                    item.call();
                }
            } catch (Exception ex) {
                throw new XMLStreamException(ex);
            }

            items.clear();
        }
    }

    //----------//
    // doIndent //
    //----------//
    /**
     * Insert a new line, followed by proper level of indentation.
     *
     * @throws XMLStreamException
     */
    protected void doIndent ()
            throws XMLStreamException
    {
        if (indentStep != null) {
            writer.writeCharacters("\n");

            for (int i = 0; i < level; i++) {
                writer.writeCharacters(indentStep);
            }
        }
    }

    //---------------//
    // indentComment //
    //---------------//
    /**
     * Indentation before comment. Always indent.
     *
     * @throws XMLStreamException
     */
    protected void indentComment ()
            throws XMLStreamException
    {
        if (indentStep != null) {
            doIndent();
        }
    }

    //-----------//
    // indentEnd //
    //-----------//
    /**
     * Indentation before end tag. Indent except on first close.
     *
     * @throws XMLStreamException
     */
    protected void indentEnd ()
            throws XMLStreamException
    {
        if (indentStep != null) {
            level--;

            if (closing) {
                doIndent();
            }

            closing = true;
        }
    }

    //-------------//
    // indentStart //
    //-------------//
    /**
     * Indentation before start tag. Always indent.
     *
     * @param localName the local tag name.
     *                  It can be used by an overriding implementation to decide to include
     *                  on-the-fly any material such as a specific comment.
     * @throws XMLStreamException
     */
    protected void indentStart (final String localName)
            throws XMLStreamException
    {
        if (indentStep != null) {
            doIndent();
            level++;
            closing = false;
        }
    }

    //------//
    // Item //
    //------//
    /**
     * Class meant to save the processing of an item (such as attribute) related to the
     * current element.
     */
    protected abstract static class Item
            implements Callable<Void>
    {
    }
}
