//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             R u n                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.lag.Section;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code Run} implements a contiguous run of pixels of the same color.
 * <p>
 * Note that the direction (vertical or horizontal) is not relevant.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Run
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Number of pixels. */
    @XmlAttribute
    private int length;

    /** Abscissa (for horizontal) / ordinate (for vertical) of first pixel. */
    @XmlAttribute
    private int start;

    /** Containing section, if any. */
    private Section section;

    //~ Constructors -------------------------------------------------------------------------------
    //-----//
    // Run //
    //-----//
    /**
     * Creates a new {@code Run} instance.
     *
     * @param start  the coordinate of start for a run (y for vertical run)
     * @param length the length of the run in pixels
     */
    public Run (int start,
                int length,
                int level)
    {
        this.start = start;
        this.length = length;
    }

    //-----//
    // Run //
    //-----//
    /** Meant for XML unmarshalling only */
    private Run ()
    {
        this(0, 0, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the length of the run in pixels
     *
     * @return this length
     */
    public final int getLength ()
    {
        return length;
    }

    //------------//
    // getSection //
    //------------//
    /**
     * Report the section that contains this run
     *
     * @return the containing section, or null if none
     */
    public Section getSection ()
    {
        return section;
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
    public final int getStart ()
    {
        return start;
    }

    //---------//
    // getStop //
    //---------//
    /**
     * Return the coordinate of the stop for a run. This is the bottom ordinate
     * for a vertical run, or the right abscissa for a horizontal run.
     *
     * @return the stop coordinate
     */
    public final int getStop ()
    {
        return (start + length) - 1;
    }

    //----------//
    // toString //
    //----------//
    /**
     * The {@code toString} method is used to get a readable image of the
     * run.
     *
     * @return a {@code String} value
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("{Run ");
        sb.append(start).append("/").append(length);
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a delta-coordinate translation to this run
     *
     * @param dc the (coordinate) translation
     */
    public final void translate (int dc)
    {
        start += dc;
    }

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

    //-------------//
    // isIdentical //
    //-------------//
    /**
     * Field by field comparison
     *
     * @param that the other Run to compare with
     * @return true if identical
     */
    public boolean isIdentical (Run that)
    {
        return (this.start == that.start) && (this.length == that.length);
    }

    //------------//
    // setSection //
    //------------//
    /**
     * Records the containing section
     *
     * @param section the section to set
     */
    public void setSection (Section section)
    {
        this.section = section;
    }

    /**
     * (package private) method to set length value.
     *
     * @param length the length to set
     */
    void setLength (int length)
    {
        this.length = length;
    }

    /**
     * (package private) method to set start value.
     *
     * @param start the start to set
     */
    void setStart (int start)
    {
        this.start = start;
    }
}
