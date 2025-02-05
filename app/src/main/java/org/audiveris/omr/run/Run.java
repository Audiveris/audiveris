//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             R u n                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.run;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>Run</code> implements a contiguous run of pixels of the same color
 * (black or white).
 * <p>
 * Note that the direction (vertical or horizontal) is not relevant.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Run
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * This is the abscissa integer value (for a horizontal run)
     * or the ordinate integer value (for a vertical run) of the first pixel encountered
     * in the run.
     */
    @XmlAttribute(name = "start")
    protected int start;

    /**
     * This is the total number of pixels that compose the run, all of the same color,
     * until a different color is encountered or the image bound is reached.
     */
    @XmlAttribute(name = "length")
    protected int length;

    /** Meant for XML unmarshalling only. */
    private Run ()
    {
        this(0, 0);
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>Run</code> instance.
     *
     * @param start  the coordinate of start for a run (y for vertical run)
     * @param length the length of the run in pixels
     */
    public Run (int start,
                int length)
    {
        this.start = start;
        this.length = length;
    }

    /**
     * Creates a new <code>Run</code> object from an existing one.
     *
     * @param that the run to copy
     */
    public Run (Run that)
    {
        this.start = that.start;
        this.length = that.length;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof Run that) {
            return (this.start == that.start) && (this.length == that.length);
        }

        return false;
    }

    //-----------------//
    // getCommonLength //
    //-----------------//
    /**
     * Report the length of the common part with another run (assumed to be adjacent)
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
    public final int getLength ()
    {
        return length;
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
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = 41 * hash + this.start;
        hash = 41 * hash + this.length;
        return hash;
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("Run{");
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
}
