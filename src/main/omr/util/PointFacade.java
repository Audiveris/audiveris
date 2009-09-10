//----------------------------------------------------------------------------//
//                                                                            //
//                           P o i n t F a c a d e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Point;

import javax.xml.bind.annotation.*;

/**
 * Class <code>PointFacade</code> is a (hopefully temporary) fix to allow Xml
 * binding of standard class Point that we cannot annotate
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
    // getX //
    //------//
    public int getX ()
    {
        return point.x;
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

    //------//
    // getY //
    //------//
    public int getY ()
    {
        return point.y;
    }
}
