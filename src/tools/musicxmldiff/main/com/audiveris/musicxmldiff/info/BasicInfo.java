package com.audiveris.musicxmldiff.info;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "basic")
public abstract class BasicInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Name of this entity. */
    @XmlAttribute(name = "name")
    protected final String name;

    /** Is the entity optional?. */
    @XmlElement(name = "optional")
    protected final Boolean optional;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicInfo object.
     */
    public BasicInfo (String name,
                      Boolean optional)
    {
        this.name = name;
        this.optional = optional;
    }

    protected BasicInfo ()
    {
        name = null;
        optional = false;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    public String getName ()
    {
        return name;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" '").append(name).append("'");

        if (optional != null && optional.booleanValue() == true) {
            sb.append(" opt");
        }

        return sb.toString();
    }
}
