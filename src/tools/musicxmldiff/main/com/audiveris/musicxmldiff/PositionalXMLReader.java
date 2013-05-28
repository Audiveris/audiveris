//----------------------------------------------------------------------------//
//                                                                            //
//                     P o s i t i o n a l X M L R e a d e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Class {@code PositionalXMLReader} allows to read an XML stream
 * into a Document and later retrieve the source position that
 * corresponds to any Document node.
 *
 * @author Hervé Bitteur
 */
public class PositionalXMLReader
{
    //~ Static fields/initializers ---------------------------------------------

    /** User data key to record source line number. */
    private static final String KEY_LINE = "lineNumber";

    /** User data key to record source column number. */
    private static final String KEY_COLUMN = "columnNumber";

    /** Handler to copy user data from one node to the other. */
    private static UserDataHandler dataCopier = new UserDataHandler()
    {
        @Override
        public void handle (short operation,
                            String key,
                            Object data,
                            Node src,
                            Node dst)
        {
            if (dst != null) {
                dst.setUserData(key, data, this);
            }
        }
    };

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getColumn //
    //-----------//
    /**
     * Report the ending column where the provided node was read in
     * its source line.
     *
     * @param node the provided node
     * @return the column number, or null
     */
    public static String getColumn (Node node)
    {
        if (node == null) {
            return null;
        }

        return (String) node.getUserData(KEY_COLUMN);
    }

    //
    //---------//
    // getLine //
    //---------//
    /**
     * Report the source line where the provided node was read.
     *
     * @param node the provided node
     * @return the line number, or null
     */
    public static String getLine (Node node)
    {
        if (node == null) {
            return null;
        }

        return (String) node.getUserData(KEY_LINE);
    }

    //--------//
    // getRef //
    //--------//
    /**
     * Convenient method to report line + column information regarding
     * the source file of the provided node.
     *
     * @param node the provided node
     * @return a string formatted as: [line:123,col:45]
     */
    public static String getRef (Node node)
    {
        if (node == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");

        String line = getLine(node);

        if (line != null) {
            sb.append("line:")
                    .append(line);
        }

        String col = getColumn(node);

        if (col != null) {
            sb.append(",col:")
                    .append(col);
        }

        sb.append("]");

        return sb.toString();
    }

    //---------//
    // readXML //
    //---------//
    /**
     * Build a Document out of the provided XML stream, while keeping
     * source line and column informations as user data in the created
     * document.
     *
     * @param is the provided input stream
     * @return the created document instance
     * @throws IOException
     * @throws SAXException
     */
    public Document readXML (final InputStream is)
            throws IOException, SAXException
    {
        final Document doc;
        SAXParser parser;

        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();

            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);

            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(
                    "Can't create SAX parser / DOM builder.",
                    e);
        }

        final Stack<Element> elementStack = new Stack<>();
        final StringBuilder textBuffer = new StringBuilder();

        final DefaultHandler handler = new DefaultHandler()
        {
            private Locator locator;

            @Override
            public void setDocumentLocator (final Locator locator)
            {
                this.locator = locator;
            }

            @Override
            public void startElement (final String uri,
                                      final String localName,
                                      final String qName,
                                      final Attributes attributes)
                    throws SAXException
            {
                addTextIfNeeded();

                final Element el = doc.createElement(qName);

                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(
                            attributes.getQName(i),
                            attributes.getValue(i));
                }

                el.setUserData(
                        KEY_LINE,
                        String.valueOf(this.locator.getLineNumber()),
                        dataCopier);
                el.setUserData(
                        KEY_COLUMN,
                        String.valueOf(this.locator.getColumnNumber()),
                        dataCopier);

                elementStack.push(el);
            }

            @Override
            public void endElement (final String uri,
                                    final String localName,
                                    final String qName)
            {
                addTextIfNeeded();

                final Element closedEl = elementStack.pop();

                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    final Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters (final char[] ch,
                                    final int start,
                                    final int length)
                    throws SAXException
            {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded ()
            {
                if (textBuffer.length() > 0) {
                    final Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(
                            textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };

        Reader reader = new InputStreamReader(is, "UTF-8");
        InputSource source = new InputSource(reader);
        parser.parse(source, handler);

        return doc;
    }
}
