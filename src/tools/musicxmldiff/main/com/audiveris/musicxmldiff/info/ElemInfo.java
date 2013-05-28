package com.audiveris.musicxmldiff.info;

import com.audiveris.musicxmldiff.Tolerance.Delta;
import com.audiveris.musicxmldiff.Tolerance.Ratio;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "element")
public class ElemInfo
        extends NodeInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Contained attributes. */
    protected Map<String, AttrInfo> attributes;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // ElemInfo //
    //----------//
    /**
     * Creates a new ElemInfo object.
     *
     * @param name       DOCUMENT ME!
     * @param optional   DOCUMENT ME!
     * @param ratio      DOCUMENT ME!
     * @param delta      DOCUMENT ME!
     * @param attributes DOCUMENT ME!
     */
    public ElemInfo (String name,
                     Boolean optional,
                     Ratio ratio,
                     Delta delta,
                     Map<String, AttrInfo> attributes)
    {
        super(name, optional, ratio, delta);
        this.attributes = attributes;
    }

    /**
     * No-arg meant for JAXB
     */
    protected ElemInfo ()
    {
        super(null, null, null, null);
        attributes = null;
    }

    //~ Methods ----------------------------------------------------------------
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    private AttrInfo[] getAttrs ()
    {
        if (attributes == null) {
            return null;
        } else {
            return attributes.values().toArray(new AttrInfo[attributes.size()]);
        }
    }

    private void setAttrs (AttrInfo[] attrs)
    {
        if (attributes == null) {
            attributes = new TreeMap<>();
        } else {
            attributes.clear();
        }

        for (AttrInfo attr : attrs) {
            attributes.put(attr.getName(), attr);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if ((attributes != null) && !attributes.isEmpty()) {
            sb.append(" attributes:")
                    .append(attributes);
        }

        return sb.toString();
    }
}
