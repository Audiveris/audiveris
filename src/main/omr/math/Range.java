//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            R a n g e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
