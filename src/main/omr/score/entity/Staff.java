//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t a f f                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.StaffInfo;

import omr.util.TreeNode;

import java.util.Iterator;

/**
 * Class <code>Staff</code> handles a staff in a system part. It is useful for
 * its geometric parameters (topLeft corner, width and height, ability to
 * convert between a PixelPoint ordinate and a staff-based pitchPosition. But
 * it contains no further entities, the Measure's are the actual containers.
 * Within a measure, some entities may be assigned a staff, more like a tag than
 * like a parent.
 *
 *
 * @author Hervé Bitteur
 */
public class Staff
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Staff.class);

    //~ Instance fields --------------------------------------------------------

    /** Top left corner of the staff (relative to the page top left corner) */
    private final PixelPoint topLeft;

    /** Related info from sheet analysis */
    private StaffInfo info;

    /** Id of staff in containing system part */
    private int id;

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
     * @param topLeft the coordinate of the upper left corner of this staff
     * @param width the staff width
     * @param height the staff height
     */
    public Staff (StaffInfo  info,
                  SystemPart part,
                  PixelPoint topLeft,
                  int        width,
                  int        height)
    {
        super(part);

        this.info = info;
        this.topLeft = topLeft;

        setBox(new PixelRectangle(topLeft.x, topLeft.y, width, height));
        getCenter();

        // Assign id
        id = getParent()
                 .getChildren()
                 .indexOf(this) + 1;
    }

    //~ Methods ----------------------------------------------------------------

    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
    }

    public boolean isDummy ()
    {
        return dummy;
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
        return getBox().height;
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
    public PixelPoint getTopLeft ()
    {
        return topLeft;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the staff width
     *
     * @param unitWidth width in units of the staff
     */
    public void setWidth (int unitWidth)
    {
        PixelRectangle newBox = getBox();
        reset();

        newBox.width = unitWidth;
        setBox(newBox);
        getCenter();
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
        return getBox().width;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
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

    //---------------//
    // pitchToPixels //
    //---------------//
    public int pitchToPixels (double pitchPosition)
    {
        int interline = getScale()
                            .interline();

        return (int) Math.rint(((pitchPosition + 4) * interline) / 2.0);
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

            sb.append(" pageTopLeft=")
              .append(topLeft);
            sb.append(" width=")
              .append(getWidth());
            sb.append(" size=")
              .append(getHeight());
            sb.append("}");

            return sb.toString();
        } catch (NullPointerException e) {
            return "{Staff INVALID}";
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // PartIterator //
    //--------------//
    /**
     * Class <code>PartIterator</code> implements an iterator on the sequence
     * of staves within all parallel measures of a SystemPart
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

        public boolean hasNext ()
        {
            return staffIterator.hasNext();
        }

        public Staff next ()
        {
            return (Staff) staffIterator.next();
        }

        public void remove ()
        {
            throw new UnsupportedOperationException("Not supported operation");
        }
    }

    //----------------//
    // SystemIterator //
    //----------------//
    /**
     * Class <code>SystemIterator</code> implements an iterator on the
     * sequence of staves within all parallel measures of a system
     */
    public static class SystemIterator
        implements Iterator<Staff>
    {
        //~ Instance fields ----------------------------------------------------

        // Constant
        private final int                measureIndex;
        private final Iterator<TreeNode> partIterator;

        // Non constant
        private SystemPart   part;
        private Measure      measure;
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

        public Staff next ()
        {
            if (hasNext()) {
                return partStaffIterator.next();
            } else {
                return null;
            }
        }

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
