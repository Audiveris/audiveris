//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e a s u r e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.DurationFactor;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Voice.VoiceKind;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.MeasureRepeatInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SimileMarkInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Trimmable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Measure</code> represents a measure in a system <code>Part</code>.
 * <p>
 * It vertically embraces the staves (usually 1 or 2) of the containing part.
 * <p>
 * It is contained in a <code>MeasureStack</code> which embraces all the vertically aligned
 * <code>Measure</code> instances in the same <code>System</code>.
 * <p>
 * The image below represents one system, decomposed:
 * <ul>
 * <li>vertically in parts (and staves within their part)
 * <li>horizontally in measure stacks (and measures within their stack)
 * </ul>
 * <img src="doc-files/Measure-vs-MeasureStack.png" alt="Measure vs MeasureStack">
 *
 * @see MeasureStack
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "measure")
public class Measure
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Measure.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Indicates if the measure is in some abnormal rhythm status. */
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    /**
     * Part barline, if any, on the left side of the measure.
     * <p>
     * This left barline element exists only when we have a back-to-back repeat sign located between
     * two measures, in which case it is assigned to the second measure.
     */
    @XmlElement(name = "left-barline")
    private PartBarline leftBarline;

    /** Mid part barline, if any. */
    @XmlElement(name = "mid-barline")
    private PartBarline midBarline;

    /**
     * Part barline, if any, on the right side of the measure.
     * <p>
     * Except the case of a measure located on system right side with no ending barline,
     * all measures have a right barline.
     */
    @XmlElement(name = "right-barline")
    private PartBarline rightBarline;

    /**
     * We can have several Clefs per staff in a single measure.
     * <p>
     * These are implemented as a list, kept ordered by clef full abscissa.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "clefs")
    @Trimmable.Collection
    private final ArrayList<ClefInter> clefs = new ArrayList<>();

    /**
     * We can have at most one Key signature per staff in a single measure.
     * <p>
     * Keys may differ between staves.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "keys")
    @Trimmable.Collection
    private final LinkedHashSet<KeyInter> keys = new LinkedHashSet<>();

    /**
     * We can have at most one Time signature per staff in a single measure.
     * <p>
     * All time signatures are expected to be identical in the same measure stack.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "times")
    @Trimmable.Collection
    private final LinkedHashSet<AbstractTimeInter> timeSigs = new LinkedHashSet<>();

    /**
     * All head chords, both standard and small, in this measure.
     * <p>
     * Populated by CHORDS step.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "head-chords")
    @Trimmable.Collection
    private final LinkedHashSet<HeadChordInter> headChords = new LinkedHashSet<>();

    /**
     * All rest chords in this measure.
     * <p>
     * Populated by RHYTHMS step.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "rest-chords")
    @Trimmable.Collection
    private final LinkedHashSet<RestChordInter> restChords = new LinkedHashSet<>();

    /**
     * All flags in this measure.
     * <p>
     * Populated by SYMBOLS step.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "flags")
    @Trimmable.Collection
    private final LinkedHashSet<FlagInter> flags = new LinkedHashSet<>();

    /** All tuplets in this measure. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "tuplets")
    @Trimmable.Collection
    private final LinkedHashSet<TupletInter> tuplets = new LinkedHashSet<>();

    /** All augmentation dots in this measure. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "augmentations-dots")
    @Trimmable.Collection
    private final LinkedHashSet<AugmentationDotInter> augDots = new LinkedHashSet<>();

    /**
     * All measure repeat signs in this measure.
     * Deprecated, replaced by 'repeatSigns' field.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "similes")
    @Trimmable.Collection
    @Deprecated
    @SuppressWarnings("deprecation")
    private final LinkedHashSet<SimileMarkInter> OLD_similes = new LinkedHashSet<>();

    /** All measure repeat signs in this measure. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "repeat-signs")
    @Trimmable.Collection
    private final LinkedHashSet<MeasureRepeatInter> repeatSigns = new LinkedHashSet<>();

    /**
     * All voices within this measure, sorted by voice id.
     * <p>
     * Populated by RHYTHMS step.
     */
    @XmlElement(name = "voice")
    @Trimmable.Collection
    private final ArrayList<Voice> voices = new ArrayList<>();

    // Transient data
    //---------------

    /** To flag a dummy measure. */
    private boolean dummy;

    /** The containing part. */
    @Navigable(false)
    private Part part;

    /** The containing measure stack. */
    @Navigable(false)
    private MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private Measure ()
    {
        this.part = null;
    }

    /**
     * Creates a new <code>Measure</code> object.
     *
     * @param part the containing part
     */
    public Measure (Part part)
    {
        this.part = part;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------------//
    // addDummyMeasureRest //
    //---------------------//
    /**
     * Insert a measure-long rest, with related chord, on provided (dummy) staff
     * in this (dummy) measure.
     *
     * @param staff specified (dummy) staff in measure
     */
    public void addDummyMeasureRest (Staff staff)
    {
        // We use fakes that mimic a rest chord with its measure-long rest.
        //
        // NOTA: Because the fake entities (chord / rest) are not handled in any SIG and thus can't
        // be part of containment relations, we have to manually handle this via the use of:
        // - 'members' item in FakeChord
        // - 'chord'   item in FakeRest
        //
        class FakeChord
                extends RestChordInter
        {
            List<Inter> members;

            @Override
            public List<Inter> getMembers ()
            {
                return members;
            }

            @Override
            public DurationFactor getTupletFactor ()
            {
                return null;
            }

            @Override
            public boolean isMeasureRest ()
            {
                return true; // By construction
            }
        }

        class FakeRest
                extends RestInter
        {
            FakeChord chord;

            FakeRest (Staff staff,
                      Shape restShape,
                      double restPitch)
            {
                super(null, restShape, 0.0, staff, restPitch);
            }

            @Override
            public RestChordInter getChord ()
            {
                return chord;
            }
        }

        // Determine precise shape and pitch for measure-long rest (WHOLE_REST or BREVE_REST)
        final Shape restShape = staff.getSystem().getSmallestMeasureRestShape();
        final double restPitch = (restShape == Shape.WHOLE_REST) ? -1.5
                : (restShape == Shape.BREVE_REST ? -1 : 0);

        FakeRest measureRest = new FakeRest(staff, restShape, restPitch);
        FakeChord chord = new FakeChord();

        chord.setStaff(staff);
        chord.setTimeOffset(Rational.ZERO);
        chord.members = Collections.singletonList(measureRest);
        measureRest.chord = chord;

        addInter(chord);
        addVoice(Voice.createMeasureRestVoice(chord, this));
    }

    //----------//
    // addInter //
    //----------//
    /**
     * Include the provided inter into its proper set within this measure.
     *
     * @param inter the inter to include
     */
    public void addInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP addInter {} into {}", inter, this);
        }

        switch (inter) {
            case ClefInter clefInter -> {
                final ClefInter clef = clefInter;

                if (!clefs.contains(clef)) {
                    clefs.add(clef);

                    if ((clefs.size() > 1) && (clef.getCenter() != null)) {
                        Collections.sort(clefs, Inters.byFullCenterAbscissa);
                    }
                }
            }
            case KeyInter keyInter -> keys.add(keyInter);
            case AbstractTimeInter abstractTimeInter -> timeSigs.add(abstractTimeInter);
            case AbstractChordInter chord -> {
                chord.setMeasure(this);

                switch (chord) {
                    case HeadChordInter headChordInter -> headChords.add(headChordInter);
                    case RestChordInter restChordInter -> restChords.add(restChordInter);
                    default -> {}
                }
            }
            case FlagInter flagInter -> flags.add(flagInter);
            case TupletInter tupletInter -> tuplets.add(tupletInter);
            case AugmentationDotInter augmentationDotInter -> augDots.add(augmentationDotInter);
            case MeasureRepeatInter measureRepeat -> repeatSigns.add(measureRepeat);
            default -> logger.error("Attempt to use addInter() with {}", inter);
        }
    }

    //--------------------//
    // addOtherCollection //
    //--------------------//
    private void addOtherCollection (Collection<? extends Inter> otherCollection)
    {
        for (Inter inter : otherCollection) {
            addInter(inter);
        }
    }

    //----------//
    // addVoice //
    //----------//
    /**
     * Add a voice into measure.
     *
     * @param voice the voice to add
     */
    public void addVoice (Voice voice)
    {
        voices.add(voice);
    }

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        try {
            Trimmable.afterMarshal(this);
        } catch (Exception ex) {
            logger.error("Error afterMarshal {}", ex);
        }
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     */
    @SuppressWarnings("deprecation")
    public void afterReload ()
    {
        try {
            final SIGraph sig = part.getSystem().getSig();
            boolean upgraded = false;

            // Clefs, keys, timeSigs to fill measure
            List<Inter> measureInters = filter(
                    sig.inters(
                            new Class[] { ClefInter.class, KeyInter.class,
                                    AbstractTimeInter.class }));

            for (Inter inter : measureInters) {
                addInter(inter);
            }

            // Voices
            for (Voice voice : voices) {
                upgraded |= voice.afterReload(this);
            }

            // Chords
            for (AbstractChordInter chord : getHeadChords()) {
                chord.afterReload(this);
            }

            for (AbstractChordInter chord : getRestChords()) {
                chord.afterReload(this);
            }

            // Old similes replaced by repeatSigns
            if (!OLD_similes.isEmpty()) {
                for (SimileMarkInter simile : OLD_similes) {
                    repeatSigns.add(SimileMarkInter.replace(simile));
                }
                OLD_similes.clear();
                upgraded = true;
            }

            if (upgraded) {
                part.getSystem().getSheet().getStub().setUpgraded(true);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        part = (Part) parent;
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        try {
            Trimmable.beforeMarshal(this);
        } catch (Exception ex) {
            logger.error("Error beforeMarshal {}", ex);
        }
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this measure from its contained voices,
     * compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        // Check duration of each voice
        for (Voice voice : voices) {
            voice.checkDuration();
        }
    }

    //-------------//
    // clearVoices //
    //-------------//
    /**
     * Reset collection of voices for this measure.
     */
    public void clearVoices ()
    {
        voices.clear();
    }

    //--------//
    // filter //
    //--------//
    /**
     * Retrieve among the provided inters the ones contained in this measure.
     *
     * @param inters the provided inters
     * @return the contained inters
     */
    public List<Inter> filter (Collection<Inter> inters)
    {
        final int left = getLeft();
        final int right = getRight();
        final List<Inter> kept = new ArrayList<>();

        for (Inter inter : inters) {
            Point center = inter.getCenter();

            // Rough abscissa limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            // Check part
            Staff staff = inter.getStaff();

            if (staff != null) {
                if (staff.getPart().getMeasureAt(center) != this) {
                    continue;
                }
            } else {
                List<Staff> stavesAround = part.getSystem().getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
                logger.warn("Inter with no staff {}, assigned to staff#{}", inter, staff.getId());
                inter.setStaff(staff);

                if (!part.getStaves().contains(staff)) {
                    continue;
                }
            }

            // Precise abscissa limits
            if ((getAbscissa(LEFT, staff) <= center.x) && (center.x <= getAbscissa(RIGHT, staff))) {
                kept.add(inter);
            }
        }

        return kept;
    }

    //---------------//
    // filterByStaff //
    //---------------//
    /**
     * Filter the inters that relate to the provided staff.
     *
     * @param inters the input collection of inters
     * @param staff  the imposed staff
     * @return the inters that related to staff
     */
    private Set<Inter> filterByStaff (Set<? extends Inter> inters,
                                      Staff staff)
    {
        final Set<Inter> found = new LinkedHashSet<>();

        for (Inter inter : inters) {
            if (inter.getStaff() == staff) {
                found.add(inter);
            }
        }

        return found;
    }

    //-----------------//
    // generateVoiceId //
    //-----------------//
    /**
     * Generate a new voice ID, based on voice kind and current measure voices.
     *
     * @param kind the voice kind (HIGH, LOW, INFRA)
     * @return the generated ID, or -1 if none could be assigned.
     */
    public int generateVoiceId (VoiceKind kind)
    {
        final int idOffset = kind.idOffset();

        for (int id = idOffset + 1;; id++) {
            if (getVoiceById(id) == null) {
                return id;
            }
        }
    }

    //-------------//
    // getAbscissa //
    //-------------//
    /**
     * Report abscissa of desired measure side at ordinate of provided staff.
     * <p>
     * We consistently use the abscissa center of the right-most barline in
     * {@link StaffBarlineInter}.
     *
     * @param side  desired horizontal side
     * @param staff staff for ordinate
     * @return x value
     */
    public int getAbscissa (HorizontalSide side,
                            Staff staff)
    {
        Objects.requireNonNull(staff, "Null staff for Measure.getAbscissa()");
        return switch (side) {
            case LEFT -> {
                final PartBarline leftBar = getPartBarlineOn(LEFT);
                yield (leftBar != null) //
                        ? leftBar.getRightX(part, staff)
                        : staff.getAbscissa(LEFT);
            }
            case RIGHT -> (rightBarline != null) //
                    ? rightBarline.getRightX(part, staff)
                    : staff.getAbscissa(RIGHT);
        };
    }

    //---------------------//
    // getAugmentationDots //
    //---------------------//
    /**
     * Report the augmentation dots in this measure.
     *
     * @return the augmentation dots in measure
     */
    public Set<AugmentationDotInter> getAugmentationDots ()
    {
        return Collections.unmodifiableSet(augDots);
    }

    //---------------//
    // getBeamGroups //
    //---------------//
    /**
     * Report the collection of beam groups that are involved with this measure.
     * <p>
     * NOTA: A beam (and a beam group) may span several measures.
     *
     * @return the set of beam groups
     */
    public Set<BeamGroupInter> getBeamGroups ()
    {
        final Set<BeamGroupInter> beamGroups = new LinkedHashSet<>();

        for (AbstractChordInter chord : getStandardChords()) {
            final BeamGroupInter group = chord.getBeamGroup();

            if (group != null) {
                beamGroups.add(group);
            }
        }

        return beamGroups;
    }

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Report the first clef, if any, defined before this measure point
     * (looking in the beginning of the measure, then in previous measures in the same
     * system) while staying in the same physical staff.
     * <p>
     * NOTA: There is no point in looking before the current system, since any system staff is
     * required to start with a clef.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the latest clef defined, or null
     */
    public ClefInter getClefBefore (Point point,
                                    Staff staff)
    {
        // First, look in this measure, with same staff, going backwards
        ClefInter clef = getMeasureClefBefore(point, staff);

        if (clef != null) {
            return clef;
        }

        // Look in preceding measures, within the same system/part, within the same staff
        Measure measure = this;

        while ((measure = measure.getPrecedingInSystem()) != null) {
            clef = measure.getLastMeasureClef(staff);

            if (clef != null) {
                return clef;
            }
        }

        return null;
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * @return the clefs
     */
    public List<ClefInter> getClefs ()
    {
        return Collections.unmodifiableList(clefs);
    }

    //--------------------------//
    // getContainedPartBarlines //
    //--------------------------//
    /**
     * Report the PartBarlines this measure <b>strictly contains</b>
     * (as opposed to {@link #getPartBarlineOn(HorizontalSide)})
     *
     * @return the list of contained PartBarlines, perhaps empty but not null
     * @see #getPartBarlineOn(HorizontalSide)
     */
    public List<PartBarline> getContainedPartBarlines ()
    {
        List<PartBarline> list = new ArrayList<>();

        if (leftBarline != null) {
            list.add(leftBarline);
        }

        if (midBarline != null) {
            list.add(midBarline);
        }

        if (rightBarline != null) {
            list.add(rightBarline);
        }

        return list;
    }

    //---------------------//
    // getFirstMeasureClef //
    //---------------------//
    /**
     * Report the first clef (if any) in this measure, if tagged with the specified
     * staff index
     *
     * @param staffIndexInPart the imposed part-based staff index
     * @return the first clef, or null
     */
    public ClefInter getFirstMeasureClef (int staffIndexInPart)
    {
        // Going forward
        for (ClefInter clef : clefs) {
            if (clef.getStaff().getIndexInPart() == staffIndexInPart) {
                return clef;
            }
        }

        return null;
    }

    //----------//
    // getFlags //
    //----------//
    /**
     * Report the flags in this measure.
     *
     * @return the flags in measure
     */
    public Set<FlagInter> getFlags ()
    {
        return Collections.unmodifiableSet(flags);
    }

    //---------------//
    // getHeadChords //
    //---------------//
    /**
     * Report the head chords in this measure.
     *
     * @return the measure head chords
     */
    public Set<HeadChordInter> getHeadChords ()
    {
        return Collections.unmodifiableSet(headChords);
    }

    //--------------------//
    // getHeadChordsAbove //
    //--------------------//
    /**
     * Report the collection of head-chords whose head is located in the staff above the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of head chords
     */
    public Collection<HeadChordInter> getHeadChordsAbove (Point2D point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrAbove(point);
        Collection<HeadChordInter> found = new ArrayList<>();

        for (HeadChordInter chord : getHeadChords()) {
            if (chord.getBottomStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //--------------------//
    // getHeadChordsBelow //
    //--------------------//
    /**
     * Report the collection of head-chords whose head is located in the staff below the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of head chords
     */
    public Collection<HeadChordInter> getHeadChordsBelow (Point2D point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrBelow(point);
        Collection<HeadChordInter> found = new ArrayList<>();

        for (HeadChordInter chord : getHeadChords()) {
            if (chord.getTopStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y > point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //--------//
    // getKey //
    //--------//
    /**
     * Report the potential key signature in this measure for the specified staff index
     * in part.
     *
     * @param staffIndexInPart staff index in part
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKey (int staffIndexInPart)
    {
        for (KeyInter key : keys) {
            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                return key;
            }
        }

        return null;
    }

    //--------//
    // getKey //
    //--------//
    /**
     * Report the potential key signature in this measure for the specified staff.
     *
     * @param staff the desired staff
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKey (Staff staff)
    {
        return getKey(staff.getIndexInPart());
    }

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the first key, if any, found at the beginning of this measure, then in
     * previous measures in the same system, while staying in the same physical staff.
     * <p>
     * NOTA: There is no point in looking before the current system, since any system staff is
     * required to start with a key or nothing.
     *
     * @param staff the containing staff (cannot be null)
     * @return the latest key defined, or null
     */
    public KeyInter getKeyBefore (Staff staff)
    {
        // Look in current & preceding measures, within the same system/part, within the same staff
        final int idx = staff.getIndexInPart();
        Measure measure = this;

        while (measure != null) {
            KeyInter key = measure.getKey(idx);

            if (key != null) {
                return key;
            }

            measure = measure.getPrecedingInSystem();
        }

        return null; // No key previously defined
    }

    //--------------------//
    // getLastMeasureClef //
    //--------------------//
    /**
     * Report the last clef (if any) in this measure, with the specified staff.
     *
     * @param staff the imposed staff
     * @return the last clef, or null
     */
    public ClefInter getLastMeasureClef (Staff staff)
    {
        // Going backwards
        for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
            ClefInter clef = lit.previous();

            if (clef.getStaff() == staff) {
                return clef;
            }
        }

        return null;
    }

    //---------//
    // getLeft //
    //---------//
    private int getLeft ()
    {
        int left = Integer.MAX_VALUE;

        for (Staff staff : getPart().getStaves()) {
            left = Math.min(left, getAbscissa(LEFT, staff));
        }

        return left;
    }

    //--------------------//
    // getLeftPartBarline //
    //--------------------//
    /**
     * Report the PartBarline, if any, on left.
     *
     * @return left PartBarline or null
     */
    public PartBarline getLeftPartBarline ()
    {
        return leftBarline;
    }

    //----------------------//
    // getMeasureClefBefore //
    //----------------------//
    /**
     * Report the current clef, if any, defined within this measure and staff, and
     * located before this measure point.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the measure clef defined, or null
     */
    public ClefInter getMeasureClefBefore (Point point,
                                           Staff staff)
    {
        Objects.requireNonNull(point, "Point is null");
        Objects.requireNonNull(staff, "Staff is null");

        // Look in this measure, with same staff, going backwards
        for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
            ClefInter clef = lit.previous();

            if ((clef.getStaff() == staff) && (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        return null; // No clef previously defined in this measure and staff
    }

    //-------------------//
    // getMeasureRepeats //
    //-------------------//
    /**
     * Report the measure repeat signs in this measure.
     *
     * @return the measure repeat signs in measure
     */
    public Set<MeasureRepeatInter> getMeasureRepeats ()
    {
        return Collections.unmodifiableSet(repeatSigns);
    }

    //----------------------//
    // getMeasureRestChords //
    //----------------------//
    /**
     * Report all rest-chords meant to last the whole measure.
     *
     * @return the measure-long rest chords in measure
     */
    public Set<AbstractChordInter> getMeasureRestChords ()
    {
        final SystemInfo system = stack.getSystem();
        final Set<AbstractChordInter> set = new LinkedHashSet<>();

        for (RestChordInter chord : getRestChords()) {
            final List<Inter> members = chord.getMembers();

            if (!members.isEmpty() && system.isMeasureRestShape(members.get(0).getShape())) {
                set.add(chord);
            }
        }

        return set;
    }

    //-------------------//
    // getMidPartBarline //
    //-------------------//
    /**
     * Report the mid barline, if any.
     *
     * @return the mid barline or null
     */
    public PartBarline getMidPartBarline ()
    {
        return midBarline;
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the measure ID within page (in fact the related stack ID).
     * <p>
     * NOTA: @XmlAttribute annotation forces this information to be written in book file
     * (although it is not used when unmarshalling)
     *
     * @return the page ID of containing stack
     */
    @XmlAttribute(name = "id")
    @SuppressWarnings("unused")
    private String getPageId ()
    {
        if (stack != null) {
            return stack.getPageId();
        }

        return null;
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the containing part.
     *
     * @return the part that contains this measure
     */
    public Part getPart ()
    {
        return part;
    }

    //------------------//
    // getPartBarlineOn //
    //------------------//
    /**
     * Report the PartBarline, if any, located on desired side of the measure
     * (<b>regardless</b> whether it strictly belongs to the measure or not,
     * as opposed to {@link #getContainedPartBarlines()}).
     *
     * @param side desired side
     * @return the PartBarline found, or null
     * @see #getContainedPartBarlines()
     */
    public PartBarline getPartBarlineOn (HorizontalSide side)
    {
        return switch (side) {
            case LEFT -> {
                // Measure specific left bar?
                if (leftBarline != null) {
                    yield leftBarline;
                }

                // Previous measure in part?
                final Measure prevMeasure = getSibling(LEFT);

                if (prevMeasure != null) {
                    yield prevMeasure.getRightPartBarline();
                }

                // Part starting bar?
                if (part.getLeftPartBarline() != null) {
                    yield part.getLeftPartBarline();
                }

                yield null; // No barline found on LEFT
            }
            case RIGHT -> rightBarline; // Measure (right) bar?
        };
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure of this one, either in this system / part, or in the
     * preceding system / part, but still in the same page.
     *
     * @return the preceding measure, or null if not found in the page
     */
    public Measure getPrecedingInPage ()
    {
        // Look in current part
        Measure prevMeasure = getPrecedingInSystem();

        if (prevMeasure != null) {
            return prevMeasure;
        }

        Part precedingPart = getPart().getPrecedingInPage();

        if (precedingPart != null) {
            return precedingPart.getLastMeasure();
        } else {
            return null;
        }
    }

    //---------------------//
    // getPrecedingInScore //
    //---------------------//
    /**
     * Report the preceding measure of this one, either in this system / part, or in the
     * preceding system / part, or in the preceding page.
     *
     * @return the preceding measure, or null if not found in score
     */
    public Measure getPrecedingInScore ()
    {
        // Look in current part
        final Measure prevMeasure = getPrecedingInSystem();

        if (prevMeasure != null) {
            return prevMeasure;
        }

        // Look in preceding part in page
        Part precedingPart = part.getPrecedingInPage();

        if (precedingPart != null) {
            return precedingPart.getLastMeasure();
        }

        // Look in preceding part in preceding page
        final Page page = part.getSystem().getPage();
        final Score score = page.getScore();
        final Page precedingPage = score.getPrecedingPage(page);

        if (precedingPage != null) {
            final LogicalPart logicalPart = part.getLogicalPart();
            precedingPart = precedingPage.getLastSystem().getPhysicalPart(logicalPart);

            if (precedingPart != null) {
                return precedingPart.getLastMeasure();
            }
        }

        return null;
    }

    //----------------------//
    // getPrecedingInSystem //
    //----------------------//
    /**
     * Return the preceding measure within the same system.
     *
     * @return previous sibling measure in system, or null
     */
    public Measure getPrecedingInSystem ()
    {
        int index = part.getMeasures().indexOf(this);

        if (index > 0) {
            return part.getMeasures().get(index - 1);
        }

        return null;
    }

    //---------------//
    // getRestChords //
    //---------------//
    /**
     * Report the rest chords in this measure.
     *
     * @return all rest chords in this measure
     */
    public Set<RestChordInter> getRestChords ()
    {
        return Collections.unmodifiableSet(restChords);
    }

    //----------//
    // getRight //
    //----------//
    private int getRight ()
    {
        int right = 0;

        for (Staff staff : getPart().getStaves()) {
            right = Math.max(right, getAbscissa(RIGHT, staff));
        }

        return right;
    }

    //---------------------//
    // getRightPartBarline //
    //---------------------//
    /**
     * Report the right PartBarline, if any.
     *
     * @return the ending PartBarline or null
     */
    public PartBarline getRightPartBarline ()
    {
        return rightBarline;
    }

    //------------//
    // getSibling //
    //------------//
    /**
     * Report the sibling measure on the provided side.
     *
     * @param side horizontal side
     * @return sibling measure, or null if none
     */
    public Measure getSibling (HorizontalSide side)
    {
        final List<Measure> measures = part.getMeasures();
        final int index = measures.indexOf(this);

        return switch (side) {
            case LEFT -> (index > 0) ? measures.get(index - 1) : null;
            case RIGHT -> (index < (measures.size() - 1)) ? measures.get(index + 1) : null;
        };
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Report mid point of desired measure side at ordinate of provided staff
     *
     * @param side  desired horizontal side
     * @param staff staff for ordinate
     * @return mid point on desired side
     */
    public Point2D getSidePoint (HorizontalSide side,
                                 Staff staff)
    {
        return switch (side) {
            case LEFT -> {
                // Measure specific left bar?
                if (leftBarline != null) {
                    yield leftBarline.getStaffBarline(part, staff).getReferenceCenter();
                }

                // Previous measure in part?
                final Measure prevMeasure = getSibling(LEFT);
                if (prevMeasure != null) {
                    yield prevMeasure.getSidePoint(RIGHT, staff);
                }

                // Part starting bar?
                if (part.getLeftPartBarline() != null) {
                    yield part.getLeftPartBarline().getStaffBarline(part, staff)
                            .getReferenceCenter();
                }

                // No bar, use start of staff
                final int x = staff.getAbscissa(LEFT);
                yield new Point(x, staff.getMidLine().yAt(x));
            }

            case RIGHT -> {
                // Measure (right) bar?
                if (rightBarline != null) {
                    yield rightBarline.getStaffBarline(part, staff).getReferenceCenter();
                }

                // No bar, use end of staff
                final int x = staff.getAbscissa(RIGHT);
                yield new Point(x, staff.getMidLine().yAt(x));
            }
        };
    }

    //----------//
    // getStack //
    //----------//
    /**
     * @return the stack
     */
    public MeasureStack getStack ()
    {
        return stack;
    }

    //-------------------//
    // getStandardChords //
    //-------------------//
    /**
     * Report the collection of standard chords (head chords, rest chords) but not the
     * SmallChordInter instances.
     *
     * @return the set of all standard chords in this measure
     */
    public Set<AbstractChordInter> getStandardChords ()
    {
        final Set<AbstractChordInter> stdChords = new LinkedHashSet<>();
        stdChords.addAll(getHeadChords());
        stdChords.addAll(getRestChords());

        // Remove small head chords if any
        for (Iterator<AbstractChordInter> it = stdChords.iterator(); it.hasNext();) {
            if (it.next() instanceof SmallChordInter) {
                it.remove();
            }
        }

        return stdChords;
    }

    //-----------------------//
    // getStandardHeadChords //
    //-----------------------//
    /**
     * Report all standard (not small) head chords in this measure.
     *
     * @return all non-small head chords in measure
     */
    public Set<HeadChordInter> getStandardHeadChords ()
    {
        final Set<HeadChordInter> standardHeadChords = new LinkedHashSet<>(getHeadChords());

        for (Iterator<HeadChordInter> it = standardHeadChords.iterator(); it.hasNext();) {
            final HeadChordInter headChord = it.next();
            final List<Inter> notes = headChord.getMembers();

            if (notes.isEmpty() || notes.get(0).getShape().isSmallHead()) {
                it.remove();
            }
        }

        return standardHeadChords;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure (whatever the staff).
     *
     * @return the measure time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature ()
    {
        if (!timeSigs.isEmpty()) {
            return timeSigs.iterator().next();
        }

        return null; // Not found
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure for the specified staff index.
     *
     * @param staffIndexInPart imposed part-based staff index
     * @return the staff time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature (int staffIndexInPart)
    {
        for (AbstractTimeInter ts : timeSigs) {
            final int index = part.getStaves().indexOf(ts.getStaff());

            if (index == staffIndexInPart) {
                return ts;
            }
        }

        return null; // Not found
    }

    //-----------------//
    // getTimingInters //
    //-----------------//
    /**
     * Report the set of measure inters involved in timing.
     *
     * @return chords, beams, flags, augmentation dots, tuplets
     */
    public Set<Inter> getTimingInters ()
    {
        Set<Inter> set = new LinkedHashSet<>();

        for (BeamGroupInter beamGroup : getBeamGroups()) {
            set.addAll(beamGroup.getMembers()); // Beams, what for ????????????????
        }

        set.addAll(getHeadChords());
        set.addAll(getFlags());
        set.addAll(getRestChords());
        set.addAll(getAugmentationDots());
        set.addAll(getTuplets());

        return set;
    }

    //------------//
    // getTuplets //
    //------------//
    /**
     * Report the tuplets in this measure.
     *
     * @return all tuplets in measure
     */
    public Set<TupletInter> getTuplets ()
    {
        return Collections.unmodifiableSet(tuplets);
    }

    //--------------//
    // getVoiceById //
    //--------------//
    private Voice getVoiceById (int id)
    {
        for (Voice voice : voices) {
            if (voice.getId() == id) {
                return voice;
            }
        }

        return null;
    }

    //-----------//
    // getVoices //
    //-----------//
    /**
     * Report the sequence of voices in this measure.
     *
     * @return sequence of voices
     */
    public List<Voice> getVoices ()
    {
        return Collections.unmodifiableList(voices);
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the measure width
     *
     * @return measure width (at first staff in measure part)
     */
    public int getWidth ()
    {
        final Staff firstStaff = part.getFirstStaff();
        final int left = getAbscissa(LEFT, firstStaff);
        final int right = getAbscissa(RIGHT, firstStaff);

        return right - left;
    }

    //---------//
    // hasKeys //
    //---------//
    /**
     * Report whether there is at least one key signature, whatever the staff, in this
     * measure.
     *
     * @return true if one key was found
     */
    public boolean hasKeys ()
    {
        return !keys.isEmpty();
    }

    //-------------//
    // hasSameKeys //
    //-------------//
    /**
     * Report whether all key signatures, whatever the staff, are the same.
     *
     * @return true if identical
     */
    public boolean hasSameKeys ()
    {
        if (!hasKeys()) {
            return true;
        }

        final int staffCount = part.getStaves().size();
        Integer prevFifths = null;

        for (int index = 0; index < staffCount; index++) {
            KeyInter key = getKey(index);

            if (key == null) {
                return false;
            }

            if ((prevFifths != null) && !prevFifths.equals(key.getFifths())) {
                return false;
            }

            prevFifths = key.getFifths();
        }

        return true;
    }

    //----------------//
    // inferVoiceKind //
    //----------------//
    /**
     * Infer the voice kind for a voice started by the provided chord.
     *
     * @param chord the provided chord (assumed to be the first in voice)
     * @return the inferred voice kind
     */
    public VoiceKind inferVoiceKind (AbstractChordInter chord)
    {
        final Staff startingStaff = chord.getTopStaff();

        if (part.isMerged()) {
            return switch (chord.getStemDir()) {
                case -1 -> VoiceKind.HIGH;
                case +1 -> VoiceKind.LOW;
                default -> (startingStaff == part.getFirstStaff()) ? VoiceKind.HIGH : VoiceKind.LOW;
            };
        } else {
            final int index = part.getStaves().indexOf(startingStaff);

            if ((index >= 0) && (index < VoiceKind.values().length)) {
                return VoiceKind.values()[index];
            }

            logger.error("{} Weird staff index {} in part", startingStaff, index);

            return VoiceKind.HIGH;
        }
    }

    //------------//
    // isAbnormal //
    //------------//
    /**
     * Report whether this measure is abnormal.
     *
     * @return the abnormal status
     */
    public boolean isAbnormal ()
    {
        return abnormal;
    }

    //---------//
    // isDummy //
    //---------//
    /**
     * Tell whether this measure is dummy (in a dummy part).
     *
     * @return true if so
     */
    public boolean isDummy ()
    {
        return dummy;
    }

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the provided rest chord is a measure-long rest.
     *
     * @param restChord the provided rest chord
     * @return true if rest chord is actually a measure rest, false otherwise
     */
    public boolean isMeasureRest (RestChordInter restChord)
    {
        final Inter noteInter = restChord.getMembers().get(0);
        final Shape shape = noteInter.getShape();

        return stack.getSystem().isMeasureRestShape(shape);
    }

    //----------------//
    // mergeWithBelow //
    //----------------//
    /**
     * Merge this measure with the content of the measure just below.
     *
     * @param measureBelow the measure below
     */
    public void mergeWithBelow (Measure measureBelow)
    {
        // Barlines
        if (measureBelow.leftBarline != null) {
            if (leftBarline == null) {
                leftBarline = measureBelow.leftBarline;
            } else {
                leftBarline.mergeWithBelow(measureBelow.leftBarline);
            }
        }

        if (measureBelow.midBarline != null) {
            if (midBarline == null) {
                midBarline = measureBelow.midBarline;
            } else {
                midBarline.mergeWithBelow(measureBelow.midBarline);
            }
        }

        if (measureBelow.rightBarline != null) {
            if (rightBarline == null) {
                rightBarline = measureBelow.rightBarline;
            } else {
                rightBarline.mergeWithBelow(measureBelow.rightBarline);
            }
        }

        // Keys
        keys.addAll(measureBelow.keys);

        mergeWithOther(measureBelow);

        // Voices: rhythm reprocessed
        //
        measureBelow.switchItemsPart(part);
    }

    //----------------//
    // mergeWithOther //
    //----------------//
    /**
     * Merge this measure with the content of the provided other measure.
     *
     * @param other the other measure
     */
    private void mergeWithOther (Measure other)
    {
        // Keys: addressed by caller
        //
        addOtherCollection(other.clefs);
        addOtherCollection(other.timeSigs);
        addOtherCollection(other.headChords);
        addOtherCollection(other.restChords);
        addOtherCollection(other.flags);
        addOtherCollection(other.tuplets);
        addOtherCollection(other.augDots);
        addOtherCollection(other.repeatSigns);

        // Voices: addressed by caller
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this measure with the content of the following measure on the right.
     *
     * @param right the following measure
     */
    public void mergeWithRight (Measure right)
    {
        // Barlines
        if (midBarline == null) {
            midBarline = rightBarline;
        }

        setRightPartBarline(right.rightBarline);

        // Keys
        if (right.hasKeys()) {
            if (hasKeys()) {
                logger.warn("Attempt to merge keySigs from 2 measures {} and {}", this, right);
            } else {
                keys.addAll(right.keys);
            }
        }

        mergeWithOther(right);

        // Voices: when merging voidStart <- realStart
        voices.addAll(right.voices);

        // Voices: when removing a barline, rhythm is reprocessed, all voices recreated from scratch
    }

    //-------------//
    // purgeVoices //
    //-------------//
    /**
     * Purge measure voices.
     */
    public void purgeVoices ()
    {
        for (Iterator<Voice> it = voices.iterator(); it.hasNext();) {
            if (it.next().getFirstChord() == null) {
                it.remove();
            }
        }
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * Remove the provided inter from measure internals.
     *
     * @param inter the inter to remove
     */
    public void removeInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP removeInter {} from {}", inter, this);
        }

        switch (inter) {
            case ClefInter clefInter -> clefs.remove(clefInter);
            case KeyInter keyInter -> keys.remove(keyInter);
            case AbstractTimeInter abstractTimeInter -> timeSigs.remove(abstractTimeInter);
            case HeadChordInter headChordInter -> {
                headChords.remove(headChordInter);
                removeVoiceChord(headChordInter);
            }
            case RestChordInter restChordInter -> {
                restChords.remove(restChordInter);
                removeVoiceChord(restChordInter);
            }
            case FlagInter flagInter -> flags.remove(flagInter);
            case TupletInter tupletInter -> tuplets.remove(tupletInter);
            case AugmentationDotInter augmentationDotInter -> augDots.remove(augmentationDotInter);
            case MeasureRepeatInter repeatSign -> repeatSigns.remove(repeatSign);
            default -> logger.error("Attempt to use removeInter() with {}", inter);
        }
    }

    //------------------//
    // removeVoiceChord //
    //------------------//
    /**
     * Remove the provided chord from a containing voice if any.
     *
     * @param chord head or rest chord to be removed
     */
    private void removeVoiceChord (AbstractChordInter chord)
    {
        for (Voice voice : voices) {
            if (voice.removeChord(chord)) {
                if (voice.getChords().isEmpty()) {
                    voices.remove(voice);
                    break;
                }
            }
        }
    }

    //--------------//
    // removeVoices //
    //--------------//
    /**
     * Remove the provided voices.
     *
     * @param toRemove the voices to remove
     */
    public void removeVoices (Collection<Voice> toRemove)
    {
        voices.removeAll(toRemove);
    }

    //--------------//
    // renameVoices //
    //--------------//
    /**
     * Adjust voice ID per kind, in line with their order.
     */
    public void renameVoices ()
    {
        for (VoiceKind kind : VoiceKind.values()) {
            int id = kind.idOffset();

            for (int i = 0; i < voices.size(); i++) {
                final Voice voice = voices.get(i);

                if (voice.getKind() == kind) {
                    voice.setId(++id);
                }
            }
        }
    }

    //------------//
    // renderArea //
    //------------//
    /**
     * Render the measure area with provided color.
     *
     * @param g     graphics context
     * @param color provided color
     */
    public void renderArea (Graphics2D g,
                            Color color)
    {
        g.setColor(color);

        final int left = getLeft();
        final int right = getRight();

        final LineInfo firstLine = part.getFirstStaff().getFirstLine();
        int top = Math.min(firstLine.yAt(left), firstLine.yAt(right));

        final LineInfo lastLine = part.getLastStaff().getLastLine();
        int bottom = Math.max(lastLine.yAt(left), lastLine.yAt(right));

        // Most timing inters from measure
        for (Inter inter : getTimingInters()) {
            Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                top = Math.min(top, bounds.y);
                bottom = Math.max(bottom, bounds.y + bounds.height);
            }
        }

        g.fill(new Rectangle(left, top, right - left + 1, bottom - top + 1));
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Replicate this measure in a target part
     *
     * @param targetPart the target part
     * @return the replicate
     */
    public Measure replicate (Part targetPart)
    {
        Measure replicate = new Measure(targetPart);

        return replicate;
    }

    //-------------//
    // resetRhythm //
    //-------------//
    /**
     * Reset rhythm information in this measure (voices, beam groups, chords).
     */
    public void resetRhythm ()
    {
        setAbnormal(false);
        voices.clear();

        // Forward reset to every chord in measure (standard and small)
        for (AbstractChordInter chord : getHeadChords()) {
            chord.resetTiming();
        }

        for (AbstractChordInter chord : getRestChords()) {
            chord.resetTiming();
        }
    }

    //-------------//
    // setAbnormal //
    //-------------//
    /**
     * Mark this measure as being abnormal or not.
     *
     * @param abnormal new value
     */
    public void setAbnormal (boolean abnormal)
    {
        this.abnormal = abnormal;
    }

    //----------//
    // setDummy //
    //----------//
    /**
     * Flag this measure as dummy.
     */
    public void setDummy ()
    {
        dummy = true;
    }

    //--------------------//
    // setLeftPartBarline //
    //--------------------//
    /**
     * Set the PartBarline on left.
     *
     * @param leftBarline left barline
     */
    public void setLeftPartBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
    }

    //-------------------//
    // setMidPartBarline //
    //-------------------//
    /**
     * Set the middle PartBarline.
     *
     * @param midBarline mid barline
     */
    public void setMidPartBarline (PartBarline midBarline)
    {
        this.midBarline = midBarline;
    }

    //---------------------//
    // setRightPartBarline //
    //---------------------//
    /**
     * Assign the (right) PartBarline that ends this measure
     *
     * @param rightBarline the right PartBarline
     */
    public void setRightPartBarline (PartBarline rightBarline)
    {
        this.rightBarline = rightBarline;
    }

    //----------//
    // setStack //
    //----------//
    /**
     * @param stack the stack to set
     */
    public void setStack (MeasureStack stack)
    {
        this.stack = stack;
    }

    //------------//
    // sortVoices //
    //------------//
    /**
     * Sort measure voices.
     */
    public void sortVoices ()
    {
        Collections.sort(voices, Voices.byOrdinate);
    }

    //---------//
    // splitAt //
    //---------//
    /**
     * Split this measure at provided abscissae.
     * This is triggered by a barline insertion.
     *
     * @param xRefs split abscissa for each staff
     * @return the populated left new measure, the old (right) measure being half-purged
     */
    public Measure splitAt (Map<Staff, Integer> xRefs)
    {
        final Measure leftMeasure = new Measure(part);

        // part: assigned to leftmeasure
        // stack: assigned by caller
        // left barline ???
        // mid barline ???
        // right barline: assigned by caller
        //
        splitCollectionAt(xRefs, leftMeasure, clefs);

        // Keys
        leftMeasure.keys.addAll(keys);
        keys.clear();

        // Times
        leftMeasure.timeSigs.addAll(timeSigs);
        timeSigs.clear();

        splitCollectionAt(xRefs, leftMeasure, headChords);
        splitCollectionAt(xRefs, leftMeasure, restChords);
        splitCollectionAt(xRefs, leftMeasure, flags);
        splitCollectionAt(xRefs, leftMeasure, tuplets);
        splitCollectionAt(xRefs, leftMeasure, augDots);

        // voices: when a measure is split, rhythm is reprocessed, all voices recreated from scratch
        //
        return leftMeasure;
    }

    //-------------//
    // splitBefore //
    //-------------//
    /**
     * Split this measure before the provided staff.
     *
     * @param pivotStaff the provided staff
     * @param partBelow  the new part below, to be populated with lower half of measure
     */
    public void splitBefore (Staff pivotStaff,
                             Part partBelow)
    {
        final Measure measureBelow = new Measure(partBelow);
        final List<Staff> stavesBelow = partBelow.getStaves();

        // Barlines
        if (leftBarline != null) {
            measureBelow.leftBarline = leftBarline.splitBefore(pivotStaff);
        }

        if (midBarline != null) {
            measureBelow.midBarline = midBarline.splitBefore(pivotStaff);
        }

        if (rightBarline != null) {
            measureBelow.rightBarline = rightBarline.splitBefore(pivotStaff);
        }

        splitCollectionBefore(stavesBelow, measureBelow, keys);
        splitCollectionBefore(stavesBelow, measureBelow, clefs);
        Collections.sort(measureBelow.clefs, Inters.byFullCenterAbscissa); // Useful???
        splitCollectionBefore(stavesBelow, measureBelow, timeSigs);
        splitCollectionBefore(stavesBelow, measureBelow, headChords);
        splitCollectionBefore(stavesBelow, measureBelow, restChords);
        splitCollectionBefore(stavesBelow, measureBelow, flags);
        splitCollectionBefore(stavesBelow, measureBelow, tuplets);
        splitCollectionBefore(stavesBelow, measureBelow, augDots);
        splitCollectionBefore(stavesBelow, measureBelow, repeatSigns);

        // Voices: rhythm is reprocessed when part is vertically split (brace removal)
        //
        measureBelow.switchItemsPart(partBelow);
        partBelow.addMeasure(measureBelow);
        measureBelow.setStack(stack);
    }

    //-------------------//
    // splitCollectionAt //
    //-------------------//
    private void splitCollectionAt (Map<Staff, Integer> xRefs,
                                    Measure leftMeasure,
                                    Collection<? extends Inter> collection)
    {
        for (Iterator<? extends Inter> it = collection.iterator(); it.hasNext();) {
            final Inter inter = it.next();
            Staff staff = inter.getStaff();

            if ((staff == null) && (inter instanceof AbstractChordInter)) {
                staff = ((AbstractChordInter) inter).getTopStaff();
            }

            if (inter.getCenter().x <= xRefs.get(staff)) {
                leftMeasure.addInter(inter);
                it.remove();
            }
        }
    }

    //-----------------------//
    // splitCollectionBefore //
    //-----------------------//
    private void splitCollectionBefore (List<Staff> stavesBelow,
                                        Measure measureBelow,
                                        Collection<? extends Inter> collection)
    {
        for (Iterator<? extends Inter> it = collection.iterator(); it.hasNext();) {
            final Inter inter = it.next();
            Staff staff = inter.getStaff();

            if ((staff == null) && (inter instanceof AbstractChordInter)) {
                staff = ((AbstractChordInter) inter).getBottomStaff();
            }

            if (stavesBelow.contains(staff)) {
                measureBelow.addInter(inter);
                it.remove();
            }
        }
    }

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change the id of the provided voice to the provided id
     * (and change the other voice, if any, which owned the provided id).
     *
     * @param voice the voice whose id must be changed
     * @param id    the new id
     * @return the old voice owner of id, if any
     */
    public Voice swapVoiceId (Voice voice,
                              int id)
    {
        // Existing voice?
        Voice oldOwner = null;

        for (Voice v : getVoices()) {
            if (v.getId() == id) {
                oldOwner = v;

                break;
            }
        }

        // Change voice id
        int oldId = voice.getId();
        voice.setId(id);

        // Assign the oldId to the oldOwner, if any
        if (oldOwner != null) {
            oldOwner.setId(oldId);
        }

        return oldOwner;
    }

    //----------------------//
    // switchCollectionPart //
    //----------------------//
    private void switchCollectionPart (Part newPart,
                                       Collection<? extends Inter> collection)
    {
        for (Inter item : collection) {
            if ((item.getSpecificPart() != null) && (item.getSpecificPart() != newPart)) {
                item.setPart(newPart);
            }
        }
    }

    //-----------------//
    // switchItemsPart //
    //-----------------//
    /**
     * Assign the provided newPart to each item pointing to a different non-null part.
     *
     * @param newPart the provided newPart
     */
    public void switchItemsPart (Part newPart)
    {
        //TODO 3 barlines ???
        //
        switchCollectionPart(newPart, clefs);
        switchCollectionPart(newPart, keys);
        switchCollectionPart(newPart, timeSigs);
        switchCollectionPart(newPart, headChords);
        switchCollectionPart(newPart, restChords);
        switchCollectionPart(newPart, flags);
        switchCollectionPart(newPart, tuplets);
        switchCollectionPart(newPart, augDots);
        switchCollectionPart(newPart, repeatSigns);

        // voices: no part field
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Measure{");

        if (stack != null) {
            sb.append('#').append(stack.getPageId());
        } else {
            sb.append("-NOSTACK-");
        }

        if (part != null) {
            if ((stack != null) && (stack.getMeasures().size() > 1)) {
                sb.append("P").append(part.getId());
            }
        } else {
            sb.append("-NOPART-");
        }

        sb.append('}');

        return sb.toString();
    }
}
