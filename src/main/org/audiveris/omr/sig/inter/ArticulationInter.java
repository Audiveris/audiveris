//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A r t i c u l a t i o n I n t e r                               //
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
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.symbol.SymbolFactory;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ArticulationInter} represents an articulation sign
 * (tenuto, accent, staccato, staccatissimo, marcato).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "articulation")
public class ArticulationInter
        extends AbstractNotationInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ArticulationInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ArticulationInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (tenuto, accent, staccato, staccatissimo, marcato)
     * @param grade evaluation value
     */
    public ArticulationInter (Glyph glyph,
                              Shape shape,
                              double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ArticulationInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create an ArticulationInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created articulation or null
     */
    public static ArticulationInter create (Glyph glyph,
                                            Shape shape,
                                            double grade,
                                            SystemInfo system,
                                            List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP ArticulationInter create {} as {}", glyph, shape);
        }

        ArticulationInter artic = (ArticulationInter) SymbolFactory.createGhost(shape, grade);
        artic.setGlyph(glyph);

        Partnership partnership = artic.lookupPartnership(systemHeadChords);

        if (partnership != null) {
            system.getSig().addVertex(artic);
            partnership.applyTo(artic);

            return artic;
        }

        return null;

        //
        //
        //        Scale scale = system.getSheet().getScale();
        //        SIGraph sig = system.getSig();
        //        final int maxDx = scale.toPixels(ChordArticulationRelation.getXOutGapMaximum());
        //        final int maxDy = scale.toPixels(ChordArticulationRelation.getYGapMaximum());
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
        //        ChordArticulationRelation bestRel = null;
        //        Inter bestChord = null;
        //        double bestYGap = Double.MAX_VALUE;
        //
        //        for (Inter chord : chords) {
        //            Rectangle chordBox = chord.getBounds();
        //
        //            // The articulation cannot intersect the chord
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
        //            ChordArticulationRelation rel = new ChordArticulationRelation();
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
        //            ArticulationInter articulation = new ArticulationInter(glyph, shape, grade);
        //            sig.addVertex(articulation);
        //            sig.addEdge(bestChord, articulation, bestRel);
        //            logger.debug("Created {}", articulation);
        //
        //            return articulation;
        //        }
        //
        //        return null;
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, ChordArticulationRelation.class)) {
                HeadChordInter chord = (HeadChordInter) sig.getOppositeInter(this, rel);

                return staff = chord.getStaff();
            }
        }

        return staff;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordArticulationRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //--------------------//
    // searchPartnerships //
    //--------------------//
    @Override
    public Collection<Partnership> searchPartnerships (SystemInfo system,
                                                       boolean doit)
    {
        // Not very optimized!
        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inter.byAbscissa);

        Partnership partnership = lookupPartnership(systemHeadChords);

        if (doit && (partnership != null)) {
            partnership.applyTo(this);
        }

        return Collections.singleton(partnership);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //-------------------//
    // lookupPartnership //
    //-------------------//
    /**
     * Try to detect a partnership between this articulation instance and a HeadChord
     * nearby.
     *
     * @param systemHeadChords ordered collection of head chords in system
     * @return the partnership found or null
     */
    private Partnership lookupPartnership (List<Inter> systemHeadChords)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(ChordArticulationRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(ChordArticulationRelation.getYGapMaximum());
        final Rectangle glyphBox = glyph.getBounds();
        final Point glyphCenter = glyph.getCenter();
        final Rectangle luBox = new Rectangle(glyphCenter);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = SIGraph.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        ChordArticulationRelation bestRel = null;
        Inter bestChord = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter chord : chords) {
            Rectangle chordBox = chord.getBounds();

            // The articulation cannot intersect the chord
            if (chordBox.intersects(glyphBox)) {
                continue;
            }

            Point center = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            int yRef = (glyphCenter.y > center.y)
                    ? (chordBox.y + chordBox.height) : chordBox.y;
            double xGap = Math.abs(center.x - glyphCenter.x);
            double yGap = Math.abs(yRef - glyphCenter.y);
            ChordArticulationRelation rel = new ChordArticulationRelation();
            rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > yGap)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestYGap = yGap;
                }
            }
        }

        if (bestRel != null) {
            return new Partnership(bestChord, bestRel, false);
        }

        return null;
    }
}
