//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s L i n k e r                                   //
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
package omr.sheet.symbol;

import omr.glyph.Shape;

import omr.sheet.SystemInfo;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Voice;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.DynamicsInter;
import omr.sig.inter.FermataInter;
import omr.sig.inter.Inter;
import omr.sig.inter.LyricItemInter;
import omr.sig.inter.PedalInter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.WedgeInter;
import omr.sig.inter.WordInter;
import omr.sig.relation.ChordNameRelation;
import omr.sig.relation.ChordPedalRelation;
import omr.sig.relation.ChordSentenceRelation;
import omr.sig.relation.ChordWedgeRelation;
import omr.sig.relation.FermataBarRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.text.TextRole;

import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
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
            dynamics.linkWithChord();
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
        List<Inter> fermatas = sig.inters(FermataInter.class);

        for (Inter inter : fermatas) {
            FermataInter fermata = (FermataInter) inter;

            if (fermata.isVip()) {
                logger.info("VIP linkFermatas on {}", fermata);
            }

            // Look for a chord (head or rest) related to this fermata
            final Point center = fermata.getCenter();
            final MeasureStack stack = system.getMeasureStackAt(center);
            final Collection<AbstractChordInter> chords = (fermata.getShape() == Shape.FERMATA_BELOW)
                    ? stack.getStandardChordsAbove(center)
                    : stack.getStandardChordsBelow(center);

            if (!fermata.linkWithChords(chords)) {
                // Check whether this fermata has a link to a barline
                if (!sig.hasRelation(fermata, FermataBarRelation.class)) {
                    fermata.delete(); // No link at all
                }
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

            for (Inter interNote : smallChord.getNotes()) {
                for (Relation rel : sig.getRelations(interNote, SlurHeadRelation.class)) {
                    SlurInter slur = (SlurInter) sig.getOppositeInter(interNote, rel);
                    AbstractHeadInter head = slur.getHead(HorizontalSide.RIGHT);

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

            if (pedal.isVip()) {
                logger.info("VIP linkPedal for {}", pedal);
            }

            final Point location = pedal.getCenter();
            final MeasureStack stack = system.getMeasureStackAt(location);
            final AbstractChordInter chordAbove = stack.getStandardChordAbove(location);

            if (chordAbove != null) {
                sig.addEdge(chordAbove, pedal, new ChordPedalRelation());
            } else {
                logger.info("No chord above {}", pedal);
            }
        }
    }

    //------------//
    // linkSegnos //
    //------------//
    private void linkSegnos ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
            SentenceInter sentence = (SentenceInter) sInter;
            final TextRole role = sentence.getRole();

            if (role == null) {
                logger.info("No role for {}", sentence);

                continue;
            }

            final Point location = sentence.getLocation();

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
                AbstractChordInter chord = stack.getEventChord(location);

                if (chord != null) {
                    sig.addEdge(chord, sentence, new ChordSentenceRelation());
                } else {
                    logger.info("No chord above direction {}", sentence);
                }
            }

            break;

            case ChordName: {
                // Map chordName with proper chord
                MeasureStack stack = system.getMeasureStackAt(location);
                AbstractChordInter chordBelow = stack.getStandardChordBelow(location);

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
            final WedgeInter wedge = (WedgeInter) inter;
            Line2D topLine = wedge.getLine1();

            for (HorizontalSide side : HorizontalSide.values()) {
                final Point2D location = (side == LEFT) ? topLine.getP1() : topLine.getP2();
                final MeasureStack stack = system.getMeasureStackAt(location);

                final AbstractChordInter chordAbove = stack.getStandardChordAbove(location);

                if (chordAbove != null) {
                    sig.addEdge(chordAbove, wedge, new ChordWedgeRelation(side));
                } else {
                    final AbstractChordInter chordBelow = stack.getStandardChordBelow(location);

                    if (chordBelow != null) {
                        sig.addEdge(chordBelow, wedge, new ChordWedgeRelation(side));
                    } else {
                        logger.info("No chord for {} {}", wedge, side);
                    }
                }
            }
        }
    }
}
