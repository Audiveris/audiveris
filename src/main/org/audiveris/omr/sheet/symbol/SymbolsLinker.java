//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s L i n k e r                                   //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.TupletsBuilder;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.NumberInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.OctaveShiftInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.relation.ChordGraceRelation;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.DotFermataRelation;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>SymbolsLinker</code> defines final relations between certain symbols.
 * <p>
 * This process can take place only when chords candidates have survived all reductions.
 *
 * @author Hervé Bitteur
 */
public class SymbolsLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolsLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Dedicated system. */
    private final SystemInfo system;

    /** SIG for the system. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SymbolsLinker</code> object.
     *
     * @param system the dedicated system
     */
    public SymbolsLinker (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------------//
    // linkAugmentationDots //
    //----------------------//
    /**
     * Harmonize augmentation dots for each chord.
     * This may discard some augmentation dots.
     */
    private void linkAugmentationDots ()
    {
        for (MeasureStack stack : system.getStacks()) {
            for (AbstractChordInter chord : stack.getStandardChords()) {
                chord.countDots();
            }
        }
    }

    //--------------//
    // linkDynamics //
    //--------------//
    private void linkDynamics ()
    {
        for (Inter inter : sig.inters(DynamicsInter.class)) {
            DynamicsInter dynamics = (DynamicsInter) inter;

            try {
                dynamics.linkWithChord();
            } catch (Exception ex) {
                logger.warn("Error in linkDynamics for {} {}", inter, ex.toString(), ex);
            }
        }
    }

    //--------------//
    // linkFermatas //
    //--------------//
    /**
     * Try to link any fermata with chords (head or rest) or barline.
     * If not successful, the fermata candidate is deleted.
     */
    private void linkFermatas ()
    {
        final int profile = system.getProfile();

        // At this point a fermata arc exists only if it is related to a fermata dot
        List<Inter> arcs = sig.inters(FermataArcInter.class);

        for (Inter arcInter : arcs) {
            FermataArcInter arc = (FermataArcInter) arcInter;
            FermataDotInter dot = null;

            try {
                for (Relation rel : sig.getRelations(arc, DotFermataRelation.class)) {
                    dot = (FermataDotInter) sig.getOppositeInter(arcInter, rel);

                    break;
                }

                if (dot == null) {
                    arc.remove();

                    continue;
                }

                FermataInter fermata = FermataInter.createAdded(arc, dot, system);

                if (fermata.isVip()) {
                    logger.info("VIP linkFermatas on {}", fermata);
                }

                // Look for a related barline
                if (!fermata.linkWithBarline(profile)) {
                    // Look for a chord (head or rest) related to this fermata
                    if (!fermata.linkWithChord(profile)) {
                        // No link to barline, no link to chord, discard it
                        fermata.remove(); // Which also removes arc and dot members
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error in linkFermatas for {} {}", arc, ex.toString(), ex);
            }
        }
    }

    //------------//
    // linkGraces //
    //------------//
    /**
     * Link grace chords at their standard chord with a ChordGraceRelation.
     * <p>
     * This applies only to the right-most chord of a grace group.
     */
    private void linkGraces ()
    {
        SmallLoop:
        for (Inter chordInter : sig.inters(SmallChordInter.class)) {
            final SmallChordInter smallChord = (SmallChordInter) chordInter;

            if (smallChord.isVip()) {
                logger.info("VIP linkGraces for {}", smallChord);
            }

            // If part of a beam group, focus only on the right-most small chord
            final BeamGroupInter beamGroup = smallChord.getBeamGroup();
            if (beamGroup != null) {
                final List<AbstractChordInter> siblings = beamGroup.getAllChords();
                if (!siblings.isEmpty() && (smallChord != siblings.get(siblings.size() - 1))) {
                    continue;
                }
            }

            try {
                // Check indirect relation: grace-head <- slur <- chord-head
                for (Inter interNote : smallChord.getNotes()) {
                    for (Relation rel : sig.getRelations(interNote, SlurHeadRelation.class)) {
                        final SlurHeadRelation shRel = (SlurHeadRelation) rel;
                        if (shRel.getSide() == HorizontalSide.LEFT) {
                            final SlurInter slur = (SlurInter) sig.getOppositeInter(interNote, rel);
                            final HeadInter head = slur.getHead(HorizontalSide.RIGHT);

                            if (head != null) {
                                final HeadChordInter ch = head.getChord();
                                sig.addEdge(ch, smallChord, new ChordGraceRelation());
                                continue SmallLoop;
                            }
                        }
                    }
                }

                // No slur, use proximity
                final Collection<Link> links = smallChord.searchLinks(system);
                if (!links.isEmpty()) {
                    final Link link = links.iterator().next();
                    link.applyTo(smallChord);
                }
            } catch (Exception ex) {
                logger.warn("Error in linkGraces for {} {}", smallChord, ex.toString(), ex);
            }
        }
    }

    //-------------//
    // linkNumbers //
    //-------------//
    /**
     * Link and convert all NumberInter's.
     */
    private void linkNumbers ()
    {
        for (Inter inter : sig.inters(NumberInter.class)) {
            final NumberInter nb = (NumberInter) inter;

            try {
                nb.linkAndConvert();
                nb.remove(); // Even if not linked!
            } catch (Exception ex) {
                logger.warn("Error in linkNumbers for {} {}", nb, ex.toString(), ex);
            }
        }
    }

    //------------------//
    // linkOctaveShifts //
    //------------------//
    /**
     * Link an OctaveShift (left and right) to proper chords if any.
     */
    private void linkOctaveShifts ()
    {
        for (Inter inter : sig.inters(OctaveShiftInter.class)) {
            final OctaveShiftInter os = (OctaveShiftInter) inter;

            try {
                if (os.isVip()) {
                    logger.info("VIP linkOctaveShifts for {}", os);
                }

                final Collection<Link> links = os.searchLinks(system);

                if (!links.isEmpty()) {
                    for (Link link : links) {
                        link.applyTo(os);
                    }
                } else {
                    logger.info("No chord linked to {}", os);
                }
            } catch (Exception ex) {
                logger.warn("Error in linkOctaveShifts for {} {}", os, ex.toString(), ex);
            }
        }
    }

    //-----------------//
    // linkOneSentence //
    //-----------------//
    /**
     * Link a text sentence, according to its role, with its related entity if any.
     *
     * @param sentence the sentence to link
     */
    public void linkOneSentence (SentenceInter sentence)
    {
        try {
            if (sentence.isVip()) {
                logger.info("VIP linkOneSentence for {}", sentence);
            }

            final TextRole role = sentence.getRole();

            if (role == null) {
                logger.info("No role for {}", sentence);

                return;
            }

            final Point2D location = sentence.getLocation();
            final Rectangle bounds = sentence.getBounds();
            final Scale scale = system.getSheet().getScale();

            switch (role) {
            case Lyrics ->
            {
                // Map each syllable with proper chord, in assigned staff
                for (Inter wInter : sentence.getMembers()) {
                    final LyricItemInter item = (LyricItemInter) wInter;
                    final int profile = Math.max(item.getProfile(), system.getProfile());
                    item.mapToChord(profile);
                }
            }

            case Direction ->
            {
                // Map direction with proper chord
                MeasureStack stack = system.getStackAt(location);

                if (stack == null) {
                    logger.info(
                            "No measure stack for direction {} {}",
                            sentence,
                            sentence.getValue());
                } else {
                    int xGapMax = scale.toPixels(ChordSentenceRelation.getXGapMax());
                    Rectangle fatBounds = new Rectangle(bounds);
                    fatBounds.grow(xGapMax, 0);

                    AbstractChordInter chord = stack.getEventChord(location, fatBounds);

                    if (chord != null) {
                        sig.addEdge(chord, sentence, new ChordSentenceRelation());
                    } else {
                        logger.info("No chord near direction {} {}", sentence, sentence.getValue());
                    }
                }
            }

            case PartName ->
            {
                // Assign part name to proper part
                Staff staff = system.getClosestStaff(sentence.getCenter());
                Part part = staff.getPart();
                part.setName(sentence);
            }

            case ChordName ->
            {
                // Map each word with proper chord, in assigned staff
                for (Inter wInter : sentence.getMembers()) {
                    final ChordNameInter word = (ChordNameInter) wInter;
                    final Link link = word.lookupLink(system);

                    if (link == null) {
                        logger.info("No chord below {}", word);
                    } else {
                        link.applyTo(wInter);
                    }
                }
            }

            case EndingNumber, EndingText ->
            {
                // Look for related ending
                final Link link = sentence.lookupEndingLink(system);

                if ((link != null) && (null == sig.getRelation(
                        link.partner,
                        sentence,
                        EndingSentenceRelation.class))) {
                    sig.addEdge(link.partner, sentence, link.relation);
                }
            }
            }
            // Roles UnknownRole, Title, Number, Creator*, Rights stand by themselves
        } catch (Exception ex) {
            logger.warn("Error in linkOneSentence for {} {}", sentence, ex.toString(), ex);
        }
    }

    //------------//
    // linkPedals //
    //------------//
    /**
     * Link Pedal (start or stop) to proper chord if any.
     */
    private void linkPedals ()
    {
        for (Inter inter : sig.inters(PedalInter.class)) {
            final PedalInter pedal = (PedalInter) inter;

            try {
                if (pedal.isVip()) {
                    logger.info("VIP linkPedal for {}", pedal);
                }

                final Collection<Link> links = pedal.searchLinks(system);

                if (!links.isEmpty()) {
                    Link link = links.iterator().next();
                    link.applyTo(pedal);
                } else {
                    logger.info("No chord above {}", pedal);
                }
            } catch (Exception ex) {
                logger.warn("Error in linkPedals for {} {}", pedal, ex.toString(), ex);
            }
        }
    }

    //-----------//
    // linkTexts //
    //-----------//
    /**
     * Link text interpretations, according to their role, with their related entity if
     * any.
     */
    private void linkTexts ()
    {
        for (Inter sInter : sig.inters(SentenceInter.class)) {
            final SentenceInter sentence = (SentenceInter) sInter;
            linkOneSentence(sentence);
        }
    }

    //-------------//
    // linkTuplets //
    //-------------//
    /**
     * Link tuplet inters to their relevant chords.
     */
    private void linkTuplets ()
    {
        for (MeasureStack stack : system.getStacks()) {
            new TupletsBuilder(stack).linkStackTuplets();
        }
    }

    //------------//
    // linkWedges //
    //------------//
    /**
     * Link wedges (left and right sides) to proper chords if any.
     */
    private void linkWedges ()
    {
        for (Inter inter : sig.inters(WedgeInter.class)) {
            try {
                if (inter.isVip()) {
                    logger.info("VIP linkWedges for {}", inter);
                }

                final WedgeInter wedge = (WedgeInter) inter;
                final Collection<Link> links = wedge.searchLinks(system);

                for (Link link : links) {
                    link.applyTo(wedge);
                }
            } catch (Exception ex) {
                logger.warn("Error in linkWedges for {} {}", inter, ex.toString(), ex);
            }
        }
    }

    //---------//
    // process //
    //---------//
    /**
     * Process all links.
     */
    public void process ()
    {
        linkDynamics();
        linkTexts();
        linkPedals();
        linkWedges();
        linkFermatas();
        linkGraces();
        linkAugmentationDots();
        linkTuplets();
        linkOctaveShifts();
        linkNumbers();
    }

    //-------------------//
    // unlinkOneSentence //
    //-------------------//
    /**
     * Unlink a text sentence, according to its role, with its related entity if any.
     *
     * @param sentence the sentence to unlink
     * @param oldRole  the role this sentence had
     */
    public void unlinkOneSentence (SentenceInter sentence,
                                   TextRole oldRole)
    {
        try {
            if (sentence.isVip()) {
                logger.info("VIP unlinkOneSentence for {}", sentence);
            }

            if (oldRole == null) {
                logger.info("Null old role for {}", sentence);

                return;
            }

            switch (oldRole) {
            case Lyrics ->
            {
                for (Inter wInter : sentence.getMembers()) {
                    LyricItemInter item = (LyricItemInter) wInter;

                    for (Relation rel : sig.getRelations(item, ChordSyllableRelation.class)) {
                        sig.removeEdge(rel);
                    }
                }
            }

            case Direction ->
            {
                for (Relation rel : sig.getRelations(sentence, ChordSentenceRelation.class)) {
                    sig.removeEdge(rel);
                }
            }

            case PartName ->
            {
                // Look for proper part
                Staff staff = system.getClosestStaff(sentence.getCenter());
                final Part part = staff.getPart();
                part.setName((SentenceInter) null);
            }

            case ChordName ->
            {
                for (Inter wInter : sentence.getMembers()) {
                    for (Relation rel : sig.getRelations(wInter, ChordNameRelation.class)) {
                        sig.removeEdge(rel);
                    }
                }
            }

            case EndingNumber, EndingText ->
            {
                for (Relation rel : sig.getRelations(sentence, EndingSentenceRelation.class)) {
                    sig.removeEdge(rel);
                }
            }
            }
        } catch (Exception ex) {
            logger.warn("Error in unlinkOneSentence for {} {}", sentence, ex.toString(), ex);
        }
    }
}
