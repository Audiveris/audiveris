//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s L i n k e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.sheet.SystemInfo;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Voice;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.ChordInter;
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

/**
 * Class {@code SymbolsLinker} defines final relations between certain symbols.
 * <p>
 * This process can take place only when chords candidates have survived to all reductions.
 *
 * @author Hervé Bitteur
 */
class SymbolsLinker
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
        linkTexts();
        linkPedals();
        linkWedges();
        linkGraces();
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
                            logger.info("{} assigned {}", smallChord, voice);

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
            final Point location = pedal.getCenter();
            final MeasureStack stack = system.getMeasureStackAt(location);
            final ChordInter chordAbove = stack.getChordAbove(location);

            if (chordAbove != null) {
                sig.addEdge(chordAbove, pedal, new ChordPedalRelation());
            } else {
                logger.info("No chord above {}", pedal);
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
            SentenceInter sentence = (SentenceInter) sInter;
            final TextRole role = sentence.getRole();

            if (role == null) {
                logger.info("No role for {}", sentence);

                continue;
            }

            final Point location = sentence.getLocation();

            switch (role) {
            case Lyrics: {
                // Map each syllable with proper chord
                for (Inter wInter : sentence.getMembers()) {
                    LyricItemInter item = (LyricItemInter) wInter;
                    item.mapToChord();
                }
            }

            break;

            case Direction: {
                // Map direction with proper chord
                MeasureStack stack = system.getMeasureStackAt(location);
                ChordInter chord = stack.getEventChord(location);

                if (chord != null) {
                    sig.addEdge(chord, sentence, new ChordSentenceRelation());
                } else {
                    logger.info("No chord above {}", sentence);
                }
            }

            break;

            case ChordName: {
                // Map chordName with proper chord
                MeasureStack stack = system.getMeasureStackAt(location);
                ChordInter chordBelow = stack.getChordBelow(location);

                if (chordBelow != null) {
                    WordInter word = sentence.getFirstWord(); // The single word in fact
                    sig.addEdge(chordBelow, word, new ChordNameRelation());
                } else {
                    logger.info("No chord above {}", sentence);
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
     * Link Wedges (left and right sides) to proper chords if any.
     */
    private void linkWedges ()
    {
        for (Inter inter : sig.inters(WedgeInter.class)) {
            final WedgeInter wedge = (WedgeInter) inter;
            Line2D topLine = wedge.getLine1();

            for (HorizontalSide side : HorizontalSide.values()) {
                final Point2D location = (side == LEFT) ? topLine.getP1() : topLine.getP2();
                final MeasureStack stack = system.getMeasureStackAt(location);

                final ChordInter chordAbove = stack.getChordAbove(location);

                if (chordAbove != null) {
                    sig.addEdge(chordAbove, wedge, new ChordWedgeRelation(side));
                } else {
                    final ChordInter chordBelow = stack.getChordBelow(location);

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
