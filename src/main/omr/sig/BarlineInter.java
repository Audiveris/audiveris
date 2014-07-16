//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r l i n e I n t e r                                    //
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

import omr.math.Line;

/**
 * Class {@code BarlineInter} represents an interpretation of bar line (thin or thick
 * vertical segment).
 *
 * @author Hervé Bitteur
 */
public class BarlineInter
        extends AbstractVerticalInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BarlineInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the bar line width
     */
    public BarlineInter (Glyph glyph,
                         Shape shape,
                         GradeImpacts impacts,
                         Line median,
                         double width)
    {
        super(glyph, shape, impacts, median, width);
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

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= 0.6; // TODO, quick & dirty
    }
}
