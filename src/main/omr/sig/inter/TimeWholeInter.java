//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T i m e W h o l e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.entity.TimeValue;

import omr.sheet.Staff;

/**
 * Class {@code TimeWholeInter} is a time signature defined by a single symbol (either
 * COMMON or CUT).
 *
 * @author Hervé Bitteur
 */
public class TimeWholeInter
        extends TimeInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code TimeWholeInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (COMMON_TIME or CUT_TIME only)
     * @param grade evaluation grade
     */
    public TimeWholeInter (Glyph glyph,
                           Shape shape,
                           double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getValue //
    //----------//
    @Override
    public TimeValue getValue ()
    {
        return new TimeValue(shape);
    }

    //-----------//
    // replicate //
    //-----------//
    @Override
    public TimeWholeInter replicate (Staff targetStaff)
    {
        TimeWholeInter inter = new TimeWholeInter(null, shape, 0);
        inter.setStaff(targetStaff);

        return inter;
    }
}
