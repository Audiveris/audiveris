//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    O r n a m e n t I n t e r                                   //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordOrnamentRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code OrnamentInter} represents an ornament interpretation.
 * (TR, TURN, TURN_INVERTED, TURN_UP, TURN_SLASH, MORDENT, MORDENT_INVERTED, GRACE_NOTE_SLASH,
 * GRACE_NOTE)
 *
 * @author Hervé Bitteur
 */
public class OrnamentInter
        extends AbstractNotationInter
{    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OrnamentInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new OrnamentInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (TR, TURN, TURN_INVERTED, TURN_UP, TURN_SLASH, MORDENT,
     *              MORDENT_INVERTED, GRACE_NOTE_SLASH, GRACE_NOTE)
     * @param grade evaluation value
     */
    public OrnamentInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private OrnamentInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordOrnamentRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, ChordOrnamentRelation.class)) {
                HeadChordInter chord = (HeadChordInter) sig.getOppositeInter(this, rel);

                return staff = chord.getStaff();
            }
        }

        return staff;
    }
//
//    //--------//
//    // create //
//    //--------//
//    /**
//     * (Try to) create an OrnamentInter.
//     *
//     * @param glyph            underlying glyph
//     * @param shape            detected shape
//     * @param grade            assigned grade
//     * @param system           containing system
//     * @param systemHeadChords system head chords, ordered by abscissa
//     * @return the created articulation or null
//     */
//    public static OrnamentInter create (Glyph glyph,
//                                        Shape shape,
//                                        double grade,
//                                        SystemInfo system,
//                                        List<Inter> systemHeadChords)
//    {
//        Scale scale = system.getSheet().getScale();
//        SIGraph sig = system.getSig();
//        final int maxDx = scale.toPixels(ChordOrnamentRelation.getXOutGapMaximum());
//        final int maxDy = scale.toPixels(ChordOrnamentRelation.getYGapMaximum());
//        final Rectangle glyphBox = glyph.getBounds();
//        final Point glyphCenter = glyph.getCenter();
//        final Rectangle luBox = new Rectangle(glyphCenter);
//        luBox.grow(maxDx, maxDy);
//
//        final List<Inter> chords = SIGraph.intersectedInters(
//                systemHeadChords,
//                GeoOrder.BY_ABSCISSA,
//                luBox);
//
//        if (chords.isEmpty()) {
//            return null;
//        }
//
//        ChordOrnamentRelation bestRel = null;
//        Inter bestChord = null;
//        double bestYGap = Double.MAX_VALUE;
//
//        for (Inter chord : chords) {
//            Rectangle chordBox = chord.getBounds();
//
//            // The ornament cannot intersect the chord
//            if (chordBox.intersects(glyphBox)) {
//                continue;
//            }
//
//            Point center = chord.getCenter();
//
//            // Select proper chord reference point (top or bottom)
//            int yRef = (glyphCenter.y > center.y) ? (chordBox.y + chordBox.height)
//                    : chordBox.y;
//            double xGap = Math.abs(center.x - glyphCenter.x);
//            double yGap = Math.abs(yRef - glyphCenter.y);
//            ChordOrnamentRelation rel = new ChordOrnamentRelation();
//            rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));
//
//            if (rel.getGrade() >= rel.getMinGrade()) {
//                if ((bestRel == null) || (bestYGap > yGap)) {
//                    bestRel = rel;
//                    bestChord = chord;
//                    bestYGap = yGap;
//                }
//            }
//        }
//
//        if (bestRel != null) {
//            OrnamentInter ornament = new OrnamentInter(glyph, shape, grade);
//            sig.addVertex(ornament);
//            sig.addEdge(bestChord, ornament, bestRel);
//            logger.debug("Created {}", ornament);
//
//            return ornament;
//        }
//
//        return null;
//    }
//
    //-----------//
    // internals //
    //-----------//

    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }
}
