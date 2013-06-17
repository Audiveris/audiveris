//----------------------------------------------------------------------------//
//                                                                            //
//                             B e a m G r o u p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Line;
import omr.math.Rational;


import omr.sheet.Scale;

import omr.util.HorizontalSide;
import omr.util.Navigable;
import omr.util.TreeNode;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BeamGroup} represents a group of related beams.
 * It handles the level of each beam within the group.
 * The contained beams are sorted in increasing order from stem/chord tail to
 * stem/chord head
 *
 * @author Hervé Bitteur
 */
public class BeamGroup
        implements Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BeamGroup.class);

//    /** A Beam comparator based on level. (within the same group only) */
//    private static final Comparator<Beam> byLevel = new Comparator<Beam>()
//    {
//        @Override
//        public int compare (Beam b1,
//                            Beam b2)
//        {
//            if (b1 == b2) {
//                return 0;
//            }
//
//            // Find a common chord, and use reverse order from head location
//            for (Chord chord : b1.getChords()) {
//                if (b2.getChords().contains(chord)) {
//                    int x = chord.getStem().getLocation().x;
//                    int y = b1.getLine().yAtX(x);
//                    int yOther = b2.getLine().yAtX(x);
//                    int yHead = chord.getHeadLocation().y;
//
//                    int result = Integer.signum(
//                            Math.abs(yHead - yOther) - Math.abs(yHead - y));
//
//                    if (result == 0) {
//                        // This should not happen
//                        //                    logger.warn(
//                        //                        other.getContextString() + " equality between " +
//                        //                        this.toLongString() + " and " + other.toLongString());
//                        //                    logger.warn(
//                        //                        "Beam comparison data " + "x=" + x + " y=" + y +
//                        //                        " yOther=" + yOther + " yHead=" + yHead);
//                        b1.addError(chord.getStem(), "Weird beam configuration");
//                    }
//
//                    return result;
//                }
//            }
//
//            // No common chord
//        }
//    };
    //~ Instance fields --------------------------------------------------------
    //
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /** Id for debug mainly. */
    private final int id;

    /** Containing measure. */
    @Navigable(false)
    private final Measure measure;

    /** Collection of contained beams. */
    private List<Beam> beams = new ArrayList<>();

    /** Same voice for all chords of this beam group. */
    private Voice voice;

    //~ Constructors -----------------------------------------------------------
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
        measure.addGroup(this);
        id = measure.getBeamGroups().indexOf(this) + 1;

        logger.debug("{} Created {}", measure.getContextString(), this);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate all the BeamGroup instances for a given measure.
     *
     * @param measure the containing measure
     */
    public static void populate (Measure measure)
    {
        // Link beams to chords
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.linkChords();
        }

        // Build beam groups for this measure
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.determineGroup();
        }

        // Close the connections between chords/stems and beams
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.closeConnections();
        }

        // Separate illegal beam groups
        BeamGroup.SplitOrder split;

        // In case something goes wrong, use an upper limit to loop
        int loopNb = constants.maxSplitLoops.getValue();

        while ((split = checkBeamGroups(measure)) != null) {
            if (--loopNb < 0) {
                measure.addError("Loop detected in BeamGroup split");

                break;
            }

            split.group.splitGroup(split);
        }

        // Dump results
        if (logger.isDebugEnabled()) {
            logger.debug(measure.getContextString());

            for (BeamGroup group : measure.getBeamGroups()) {
                logger.debug("   {}", group);
            }
        }

        // Harmonize the slopes of all beams within each beam group
        for (BeamGroup group : measure.getBeamGroups()) {
            group.align();
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
    public void addBeam (Beam beam)
    {
        if (!beams.add(beam)) {
            beam.addError(beam + " already in " + this);
        }

        if (beam.isVip()) {
            setVip();
        }

        if (isVip() || logger.isDebugEnabled()) {
            logger.info("{} Added {} to {}",
                    measure.getContextString(), beam, this);
        }
    }

    //-------------------//
    // computeStartTimes //
    //-------------------//
    /**
     * Compute start times for all chords of this beam group,
     * assuming the first chord of the group already has its
     * startTime set.
     */
    public void computeStartTimes ()
    {
        Chord prevChord = null;

        for (Chord chord : getChords()) {
            if (prevChord != null) {
                try {
                    // Here we must check for interleaved rest
                    Note rest = Chord.lookupRest(prevChord, chord);

                    if (rest != null) {
                        rest.getChord().setStartTime(prevChord.getEndTime());
                        chord.setStartTime(rest.getChord().getEndTime());
                    } else {
                        chord.setStartTime(prevChord.getEndTime());
                    }
                } catch (Exception ex) {
                    chord.addError(
                            "Cannot compute chord time based on previous chord");
                }
            } else {
                if (chord.getStartTime() == null) {
                    chord.addError(
                            "Computing beam group times with first chord not set");
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
    public List<Beam> getBeams ()
    {
        return beams;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the x-ordered collection of chords that are grouped by
     * this beam group.
     *
     * @return the (perhaps empty) collection of 'beamed' chords.
     */
    public List<Chord> getChords ()
    {
        List<Chord> chords = new ArrayList<>();

        for (Beam beam : getBeams()) {
            for (Chord chord : beam.getChords()) {
                if (!chords.contains(chord)) {
                    chords.add(chord);
                }
            }
        }

        Collections.sort(chords, Chord.byAbscissa);
        return chords;
    }

    //--------------//
    // getLastChord //
    //--------------//
    /**
     * Report the last chord on the right.
     *
     * @return the last chord
     */
    public Chord getLastChord ()
    {
        List<Chord> chords = getChords();
        if (!chords.isEmpty()) {
            return chords.get(chords.size() - 1);
        } else {
            return null;
        }
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the total duration of the sequence of chords within this
     * group.
     *
     * @return the total group duration, perhaps null
     */
    public Rational getDuration ()
    {
        Rational duration = null;
        SortedSet<Chord> chords = new TreeSet<>(Chord.byAbscissa);

        for (Beam beam : beams) {
            for (Chord chord : beam.getChords()) {
                chords.add(chord);
            }
        }

        for (Chord chord : chords) {
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

    //----------//
    // getLevel //
    //----------//
    /**
     * Report the level of a beam within its containing BeamGroup.
     *
     * @param beam the given beam (assumed to be part of this group)
     * @return the beam level within the group, counted from 1
     */
    public int getLevel (Beam beam)
    {
        int level = 1;

        for (Beam b : beams) {
            if (b == beam) {
                return level;
            } else {
                level++;
            }
        }

        // This should not happen
        beam.addError("Unable to find beam in its group. size=" + beams.size());

        return 0;
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
     * Remove a beam from this group (in order to assign the beam to
     * another group).
     *
     * @param beam the beam to remove
     */
    public void removeBeam (Beam beam)
    {
        if (!beams.remove(beam)) {
            beam.addError(beam + " not found in " + this);
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
            Chord prevChord = null;

            for (Chord chord : getChords()) {
                if (prevChord != null) {
                    // Here we must check for interleaved rest
                    Note rest = Chord.lookupRest(prevChord, chord);

                    if (rest != null) {
                        rest.getChord().setVoice(voice);
                    }
                }

                chord.setVoice(voice);
                prevChord = chord;
            }
        } else if (!this.voice.equals(voice)) {
            getChords().get(0).addError(
                    "Group. Reassigning voice from " + this.voice + " to " + voice
                    + " in " + this);
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
            for (Beam beam : beams) {
                sb.append(beam).append(" ");
            }
        }

        sb.append("]").append("}");

        return sb.toString();
    }

    //-------//
    // align //
    //-------//
    /**
     * Force all beams (and beam items) to use the same slope within
     * that beam group.
     */
    private void align ()
    {
        // Retrieve the longest beam and use its slope
        double bestLength = 0;
        Beam bestBeam = null;

        for (Beam beam : beams) {
            // Extrema points of Beam hooks are not reliable, skip them
            if (beam.isHook()) {
                continue;
            }

            double length = beam.getPoint(HorizontalSide.LEFT).distance(beam.
                    getPoint(HorizontalSide.RIGHT));

            if (length > bestLength) {
                bestLength = length;
                bestBeam = beam;
            }
        }

        if (bestBeam != null) {
            double slope = bestBeam.getLine().getSlope();

            for (Beam beam : beams) {
                Point left = beam.getPoint(HorizontalSide.LEFT);
                Point right = beam.getPoint(HorizontalSide.RIGHT);
                double yMid = (left.y + right.y) / 2d;
                double dy = (right.x - left.x) * slope;
                left.y = (int) Math.rint(yMid - (dy / 2));
                right.y = (int) Math.rint(yMid + (dy / 2));
                beam.setPoint(HorizontalSide.LEFT, left);
                beam.setPoint(HorizontalSide.RIGHT, right);
            }
        }
    }

    //-----------------//
    // checkBeamGroups //
    //-----------------//
    /**
     * Check all the BeamGroup instances of the given measure, to find
     * the first split if any to perform.
     *
     * @param measure the given measure
     * @return the first split parameters, or null if everything is OK
     */
    private static SplitOrder checkBeamGroups (Measure measure)
    {
        for (BeamGroup group : measure.getBeamGroups()) {
            SplitOrder split = group.checkForSplit();

            if (split != null) {
                return split;
            }
        }

        return null;
    }

    //---------------//
    // checkForSplit //
    //---------------//
    /**
     * Run a consistency check on the group, and detect when a group
     * has to be split.
     *
     * @return the split order parameters, or null if no split is needed
     */
    private SplitOrder checkForSplit ()
    {
        // Make sure all chords are part of the same group
        // We check the vertical distance between any chord and the beams
        // above or below the chord.
        for (Chord chord : getChords()) {
            Rectangle chordBox = chord.getBox();
            for (Beam beam : beams) {
                // Beam hooks are not concerned
                if (beam.isHook()) {
                    continue;
                }
                // Skip beams attached to this chord
                if (beam.getChords().contains(chord)) {
                    continue;
                }
                // Check abscissa overlap
                Rectangle beamBox = beam.getBox();
                int xOverlap = Math.min(chordBox.x + chordBox.width,
                        beamBox.x + beamBox.width)
                               - Math.max(chordBox.x, beamBox.x);
                if (xOverlap <= 0) {
                    continue;
                }

                // Check vertical gap
                Line line = beam.getLine();
                Point tail = chord.getTailLocation();
                int lineY = line.yAtX(tail.x);
                int yOverlap = Math.min(lineY, chordBox.y + chordBox.height)
                               - Math.max(lineY, chordBox.y);
                if (yOverlap >= 0) {
                    continue;
                }
                int tailDy = Math.abs(lineY - tail.y);
                double normedDy = chord.getScale().pixelsToFrac(tailDy);
                double maxChordDy = constants.maxChordDy.getValue();
                if (normedDy > maxChordDy) {
                    logger.debug("Vertical gap between {} and {}, {} vs {}",
                            chord, beam, normedDy, maxChordDy);
                    // Split the beam group here
                    return new SplitOrder(this, chord);
                }
            }

        }

        return null; // everything is OK
    }

    //------------//
    // splitChord //
    //------------//
    /**
     * We actually split the chord which embraces the two beam groups.
     * At this point, each beam has been moved to its proper group, either
     * this (old) group or the (new) alienGroup.
     * What remains to be done is to split the pivot chord between the two.
     *
     * @param pivotChord the chord to split
     * @param alienGroup the new beam group (based on alienChord)
     */
    private void splitChord (Chord pivotChord,
                             BeamGroup alienGroup)
    {
        logger.debug("Shared : {}", pivotChord);

        // Create a clone of pivotChord (w/o any beam initially)
        Chord cloneChord = pivotChord.duplicate();

        List<Beam> alienBeams = alienGroup.getBeams();
        for (Iterator<Beam> bit = pivotChord.getBeams().iterator(); bit.hasNext();) {
            Beam beam = bit.next();

            if (alienBeams.contains(beam)) {
                // Cut the link chord -> beam
                bit.remove();
                // Cut the link chord <- beam
                beam.removeChord(pivotChord);

                // Link beam to cloneChord
                cloneChord.addBeam(beam);
                beam.addChord(cloneChord);
            }
        }

        logger.debug("Remaining : {}", pivotChord);
        logger.debug("Alien : {}", cloneChord);
    }

    //------------//
    // splitGroup //
    //------------//
    /**
     * Actually split a group in two, according to the split parameters.
     *
     * @param split the split parameters
     */
    private void splitGroup (SplitOrder split)
    {
        logger.debug("processing {}", split);

        // The group on alienChord side
        BeamGroup chordGroup = new BeamGroup(measure);

        // Check all former beams: any beam linked to the alienChord should be
        // moved to the chordGroup.
        List<Beam> chordBeams = new ArrayList<>(); // To avoid concurrent modifs

        for (Beam beam : beams) {
            if (beam.getChords().contains(split.alienChord)) {
                chordBeams.add(beam);
            }
        }

        // Now make the switch
        for (Beam beam : chordBeams) {
            beam.switchToGroup(chordGroup);
        }

        // Detect the chord which is shared by the two groups
        // And duplicate this chord for both groups
        for (Beam aBeam : chordGroup.beams) {
            for (Chord aChord : aBeam.getChords()) {
                // Compare with chords left in group
                for (Beam beam : this.beams) {
                    for (Chord chord : beam.getChords()) {
                        // Is this (alien) chord also part of the old Group ?
                        if (chord == aChord) {
                            splitChord(chord, chordGroup);

                            return;
                        }
                    }
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

        Constant.Integer maxSplitLoops = new Constant.Integer(
                "loops",
                10,
                "Maximum number of loops allowed for splitting beam groups");

        Scale.Fraction maxChordDy = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between a chord and a beam");

    }

    //------------//
    // SplitOrder //
    //------------//
    /**
     * Class {@code SplitOrder} records a beam group split order.
     * Splitting must be separate from browsing to avoid concurrent modification
     * of collections
     */
    private static class SplitOrder
    {
        //~ Instance fields ----------------------------------------------------

        /** The beam group to be split. */
        final BeamGroup group;

        /** A chord of this group, where multiplicity was detected. */
        final Chord alienChord;

        //~ Constructors -------------------------------------------------------
        public SplitOrder (BeamGroup group,
                           Chord alienChord)
        {
            this.group = group;
            this.alienChord = alienChord;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Split");
            sb.append("\n\tgroup=").append(group);
            sb.append("\n\talienChord=").append(alienChord);
            sb.append("}");

            return sb.toString();
        }
    }
}
