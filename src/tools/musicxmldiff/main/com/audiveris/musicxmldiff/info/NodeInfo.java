package com.audiveris.musicxmldiff.info;

import com.audiveris.musicxmldiff.Tolerance;

import com.audiveris.musicxmldiff.Tolerance.Delta;
import com.audiveris.musicxmldiff.Tolerance.Ratio;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "node")
public abstract class NodeInfo
        extends BasicInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Ratio-based tolerance, if any. */
    @XmlElement(name = "tolerance-ratio")
    protected final Tolerance.Ratio ratio;

    /** Delta-based tolerance, if any. */
    @XmlElement(name = "tolerance-delta")
    protected final Tolerance.Delta delta;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // NodeInfo //
    //----------//
    /**
     * Creates a new NodeInfo object.
     *
     * @param name     DOCUMENT ME!
     * @param optional DOCUMENT ME!
     * @param ratio    DOCUMENT ME!
     * @param delta    DOCUMENT ME!
     */
    public NodeInfo (String name,
                     Boolean optional,
                     Ratio ratio,
                     Delta delta)
    {
        super(name, optional);
        this.ratio = ratio;
        this.delta = delta;
    }

    /**
     * No-arg meant for JAXB
     */
    protected NodeInfo ()
    {
        super(null, null);
        ratio = null;
        delta = null;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (ratio != null) {
            sb.append(" ratio:")
                    .append((float) ratio.getMaxGap());
        }

        if (delta != null) {
            sb.append(" delta:")
                    .append((float) delta.getMaxGap());
        }

        return sb.toString();
    }
}
