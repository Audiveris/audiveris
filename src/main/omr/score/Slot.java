//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l o t                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.Population;

import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>Slot</code> represents a roughly defined time slot within a
 * measure, to gather all chord entities (rests, notes, noteheads) that occur at
 * the same time because their abscissae are roughly the same.
 * <p><img src="doc-files/Slot.jpg" alt="diagram">
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Slot
    implements Comparable<Slot>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Slot.class);

    /** Specific comparator to sort chords within a slot */
    private static final Comparator<Chord> chordComparator = new Comparator<Chord>() {
        public int compare (Chord c1,
                            Chord c2)
        {
            Note n1 = (Note) c1.getNotes()
                               .get(0);
            Note n2 = (Note) c2.getNotes()
                               .get(0);

            // First : staff
            int dStaff = n1.getStaff()
                           .getId() - n2.getStaff()
                                        .getId();

            if (dStaff != 0) {
                return Integer.signum(dStaff);
            }

            // Second : head ordinate
            int dHead = c1.getHeadLocation().y - c2.getHeadLocation().y;

            if (dHead != 0) {
                return Integer.signum(dHead);
            }

            // Third : chord id
            return Integer.signum(c1.getId() - c2.getId());
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Abscissa of the slot, in units since system start */
    private Integer x;

    /** The containing measure */
    private Measure measure;

    /** Collection of glyphs in the slot */
    private List<Glyph> glyphs = new ArrayList<Glyph>();

    /** (Sorted) collection of chords in this slot */
    private List<Chord> chords = new ArrayList<Chord>();

    /** Time offset since measure start */
    private Integer offset;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Slot //
    //------//
    /**
     * Creates a new Slot object.
     *
     * @param measure the containing measure
     */
    public Slot (Measure measure)
    {
        this.measure = measure;
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // addGlyph //
    //----------//
    /**
     * Insert a glyph (supposedly from a chord) into this slot, invalidating the
     * internal computed data
     *
     * @param glyph the glyph to insert
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        x = null;
    }

    //------------------------//
    // allocateChordsAndNotes //
    //------------------------//
    /**
     * Based on the current collection of glyphs within this slot, allocate the
     * proper chords, a strategy based on each glyph-related stem.
     */
    public void allocateChordsAndNotes ()
    {
        // Allocate 1 chord per stem, per rest, per (whole) note
        for (Glyph glyph : glyphs) {
            if (glyph.getStemNumber() > 0) {
                // Beware of noteheads with 2 stems, we need to duplicate them
                // in order to actually have two chords.
                if (glyph.getLeftStem() != null) {
                    Chord chord = getStemChord(glyph.getLeftStem());
                    Note.createPack(chord, glyph);
                }

                if (glyph.getRightStem() != null) {
                    Chord chord = getStemChord(glyph.getRightStem());
                    Note.createPack(chord, glyph);
                }
            } else {
                Chord chord = new Chord(measure);
                chords.add(chord);
                Note.createPack(chord, glyph);
            }
        }
    }

    //-------------//
    // buildVoices //
    //-------------//
    /**
     * This static method browses a measure with its slots and chords, in order
     * to compute the various voices.
     *
     * @param measure the measure to process
     */
    public static void buildVoices (Measure measure)
    {
        if (logger.isFineEnabled()) {
            logger.info(measure.getContextString());
        }

        // Map to go from a BeamGroup to its related voice
        Map<BeamGroup, Integer> groupVoices = new HashMap<BeamGroup, Integer>();

        // activeChords gathers the chords that are "active" (not terminated) at
        // the time slot being considered. Initially, it is empty.
        List<Chord>             activeChords = new ArrayList<Chord>();
        Slot                    prevSlot = null;
        int                     maxVoice = 0;

        for (Slot slot : measure.getSlots()) {
            // Sorted Map of voice -> chord, for the current slot
            SortedMap<Integer, Chord> voiceMap = new TreeMap<Integer, Chord>();

            // Slot offset
            if (prevSlot != null) {
                slot.setOffset(
                    prevSlot.getOffset() + prevSlot.getShortestDuration());
            } else {
                slot.setOffset(0);
            }

            if (logger.isFineEnabled()) {
                logger.fine("." + slot);
            }

            // Sort chords within the slot
            Collections.sort(slot.getChords(), chordComparator);

            // Purge collection of active chords for this slot
            for (Chord chord : slot.getChords()) {
                chord.setEndTime(slot.getOffset() + chord.getDuration());
                activeChords.add(chord);
            }

            for (Iterator<Chord> it = activeChords.iterator(); it.hasNext();) {
                Chord active = it.next();

                if (active.getEndTime() <= slot.getOffset()) {
                    it.remove();
                }
            }

            // Sort active chords
            Collections.sort(activeChords, chordComparator);

            if (logger.isFineEnabled()) {
                logger.fine("Active chords=" + activeChords);
            }

            // Reuse voices when we have to
            for (Chord chord : activeChords) {
                if (chord.getVoice() != null) {
                    // Keep same voice since extended from previous slots
                    voiceMap.put(chord.getVoice(), chord);

                    continue;
                } else {
                    // Check for extension of a beam group
                    BeamGroup group = chord.getBeamGroup();

                    if (group != null) {
                        Integer voice = groupVoices.get(group);

                        if (voice != null) {
                            chord.setVoice(voice);
                            voiceMap.put(voice, chord);

                            continue;
                        }
                    }
                }
            }

            // Assign remaining chords
            for (Chord chord : activeChords) {
                if (chord.getVoice() == null) {
                    // Take first available voice
                    int voice;

                    if (voiceMap.isEmpty()) {
                        voice = 1;
                    } else {
                        for (voice = 1; voice <= voiceMap.lastKey(); voice++) {
                            if (voiceMap.get(voice) == null) {
                                // Available
                                break;
                            }
                        }
                    }

                    chord.setVoice(voice);
                    voiceMap.put(voice, chord);

                    BeamGroup group = chord.getBeamGroup();

                    if (group != null) {
                        groupVoices.put(group, voice);
                    }
                }
            }

            if (logger.isFineEnabled()) {
                for (Chord chord : slot.getChords()) {
                    print(chord);
                }
            }

            maxVoice = Math.max(maxVoice, voiceMap.lastKey());
            prevSlot = slot;
        }

        // Remember the maximum number of voices in that measure
        measure.setVoicesNumber(maxVoice);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare this slot to another, as needed to insert slots in an ordered
     * collection
     *
     * @param other another slot
     * @return -1, 0 or +1, according to their relative abscissae
     */
    public int compareTo (Slot other)
    {
        return Integer.signum(getX() - other.getX());
    }

    //---------------//
    // getChordAbove //
    //---------------//
    public Chord getChordAbove (SystemPoint point)
    {
        Chord chordAbove = null;

        // We look for the chord just above
        for (Chord chord : getChords()) {
            if (chord.getHeadLocation().y < point.y) {
                chordAbove = chord;
            } else {
                break;
            }
        }

        return chordAbove;
    }

    //---------------//
    // getChordBelow //
    //---------------//
    public Chord getChordBelow (SystemPoint point)
    {
        // We look for the chord just below
        for (Chord chord : getChords()) {
            if (chord.getHeadLocation().y > point.y) {
                return chord;
            }
        }

        // Not found
        return null;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the (ordered) collection of chords in this time slot
     *
     * @return the collection of chords
     */
    public List<Chord> getChords ()
    {
        return chords;
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    public List<Chord> getEmbracedChords (SystemPoint top,
                                          SystemPoint bottom)
    {
        List<Chord> chords = new ArrayList<Chord>();

        for (Chord chord : getChords()) {
            if (chord.isEmbracedBy(top, bottom)) {
                chords.add(chord);
            }
        }

        return chords;
    }

    //-----------//
    // getOffset //
    //-----------//
    /**
     * Report the time offset of this time slot since beginning of the measure
     *
     * @return the time offset of this time slot.
     */
    public int getOffset ()
    {
        return offset;
    }

    //---------------------//
    // getShortestDuration //
    //---------------------//
    /**
     * Since there may be several chords aligned (starting) in this slot, this
     * method reports the shortest duration among all chords of this slot. This
     * in turn defines the time offset of the following slot.
     *
     * @return the duration of the chord with shortest duration
     */
    public int getShortestDuration ()
    {
        int best = Integer.MAX_VALUE;

        for (Chord chord : getChords()) {
            if (best > chord.getDuration()) {
                best = chord.getDuration();
            }
        }

        return best;
    }

    //------------------//
    // getSuitableChord //
    //------------------//
    public Chord getSuitableChord (SystemPoint point)
    {
        Chord chordAbove = null;
        Chord chordBelow = null;

        // We look for a chord just above or just below
        for (Chord chord : getChords()) {
            if (chord.getHeadLocation().y < point.y) {
                chordAbove = chord;
            } else {
                chordBelow = chord;

                break;
            }
        }

        if (chordAbove != null) {
            for (TreeNode node : chordAbove.getNotes()) {
                Note note = (Note) node;

                return chordAbove;
            }
        }

        if (chordBelow != null) {
            for (TreeNode node : chordBelow.getNotes()) {
                Note note = (Note) node;

                return chordBelow;
            }
        }

        return null;
    }

    //------//
    // getX //
    //------//
    /**
     * Report the abscissa of this slot
     *
     * @return the slot abscissa, wrt the containing system (and not measure)
     */
    public int getX ()
    {
        if (x == null) {
            Population population = new Population();

            for (Glyph glyph : glyphs) {
                population.includeValue(
                    measure.getSystem()
                           .toSystemPoint(glyph.getCenter()).x);
            }

            if (population.getCardinality() > 0) {
                x = (int) Math.rint(population.getMeanValue());
            }
        }

        return x;
    }

    //---------------//
    // isAlignedWith //
    //---------------//
    /**
     * Check whether a system point is roughly aligned with this slot instance.
     *
     * @param sysPt the system point to check
     * @return true if aligned
     */
    public boolean isAlignedWith (SystemPoint sysPt)
    {
        return Math.abs(sysPt.x - getX()) <= measure.getScale()
                                                    .toUnits(constants.maxDx);
    }

    //-----------//
    // setOffset //
    //-----------//
    /**
     * Assign the time offset, since the beginning of the measure, for all
     * chords in this time slot
     *
     * @param offset time offset using {@link omr.score.Note#QUARTER_DURATION}
     * value
     */
    public void setOffset (int offset)
    {
        this.offset = offset;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Slot");

        sb.append(" x=")
          .append(getX());

        if (offset != null) {
            sb.append(" offset=")
              .append(offset);
        }

        sb.append(" glyphs=[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId())
              .append("/")
              .append(glyph.getShape())
              .append(" ");
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a slot with this note glyph
     *
     * @param glyph a chord-relevant glyph (rest, note or notehead)
     * @param measure the containing measure
     */
    static void populate (Glyph       glyph,
                          Measure     measure,
                          SystemPoint sysPt)
    {
        //        if (logger.isFineEnabled()) {
        //            logger.fine("Populating slot with " + glyph);
        //        }

        // First look for a suitable slot
        for (Slot slot : measure.getSlots()) {
            if (slot.isAlignedWith(sysPt)) {
                slot.addGlyph(glyph);

                return;
            }
        }

        // No compatible slot, create a brand new one
        Slot slot = new Slot(measure);
        slot.addGlyph(glyph);
        measure.getSlots()
               .add(slot);
    }

    //-------//
    // print //
    //-------//
    /**
     * A debugging method
     *
     * @param chord the chord to print
     */
    private static void print (Chord chord)
    {
        // Print out
        StringBuilder sb = new StringBuilder();
        sb.append(".. Chord#")
          .append(chord.getId());
        sb.append(" voice#")
          .append(chord.getVoice());
        sb.append(" dur=")
          .append(
            chord.getPart().getScorePart().simpleDurationOf(
                chord.getDuration()));

        // Staff ?
        Note note = (Note) chord.getNotes()
                                .get(0);

        if (note != null) {
            sb.append(" staff#")
              .append(note.getStaff().getId());
        }

        if (chord.getBeams()
                 .size() > 0) {
            sb.append(" beamGroup#")
              .append(chord.getBeams().first().getGroup().getId());
        }

        logger.fine(sb.toString());
    }

    //--------------//
    // getStemChord //
    //--------------//
    /**
     * Given a stem, look up for a slot that already contains it, otherwise
     * create a brand new slot to host the stem.
     *
     * @param stem the stem to look up
     * @return the existing/created slot that contains the stem
     */
    private Chord getStemChord (Glyph stem)
    {
        // Check we don't already have this stem in a chord
        for (Chord chord : chords) {
            if (chord.getStem() == stem) {
                return chord;
            }
        }

        // Not found, let's create it
        Chord chord = new Chord(measure);
        chords.add(chord);
        chord.setStem(stem);

        return chord;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Maximum horizontal distance between a slot and a glyph candidate
         */
        Scale.Fraction maxDx = new Scale.Fraction(
            1.25,
            "Maximum horizontal distance between a slot and a glyph candidate");
    }
}
