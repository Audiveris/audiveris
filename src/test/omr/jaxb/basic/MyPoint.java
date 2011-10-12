//----------------------------------------------------------------------------//
//                                                                            //
//                               M y P o i n t                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.basic;

import java.awt.Point;

import javax.xml.bind.annotation.*;

/**
 *
 * @author hb115668
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "point")
public class MyPoint
{
    //~ Instance fields --------------------------------------------------------

    private Point p;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of MyPoint */
    public MyPoint ()
    {
    }

    /**
     * Creates a new MyPoint object.
     *
     * @param x DOCUMENT ME!
     * @param y DOCUMENT ME!
     */
    public MyPoint (Point p)
    {
        this.p = p;
    }

    //~ Methods ----------------------------------------------------------------

    public Point getPoint ()
    {
        return p;
    }

    @XmlElement
    public void setX (int x)
    {
        p.x = x;
    }

    public int getX ()
    {
        return p.x;
    }

    @XmlElement
    public void setY (int y)
    {
        p.y = y;
    }

    public int getY ()
    {
        return p.y;
    }
}
