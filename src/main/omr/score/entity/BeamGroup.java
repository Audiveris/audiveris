//----------------------------------------------------------------------------//
//                                                                            //
//                             B e a m G r o u p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.SystemPoint;

import omr.util.Navigable;
import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>BeamGroup</code> represents a group of related beams. It handles
 * the level of each beam within the group. The contained beams are ordered in
 * increasing order from stem/chord tail to stem/chord head
 *
 * @author Hervé Bitteur
 */
public class BeamGroup
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BeamGroup.class);

    //~ Instance fields --------------------------------------------------------

    /** Id for debug mainly */
    private final int id;

    /** Containing measure */
    @Navigable(false)
    private final Measure measure;

    /** Ordered collection of contained beams */
    private SortedSet<Beam> beams = new TreeSet<Beam>();

    /** Same voice for all chords of this beam group */
    private Voice voice;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // BeamGroup //
    //-----------//
    /**
     * Creates a new instance of BeamGroup
     * @param measure the containing measure
     */
    public BeamGroup (Measure measure)
    {
        this.measure = measure;
        measure.addGroup(this);
        id = measure.getBeamGroups()
                    .indexOf(this) + 1;
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the ordered set of beams that are part of this group
     *
     * @return the ordered set of contained beams
     */
    public SortedSet<Beam> getBeams ()
    {
        return beams;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the x-ordered collection of chords that are grouped by this
     * beam group.
     *
     * @return the (perhaps empty) collection of 'beamed' chords.
     */
    public SortedSet<Chord> getChords ()
    {
        SortedSet<Chord> chords = new TreeSet<Chord>();

        for (Beam beam : getBeams()) {
            for (Chord chord : beam.getChords()) {
                chords.add(chord);
            }
        }

        return chords;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the group id (unique within the measure, starting from 1)
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
     * Report the level of a beam within its containing BeamGroup
     *
     * @param beam the given beam (assumed to be part of this group)
     * @return the beam level within the group
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

    //---------//
    // addBeam //
    //---------//
    /**
     * Include a beam as part of this group
     *
     * @param beam the beam to include
     */
    public void addBeam (Beam beam)
    {
        if (!beams.add(beam)) {
            beam.addError(beam + " already in " + this);
        }
    }

    //-------------------//
    // computeStartTimes //
    //-------------------//
    /**
     * Compute start times for all chords of this beam group, assuming the first
     * chord of the group already has its startTime set
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
                        rest.getChord()
                            .setStartTime(prevChord.getEndTime());
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
    // populate //
    //----------//
    /**
     * Populate all the BeamGroup instances for a given measure
     *
     * @param measure the containing measure
     */
    public static void populate (Measure measure)
    {
        // Allocate beams to chords
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.linkChords();
        }

        // Build beam groups for this measure
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.determineGroup();
        }

        // Separate illegal beam groups
        BeamGroup.SplitOrder split;

        int                  loopNb = constants.maxSplitLoops.getValue();

        while ((split = checkBeamGroups(measure)) != null) {
            if (--loopNb < 0) {
                measure.addError("Loop detected in BeamGroup split");

                break;
            }

            ///  java.lang.System.out.println("populate. processSplit " + split);
            split.group.splitGroup(split);
        }

        // Dump results
        if (logger.isFineEnabled()) {
            logger.fine(measure.getContextString());

            for (BeamGroup group : measure.getBeamGroups()) {
                logger.fine("   " + group);
            }
        }

        // Close the connections between chords/stems and beams
        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;
            beam.closeConnections();
        }

        // Harmonize the slopes of all beams within each beam group
        for (BeamGroup group : measure.getBeamGroups()) {
            group.align();
        }
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Assign a voice to this beam group
     *
     * @param voice the voice to assign
     */
    public void setVoice (Voice voice)
    {
        // Already done?
        if (this.voice == null) {
            this.voice = voice;

            // Extend this information to the beamed chords
            // Including the interleaved rests if any
            Chord prevChord = null;

            for (Chord chord : getChords()) {
                if (prevChord != null) {
                    // Here we must check for interleaved rest
                    Note rest = Chord.lookupRest(prevChord, chord);

                    if (rest != null) {
                        rest.getChord()
                            .setVoice(voice);
                    }
                }

                chord.setVoice(voice);
                prevChord = chord;
            }
        } else if (!this.voice.equals(voice)) {
            getChords()
                .first()
                .addError(
                "Group. Reassigning voice from " + this.voice + " to " + voice +
                " in " + this);
        }
    }

    //------------//
    // removeBeam //
    //------------//
    /**
     * Remove a beam from this group (in order to assign the beam to another
     * group)
     *
     * @param beam the beam to remove
     */
    public void removeBeam (Beam beam)
    {
        if (!beams.remove(beam)) {
            beam.addError(beam + " not found in " + this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{BeamGroup")
          .append(" #")
          .append(id)
          .append(" beams[");

        if (beams != null) {
            for (Beam beam : beams) {
                sb.append(beam + " ");
            }
        }

        sb.append("]")
          .append("}");

        return sb.toString();
    }

    //-----------------//
    // checkBeamGroups //
    //-----------------//
    /**
     * Check all the BeamGroup instances of the given measure, to find the first
     * split if any to perform
     *
     * @param measure the given measure
     * @return the first split parameters, or null if everything is OK
     */
    private static SplitOrder checkBeamGroups (Measure measure)
    {
        for (BeamGroup group : measure.getBeamGroups()) {
            SplitOrder split = group.checkGroup();

            if (split != null) {
                return split;
            }
        }

        return null;
    }

    //------------//
    // checkGroup //
    //------------//
    /**
     * Run a consistency check on the group, and detect when a group has to be
     * split
     *
     * @return the split order parameters, or null if no split is needed
     */
    private SplitOrder checkGroup ()
    {
        // Make sure all chords are part of the same group
        // We use the fact that for any given slot, there must be at most one
        // chord for this beam group
        // Another possibility might be to use beam slope and proximity TODO
        for (Slot slot : measure.getSlots()) {
            Chord prevChord = null;

            for (Beam beam : this.beams) {
                for (Chord chord : beam.getChords()) {
                    if (slot.getChords()
                            .contains(chord)) {
                        if (prevChord == null) {
                            prevChord = chord;
                        } else if (prevChord != chord) {
                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    measure + " Suspicious BeamGroup " + this);
                            }

                            // Split the beam group here
                            return new SplitOrder(this, beam, prevChord, chord);
                        }
                    }
                }
            }
        }

        return null; // everything is OK
    }

    //------------//
    // splitChord //
    //------------//
    /**
     * We actually split a chord which embraces the two beam groups
     *
     * @param chord the chord to split
     * @param split the split parameters
     * @param alienGroup the alien beam group
     */
    private void splitChord (Chord      chord,
                             SplitOrder split,
                             BeamGroup  alienGroup)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Shared : " + chord);
        }

        Chord   alienChord = chord.duplicate();

        // Split beams properly between chord & alienChord
        boolean started = false;

        for (Iterator<Beam> bit = chord.getBeams()
                                       .iterator(); bit.hasNext();) {
            Beam beam = bit.next();

            if (!started && (beam == split.alienBeam)) {
                started = true;
            }

            if (started) {
                if (logger.isFineEnabled()) {
                    logger.fine("Beam to switch: " + beam.toLongString());
                }

                // Cut the link chord -> beam
                bit.remove();

                // Cut the link chord <- beam
                beam.removeChord(chord);

                // Link beam to alienChord
                alienChord.addBeam(beam);
                beam.addChord(alienChord);

                // Switch beam group
                beam.switchGroup(alienGroup);
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("Remaining : " + chord);
            logger.fine("Alien : " + alienChord);
        }
    }

    //------------//
    // splitGroup //
    //------------//
    /**
     * Actually split a group in two, according to the split parameters
     *
     * @param split the split parameters
     */
    private void splitGroup (SplitOrder split)
    {
        if (logger.isFineEnabled()) {
            logger.fine("processing " + split);
        }

        BeamGroup  alienGroup = new BeamGroup(measure);

        // Check all former beams: any beam linked to the alienChord should be
        // moved to the alienGroup as well.
        List<Beam> alienBeams = new ArrayList<Beam>(); // To avoid concurrent modifs

        for (Beam beam : beams) {
            if (beam.getChords()
                    .contains(split.alienChord)) {
                alienBeams.add(beam);
            }
        }

        // Now make the switch
        for (Beam beam : alienBeams) {
            beam.switchGroup(alienGroup);
        }

        // Detect the chord which is shared by the two groups
        // And duplicate this chord for both groups
        for (Beam aBeam : alienGroup.beams) {
            for (Chord aChord : aBeam.getChords()) {
                for (Beam beam : this.beams) {
                    for (Chord chord : beam.getChords()) {
                        // Is this (alien) chord also part of the old Group ?
                        if (chord == aChord) {
                            splitChord(chord, split, alienGroup);

                            return;
                        }
                    }
                }
            }
        }
    }

    //-------//
    // align //
    //-------//
    /**
     * Force all beams (and beam items) to use the same slope within that beam
     * group
     */
    private void align ()
    {
        // Retrieve the longest beam and use its slope
        double bestLength = 0;
        Beam   bestBeam = null;

        for (Beam beam : beams) {
            // Extrema points of Beam hooks are not reliable, skip them
            if (beam.isHook()) {
                continue;
            }

            double length = beam.getLeftPoint()
                                .distance(beam.getRightPoint());

            if (length > bestLength) {
                bestLength = length;
                bestBeam = beam;
            }
        }

        if (bestBeam != null) {
            double slope = bestBeam.getLine()
                                   .getSlope();

            for (Beam beam : beams) {
                SystemPoint left = beam.getLeftPoint();
                SystemPoint right = beam.getRightPoint();
                double      yMid = (left.y + right.y) / 2d;
                double      dy = (right.x - left.x) * slope;
                left.y = (int) Math.rint(yMid - (dy / 2));
                right.y = (int) Math.rint(yMid + (dy / 2));
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

        /**
         * Maximum number of loops allowed for splitting beam groups
         */
        Constant.Integer maxSplitLoops = new Constant.Integer(
            "loops",
            10,
            "Maximum number of loops allowed for splitting beam groups");
    }

    //------------//
    // SplitOrder //
    //------------//
    /**
     * Class <code>SplitOrder</code> records a beam group split order.
     * Splitting must be separate from browsing to avoid concurrent modification
     * of collections
     */
    private static class SplitOrder
    {
        //~ Instance fields ----------------------------------------------------

        /** The beam group to be split */
        final BeamGroup group;

        /** The beam of this group to feed an alien group */
        final Beam alienBeam;

        /** The first chord in the slot */
        final Chord firstChord;

        /** The second chord in the same slot, meant for the alien group */
        final Chord alienChord;

        //~ Constructors -------------------------------------------------------

        public SplitOrder (BeamGroup group,
                           Beam      alienBeam,
                           Chord     firstChord,
                           Chord     alienChord)
        {
            this.group = group;
            this.alienBeam = alienBeam;
            this.firstChord = firstChord;
            this.alienChord = alienChord;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Split");
            sb.append("\n\tgroup=")
              .append(group);
            sb.append("\n\tfirstChord=")
              .append(firstChord);
            sb.append("\n\talienBeam=")
              .append(alienBeam);
            sb.append("\n\talienChord=")
              .append(alienChord);
            sb.append("}");

            return sb.toString();
        }
    }
}
