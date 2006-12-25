//----------------------------------------------------------------------------//
//                                                                            //
//                             B e a m G r o u p                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.util.Logger;
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
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class BeamGroup
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BeamGroup.class);

    //~ Instance fields --------------------------------------------------------

    /** Id for debug mainly */
    private final int id;

    /** Containing measure */
    private final Measure measure;

    /** Ordered collection of contained beams */
    private SortedSet<Beam> beams = new TreeSet<Beam>();

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
        logger.warning(
            beam.getContextString() +
            " Unable to find beam in its group. size=" + beams.size());

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
            logger.warning(
                beam.getContextString() + " " + beam + " already in " + this);
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
        BeamGroup.Split split;

        while ((split = checkBeamGroups(measure)) != null) {
            java.lang.System.out.println("populate. processSplit " + split);
            split.group.processSplit(split);
        }

        // Dump results
        if (logger.isFineEnabled()) {
            logger.fine(measure.getContextString());

            for (BeamGroup group : measure.getBeamGroups()) {
                logger.fine("   " + group);
            }
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
            logger.warning(beam + " not found in " + this);
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

        for (Beam beam : beams) {
            sb.append(beam + " ");
        }

        sb.append("]")
          .append("}");

        return sb.toString();
    }

    //-------//
    // check //
    //-------//
    /**
     * Run a consistency check on the group, and detect when a group has to be
     * split
     *
     * @return the split parameters, or null if no split is needed
     */
    private Split check ()
    {
        // Make sure all chords are part of the same group
        // We use the fact that for any given slot, there must be at most one
        // chord for this beam group
        // Another possibility might be to use beam slope and proximity TBD
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
                            return new Split(this, beam, prevChord, chord);
                        }
                    }
                }
            }
        }

        return null; // everything is OK
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
    private static Split checkBeamGroups (Measure measure)
    {
        for (BeamGroup group : measure.getBeamGroups()) {
            java.lang.System.out.println(
                measure.getContextString() + " checkBeamGroups group=" + group);

            Split split = group.check();

            if (split != null) {
                return split;
            }
        }

        return null;
    }

    //--------------//
    // processSplit //
    //--------------//
    /**
     * Actually split a group in two, according to the split parameters
     *
     * @param split the split parameters
     */
    private void processSplit (Split split)
    {
        if (logger.isFineEnabled()) {
            logger.fine("processing " + split);
        }

        BeamGroup  alienGroup = new BeamGroup(measure);

        // Check all former beams: any beam linked to the alienChord should be
        // moved to the alienGroup as well.
        List<Beam> aliens = new ArrayList<Beam>(); // To avoid concurrent modifs

        for (Iterator<Beam> bit = this.beams.iterator(); bit.hasNext();) {
            Beam beam = bit.next();

            if (beam.getChords()
                    .contains(split.alienChord)) {
                aliens.add(beam);
            }
        }

        // Now make the switch
        for (Beam beam : aliens) {
            beam.setGroup(alienGroup);
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

    //------------//
    // splitChord //
    //------------//
    private void splitChord (Chord     chord,
                             Split     split,
                             BeamGroup alienGroup)
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

                // Remove beam from chord
                bit.remove();

                // Link beam to alienChord
                alienChord.addBeam(beam);
                beam.addChord(alienChord);

                // Switch beam group
                beam.setGroup(alienGroup);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Split //
    //-------//
    /**
     * Class <code>Split</code> records a split order. Splitting must be
     * separate from browsing to avoid concurrent modification of collections
     */
    private static class Split
    {
        final BeamGroup group;
        final Beam      alienBeam;
        final Chord     firstChord;
        final Chord     alienChord;

        public Split (BeamGroup group,
                      Beam      alienBeam,
                      Chord     firstChord,
                      Chord     alienChord)
        {
            this.group = group;
            this.alienBeam = alienBeam;
            this.firstChord = firstChord;
            this.alienChord = alienChord;
        }

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
