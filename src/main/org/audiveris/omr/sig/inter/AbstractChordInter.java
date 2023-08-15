//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C h o r d I n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.DurationFactor;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>AbstractChordInter</code> represents an ensemble of notes (rests, heads)
 * attached to the same stem if any, and that start on the same time slot in a part.
 * <p>
 * A chord made of small heads (typically used in Acciaccatura and Appoggiatura) is a
 * {@link SmallChordInter}.
 * A chord made of (non-small) heads or rests is called a "standard" chord by opposition to "small".
 * <p>
 * <b>NOTA:</b>We assume that all notes of a chord have the same duration.
 * Otherwise separate chord instances must be created.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractChordInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractChordInter.class);

    /** For comparing chords by head location ordinate. */
    public static final Comparator<AbstractChordInter> byHeadOrdinate = (c1,
                                                                         c2) -> Integer.compare(
                                                                                 c1.getHeadLocation().y,
                                                                                 c2.getHeadLocation().y);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Preferred voice ID, if any. */
    @XmlAttribute(name = "preferred-voice-id")
    private Integer preferredVoiceId;

    // Transient data
    //---------------

    /**
     * Sequence of beams if any this chord is linked to,
     * kept ordered from tail to head. Lazily computed.
     */
    protected List<AbstractBeamInter> beams;

    /** Location for chord head (head farthest from chord tail). Lazily computed. */
    protected Point headLocation;

    /** Location for chord tail. Lazily computed. */
    protected Point tailLocation;

    /** Containing measure. */
    protected Measure measure;

    /** Number of augmentation dots. */
    protected int dotsNumber;

    /** Containing slot, if any (no slot for whole/multi rests). */
    protected Slot slot;

    /** Start time (since beginning of the containing measure). */
    protected Rational timeOffset;

    /** Voice this chord belongs to. */
    protected Voice voice;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractChordInter ()
    {
    }

    /**
     * Creates a new <code>AbstractChordInter</code> object.
     *
     * @param grade the interpretation quality
     */
    protected AbstractChordInter (Double grade)
    {
        super((Glyph) null, null, null, grade);
    }

    /**
     * Protected constructor meant for SmallChordInter based on grace note.
     *
     * @param glyph
     * @param shape
     * @param grade
     */
    protected AbstractChordInter (Glyph glyph,
                                  Shape shape,
                                  Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    /**
     * Add the chord to containing measure.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        Point center = getCenter();

        if (center != null) {
            MeasureStack stack = sig.getSystem().getStackAt(center);

            if (stack != null) {
                stack.addInter(this);
            }
        } else {
            logger.debug("No bounds yet for chord {}", this);
        }
    }

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof AbstractNoteInter)) {
            throw new IllegalArgumentException(
                    "Only AbstractNoteInter can be added to AbstractChordInter");
        }

        EnsembleHelper.addMember(this, member);
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param measure containing measure
     */
    public void afterReload (Measure measure)
    {
        try {
            this.measure = measure;

            // Augmentation dot(s)?
            countDots();

            // Staff for rest chord
            if (this instanceof RestChordInter) {
                setStaff(getNotes().get(0).getStaff());
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    protected void computeLocations ()
    {
        AbstractNoteInter leading = getLeadingNote();

        if (leading == null) {
            return;
        }

        tailLocation = headLocation = leading.getCenter();
    }

    //-----------//
    // countDots //
    //-----------//
    /**
     * Count the number of augmentation dots for this chord.
     * TODO: What if, within the chord heads, some have a different count of dots?
     */
    public void countDots ()
    {
        final List<Inter> notes = getMembers();

        if (notes.isEmpty()) {
            return;
        }

        if (notes.size() == 1) {
            dotsNumber = ((AbstractNoteInter) notes.get(0)).getDotCount();
        } else {
            Population pop = new Population();

            for (Inter note : notes) {
                pop.includeValue(((AbstractNoteInter) note).getDotCount());
            }

            double val = pop.getMeanValue();
            double std = pop.getStandardDeviation();
            dotsNumber = (int) Math.rint(val);

            if (std != 0) {
                logger.debug("Inconsistent dots in {}, assumed {}", this, dotsNumber);

                if (dotsNumber == 0) {
                    // Delete the discarded augmentation relations
                    for (Inter note : notes) {
                        int count = ((AbstractNoteInter) note).getDotCount();

                        if (count != 0) {
                            for (Relation dn : sig.getRelations(note, AugmentationRelation.class)) {
                                Inter dot = sig.getOppositeInter(note, dn);

                                if (!dot.isManual()) {
                                    if (measure != null) {
                                        measure.removeInter(dot);
                                    }

                                    dot.remove();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isVip()) {
            logger.info("VIP {} counted dots: {}", this, dotsNumber);
        }
    }

    //--------------//
    // getBeamGroup //
    //--------------//
    /**
     * Report the group of beams this chord belongs to
     *
     * @return the related group of beams
     */
    public BeamGroupInter getBeamGroup ()
    {
        if (!getBeams().isEmpty()) {
            return getBeams().get(0).getGroup();
        } else {
            return null;
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the sequence of beams that are attached to this chord,
     * ordered from the tail to the head of the chord.
     *
     * @return the list of attached beams, perhaps empty
     */
    public List<AbstractBeamInter> getBeams ()
    {
        if (beams == null) {
            beams = new ArrayList<>();

            final StemInter stem = getStem();

            if (stem != null) {
                for (Relation bs : sig.getRelations(stem, BeamStemRelation.class)) {
                    AbstractBeamInter beam = (AbstractBeamInter) sig.getOppositeInter(stem, bs);
                    beams.add(beam);
                }

                final Point headLoc = getHeadLocation();

                if (headLoc != null) {
                    // Keep the sequence ordered by distance from chord tail
                    Collections.sort(
                            beams,
                            (AbstractBeamInter b1,
                             AbstractBeamInter b2) ->
                            {
                                int x = getCenter().x;
                                double y1 = LineUtil.yAtX(b1.getMedian(), x);
                                double y2 = LineUtil.yAtX(b2.getMedian(), x);
                                int yHead = headLoc.y;

                                return Double.compare(Math.abs(yHead - y2), Math.abs(yHead - y1));
                            });
                }
            }
        }

        return beams;
    }

    //-----------------------//
    // getBeamsOrFlagsNumber //
    //-----------------------//
    /**
     * Report the number of beams/flags that apply to this chord.
     *
     * @return beams count + flags count
     */
    public int getBeamsOrFlagsNumber ()
    {
        return getBeams().size() + getFlagsNumber();
    }

    //----------------//
    // getBottomStaff //
    //----------------//
    /**
     * Assuming a chord can embrace at most two staves, return the bottom one.
     *
     * @return the lower staff for this chord
     */
    public Staff getBottomStaff ()
    {
        final List<Inter> notes = getMembers();

        if (!notes.isEmpty()) {
            return notes.get(0).getStaff();
        }

        return null;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return super.getBounds();
    }

    //-------------------//
    // getBoundsWithDots //
    //-------------------//
    /**
     * Report the chord bounding box, including the related augmentation dot(s) if any.
     *
     * @return the bounding box of chord + augmentation dots
     */
    public Rectangle getBoundsWithDots ()
    {
        final Rectangle box = getBounds();
        final int n = getDotsNumber();

        if (n > 0) {
            // Expand box with the related dots
            for (Inter member : getMembers()) {
                AbstractNoteInter note = (AbstractNoteInter) member;
                AugmentationDotInter firstDot = note.getFirstAugmentationDot();

                if (firstDot != null) { // Safer
                    box.add(firstDot.getBounds());

                    if (n > 1) {
                        AugmentationDotInter secondDot = firstDot.getSecondAugmentationDot();

                        if (secondDot != null) { // Safer
                            box.add(secondDot.getBounds());
                        }
                    }
                }
            }
        }

        return box;
    }

    //--------------//
    // getChordName //
    //--------------//
    /**
     * Report the chordName (such as Am7), if any, related to this chord.
     *
     * @return the related chord name or null
     */
    public ChordNameInter getChordName ()
    {
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, ChordNameRelation.class)) {
                return (ChordNameInter) sig.getOppositeInter(this, rel);
            }
        }

        return null;
    }

    //---------------//
    // getDotsNumber //
    //---------------//
    /**
     * Report the number of augmentation dots that impact this chord
     *
     * @return the number of dots (should be the same for all notes within this chord)
     */
    public int getDotsNumber ()
    {
        return dotsNumber;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the real duration computed for this chord, including the
     * tuplet impact if any, with null value for whole/multi rest.
     *
     * @return the real chord/note duration, or null for a measure-long rest chord
     * @see AbstractNoteInter#getShapeDuration
     * @see #getDurationSansDotOrTuplet
     * @see #getDurationSansTuplet
     */
    public Rational getDuration ()
    {
        if (isMeasureRest()) {
            return null;
        } else {
            Rational sansTuplet = getDurationSansTuplet();

            DurationFactor tupletFactor = getTupletFactor();

            if (tupletFactor == null) {
                return sansTuplet;
            } else {
                return sansTuplet.times(tupletFactor);
            }
        }
    }

    //----------------------------//
    // getDurationSansDotOrTuplet //
    //----------------------------//
    /**
     * Report the duration of this chord, taking flag/beams into account, but not the
     * dot or tuplet impacts if any.
     * <p>
     * The duration is assumed to be the same for all notes of this chord, otherwise the chord must
     * be split.
     * <p>
     * Nota: this value is not cached, but computed on every call.
     *
     * @return the chord duration, with beam/flag but without dot and tuplet
     * @see AbstractNoteInter#getShapeDuration
     * @see #getDurationSansTuplet
     * @see #getDuration
     */
    public Rational getDurationSansDotOrTuplet ()
    {
        final List<Inter> notes = getMembers();
        Rational dur = null;

        if (!notes.isEmpty()) {
            // All note heads are assumed to be the same within one chord
            final Inter note = notes.get(0);
            final Shape noteShape = note.getShape();

            // Duration from note shape
            dur = noteShape.getNoteDuration();

            if (!noteShape.isRest()) {
                // Apply flags/beams for non-rests
                final int fbn = getBeamsOrFlagsNumber();

                if (fbn > 0) {
                    /*
                     * Some mirrored notes exhibit a void note head because the same
                     * head is shared by a half-note and at the same time by a beam group.
                     * In the case of the beam/flag side of the mirror, strictly speaking,
                     * the note head should be considered as black.
                     */
                    if ((noteShape == Shape.NOTEHEAD_VOID) && (note.getMirror() != null)) {
                        dur = Shape.NOTEHEAD_BLACK.getNoteDuration();
                    }

                    for (int i = 0; i < fbn; i++) {
                        dur = dur.divides(2);
                    }
                }
            }
        }

        return dur;
    }

    //-----------------------//
    // getDurationSansTuplet //
    //-----------------------//
    /**
     * Report the intrinsic duration of this chord, taking flag/beams and dots into
     * account, but not the tuplet impact if any.
     * <p>
     * The duration is assumed to be the same for all notes of this chord, otherwise the chord must
     * be split.
     * <p>
     * Nota: this value is not cached, but computed on every call.
     *
     * @return the intrinsic chord duration (no tuplet)
     * @see AbstractNoteInter#getShapeDuration
     * @see #getDurationSansDotOrTuplet
     * @see #getDuration
     */
    public Rational getDurationSansTuplet ()
    {
        final List<Inter> notes = getMembers();
        final Rational sansDot = getDurationSansDotOrTuplet();

        if ((sansDot != null) && !notes.isEmpty()) {
            // All note heads are assumed to be the same within one chord
            Inter note = notes.get(0);
            Shape noteShape = note.getShape();

            if (!sig.getSystem().isMeasureRestShape(noteShape)) {
                // Apply dotaugmentation
                if (dotsNumber == 1) {
                    return sansDot.times(new Rational(3, 2));
                } else if (dotsNumber == 2) {
                    return sansDot.times(new Rational(7, 4));
                }
            }
        }

        return sansDot;
    }

    //------------//
    // getEndTime //
    //------------//
    /**
     * Report the time when this chord ends
     *
     * @return chord ending time, since beginning of the measure
     */
    public Rational getEndTime ()
    {
        if (isMeasureRest()) {
            return null;
        }

        Rational chordDur = getDuration();

        if (chordDur == null) {
            return null;
        } else {
            if (timeOffset == null) {
                logger.info("{} No timeOffset for {}", (measure != null) ? measure : "", this);
                return null;
            } else {
                return timeOffset.plus(chordDur);
            }
        }
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    /**
     * Report the number of (individual) flags attached to the chord
     *
     * @return the number of individual flags
     */
    public int getFlagsNumber ()
    {
        return 0;
    }

    //------------------------//
    // getFollowingTiedChords //
    //------------------------//
    /**
     * Report the x-ordered collection of chords which are directly tied to the right of
     * this chord.
     *
     * @return the (perhaps empty) collection of tied chords
     */
    public List<AbstractChordInter> getFollowingTiedChords ()
    {
        final List<AbstractChordInter> tied = new ArrayList<>();

        if (sig != null) {
            for (Inter inter : getMembers()) {
                AbstractNoteInter note = (AbstractNoteInter) inter;
                Set<Relation> rels = sig.getRelations(note, SlurHeadRelation.class);

                for (Relation rel : rels) {
                    SlurInter slur = (SlurInter) sig.getOppositeInter(note, rel);
                    HeadInter leftNote = slur.getHead(HorizontalSide.LEFT);
                    HeadInter rightNote = slur.getHead(HorizontalSide.RIGHT);

                    if (slur.isTie() && (leftNote == note) && (rightNote != null)) {
                        tied.add(rightNote.getChord());
                    }
                }
            }

            Collections.sort(tied, Inters.byAbscissa);
        }

        return tied;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    /**
     * Report the location of the chord head (the head which is farthest from the tail).
     *
     * @return the head location
     */
    public Point getHeadLocation ()
    {
        if (headLocation == null) {
            computeLocations();
        }

        return new Point(headLocation);
    }

    //----------------//
    // getHighestNote //
    //----------------//
    /**
     * Report the note located in highest position.
     *
     * @return the highest note
     */
    public AbstractNoteInter getHighestNote ()
    {
        final List<Inter> notes = getMembers(); // Members are returned bottom up

        if (notes.isEmpty()) {
            logger.warn("No notes in chord " + this);

            return null;
        }

        return (AbstractNoteInter) notes.get(notes.size() - 1);
    }

    //----------------//
    // getLeadingNote //
    //----------------//
    /**
     * Report the highest note by default.
     *
     * @return the leading note
     */
    public AbstractNoteInter getLeadingNote ()
    {
        return getHighestNote();
    }

    //------------//
    // getMeasure //
    //------------//
    /**
     * Report the containing measure, if any.
     *
     * @return containing measure or null
     */
    public Measure getMeasure ()
    {
        return measure;
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * {@inheritDoc}
     *
     * @return the chord notes, ordered bottom up
     */
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byReverseCenterOrdinate);
    }

    //-----------------------------//
    // getNextChordInVoiceSequence //
    //-----------------------------//
    /**
     * Report the next chord, if any, declared as same voice in sequence for this one.
     *
     * @return the next chord in voice sequence found, or null
     */
    public AbstractChordInter getNextChordInVoiceSequence ()
    {
        if (sig != null) {
            for (Relation rel : sig.outgoingEdgesOf(this)) {
                if (rel instanceof NextInVoiceRelation) {
                    return (AbstractChordInter) sig.getOppositeInter(this, rel);
                }
            }
        }

        return null;
    }

    //----------//
    // getNotes //
    //----------//
    /**
     * Report the chord notes.
     *
     * @return the chord notes, ordered bottom up
     */
    public List<? extends Inter> getNotes ()
    {
        return getMembers();
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            final List<Inter> notes = getMembers();

            if (!notes.isEmpty()) {
                part = notes.get(0).getPart();
            }
        }

        return super.getPart();
    }

    //---------------------//
    // getPreferredVoiceId //
    //---------------------//
    /**
     * Report the preferred voice ID if any.
     *
     * @return the preferredVoiceId, perhaps null
     */
    public Integer getPreferredVoiceId ()
    {
        return preferredVoiceId;
    }

    //---------//
    // getSlot //
    //---------//
    /**
     * Report the slot this chord belongs to
     *
     * @return the containing slot (or null if none)
     */
    public Slot getSlot ()
    {
        return slot;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * If the chord embraces just one staff, this staff is returned, but if it embraces
     * two staves, null is returned and {@link #getStaves()} should be used instead.
     *
     * @return the single embraced staff, or null
     */
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            final Staff topStaff = getTopStaff();

            if (topStaff == getBottomStaff()) {
                staff = topStaff;
            }
        }

        return staff;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the sequence of 1 or 2 staves, ordered top down, embraced by this chord.
     *
     * @return a sequence of 1 or 2 staves
     */
    public List<Staff> getStaves ()
    {
        Staff staff1 = getTopStaff();
        Staff staff2 = getBottomStaff();

        if (staff1 != staff2) {
            return Arrays.asList(staff1, staff2);
        } else {
            return Arrays.asList(staff1);
        }
    }

    //---------//
    // getStem //
    //---------//
    /**
     * @return null
     */
    public StemInter getStem ()
    {
        return null;
    }

    //------------//
    // getStemDir //
    //------------//
    /**
     * @return 0 since no stem
     */
    public int getStemDir ()
    {
        return 0;
    }

    //-----------------//
    // getTailLocation //
    //-----------------//
    /**
     * Report the location of the tail of the chord
     *
     * @return the tail location
     */
    public Point getTailLocation ()
    {
        if (tailLocation == null) {
            computeLocations();
        }

        return new Point(tailLocation);
    }

    //---------------//
    // getTimeOffset //
    //---------------//
    /**
     * Report the time offset for this chord
     *
     * @return timeOffset chord time offset within the measure stack
     */
    public Rational getTimeOffset ()
    {
        return timeOffset;
    }

    //-------------//
    // getTopStaff //
    //-------------//
    /**
     * Assuming a chord can embrace at most two staves, return the top one.
     *
     * @return the upper staff for this chord
     */
    public Staff getTopStaff ()
    {
        final List<Inter> notes = getMembers();

        if (!notes.isEmpty()) {
            return notes.get(notes.size() - 1).getStaff();
        }

        return null;
    }

    //-----------//
    // getTuplet //
    //-----------//
    /**
     * Report the linked tuplet inter, if any
     *
     * @return the related tuplet or null
     */
    public TupletInter getTuplet ()
    {
        if (sig != null) {
            for (Relation tcRel : sig.getRelations(this, ChordTupletRelation.class)) {
                return (TupletInter) sig.getOppositeInter(this, tcRel);
            }
        }

        return null;
    }

    //-----------------//
    // getTupletFactor //
    //-----------------//
    /**
     * Report the chord tuplet factor, if any
     *
     * @return the factor to apply, or null
     */
    public DurationFactor getTupletFactor ()
    {
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, ChordTupletRelation.class)) {
                TupletInter tuplet = (TupletInter) sig.getOppositeInter(this, rel);

                return tuplet.getDurationFactor();
            }
        }

        return null;
    }

    //----------//
    // getVoice //
    //----------//
    /**
     * Report the (single) voice used by the notes of this chord
     *
     * @return the chord voice
     */
    @Override
    public Voice getVoice ()
    {
        return voice;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (sig != null) {
            if (sig.containsVertex(this)) {
                if (slot != null) {
                    sb.append(" slot#").append(slot.getId());
                }

                if (timeOffset != null) {
                    sb.append(" off:").append(timeOffset);
                }

                Rational dur = getDuration();

                if (dur != null) {
                    sb.append(" dur:").append(dur);
                }
            }
        } else {
            sb.append(" noSIG");
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Invalidate cached information. (following the addition or removal of a stem or note)
     */
    @Override
    public void invalidateCache ()
    {
        beams = null;
        bounds = null;
        headLocation = null;
        tailLocation = null;

        // Compute global grade based on contained notes (TODO: +stem as well?)
        setGrade(EnsembleHelper.computeMeanContextualGrade(this));

        // Include chord in stack
        if ((sig != null) && !isRemoved() && (measure == null)) {
            Point center = getCenter();

            if (center != null) {
                MeasureStack stack = sig.getSystem().getStackAt(center);

                if (stack != null) {
                    stack.addInter(this);
                }
            } else {
                logger.debug("invalidateCache. No bounds for chord {}", this);
            }
        }
    }

    //--------------//
    // isEmbracedBy //
    //--------------//
    /**
     * Check whether the notes of this chord stand within the given vertical range
     *
     * @param top    top of vertical range
     * @param bottom bottom of vertical range
     * @return true if all notes are within the given range
     */
    public boolean isEmbracedBy (Point top,
                                 Point bottom)
    {
        final List<Inter> notes = getMembers();

        for (Inter note : notes) {
            Point center = note.getCenter();

            if ((center.y >= top.y) && (center.y <= bottom.y)) {
                return true;
            }
        }

        return false;
    }

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the chord/note is a measure-long rest.
     *
     * @return true if measure rest
     */
    public boolean isMeasureRest ()
    {
        final List<Inter> notes = getMembers();

        if (!notes.isEmpty()) {
            return sig.getSystem().isMeasureRestShape(notes.get(0).getShape());
        }

        return false;
    }

    //--------//
    // isRest //
    //--------//
    /**
     * Report whether the chord is rest-based
     *
     * @return true if composed of rest
     */
    public boolean isRest ()
    {
        final List<Inter> notes = getMembers();

        if (!notes.isEmpty()) {
            return notes.get(0).getShape().isRest();
        }

        return false;
    }

    //-------------//
    // isWholeHead //
    //-------------//
    /**
     * Check whether the chord/note contains a whole head.
     *
     * @return true if whole head
     */
    public boolean isWholeHead ()
    {
        Shape shape = getMembers().get(0).getShape();

        return ShapeSet.StemLessHeads.contains(shape);
    }

    //-----------------//
    // justAssignVoice //
    //-----------------//
    /**
     * Just assign a voice to this chord, with no propagation.
     * Method meant for afterReload only.
     *
     * @see #setVoice(Voice)
     * @param voice the voice to assign
     */
    public void justAssignVoice (Voice voice)
    {
        this.voice = voice;
    }

    //----------//
    // pushTime //
    //----------//
    /**
     * Try to push time further off this chord.
     */
    private void pushTime ()
    {
        pushTimeViaVoiceSequence(); // Propagate via voice sequence if any
        pushTimeViaGroup(); // Propagate via beam group if any
        pushTimeViaTie(); // Propagate via tie if any
    }

    //------------------//
    // pushTimeViaGroup //
    //------------------//
    /**
     * Push time information via beam group structure, if any.
     * <p>
     * NOTA: A beam group should not go past measure borders, but we double-check for safety.
     */
    public void pushTimeViaGroup ()
    {
        // Propagate via beam group if any
        BeamGroupInter group = getBeamGroup();

        if (group != null) {
            AbstractChordInter prevChord = null;

            // Reminder: group.getAllChords() report head chords plus interleaved rest chords if any
            for (AbstractChordInter chord : group.getAllChords()) {
                if (prevChord != null) {
                    try {
                        if (measure == chord.measure) {
                            chord.setTimeOffset(prevChord.getEndTime());
                        }
                    } catch (Exception ex) {
                        logger.warn("{} Cannot compute chord time based on previous chord", chord);
                    }
                } else if (chord.getTimeOffset() == null) {
                    logger.warn("{} Computing beam group times with first chord not set", chord);
                }

                if (measure == chord.getMeasure()) {
                    prevChord = chord;
                }
            }

            final AbstractChordInter lastChord = group.getLastChord();

            if (measure == lastChord.measure) {
                // Warning: No more propagation via group!
                lastChord.pushTimeViaVoiceSequence(); // Propagate via voice sequence if any
                lastChord.pushTimeViaTie(); // Propagate via tie if any
            }
        }
    }

    //----------------//
    // pushTimeViaTie //
    //----------------//
    /**
     * Push time information via tie structure, if any.
     */
    public void pushTimeViaTie ()
    {
        // Propagate via tie if any
        List<AbstractChordInter> tied = getFollowingTiedChords();

        for (AbstractChordInter ch : tied) {
            // Make sure the tied chords belong to the same measure
            if (measure == ch.measure) {
                ch.setTimeOffset(getEndTime());

                ch.pushTime(); // If possible, push further
            }
        }
    }

    //--------------------------//
    // pushTimeViaVoiceSequence //
    //--------------------------//
    /**
     * Push time information via voice sequence structure, if any.
     */
    public void pushTimeViaVoiceSequence ()
    {
        // Propagate via voice sequence if any
        final AbstractChordInter ch = getNextChordInVoiceSequence();

        if ((ch != null) && (measure == ch.measure)) {
            ch.setTimeOffset(getEndTime());

            ch.pushTime(); // If possible, push further
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the chord from containing measure.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        if (measure != null) {
            measure.removeInter(this);
        }

        super.remove(extensive);
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof AbstractNoteInter)) {
            throw new IllegalArgumentException(
                    "Only AbstractNoteInter can be removed from ChordInter");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-------------//
    // resetTiming //
    //-------------//
    /**
     * Reset cached data related to timing.
     */
    public void resetTiming ()
    {
        slot = null;
        voice = null;
        timeOffset = null;
    }

    //----------------//
    // setAndPushTime //
    //----------------//
    /**
     * Set chord time offset and push it via groups (beam or tie).
     *
     * @param timeOffset new time offset value for this chord
     */
    public void setAndPushTime (Rational timeOffset)
    {
        setTimeOffset(timeOffset);

        if (timeOffset != null) {
            pushTime(); // If possible, push further
        }
    }

    //------------//
    // setMeasure //
    //------------//
    /**
     * Set the containing measure.
     *
     * @param measure the measure that contains this chord
     */
    public void setMeasure (Measure measure)
    {
        this.measure = measure;
    }

    //---------------------//
    // setPreferredVoiceId //
    //---------------------//
    /**
     * Assign a preferred voice ID.
     *
     * @param preferredVoiceId the preferredVoiceId to set
     */
    public void setPreferredVoiceId (Integer preferredVoiceId)
    {
        this.preferredVoiceId = preferredVoiceId;
    }

    //---------//
    // setSlot //
    //---------//
    /**
     * Return the containing time slot, if any
     *
     * @param slot containing slot or null
     */
    public void setSlot (Slot slot)
    {
        this.slot = slot;
    }

    //---------------//
    // setTimeOffset //
    //---------------//
    /**
     * Remember the time offset for this chord.
     * <p>
     * Information is <b>NOT</b> recursively propagated to other chords linked by beam or tie.
     * if needed, do it via {@link #pushTimeViaGroup()} and {@link #pushTimeViaTie()}.
     *
     * @param timeOffset chord starting time (counted since measure start)
     */
    public void setTimeOffset (Rational timeOffset)
    {
        this.timeOffset = timeOffset;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Set a voice to this chord, and recursively propagate to the other chords
     * related by beam or tie.
     *
     * @see #justAssignVoice(Voice)
     * @param voice the voice to set
     */
    public void setVoice (Voice voice)
    {

        if (this.voice != voice) {
            if (isVip()) {
                logger.info("VIP {} setVoice from {} to {}", this, this.voice, voice);
            }

            this.voice = voice;

            // Update the voice entity
            if ((voice != null) && !isMeasureRest() && !(this instanceof SmallChordInter)) {
                voice.addChord(this);
            }

            // Extend to other grouped chords if any
            final BeamGroupInter group = getBeamGroup();

            if (group != null) {
                group.setVoice(voice);
            }

            // Extend to the following tied chord(s?) as well
            List<AbstractChordInter> tied = getFollowingTiedChords();

            for (AbstractChordInter chord : tied) {
                // Make sure the tied chords belong to the same measure
                if (this.measure == chord.measure) {
                    chord.setVoice(voice);
                }
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * From a provided Chord collection, report the chord which has the
     * closest abscissa to a provided point.
     *
     * @param chords the collection of chords to browse
     * @param point  the reference point
     * @return the abscissa-wise closest chord
     */
    public static AbstractChordInter getClosestChord (Collection<AbstractChordInter> chords,
                                                      Point point)
    {
        AbstractChordInter bestChord = null;
        int bestDx = Integer.MAX_VALUE;

        for (AbstractChordInter chord : chords) {
            int dx = Math.abs(chord.getHeadLocation().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestChord = chord;
            }
        }

        return bestChord;
    }
}
