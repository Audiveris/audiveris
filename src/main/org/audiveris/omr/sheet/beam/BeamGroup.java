//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m G r o u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code BeamGroup} represents a group of related beams.
 * <p>
 * NOTA: Beams in a BeamGroup are in no particular order.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "beam-group")
public class BeamGroup
        implements Vip
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamGroup.class);

    // Persistent data
    //----------------
    //
    /** Id for debug mainly, unique within measure stack. */
    @XmlAttribute
    private final int id;

    /** Indicates a beam group that is linked to more than one staff. */
    @XmlAttribute(name = "multi-staff")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean multiStaff;

    /** Set of contained beams. */
    @XmlList
    @XmlIDREF
    @XmlValue
    private final LinkedHashSet<AbstractBeamInter> beams = new LinkedHashSet<>();

    // Transient data
    //---------------
    //
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /** Containing measure. */
    @Navigable(false)
    private Measure measure;

    /** Same voice for all chords in this beam group. */
    private Voice voice;

    /**
     * Creates a new instance of BeamGroup.
     *
     * @param measure the containing measure
     */
    public BeamGroup (Measure measure)
    {
        this.measure = measure;
        id = getNextGroupId(measure);
        measure.addBeamGroup(this);

        logger.debug("{} Created {}", measure, this);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BeamGroup ()
    {
        this.id = 0;
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
        beams.add(beam);

        if (beam.isVip()) {
            setVip(true);
        }

        if (isVip() || logger.isDebugEnabled()) {
            logger.info("{} Added {} to {}", measure, beam, this);
        }
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param measure the containing measure
     */
    public void afterReload (Measure measure)
    {
        try {
            this.measure = measure;

            for (AbstractBeamInter beam : beams) {
                beam.setGroup(this);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-------------//
    // assignVoice //
    //-------------//
    /**
     * Just assign a voice to this beam group.
     *
     * @param voice the voice to assign
     */
    public void assignVoice (Voice voice)
    {
        this.voice = voice;
    }

    //--------------------//
    // computeTimeOffsets //
    //--------------------//
    /**
     * Compute time offsets for all chords of this beam group, assuming the first chord
     * of the group already has its time offset assigned.
     */
    public void computeTimeOffsets ()
    {
        AbstractChordInter prevChord = null;

        for (AbstractChordInter chord : getAllChords()) {
            if (prevChord != null) {
                try {
                    chord.setTimeOffset(prevChord.getEndTime());
                } catch (Exception ex) {
                    logger.warn("{} Cannot compute chord time based on previous chord", chord);
                }
            } else if (chord.getTimeOffset() == null) {
                logger.warn("{} Computing beam group times with first chord not set", chord);
            }

            prevChord = chord;
        }
    }

    //--------------//
    // getAllChords //
    //--------------//
    /**
     * Report the x-ordered collection of chords that are grouped by this beam group,
     * including the interleaved rests if any.
     *
     * @return the (perhaps empty) collection of 'beamed' chords and interleaved rests.
     */
    public List<AbstractChordInter> getAllChords ()
    {
        final List<AbstractChordInter> allChords = new ArrayList<>();
        AbstractChordInter prevChord = null;

        for (AbstractChordInter chord : getChords()) {
            if (prevChord != null) {
                AbstractNoteInter rest = measure.lookupRest(prevChord, chord);

                if (rest != null) {
                    allChords.add(rest.getChord());
                }
            }

            allChords.add(chord);
            prevChord = chord;
        }

        Collections.sort(allChords, Inters.byAbscissa);

        return allChords;
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the beams that are part of this group.
     *
     * @return the collection of contained beams
     */
    public Set<AbstractBeamInter> getBeams ()
    {
        return beams;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the x-ordered collection of chords that are grouped by this beam group.
     *
     * @return the (perhaps empty) collection of 'beamed' chords.
     */
    public List<AbstractChordInter> getChords ()
    {
        List<AbstractChordInter> chords = new ArrayList<>();

        for (AbstractBeamInter beam : getBeams()) {
            for (AbstractChordInter chord : beam.getChords()) {
                if (!chords.contains(chord)) {
                    chords.add(chord);
                }
            }
        }

        Collections.sort(chords, Inters.byAbscissa);

        return chords;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the total duration of the sequence of chords within this group.
     * <p>
     * Beware, there may be rests inserted within beam-grouped notes.
     *
     * @return the total group duration, perhaps null
     */
    public Rational getDuration ()
    {
        final AbstractChordInter first = getFirstChord();
        final Rational firstOffset = first.getTimeOffset();

        final AbstractChordInter last = getLastChord();
        final Rational lastOffset = last.getTimeOffset();

        if (firstOffset == null || lastOffset == null) {
            return null;
        }

        return lastOffset.minus(firstOffset).plus(last.getDuration());
    }

    //---------------//
    // getFirstChord //
    //---------------//
    /**
     * Report the first chord on the left.
     *
     * @return the first chord
     */
    public AbstractChordInter getFirstChord ()
    {
        List<AbstractChordInter> chords = getChords();

        if (!chords.isEmpty()) {
            return chords.get(0);
        } else {
            return null;
        }
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
    public AbstractChordInter getLastChord ()
    {
        List<AbstractChordInter> chords = getChords();

        if (!chords.isEmpty()) {
            return chords.get(chords.size() - 1);
        } else {
            return null;
        }
    }

    //----------//
    // getVoice //
    //----------//
    /**
     * Report the assigned voice.
     *
     * @return beam group voice
     */
    public Voice getVoice ()
    {
        return voice;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Assign a voice to this beam group, and to the related entities.
     *
     * @param voice the voice to assign
     */
    public void setVoice (Voice voice)
    {
        if (this.voice != voice) {
            this.voice = voice;

            // Forward this information to the beamed chords, including the interleaved rests if any
            for (AbstractChordInter chord : getAllChords()) {
                chord.setVoice(voice);
            }
        }
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Tell whether this beam group is linked to more than one staff.
     *
     * @return the multiStaff
     */
    public boolean isMultiStaff ()
    {
        return multiStaff;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //------------//
    // removeBeam //
    //------------//
    /**
     * Remove a beam from this group.
     *
     * @param beam the beam to remove
     */
    public void removeBeam (AbstractBeamInter beam)
    {
        if (!beams.remove(beam)) {
            logger.warn(beam + " not found in " + this);
        }

        if (beams.isEmpty()) {
            // Remove this group from its containing measure
            if (measure != null) {
                measure.removeBeamGroup(this);
            }
        }
    }

    //-------------//
    // resetTiming //
    //-------------//
    /**
     *
     */
    public void resetTiming ()
    {
        voice = null;
    }

    //------------//
    // setMeasure //
    //------------//
    /**
     * @param measure the measure to set
     */
    public void setMeasure (Measure measure)
    {
        this.measure = measure;
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

    //---------------//
    // checkForSplit //
    //---------------//
    /**
     * Run a consistency check on the group, and detect when a group has to be split.
     *
     * @return the detected alien chord, or null if no split is needed
     */
    private AbstractChordInter checkForSplit ()
    {
        final Scale scale = measure.getPart().getSystem().getSheet().getScale();
        final double maxChordDy = constants.maxChordDy.getValue();

        // Make sure all chords are part of the same group
        // We check the vertical distance between any chord and the beams above or below the chord.
        for (AbstractChordInter chord : getChords()) {
            if (chord.isVip()) {
                logger.info("VIP checkForSplit on {}", chord);
            }

            final Rectangle chordBox = chord.getBounds();
            final Point tail = chord.getTailLocation();

            // Get the collection of questionable beams WRT chord
            List<AbstractBeamInter> questionableBeams = new ArrayList<>();

            for (AbstractBeamInter beam : beams) {
                // Skip beam hooks
                // Skip beams attached to this chord
                // Skip beams with no abscissa overlap WRT this chord
                if (!beam.isHook() && !beam.getChords().contains(chord)
                            && (GeoUtil.xOverlap(beam.getBounds(), chordBox) > 0)) {
                    // Check vertical gap
                    int lineY = (int) Math.rint(LineUtil.yAtX(beam.getMedian(), tail.x));
                    int yOverlap = Math.min(lineY, chordBox.y + chordBox.height) - Math.max(
                            lineY,
                            chordBox.y);

                    if (yOverlap < 0) {
                        questionableBeams.add(beam);
                    }
                }
            }

            if (questionableBeams.isEmpty()) {
                continue; // No problem found around the chord at hand
            }

            // Sort these questionable beams vertically, at chord stem abscissa,
            // according to distance from chord tail.
            Collections.sort(questionableBeams, new Comparator<AbstractBeamInter>()
                     {
                         @Override
                         public int compare (AbstractBeamInter b1,
                                             AbstractBeamInter b2)
                         {
                             final double y1 = LineUtil.yAtX(b1.getMedian(), tail.x);
                             double tailDy1 = Math.abs(y1 - tail.y);
                             final double y2 = LineUtil.yAtX(b2.getMedian(), tail.x);
                             double tailDy2 = Math.abs(y2 - tail.y);

                             return Double.compare(tailDy1, tailDy2);
                         }
                     });

            AbstractBeamInter nearestBeam = questionableBeams.get(0);
            int lineY = (int) Math.rint(LineUtil.yAtX(nearestBeam.getMedian(), tail.x));
            int tailDy = Math.abs(lineY - tail.y);
            double normedDy = scale.pixelsToFrac(tailDy);

            if (normedDy > maxChordDy) {
                logger.debug(
                        "Vertical gap between {} and {}, {} vs {}",
                        chord,
                        nearestBeam,
                        normedDy,
                        maxChordDy);

                // Split the beam group here
                return chord;
            }
        }

        return null; // everything is OK
    }

    //-------------//
    // countStaves //
    //-------------//
    /**
     * Check whether this group is linked to more than one staff.
     * If so, it is flagged as such.
     */
    private void countStaves ()
    {
        Set<Staff> staves = new LinkedHashSet<>();

        for (AbstractBeamInter beam : beams) {
            SIGraph sig = beam.getSig();

            for (Relation rel : sig.getRelations(beam, BeamStemRelation.class)) {
                Inter stem = sig.getOppositeInter(beam, rel);
                Staff staff = stem.getStaff();

                if (staff != null) {
                    staves.add(staff);
                }
            }
        }

        if (staves.size() > 1) {
            multiStaff = Boolean.TRUE;
        }
    }

    //-------//
    // split //
    //-------//
    private void split (AbstractChordInter alienChord)
    {
        new Splitter(alienChord).process();
    }

    //-------------//
    // includeBeam //
    //-------------//
    /**
     * Manually include (or re-include) a beam into the measure BeamGroup structure.
     *
     * @param beam    beam to include
     * @param measure containing measure
     */
    public static void includeBeam (AbstractBeamInter beam,
                                    Measure measure)
    {
        // Look for a compatible group (via a common stem)
        for (BeamGroup group : measure.getBeamGroups()) {
            for (AbstractBeamInter b : group.getBeams()) {
                if (beam.hasCommonStemWith(b)) {
                    assignGroup(group, beam);

                    return; // Found a hosting group
                }
            }
        }

        // Not found, create a new one
        BeamGroup group = new BeamGroup(measure);
        assignGroup(group, beam);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate all the BeamGroup instances for a given measure.
     *
     * @param measure         the containing measure
     * @param checkGroupSplit true for check on group split
     */
    public static void populate (Measure measure,
                                 boolean checkGroupSplit)
    {
        measure.clearBeamGroups();

        // Retrieve beams in this measure
        Set<AbstractBeamInter> beams = new LinkedHashSet<>();

        for (AbstractChordInter chord : measure.getHeadChords()) {
            beams.addAll(chord.getBeams());
        }

        // Reset group info in each beam
        for (AbstractBeamInter beam : beams) {
            beam.setGroup(null);
        }

        // Build beam groups for this measure stack
        for (AbstractBeamInter beam : beams) {
            if (!beam.isRemoved() && (beam.getGroup() == null)) {
                BeamGroup group = new BeamGroup(measure);
                assignGroup(group, beam);
                logger.debug("{}", group);
            }
        }

        if (checkGroupSplit) {
            // In case something goes wrong, use an upper limit to loop
            int loopNb = constants.maxSplitLoops.getValue();

            while (checkBeamGroups(measure)) {
                if (--loopNb < 0) {
                    logger.warn("Loop detected in BeamGroup split in {}", measure);

                    break;
                }
            }
        }

        // Detect groups that are linked to more than one staff
        for (BeamGroup group : measure.getBeamGroups()) {
            group.countStaves();
            logger.debug("   {}", group);
        }
    }

    //-------------//
    // assignGroup //
    //-------------//
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

        for (AbstractChordInter chord : beam.getChords()) {
            for (AbstractBeamInter b : chord.getBeams()) {
                if (b.getGroup() == null) {
                    assignGroup(group, b);
                }
            }
        }
    }

    //-----------------//
    // checkBeamGroups //
    //-----------------//
    /**
     * Check all the BeamGroup instances of the given measure, to find the first
     * split if any to perform.
     *
     * @param measure the given measure
     * @return the first split parameters, or null if everything is OK
     */
    private static boolean checkBeamGroups (Measure measure)
    {
        for (BeamGroup group : measure.getBeamGroups()) {
            AbstractChordInter alienChord = group.checkForSplit();

            if (alienChord != null) {
                group.split(alienChord);

                return true;
            }
        }

        return false;
    }

    /**
     * Find proper unique ID for a new group.
     *
     * @return proper ID
     */
    private static int getNextGroupId (Measure measure)
    {
        int max = 0;

        for (BeamGroup group : measure.getBeamGroups()) {
            max = Math.max(max, group.getId());
        }

        return ++max;
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

        /** Chord detected as belonging to a (new) alien group. */
        private final AbstractChordInter alienChord;

        /**
         * Beams that belong to new alien group.
         * (Initially populated with all beams (except beam hooks) attached to alienChord)
         */
        private Set<AbstractBeamInter> alienBeams;

        /** The new alien group. */
        private BeamGroup alienGroup;

        /** The chord that embraces both (old) group and (new) alien group. */
        private HeadChordInter pivotChord;

        /**
         * Create a splitter for this BeamGroup, triggered by alienChord
         *
         * @param alienChord a detected chord that should belong to a separate group
         */
        Splitter (AbstractChordInter alienChord)
        {
            this.alienChord = alienChord;
        }

        //---------//
        // process //
        //---------//
        /**
         * Actually split the group in two, around the detected pivot chord.
         * <p>
         * Some beams of this group instance are moved to a new separate BeamGroup instance.
         * The two instances are articulated around a pivot chord, common to both groups.
         * <p>
         */
        public void process ()
        {
            logger.debug("{} splitter on {}", BeamGroup.this, alienChord);

            // The new group on alienChord side
            alienGroup = createAlienGroup();

            // Detect the pivot chord shared by the two groups, and "split" it for both groups
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
            alienBeams = new LinkedHashSet<>(alienChord.getBeams());

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
         * Look through the chords on the alienGroup to detect the one which is shared
         * by both this group and the alienGroup.
         *
         * @return the pivot chord found
         */
        private HeadChordInter detectPivotChord ()
        {
            List<AbstractChordInter> commons = getChords();
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
            for (AbstractBeamInter beam : new ArrayList<>(beams)) {
                // If beam is attached to pivotChord, skip it
                if (pivotBeams.contains(beam)) {
                    continue;
                }

                // Check every beam chord, for touching an alienBeam
                for (AbstractChordInter chord : beam.getChords()) {
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
         * This does not apply to beam hooks.
         */
        private void dispatchPivotBeams ()
        {
            // Select the tail beam of alienChord
            final AbstractBeamInter alienTailBeam = alienChord.getBeams().get(0);

            final List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            Boolean onAlienSide = null;

            // Inspect the pivot beams, from tail to head
            for (int ib = 0; ib < pivotBeams.size(); ib++) {
                AbstractBeamInter b = pivotChord.getBeams().get(ib);

                if (b.isHook()) {
                    continue;
                }

                if (onAlienSide == null) {
                    onAlienSide = alienBeams.contains(b);
                }

                if (b == alienTailBeam) {
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

        //------------------//
        // extractShortStem //
        //------------------//
        private StemInter extractShortStem (AbstractChordInter chord,
                                            int yStop)
        {
            final int stemDir = chord.getStemDir();
            final StemInter rootStem = chord.getStem();

            // Ordinate of head side of stem
            final int yStart = (int) Math.rint(
                    ((stemDir > 0) ? rootStem.getTop() : rootStem.getBottom()).getY());

            return rootStem.extractSubStem(yStart, yStop);
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
         * <p>
         * The beam group (old or alien) located at tail of pivot chord reuses pivot chord & stem.
         * The other group (the one closer to heads) must use a shorter stem (and chord).
         * <p>
         * Also we have to void exclusion between any beam and the opposite (mirror) chord/stem
         */
        private void splitChord ()
        {
            logger.debug("splitChord: {}", pivotChord);

            final SIGraph sig = pivotChord.getSig();
            final List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            final StemInter pivotStem = pivotChord.getStem();

            // Create a clone of pivotChord (heads are duplicated, but no stem or beams initially)
            HeadChordInter shortChord = pivotChord.duplicate(true);

            // The beams closer to tail will stay with pivotChord and its long stem
            // The beams closer to head (headBeams) will migrate to a new short chord & stem
            // For this, let's look at tail end of pivotChord
            final boolean aliensAtTail = alienBeams.contains(pivotBeams.get(0));
            final Set<AbstractBeamInter> headBeams = aliensAtTail ? beams : alienBeams;

            // Determine tail end for short stem, by walking on pivot from tail to head
            AbstractBeamInter firstHeadBeam = null;

            for (int i = 0; i < pivotBeams.size(); i++) {
                AbstractBeamInter beam = pivotBeams.get(i);

                if (headBeams.contains(beam)) {
                    firstHeadBeam = beam;

                    // Beam hooks to move?
                    for (AbstractBeamInter b : pivotBeams.subList(i + 1, pivotBeams.size())) {
                        if (b.isHook()) {
                            headBeams.add(b);
                        }
                    }

                    break;
                }
            }

            // Build shortStem
            Relation r = sig.getRelation(firstHeadBeam, pivotStem, BeamStemRelation.class);
            BeamStemRelation bsRel = (BeamStemRelation) r;
            int y = (int) Math.rint(bsRel.getExtensionPoint().getY());
            final StemInter shortStem = extractShortStem(pivotChord, y);
            shortChord.setStem(shortStem);
            sig.addEdge(shortStem, pivotStem, new StemAlignmentRelation());

            // Link mirrored heads to short stem
            for (Inter note : shortChord.getNotes()) {
                for (Relation hs : sig.getRelations(note.getMirror(), HeadStemRelation.class)) {
                    sig.addEdge(note, shortStem, hs.duplicate());
                }
            }

            // Update information related to headBeams
            for (AbstractBeamInter beam : headBeams) {
                // Avoid exclusion between head beam and pivotStem
                sig.addEdge(beam, pivotStem, new NoExclusion());

                // Move BeamStem relation from pivot to short
                Relation bs = sig.getRelation(beam, pivotStem, BeamStemRelation.class);

                if (bs != null) {
                    sig.removeEdge(bs);
                    sig.addEdge(beam, shortStem, bs);
                }
            }

            // Notify updates to both chords
            shortChord.invalidateCache();
            pivotChord.invalidateCache();

            measure.getStack().addInter(shortChord);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer maxSplitLoops = new Constant.Integer(
                "loops",
                10,
                "Maximum number of loops allowed for splitting beam groups");

        private final Scale.Fraction maxChordDy = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between a chord and a beam");
    }
}
