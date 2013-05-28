//----------------------------------------------------------------------------//
//                                                                            //
//                  M u s i c D i f f e r e n c e L i s t e n e r             //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.NodeDetail;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A specific DifferenceListener meant to filter out irrelevant
 * MusicXML differences.
 *
 * @author Hervé Bitteur
 */
public class MusicDifferenceListener
        implements DifferenceListener
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying node & difference filter, if any. */
    private final Filter filter;

    /** Output printer. */
    private final Printer output;

    /** To assign an ID to each difference. */
    int diffId = 0;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------------------//
    // MusicDifferenceListener //
    //-------------------------//
    /**
     * Creates a new MusicDifferenceListener object.
     *
     * @param filter the filtering instance to use, or null
     * @param output the output printer
     */
    public MusicDifferenceListener (Filter filter,
                                    Printer output)
    {
        this.filter = filter;
        this.output = output;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------------//
    // differenceFound //
    //-----------------//
    /**
     * Report how to qualify the provided difference
     *
     * @param difference the difference to inspect
     * @return the difference qualification
     */
    @Override
    public int differenceFound (Difference difference)
    {

        NodeDetail controlDetail = difference.getControlNodeDetail();
        String controlValue = controlDetail.getValue();
        Node controlNode = controlDetail.getNode();
        NodeDetail testDetail = difference.getTestNodeDetail();
        String testValue = testDetail.getValue();

        if (controlNode != null) {
            short controlType = controlNode.getNodeType();

            switch (controlType) {
            case Node.ELEMENT_NODE: {
                // Check irrelevant missing attribute
                if (difference.getDescription()
                        .equals("attribute name")) {
                    Element elem = (Element) controlNode;
                    String attrName = controlValue.equals("null") ? testValue
                            : controlValue;

                    if (filter != null && filter.canIgnore(elem, attrName)) {
                        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                    }
                }

                break;
            }

            case Node.ATTRIBUTE_NODE: {
                Attr attr = (Attr) controlNode;
                Element elem = attr.getOwnerElement();

                // Irrelevant attribute?
                if (filter != null && filter.canIgnore(elem, attr)) {
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }

                // Tolerance on attribute value?
                if (filter != null && filter.canTolerate(
                        elem,
                        attr,
                        controlValue,
                        testValue)) {
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }

                break;
            }

            case Node.TEXT_NODE: {
                Node controlParent = controlNode.getParentNode();

                if (controlParent != null) {
                    if (filter != null && filter.canTolerate(
                            controlParent,
                            controlValue,
                            testValue)) {
                        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                    }

                    break;
                }
            }
            default:
            }
        } else {
            ///output.dump(--diffId, difference);
            Node testNode = testDetail.getNode();
            if (filter != null && filter.canIgnore(testNode)) {
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            }
        }

        // Default
        return RETURN_ACCEPT_DIFFERENCE;
    }

    @Override
    public void skippedComparison (Node control,
                                   Node test)
    {
    }
}
