//----------------------------------------------------------------------------//
//                                                                            //
//                           M u s i c P r i n t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.NodeDetail;

import org.w3c.dom.Node;

import java.io.PrintStream;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * A Printer meant for MusicXML usage.
 *
 * @author Hervé Bitteur
 */
public class MusicPrinter
        implements Printer
{
    //~ Instance fields --------------------------------------------------------

    /** Actual output. */
    private final PrintStream out;

    //~ Constructors -----------------------------------------------------------
    //
    //--------------//
    // MusicPrinter //
    //--------------//
    /**
     * Creates a new MusicPrinter object.
     *
     * @param out the actual output stream
     */
    public MusicPrinter (PrintStream out)
    {
        this.out = out;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------//
    // dump //
    //------//
    @Override
    public void dump (int id,
                      Difference difference)
    {
        print(String.format("%4d ", id));
        print(difference.isRecoverable() ? "REC " : "    ");
        println(difference.toString());

        NodeDetail controlDetail = difference.getControlNodeDetail();
        Node controlNode = controlDetail.getNode();
        String controlValue = stringOf(controlNode);

        if (controlValue != null) {
            println("             ctrl"
                    + PositionalXMLReader.getRef(controlNode)
                    + " " + controlValue);
        }

        NodeDetail testDetail = difference.getTestNodeDetail();
        Node testNode = testDetail.getNode();
        String testValue = stringOf(testNode);

        if (testValue != null) {
            println("             test"
                    + PositionalXMLReader.getRef(testNode)
                    + " " + testValue);
        }
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print (Object obj)
    {
        out.print(obj);
    }

    //---------//
    // println //
    //---------//
    @Override
    public void println (Object obj)
    {
        out.println(obj);
    }

    //---------//
    // println //
    //---------//
    @Override
    public void println ()
    {
        out.println();
    }

    //----------//
    // stringOf //
    //----------//
    @Override
    public String stringOf (Node node)
    {
        if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
            StringWriter sw = new StringWriter();

            try {
                Transformer t = TransformerFactory.newInstance()
                        .newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                t.transform(new DOMSource(node), new StreamResult(sw));
            } catch (TransformerException te) {
                System.out.println("nodeToString Transformer Exception");
            }

            return sw.toString();
        }

        return null;
    }
}
