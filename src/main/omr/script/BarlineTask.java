//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r l i n e T a s k                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import omr.step.StepException;
import omr.step.Stepping;
import omr.step.Steps;

import java.util.Collection;

/**
 * Class {@code BarlineTask} assigns (or deassign) a barline shape to a
 * collection of glyphs.
 *
 * <p>If the compound flag is set, a compound glyph is composed from the
 * provided glyphs and assigned the shape. Otherwise, each provided glyph is
 * assigned the shape.</p>
 *
 * @author Hervé Bitteur
 */
public class BarlineTask
    extends AssignTask
{
    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BarlineTask //
    //-------------//
    /**
     * Create a barline assignment task
     *
     * @param shape the assigned shape (or null for a de-assignment)
     * @param compound true if all glyphs are to be merged into one compound
     * which is assigned to the given shape, false if each and every glyph is to
     * be assigned to the given shape
     * @param glyphs the collection of concerned glyphs
     */
    public BarlineTask (Sheet             sheet,
                        Shape             shape,
                        boolean           compound,
                        Collection<Glyph> glyphs)
    {
        super(sheet, shape, compound, glyphs);
    }

    //-------------//
    // BarlineTask //
    //-------------//
    /**
     * Convenient way to create a barline deassignment task
     *
     * @param glyphs the collection of glyphs to deassign
     */
    public BarlineTask (Sheet             sheet,
                        Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
    }

    //-------------//
    // BarlineTask //
    //-------------//
    /** No-arg constructor for JAXB only */
    protected BarlineTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        if (sheet.getSystemsBuilder() != null) {
            try {
                sheet.getSystemsBuilder()
                     .buildSystems();
                Stepping.reprocessSheet(
                    Steps.valueOf(Steps.MEASURES),
                    sheet,
                    sheet.getSystems(),
                    false);
            } catch (StepException ex) {
                logger.warning("Error in BarlineTask", ex);
            }
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" barline");

        return sb.toString();
    }
}
