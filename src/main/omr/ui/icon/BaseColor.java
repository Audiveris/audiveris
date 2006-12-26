/*
 * BaseColor.java
 *
 * Created on 8 décembre 2006, 17:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package omr.ui.icon;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 *
 * @author hb115668
 */
@XmlRootElement(name = "base-color")
public class BaseColor
{
    //~ Instance fields --------------------------------------------------------

    @XmlAttribute(name = "R")
    public int                          R;
    @XmlAttribute(name = "G")
    public int                          G;
    @XmlAttribute(name = "B")
    public int                          B;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of BaseColor */
    public BaseColor ()
    {
    }
}
