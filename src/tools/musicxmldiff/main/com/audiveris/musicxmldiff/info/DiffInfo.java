package com.audiveris.musicxmldiff.info;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "difference")
public class DiffInfo
        extends BasicInfo
{

    public DiffInfo (String name,
                     Boolean optional)
    {
        super(name, optional);
    }

    protected DiffInfo ()
    {
    }
}
