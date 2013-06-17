//----------------------------------------------------------------------------//
//                                                                            //
//                           P o i n t F a c a d e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Point;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PointFacade} is a (hopefully temporary) fix to allow Xml
 * binding of standard class Point that we cannot annotate
 *
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "point")
public class PointFacade
{
    //~ Instance fields --------------------------------------------------------

    /** The interfaced Point instance */
    private Point point;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // PointFacade //
    //-------------//
    /**
     * Creates a new instance of PointFacade
     */
    public PointFacade ()
    {
    }

    //-------------//
    // PointFacade //
    //-------------//
    /**
     * Creates a new PointFacade object.
     *
     * @param point the interfaced point
     */
    public PointFacade (Point point)
    {
        this.point = point;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getPoint //
    //----------//
    /**
     * Report the interfaced point
     *
     * @return the actual point
     */
    public Point getPoint ()
    {
        return point;
    }

    //------//
    // getX //
    //------//
    public int getX ()
    {
        return point.x;
    }

    //------//
    // getY //
    //------//
    public int getY ()
    {
        return point.y;
    }

    //------//
    // setX //
    //------//
    @XmlAttribute
    public void setX (int x)
    {
        if (point == null) {
            point = new Point();
        }

        point.x = x;
    }

    //------//
    // setY //
    //------//
    @XmlAttribute
    public void setY (int y)
    {
        if (point == null) {
            point = new Point();
        }

        point.y = y;
    }
}
