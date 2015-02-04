//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A r p e g g i a t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code Arpeggiate} represents a arpeggiate event.
 * For the time being we don't handle up & down variations
 *
 * @author Hervé Bitteur
 */
public class Arpeggiate
        extends AbstractNotation
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Arpeggiate.class);

    //~ Constructors -------------------------------------------------------------------------------
    //------------//
    // Arpeggiate //
    //------------//
    /**
     * Creates a new instance of Arpeggiate event
     *
     * @param measure measure that contains this mark
     * @param point   location of mark
     * @param chord   the chord related to the mark
     * @param glyph   the underlying glyph
     */
    public Arpeggiate (OldMeasure measure,
                       Point point,
                       OldChord chord,
                       Glyph glyph)
    {
        super(measure, point, chord, glyph);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * Used by SystemTranslator to allocate the arpeggiate marks
     *
     * @param glyph   underlying glyph
     * @param measure measure where the mark is located
     * @param point   location for the mark
     */
    public static void populate (Glyph glyph,
                                 OldMeasure measure,
                                 Point point)
    {
        if (glyph.isVip()) {
            logger.info("Arpeggiate. populate {}", glyph.idString());
        }

        // A Arpeggiate relates to ALL the embraced note(s)
        // We look on the right
        int dx = measure.getScale().toPixels(constants.areaDx);
        Point shiftedPoint = new Point(point.x + dx, point.y);
        OldSlot slot = measure.getClosestSlot(shiftedPoint);

        if (slot == null) {
            measure.addError(glyph, "Suspicious arpeggiate without slots");

            return;
        }

        // We look for ALL embraced chord notes
        Rectangle box = glyph.getBounds();
        Point top = new Point(box.x + (box.width / 2), box.y);
        Point bottom = new Point(box.x + (box.width / 2), box.y + box.height);
        List<OldChord> chords = slot.getEmbracedChords(top, bottom);

        if (!chords.isEmpty()) {
            // Allocate an instance with first embraced chord
            Arpeggiate arpeggiate = new Arpeggiate(measure, point, chords.get(0), glyph);
            glyph.setTranslation(arpeggiate);

            // Add the rest of embraced chords
            for (int i = 1; i < chords.size(); i++) {
                OldChord chord = chords.get(i);
                chord.addNotation(arpeggiate);
            }
        } else {
            measure.addError(glyph, "Arpeggiate without embraced notes");
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Abscissa translate when looking for embraced notes */
        Scale.Fraction areaDx = new Scale.Fraction(
                1.5,
                "Abscissa shift when looking for embraced notes");
    }
}
