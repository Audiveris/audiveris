//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c F i l t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import com.audiveris.musicxmldiff.info.FilterInfo;

import org.custommonkey.xmlunit.Difference;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;

/**
 * Basic Filter implementation, customized by provided info file.
 *
 * @author Hervé Bitteur
 */
public class BasicFilter
        implements Filter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Special name meant to specify lack of element name. */
    private static final String NO_ELEM = "*";
    
    //~ Instance fields --------------------------------------------------------

    /** Filtering information. */
    private FilterInfo info;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicFilter object.
     *
     * @param filterStream input stream containing filter informations
     */
    public BasicFilter (InputStream filterStream)
    {
        try {
            info = FilterInfo.unmarshall(filterStream);
        } catch (JAXBException | FileNotFoundException ex) {
            System.err.println("Could not unmarshall filter stream " + ex);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------//
    // canIgnore //
    //-----------//
    @Override
    public boolean canIgnore (Difference difference)
    {
        // Check for redundancy
        if (info.canIgnoreDiff(difference.getDescription())) {
            return true;
        }

        if (canIgnore(difference.getControlNodeDetail().getNode())) {
            return true;
        }

        if (canIgnore(difference.getTestNodeDetail().getNode())) {
            return true;
        }

        return false;
    }

    //-----------//
    // canIgnore //
    //-----------//
    @Override
    public boolean canIgnore (Node node)
    {
        if (node == null) {
            return false;
        } else {
            String nodeName = node.getNodeName();

            if (info.canIgnoreElem(nodeName)) {
                return true;
            } else {
                Node parent = node.getParentNode();

                return canIgnore(parent);
            }
        }
    }

    //-----------//
    // canIgnore //
    //-----------//
    @Override
    public boolean canIgnore (Element elem,
                              Attr attr)
    {
        return canIgnore(elem, attr.getName());
    }

    //-----------//
    // canIgnore //
    //-----------//
    @Override
    public boolean canIgnore (Element elem,
                              String attrName)
    {
        // First check specific attribute
        Boolean bool = info.canIgnoreAttr(elem.getTagName(), attrName);
        if (bool != null) {
            return bool.booleanValue();
        }

        // Then fall back on global attributes
        bool = info.canIgnoreAttr(NO_ELEM, attrName);
        if (bool != null) {
            return bool.booleanValue();
        } else {
            return false;
        }
    }

    //-------------//
    // canTolerate //
    //-------------//
    @Override
    public boolean canTolerate (Node parent,
                                String controlValue,
                                String testValue)
    {
        return info.canTolerateElem(
                parent.getNodeName(), controlValue, testValue);
    }

    //-------------//
    // canTolerate //
    //-------------//
    @Override
    public boolean canTolerate (Element elem,
                                Attr attr,
                                String controlValue,
                                String testValue)
    {
        String attrName = attr.getName();

        // First check specific attributes
        Boolean bool = info.canTolerateAttr(
                elem.getNodeName(),
                attrName,
                controlValue,
                testValue);
        if (bool != null) {
            return bool.booleanValue();
        }

        // Then fall back on global attributes
        bool = info.canTolerateAttr(NO_ELEM, attrName, controlValue, testValue);
        if (bool != null) {
            return bool.booleanValue();
        } else {
            return false;
        }
    }
}
