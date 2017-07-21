//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s L i n k e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordPedalRelation;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.DotFermataRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SymbolsLinker} defines final relations between certain symbols.
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
     * Creates a new {@code SymbolsLinker} object.
     *
     * @param system the dedicated system
     */
    public SymbolsLinker (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
    {
        linkDynamics();
        linkTexts();
        linkPedals();
        linkWedges();
        linkFermatas();
        linkGraces();
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
                    arc.delete();

                    continue;
                }

                FermataInter fermata = FermataInter.create(arc, dot, system);
                sig.addVertex(fermata);

                if (fermata.isVip()) {
                    logger.info("VIP linkFermatas on {}", fermata);
                }

                // Look for a related barline
                if (!fermata.linkWithBarline()) {
                    // Look for a chord (head or rest) related to this fermata
                    final Point center = fermata.getCenter();
                    final Rectangle bounds = arc.getBounds();
                    final MeasureStack stack = system.getMeasureStackAt(center);
                    final Collection<AbstractChordInter> chords = (fermata.getShape() == Shape.FERMATA_BELOW)
                            ? stack.getStandardChordsAbove(
                                    center,
                                    bounds) : stack.getStandardChordsBelow(center, bounds);

                    if (!fermata.linkWithChords(chords)) {
                        // No link to barline, no link to chord, discard it
                        fermata.delete();
                        arc.delete();
                        dot.delete();
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
     * Link grace chords with their standard chord.
     */
    private void linkGraces ()
    {
        SmallLoop:
        for (Inter chordInter : sig.inters(SmallChordInter.class)) {
            final SmallChordInter smallChord = (SmallChordInter) chordInter;

            if (smallChord.isVip()) {
                logger.info("VIP linkGracesl for {}", smallChord);
            }

            try {
                for (Inter interNote : smallChord.getNotes()) {
                    for (Relation rel : sig.getRelations(interNote, SlurHeadRelation.class)) {
                        SlurInter slur = (SlurInter) sig.getOppositeInter(interNote, rel);
                        HeadInter head = slur.getHead(HorizontalSide.RIGHT);

                        if (head != null) {
                            Voice voice = head.getVoice();

                            if (voice != null) {
                                smallChord.setVoice(voice);
                                logger.debug("{} assigned {}", smallChord, voice);

                                continue SmallLoop;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error in linkGraces for {} {}", smallChord, ex.toString(), ex);
            }
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

                final Point location = pedal.getCenter();
                final Rectangle bounds = pedal.getBounds();
                final MeasureStack stack = system.getMeasureStackAt(location);
                final AbstractChordInter chordAbove = stack.getStandardChordAbove(location, bounds);

                if (chordAbove != null) {
                    sig.addEdge(chordAbove, pedal, new ChordPedalRelation());
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
        final Scale scale = system.getSheet().getScale();

        for (Inter sInter : sig.inters(SentenceInter.class)) {
            final SentenceInter sentence = (SentenceInter) sInter;

            try {
                if (sentence.isVip()) {
                    logger.info("VIP linkTexts for {}", sentence);
                }

                final TextRole role = sentence.getRole();

                if (role == null) {
                    logger.info("No role for {}", sentence);

                    continue;
                }

                final Point location = sentence.getLocation();
                final Rectangle bounds = sentence.getBounds();

                switch (role) {
                case Lyrics: {
                    // Map each syllable with proper chord, in staff just above
                    for (Inter wInter : sentence.getMembers()) {
                        LyricItemInter item = (LyricItemInter) wInter;
                        item.mapToChord();
                    }
                }

                break;

                case Direction: {
                    // Map direction with proper chord
                    MeasureStack stack = system.getMeasureStackAt(location);

                    if (stack == null) {
                        logger.info(
                                "No measure stack for direction {} {}",
                                sentence,
                                sentence.getValue());

                        break;
                    }

                    int xGapMax = scale.toPixels(ChordSentenceRelation.getXGapMax());
                    Rectangle fatBounds = new Rectangle(bounds);
                    fatBounds.grow(xGapMax, 0);

                    AbstractChordInter chord = stack.getEventChord(location, fatBounds);

                    if (chord != null) {
                        sig.addEdge(chord, sentence, new ChordSentenceRelation());
                    } else {
                        logger.info(
                                "No chord above direction {} {}",
                                sentence,
                                sentence.getValue());
                    }
                }

                break;

                case ChordName: {
                    // Map chordName with proper chord
                    MeasureStack stack = system.getMeasureStackAt(location);
                    AbstractChordInter chordBelow = stack.getStandardChordBelow(location, bounds);

                    if (chordBelow != null) {
                        WordInter word = sentence.getFirstWord(); // The single word in fact
                        sig.addEdge(chordBelow, word, new ChordNameRelation());
                    } else {
                        logger.info("No chord above chordName {}", sentence);
                    }
                }

                break;

                default:

                // Roles other than [Lyrics, Direction, ChordName] don't use relations
                }
            } catch (Exception ex) {
                logger.warn("Error in linkTexts for {} {}", sentence, ex.toString(), ex);
            }
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
                final Line2D topLine = wedge.getLine1();

                for (HorizontalSide side : HorizontalSide.values()) {
                    final Point2D location = (side == LEFT) ? topLine.getP1()
                            : topLine.getP2();
                    final MeasureStack stack = system.getMeasureStackAt(location);
                    final AbstractChordInter chordAbove = stack.getStandardChordAbove(
                            location,
                            null);

                    if (chordAbove != null) {
                        sig.addEdge(chordAbove, wedge, new ChordWedgeRelation(side));
                    } else {
                        AbstractChordInter chordBelow = stack.getStandardChordBelow(location, null);

                        if (chordBelow != null) {
                            sig.addEdge(chordBelow, wedge, new ChordWedgeRelation(side));
                        } else {
                            logger.info("No chord for {} {}", wedge, side);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error in linkWedges for {} {}", inter, ex.toString(), ex);
            }
        }
    }
}
