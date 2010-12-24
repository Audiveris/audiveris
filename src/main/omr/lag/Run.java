//----------------------------------------------------------------------------//
//                                                                            //
//                                   R u n                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Run</code> implements a contiguous run of pixels of the same
 * color. Note that the direction (vertical or horizontal) is not relevant.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Run
{
    //~ Instance fields --------------------------------------------------------

    /** Number of pixels */
    @XmlAttribute
    private int length;

    /** Average pixel level along the run */
    @XmlAttribute
    private int level;

    /** Abscissa (for horizontal) / ordinate (for vertical) of first pixel */
    @XmlAttribute
    private int start;

    //~ Constructors -----------------------------------------------------------

    //-----//
    // Run //
    //-----//
    /**
     * Creates a new <code>Run</code> instance.
     *
     * @param start  the coordinate of start for a run (y for vertical run)
     * @param length the length of the run in pixels
     * @param level the average level of gray in the run (0 for totally black,
     *               255 for totally white)
     */
    public Run (int start,
                int length,
                int level)
    {
        this.start = start;
        this.length = length;
        this.level = level;
    }

    //-----//
    // Run //
    //-----//
    /** Meant for XML unmarshalling only */
    private Run ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getCommonLength //
    //-----------------//
    /**
     * Report the length of the common part with another run (assumed to be
     * adjacent)
     *
     * @param other the other run
     * @return the length of the common part
     */
    public int getCommonLength (Run other)
    {
        int startCommon = Math.max(this.getStart(), other.getStart());
        int stopCommon = Math.min(this.getStop(), other.getStop());

        return stopCommon - startCommon + 1;
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the length of the run in pixels
     *
     * @return this length
     */
    public int getLength ()
    {
        return length;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Return the mean gray level of the run
     *
     * @return the average value of gray level along this run
     */
    public int getLevel ()
    {
        return level;
    }

    //----------//
    // getStart //
    //----------//
    /**
     * Report the starting coordinate of the run (x for horizontal, y for
     * vertical)
     *
     * @return the start coordinate
     */
    public int getStart ()
    {
        return start;
    }

    //---------//
    // getStop //
    //---------//
    /**
     * Return the coordinate of the stop for a run.  This is the bottom ordinate
     * for a vertical run, or the right abscissa for a horizontal run.
     *
     * @return the stop coordinate
     */
    public int getStop ()
    {
        return (start + length) - 1;
    }

    //----------//
    // toString //
    //----------//
    /**
     * The <code>toString</code> method is used to get a readable image of the
     * run.
     *
     * @return a <code>String</code> value
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("{Run ");
        sb.append(start)
          .append("/")
          .append(length);
        sb.append("@")
          .append(level);
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a delta-coordinate translation to this run
     * @param dc the (coordinate) translation
     */
    public void translate (int dc)
    {
        start += dc;
    }
}
