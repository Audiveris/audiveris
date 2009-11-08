//----------------------------------------------------------------------------//
//                                                                            //
//                          A r t i c u l a t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>Articulation</code> represents an articulation event, a special
 * notation.
 * This should apply to:
 * <pre>
 * accent                standard
 * strong-accent        standard
 * staccato                standard
 * tenuto                standard
 * detached-legato        nyi
 * staccatissimo        standard
 * spiccato                nyi
 * scoop                nyi
 * plop                        nyi
 * doit                        nyi
 * falloff                nyi
 * breath-mark                nyi ???
 * caesura                nyi ???
 * stress                nyi
 * unstress                nyi
 * other-articulation        nyi
 * </pre>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Articulation
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Articulation.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // Articulation //
    //--------------//
    /**
     * Creates a new instance of Articulation event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Articulation (Measure     measure,
                         SystemPoint point,
                         Chord       chord,
                         Glyph       glyph)
    {
        super(measure, point, chord, glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        // An Articulation relates to the note below or above on the same time slot
        Chord chord = measure.getEventChord(point);

        if (chord != null) {
            glyph.setTranslation(
                new Articulation(measure, point, chord, glyph));
        }
    }
}
