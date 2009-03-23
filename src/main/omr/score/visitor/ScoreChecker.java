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
import omr.score.entity.Staff;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.util.*;

/**
 * Class <code>ScoreChecker</code> can visit the score hierarchy and perform
 * global checking on score nodes.
 * <p>We use it for: <ul>
 * <li>Improving the recognition of beam hooks</li>
 * <li>Forcing consistency among time signatures</li>
 * <li>Making sure all dynamics can be assigned a shape</li>
 * </ul>
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
    /**
     * Not used
     * @param score
     * @return
     */
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
    /**
     * Method use to check and correct the consistency between all time
     * signatures that occur in parallel measures.
     * @param timeSignature the score entity that triggers the check
     * @return true
     */
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                timeSignature.getContextString() + " Checking " +
                timeSignature);
        }

        try {
            // Trigger computation of Num & Den if not already done
            Shape shape = timeSignature.getShape();

            if (shape == null) {
                if ((timeSignature.getNumerator() == null) ||
                    (timeSignature.getDenominator() == null)) {
                    timeSignature.addError(
                        "Time signature with no rational value");
                } else { // Normal complex shape
                    logger.info("*** complex " + timeSignature);
                }
            } else if (shape == Shape.NO_LEGAL_SHAPE) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (Shape.SingleTimes.contains(shape)) {
                timeSignature.addError(
                    "Orphan time signature shape : " + shape);
            } else { // Normal simple shape

                if (!timeSignature.isDummy()) {
                    checkSimpleTime(timeSignature);
                }
            }
        } catch (InvalidTimeSignature its) {
            logger.warning("visit. InvalidTimeSignature", its);
        }

        return true;
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    /**
     * Check that each dynamics shape can be computed
     * @param dynamics the dynamics item
     * @return true
     */
    @Override
    public boolean visit (Dynamics dynamics)
    {
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

    //-----------------//
    // checkSimpleTime //
    //-----------------//
    private void checkSimpleTime (TimeSignature timeSignature)
    {
        // Check others, similar abscissa, in all other staves of the system
        // Use score hierarchy, same system, all parts, same measure id
        // If there is no time sig, create a dummy one
        // If there is one, make sure the sig is identical
        // Priority to manually assigned shapes of course
        TimeSignature bestSig = findBestTime(timeSignature.getMeasure());

        if (bestSig != null) {
            for (Staff.SystemIterator sit = new Staff.SystemIterator(
                timeSignature.getMeasure()); sit.hasNext();) {
                Staff         staff = sit.next();
                Measure       measure = sit.getMeasure();
                TimeSignature sig = measure.getTimeSignature(staff);

                if (sig == null) {
                    sig = new TimeSignature(measure, staff, bestSig);

                    try {
                        logger.info(
                            sig.getContextString() + " Created time sig " +
                            sig.getNumerator() + "/" + sig.getDenominator());
                    } catch (InvalidTimeSignature ignored) {
                        logger.warning("InvalidTimeSignature", ignored);
                    }
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            sig.getContextString() + " Existing sig " + sig);
                    }
                }
            }
        }
    }

    //--------------//
    // findBestTime //
    //--------------//
    /**
     * Report the best time signature for all parallel measures (among all the
     * parallel candidate time signatures)
     * @param measure
     * @return the best signature, or null if no suitable signature found
     */
    private TimeSignature findBestTime (Measure measure)
    {
        TimeSignature manualSig = null;

        try {
            manualSig = findManualTime(measure); // Perhaps null
        } catch (InconsistentTimeSignatures ex) {
            logger.warning("InconsistentTimeSignatures");

            return null;
        }

        TimeSignature bestSig = manualSig;

        for (Staff.SystemIterator sit = new Staff.SystemIterator(measure);
             sit.hasNext();) {
            Staff staff = sit.next();
            measure = sit.getMeasure();

            TimeSignature sig = measure.getTimeSignature(staff);

            if ((sig == null) || sig.isDummy()) {
                continue;
            }

            try {
                // Make sure the signature is valid
                int num = sig.getNumerator();
                int den = sig.getDenominator();

                // First instance?
                if (bestSig == null) {
                    bestSig = sig;

                    continue;
                }

                // Still consistent?
                if ((num == bestSig.getNumerator()) &&
                    (den == bestSig.getDenominator())) {
                    continue;
                }

                // Inconsistency detected
                if (manualSig != null) {
                    // Replace sig !
                    TimeSignature oldSig = sig;
                    oldSig.getParent()
                          .getChildren()
                          .remove(oldSig);
                    sig = new TimeSignature(measure, staff, manualSig);

                    // Assign this manual sig to this incorrect sig?

                    ///oldSig.deassign();
                    replaceSig(oldSig, manualSig);
                } else {
                    // Inconsistent sigs
                    if (logger.isFineEnabled()) {
                        logger.fine("Inconsistency between time sigs");
                    }

                    sig.addError("Inconsistent time signature ");
                    bestSig.addError("Inconsistent time signature");

                    return null;
                }
            } catch (InvalidTimeSignature ex) {
                // Skip invalid signatures
            }
        }

        return bestSig;
    }

    //----------------//
    // findManualTime //
    //----------------//
    /**
     * Report a suitable manually assigned time signature, if any. For this, we
     * need to find a manual time sig, after having checked that all manual time
     * sigs in the measure are consistent. This method
     * @param measure the reference measure
     * @return the suitable manual sig, if any
     * @throws InconsistentTimeSignatures if at least two manual sigs differ
     */
    private TimeSignature findManualTime (Measure measure)
        throws InconsistentTimeSignatures
    {
        TimeSignature manualSig = null;

        for (Staff.SystemIterator sit = new Staff.SystemIterator(measure);
             sit.hasNext();) {
            Staff         staff = sit.next();
            TimeSignature sig = sit.getMeasure()
                                   .getTimeSignature(staff);

            if ((sig != null) && !sig.isDummy() && sig.isManual()) {
                try {
                    // Make sure the signature is valid
                    int num = sig.getNumerator();
                    int den = sig.getDenominator();

                    // First instance?
                    if (manualSig == null) {
                        manualSig = sig;

                        continue;
                    }

                    // Still consistent?
                    if ((num == manualSig.getNumerator()) &&
                        (den == manualSig.getDenominator())) {
                    } else {
                        sig.addError("Inconsistent time signature");
                        manualSig.addError("Inconsistent time signature");

                        throw new InconsistentTimeSignatures();
                    }
                } catch (InvalidTimeSignature ex) {
                    // Unusable signature, forget about this one
                }
            }
        }

        return manualSig;
    }

    //------------//
    // replaceSig //
    //------------//
    /**
     * Replaces in situ the time signature 'oldSig' by the logical information
     * of 'newSig'. We use the intersected glyphs of the old sig as the glyphs
     * for the newly built signature.
     * @param oldSig the old (incorrect) time sig
     * @param newSig the correct sig to assign in lieu of oldSig
     */
    private void replaceSig (TimeSignature oldSig,
                             TimeSignature newSig)
    {
        Shape shape = null;

        try {
            shape = newSig.getShape();
        } catch (InvalidTimeSignature ex) {
            return;
        }

        SystemInfo        systemInfo = oldSig.getSystem()
                                             .getInfo();
        Collection<Glyph> glyphs = systemInfo.lookupIntersectedGlyphs(
            Glyph.getContourBox(oldSig.getGlyphs()));

        if (logger.isFineEnabled()) {
            logger.fine("oldSig " + Glyph.toString(glyphs));
        }

        Glyph compound = systemInfo.buildCompound(glyphs);
        systemInfo.computeGlyphFeatures(compound);
        compound = systemInfo.addGlyph(compound);
        compound.setShape(shape, Evaluation.ALGORITHM);
        logger.info(shape + " assigned to glyph#" + compound.getId());
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------------------//
    // InconsistentTimeSignatures //
    //----------------------------//
    /**
     * Used to signal that parallel time signatures are not consistent
     */
    public static class InconsistentTimeSignatures
        extends Exception
    {
        //~ Constructors -------------------------------------------------------

        public InconsistentTimeSignatures ()
        {
            super("Time signatures are inconsistent");
        }
    }

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
            "Margin around stem height for intersecting beams & beam hooks");
    }
}
