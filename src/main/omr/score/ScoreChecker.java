//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Beam;
import omr.score.entity.BeamGroup;
import omr.score.entity.Chord;
import omr.score.entity.Dynamics;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.TreeNode;
import omr.util.WrappedBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.SortedSet;

/**
 * Class {@code ScoreChecker} can visit the score hierarchy and perform
 * global checking on score nodes.
 * <p>We use it for: <ul>
 * <li>Improving the recognition of beam hooks</li>
 * <li>Fixing false beam hooks</li>
 * <li>Forcing consistency among time signatures</li>
 * <li>Making sure all dynamics can be assigned a shape</li>
 * <li>Merge note heads with pitches too close to each other</li>
 * <li>Enforce consistency of note heads within the same chord</li>
 * </ul>
 *
 * TODO: Split this class into smaller modular classes, one per feature
 * since the browing of the score by itself is very cheap (.15 ms for a page)
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

    /** Specific predicate for beam hooks */
    private static final Predicate<Shape> hookPredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return ShapeRange.Beams.contains(shape) &&
                   (shape != Shape.COMBINING_STEM);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Glyph evaluator */
    private final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

    /** Output of the checks */
    private final WrappedBoolean modified;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreChecker //
    //--------------//
    /**
     * Creates a new ScoreChecker object.
     * @param modified This is actually an out parameter, to tell if one or
     * several entities have been modified by the score visit
     */
    public ScoreChecker (WrappedBoolean modified)
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
        try {
            if (!beam.isHook() ||
                beam.getItems()
                    .first()
                    .getGlyph()
                    .isManualShape()) {
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
            modified.set(true);

            return true;
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + beam,
                ex);
        }

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        try {
            // Check note heads pitches
            checkNotePitches(chord);

            // Check note heads consistency
            checkNoteConsistency(chord);

            // Check void note heads WRT flags or beams
            checkVoidHeads(chord);
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + chord,
                ex);
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
        try {
            dynamics.getShape();
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + dynamics,
                ex);
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    /**
     * This method is used to detect & fix unrecognized beam hooks
     * @param measure the measure to browse
     * @return true. The real output is stored in the modified global which is
     * set to true if at least a beam_hook has been fixed.
     */
    @Override
    public boolean visit (Measure measure)
    {
        try {
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
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + measure,
                ex);
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
        try {
            if (logger.isFineEnabled()) {
                logger.fine("Checking score ...");
            }

            score.acceptChildren(this);
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + score,
                ex);
        }

        return false;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    /**
     * Method used to check and correct the consistency between all time
     * signatures that occur in parallel measures.
     * @param timeSignature the score entity that triggers the check
     * @return true
     */
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine(
                    timeSignature.getContextString() + " Checking " +
                    timeSignature);
            }

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
                } else {
                    logger.fine("Complex " + timeSignature);

                    // Normal complex shape
                    if (!timeSignature.isDummy()) {
                        checkTimeSig(timeSignature);
                    }
                }
            } else if (shape == Shape.NO_LEGAL_TIME) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (ShapeRange.PartialTimes.contains(shape)) {
                // This time sig has the shape of a single digit
                // So some other part is still missing
                timeSignature.addError(
                    "Orphan time signature shape : " + shape);
            } else {
                // Normal predefined shape
                if (!timeSignature.isDummy()) {
                    checkTimeSig(timeSignature);
                }
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " +
                timeSignature,
                ex);
        }

        return true;
    }

    //- Utilities --------------------------------------------------------------

    //------------------//
    // arePitchDeltasOk //
    //------------------//
    private boolean arePitchDeltasOk (List<Note> list)
    {
        double minDeltaPitch = constants.minDeltaNotePitch.getValue();
        Note   lastNote = null;

        for (Note note : list) {
            if (lastNote != null) {
                double deltaPitch = note.getPitchPosition() -
                                    lastNote.getPitchPosition();

                if (Math.abs(deltaPitch) < minDeltaPitch) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Too small delta pitch between " + note + " & " +
                            lastNote);
                    }

                    mergeNotes(lastNote, note);

                    return false;
                }
            }

            lastNote = note;
        }

        return true;
    }

    //----------------------//
    // checkNoteConsistency //
    //----------------------//
    /**
     * Check that all note heads of a chord are of the same shape
     * (either all black or all void).
     * @param chord
     */
    private void checkNoteConsistency (Chord chord)
    {
        EnumMap<Shape, List<Note>> shapes = new EnumMap<Shape, List<Note>>(
            Shape.class);

        for (TreeNode node : chord.getNotes()) {
            Note note = (Note) node;

            if (!note.isRest()) {
                Shape      shape = note.getShape();
                List<Note> notes = shapes.get(shape);

                if (notes == null) {
                    notes = new ArrayList<Note>();
                    shapes.put(shape, notes);
                }

                notes.add(note);
            }
        }

        if (shapes.keySet()
                  .size() > 1) {
            chord.addError(
                chord.getStem(),
                "Note inconsistency in " + chord + shapes);

            // Check evaluations
            double bestEval = Double.MIN_VALUE;
            Shape  bestShape = null;

            for (Shape shape : shapes.keySet()) {
                List<Note> notes = shapes.get(shape);

                for (Note note : notes) {
                    for (Glyph glyph : note.getGlyphs()) {
                        if (glyph.getGrade() > bestEval) {
                            bestEval = glyph.getGrade();
                            bestShape = shape;
                        }
                    }
                }
            }

            logger.info(chord + " aligned on shape " + bestShape);

            final Shape      baseShape = bestShape; // Must be final
            Predicate<Shape> predicate = new Predicate<Shape>() {
                final Collection<Shape> desiredShapes = Arrays.asList(
                    Note.getActualShape(baseShape, 1),
                    Note.getActualShape(baseShape, 2),
                    Note.getActualShape(baseShape, 3));

                public boolean check (Shape shape)
                {
                    return desiredShapes.contains(shape);
                }
            };

            ScoreSystem system = chord.getSystem();

            for (Shape shape : shapes.keySet()) {
                if (shape == bestShape) {
                    continue;
                }

                List<Note> notes = shapes.get(shape);

                for (Note note : notes) {
                    for (Glyph glyph : note.getGlyphs()) {
                        Evaluation vote = evaluator.topVote(
                            glyph,
                            Grades.consistentNoteMinGrade,
                            system.getInfo(),
                            predicate);

                        if (vote != null) {
                            glyph.setEvaluation(vote);
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // checkNotePitches //
    //------------------//
    /**
     * Check that on each side of the chord stem, the notes pitches are
     * not too close to each other.
     * @param chord the chord at hand
     */
    private void checkNotePitches (Chord chord)
    {
        Glyph stem = chord.getStem();

        if (stem == null) {
            return;
        }

        PixelPoint     pixPoint = stem.getAreaCenter();
        PixelPoint     stemCenter = pixPoint;

        // Look on left and right sides
        List<TreeNode> allNotes = new ArrayList<TreeNode>(chord.getNotes());
        Collections.sort(allNotes, Chord.noteHeadComparator);

        List<Note> lefts = new ArrayList<Note>();
        List<Note> rights = new ArrayList<Note>();

        for (TreeNode nNode : allNotes) {
            Note       note = (Note) nNode;
            PixelPoint center = note.getCenter();

            if (center.x < stemCenter.x) {
                lefts.add(note);
            } else {
                rights.add(note);
            }
        }

        // Check on left & right
        if (!arePitchDeltasOk(lefts)) {
            modified.set(true);
        }

        if (!arePitchDeltasOk(rights)) {
            modified.set(true);
        }
    }

    //--------------//
    // checkTimeSig //
    //--------------//
    /**
     * Here we check time signature across all staves of the system
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

    //----------------//
    // checkVoidHeads //
    //----------------//
    /**
     * Check that void note heads do not coexist with flags of beams
     * @param chord
     */
    private void checkVoidHeads (Chord chord)
    {
        // Void heads?
        double      noteGrade = Double.MIN_VALUE;
        List<Glyph> voidGlyphs = new ArrayList<Glyph>();

        for (TreeNode node : chord.getNotes()) {
            Note  note = (Note) node;
            Shape noteShape = note.getShape();

            if (ShapeRange.VoidNoteHeads.contains(noteShape)) {
                for (Glyph glyph : note.getGlyphs()) {
                    noteGrade = Math.max(noteGrade, glyph.getGrade());
                    voidGlyphs.add(glyph);
                }
            }
        }

        if (voidGlyphs.isEmpty()) {
            return;
        }

        ScoreSystem      system = chord.getSystem();
        Predicate<Shape> predicate = new Predicate<Shape>() {
            public boolean check (Shape shape)
            {
                return ShapeRange.BlackNoteHeads.contains(shape);
            }
        };

        // Flags or beams
        boolean fix = false;

        if (!chord.getBeams()
                  .isEmpty()) {
            // We trust beams
            logger.info(
                chord.getContextString() + " Head/beam conflict in " + chord);
            fix = true;
        } else if (chord.getFlagsNumber() > 0) {
            // Check grade of flag(s)
            double flagGrade = Double.MIN_VALUE;

            for (Glyph flag : chord.getFlagGlyphs()) {
                flagGrade = Math.max(flagGrade, flag.getGrade());
            }

            logger.info(
                chord.getContextString() + " Head/flag conflict in " + chord);

            if (noteGrade <= flagGrade) {
                fix = true;
            }
        }

        if (fix) {
            // Change note shape (void -> black)
            for (Glyph glyph : voidGlyphs) {
                Evaluation vote = evaluator.topVote(
                    glyph,
                    Grades.consistentNoteMinGrade,
                    system.getInfo(),
                    predicate);

                if (vote != null) {
                    glyph.setEvaluation(vote);
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
                    // Assign this manual sig to this (incorrect) sig
                    sig.copy(manualSig);
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

    //------------//
    // mergeNotes //
    //------------//
    private void mergeNotes (Note first,
                             Note second)
    {
        if ((first.getShape() == Shape.VOID_NOTEHEAD) &&
            (second.getShape() == Shape.VOID_NOTEHEAD)) {
            List<Glyph> glyphs = new ArrayList<Glyph>();

            glyphs.addAll(first.getGlyphs());
            glyphs.addAll(second.getGlyphs());

            SystemInfo system = first.getSystem()
                                     .getInfo();
            Glyph      compound = system.buildTransientCompound(glyphs);
            Evaluation vote = GlyphNetwork.getInstance()
                                          .vote(
                compound,
                Grades.mergedNoteMinGrade,
                first.getSystem().getInfo());

            if (vote != null) {
                compound = system.addGlyph(compound);
                compound.setEvaluation(vote);
                logger.info(
                    "Glyph#" + compound.getId() + " merged two note heads");
            }
        }
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
        final PixelPoint   chordCenter = chord.getCenter();
        final ScoreSystem  system = chord.getSystem();
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
            PixelPoint glyphCenter = glyph.getAreaCenter();

            if ((chordCenter.to(glyphCenter).y * stemDir) > 0) {
                if (logger.isFineEnabled()) {
                    logger.fine("Glyph#" + glyph.getId() + " not on beam side");
                }

                continue;
            }

            // Check if a beam appears in the top evaluations
            Evaluation vote = network.topVote(
                glyph,
                Grades.hookMinGrade,
                system.getInfo(),
                hookPredicate);

            if (vote != null) {
                glyph.setShape(vote.shape, Evaluation.ALGORITHM);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "glyph#" + glyph.getId() + " recognized as " +
                        vote.shape);
                }

                modified.set(true);
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
        Constant.Double              minDeltaNotePitch = new Constant.Double(
            "PitchPosition",
            1.5,
            "Minimum pitch difference between note heads on same stem side");
    }
}
