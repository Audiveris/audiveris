//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            R a n g e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.math;

import java.util.Comparator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Handles a range of values using a (min, main, max) triplet.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "range")
public class Range
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To sort by increasing main. */
    public static final Comparator<Range> byMain = (e1, e2) -> Double.compare(e1.main, e2.main);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Key at beginning of range. */
    @XmlAttribute
    public final int min;

    /** Key at highest count in range. */
    @XmlAttribute
    public final int main;

    /** Key at end of range. */
    @XmlAttribute
    public final int max;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Range} object.
     *
     * @param min  x start value
     * @param main x at highest y value
     * @param max  x stop value
     */
    public Range (int min,
                  int main,
                  int max)
    {
        this.min = min;
        this.main = main;
        this.max = max;
    }

    /** No-arg constructor meant for JAXB. */
    private Range ()
    {
        this.min = 0;
        this.main = 0;
        this.max = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getWidth //
    //----------//
    /**
     * Report the range width, (max - min + 1).
     *
     * @return range width
     */
    public int getWidth ()
    {
        return max - min + 1;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format("(%d,%d,%d)", min, main, max);
    }
}
