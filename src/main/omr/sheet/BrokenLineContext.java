//----------------------------------------------------------------------------//
//                                                                            //
//                     B r o k e n L i n e C o n t e x t                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.util.BrokenLine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BrokenLineContext} gathers a broken line with its system
 * context (system above if any, and system below if any).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "limit")
public class BrokenLineContext
{
    //~ Instance fields --------------------------------------------------------

    /** Id of system above, if any */
    @XmlAttribute(name = "system-above")
    public final int systemAbove;

    /** Id of system below, if any */
    @XmlAttribute(name = "system-below")
    public final int systemBelow;

    /** The broken line */
    @XmlElement(name = "line")
    public final BrokenLine line;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------------//
    // BrokenLineContext //
    //-------------------//
    /**
     * Creates a new BrokenLineContext object.
     *
     * @param systemAbove Id of system above the line, or zero
     * @param systemBelow Id of system below the line, or zero
     * @param line        The broken line
     */
    public BrokenLineContext (int systemAbove,
                              int systemBelow,
                              BrokenLine line)
    {
        this.systemAbove = systemAbove;
        this.systemBelow = systemBelow;
        this.line = line;
    }

    //-------------------//
    // BrokenLineContext // No-arg constructor for JAXB only
    //-------------------//
    private BrokenLineContext ()
    {
        this.systemAbove = 0;
        this.systemBelow = 0;
        this.line = null;
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Context");
        sb.append(" above:")
                .append(systemAbove);
        sb.append(" below:")
                .append(systemBelow);
        sb.append(" line:")
                .append(line);

        return sb.toString();
    }
}
