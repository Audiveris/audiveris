//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d s B u i l d e r                                   //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemPortion;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code ChordsBuilder} works, at system level to gather, staff by staff, all
 * notes (heads and rests) into chords.
 * <p>
 * We assume a chord belongs to just one staff. To be modified if a chord could belong to several
 * staves (a part in that case).
 *
 * @author Hervé Bitteur
 */
public class ChordsBuilder
{

    private static final Logger logger = LoggerFactory.getLogger(ChordsBuilder.class);

    /** To sort head-stem relations left to right. */
    private static final Comparator<Relation> byHeadSide = new Comparator<Relation>()
    {
        @Override
        public int compare (Relation o1,
                            Relation o2)
        {
            final HorizontalSide s1 = ((HeadStemRelation) o1).getHeadSide();
            final HorizontalSide s2 = ((HeadStemRelation) o2).getHeadSide();

            return s1.compareTo(s2);
        }
    };

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** System SIG. */
    private final SIGraph sig;

    /**
     * Creates a new {@code ChordsBuilder} object.
     *
     * @param system the dedicated system
     */
    public ChordsBuilder (SystemInfo system)
    {
        this.system = system;
        sig = system.getSig();
    }

    //-----------------//
    // buildHeadChords //
    //-----------------//
    /**
     * Gather note heads by chords.
     */
    public void buildHeadChords ()
    {
        for (Part part : system.getParts()) {
            // Stem-based chords defined so far in part
            List<HeadChordInter> stemChords = new ArrayList<>();

            for (Staff staff : part.getStaves()) {
                // Heads in staff
                List<Inter> heads = sig.inters(staff, HeadInter.class);
                Collections.sort(heads, Inters.byCenterAbscissa);
                logger.debug("Staff#{} heads:{}", staff.getId(), heads.size());

                // Isolated heads (instances of WholeInter or SmallWholeInter) found so far in staff
                List<HeadInter> wholeHeads = new ArrayList<>();

                for (Inter inter : heads) {
                    HeadInter head = (HeadInter) inter;

                    if (!connectHead(head, staff, stemChords)) {
                        wholeHeads.add(head);
                    }
                }

                // Aggregate whole heads into vertical chords
                detectWholeVerticals(wholeHeads);
            }
        }

        // Dispatch head chords by containing measure
        dispatchChords();
    }

    //-----------------//
    // buildRestChords //
    //-----------------//
    /**
     * Allocate a chord for every rest.
     */
    public void buildRestChords ()
    {
        List<Inter> rests = sig.inters(RestInter.class);

        for (Inter rest : rests) {
            RestChordInter chord = new RestChordInter(-1);
            chord.setBounds(rest.getBounds());
            sig.addVertex(chord);
            chord.setStaff(system.getClosestStaff(rest.getCenter()));
            chord.addMember(rest);
        }
    }

    //---------------------//
    // checkCanonicalShare //
    //---------------------//
    /**
     * Check if the provided head exhibits a canonical configuration with its 2 stems.
     *
     * @param head provided head
     * @param rels the two head-stem relations
     * @return null if OK, the wrong head side if not OK
     */
    private HorizontalSide checkCanonicalShare (HeadInter head,
                                                List<Relation> rels)
    {
        boolean ok = true;
        final List<StemInter> stems = new ArrayList<>();

        for (HorizontalSide side : HorizontalSide.values()) {
            int idx = (side == LEFT) ? 0 : 1;
            StemPortion expected = (idx == 0) ? StemPortion.STEM_TOP : StemPortion.STEM_BOTTOM;
            HeadStemRelation rel = (HeadStemRelation) rels.get(idx);
            StemInter stem = (StemInter) sig.getOppositeInter(head, rel);
            stems.add(stem);

            Line2D stemLine = stem.computeExtendedLine();
            StemPortion portion = rel.getStemPortion(head, stemLine, null);

            if (portion != expected) {
                ok = false;
            }
        }

        if (!ok) {
            // Here we must discard one head-stem connection, but which one?
            // A stem attached to a side portion of a beam can no longer be deleted
            for (HorizontalSide side : HorizontalSide.values()) {
                int idx = (side == LEFT) ? 0 : 1;
                StemInter stem = stems.get(idx);
                Set<HeadInter> remainingHeads = stem.getHeads();
                remainingHeads.remove(head);

                for (Relation rel : sig.getRelations(stem, BeamStemRelation.class)) {
                    BeamStemRelation bsRel = (BeamStemRelation) rel;
                    BeamPortion beamPortion = bsRel.getBeamPortion();

                    if ((beamPortion != BeamPortion.CENTER) && remainingHeads.isEmpty()) {
                        return HorizontalSide.values()[1 - idx];
                    }
                }
            }

            // No beam-stuck stem found, use global quality...
            double leftGrade = stems.get(0).getGrade() * ((Support) rels.get(0)).getGrade();
            double rightGrade = stems.get(1).getGrade() * ((Support) rels.get(1)).getGrade();
            final int idx = (leftGrade < rightGrade) ? 0 : 1;

            return HorizontalSide.values()[idx];
        }

        return null;
    }

    //-------------//
    // connectHead //
    //-------------//
    /**
     * Try to connect the provided head into proper stem-based chord.
     * <p>
     * If a head is linked to a stem then it is part of the "stem-based" chord.
     * If a head is linked to 2 stems, one on left and one on right side on opposite directions,
     * then we have 2 mirrored chords around that same "shared" head which is then duplicated in
     * each mirrored chord.
     * <p>
     * NOTA: Only one head can be "shared" by 2 mirrored chords and it must be the end of each stem.
     * If there are several heads to be shared, then one of the stems is wrong and must be deleted
     * (with its containing chord).
     * <p>
     * Without stem, we may have chords made of a single whole head, or made of a vertical vector of
     * several whole heads.
     *
     * @param head       provided head
     * @param staff      related staff
     * @param stemChords (input/output) stem-based chords defined so far
     * @return true if stem connection was found, false otherwise (no stem, it's a whole)
     */
    private boolean connectHead (HeadInter head,
                                 Staff staff,
                                 List<HeadChordInter> stemChords)
    {
        if (head.isVip()) {
            logger.info("VIP connectHead {}", head);
        }

        // Look for connected stems
        List<Relation> rels = new ArrayList<>(sig.getRelations(head, HeadStemRelation.class));

        if (rels.size() == 2) {
            // A head with 2 stems needs to be logically duplicated
            // REDUCTION step was run before, but wrong head-stem configurations may still exist.
            // Check that configuration is canonical (bottom left, top right)
            // Check that head is the ending of each stem
            Collections.sort(rels, byHeadSide);

            HorizontalSide poorSide = checkCanonicalShare(head, rels);

            if (poorSide != null) {
                Relation poorRel = rels.get((poorSide == LEFT) ? 0 : 1);
                StemInter poorStem = (StemInter) sig.getOppositeInter(head, poorRel);
                sig.removeEdge(poorRel);
                rels.remove(poorRel);

                if (poorStem.getHeads().isEmpty()) {
                    logger.info("Deleting {} on {} side of {}", poorStem, poorSide, head);
                    remove(poorStem, stemChords);
                }
            }
        }

        if (rels.isEmpty()) {
            return false;
        }

        HeadInter mirrorHead = null;

        for (Relation rel : rels) {
            StemInter stem = (StemInter) sig.getOppositeInter(head, rel);

            if (rels.size() == 2) {
                if (((HeadStemRelation) rel).getHeadSide() == RIGHT) {
                    mirrorHead = head;
                    head = duplicateHead(head, stem);
                    head.setStaff(staff);
                    staff.addNote(head);
                }
            }

            // Look for compatible existing chord
            List<HeadChordInter> chords = getStemChords(stem, stemChords);
            final HeadChordInter chord;

            if (!chords.isEmpty()) {
                // At this point, we have exactly one chord per stem, so join the chord
                chord = chords.get(0);
                chord.addMember(head);
            } else {
                // Create a brand-new stem-based chord
                boolean isSmall = head.getShape().isSmall();
                chord = isSmall ? new SmallChordInter(-1) : new HeadChordInter(-1);
                sig.addVertex(chord);
                chord.setStem(stem);
                chord.addMember(head);
                stemChords.add(chord);
            }

            if (mirrorHead != null) {
                sig.addEdge(head, mirrorHead.getChord(), new NoExclusion());
                sig.addEdge(head.getChord(), mirrorHead, new NoExclusion());
            }
        }

        return true;
    }

    //----------------------//
    // detectWholeVerticals //
    //----------------------//
    /**
     * Review the provided collection of whole heads in a staff to come up with vertical
     * sequences (chords).
     *
     * @param wholeHeads the provided list of whole heads (no stem) in current staff
     */
    private void detectWholeVerticals (List<HeadInter> wholeHeads)
    {
        Collections.sort(wholeHeads, Inters.byCenterOrdinate);

        for (int i = 0, iBreak = wholeHeads.size(); i < iBreak; i++) {
            final HeadInter h1 = wholeHeads.get(i);
            AbstractChordInter chord = h1.getChord();

            if (chord != null) {
                continue; // Head already included in a chord
            }

            // Start a brand new stem-less chord (whole note)
            chord = new HeadChordInter(-1);
            chord.setBounds(h1.getBounds()); // Initial chord bounds
            sig.addVertex(chord);
            chord.setStaff(h1.getStaff());
            chord.addMember(h1);

            int p1 = (int) Math.rint(h1.getPitch());

            for (HeadInter h2 : wholeHeads.subList(i + 1, iBreak)) {
                final int p2 = (int) Math.rint(h2.getPitch());

                if (p2 > (p1 + 2)) {
                    break; // Vertical gap is too large, this is the end for current chord
                }

                // Check horizontal fit
                if (GeoUtil.xOverlap(chord.getBounds(), h2.getBounds()) > 0) {
                    chord.addMember(h2);
                    p1 = p2;
                }
            }
        }
    }

    //----------------//
    // dispatchChords //
    //----------------//
    private void dispatchChords ()
    {
        for (Inter inter : sig.inters(AbstractChordInter.class)) {
            AbstractChordInter chord = (AbstractChordInter) inter;
            Part part = chord.getPart();
            Measure measure = part.getMeasureAt(chord.getCenter());
            measure.addInter(chord);
        }
    }

    //---------------//
    // duplicateHead //
    //---------------//
    /**
     * Duplicate a note head which is shared between a stem on left and a stem on right.
     *
     * @param head      the head at hand (which will be the "left" head)
     * @param rightStem the stem on right
     * @return the duplicated head (for the "right" side)
     */
    private HeadInter duplicateHead (HeadInter head,
                                     StemInter rightStem)
    {
        // Handle inters
        final HeadInter leftHead = head;
        final HeadInter rightHead;

        // TODO: perhaps duplicate void -> black when flag/beam involved
        // But in this CHORDS step, the beams exist but not the flags yet
        rightHead = leftHead.duplicate();
        leftHead.getSig().addVertex(rightHead);
        rightHead.setMirror(leftHead);

        // Handle relations as well
        Set<Relation> supports = sig.getRelations(leftHead, Support.class);

        for (Relation rel : supports) {
            if (rel instanceof HeadStemRelation) {
                // Move right-located stem from left head to new (right) head.
                // Whatever the side, avoid overlap exclusion between those heads and stems
                StemInter stem = (StemInter) sig.getOppositeInter(leftHead, rel);
                HorizontalSide side = ((HeadStemRelation) rel).getHeadSide();

                if (side == LEFT) {
                    sig.addEdge(rightHead, stem, new NoExclusion());
                } else {
                    sig.removeEdge(rel);
                    sig.addEdge(rightHead, stem, rel);
                    sig.addEdge(leftHead, stem, new NoExclusion());
                }
            } else if (rel instanceof BeamHeadRelation) {
                // Move right-located beams from left head to new (right) head
                AbstractBeamInter beam = (AbstractBeamInter) sig.getOppositeInter(leftHead, rel);

                if (sig.getRelation(beam, rightStem, BeamStemRelation.class) != null) {
                    sig.removeEdge(rel);
                    sig.addEdge(beam, rightHead, rel);
                }
            } else if (rel instanceof HeadHeadRelation) {
                // Supporting head must be on *same* stem
                HeadInter otherHead = (HeadInter) sig.getOppositeInter(leftHead, rel);

                if (otherHead.getCenter().y < head.getCenter().y) {
                    // Migrate this relation from left mirror to right mirror
                    sig.removeEdge(rel);
                    sig.insertSupport(otherHead, rightHead, HeadHeadRelation.class);
                }
            } else if (rel instanceof AlterHeadRelation) {
                // TODO
            } else if (rel instanceof AugmentationRelation) {
                // To which head(s) does the dot apply?
                // This method runs in CHORDS step, and flags are not yet available (only beams are)
                // So we must postpone the decision.
            }
        }

        return rightHead;
    }

    //---------------//
    // getStemChords //
    //---------------//
    /**
     * Report the chord(s) currently attached to the provided stem.
     * <p>
     * We can have:
     * <ul>
     * <li>No chord found, simply because this stem has not yet been processed.</li>
     * <li>One chord found, this is the normal case.</li>
     * <li>Two chords found, when the same stem is "shared" by two chords (as in complex structures
     * in Dichterliebe example).</li>
     * </ul>
     *
     * @param stem       the provided stem
     * @param stemChords all stem-based chords already defined in part at hand
     * @return the perhaps empty collection of chords found for this stem
     */
    private List<HeadChordInter> getStemChords (StemInter stem,
                                                List<HeadChordInter> stemChords)
    {
        final List<HeadChordInter> found = new ArrayList<>();

        for (HeadChordInter chord : stemChords) {
            if (chord.getStem() == stem) {
                found.add(chord);
            }
        }

        return found;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the provided poor stem, and the chord it may be part of.
     *
     * @param poorStem   the stem to delete
     * @param stemChords the chords created so far
     */
    private void remove (StemInter poorStem,
                         List<HeadChordInter> stemChords)
    {
        // Make sure we have not already created a chord around the poor stem
        for (Relation rel : sig.getRelations(poorStem, ChordStemRelation.class)) {
            HeadChordInter chord = (HeadChordInter) sig.getOppositeInter(poorStem, rel);
            stemChords.remove(chord);
            chord.remove();
        }

        // Delete stem from sig
        poorStem.remove();
    }
}
