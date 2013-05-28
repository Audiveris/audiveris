//----------------------------------------------------------------------------//
//                                                                            //
//                                F i l t e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.custommonkey.xmlunit.Difference;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * General specification of a filter for XML comparison.
 *
 * @author Hervé Bitteur
 */
public interface Filter
{

    /**
     * Check whether the provided difference can be safely ignored,
     * since it either duplicates another one or the nodes involved
     * are not relevant.
     *
     * @param difference the provided difference
     * @return true if not relevant
     */
    boolean canIgnore (Difference difference);

    /**
     * Check whether the provided node can be ignored.
     *
     * @param node the provided node
     * @return true if the node can be ignored in comparison (with all its
     *         child nodes, if any)
     */
    boolean canIgnore (Node node);

    /**
     * Check whether the provided attribute can be ignored.
     *
     * @param elem the containing element
     * @param attr the provided attribute
     * @return true if not relevant
     */
    boolean canIgnore (Element elem,
                       Attr attr);

    /**
     * Check whether the provided attribute can be ignored.
     *
     * @param elem     the containing element
     * @param attrName the provided attribute name
     * @return true if not relevant
     */
    boolean canIgnore (Element elem,
                       String attrName);

    /**
     * Check whether the provided values can be considered equal.
     *
     * @param parent       the containing node
     * @param controlValue value for control element
     * @param testValue    value for test element
     * @return true if element difference is not significant
     */
    boolean canTolerate (Node parent,
                         String controlValue,
                         String testValue);

    /**
     * Check whether the provided attribute values can be considered
     * as equal.
     *
     * @param elem         the containing element
     * @param attr         the provided attribute
     * @param controlValue value for control attribute
     * @param testValue    value for test attribute
     * @return true if attribute difference is not significant
     */
    boolean canTolerate (Element elem,
                         Attr attr,
                         String controlValue,
                         String testValue);
}
