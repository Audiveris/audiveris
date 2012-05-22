//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l o t                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.InjectionSolver;
import omr.math.Rational;

import omr.score.common.PixelPoint;

import omr.util.HorizontalSide;
import omr.util.Navigable;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class {@code Slot} represents a roughly defined time slot within a
 * measure, to gather all chord entities (rests, whole notes or
 * noteheads) that occur at the same time because their abscissae are
 * roughly the same.
 *
 * <p>There are two policies to define slots: one based on heads implemented in
 * {@link HeadBasedSlot} and one based on stems implemented in
 * {@link StemBasedSlot}.
 *
 * <p>The slot embraces all the staves of this part measure. Perhaps we should
 * consider merging slots between parts as well?
 *
 * <p>On the following picture, slots are indicated by vertical blue lines <br/>
 * <img src="doc-files/Slot.png" alt="diagram">
 *
 * @author Hervé Bitteur
 */
public abstract class Slot
        implements Comparable<Slot>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Slot.class);

    /** Chord comparator to sort chords vertically within the same slot */
    public static final Comparator<Chord> chordComparator = new Comparator<Chord>()
    {

        @Override
        public int compare (Chord c1,
                            Chord c2)
        {
            Note n1 = (Note) c1.getNotes().get(0);
            Note n2 = (Note) c2.getNotes().get(0);

            // First : staff
            int dStaff = n1.getStaff().getId() - n2.getStaff().getId();

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
    /** Id unique within the containing measure */
    private int id;

    /** Reference point of the slot */
    protected PixelPoint refPoint;

    /** The containing measure */
    @Navigable(false)
    protected Measure measure;

    /** Collection of glyphs in the slot */
    protected List<Glyph> glyphs = new ArrayList<>();

    /** Cached margin for abscissa alignment */
    protected final int xMargin;

    /** Collection of chords in this slot, sorted by staff, then by ordinate */
    private List<Chord> chords = new ArrayList<>();

    /** Time offset since measure start */
    private Rational startTime;

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

        double slotMargin = measure.getScore().getSlotMargin();
        xMargin = (int) Math.rint(
                measure.getScale().getInterline() * slotMargin);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // dumpSystemSlots //
    //-----------------//
    public static void dumpSystemSlots (ScoreSystem system)
    {
        // Dump all measure slots
        logger.fine(system.toString());

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;

            logger.fine(part.toString());

            for (TreeNode mn : part.getMeasures()) {
                Measure measure = (Measure) mn;

                logger.fine(measure.toString());

                for (Slot slot : measure.getSlots()) {
                    logger.fine(slot.toString());
                }
            }
        }
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a slot with the provided note glyph.
     *
     * @param glyph   a chord-relevant glyph (rest, note or notehead)
     * @param measure the containing measure
     * @param policy  the policy to use for slot positioning
     */
    public static void populate (Glyph glyph,
                                 Measure measure,
                                 SlotPolicy policy)
    {
        final boolean logging = glyph.isVip() || logger.isFineEnabled();

        if (logging) {
            logger.info("Populating slot with {0}", glyph);
        }

        // Special case for measure rests: they don't belong to any time slot,
        // and their duration is the measure duration
        if (glyph.getShape().isMeasureRest()) {
            measure.addWholeChord(glyph);
        } else {
            PixelPoint pt = null;

            // Use the stem abscissa or the note abscissa
            switch (policy) {
            case STEM_BASED:

                if (glyph.getStemNumber() == 1) {
                    pt = glyph.getFirstStem().getAreaCenter();
                }

                break;

            case HEAD_BASED:
                pt = glyph.getAreaCenter();

                break;
            }

            if (pt == null) {
                pt = glyph.getAreaCenter();
            }

            // First look for a compatible slot
            for (Slot slot : measure.getSlots()) {
                if (slot.isAlignedWith(pt)) {
                    if (logging) {
                        logger.info("{0} Joining slot {1}", new Object[]{measure,
                                                                         slot});
                    }

                    slot.addGlyph(glyph);

                    return;
                }
            }

            // No compatible slot found, so let's create a brand new one
            Slot slot = null;

            switch (policy) {
            case STEM_BASED:
                slot = new StemBasedSlot(measure, pt);

                break;

            case HEAD_BASED:
                slot = new HeadBasedSlot(measure);

                break;
            }

            slot.addGlyph(glyph);

            if (logging) {
                logger.info("{0} Creating new slot {1}", new Object[]{measure,
                                                                      slot});
            }

            measure.getSlots().add(slot);
        }
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Insert a glyph (supposedly from a chord) into this slot.
     *
     * @param glyph the glyph to insert
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
    }

    //------------------------//
    // allocateChordsAndNotes //
    //------------------------//
    /**
     * Based on the current collection of glyphs within this slot,
     * allocate the proper chords, a strategy based on each
     * glyph-related stem.
     */
    public void allocateChordsAndNotes ()
    {
        // Allocate 1 chord per stem, per rest, per (whole) note
        for (Glyph glyph : glyphs) {
            if (glyph.getStemNumber() > 0) {
                // Beware of noteheads with 2 stems, we need to duplicate them
                // in order to actually have two logical chords.
                for (HorizontalSide side : HorizontalSide.values()) {
                    Glyph stem = glyph.getStem(side);

                    if (stem != null) {
                        Chord chord = getStemChord(stem);
                        Note.createPack(chord, glyph);
                    }
                }
            } else {
                Chord chord = new Chord(measure, this);
                chords.add(chord);
                Note.createPack(chord, glyph);
            }
        }
    }

    //-------------//
    // BuildVoices //
    //-------------//
    /**
     * Compute the various voices and start times in this slot.
     *
     * @param activeChords the chords which were active right before this slot
     */
    public void buildVoices (List<Chord> activeChords)
    {
        // Sort chords vertically  within the slot
        Collections.sort(chords, chordComparator);

        logger.fine("buildVoices for Slot#{0} Actives={1} Chords={2}",
                    new Object[]{getId(), activeChords, chords});

        // Use the active chords before this slot to compute start time
        computeStartTime(activeChords);

        // Chords that are ending, with their voice available
        List<Chord> endingChords = new ArrayList<>();

        // Chords that are ending, with voice not available (beam group)
        List<Chord> passingChords = new ArrayList<>();

        for (Chord chord : activeChords) {
            // Look for chord that finishes at the slot at hand
            // Make sure voice is really available
            if (!chord.isWholeDuration()) {
                if ((chord.getEndTime().compareTo(startTime) <= 0)) {
                    BeamGroup group = chord.getBeamGroup();

                    if ((group == null) || (chord == group.getChords().last())) {
                        endingChords.add(chord);
                    } else {
                        passingChords.add(chord);
                    }
                }
            } else {
                // Chord (and its voice) continues past the slot at hand
            }
        }

        logger.fine("endingChords={0}", endingChords);
        logger.fine("passingChords={0}", passingChords);
        logger.fine("Chords={0}", chords);

        InjectionSolver solver = new InjectionSolver(
                chords.size(),
                endingChords.size() + chords.size(),
                new MyDistance(chords, endingChords));
        int[] links = solver.solve();

        for (int i = 0; i < links.length; i++) {
            int index = links[i];

            // Map new chord to an ending chord?
            if (index < endingChords.size()) {
                Voice voice = endingChords.get(index).getVoice();
                logger.fine("Slot#{0} Reusing voice#{1}", new Object[]{getId(),
                                                                       voice.
                            getId()});

                Chord ch = chords.get(i);

                try {
                    ch.setVoice(voice);
                } catch (Exception ex) {
                    ch.addError("Failed to set voice of chord");

                    return;
                }
            }
        }

        // Assign remaining non-mapped chords, using 1st voice available
        assignVoices(chords);

        // Purge collection of active chords for this slot
        // Add the chords that start with this slot
        activeChords.removeAll(endingChords);
        activeChords.removeAll(passingChords); // ?????
        activeChords.addAll(chords);
        Collections.sort(activeChords, chordComparator);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare this slot to another, as needed to insert slots in an
     * ordered collection.
     *
     * @param other another slot
     * @return -1, 0 or +1, according to their relative abscissae
     */
    @Override
    public int compareTo (Slot other)
    {
        return Integer.signum(getX() - other.getX());
    }

    //------//
    // getX //
    //------//
    /**
     * Report the abscissa of this slot.
     *
     * @return the slot abscissa, wrt the page (and not measure)
     */
    public abstract int getX ();

    //---------------//
    // getChordAbove //
    //---------------//
    /**
     * Report the chord which is just above the given point in this
     * slot.
     *
     * @param point the given point
     * @return the chord above, or null
     */
    public Chord getChordAbove (PixelPoint point)
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
    /**
     * Report the chord which is just below the given point in this
     * slot.
     *
     * @param point the given point
     * @return the chord below, or null
     */
    public Chord getChordBelow (PixelPoint point)
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
     * Report the (sorted) collection of chords in this time slot.
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
    /**
     * Report the chords whose notes stand in the given vertical range.
     *
     * @param top    upper point of range
     * @param bottom lower point of range
     * @return the collection of chords, which may be empty
     */
    public List<Chord> getEmbracedChords (PixelPoint top,
                                          PixelPoint bottom)
    {
        List<Chord> embracedChords = new ArrayList<>();

        for (Chord chord : getChords()) {
            if (chord.isEmbracedBy(top, bottom)) {
                embracedChords.add(chord);
            }
        }

        return embracedChords;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the slot Id.
     *
     * @return the slot id (for debug)
     */
    public int getId ()
    {
        return id;
    }

    //------------------//
    // getLocationGlyph //
    //------------------//
    /**
     * Report a glyph that can be used to show the location of the slot.
     *
     * @return a glyph from the slot
     */
    public Glyph getLocationGlyph ()
    {
        if (!glyphs.isEmpty()) {
            return glyphs.iterator().next();
        } else {
            return null;
        }
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the time offset of this time slot since the beginning of
     * the measure.
     *
     * @return the time offset of this time slot.
     */
    public Rational getStartTime ()
    {
        return startTime;
    }

    //----------//
    // getStems //
    //----------//
    /**
     * Report the set of all stems that belong to this slot.
     *
     * @return the set of related stems
     */
    public Set<Glyph> getStems ()
    {
        Set<Glyph> stems = new HashSet<>();

        for (Chord chord : chords) {
            Glyph stem = chord.getStem();

            if (stem != null) {
                stems.add(stem);
            }
        }

        return stems;
    }

    //-------------//
    // includeSlot //
    //-------------//
    /**
     * Include the other slot into this one.
     *
     * @param that the other slot to include
     */
    public void includeSlot (Slot that)
    {
        glyphs.addAll(that.glyphs);
    }

    //---------------//
    // isAlignedWith //
    //---------------//
    /**
     * Check whether a system point is roughly aligned with this slot
     * instance.
     *
     * @param sysPt the system point to check
     * @return true if aligned
     */
    public boolean isAlignedWith (PixelPoint sysPt)
    {
        return Math.abs(sysPt.x - getX()) <= xMargin;
    }

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Assign the startTime since the beginning of the measure,
     * for all chords in this time slot.
     *
     * @param startTime time offset since measure start
     */
    public void setStartTime (Rational startTime)
    {
        if (this.startTime == null) {
            logger.fine("setStartTime {0} for Slot #{1}", new Object[]{startTime,
                                                                       getId()});

            this.startTime = startTime;

            // Assign to all chords of this slot first
            for (Chord chord : getChords()) {
                chord.setStartTime(startTime);
            }

            // Then, extend this information through the beamed chords if any
            for (Chord chord : getChords()) {
                BeamGroup group = chord.getBeamGroup();

                if (group != null) {
                    group.computeStartTimes();
                }
            }

            // Update all voices
            for (Voice voice : measure.getVoices()) {
                voice.updateSlotTable();
            }
        } else {
            if (!this.startTime.equals(startTime)) {
                getChords().get(0).addError(
                        "Reassigning startTime from " + this.startTime + " to "
                        + startTime + " in " + this);
            }
        }
    }

    //---------------//
    // toChordString //
    //---------------//
    public String toChordString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("slot#").append(getId());

        if (getStartTime() != null) {
            sb.append(" start=").append(String.format("%5s", getStartTime()));
        }

        sb.append(" [");

        boolean started = false;

        for (Chord chord : getChords()) {
            if (started) {
                sb.append(",");
            }

            sb.append(chord);
            started = true;
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Slot#").append(id);

        sb.append(" x=").append(getX());

        if (startTime != null) {
            sb.append(" start=").append(startTime);
        }

        sb.append(" glyphs=[");

        for (Glyph glyph : glyphs) {
            sb.append("#").append(glyph.getId()).append("/").append(glyph.
                    getShape()).append(" ");
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // toVoiceString //
    //---------------//
    public String toVoiceString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("slot#").append(getId()).append(" start=").append(String.
                format("%5s", getStartTime())).append(" [");

        SortedMap<Integer, Chord> voiceChords = new TreeMap<>();

        for (Chord chord : getChords()) {
            voiceChords.put(chord.getVoice().getId(), chord);
        }

        boolean started = false;
        int voiceMax = measure.getVoicesNumber();

        for (int iv = 1; iv <= voiceMax; iv++) {
            if (started) {
                sb.append(", ");
            } else {
                started = true;
            }

            Chord chord = voiceChords.get(iv);

            if (chord != null) {
                sb.append("V").append(chord.getVoice().getId());
                sb.append(" Ch#").append(String.format("%02d", chord.getId()));
                sb.append(" St").append(chord.getStaff().getId());
                sb.append(" Dur=").append(String.format("%5s",
                                                        chord.getDuration()));
            } else {
                sb.append("----------------------");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    /**
     * Assign available voices to the chords that have yet no voice
     * assigned.
     *
     * @param chords the collection of chords to process for this slot
     */
    private void assignVoices (Collection<Chord> chords)
    {
        // Assign remaining non-mapped chords, using 1st voice available
        for (Chord chord : chords) {
            // Process only the chords that have no voice assigned yet
            if (chord.getVoice() == null) {
                for (Voice voice : measure.getVoices()) {
                    if (voice.isFree(this)) {
                        chord.setVoice(voice);

                        break;
                    }
                }

                if (chord.getVoice() == null) {
                    logger.fine("{0} Slot#{1} creating voice for Ch#{2}",
                                new Object[]{chord.getContextString(), id,
                                             chord.getId()});

                    // Add a new voice
                    new Voice(chord);
                }
            }
        }
    }

    //------------------//
    // computeStartTime //
    //------------------//
    /**
     * Based on the active chords before this slot, determine the next
     * expiration time, which governs this slot.
     *
     * @param activeChords
     */
    private void computeStartTime (Collection<Chord> activeChords)
    {
        Rational slotTime = Rational.MAX_VALUE;

        for (Chord chord : activeChords) {
            if (!chord.isWholeDuration()) { // Skip the "whole" chords

                Rational endTime = chord.getEndTime();

                if (endTime.compareTo(slotTime) < 0) {
                    slotTime = endTime;
                }
            }
        }

        if (slotTime.equals(Rational.MAX_VALUE)) {
            slotTime = Rational.ZERO;
        }

        logger.fine("slotTime={0}", slotTime);

        setStartTime(slotTime);
    }

    //--------------//
    // getStemChord //
    //--------------//
    /**
     * Given a stem, look up for a chord that already contains it,
     * otherwise create a brand new chord to host the stem.
     *
     * @param stem the stem to look up
     * @return the existing/created chord that contains the stem
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
        Chord chord = new Chord(measure, this);
        chords.add(chord);
        chord.setStem(stem);
        stem.setTranslation(chord);

        return chord;
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // MyDistance //
    //------------//
    private static final class MyDistance
            implements InjectionSolver.Distance
    {
        //~ Static fields/initializers -----------------------------------------

        private static final int NO_LINK = 20;

        private static final int STAFF_DIFF = 40;

        private static final int INCOMPATIBLE_VOICES = 10000; // Forbidden

        //~ Instance fields ----------------------------------------------------
        private final List<Chord> news;

        private final List<Chord> olds;

        //~ Constructors -------------------------------------------------------
        public MyDistance (List<Chord> news,
                           List<Chord> olds)
        {
            this.news = news;
            this.olds = olds;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int getDistance (int in,
                                int ip)
        {
            // No link to an old chord
            if (ip >= olds.size()) {
                return NO_LINK;
            }

            Chord newChord = news.get(in);
            Chord oldChord = olds.get(ip);

            if ((newChord.getVoice() != null)
                    && (oldChord.getVoice() != null)
                    && (newChord.getVoice() != oldChord.getVoice())) {
                return INCOMPATIBLE_VOICES;
            } else if (newChord.getStaff() != oldChord.getStaff()) {
                return STAFF_DIFF;
            } else {
                int dy = Math.abs(
                        newChord.getHeadLocation().y
                        - oldChord.getHeadLocation().y) / newChord.getScale().
                        getInterline();
                int dStem = Math.abs(
                        newChord.getStemDir() - oldChord.getStemDir());

                return dy + (2 * dStem);
            }
        }
    }
}
