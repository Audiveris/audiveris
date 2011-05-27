//----------------------------------------------------------------------------//
//                                                                            //
//                          S t a f f M a n a g e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.ConstantSet;

import omr.math.GeoPath;

import omr.score.common.PixelPoint;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.util.Navigable;

import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;

/**
 * Class {@code StaffManager} handles physical information about all the staves
 * of a given sheet.
 *
 * @author Hervé Bitteur
 */
public class StaffManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** The sequence of staves, from top to bottom */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // StaffManager //
    //--------------//
    /**
     * Creates a new StaffManager object.
     * @param sheet the related sheet
     */
    public StaffManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getFirstStaff //
    //---------------//
    public StaffInfo getFirstStaff ()
    {
        if (staves.isEmpty()) {
            return null;
        } else {
            return staves.get(0);
        }
    }

    //------------//
    // getIndexOf //
    //------------//
    ///TODO @Deprecated
    public int getIndexOf (StaffInfo staff)
    {
        return staves.indexOf(staff);
    }

    //----------//
    // getRange //
    //----------//
    /**
     * Report a view on the range of staves from first to last (both inclusive)
     * @param first the first staff of the range
     * @param last the last staff of the range
     * @return a view on this range
     */
    public List<StaffInfo> getRange (StaffInfo first,
                                     StaffInfo last)
    {
        return staves.subList(getIndexOf(first), getIndexOf(last) + 1);
    }

    //----------//
    // getStaff //
    //----------//
    ///TODO @Deprecated
    public StaffInfo getStaff (int index)
    {
        return staves.get(index);
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff whose area contains the provided point
     * @param point the provided point
     * @return the nearest staff, or null if none found
     */
    public StaffInfo getStaffAt (PixelPoint point)
    {
        for (StaffInfo staff : staves) {
            Rectangle2D box = staff.getAreaBounds();

            if (point.y > box.getMaxY()) {
                continue;
            }

            if (point.y < box.getMinY()) {
                break;
            }

            if (staff.getArea()
                     .contains(point)) {
                return staff;
            }
        }

        return null;
    }

    //---------------//
    // getStaffCount //
    //---------------//
    /**
     * Report the total number of staves, whatever their containing systems
     * @return the count of staves
     */
    public int getStaffCount ()
    {
        return staves.size();
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * Assign the whole sequence of staves
     * @param staves  the (new) staves
     */
    public void setStaves (Collection<StaffInfo> staves)
    {
        reset();
        this.staves.addAll(staves);
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report an unmodifiable view (perhaps empty) of list of current staves
     * @return a view on staves
     */
    public List<StaffInfo> getStaves ()
    {
        return Collections.unmodifiableList(staves);
    }

    //----------//
    // addStaff //
    //----------//
    /**
     * Append one staff to the current collection
     * @param staff the staff to add
     */
    public void addStaff (StaffInfo staff)
    {
        staves.add(staff);
    }

    //--------------------//
    // computeStaffLimits //
    //--------------------//
    public void computeStaffLimits ()
    {
        final int width = sheet.getWidth();
        final int height = sheet.getHeight();
        StaffInfo prevStaff = null;
        double    samplingDx = sheet.getScale()
                                    .toPixelsDouble(constants.samplingDx);
        final int sampleCount = (int) Math.rint(width / samplingDx);
        samplingDx = width / sampleCount;

        for (StaffInfo staff : staves) {
            if (prevStaff == null) {
                // Very first staff
                staff.setTopLimit(
                    new GeoPath(new Line2D.Double(0, 0, width, 0)));
            } else {
                // Define a middle line between last line of previous staff 
                // and first line of current staff
                LineInfo prevLine = prevStaff.getLastLine();
                LineInfo nextLine = staff.getFirstLine();
                GeoPath  middle = new GeoPath();

                for (int i = 0; i <= sampleCount; i++) {
                    int    x = (int) Math.rint(i * samplingDx);
                    double y = (prevLine.yAt(x) + nextLine.yAt(x)) / 2;

                    if (i == 0) {
                        middle.moveTo(x, y);
                    } else {
                        middle.lineTo(x, y);
                    }
                }

                prevStaff.setBottomLimit(middle);
                staff.setTopLimit(middle);
            }

            // Remember this staff for next one
            prevStaff = staff;
        }

        // Bottom of last staff
        prevStaff.setBottomLimit(
            new GeoPath(new Line2D.Double(0, height, width, height)));
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint all the staff lines
     * @param g the graphics context (including current color and stroke)
     */
    public void render (Graphics2D g)
    {
        for (StaffInfo staff : staves) {
            staff.render(g);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Empty the whole collection of staves
     */
    public void reset ()
    {
        staves.clear();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction samplingDx = new Scale.Fraction(
            4d,
            "Abscissa sampling to determine vertical limits of staff areas");
    }
}
