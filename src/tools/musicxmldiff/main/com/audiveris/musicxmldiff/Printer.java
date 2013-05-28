//----------------------------------------------------------------------------//
//                                                                            //
//                               P r i n t e r                                //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.custommonkey.xmlunit.Difference;
import org.w3c.dom.Node;

/**
 * Specification for Printer features.
 *
 * @author Hervé Bitteur
 */
public interface Printer
{

    /**
     * Print an object on the output stream.
     *
     * @param obj the object to print
     */
    void print (Object obj);

    /**
     * Print an object on the output stream, followed by a newline.
     *
     * @param obj the object to print
     */
    void println (Object obj);

    /**
     * Print a newline on the output stream.
     */
    void println ();

    /**
     * Report a detailed representation of a Node instance.
     * @param node the provided node
     * @return the description
     */
    String stringOf (Node node);

    /**
     * Dump the provided difference on the output stream.
     * @param id a label
     * @param difference the difference to dump 
     */
    void dump (int id,
               Difference difference);
}
