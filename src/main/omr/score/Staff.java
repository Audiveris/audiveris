//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t a f f                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Shape;

import omr.lag.Lag;
import static omr.score.ScoreConstants.*;
import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.StaffInfo;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.List;

/**
 * Class <code>Staff</code> handles a staff in a system part. It is useful for
 * its geometric parameters (topLeft corner, width and height, ability to
 * convert between a SystemPoint ordinate and a staff-based pitchPosition. But
 * it contains no further entities, the Measures are the actual containers.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Staff
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Staff.class);

    //~ Instance fields --------------------------------------------------------

    /** Top left corner of the staff (relative to the system top left corner) */
    private PagePoint topLeft;

    /** Actual cached display origin */
    private ScorePoint displayOrigin;

    /** Related info from sheet analysis */
    private StaffInfo info;

    /** Index of staff in containing system part */
    private int partIndex;

    /** Staff height (units) */
    private int height;

    /** Staff width (units) */
    private int width;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Staff //
    //-------//
    /**
     * Build a staff, given all its parameters
     *
     * @param info the physical information read from the sheet
     * @param part the containing systemPart
     * @param topLeft the coordinate,in units, wrt the score upper left
     *                  corner, of the upper left corner of this staff
     * @param width the staff width, in units
     * @param height the staff height, in units
     * @param partIndex the index of the staff in the containing system part,
     *                  starting at 0
     */
    public Staff (StaffInfo  info,
                  SystemPart part,
                  PagePoint  topLeft,
                  int        width,
                  int        height,
                  int        partIndex)
    {
        super(part);

        this.info = info;
        this.topLeft = topLeft;
        this.width = width;
        this.height = height;
        this.partIndex = partIndex;
    }

    //-------//
    // Staff //
    //-------//
    /**
     * Default constructor (needed by XML binder)
     */
    private Staff ()
    {
        super(null);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // setDisplayOrigin //
    //------------------//
    /**
     * Assign proper staff display origin
     *
     * @param displayOrigin staff display origin
     */
    public void setDisplayOrigin (ScorePoint displayOrigin)
    {
        this.displayOrigin = displayOrigin;
    }

    //------------------//
    // getDisplayOrigin //
    //------------------//
    /**
     * Report the staff display origin in the score display of this staff
     *
     * @return the (staff-specific) displayOrigin
     */
    @Override
    public ScorePoint getDisplayOrigin ()
    {
        return displayOrigin;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the staff
     *
     * @return height in units
     */
    public int getHeight ()
    {
        return height;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet
     *
     * @return the info entity for this staff
     */
    public StaffInfo getInfo ()
    {
        return info;
    }

    //---------------//
    // getStaffIndex //
    //---------------//
    /**
     * Report the staff index within the containing system part
     *
     * @return the index, counting from 0
     */
    public int getStaffIndex ()
    {
        return partIndex;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the system entity
     */
    public System getSystem ()
    {
        // Beware, staff is not a direct child of System, there is an
        // intermediate StaffList to skip
        return (System) container.getContainer()
                                 .getContainer();
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the top left corner of the staff, wrt the score
     *
     * @return the top left coordinates
     */
    public PagePoint getTopLeft ()
    {
        return topLeft;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the staff width
     *
     * @param width width in units f the staff
     */
    public void setWidth (int width)
    {
        this.width = width;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the staff
     *
     * @return the width in units
     */
    public int getWidth ()
    {
        return width;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //-------------//
    // pitchToUnit //
    //-------------//
    /**
     * Compute the ordinate Y (counted in units and measured from staff displayOrigin)
     * that corresponds to a given step line
     *
     * @param pitchPosition the pitch position (-4 for top line, +4 for bottom
     *                      line)
     * @return the ordinate in pixels, counted from staff displayOrigin (upper line),
     * so top line is 0px and bottom line is 64px (with an inter line of 16).
     */
    public static int pitchToUnit (double pitchPosition)
    {
        return (int) Math.rint(((pitchPosition + 4) * INTER_LINE) / 2.0);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Staff" + " topLeft=" + topLeft + " width=" + width + " size=" +
               height + " origin=" + displayOrigin + "}";
    }

    //-------------//
    // unitToPitch //
    //-------------//
    /**
     * Compute the pitch position of a given ordinate Y (counted in units and
     * measured from staff displayOrigin)
     *
     *
     * @param unit the ordinate in pixel units, counted from staff displayOrigin (upper
     * line), so top line is 0px and bottom line is 64px (with an inter line of
     * 16).
     * @return the pitch position (-4 for top line, +4 for bottom line)
     */
    public static int unitToPitch (int unit)
    {
        return (int) Math.rint(((2D * unit) - (4D * INTER_LINE)) / INTER_LINE);
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute the pitch position of a pixel point
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (PixelPoint pt)
    {
        return info.pitchPositionOf(pt);
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute the pitch position of a system point
     *
     * @param pt the system point
     * @return the pitch position
     */
    public double pitchPositionOf (SystemPoint pt)
    {
        return info.pitchPositionOf(getSystem().toPixelPoint(pt));
    }
}
