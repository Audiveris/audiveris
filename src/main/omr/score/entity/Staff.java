//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t a f f                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.PixelPoint;
import omr.score.common.ScorePoint;
import omr.score.common.SystemPoint;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.StaffInfo;

/**
 * Class <code>Staff</code> handles a staff in a system part. It is useful for
 * its geometric parameters (topLeft corner, width and height, ability to
 * convert between a SystemPoint ordinate and a staff-based pitchPosition. But
 * it contains no further entities, the Measure's are the actual containers.
 * Within a measure, some entities may be assigned a staff, more like a tag than
 * like a parent.
 *
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

    /** Top left corner of the staff (relative to the page top left corner) */
    private PagePoint topLeft;

    /** Actual cached display origin */
    private ScorePoint displayOrigin;

    /** Related info from sheet analysis */
    private StaffInfo info;

    /** Id of staff in containing system part */
    private int id;

    /** Staff height (units) */
    private int height;

    /** Staff width (units) */
    private int width;

    /** Flag an artificial staff */
    private boolean dummy;

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
     */
    public Staff (StaffInfo  info,
                  SystemPart part,
                  PagePoint  topLeft,
                  int        width,
                  int        height)
    {
        super(part);

        this.info = info;
        this.topLeft = topLeft;
        this.width = width;
        this.height = height;

        // Assign id
        id = getParent()
                 .getChildren()
                 .indexOf(this) + 1;
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

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(
            new SystemPoint(
                width / 2,
                topLeft.y - getSystem().getTopLeft().y + (height / 2)));
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
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

    //-------//
    // getId //
    //-------//
    /**
     * Report the staff id within the containing system part
     *
     * @return the id, counting from 1
     */
    public int getId ()
    {
        return id;
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
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{Staff");

            if (isDummy()) {
                sb.append(" dummy");
            }

            sb.append(" topLeft=")
              .append(topLeft);
            sb.append(" width=")
              .append(width);
            sb.append(" size=")
              .append(height);
            sb.append(" origin=")
              .append(displayOrigin);
            sb.append("}");

            return sb.toString();
        } catch (NullPointerException e) {
            return "{Staff INVALID}";
        }
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

    public boolean isDummy ()
    {
        return dummy;
    }

    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
    }
}
