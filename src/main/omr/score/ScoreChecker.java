//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.math.GeoUtil;

import omr.score.entity.Beam;
import omr.score.entity.BeamGroup;
import omr.score.entity.Chord;
import omr.score.entity.Dynamics;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.TreeNode;
import omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * since the browsing of the score by itself is very cheap (.15 ms for a page)
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
    private static final Logger logger = LoggerFactory.getLogger(ScoreChecker.class);

    /** Specific predicate for beam hooks */
    private static final Predicate<Shape> hookPredicate = new Predicate<Shape>()
    {
        @Override
        public boolean check (Shape shape)
        {
            return ShapeSet.Beams.contains(shape);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Glyph evaluator */
    private final ShapeEvaluator evaluator = GlyphNetwork.getInstance();

    /** Output of the checks */
    private final WrappedBoolean modified;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // ScoreChecker //
    //--------------//
    /**
     * Creates a new ScoreChecker object.
     *
     * @param modified This is actually an out parameter, to tell if one or
     *                 several entities have been modified by the score visit
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
     *
     * @param beam the beam to check
     * @return true
     */
    @Override
    public boolean visit (Beam beam)
    {
        try {
            Glyph glyph = beam.getFirstItem().getGlyph();

            if (!beam.isHook() || glyph.isManualShape()) {
                return true;
            }

            List<Chord> chords = beam.getChords();

            if (chords.size() > 1) {
                beam.addError(glyph, "Beam hook connected to several chords");

                return true;
            }

            if (chords.isEmpty()) {
                beam.addError(glyph, "Beam hook connected to no chords");

                return true;
            }

            // Check that there is at least one full beam on the same chord
            // And vertically closer than the chord head 
            Chord chord = chords.get(0);
            int stemX = chord.getStem().getLocation().x;
            double hookY = glyph.getCentroid().y;
            int headY = chord.getHeadLocation().y;
            double toHead = Math.abs(headY - hookY);

            for (Beam b : chord.getBeams()) {
                if (!b.isHook()) {
                    // Check hook is closer to beam than to head
                    double beamY = b.getLine().yAtX(stemX);
                    double toBeam = Math.abs(beamY - hookY);

                    if (toBeam <= toHead) {
                        return true;
                    }
                }
            }

            // No real beam found on the same chord, so let's discard the hook
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info("{} Removing false beam hook {}",
                        beam.getMeasure().getContextString(), glyph.idString());
            }

            glyph.setShape(null);
            modified.set(true);

            return true;
        } catch (Exception ex) {
            logger.warn(
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

            // Check note heads do not appear on both stem head and tail
            checkHeadLocations(chord);
        } catch (Exception ex) {
            logger.warn(
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
     *
     * @param dynamics the dynamics item
     * @return true
     */
    @Override
    public boolean visit (Dynamics dynamics)
    {
        try {
            dynamics.getShape();
        } catch (Exception ex) {
            logger.warn(
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
     *
     * @param measure the measure to browse
     * @return true. The real output is stored in the modified global which is
     *         set to true if at least a beam_hook has been fixed.
     */
    @Override
    public boolean visit (Measure measure)
    {
        try {
            final Scale scale = measure.getScale();
            final ScoreSystem system = measure.getSystem();
            final SystemInfo systemInfo = system.getInfo();

            // Check the beam groups for non-recognized hooks
            for (BeamGroup group : measure.getBeamGroups()) {
                for (Chord chord : group.getChords()) {
                    Glyph stem = chord.getStem();

                    // We could have rests (w/o stem!)
                    if (stem != null) {
                        searchHooks(
                                chord,
                                systemInfo.lookupIntersectedGlyphs(
                                systemInfo.stemBoxOf(stem),
                                stem));
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn(
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
     *
     * @param score
     * @return true
     */
    @Override
    public boolean visit (Score score)
    {
        try {
            logger.debug("Checking score ...");
            score.acceptChildren(this);
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + score,
                    ex);
        }

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    /**
     * Check that all slurs have embraced notes on each end, except
     * perhaps on left and right sides of the part
     *
     * @param systemPart the part to process
     * @return true, since measures below must be visited too
     */
    @Override
    public boolean visit (SystemPart systemPart)
    {
        systemPart.checkSlurConnections();

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    /**
     * Method used to check and correct the consistency between all time
     * signatures that occur in parallel measures.
     *
     * @param timeSignature the score entity that triggers the check
     * @return true
     */
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        try {
            logger.debug("{} Checking {}",
                    timeSignature.getContextString(), timeSignature);

            // Trigger computation of Num & Den if not already done
            Shape shape = timeSignature.getShape();

            if (shape == null) {
                // This is assumed to be a complex time sig
                // (with no equivalent predefined shape)
                // Just check we are able to get num and den
                if ((timeSignature.getNumerator() == null)
                    || (timeSignature.getDenominator() == null)) {
                    timeSignature.addError(
                            "Time signature with no rational value");
                } else {
                    logger.debug("Complex {}", timeSignature);

                    // Normal complex shape
                    if (!timeSignature.isDummy()) {
                        checkTimeSig(timeSignature);
                    }
                }
            } else if (shape == Shape.NO_LEGAL_TIME) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (ShapeSet.PartialTimes.contains(shape)) {
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
            logger.warn(
                    getClass().getSimpleName() + " Error visiting "
                    + timeSignature,
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
        Note lastNote = null;

        for (Note note : list) {
            if (lastNote != null) {
                double deltaPitch = note.getPitchPosition()
                                    - lastNote.getPitchPosition();

                if (Math.abs(deltaPitch) < minDeltaPitch) {
                    logger.debug("Too small delta pitch between {} & {}",
                            note, lastNote);
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
     *
     * @param chord
     */
    private void checkNoteConsistency (Chord chord)
    {
        EnumMap<Shape, List<Note>> shapes = new EnumMap<>(Shape.class);

        for (TreeNode node : chord.getNotes()) {
            Note note = (Note) node;

            if (!note.isRest()) {
                Shape shape = note.getShape();
                List<Note> notes = shapes.get(shape);

                if (notes == null) {
                    notes = new ArrayList<>();
                    shapes.put(shape, notes);
                }

                notes.add(note);
            }
        }

        if (shapes.keySet().size() > 1) {
            chord.addError(chord.getStem(),
                    "Note inconsistency in " + chord + shapes);

            // Check evaluations
            double bestEval = Double.MIN_VALUE;
            Shape bestShape = null;

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

            logger.debug("{} aligned on shape {}", chord, bestShape);

            final Shape baseShape = bestShape; // Must be final
            Predicate<Shape> predicate = new Predicate<Shape>()
            {
                final Collection<Shape> desiredShapes = Arrays.asList(
                        Note.getActualShape(baseShape, 1),
                        Note.getActualShape(baseShape, 2),
                        Note.getActualShape(baseShape, 3));

                @Override
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
                        Evaluation vote = evaluator.vote(
                                glyph,
                                system.getInfo(),
                                Grades.consistentNoteMinGrade,
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
     *
     * @param chord the chord at hand
     */
    private void checkNotePitches (Chord chord)
    {
        Glyph stem = chord.getStem();

        if (stem == null) {
            return;
        }

        Point pixPoint = stem.getAreaCenter();
        Point stemCenter = pixPoint;

        // Look on left and right sides
        List<TreeNode> allNotes = new ArrayList<>(chord.getNotes());
        Collections.sort(allNotes, Chord.noteHeadComparator);

        List<Note> lefts = new ArrayList<>();
        List<Note> rights = new ArrayList<>();

        for (TreeNode nNode : allNotes) {
            Note note = (Note) nNode;
            Point center = note.getCenter();

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
     *
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
                Staff staff = sit.next();
                Measure measure = sit.getMeasure();
                TimeSignature sig = measure.getTimeSignature(staff);

                if (sig == null) {
                    sig = new TimeSignature(measure, staff, bestSig);

                    try {
                        logger.debug("{} Created time sig {}/{}",
                                sig.getContextString(),
                                sig.getNumerator(), sig.getDenominator());
                    } catch (InvalidTimeSignature ignored) {
                        logger.warn("InvalidTimeSignature", ignored);
                    }
                } else {
                    logger.debug("{} Existing sig {}",
                            sig.getContextString(), sig);
                }
            }
        }
    }

    //----------------//
    // checkVoidHeads //
    //----------------//
    /**
     * Check that void note heads do not coexist with flags or beams.
     *
     * @param chord
     */
    private void checkVoidHeads (Chord chord)
    {
        // Void heads?
        double noteGrade = Double.MIN_VALUE;
        Set<Glyph> voidGlyphs = new HashSet<>();

        for (TreeNode node : chord.getNotes()) {
            Note note = (Note) node;
            Shape noteShape = note.getShape();

            if (ShapeSet.VoidNoteHeads.contains(noteShape)) {
                for (Glyph glyph : note.getGlyphs()) {
                    noteGrade = Math.max(noteGrade, glyph.getGrade());
                    voidGlyphs.add(glyph);
                }
            }
        }

        if (voidGlyphs.isEmpty()) {
            return;
        }

        Predicate<Shape> blackHeadPredicate = new Predicate<Shape>()
        {
            @Override
            public boolean check (Shape shape)
            {
                return ShapeSet.BlackNoteHeads.contains(shape);
            }
        };

        // Flags or beams
        boolean fix = false;

        if (!chord.getBeams().isEmpty()) {
            // We trust beams
            logger.debug("{} Head/beam conflict in {}",
                    chord.getContextString(), chord);
            fix = true;
        } else if (chord.getFlagsNumber() > 0) {
            // Check grade of flag(s)
            double flagGrade = Double.MIN_VALUE;

            for (Glyph flag : chord.getFlagGlyphs()) {
                flagGrade = Math.max(flagGrade, flag.getGrade());
            }

            logger.debug("{} Head/flag conflict in {}",
                    chord.getContextString(), chord);

            if (noteGrade <= flagGrade) {
                fix = true;
            }
        }

        if (fix) {
            // Change note shape (void -> black)
            for (Glyph glyph : voidGlyphs) {
                Evaluation vote = evaluator.rawVote(
                        glyph,
                        Grades.consistentNoteMinGrade,
                        blackHeadPredicate);

                if (vote != null) {
                    glyph.setEvaluation(vote);
                }
            }
        }
    }

    //--------------------//
    // checkHeadLocations //
    //--------------------//
    /**
     * Check that note heads do not appear on both stem head and tail.
     * On tail we can have nothing or beams or flags, but no heads
     *
     * @param chord the chord to check
     */
    private void checkHeadLocations (Chord chord)
    {
        // This test applies only to chords with stem
        Glyph stem = chord.getStem();
        if (stem == null) {
            return;
        }

        Rectangle tailBox = new Rectangle(chord.getTailLocation());
        int halfTailBoxSide = chord.getScale().toPixels(constants.halfTailBoxSide);
        tailBox.grow(halfTailBoxSide, halfTailBoxSide);

        for (TreeNode node : chord.getNotes()) {
            Note note = (Note) node;

            // If note is close to tail, it can't be a note
            if (note.getBox().intersects(tailBox)) {
                for (Glyph glyph : note.getGlyphs()) {
                    if (logger.isDebugEnabled() || glyph.isVip()) {
                        logger.info("Note {} too close to tail of stem {}",
                                note, stem);
                    }
                    glyph.setShape(null);
                }
                modified.set(true);
            }
        }
    }

    //-----------------//
    // findBestTimeSig //
    //-----------------//
    /**
     * Report the best time signature for all parallel measures
     * (among all the parallel candidate time signatures)
     *
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
                if ((num == bestSig.getNumerator())
                    && (den == bestSig.getDenominator())
                    && sig.getShape() == bestSig.getShape()) {
                    continue;
                }

                // Inconsistency detected
                if (manualSig != null) {
                    // Assign this manual sig to this (different) sig
                    sig.copy(manualSig);
                } else {
                    // Inconsistent sigs
                    logger.debug("Inconsistency between time sigs");
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
     * Report a suitable manually assigned time signature, if any.
     * For this, we need to find a manual time sig, after having checked that
     * all manual time sigs in the measure are consistent.
     *
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
            Staff staff = sit.next();
            TimeSignature sig = sit.getMeasure().getTimeSignature(staff);

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
                    if ((num != manualSig.getNumerator())
                        || (den != manualSig.getDenominator())) {
                        sig.addError("Inconsistent time signature");
                        manualSig.addError("Inconsistent time signature");

                        throw new InconsistentTimeSignatures();
                    }
                } catch (InvalidTimeSignature | InconsistentTimeSignatures ex) {
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
        if ((first.getShape() == Shape.NOTEHEAD_VOID)
            && (second.getShape() == Shape.NOTEHEAD_VOID)) {
            List<Glyph> glyphs = new ArrayList<>();

            glyphs.addAll(first.getGlyphs());
            glyphs.addAll(second.getGlyphs());

            SystemInfo system = first.getSystem().getInfo();
            Glyph compound = system.buildTransientCompound(glyphs);
            Evaluation vote = GlyphNetwork.getInstance().vote(
                    compound,
                    first.getSystem().getInfo(),
                    Grades.mergedNoteMinGrade);

            if (vote != null) {
                compound = system.addGlyph(compound);
                compound.setEvaluation(vote);
                logger.debug("{} merged two note heads", compound.idString());
            }
        }
    }

    //-------------//
    // searchHooks //
    //-------------//
    /**
     * Search unrecognized beam hooks among the glyphs around the
     * provided chord.
     *
     * @param chord  the provided chord
     * @param glyphs the surrounding glyphs
     */
    private void searchHooks (Chord chord,
                              Collection<Glyph> glyphs)
    {
        // Up(+1) or down(-1) stem?
        final int stemDir = chord.getStemDir();
        final Point chordCenter = chord.getCenter();
        final ScoreSystem system = chord.getSystem();
        final GlyphNetwork network = GlyphNetwork.getInstance();

        for (Glyph glyph : glyphs) {
            if (glyph.getShape() != null) {
                continue;
            }

            logger.debug("Spurious {}", glyph.idString());

            // Check we are on the tail (beam) end of the stem
            // Beware, stemDir is >0 upwards, while y is >0 downwards
            Point glyphCenter = glyph.getAreaCenter();

            if ((GeoUtil.vectorOf(chordCenter, glyphCenter).y * stemDir) > 0) {
                logger.debug("{} not on beam side", glyph.idString());

                continue;
            }

            // Check if a beam appears in the top evaluations
            Evaluation vote = network.vote(
                    glyph,
                    system.getInfo(),
                    Grades.hookMinGrade,
                    hookPredicate);

            if (vote != null) {
                glyph.setShape(vote.shape, Evaluation.ALGORITHM);

                logger.debug("{} recognized as {}",
                        glyph.idString(), vote.shape);

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

        Constant.Double minDeltaNotePitch = new Constant.Double(
                "PitchPosition",
                1.5,
                "Minimum pitch difference between note heads on same stem side");

        Scale.Fraction halfTailBoxSide = new Scale.Fraction(
                1,
                "Half side of box on stem tail to exclude notes");

    }
}
