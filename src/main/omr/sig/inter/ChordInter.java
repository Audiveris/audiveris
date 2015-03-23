//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C h o r d I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.math.LineUtil;
import omr.math.Population;
import omr.math.Rational;

import omr.score.entity.DurationFactor;

import omr.sheet.BeamGroup;
import omr.sheet.Measure;
import omr.sheet.Slot;
import omr.sheet.Voice;

import omr.sig.SIGraph;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code ChordInter} represents an ensemble of entities (rests, notes) attached
 * to the same stem if any, and that start on the same time slot in a part.
 * <p>
 * <b>NOTA:</b>We assume that all notes of a chord have the same duration.
 * Otherwise separate chord instances must be created.
 *
 * @author Hervé Bitteur
 */
public abstract class ChordInter
        extends AbstractInter
        implements InterMutableEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordInter.class);

    /**
     * Compare two notes of the same chord, ordered by increasing distance from chord
     * head ordinate.
     */
    public static Comparator<AbstractNoteInter> noteHeadComparator = new Comparator<AbstractNoteInter>()
    {
        @Override
        public int compare (AbstractNoteInter n1,
                            AbstractNoteInter n2)
        {
            if (n1 == n2) {
                return 0;
            }

            ChordInter c1 = (ChordInter) n1.getEnsemble();

            if (c1 != n2.getEnsemble()) {
                logger.error("Ordering notes from different chords");
            }

            return c1.getStemDir() * (n2.getCenter().y - n1.getCenter().y);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Chord notes (both heads and rests). */
    protected final List<AbstractNoteInter> notes = new ArrayList<AbstractNoteInter>();

    /** Attached stem if any. */
    protected StemInter stem;

    /** Location for chord head (head farthest from chord tail). */
    protected Point headLocation;

    /** Location for chord tail. */
    protected Point tailLocation;

    /** Collection of beams this chord is connected to. */
    protected final List<AbstractBeamInter> beams = new ArrayList<AbstractBeamInter>();

    /** Containing measure. */
    protected Measure measure;

    //-- Resettable rhythm data ---
    //-----------------------------
    //
    /** Number of augmentation dots. */
    protected int dotsNumber;

    /** Containing slot, if any (no slot for whole/multi rests). */
    protected Slot slot;

    /** Voice this chord belongs to. */
    protected Voice voice;

    /** Start time since beginning of the containing measure. */
    protected Rational startTime;

    /** Ratio to get actual rawDuration WRT graphical notation. */
    protected DurationFactor tupletFactor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordInter} object.
     *
     * @param grade the interpretation quality
     */
    public ChordInter (double grade)
    {
        super(null, null, null, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    public static ChordInter getClosestChord (Collection<ChordInter> chords,
                                              Point point)
    {
        ChordInter bestChord = null;
        int bestDx = Integer.MAX_VALUE;

        for (ChordInter chord : chords) {
            int dx = Math.abs(chord.getHeadLocation().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestChord = chord;
            }
        }

        return bestChord;
    }

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
                            double y1 = LineUtil.intersectionAtX(b1.getMedian(), x).getY();
                            double y2 = LineUtil.intersectionAtX(b2.getMedian(), x).getY();
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
            note.setEnsemble(this);
            reset();
        } else {
            throw new IllegalArgumentException("Only AbstractNoteInter can be added to ChordInter");
        }
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
                logger.info("Inconsistent dots in {}, assumed {}", this, dotsNumber);

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
     * ordered from the tail of the chord.
     *
     * @return the attached beams
     */
    public List<AbstractBeamInter> getBeams ()
    {
        return beams;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (box == null) {
            if (stem != null) {
                box = stem.getBounds();
            }

            for (Inter member : notes) {
                if (box == null) {
                    box = member.getBounds();
                } else {
                    box = box.union(member.getBounds());
                }
            }
        }

        return super.getBounds();
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
                    final int fbn = getFlagsNumber() + beams.size();

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
            return startTime.plus(chordDur);
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
            if (stem.isDeleted()) {
                logger.warn("BINGO stem {} is deleted!", stem);
            }

            final Set<Relation> rels = sig.getRelations(stem, FlagStemRelation.class);

            for (Relation rel : rels) {
                FlagInter flagInter = (FlagInter) sig.getOppositeInter(stem, rel);
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
    public List<ChordInter> getFollowingTiedChords ()
    {
        final SIGraph sig = getSig();
        final List<ChordInter> tied = new ArrayList<ChordInter>();

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

        Collections.sort(tied, ChordInter.byAbscissa);

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

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the starting time for this chord
     *
     * @return startTime chord starting time (counted within the measure)
     */
    public Rational getStartTime ()
    {
        return startTime;
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
     * Report the stem direction of this chord
     *
     * @return -1 if stem is down, 0 if no stem, +1 if stem is up
     */
    public int getStemDir ()
    {
        if (stem == null) {
            return 0;
        } else {
            return Integer.signum(getHeadLocation().y - getTailLocation().y);
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
        startTime = null;
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

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Remember the starting time for this chord
     *
     * @param startTime chord starting time (counted within the measure)
     * @return true if OK
     */
    public boolean setStartTime (Rational startTime)
    {
        if (isVip()) {
            logger.info("VIP {} setStartTime from {} to {}", this, this.startTime, startTime);
        }

        // Already done?
        if (this.startTime == null) {
            this.startTime = startTime;
        } else if (!this.startTime.equals(startTime)) {
            logger.debug("{} Reassign startTime from {} to {}", this, this.startTime, startTime);

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
     * Assign a voice to this chord.
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
                List<ChordInter> tied = getFollowingTiedChords();

                for (ChordInter chord : tied) {
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
        } else {
            if (!isWholeRest()) {
                if (slot != null) {
                    voice.startChord(slot, this);
                }
            }
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        Rational sansTuplet = getDurationSansTuplet();

        if (sansTuplet != null) {
            sb.append(" dur:").append(sansTuplet);
        } else {
            sb.append(" noDur");
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
                        (stemBox.y + stemBox.height));
            }

            headLocation = new Point(tailLocation.x, leading.getCenter().y);
        }
    }

    //-------//
    // reset //
    //-------//
    private void reset ()
    {
        box = null;
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
