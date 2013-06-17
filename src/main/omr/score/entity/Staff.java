//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t a f f                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.grid.StaffInfo;

import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

/**
 * Class {@code Staff} handles a staff in a system part.
 * It is useful for its geometric parameters (topLeft corner, width and height,
 * ability to convert between a Point ordinate and a staff-based
 * pitchPosition.
 * But it contains no further entities, the Measure's are the actual containers.
 * Within a measure, some entities may be assigned a staff, more like a tag than
 * like a parent.
 *
 * @author Hervé Bitteur
 */
public class Staff
        extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Staff.class);

    //~ Instance fields --------------------------------------------------------
    /** Top left corner of the staff (relative to the page top left corner) */
    private final Point topLeft;

    /** Related info from sheet analysis */
    private StaffInfo info;

    /** Id of staff in containing system part */
    private final int id;

    /** Flag an artificial staff */
    private boolean dummy;

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Staff //
    //-------//
    /**
     * Build a staff, given all its parameters.
     *
     * @param info    the physical information read from the sheet
     * @param part    the containing systemPart
     * @param topLeft the coordinate of the upper left corner of this staff,
     *                usually null for dummy staves
     * @param width   the staff width
     * @param height  the staff height
     */
    public Staff (StaffInfo info,
                  SystemPart part,
                  Point topLeft,
                  int width,
                  int height)
    {
        super(part);

        this.info = info;
        this.topLeft = topLeft;

        if (info != null) {
            info.setScoreStaff(this);
        }

        if (topLeft != null) {
            setBox(new Rectangle(topLeft.x, topLeft.y, width, height));
            getCenter();
        }

        // Assign id
        id = 1 + getChildIndex();
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the staff.
     *
     * @return height in units
     */
    public int getHeight ()
    {
        return getBox().height;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the staff id within the containing system part.
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
     * Report the physical information retrieved from the sheet.
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
     * Report the coordinates of the top left corner of the staff,
     * wrt the containing page.
     *
     * @return the top left coordinates
     */
    public Point getTopLeft ()
    {
        return topLeft;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the staff.
     *
     * @return the width in units
     */
    public int getWidth ()
    {
        return getBox().width;
    }

    public boolean isDummy ()
    {
        return dummy;
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute the pitch position of a pixel point.
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (Point pt)
    {
        return info.pitchPositionOf(pt);
    }

    //---------------//
    // pitchToPixels //
    //---------------//
    public int pitchToPixels (double pitchPosition)
    {
        int interline = getScale()
                .getInterline();

        return (int) Math.rint(((pitchPosition + 4) * interline) / 2.0);
    }

    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the staff width.
     *
     * @param width width of the staff
     */
    public void setWidth (int width)
    {
        Rectangle newBox = getBox();
        reset();

        newBox.width = width;
        setBox(newBox);
        getCenter();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Staff");

        try {
            if (isDummy()) {
                sb.append(" dummy");
            }

            sb.append(" topLeft=")
                    .append(topLeft);
            sb.append(" width=")
                    .append(getWidth());
            sb.append(" size=")
                    .append(getHeight());
        } catch (NullPointerException e) {
            sb.append("NONE");
        }

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------//
    // PartIterator //
    //--------------//
    /**
     * Class {@code PartIterator} implements an iterator on the
     * sequence of staves within all parallel measures of a SystemPart.
     */
    public static class PartIterator
            implements Iterator<Staff>
    {
        //~ Instance fields ----------------------------------------------------

        // Constant
        private final Iterator<TreeNode> staffIterator;

        //~ Constructors -------------------------------------------------------
        public PartIterator (Measure measure)
        {
            staffIterator = measure.getPart()
                    .getStaves()
                    .iterator();
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public boolean hasNext ()
        {
            return staffIterator.hasNext();
        }

        @Override
        public Staff next ()
        {
            return (Staff) staffIterator.next();
        }

        @Override
        public void remove ()
        {
            throw new UnsupportedOperationException("Not supported operation");
        }
    }

    //----------------//
    // SystemIterator //
    //----------------//
    /**
     * Class {@code SystemIterator} implements an iterator on the
     * sequence of staves within all parallel measures of a system
     * whatever the containing part.
     */
    public static class SystemIterator
            implements Iterator<Staff>
    {
        //~ Instance fields ----------------------------------------------------

        // Constant
        private final int measureIndex;

        private final Iterator<TreeNode> partIterator;

        // Non constant
        private SystemPart part;

        private Measure measure;

        private PartIterator partStaffIterator;

        //~ Constructors -------------------------------------------------------
        public SystemIterator (Measure measure)
        {
            measureIndex = measure.getParent()
                    .getChildren()
                    .indexOf(measure);
            partIterator = measure.getSystem()
                    .getParts()
                    .iterator();

            if (partIterator.hasNext()) {
                toNextPart();
            }
        }

        //~ Methods ------------------------------------------------------------
        public Measure getMeasure ()
        {
            return measure;
        }

        public SystemPart getPart ()
        {
            return part;
        }

        @Override
        public boolean hasNext ()
        {
            if (partStaffIterator == null) {
                return false;
            } else if (partStaffIterator.hasNext()) {
                return true;
            } else {
                // Do we have following parts?
                if (partIterator.hasNext()) {
                    toNextPart();

                    return partStaffIterator.hasNext();
                } else {
                    // This is the end ...
                    return false;
                }
            }
        }

        @Override
        public Staff next ()
        {
            if (hasNext()) {
                return partStaffIterator.next();
            } else {
                return null;
            }
        }

        @Override
        public void remove ()
        {
            throw new UnsupportedOperationException("Not supported operation.");
        }

        private void toNextPart ()
        {
            part = (SystemPart) partIterator.next();
            measure = (Measure) part.getMeasures()
                    .get(measureIndex);
            partStaffIterator = new PartIterator(measure);
        }
    }
}
