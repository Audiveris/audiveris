//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;

import omr.log.Logger;

import omr.math.Rational;

import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.entity.Beam;
import omr.score.entity.BeamGroup;
import omr.score.entity.Chord;
import omr.score.entity.Dynamics;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Wrapper;

import java.util.*;

/**
 * Class <code>ScoreChecker</code> can visit the score hierarchy and perform
 * global checking on score nodes.
 * <p>We use it for: <ul>
 * <li>Improving the recognition of beam hooks</li>
 * <li>Fixing false beam hooks</li>
 * <li>Forcing consistency among time signatures</li>
 * <li>Making sure all dynamics can be assigned a shape</li>
 * </ul>
 *
 * @author Hervé Bitteur
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

    private final Wrapper<Boolean> modified;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreChecker //
    //--------------//
    /**
     * Creates a new ScoreChecker object.
     * @param modified An out parameter, to tell if entities have been modified
     */
    public ScoreChecker (Wrapper<Boolean> modified)
    {
        this.modified = modified;
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // visit Beam //
    //------------//
    /**
     * Check that all beam hooks are legal
     * @param beam the beam to check
     * @return true
     */
    @Override
    public boolean visit (Beam beam)
    {
        if (!beam.isHook()) {
            return true;
        }

        Glyph            glyph = beam.getItems()
                                     .first()
                                     .getGlyph();
        SortedSet<Chord> chords = beam.getChords();

        if (chords.size() > 1) {
            beam.addError(glyph, "Beam hook connected to several chords");

            return true;
        }

        if (chords.isEmpty()) {
            beam.addError(glyph, "Beam hook connected to no chords");

            return true;
        }

        // Check that there is at least one full beam on the same chord
        for (Beam b : chords.first()
                            .getBeams()) {
            if (!b.isHook()) {
                return true;
            }
        }

        // No real beam found on the same chord, so let's discard the hook
        if (logger.isFineEnabled()) {
            logger.fine("Removing false beam hook glyph#" + glyph.getId());
        }

        glyph.setShape(null);
        modified.value = true;

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

                    final PixelRectangle box = stem.getContourBox();
                    box.grow(xMargin, yMargin);

                    final Collection<Glyph> glyphs = systemInfo.lookupIntersectedGlyphs(
                        box,
                        stem);
                    searchHooks(chord, glyphs);
                }
            }
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Not used
     * @param score
     * @return true
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
                // This is assumed to be a complex time sig
                // (with no equivalent predefined shape)
                // Just check we are able to get num and den
                if ((timeSignature.getNumerator() == null) ||
                    (timeSignature.getDenominator() == null)) {
                    timeSignature.addError(
                        "Time signature with no rational value");
                } else { // Normal complex shape
                    logger.fine("Complex " + timeSignature);
                }
            } else if (shape == Shape.NO_LEGAL_TIME) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (ShapeRange.PartialTimes.contains(shape)) {
                // This time sig has the same of a single digit
                // So some other part is still missing
                timeSignature.addError(
                    "Orphan time signature shape : " + shape);
            } else { // Normal predefined shape

                if (!timeSignature.isDummy()) {
                    checkTimeSig(timeSignature);
                }
            }
        } catch (InvalidTimeSignature its) {
            // Error already posted in the errors window????
            logger.warning("visit. InvalidTimeSignature", its);
        }

        return true;
    }

    //- Utilities --------------------------------------------------------------

    //--------------//
    // checkTimeSig //
    //--------------//
    /**
     * Here we check time signature considered as "not complex" (TBD: why?)
     * @param timeSignature the sig to check
     */
    private void checkTimeSig (TimeSignature timeSignature)
    {
        // Check others, similar abscissa, in all other staves of the system
        // Use score hierarchy, same system, all parts, same measure id
        // If there is no time sig, create a dummy one
        // If there is one, make sure the sig is identical
        // Priority to manually assigned shapes of course
        TimeSignature bestSig = findBestTimeSig(timeSignature.getMeasure());

        if (bestSig != null) {
            for (Staff.SystemIterator sit = new Staff.SystemIterator(
                timeSignature.getMeasure()); sit.hasNext();) {
                Staff         staff = sit.next();
                Measure       measure = sit.getMeasure();
                TimeSignature sig = measure.getTimeSignature(staff);

                if (sig == null) {
                    sig = new TimeSignature(measure, staff, bestSig);

                    try {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                sig.getContextString() + " Created time sig " +
                                sig.getNumerator() + "/" +
                                sig.getDenominator());
                        }
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

    //-----------------//
    // findBestTimeSig //
    //-----------------//
    /**
     * Report the best time signature for all parallel measures (among all the
     * parallel candidate time signatures)
     * @param measure the reference measure
     * @return the best signature, or null if no suitable signature found
     */
    private TimeSignature findBestTimeSig (Measure measure)
    {
        TimeSignature manualSig;

        try {
            manualSig = findManualTimeSig(measure); // Perhaps null
        } catch (Exception ex) {
            measure.addError("Inconsistent Measures or TimeSignatures");

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
                    replaceTimeSig(oldSig, manualSig);
                } else {
                    // Inconsistent sigs
                    if (logger.isFineEnabled()) {
                        logger.fine("Inconsistency between time sigs");
                    }

                    sig.addError("Inconsistent time signature ");
                    bestSig.addError("Inconsistent time signature");

                    return null;
                }
            } catch (Exception ex) {
                // Skip invalid signatures
            }
        }

        return bestSig;
    }

    //-------------------//
    // findManualTimeSig //
    //-------------------//
    /**
     * Report a suitable manually assigned time signature, if any. For this, we
     * need to find a manual time sig, after having checked that all manual time
     * sigs in the measure are consistent.
     * @param measure the reference measure
     * @return the suitable manual sig, if any
     * @throws InconsistentTimeSignatures if at least two manual sigs differ
     */
    private TimeSignature findManualTimeSig (Measure measure)
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
                    if ((num != manualSig.getNumerator()) ||
                        (den != manualSig.getDenominator())) {
                        sig.addError("Inconsistent time signature");
                        manualSig.addError("Inconsistent time signature");

                        throw new InconsistentTimeSignatures();
                    }
                } catch (Exception ex) {
                    // Unusable signature, forget about this one
                }
            }
        }

        return manualSig;
    }

    //----------------//
    // replaceTimeSig //
    //----------------//
    /**
     * Replaces in situ the time signature 'oldSig' by the logical information
     * of 'newSig'. We use the intersected glyphs of the old sig as the glyphs
     * for the newly built signature.
     * @param oldSig the old (incorrect) time sig
     * @param newSig the correct sig to assign in lieu of oldSig
     */
    private void replaceTimeSig (TimeSignature oldSig,
                                 TimeSignature newSig)
    {
        Shape shape;

        try {
            shape = newSig.getShape();
        } catch (InvalidTimeSignature ex) {
            return;
        }

        SystemInfo        systemInfo = oldSig.getSystem()
                                             .getInfo();
        Collection<Glyph> glyphs = systemInfo.lookupIntersectedGlyphs(
            Glyphs.getContourBox(oldSig.getGlyphs()));

        if (logger.isFineEnabled()) {
            logger.fine("oldSig " + Glyphs.toString(glyphs));
        }

        Glyph compound = systemInfo.buildTransientCompound(glyphs);
        systemInfo.computeGlyphFeatures(compound);
        compound = systemInfo.addGlyph(compound);
        compound.setShape(shape, Evaluation.ALGORITHM);

        if (shape == Shape.CUSTOM_TIME_SIGNATURE) {
            try {
                compound.setRational(
                    new Rational(
                        newSig.getNumerator(),
                        newSig.getDenominator()));
            } catch (InvalidTimeSignature ex) {
                logger.warning("Invalid time signature", ex);
            }
        }

        logger.info(shape + " assigned to glyph#" + compound.getId());
    }

    //-------------//
    // searchHooks //
    //-------------//
    /**
     * Search unrecognized beam hooks among the glyphs around the provided chord
     * @param chord the provided chord
     * @param glyphs the surrounding glyphs
     */
    private void searchHooks (Chord             chord,
                              Collection<Glyph> glyphs)
    {
        // Up(+1) or down(-1) stem?
        final int          stemDir = chord.getStemDir();
        final SystemPoint  chordCenter = chord.getCenter();
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
            // Beware, stemDir is >0 upwards, while y is >0 downwards
            SystemPoint glyphCenter = system.toSystemPoint(
                glyph.getAreaCenter());

            if ((chordCenter.to(glyphCenter).y * stemDir) > 0) {
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

                if (ShapeRange.Beams.contains(vote.shape)) {
                    glyph.setShape(vote.shape, Evaluation.ALGORITHM);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "glyph#" + glyph.getId() + " recognized as " +
                            vote.shape);
                    }

                    modified.value = true;

                    break;
                }
            }
        }
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
