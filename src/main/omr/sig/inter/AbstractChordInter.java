//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C h o r d I n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

import omr.glyph.Shape;

import omr.math.LineUtil;
import omr.math.Population;
import omr.math.Rational;

import omr.sheet.DurationFactor;
import omr.sheet.Part;
import omr.sheet.Staff;
import omr.sheet.beam.BeamGroup;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.Slot;
import omr.sheet.rhythm.Voice;

import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;
import omr.sig.relation.ChordTupletRelation;

import omr.util.HorizontalSide;

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

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;

/**
 * Class {@code AbstractChordInter} represents an ensemble of notes (rests, heads)
 * attached to the same stem if any, and that start on the same time slot in a part.
 * <p>
 * <b>NOTA:</b>We assume that all notes of a chord have the same duration.
 * Otherwise separate chord instances must be created.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractChordInter
        extends AbstractInter
        implements InterMutableEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractChordInter.class);

    private static final List<AbstractBeamInter> NO_BEAM = Collections.emptyList();

    /**
     * Compare two notes of the same chord, ordered by increasing distance from chord
     * head ordinate.
     */
    public static final Comparator<AbstractNoteInter> noteHeadComparator = new Comparator<AbstractNoteInter>()
    {
        @Override
        public int compare (AbstractNoteInter n1,
                            AbstractNoteInter n2)
        {
            if (n1 == n2) {
                return 0;
            }

            AbstractChordInter c1 = (AbstractChordInter) n1.getEnsemble();

            if (c1 != n2.getEnsemble()) {
                logger.error("Comparing notes from different chords");
            }

            return c1.getStemDir() * (n1.getCenter().y - n2.getCenter().y);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /**
     * Sequence of chord notes (one or several heads or just a single rest),
     * kept ordered bottom-up.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "notes")
    protected final List<AbstractNoteInter> notes = new ArrayList<AbstractNoteInter>();

    /** Attached stem if any. */
    @XmlIDREF
    @XmlElement(name = "stem")
    protected StemInter stem;

    /** Sequence of beams this chord is linked to, kept ordered from tail to head. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "beams")
    protected List<AbstractBeamInter> beams = new ArrayList<AbstractBeamInter>();

    // Transient data
    //---------------
    //
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

    /** Start time, if any, since beginning of the containing measure. */
    protected Rational timeOffset;

    /** Voice this chord belongs to. */
    protected Voice voice;

    /** Ratio to get actual rawDuration WRT graphical notation. */
    protected DurationFactor tupletFactor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractChordInter} object.
     *
     * @param grade the interpretation quality
     */
    public AbstractChordInter (double grade)
    {
        super(null, null, null, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractChordInter ()
    {
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

    //---------//
    // addBeam //
    //---------//
    /**
     * Insert a beam as attached to this chord
     *
     * @param beam the attached beam
     */
    public void addBeam (AbstractBeamInter beam)
    {
        if (beams == null) {
            beams = new ArrayList<AbstractBeamInter>();
        }

        if (!beams.contains(beam)) {
            beams.add(beam);

            // Keep the sequence sorted from chord tail
            Collections.sort(
                    beams,
                    new Comparator<AbstractBeamInter>()
            {
                @Override
                public int compare (AbstractBeamInter b1,
                                    AbstractBeamInter b2)
                {
                    int x = stem.getCenter().x;
                    double y1 = LineUtil.yAtX(b1.getMedian(), x);
                    double y2 = LineUtil.yAtX(b2.getMedian(), x);
                    int yHead = getHeadLocation().y;

                    return Double.compare(Math.abs(yHead - y2), Math.abs(yHead - y1));
                }
            });
        }
    }

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (member instanceof AbstractNoteInter) {
            AbstractNoteInter note = (AbstractNoteInter) member;
            notes.add(note);

            if (notes.size() > 1) {
                Collections.sort(notes, AbstractPitchedInter.bottomUp);
            }

            note.setEnsemble(this);
            reset();
        } else {
            throw new IllegalArgumentException(
                    "Only AbstractNoteInter can be added to AbstractChordInter");
        }
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload (Measure measure)
    {
        try {
            this.measure = measure;

            // Augmentation dot(s)?
            countDots();

            if (sig == null) {
                logger.error("No sig for inter#" + getId());
            } else {
                // Tuplet?
                for (Relation rel : sig.getRelations(this, ChordTupletRelation.class)) {
                    TupletInter tuplet = (TupletInter) sig.getOppositeInter(this, rel);
                    setTupletFactor(tuplet.getDurationFactor());

                    break; // safer, although there should be at most one such relation.
                }
            }

            // Staff for rest chord
            if (this instanceof RestChordInter) {
                setStaff(getNotes().get(0).getStaff());
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-------------//
    // assignVoice //
    //-------------//
    /**
     * Just assign a voice to this chord.
     *
     * @param voice the voice to assign
     */
    public void assignVoice (Voice voice)
    {
        this.voice = voice;
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
        if (notes.isEmpty()) {
            return;
        }

        if (notes.size() == 1) {
            dotsNumber = notes.get(0).getDotCount();
        } else {
            Population pop = new Population();

            for (AbstractNoteInter note : notes) {
                pop.includeValue(note.getDotCount());
            }

            double val = pop.getMeanValue();
            double std = pop.getStandardDeviation();
            dotsNumber = (int) Math.rint(val);

            if (std != 0) {
                logger.debug("Inconsistent dots in {}, assumed {}", this, dotsNumber);

                if (dotsNumber == 0) {
                    // Delete the discarded augmentation relations
                    for (AbstractNoteInter note : notes) {
                        int count = note.getDotCount();

                        if (count != 0) {
                            for (Relation dn : sig.getRelations(note, AugmentationRelation.class)) {
                                Inter dot = sig.getOppositeInter(note, dn);
                                dot.delete();
                            }
                        }
                    }
                }
            }
        }

        if (isVip()) {
            logger.info("{} counted dots: {}", this, dotsNumber);
        }
    }

    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (measure != null) {
            measure.removeInter(this);
        }

        super.delete();
    }

    //--------------//
    // getBeamGroup //
    //--------------//
    /**
     * Report the group of beams this chord belongs to
     *
     * @return the related group of beams
     */
    public BeamGroup getBeamGroup ()
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
     * @return the attached beams
     */
    public List<AbstractBeamInter> getBeams ()
    {
        if (beams != null) {
            return beams;
        } else {
            return NO_BEAM;
        }
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
            if (stem != null) {
                bounds = stem.getBounds();
            }

            for (Inter member : notes) {
                final Rectangle memberBounds = member.getBounds();

                if (bounds == null) {
                    bounds = memberBounds;
                } else {
                    bounds = bounds.union(memberBounds);
                }
            }
        }

        return super.getBounds();
    }

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

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (stem != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append("stem:").append(stem);
        }

        return sb.toString();
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
     * @return the real chord/note duration, or null for a whole rest chord
     * @see AbstractNoteInter#getShapeDuration
     * @see #getDurationSansDotOrTuplet
     * @see #getDurationSansTuplet
     */
    public Rational getDuration ()
    {
        if (isWholeRest()) {
            return null;
        } else {
            Rational sansTuplet = getDurationSansTuplet();

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
     * A specific value (WHOLE_DURATION) indicates the whole/multi rest chord.
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
        Rational dur = null;

        if (!notes.isEmpty()) {
            // All note heads are assumed to be the same within one chord
            AbstractNoteInter note = notes.get(0);
            Shape noteShape = note.getShape();

            // Duration from note shape
            dur = AbstractNoteInter.getShapeDuration(noteShape);

            if (!noteShape.isWholeRest()) {
                // Apply flags/beams for non-rests
                if (!noteShape.isRest()) {
                    final int beamNb = (beams != null) ? beams.size() : 0;
                    final int fbn = getFlagsNumber() + beamNb;

                    if (fbn > 0) {
                        /*
                         * Some mirrored notes exhibit a void note head because the same
                         * head is shared by a half-note and at the same time by a beam group.
                         * In the case of the beam/flag side of the mirror, strictly speaking,
                         * the note head should be considered as black.
                         */
                        if ((noteShape == Shape.NOTEHEAD_VOID) && (note.getMirror() != null)) {
                            dur = AbstractNoteInter.getShapeDuration(Shape.NOTEHEAD_BLACK);
                        }

                        for (int i = 0; i < fbn; i++) {
                            dur = dur.divides(2);
                        }
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
     * A specific value (WHOLE_DURATION) indicates the whole/multi rest chord.
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
        Rational sansDot = getDurationSansDotOrTuplet();

        if ((sansDot != null) && !notes.isEmpty()) {
            // All note heads are assumed to be the same within one chord
            AbstractNoteInter note = notes.get(0);
            Shape noteShape = note.getShape();

            if (!noteShape.isWholeRest()) {
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
        if (isWholeRest()) {
            return null;
        }

        Rational chordDur = getDuration();

        if (chordDur == null) {
            return null;
        } else {
            return timeOffset.plus(chordDur);
        }
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    /**
     * Report the number of (individual) flags attached to the chord stem
     *
     * @return the number of individual flags
     */
    public int getFlagsNumber ()
    {
        int count = 0;

        if (stem != null) {
            final Set<Relation> rels = sig.getRelations(stem, FlagStemRelation.class);

            for (Relation rel : rels) {
                AbstractFlagInter flagInter = (AbstractFlagInter) sig.getOppositeInter(stem, rel);
                count += flagInter.getValue();
            }
        }

        return count;
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
        final List<AbstractChordInter> tied = new ArrayList<AbstractChordInter>();

        for (Inter inter : getMembers()) {
            AbstractNoteInter note = (AbstractNoteInter) inter;
            Set<Relation> rels = sig.getRelations(note, SlurHeadRelation.class);

            for (Relation rel : rels) {
                SlurInter slur = (SlurInter) sig.getOppositeInter(note, rel);
                AbstractHeadInter leftNote = slur.getHead(HorizontalSide.LEFT);
                AbstractHeadInter rightNote = slur.getHead(HorizontalSide.RIGHT);

                if (slur.isTie() && (leftNote == note) && (rightNote != null)) {
                    tied.add(rightNote.getChord());
                }
            }
        }

        Collections.sort(tied, AbstractChordInter.byAbscissa);

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

        return headLocation;
    }

    //----------------//
    // getLeadingNote //
    //----------------//
    /**
     * Report the note which if vertically farthest from stem tail.
     * For wholes & breves, it's the head itself.
     * For rest chords, it's the rest itself
     *
     * @return the leading note
     */
    public AbstractNoteInter getLeadingNote ()
    {
        if (!notes.isEmpty()) {
            if (stem != null) {
                // Find the note farthest from stem middle point
                Point middle = stem.getCenter();
                AbstractNoteInter bestNote = null;
                int bestDy = Integer.MIN_VALUE;

                for (AbstractNoteInter note : notes) {
                    int noteY = note.getCenter().y;
                    int dy = Math.abs(noteY - middle.y);

                    if (dy > bestDy) {
                        bestNote = note;
                        bestDy = dy;
                    }
                }

                return bestNote;
            } else {
                return notes.get(0);
            }
        } else {
            logger.warn("No notes in chord " + this);

            return null;
        }
    }

    //------------//
    // getMeasure //
    //------------//
    public Measure getMeasure ()
    {
        return measure;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return notes;
    }

    //----------//
    // getNotes //
    //----------//
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
            if (!notes.isEmpty()) {
                part = notes.get(0).getPart();
            }
        }

        return part;
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
     * two staves, null is returned then {@link #getStaves()} should be used instead.
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
     * @return the stem
     */
    public StemInter getStem ()
    {
        return stem;
    }

    //------------//
    // getStemDir //
    //------------//
    /**
     * Report the stem direction of this chord, from head to tail
     *
     * @return -1 if stem is up, 0 if no stem, +1 if stem is down
     */
    public int getStemDir ()
    {
        if (stem == null) {
            return 0;
        } else {
            return Integer.signum(getTailLocation().y - getHeadLocation().y);
        }
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

        return tailLocation;
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
        for (Relation tcRel : sig.getRelations(this, ChordTupletRelation.class)) {
            return (TupletInter) sig.getOppositeInter(this, tcRel);
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
        return tupletFactor;
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
        for (AbstractNoteInter note : notes) {
            Point center = note.getCenter();

            if ((center.y >= top.y) && (center.y <= bottom.y)) {
                return true;
            }
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
        if (!notes.isEmpty()) {
            return notes.get(0).getShape().isRest();
        }

        return false;
    }

    //-------------//
    // isWholeRest //
    //-------------//
    /**
     * Check whether the chord/note is a whole rest (not necessarily a measure rest)
     *
     * @return true if whole rest
     */
    public boolean isWholeRest ()
    {
        if (!notes.isEmpty()) {
            return notes.get(0).getShape().isWholeRest();
        }

        return false;
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (member instanceof AbstractNoteInter) {
            notes.remove((AbstractNoteInter) member);
            reset();
        } else {
            throw new IllegalArgumentException(
                    "Only AbstractNoteInter can be removed from ChordInter");
        }
    }

    //-------------//
    // resetTiming //
    //-------------//
    public void resetTiming ()
    {
        dotsNumber = 0;
        slot = null;
        voice = null;
        timeOffset = null;
        tupletFactor = null;
    }

    //------------//
    // setMeasure //
    //------------//
    public void setMeasure (Measure measure)
    {
        this.measure = measure;
    }

    //---------//
    // setSlot //
    //---------//
    public void setSlot (Slot slot)
    {
        this.slot = slot;
    }

    //----_----------//
    // setTimeOffset //
    //------_--------//
    /**
     * Remember the time offset for this chord
     *
     * @param timeOffset chord starting time (counted within the measure)
     * @return true if OK
     */
    public boolean setTimeOffset (Rational timeOffset)
    {
        if (isVip()) {
            logger.info("VIP {} setTimeOffset from {} to {}", this, this.timeOffset, timeOffset);
        }

        // Already done?
        if (this.timeOffset == null) {
            this.timeOffset = timeOffset;
        } else if (!this.timeOffset.equals(timeOffset)) {
            logger.debug("{} Reassign timeOffset from {} to {}", this, this.timeOffset, timeOffset);

            return false;
        }

        return true;
    }

    //---------//
    // setStem //
    //---------//
    /**
     * @param stem the stem to set
     */
    public void setStem (StemInter stem)
    {
        this.stem = stem;
        reset();
    }

    //-----------------//
    // setTupletFactor //
    //-----------------//
    /**
     * Assign a tuplet factor to this chord
     *
     * @param tupletFactor the factor to apply
     */
    public void setTupletFactor (DurationFactor tupletFactor)
    {
        this.tupletFactor = tupletFactor;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Assign a voice to this chord, and to the related ones.
     *
     * @param voice the voice to assign
     */
    public void setVoice (Voice voice)
    {
        // Already done?
        if (this.voice == null) {
            this.voice = voice;

            // Update the voice entity
            if (!isWholeRest()) {
                if (slot != null) {
                    voice.startChord(slot, this);
                }

                // Extend this voice to other grouped chords if any
                BeamGroup group = getBeamGroup();

                if (group != null) {
                    logger.debug(
                            "{} extending voice#{} to group#{}",
                            this,
                            voice.getId(),
                            group.getId());

                    group.setVoice(voice);
                }

                // Extend to the following tied chords as well
                List<AbstractChordInter> tied = getFollowingTiedChords();

                for (AbstractChordInter chord : tied) {
                    logger.debug("{} tied to {}", this, chord);

                    // Check the tied chords belong to the same measure
                    if (this.measure == chord.measure) {
                        logger.debug(
                                "{} extending voice#{} to tied chord#{}",
                                this,
                                voice.getId(),
                                chord.getId());

                        chord.setVoice(voice);
                    } else {
                        // Chords tied across measure boundary
                        logger.debug("{} Cross tie -> {}", this, chord);
                    }
                }
            }
        } else if (this.voice != voice) {
            logger.warn(
                    "{} Attempt to reassign voice from " + this.voice.getId() + " to " + voice.getId(),
                    this);
        } else if (!isWholeRest()) {
            if (slot != null) {
                voice.startChord(slot, this);
            }
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    @SuppressWarnings("unused")
    protected void beforeMarshal (Marshaller m)
    {
        // Nullify 'beams', so that no empty element appears in XML output.
        if ((beams != null) && beams.isEmpty()) {
            beams = null;
        }

        super.beforeMarshal(m);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (sig != null) {
            Rational sansTuplet = getDurationSansTuplet();

            if (sansTuplet != null) {
                sb.append(" dur:").append(sansTuplet);
            } else {
                sb.append(" noDur");
            }
        } else {
            sb.append(" NOSIG");
        }

        return sb.toString();
    }

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    private void computeLocations ()
    {
        AbstractNoteInter leading = getLeadingNote();

        if (leading == null) {
            return;
        }

        if (stem == null) {
            tailLocation = headLocation = leading.getCenter();
        } else {
            Rectangle stemBox = stem.getBounds();

            if (stem.getCenter().y < leading.getCenter().y) {
                // Stem is up
                tailLocation = new Point(stemBox.x + (stemBox.width / 2), stemBox.y);
            } else {
                // Stem is down
                tailLocation = new Point(
                        stemBox.x + (stemBox.width / 2),
                        ((stemBox.y + stemBox.height) - 1));
            }

            headLocation = new Point(tailLocation.x, leading.getCenter().y);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached information. (following the addition or removal of a stem or note)
     */
    private void reset ()
    {
        staff = null;
        bounds = null;
        headLocation = null;
        tailLocation = null;

        // Compute global grade based on contained notes (TODO: +stem as well?)
        if (!notes.isEmpty() && (sig != null)) {
            double gr = 0;

            for (Inter inter : notes) {
                gr += sig.computeContextualGrade(inter, false);
            }

            gr /= notes.size();
            setGrade(gr);
        }
    }
}
