//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d s B u i l d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    //~ Static fields/initializers -----------------------------------------------------------------

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

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** System SIG. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // buildHeadChords //
    //-----------------//
    /**
     * Gather note heads by chords.
     * <p>
     * If a head is linked to a stem then it is part of the "stem-based" chord.
     * If a head is linked to 2 stems, one on left and one on right side, then we have 2 mirrored
     * chords around that same head.
     * <p>
     * Without stem, we may have chords made of a single whole head, or made of a vertical vector of
     * several whole heads.
     */
    public void buildHeadChords ()
    {
        for (Part part : system.getParts()) {
            // Stem-based chords defined so far in part
            List<AbstractChordInter> stemChords = new ArrayList<AbstractChordInter>();

            for (Staff staff : part.getStaves()) {
                List<Inter> heads = sig.inters(staff, HeadInter.class); // Heads in staff
                Collections.sort(heads, Inter.byCenterAbscissa);
                logger.debug("Staff#{} heads:{}", staff.getId(), heads.size());

                // Isolated heads (instances of WholeInter or SmallWholeInter) found so far in staff
                List<HeadInter> wholeHeads = new ArrayList<HeadInter>();

                for (Inter inter : heads) {
                    HeadInter head = (HeadInter) inter;

                    // Look for connected stems
                    List<Relation> rels = new ArrayList<Relation>(
                            sig.getRelations(head, HeadStemRelation.class));

                    if (rels.size() == 2) {
                        Collections.sort(rels, byHeadSide);
                    }

                    if (!rels.isEmpty()) {
                        HeadInter mirrorHead = null;

                        for (Relation rel : rels) {
                            StemInter stem = (StemInter) sig.getOppositeInter(head, rel);

                            // A head with 2 stems needs to be logically duplicated
                            if (rels.size() == 2) {
                                if (((HeadStemRelation) rel).getHeadSide() == RIGHT) {
                                    mirrorHead = head;
                                    head = duplicateHead(head, stem);
                                    head.setStaff(staff);
                                    staff.addNote(head);
                                }
                            }

                            // Look for compatible existing chord
                            List<AbstractChordInter> chords = getStemChords(stem, stemChords);
                            final AbstractChordInter chord;

                            if (!chords.isEmpty()) {
                                // At this point, we can have at most one chord per stem
                                // Join the chord
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
                                head.getChord().setMirror(mirrorHead.getChord());
                                mirrorHead.getChord().setMirror(head.getChord());
                                sig.addEdge(head, mirrorHead.getChord(), new NoExclusion());
                                sig.addEdge(head.getChord(), mirrorHead, new NoExclusion());
                            }
                        }
                    } else {
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
    public void buildRestChords ()
    {
        List<Inter> rests = sig.inters(RestInter.class);

        for (Inter rest : rests) {
            RestChordInter chord = new RestChordInter(-1);
            sig.addVertex(chord);
            chord.setStaff(system.getClosestStaff(rest.getCenter()));
            chord.addMember(rest);
            chord.getBounds(); // To make sure chord box is computed

            // Insert rest chord into measure (TODO: rest location is questionable)
            dispatchChord(chord);
        }
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
        Collections.sort(wholeHeads, Inter.byCenterOrdinate);

        for (int i = 0, iBreak = wholeHeads.size(); i < iBreak; i++) {
            final HeadInter h1 = wholeHeads.get(i);
            HeadChordInter chord = (HeadChordInter) h1.getEnsemble();

            if (chord != null) {
                continue; // Head already included in a chord
            }

            // Start a brand new stem-less chord (whole note)
            chord = new HeadChordInter(-1);
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

    //---------------//
    // dispatchChord //
    //---------------//
    private void dispatchChord (AbstractChordInter chord)
    {
        Part part = chord.getPart();
        Measure measure = part.getMeasureAt(chord.getCenter());
        measure.addInter(chord);
    }

    //----------------//
    // dispatchChords //
    //----------------//
    private void dispatchChords ()
    {
        for (Inter inter : sig.inters(AbstractChordInter.class)) {
            AbstractChordInter chord = (AbstractChordInter) inter;
            dispatchChord(chord);
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
        rightHead = leftHead.duplicate();

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

                // Since head-chords are now built BEFORE slurs, this case no longer applies.
                //            } else if (rel instanceof SlurHeadRelation) {
                //                // Connect the slur to both heads (this will be disambiguated later)
                //                SlurInter slur = (SlurInter) sig.getOppositeInter(leftHead, rel);
                //                HorizontalSide side = ((SlurHeadRelation) rel).getSide();
                //                sig.addEdge(slur, rightHead, new SlurHeadRelation(side));
            } else if (rel instanceof AlterHeadRelation) {
                // TODO
            } else if (rel instanceof AugmentationRelation) {
                // TODO: to which head(s) does the dot apply?
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
     * We can have: <ul>
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
    private List<AbstractChordInter> getStemChords (StemInter stem,
                                                    List<AbstractChordInter> stemChords)
    {
        final List<AbstractChordInter> found = new ArrayList<AbstractChordInter>();

        for (AbstractChordInter chord : stemChords) {
            if (chord.getStem() == stem) {
                found.add(chord);
            }
        }

        return found;
    }
}
