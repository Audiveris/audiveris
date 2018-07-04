//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         I n d e n t i n g X M L S t r e a m W r i t e r                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Class {@code IndentingXMLStreamWriter} handles indentation for XML output.
 * <p>
 * It is a workaround to avoid JAXB default limit to 8 steps.
 *
 * @author Kohsuke Kawaguchi (the author of the internal Sun implementation of class
 * IndentingXMLStreamWriter in com.sun.xml.internal.txw2.output package, this class is derived from)
 * @author Hervé Bitteur (using an underlying writer instance)
 */
public class IndentingXMLStreamWriter
        implements XMLStreamWriter
{
    //~ Enumerations -------------------------------------------------------------------------------

    /** What we have written in current element. */
    protected static enum Seen
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Neither data/text nor sub-element. */
        NOTHING,
        /** A (sub-) element. */
        ELEMENT,
        /** CData or characters. */
        DATA;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The actual writer, to which any real work is delegated. */
    protected final XMLStreamWriter writer;

    /** The indentation for one step. If null, no indentation is performed at all. */
    protected final String indentStep;

    /** Stack of states, parallel to elements being written. */
    protected final Deque<Seen> stateStack = new ArrayDeque<Seen>();

    /** Current state (in current element). */
    protected Seen state = Seen.NOTHING;

    /** Current stack depth. */
    protected int depth = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code IndentingXmlStreamWriter} object with default indent step of
     * 2 spaces.
     *
     * @param writer the underlying writer
     */
    public IndentingXMLStreamWriter (XMLStreamWriter writer)
    {
        this(writer, "  ");
    }

    /**
     * Creates a new {@code IndentingXmlStreamWriter} object.
     *
     * @param writer     the underlying writer
     * @param indentStep the indentation string for one step. If null, no indentation is performed.
     */
    public IndentingXMLStreamWriter (XMLStreamWriter writer,
                                     String indentStep)
    {
        this.writer = writer;
        this.indentStep = indentStep;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void close ()
            throws XMLStreamException
    {
        writer.close();
    }

    @Override
    public void flush ()
            throws XMLStreamException
    {
        writer.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext ()
    {
        return writer.getNamespaceContext();
    }

    @Override
    public String getPrefix (String uri)
            throws XMLStreamException
    {
        return writer.getPrefix(uri);
    }

    @Override
    public Object getProperty (String name)
            throws IllegalArgumentException
    {
        return writer.getProperty(name);
    }

    @Override
    public void setDefaultNamespace (String uri)
            throws XMLStreamException
    {
        writer.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext (NamespaceContext context)
            throws XMLStreamException
    {
        writer.setNamespaceContext(context);
    }

    @Override
    public void setPrefix (String prefix,
                           String uri)
            throws XMLStreamException
    {
        writer.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute (String localName,
                                String value)
            throws XMLStreamException
    {
        writer.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute (String prefix,
                                String namespaceURI,
                                String localName,
                                String value)
            throws XMLStreamException
    {
        writer.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute (String namespaceURI,
                                String localName,
                                String value)
            throws XMLStreamException
    {
        writer.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeCData (String data)
            throws XMLStreamException
    {
        state = Seen.DATA;
        writer.writeCData(data);
    }

    @Override
    public void writeCharacters (String text)
            throws XMLStreamException
    {
        state = Seen.DATA;
        writer.writeCharacters(text);
    }

    @Override
    public void writeCharacters (char[] text,
                                 int start,
                                 int len)
            throws XMLStreamException
    {
        state = Seen.DATA;
        writer.writeCharacters(text, start, len);
    }

    @Override
    public void writeComment (String data)
            throws XMLStreamException
    {
        writer.writeComment(data);
    }

    @Override
    public void writeDTD (String dtd)
            throws XMLStreamException
    {
        writer.writeDTD(dtd);
    }

    @Override
    public void writeDefaultNamespace (String namespaceURI)
            throws XMLStreamException
    {
        writer.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeEmptyElement (String namespaceURI,
                                   String localName)
            throws XMLStreamException
    {
        onEmptyElement();
        writer.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement (String prefix,
                                   String localName,
                                   String namespaceURI)
            throws XMLStreamException
    {
        onEmptyElement();
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement (String localName)
            throws XMLStreamException
    {
        onEmptyElement();
        writer.writeEmptyElement(localName);
    }

    @Override
    public void writeEndDocument ()
            throws XMLStreamException
    {
        writer.writeEndDocument();
    }

    @Override
    public void writeEndElement ()
            throws XMLStreamException
    {
        onEndElement();
        writer.writeEndElement();
    }

    @Override
    public void writeEntityRef (String name)
            throws XMLStreamException
    {
        writer.writeEntityRef(name);
    }

    @Override
    public void writeNamespace (String prefix,
                                String namespaceURI)
            throws XMLStreamException
    {
        writer.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeProcessingInstruction (String target)
            throws XMLStreamException
    {
        writer.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction (String target,
                                            String data)
            throws XMLStreamException
    {
        writer.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeStartDocument ()
            throws XMLStreamException
    {
        writer.writeStartDocument();

        if (indentStep != null) {
            doNewline();
        }
    }

    @Override
    public void writeStartDocument (String version)
            throws XMLStreamException
    {
        writer.writeStartDocument(version);

        if (indentStep != null) {
            doNewline();
        }
    }

    @Override
    public void writeStartDocument (String encoding,
                                    String version)
            throws XMLStreamException
    {
        writer.writeStartDocument(encoding, version);

        if (indentStep != null) {
            doNewline();
        }
    }

    @Override
    public void writeStartElement (String localName)
            throws XMLStreamException
    {
        onStartElement();
        writer.writeStartElement(localName);
    }

    @Override
    public void writeStartElement (String namespaceURI,
                                   String localName)
            throws XMLStreamException
    {
        onStartElement();
        writer.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement (String prefix,
                                   String localName,
                                   String namespaceURI)
            throws XMLStreamException
    {
        onStartElement();
        writer.writeStartElement(prefix, localName, namespaceURI);
    }

    /**
     * Print indentation for the current level.
     *
     * @throws XMLStreamException if there was an error processing the XML
     */
    protected void doIndent ()
            throws XMLStreamException
    {
        if (indentStep != null) {
            for (int i = 0; i < depth; i++) {
                writer.writeCharacters(indentStep);
            }
        }
    }

    /**
     * Insert a newline.
     *
     * @throws XMLStreamException if there was an error processing the XML
     */
    protected void doNewline ()
            throws XMLStreamException
    {
        writer.writeCharacters("\n");
    }

    /**
     * Call-back on writeEmptyElement()
     *
     * @throws XMLStreamException if there was an error processing the XML
     */
    protected void onEmptyElement ()
            throws XMLStreamException
    {
        state = Seen.ELEMENT;

        if ((indentStep != null) && (depth > 0)) {
            doNewline();
            doIndent();
        }
    }

    /**
     * Call-back on writeEndElement()
     *
     * @throws XMLStreamException if there was an error processing the XML
     */
    protected void onEndElement ()
            throws XMLStreamException
    {
        depth--;

        if ((indentStep != null) && (state == Seen.ELEMENT)) {
            doNewline();
            doIndent();
        }

        state = stateStack.removeFirst();
    }

    /**
     * Call-back on any writeStartElement()
     *
     * @throws XMLStreamException if there was an error processing the XML
     */
    protected void onStartElement ()
            throws XMLStreamException
    {
        stateStack.addFirst(Seen.ELEMENT);
        state = Seen.NOTHING;

        if ((indentStep != null) && (depth > 0)) {
            doNewline();
            doIndent();
        }

        depth++;
    }
}
