//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            R a n g e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.math;

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
     * @param min  key at start
     * @param main key at highest count
     * @param max  key at stop
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
    @Override
    public String toString ()
    {
        return String.format("(%d,%d,%d)", min, main, max);
    }
}
