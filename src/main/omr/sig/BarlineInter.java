//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r l i n e I n t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code BarlineInter} is a first attempt to define an
 * interpretation of bar line.
 *
 * @author Hervé Bitteur
 */
public class BarlineInter
        extends BasicInter
{
    //~ Instance fields --------------------------------------------------------

    /** True if this bar line defines a part. */
    private boolean partDefining;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BarlineInter object.
     *
     * @param glyph the underlying glyph
     * @param shape the assigned shape
     * @param grade the interpretation quality
     */
    public BarlineInter (Glyph glyph,
                         Shape shape,
                         double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // isPartDefining //
    //----------------//
    /**
     * @return the partDefining
     */
    public boolean isPartDefining ()
    {
        return partDefining;
    }

    //-----------------//
    // setPartDefining //
    //-----------------//
    /**
     * @param partDefining the partDefining to set
     */
    public void setPartDefining (boolean partDefining)
    {
        this.partDefining = partDefining;
    }
}
