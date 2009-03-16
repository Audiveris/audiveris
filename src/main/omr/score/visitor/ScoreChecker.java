//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C h e c k e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.entity.BeamGroup;
import omr.score.entity.Chord;
import omr.score.entity.Dynamics;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.util.Collection;
import java.util.SortedSet;

/**
 * Class <code>ScoreChecker</code> can visit the score hierarchy perform
 * global checking on score nodes.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreChecker
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreChecker.class);

    //~ Instance fields --------------------------------------------------------

    private final boolean[] modified;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreChecker //
    //--------------//
    /**
     * Creates a new ScoreChecker object.
     * @param modified An out parameter, to tell if entities have been modified
     */
    public ScoreChecker (boolean[] modified)
    {
        this.modified = modified;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Checking score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Checking " + timeSignature);
        }

        try {
            Shape shape = timeSignature.getShape();

            if (shape == null) {
                if ((timeSignature.getNumerator() == null) ||
                    (timeSignature.getDenominator() == null)) {
                    timeSignature.addError(
                        "Time signature with no rational value");
                }
            } else if (shape == Shape.NO_LEGAL_SHAPE) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (Shape.SingleTimes.contains(shape)) {
                timeSignature.addError(
                    "Orphan time signature shape : " + shape);
            }
        } catch (InvalidTimeSignature its) {
        }

        return true;
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        // Check that each dynamics shape can be computed
        dynamics.getShape();

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    /**
     * This method is used to detect & fix unrecognized beam hooks
     * @param measure the measure to browse
     * @return true. The real output is stored in the modified[] array which is
     * used to return a boolean if at least a beam_hook has been fixed.
     */
    @Override
    public boolean visit (Measure measure)
    {
        ///logger.info("Checking " + measure);
        final Scale       scale = measure.getScale();
        final int         xMargin = scale.toPixels(constants.stemXMargin);
        final int         yMargin = scale.toPixels(constants.stemYMargin);
        final ScoreSystem system = measure.getSystem();
        final SystemInfo  systemInfo = system.getInfo();

        // Check the beam groups for non-recognized hooks
        for (BeamGroup group : measure.getBeamGroups()) {
            SortedSet<Chord> chords = group.getChords();

            for (Chord chord : chords) {
                Glyph stem = chord.getStem();

                if (stem != null) { // We could have rests w/o stem!

                    final PixelRectangle    stemBox = stem.getContourBox();
                    final PixelRectangle    box = new PixelRectangle(
                        stemBox.x - xMargin,
                        stemBox.y - yMargin,
                        stemBox.width + (2 * xMargin),
                        stemBox.height + (2 * yMargin));
                    final Collection<Glyph> glyphs = systemInfo.lookupIntersectedGlyphs(
                        box,
                        stem);
                    checkHooks(chord, glyphs);
                }
            }
        }

        return true;
    }

    //------------//
    // checkHooks //
    //------------//
    /**
     * Check for unrecognized beam hooks in the glyphs around the provided chord
     * @param chord the provided chord
     * @param glyphs the surrounding glyphs
     */
    private void checkHooks (Chord             chord,
                             Collection<Glyph> glyphs)
    {
        // Up(+1) or down(-1) stem?
        final int          stemDir = chord.getStemDir();
        final int          yMiddle = chord.getCenter().y;
        final ScoreSystem  system = chord.getSystem();
        final double       hookMaxDoubt = GlyphInspector.getHookMaxDoubt();
        final GlyphNetwork network = GlyphNetwork.getInstance();

        for (Glyph glyph : glyphs) {
            if (glyph.getShape() != null) {
                continue;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Spurious glyph#" + glyph.getId());
            }

            // Check we are on the tail (beam) end of the stem
            SystemPoint center = system.toSystemPoint(glyph.getAreaCenter());

            if (((center.y - yMiddle) * stemDir) > 0) {
                if (logger.isFineEnabled()) {
                    logger.fine("Glyph#" + glyph.getId() + " not on beam side");
                }

                continue;
            }

            // Check if a beam appears in the top evaluations
            for (Evaluation vote : network.getEvaluations(glyph)) {
                if (vote.doubt > hookMaxDoubt) {
                    break;
                }

                if (Shape.Beams.contains(vote.shape)) {
                    glyph.setShape(vote.shape, Evaluation.ALGORITHM);
                    logger.info(
                        "glyph#" + glyph.getId() + " recognized as " +
                        vote.shape);
                    modified[0] = true;

                    break;
                }
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Scale.Fraction stemXMargin = new Scale.Fraction(
            0.2d,
            "Margin around stem width for intersecting beams & beam hooks");
        private final Scale.Fraction stemYMargin = new Scale.Fraction(
            0.2d,
            "Margin around stem heightfor intersecting beams & beam hooks");
    }
}
