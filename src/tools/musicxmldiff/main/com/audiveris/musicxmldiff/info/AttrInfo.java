package com.audiveris.musicxmldiff.info;

import com.audiveris.musicxmldiff.Tolerance.Delta;
import com.audiveris.musicxmldiff.Tolerance.Ratio;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "attribute")
public class AttrInfo
    extends NodeInfo
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AttrInfo object.
     *
     * @param name DOCUMENT ME!
     * @param optional DOCUMENT ME!
     * @param ratio DOCUMENT ME!
     * @param delta DOCUMENT ME!
     */
    public AttrInfo (String  name,
                     Boolean optional,
                     Ratio   ratio,
                     Delta   delta)
    {
        super(name, optional, ratio, delta);
    }

    /**
     * No-arg meant for JAXB
     */
    protected AttrInfo ()
    {
    }
}
