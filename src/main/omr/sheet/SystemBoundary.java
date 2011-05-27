//----------------------------------------------------------------------------//
//                                                                            //
//                        S y s t e m B o u n d a r y                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.log.Logger;

import omr.util.BrokenLine;

import net.jcip.annotations.NotThreadSafe;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>SystemBoundary</code> handles the closed boundary of a system
 * as a 2D area, defined by two broken lines, on north and south sides.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class SystemBoundary
    implements BrokenLine.Listener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemBoundary.class);

    //~ Enumerations -----------------------------------------------------------

    /** Enum to specify the north or south side of the system boundary */
    public static enum Side {
        //~ Enumeration constant initializers ----------------------------------


        /** North boundary */
        NORTH,
        /** South boundary */
        SOUTH;
    }

    //~ Instance fields --------------------------------------------------------

    /** Related system */
    private final SystemInfo system;

    /** The north and south limits */
    private final EnumMap<Side, BrokenLine> limits = new EnumMap<Side, BrokenLine>(
        Side.class);

    /** Handling of the SystemBoundary is delegated to a Polygon */
    private final Polygon polygon = new Polygon();

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SystemBoundary //
    //----------------//
    /**
     * Creates a new SystemBoundary object with north and south borders
     * @param system the related system
     * @param north the northern limit
     * @param south the southern limit
     */
    public SystemBoundary (SystemInfo system,
                           BrokenLine north,
                           BrokenLine south)
    {
        if ((north == null) || (south == null)) {
            throw new IllegalArgumentException(
                "SystemBoundary needs non-null limits");
        }

        this.system = system;
        limits.put(Side.NORTH, north);
        limits.put(Side.SOUTH, south);

        buildPolygon();

        // Register
        for (BrokenLine line : limits.values()) {
            line.addListener(this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds that enclose this boundary
     * @return the rectangular bounds
     */
    public Rectangle getBounds ()
    {
        return polygon.getBounds();
    }

    //----------//
    // getLimit //
    //----------//
    /**
     * Report the broken line on provided side
     * @param side the desired side (NORTH or SOUTH)
     * @return the desired limit
     */
    public BrokenLine getLimit (Side side)
    {
        return limits.get(side);
    }

    //-----------//
    // getLimits //
    //-----------//
    /**
     * Report the limits as a collection
     * @return the north and south limits
     */
    public Collection<BrokenLine> getLimits ()
    {
        return limits.values();
    }

    //----------//
    // contains //
    //----------//
    /**
     * Check whether the provided point lies within the SystemBoundary
     * @param point the provided point
     * @return true if the provided point lies within the SystemBoundary
     */
    public boolean contains (Point point)
    {
        return polygon.contains(point);
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the SystemBoundary in the provided graphic context.
     *
     * @param g     the Graphics context
     * @param editable flag to indicate that boundary is editable
     */
    public void render (Graphics g,
                        boolean  editable)
    {
        Graphics2D g2 = (Graphics2D) g;
        int        radius = limits.get(Side.NORTH)
                                  .getStickyDistance();

        Color      oldColor = g.getColor();

        if (editable) {
            g.setColor(Color.BLUE);
        }

        // Draw the polygon
        g2.drawPolygon(polygon);

        // Mark the points
        if (editable) {
            for (int i = 0; i < polygon.npoints; i++) {
                g2.drawRect(
                    polygon.xpoints[i] - radius,
                    polygon.ypoints[i] - radius,
                    2 * radius,
                    2 * radius);
            }
        }

        g.setColor(oldColor);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Boundary" + " system#" + system.getId() + " north:" +
               limits.get(Side.NORTH) + " south:" + limits.get(Side.SOUTH) +
               "}";
    }

    //--------//
    // update //
    //--------//
    /**
     * A system boundary line has changed
     * @param brokenLine the modified line
     */
    public void update (BrokenLine brokenLine)
    {
        buildPolygon();
        system.boundaryUpdated();
    }

    //--------------//
    // buildPolygon //
    //--------------//
    private void buildPolygon ()
    {
        polygon.reset();

        // North
        for (Point point : limits.get(Side.NORTH)
                                 .getPoints()) {
            polygon.addPoint(point.x, point.y);
        }

        // South (in reverse order)
        List<Point> reverse = new ArrayList<Point>(
            limits.get(Side.SOUTH).getPoints());
        Collections.reverse(reverse);

        for (Point point : reverse) {
            polygon.addPoint(point.x, point.y);
        }
    }
}
