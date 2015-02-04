//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m G r o u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.GeoUtil;
import omr.math.LineUtil;
import omr.math.Rational;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.relation.BeamHeadRelation;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.Relation;

import omr.util.Navigable;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BeamGroup} represents a group of related beams.
 * <p>
 * It handles the level of each beam within the group.
 * The contained beams are sorted in increasing order from stem/chord tail to stem/chord head
 *
 * @author Hervé Bitteur
 */
public class BeamGroup
        implements Vip
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamGroup.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /** Id for debug mainly, unique within measure stack. */
    private final int id;

    /** Containing measure. */
    @Navigable(false)
    private final Measure measure;

    /** Collection of contained beams. */
    private final List<AbstractBeamInter> beams = new ArrayList<AbstractBeamInter>();

    /** Same voice for all chords in this beam group. */
    private Voice voice;

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // BeamGroup //
    //-----------//
    /**
     * Creates a new instance of BeamGroup.
     *
     * @param measure the containing measure
     */
    public BeamGroup (Measure measure)
    {
        this.measure = measure;

        MeasureStack stack = measure.getStack();
        stack.addGroup(this);
        id = stack.getBeamGroups().indexOf(this) + 1;

        logger.debug("{} Created {}", measure, this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate all the BeamGroup instances for a given measure stack.
     *
     * @param stack the containing measure stack
     */
    public static void populate (MeasureStack stack)
    {
        // Retrieve beams in this measure
        Set<AbstractBeamInter> beams = new HashSet<AbstractBeamInter>();

        for (ChordInter chord : stack.getChords()) {
            beams.addAll(chord.getBeams());
        }

        // Build beam groups for this measure stack
        for (AbstractBeamInter beam : beams) {
            if (beam.getGroup() == null) {
                Measure measure = beam.getChords().get(0).getMeasure();
                BeamGroup group = new BeamGroup(measure);
                assignGroup(group, beam);
                logger.debug("{}", group);
            }
        }

        // In case something goes wrong, use an upper limit to loop
        int loopNb = constants.maxSplitLoops.getValue();

        while (checkBeamGroups(stack)) {
            if (--loopNb < 0) {
                logger.warn("Loop detected in BeamGroup split in {}", stack);

                break;
            }
        }

        // Dump results
        if (logger.isDebugEnabled()) {
            for (BeamGroup group : stack.getBeamGroups()) {
                logger.debug("   {}", group);
            }
        }
    }

    //---------//
    // addBeam //
    //---------//
    /**
     * Include a beam as part of this group.
     *
     * @param beam the beam to include
     */
    public void addBeam (AbstractBeamInter beam)
    {
        if (!beams.add(beam)) {
            logger.warn("{} already in {}", beam, this);
        }

        if (beam.isVip()) {
            setVip();
        }

        if (isVip() || logger.isDebugEnabled()) {
            logger.info("{} Added {} to {}", measure, beam, this);
        }
    }

    //-------------------//
    // computeStartTimes //
    //-------------------//
    /**
     * Compute start times for all chords of this beam group, assuming the first chord
     * of the group already has its startTime set.
     */
    public void computeStartTimes ()
    {
        ChordInter prevChord = null;

        for (ChordInter chord : getChords()) {
            if (prevChord != null) {
                try {
                    // Here we must check for interleaved rest
                    AbstractNoteInter rest = measure.getStack().lookupRest(prevChord, chord);

                    if (rest != null) {
                        rest.getChord().setStartTime(prevChord.getEndTime());
                        chord.setStartTime(rest.getChord().getEndTime());
                    } else {
                        chord.setStartTime(prevChord.getEndTime());
                    }
                } catch (Exception ex) {
                    logger.warn("{} Cannot compute chord time based on previous chord", chord);
                }
            } else {
                if (chord.getStartTime() == null) {
                    logger.warn("{} Computing beam group times with first chord not set", chord);
                }
            }

            prevChord = chord;
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the beams that are part of this group.
     *
     * @return the collection of contained beams
     */
    public List<AbstractBeamInter> getBeams ()
    {
        return beams;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the total duration of the sequence of chords within this group.
     *
     * @return the total group duration, perhaps null
     */
    public Rational getDuration ()
    {
        Rational duration = null;
        SortedSet<ChordInter> chords = new TreeSet<ChordInter>(ChordInter.byAbscissa);

        for (AbstractBeamInter beam : beams) {
            for (ChordInter chord : beam.getChords()) {
                chords.add(chord);
            }
        }

        for (ChordInter chord : chords) {
            Rational dur = chord.getDuration();

            if (dur != null) {
                if (duration != null) {
                    duration = duration.plus(dur);
                } else {
                    duration = dur;
                }
            }
        }

        return duration;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the group id (unique within the measure, starting from 1).
     *
     * @return the group id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getLastChord //
    //--------------//
    /**
     * Report the last chord on the right.
     *
     * @return the last chord
     */
    public ChordInter getLastChord ()
    {
        List<ChordInter> chords = getChords();

        if (!chords.isEmpty()) {
            return chords.get(chords.size() - 1);
        } else {
            return null;
        }
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Report the level of a beam within its containing BeamGroup.
     *
     * @param beam the given beam (assumed to be part of this group)
     * @return the beam level within the group, counted from 1
     */
    public int getLevel (AbstractBeamInter beam)
    {
        int level = 1;

        for (AbstractBeamInter b : beams) {
            if (b == beam) {
                return level;
            } else {
                level++;
            }
        }

        // This should not happen
        logger.warn("Unable to find beam {} in its group. size=", beam, beams.size());

        return 0;
    }

    //----------//
    // getVoice //
    //----------//
    public Voice getVoice ()
    {
        return voice;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //------------//
    // removeBeam //
    //------------//
    /**
     * Remove a beam from this group (in order to assign the beam to another group).
     *
     * @param beam the beam to remove
     */
    public void removeBeam (AbstractBeamInter beam)
    {
        if (!beams.remove(beam)) {
            logger.warn(beam + " not found in " + this);
        }
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Assign a voice to this beam group.
     *
     * @param voice the voice to assign
     */
    public void setVoice (Voice voice)
    {
        // Already done?
        if (this.voice == null) {
            this.voice = voice;

            // Formard this information to the beamed chords
            // Including the interleaved rests if any
            ChordInter prevChord = null;

            for (ChordInter chord : getChords()) {
                if (prevChord != null) {
                    // Here we must check for interleaved rest
                    AbstractNoteInter rest = measure.getStack().lookupRest(prevChord, chord);

                    if (rest != null) {
                        rest.getChord().setVoice(voice);
                    }
                }

                chord.setVoice(voice);
                prevChord = chord;
            }
        } else {
            if (voice == null) {
                this.voice = null;
            } else if (!this.voice.equals(voice)) {
                logger.warn(
                        "Reassigning voice from " + this.voice + " to " + voice + " in " + this);
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{BeamGroup#").append(id).append(" beams[");

        if (beams != null) {
            for (AbstractBeamInter beam : beams) {
                sb.append(beam).append(" ");
            }
        }

        sb.append("]").append("}");

        return sb.toString();
    }

    //-------------//
    // resetRhythm //
    //-------------//
    void resetTiming ()
    {
        voice = null;
    }

    //----------------//
    // determineGroup //
    //----------------//
    /**
     * Recursively determine BeamGroup for the provided beam, as well as all other beams
     * connected within the same group.
     *
     * @param beam    the beam seed
     * @param measure the containing measure
     */
    private static void assignGroup (BeamGroup group,
                                     AbstractBeamInter beam)
    {
        group.addBeam(beam);
        beam.setGroup(group);

        for (ChordInter chord : beam.getChords()) {
            for (AbstractBeamInter b : chord.getBeams()) {
                if (b.getGroup() == null) {
                    assignGroup(group, b);
                }
            }
        }
    }

    //
    //    //-----------------//
    //    // checkBeamGroups //
    //    //-----------------//
    //    /**
    //     * Check all the BeamGroup instances of the given measure, to find the first split
    //     * if any to perform.
    //     *
    //     * @param measure the given measure
    //     * @return the first split parameters, or null if everything is OK
    //     */
    //    private static boolean checkBeamGroups (Measure measure)
    //    {
    //        for (BeamGroup group : measure.getBeamGroups()) {
    //            ChordInter alienChord = group.checkForSplit();
    //
    //            if (alienChord != null) {
    //                group.split(alienChord);
    //
    //                return true;
    //            }
    //        }
    //
    //        return false;
    //    }
    //
    //-----------------//
    // checkBeamGroups //
    //-----------------//
    /**
     * Check all the BeamGroup instances of the given measure stack, to find the first
     * split if any to perform.
     *
     * @param stack the given measure stack
     * @return the first split parameters, or null if everything is OK
     */
    private static boolean checkBeamGroups (MeasureStack stack)
    {
        for (BeamGroup group : stack.getBeamGroups()) {
            ChordInter alienChord = group.checkForSplit();

            if (alienChord != null) {
                group.split(alienChord);

                return true;
            }
        }

        return false;
    }

    //---------------//
    // checkForSplit //
    //---------------//
    /**
     * Run a consistency check on the group, and detect when a group has to be split.
     *
     * @return the detected alien chord, or null if no split is needed
     */
    private ChordInter checkForSplit ()
    {
        final Scale scale = measure.getPart().getSystem().getSheet().getScale();
        final double maxChordDy = constants.maxChordDy.getValue();

        // Make sure all chords are part of the same group
        // We check the vertical distance between any chord and the beams above or below the chord.
        for (ChordInter chord : getChords()) {
            Rectangle chordBox = chord.getBounds();

            for (AbstractBeamInter beam : beams) {
                // Beam hooks are not concerned
                if (beam.isHook()) {
                    continue;
                }

                // Skip beams attached to this chord
                if (beam.getChords().contains(chord)) {
                    continue;
                }

                // Check abscissa overlap
                if (GeoUtil.xOverlap(beam.getBounds(), chordBox) <= 0) {
                    continue;
                }

                // Check vertical gap
                Line2D line = beam.getMedian();
                Point tail = chord.getTailLocation();
                int lineY = (int) Math.rint(LineUtil.intersectionAtX(line, tail.x).getY());
                int yOverlap = Math.min(lineY, chordBox.y + chordBox.height)
                               - Math.max(lineY, chordBox.y);

                if (yOverlap >= 0) {
                    continue;
                }

                int tailDy = Math.abs(lineY - tail.y);
                double normedDy = scale.pixelsToFrac(tailDy);

                if (normedDy > maxChordDy) {
                    logger.debug(
                            "Vertical gap between {} and {}, {} vs {}",
                            chord,
                            beam,
                            normedDy,
                            maxChordDy);

                    // Split the beam group here
                    return chord;
                }
            }
        }

        return null; // everything is OK
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the x-ordered collection of chords that are grouped by this beam group.
     *
     * @return the (perhaps empty) collection of 'beamed' chords.
     */
    private List<ChordInter> getChords ()
    {
        List<ChordInter> chords = new ArrayList<ChordInter>();

        for (AbstractBeamInter beam : getBeams()) {
            for (ChordInter chord : beam.getChords()) {
                if (!chords.contains(chord)) {
                    chords.add(chord);
                }
            }
        }

        Collections.sort(chords, ChordInter.byAbscissa);

        return chords;
    }

    private void split (ChordInter alienChord)
    {
        new Splitter(alienChord).process();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Integer maxSplitLoops = new Constant.Integer(
                "loops",
                10,
                "Maximum number of loops allowed for splitting beam groups");

        Scale.Fraction maxChordDy = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between a chord and a beam");
    }

    //----------//
    // Splitter //
    //----------//
    /**
     * Utility class meant to perform a split on this group.
     * This group is shrunk, because some of its beams are moved to a new (alien) group.
     */
    private class Splitter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Chord detected as belonging to a (new) alien group. */
        private final ChordInter alienChord;

        /** Beams that belong to new alien group.
         * (Initially populated with all beams attached to alienChord) */
        private List<AbstractBeamInter> alienBeams;

        /** The new alien group. */
        private BeamGroup alienGroup;

        /** The chord that embraces both (old) group and (new) alien group. */
        private HeadChordInter pivotChord;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a splitter for this BeamGroup, triggered by alienChord
         *
         * @param alienChord a detected chord that should belong to a separate group
         */
        public Splitter (ChordInter alienChord)
        {
            this.alienChord = alienChord;
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // process //
        //---------//
        /**
         * Actually split the group in two, around the detected alien chord.
         * <p>
         * Some beams of this group instance are moved to a new separate BeamGroup instance.
         * The two instances are articulated around a pivot chord, common to both groups.
         *
         */
        public void process ()
        {
            logger.debug("{} splitter on {}", BeamGroup.this, alienChord);

            // The new group on alienChord side
            alienGroup = createAlienGroup();

            // Detect the pivot chord which is shared by the two groups, and "split" it for both groups
            pivotChord = detectPivotChord();

            // Dispatch beams attached to pivotChord to their proper group
            dispatchPivotBeams();

            // Make sure all beams have been dispatched
            dispatchAllBeams();

            // Duplicate the chord between the two group
            splitChord();
        }

        //------------------//
        // createAlienGroup //
        //------------------//
        private BeamGroup createAlienGroup ()
        {
            alienGroup = new BeamGroup(measure);

            // Check all former beams: any beam linked to the detected alienChord should be
            // moved to the alienGroup.
            // (This cannot apply to beam hooks, they will be processed later)
            alienBeams = new ArrayList<AbstractBeamInter>(alienChord.getBeams());

            // Now apply the move
            for (AbstractBeamInter beam : alienBeams) {
                beam.switchToGroup(alienGroup);
            }

            return alienGroup;
        }

        //------------------//
        // detectPivotChord //
        //------------------//
        /**
         * Look through the chords on the alienGroup to detect the one which is shared by
         * this group and the alienGroup
         *
         * @return the pivot chord found
         */
        private HeadChordInter detectPivotChord ()
        {
            List<ChordInter> commons = getChords();
            commons.retainAll(alienGroup.getChords());

            // TODO: what if we have more than one common chord???
            return (HeadChordInter) commons.get(0);
        }

        //------------------//
        // dispatchAllBeams //
        //------------------//
        /**
         * Inspect all remaining beams in (old) group, and move to the (new) alien group
         * the ones which are connected to alien beams (except through the pivotChord).
         */
        private void dispatchAllBeams ()
        {
            List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            AllLoop:
            for (AbstractBeamInter beam : new ArrayList<AbstractBeamInter>(beams)) {
                // If beam is attached to pivotChord, skip it
                if (pivotBeams.contains(beam)) {
                    continue;
                }

                // Check every beam chord, for touching an alienBeam
                for (ChordInter chord : beam.getChords()) {
                    for (AbstractBeamInter b : chord.getBeams()) {
                        if (b.getGroup() == alienGroup) {
                            beam.switchToGroup(alienGroup);

                            continue AllLoop;
                        }
                    }
                }
            }
        }

        //--------------------//
        // dispatchPivotBeams //
        //--------------------//
        /**
         * Inspect the beams connected to pivotChord, and move to the (new) alien group
         * those which fall on the alienSide of the pivotChord.
         */
        private void dispatchPivotBeams ()
        {
            final AbstractBeamInter alienBeam = alienChord.getBeams().get(0);
            final List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            Boolean onAlienSide = null;

            for (int ib = 0; ib < pivotBeams.size(); ib++) {
                AbstractBeamInter b = pivotChord.getBeams().get(ib);

                if (b.isHook()) {
                    continue;
                }

                if (onAlienSide == null) {
                    onAlienSide = alienBeams.contains(b);
                }

                if (b == alienBeam) {
                    if (onAlienSide) {
                        // End of alien side
                        logger.debug("Alien end");

                        for (AbstractBeamInter ab : pivotBeams.subList(0, ib + 1)) {
                            if (!alienBeams.contains(ab)) {
                                ab.switchToGroup(alienGroup);
                            }
                        }
                    } else {
                        // Start of alien side
                        logger.debug("Alien start");

                        for (AbstractBeamInter ab : pivotBeams.subList(
                                ib,
                                pivotChord.getBeams().size())) {
                            if (!alienBeams.contains(ab)) {
                                ab.switchToGroup(alienGroup);
                            }
                        }
                    }

                    return;
                }
            }
        }

        //------------//
        // splitChord //
        //------------//
        /**
         * Split the chord which embraces the two beam groups.
         * <p>
         * At this point, each beam has been moved to its proper group, either this (old) group or
         * the (new) alienGroup. What remains to be done is to split the pivot chord between the
         * two groups.
         */
        private void splitChord ()
        {
            logger.debug("splitChord: {}", pivotChord);

            // Create a clone of pivotChord (w/o any beam initially)
            HeadChordInter mirrorChord = pivotChord.duplicate(true);

            // Update alienBeams
            alienBeams = alienGroup.getBeams();

            for (Iterator<AbstractBeamInter> bit = pivotChord.getBeams().iterator(); bit.hasNext();) {
                AbstractBeamInter beam = bit.next();

                if (alienBeams.contains(beam)) {
                    // Move BeamStem relation from pivot to mirror
                    SIGraph sig = beam.getSig();
                    Relation bs = sig.getRelation(
                            beam,
                            pivotChord.getStem(),
                            BeamStemRelation.class);
                    sig.removeEdge(bs);
                    sig.addEdge(beam, mirrorChord.getStem(), bs);

                    // Move BeamHead relation(s) from pivot to mirror
                    Set<Relation> bhRels = sig.getRelations(beam, BeamHeadRelation.class);

                    for (Relation bh : bhRels) {
                        Inter head = sig.getOppositeInter(beam, bh);

                        if (head.getEnsemble() == pivotChord) {
                            sig.removeEdge(bh);
                            sig.addEdge(beam, head.getMirror(), bh);
                        }
                    }

                    // Cut the links pivotChord <-> beam
                    bit.remove();
                    beam.removeChord(pivotChord);

                    // Link beam to mirrorChord
                    mirrorChord.addBeam(beam);
                    beam.addChord(mirrorChord);
                }
            }

            measure.addInter(mirrorChord);
        }
    }
}
