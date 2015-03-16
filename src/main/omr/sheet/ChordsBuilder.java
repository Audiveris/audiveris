//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d s B u i l d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.BlackHeadInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.VoidHeadInter;
import omr.sig.relation.AccidHeadRelation;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.BeamHeadRelation;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.NoExclusion;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;
import omr.sig.relation.Support;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;

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
    public void buildHeadChords ()
    {
        for (Part part : system.getParts()) {
            for (Staff staff : part.getStaves()) {
                List<ChordInter> staffChords = new ArrayList<ChordInter>(); // Chords in staff
                List<Inter> notes = sig.inters(staff, AbstractNoteInter.class); // Notes in staff
                Collections.sort(notes, Inter.byAbscissa); // Sort is not really needed
                logger.debug("Staff#{} notes:{}", staff.getId(), notes.size());

                for (Inter inter : notes) {
                    AbstractNoteInter note = (AbstractNoteInter) inter;

                    // Look for connected stems
                    List<Relation> rels = new ArrayList<Relation>(
                            sig.getRelations(note, HeadStemRelation.class));

                    if (rels.size() == 2) {
                        Collections.sort(rels, byHeadSide);
                    }

                    if (!rels.isEmpty()) {
                        AbstractHeadInter head = (AbstractHeadInter) note;
                        AbstractHeadInter mirrorHead = null;

                        for (Relation rel : rels) {
                            StemInter stem = (StemInter) sig.getOppositeInter(head, rel);

                            // A head with 2 stems needs to be logically duplicated
                            if (rels.size() == 2) {
                                if (((HeadStemRelation) rel).getHeadSide() == RIGHT) {
                                    mirrorHead = head;
                                    head = duplicateHead(head, stem);
                                }
                            }

                            // Look for compatible existing chord
                            List<ChordInter> chords = getStemChords(stem, staffChords);
                            final ChordInter chord;

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
                                chord.setStaff(staff);
                                chord.setStem(stem);
                                chord.addMember(head);
                                staffChords.add(chord);
                            }

                            if (mirrorHead != null) {
                                sig.addEdge(head.getChord(), mirrorHead.getChord(), new NoExclusion());
                                sig.addEdge(head, mirrorHead.getChord(), new NoExclusion());
                                sig.addEdge(head.getChord(), mirrorHead, new NoExclusion());
                            }
                        }
                    } else {
                        // Create a brand-new stem-less chord (whole note)
                        HeadChordInter chord = new HeadChordInter(-1);
                        sig.addVertex(chord);
                        chord.setStaff(staff);
                        chord.addMember(note);
                        staffChords.add(chord);
                    }
                }
            }
        }
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
    private AbstractHeadInter duplicateHead (AbstractHeadInter head,
                                             StemInter rightStem)
    {
        final AbstractHeadInter leftHead = head;
        final AbstractHeadInter rightHead;

        if (leftHead instanceof BlackHeadInter) {
            rightHead = ((BlackHeadInter) leftHead).duplicate();
        } else if (leftHead instanceof VoidHeadInter) {
            // TODO: perhaps void -> black when flag/beam involved
            rightHead = ((VoidHeadInter) leftHead).duplicate();
        } else {
            throw new IllegalArgumentException("Head not duplicable yet: " + head);
        }

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
            } else if (rel instanceof SlurHeadRelation) {
                // Connect the slur to both heads (this will be disambiguated later)
                SlurInter slur = (SlurInter) sig.getOppositeInter(leftHead, rel);
                HorizontalSide side = ((SlurHeadRelation) rel).getSide();
                sig.addEdge(slur, rightHead, new SlurHeadRelation(side));
            } else if (rel instanceof AccidHeadRelation) {
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
     * @param stem        the provided stem
     * @param staffChords all chords already defined in staff at hand
     * @return the perhaps empty collection of chords found for this stem
     */
    private List<ChordInter> getStemChords (StemInter stem,
                                            List<ChordInter> staffChords)
    {
        final List<ChordInter> found = new ArrayList<ChordInter>();

        for (ChordInter chord : staffChords) {
            if (chord.getStem() == stem) {
                found.add(chord);
            }
        }

        return found;
    }
}
