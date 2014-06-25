//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e F u l l I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code TimeFullInter} is a time signature defined by a single symbol (either
 * COMMON or CUT).
 *
 * @author Hervé Bitteur
 */
public class TimeFullInter
        extends TimeInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code TimeFullInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (COMMON_TIME or CUT_TIME only)
     * @param grade evaluation grade
     */
    public TimeFullInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // sigString //
    //-----------//
    public String sigString ()
    {
        if (shape == Shape.COMMON_TIME) {
            return "COMMON";
        }

        if (shape == Shape.CUT_TIME) {
            return "CUT";
        }

        return null;
    }
}
