//----------------------------------------------------------------------------//
//                                                                            //
//                                S y s t e m                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import static omr.score.ScoreConstants.*;
import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.List;

/**
 * Class <code>System</code> encapsulates a system in a score.
 *
 * <p>A system contains only one kind direct children : SystemPart instances
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class System
    extends ScoreNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(System.class);

    //~ Instance fields --------------------------------------------------------

    /** Top left corner of the system */
    private PagePoint topLeft;

    /** Actual display origin */
    private ScorePoint displayOrigin;

    /** Related info from sheet analysis */
    private SystemInfo info;

    /** System dimensions, expressed in units */
    private UnitDimension dimension;

    /** Id of first measure */
    private int firstMeasureId = 0;

    /** Id of last measure */
    private int lastMeasureId = 0;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // System //
    //--------//
    /**
     * Create a system with all needed parameters
     *
     * @param info      the physical information retrieved from the sheet
     * @param score     the containing score
     * @param topLeft   the coordinate, in units, of the upper left point of the
     *                  system in its containing score
     * @param dimension the dimension of the system, expressed in units
     */
    public System (SystemInfo    info,
                   Score         score,
                   PagePoint     topLeft,
                   UnitDimension dimension)
    {
        super(score);

        this.info = info;
        this.topLeft = topLeft;
        this.dimension = dimension;
    }

    //--------//
    // System //
    //--------//
    /**
     * Default constructor (needed by XML binder)
     */
    private System ()
    {
        this(null, null, null, null);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // setDimension //
    //--------------//
    /**
     * Set the system dimension.
     *
     * <p>Width is the distance, in units, between left edge and right edge.
     *
     * <p>Height is the distance, in units, from top of first staff, down to
     * top (and not bottom) of last staff.
     * Nota: It does not count the height of the last staff
     *
     * @param dimension system dimension, in units
     */
    public void setDimension (UnitDimension dimension)
    {
        this.dimension = dimension;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the system dimension.
     *
     * @return the system dimension, in units
     * @see #setDimension
     */
    public UnitDimension getDimension ()
    {
        return dimension;
    }

    //------------------//
    // setDisplayOrigin //
    //------------------//
    /**
     * Assign origin for score display
     *
     * @param origin display origin for this system
     */
    public void setDisplayOrigin (ScorePoint origin)
    {
        this.displayOrigin = origin;
    }

    //------------------//
    // getDisplayOrigin //
    //------------------//
    /**
     * Report the origin for this system, in the horizontal score display
     *
     * @return the display origin
     */
    public ScorePoint getDisplayOrigin ()
    {
        return displayOrigin;
    }

    //--------------//
    // getFirstPart //
    //--------------//
    /**
     * Report the first part in this system
     *
     * @return the first part entity
     */
    public SystemPart getFirstPart ()
    {
        return (SystemPart) getChildren()
                                .get(0);
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet for this system
     *
     * @return the information entity
     */
    public SystemInfo getInfo ()
    {
        return info;
    }

    //-------------//
    // getLastPart //
    //-------------//
    /**
     * Report the last part in this system
     *
     * @return the last part entity
     */
    public SystemPart getLastPart ()
    {
        return (SystemPart) getParts()
                                .get(getParts().size() - 1);
    }

    //-----------//
    // getPartAt //
    //-----------//
    /**
     * Determine the part which contains the given system point
     *
     * @param sysPt the given system point
     * @return the containing part
     */
    public SystemPart getPartAt (SystemPoint sysPt)
    {
        return getStaffAt(sysPt)
                   .getPart();
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Report the parts for this system
     *
     * @return the ordered parts
     */
    public List<TreeNode> getParts ()
    {
        return getChildren();
    }

    //------------------//
    // getRightPosition //
    //------------------//
    /**
     * Return the actual display position of the right side.
     *
     * @return the display abscissa of the right system edge
     */
    public int getRightPosition ()
    {
        return (displayOrigin.x + dimension.width) - 1;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Determine the staff which is closest to the given system point
     *
     * @param sysPt the given system point
     * @return the closest staff
     */
    public Staff getStaffAt (SystemPoint sysPt)
    {
        int   minDy = Integer.MAX_VALUE;
        Staff best = null;

        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            for (TreeNode n : part.getStaves()) {
                Staff staff = (Staff) n;
                int   midY = (staff.getTopLeft().y + (staff.getHeight() / 2)) -
                             this.getTopLeft().y;
                int   dy = Math.abs(sysPt.y - midY);

                if (dy < minDy) {
                    minDy = dy;
                    best = staff;
                }
            }
        }

        return best;
    }

    //------------//
    // setTopLeft //
    //------------//
    /**
     * Set the coordinates of the top left cormer of the system in the score
     *
     * @param topLeft the upper left point, with coordinates in units, in
     * virtual score page
     */
    public void setTopLeft (PagePoint topLeft)
    {
        this.topLeft = topLeft;
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the upper left corner of this system in its
     * containing score
     *
     * @return the top left corner
     * @see #setTopLeft
     */
    public PagePoint getTopLeft ()
    {
        return topLeft;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //--------//
    // locate //
    //--------//
    /**
     * Return the position of given ScorePoint, relative to the system.
     *
     * @param scrPt the ScorePoint in the score display
     *
     * @return -1 for left, 0 for middle, +1 for right
     */
    public int locate (ScorePoint scrPt)
    {
        if (scrPt.x < displayOrigin.x) {
            return -1;
        }

        if (scrPt.x > getRightPosition()) {
            return +1;
        }

        return 0;
    }

    //--------//
    // locate //
    //--------//
    /**
     * Return the position of given PagePoint, relative to the system
     *
     * @param pagPt the given PagePoint
     *
     * @return -1 for above, 0 for middle, +1 for below
     */
    public int locate (PagePoint pagPt)
    {
        if (pagPt.y < topLeft.y) {
            return -1;
        }

        if (pagPt.y > (topLeft.y + dimension.height + STAFF_HEIGHT)) {
            return +1;
        }

        return 0;
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Compute the point in the sheet that corresponds to a given point in the
     * score display
     *
     * @param scrPt the point in the score display
     * @return the corresponding page point
     * @see #toScorePoint
     */
    public PagePoint toPagePoint (ScorePoint scrPt)
    {
        return new PagePoint(
            (topLeft.x + scrPt.x) - displayOrigin.x,
            (topLeft.y + scrPt.y) - displayOrigin.y);
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Compute the pagepoint that correspond to a given systempoint, which is
     * basically a translation using the coordinates of the system topLeft
     * corner.
     *
     * @param sysPt the point in the system
     * @return the page point
     */
    public PagePoint toPagePoint (SystemPoint sysPt)
    {
        return new PagePoint(sysPt.x + topLeft.x, sysPt.y + topLeft.y);
    }

    //--------------//
    // toPixelPoint //
    //--------------//
    /**
     * Compute the pixel point that correspond to a given system point, which is
     * basically a translation using the coordinates of the system topLeft
     * corner, then a scaling.
     *
     * @param sysPt the point in the system
     * @return the pixel point
     */
    public PixelPoint toPixelPoint (SystemPoint sysPt)
    {
        return getScale()
                   .toPixelPoint(toPagePoint(sysPt));
    }

    //--------------//
    // toScorePoint //
    //--------------//
    /**
     * Compute the score display point that correspond to a given sheet point,
     * since systems are displayed horizontally in the score display, while they
     * are located one under the other in a sheet.
     *
     * @param pagPt the point in the sheet
     * @return the score point
     * @see #toPagePoint
     */
    public ScorePoint toScorePoint (PagePoint pagPt)
    {
        return new ScorePoint(
            (displayOrigin.x + pagPt.x) - topLeft.x,
            (displayOrigin.y + pagPt.y) - topLeft.y);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{System")
          .append(" topLeft=[")
          .append(topLeft.x)
          .append(",")
          .append(topLeft.y)
          .append("]")
          .append(" dimension=")
          .append(dimension.width)
          .append("x")
          .append(dimension.height)
          .append("}");

        return sb.toString();
    }

    //---------------//
    // toSystemPoint //
    //---------------//
    /**
     * Compute the system point that correspond to a given page point, which is
     * basically a translation using the coordinates of the system topLeft
     * corner.
     *
     * @param pagPt the point in the sheet
     * @return the system point
     */
    public SystemPoint toSystemPoint (PagePoint pagPt)
    {
        return new SystemPoint(pagPt.x - topLeft.x, pagPt.y - topLeft.y);
    }

    //---------------//
    // toSystemPoint //
    //---------------//
    /**
     * Compute the system point that correspond to a given pixel point, which is
     * basically a scaling plus a translation using the coordinates of the
     * system topLeft corner.
     *
     * @param pixPt the point in the sheet
     * @return the system point
     */
    public SystemPoint toSystemPoint (PixelPoint pixPt)
    {
        PagePoint pagPt = getScale()
                              .toPagePoint(pixPt);

        return new SystemPoint(pagPt.x - topLeft.x, pagPt.y - topLeft.y);
    }
}
