//----------------------------------------------------------------------------//
//                                                                            //
//                            F i l t e r I n f o                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.musicxmldiff.info;

import com.audiveris.musicxmldiff.Tolerance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FilterInfo} gathers customization information for
 * filtering purpose.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "filter")
public class FilterInfo
{
    //~ Static fields/initializers ---------------------------------------------

    /** Context for JAXB unmarshalling */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------
    //
    /** Info on differences. */
    protected Map<String, DiffInfo> differences;

    /** Info on elements. */
    protected Map<String, ElemInfo> elements;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // FilterInfo //
    //------------//
    /**
     * Creates a new FilterInfo object.
     */
    public FilterInfo (Map<String, DiffInfo> differences,
                       Map<String, ElemInfo> elements)
    {
        this.differences = differences;
        this.elements = elements;
    }

    //------------//
    // FilterInfo //
    //------------//
    /**
     * No-arg constructor meant for JAXB
     */
    protected FilterInfo ()
    {
        differences = null;
        elements = null;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //---------------//
    // canIgnoreDiff //
    //---------------//
    public boolean canIgnoreDiff (String name)
    {
        if (differences == null) {
            return false;
        }

        DiffInfo diff = differences.get(name);
        if (diff == null) {
            return false;
        }

        Boolean opt = diff.optional;
        if (opt == null) {
            return false;
        }

        return opt.booleanValue();
    }

    //---------------//
    // canIgnoreElem //
    //---------------//
    public boolean canIgnoreElem (String name)
    {
        if (elements == null) {
            return false;
        }

        ElemInfo elem = elements.get(name);
        if (elem == null) {
            return false;
        }

        Boolean opt = elem.optional;
        if (opt == null) {
            return false;
        }

        return opt.booleanValue();
    }

    //---------------//
    // canIgnoreAttr //
    //---------------//
    public Boolean canIgnoreAttr (String elemName,
                                  String attrName)
    {
        if (elements == null) {
            return null;
        }

        ElemInfo elem = elements.get(elemName);
        if (elem == null || elem.attributes == null) {
            return null;
        }

        AttrInfo attr = elem.attributes.get(attrName);
        if (attr == null) {
            return null;
        }

        return attr.optional;
    }

    //-----------------//
    // canTolerateElem //
    //-----------------//
    public boolean canTolerateElem (String elemName,
                                    String controlValue,
                                    String testValue)
    {
        if (elements == null) {
            return false;
        }

        ElemInfo elem = elements.get(elemName);
        if (elem == null) {
            return false;
        }

        Tolerance ratio = elem.ratio;
        if (ratio != null && ratio.check(controlValue, testValue)) {
            return true;
        }

        Tolerance delta = elem.delta;
        if (delta != null && delta.check(controlValue, testValue)) {
            return true;
        }

        return false;
    }

    //-----------------//
    // canTolerateAttr //
    //-----------------//
    public Boolean canTolerateAttr (String elemName,
                                    String attrName,
                                    String controlValue,
                                    String testValue)
    {
        if (elements == null) {
            return null;
        }

        ElemInfo elem = elements.get(elemName);
        if (elem == null || elem.attributes == null) {
            return null;
        }

        AttrInfo attr = elem.attributes.get(attrName);
        if (attr == null) {
            return null;
        }

        Tolerance ratio = attr.ratio;
        Tolerance delta = attr.delta;
        if (ratio == null && delta == null) {
            return null;
        }

        if (ratio != null && ratio.check(controlValue, testValue)) {
            return true;
        }

        if (delta != null && delta.check(controlValue, testValue)) {
            return true;
        }

        return false;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{filter ");

        if ((differences != null) && !differences.isEmpty()) {
            sb.append(" differences:")
                    .append(differences);
        }

        if ((elements != null) && !elements.isEmpty()) {
            sb.append(" elements:")
                    .append(elements);
        }

        sb.append("}");

        return sb.toString();
    }

    //----------//
    // marshall //
    //----------//
    public void marshall (OutputStream outputStream)
            throws JAXBException, FileNotFoundException, IOException
    {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(FilterInfo.class);
        }

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(this, outputStream);
    }

    //------------//
    // unmarshall //
    //------------//
    public static FilterInfo unmarshall (InputStream inputStream)
            throws JAXBException, FileNotFoundException
    {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(FilterInfo.class);
        }

        Unmarshaller um = jaxbContext.createUnmarshaller();
        FilterInfo filterInfo = (FilterInfo) um.unmarshal(inputStream);

        return filterInfo;
    }

    //----------//
    // getDiffs //
    //----------//
    @XmlElementWrapper(name = "differences")
    @XmlElement(name = "difference")
    private DiffInfo[] getDiffs ()
    {
        if (differences == null) {
            return null;
        } else {
            return differences.values().toArray(
                    new DiffInfo[differences.size()]);
        }
    }

    //----------//
    // setDiffs //
    //----------//
    private void setDiffs (DiffInfo[] diffs)
    {
        if (differences == null) {
            differences = new TreeMap<>();
        } else {
            differences.clear();
        }

        for (DiffInfo diff : diffs) {
            differences.put(diff.getName(), diff);
        }
    }

    //----------//
    // getElems //
    //----------//
    @XmlElementWrapper(name = "elements")
    @XmlElement(name = "element")
    private ElemInfo[] getElems ()
    {
        if (elements == null) {
            return null;
        } else {
            return elements.values().toArray(
                    new ElemInfo[elements.size()]);
        }
    }

    //----------//
    // setElems //
    //----------//
    private void setElems (ElemInfo[] elems)
    {
        if (elements == null) {
            elements = new TreeMap<>();
        } else {
            elements.clear();
        }

        for (ElemInfo elem : elems) {
            elements.put(elem.getName(), elem);
        }
    }
}
