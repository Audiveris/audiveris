//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C h o r d I n t e r                                  //
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
import org.audiveris.omr.sig.relation.ChordArpeggiatoRelation;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.ChordGraceRelation;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>HeadChordInter</code> is a AbstractChordInter composed of heads and possibly
 * a stem.
 * <p>
 * Heads are linked via {@link Containment} relation and stem via {@link ChordStemRelation}.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-chord")
public class HeadChordInter
        extends AbstractChordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadChordInter.class);

    /**
     * Compare two heads (assumed to be) of the same chord, ordered by increasing
     * distance from chord head ordinate.
     * It implements total ordering.
     */
    public static final Comparator<HeadInter> headComparator = (HeadInter n1,
                                                                HeadInter n2) ->
    {
        if (n1 == n2) {
            return 0;
        }

        final Point p1 = n1.getCenter();
        final Point p2 = n2.getCenter();
        final int yCmp = Integer.compare(p1.y, p2.y);

        if (yCmp != 0) {
            final AbstractChordInter chord = n1.getChord();

            return chord.getStemDir() * yCmp;
        }

        // Total ordering: use abscissa to separate heads with identical ordinates (rare case)
        return Integer.compare(p1.x, p2.x);
    };

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    protected HeadChordInter ()
    {
    }

    /**
     * Creates a new <code>HeadChordInter</code> object.
     *
     * @param grade the intrinsic grade
     */
    public HeadChordInter (Double grade)
    {
        super(grade);
    }

    /**
     * Protected constructor meant for SmallChordInter based on grace note.
     *
     * @param glyph
     * @param shape
     * @param grade
     */
    protected HeadChordInter (Glyph glyph,
                              Shape shape,
                              Double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    @Override
    protected void computeLocations ()
    {
        AbstractNoteInter leading = getLeadingNote();

        if (leading == null) {
            return;
        }

        final StemInter stem = getStem();

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

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        // First, check if stem contains the point
        final StemInter stem = getStem();

        if ((stem != null) && stem.contains(point)) {
            return true;
        }

        // Second, check if the ensemble of heads contains the point
        return super.contains(point);
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (just its heads, not its stem or its beams).
     * <p>
     * This duplication is needed when a chord is shared by two BeamGroups.
     *
     * @param toBlack should we duplicate to black head? (for void head)
     * @return a clone of this chord (including heads, but stem and beams are not copied)
     */
    public HeadChordInter duplicate (boolean toBlack)
    {
        // Beams are not copied
        HeadChordInter clone = new HeadChordInter(getGrade());
        sig.addVertex(clone);

        clone.setStaff(staff);

        // Notes (we make a deep copy of each note head)
        for (Inter note : getMembers()) {
            HeadInter head = (HeadInter) note;
            AbstractNoteInter newHead = null;

            switch (head.getShape()) {
            case NOTEHEAD_BLACK:
                newHead = head.duplicate();
                sig.addVertex(newHead);
                newHead.setMirror(head);

                break;

            case NOTEHEAD_VOID:
                newHead = toBlack ? head.duplicateAs(Shape.NOTEHEAD_BLACK) : head.duplicate();
                sig.addVertex(newHead);
                newHead.setMirror(head);

                break;

            default:
                logger.error("No duplication supported for {}", note);

                break;
            }

            if (newHead != null) {
                clone.addMember(newHead);
            }
        }

        return clone;
    }

    //---------------//
    // getArpeggiato //
    //---------------//
    /**
     * Report the chord arpeggiato if any.
     *
     * @return related arpeggiato or null
     */
    public ArpeggiatoInter getArpeggiato ()
    {
        for (Relation rel : sig.getRelations(this, ChordArpeggiatoRelation.class)) {
            return ((ArpeggiatoInter) sig.getOppositeInter(this, rel));
        }

        return null;
    }

    //------------------//
    // getArticulations //
    //------------------//
    /**
     * Report the chord articulations if any.
     *
     * @return list of articulations, perhaps empty
     */
    public List<ArticulationInter> getArticulations ()
    {
        List<ArticulationInter> found = null;

        for (Relation rel : sig.getRelations(this, ChordArticulationRelation.class)) {
            if (found == null) {
                found = new ArrayList<>();
            }

            found.add((ArticulationInter) sig.getOppositeInter(this, rel));
        }

        if (found == null) {
            return Collections.emptyList();
        }

        return found;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers()); // Based on heads

            final StemInter stem = getStem();

            if (stem != null) {
                if (bounds == null) {
                    bounds = stem.getBounds();
                } else {
                    bounds.add(getTailLocation());
                }
            }
        }

        return super.getBounds();
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    /**
     * Report the number of (individual) flags attached to the chord stem
     *
     * @return the number of individual flags
     */
    @Override
    public int getFlagsNumber ()
    {
        int count = 0;

        final StemInter stem = getStem();

        if ((stem != null) && !stem.isRemoved()) {
            final Set<Relation> rels = sig.getRelations(stem, FlagStemRelation.class);

            for (Relation rel : rels) {
                AbstractFlagInter flagInter = (AbstractFlagInter) sig.getOppositeInter(stem, rel);
                count += flagInter.getValue();
            }
        }

        return count;
    }

    //---------------//
    // getGraceChord //
    //---------------//
    /**
     * Report the grace chord, if any, which is linked on left side of this chord.
     * <p>
     * This method assumes there is either:
     * <ul>
     * <li>a direct chord-grace relation between the two chords, or
     * <li>a slur between one chord head and the grace head.
     * </ul>
     *
     * @return the linked grace chord if any
     */
    public SmallChordInter getGraceChord ()
    {
        // Direct
        for (Relation rel : sig.getRelations(this, ChordGraceRelation.class)) {
            return (SmallChordInter) sig.getOppositeInter(this, rel);
        }

        // Slur
        for (Inter interNote : getNotes()) {
            for (Relation rel : sig.getRelations(interNote, SlurHeadRelation.class)) {
                SlurInter slur = (SlurInter) sig.getOppositeInter(interNote, rel);
                HeadInter head = slur.getHead(HorizontalSide.LEFT);

                if ((head != null) && head.getShape().isSmallHead()) {
                    return (SmallChordInter) head.getChord();
                }
            }
        }

        return null;
    }

    //----------------//
    // getHeadsBounds //
    //----------------//
    /**
     * Report the bounding box of just the chord heads, without the stem if any.
     *
     * @return the heads bounding box
     */
    public Rectangle getHeadsBounds ()
    {
        return Entities.getBounds(getMembers());
    }

    //----------------//
    // getHeadsBounds //
    //----------------//
    /**
     * Report the bounding box of the heads located on desired side of the stem if any.
     *
     * @param stemSide desired side of the stem
     * @return the side heads bounding box, or null if none
     */
    public Rectangle getHeadsBounds (HorizontalSide stemSide)
    {
        final StemInter stem = getStem();

        if (stem == null) {
            return null;
        }

        final HorizontalSide headSide = stemSide.opposite();
        Rectangle rect = null;

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check side
            if (hsRel.getHeadSide() == headSide) {
                final Rectangle headBox = sig.getEdgeSource(rel).getBounds();

                if (rect == null) {
                    rect = headBox;
                } else {
                    rect.add(headBox);
                }
            }
        }

        return rect;
    }

    //----------------//
    // getLeadingNote //
    //----------------//
    /**
     * If the chord is stem-based, report the note vertically farthest from stem tail.
     * Otherwise return the highest note.
     *
     * @return the leading note
     */
    @Override
    public HeadInter getLeadingNote ()
    {
        final StemInter stem = getStem();

        if (stem == null) {
            return (HeadInter) super.getLeadingNote();
        }

        final List<Inter> notes = getMembers(); // Members are returned bottom up

        if (notes.isEmpty()) {
            logger.warn("No notes in chord " + this);

            return null;
        }

        // Find the note farthest from stem middle point
        Point middle = stem.getCenter();
        Inter bestNote = null;
        int bestDy = Integer.MIN_VALUE;

        for (Inter note : notes) {
            int noteY = note.getCenter().y;
            int dy = Math.abs(noteY - middle.y);

            if (dy > bestDy) {
                bestNote = note;
                bestDy = dy;
            }
        }

        return (HeadInter) bestNote;
    }

    //-----------//
    // getMirror //
    //-----------//
    /**
     * {@inheritDoc}
     * <p>
     * For a HeadChord, we check for a head member with a mirror.
     *
     * @return the "mirrored" chord if any
     */
    @Override
    public HeadChordInter getMirror ()
    {
        for (Inter inter : getNotes()) {
            HeadInter mirrorHead = (HeadInter) inter.getMirror();

            if (mirrorHead != null) {
                return mirrorHead.getChord();
            }
        }

        return null;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "HeadChord";
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the chord stem. It may be null temporarily such as when building the chord.
     *
     * @return the chord stem, if any
     */
    @Override
    public StemInter getStem ()
    {
        if (isRemoved()) {
            logger.debug("HeadChord#{} removed", id);

            return null;
        }

        for (Relation rel : sig.getRelations(this, ChordStemRelation.class)) {
            return (StemInter) sig.getOppositeInter(this, rel);
        }

        return null;
    }

    //------------//
    // getStemDir //
    //------------//
    /**
     * Report the stem direction of this chord, from head to tail
     *
     * @return -1 if stem is up, 0 if no stem, +1 if stem is down
     */
    @Override
    public int getStemDir ()
    {
        if (getStem() == null) {
            return 0;
        } else {
            return Integer.signum(getTailLocation().y - getHeadLocation().y);
        }
    }

    //----------//
    // hasSlash //
    //----------//
    /**
     * Report whether this chord has a slashed stem.
     *
     * @return true if so.
     */
    public boolean hasSlash ()
    {
        final StemInter stem = getStem();

        if (stem != null) {
            for (Relation rel : sig.getRelations(stem, FlagStemRelation.class)) {
                if (Shape.SMALL_FLAG_SLASH == sig.getOppositeInter(stem, rel).getShape()) {
                    return true;
                }
            }
        }

        return false;
    }

    //-----------//
    // preRemove //
    //-----------//
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        final Set<Inter> inters = new LinkedHashSet<>();

        inters.add(this);

        // Remove the chord stem if any as well
        final StemInter stem = getStem();

        if (stem != null) {
            inters.add(stem);
        }

        return inters;
    }

    //---------//
    // setStem //
    //---------//
    /**
     * Set the stem of this head chord.
     *
     * @param stem the stem to set
     */
    public final void setStem (StemInter stem)
    {
        Objects.requireNonNull(sig, "Chord not in SIG.");

        Relation rel = sig.getRelation(this, stem, ChordStemRelation.class);

        if (rel == null) {
            sig.addEdge(this, stem, new ChordStemRelation());
        }
    }
}
