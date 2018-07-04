//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    O r n a m e n t I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.relation.ChordOrnamentRelation;
import org.audiveris.omr.sig.relation.Link;
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
 * Class {@code OrnamentInter} represents an ornament interpretation.
 * (TR, TURN, TURN_INVERTED, TURN_UP, TURN_SLASH, MORDENT, MORDENT_INVERTED, GRACE_NOTE_SLASH,
 * GRACE_NOTE)
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ornament")
public class OrnamentInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

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
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid OrnamentInter.
     * <p>
     * TODO: this is to be refined for GRACE ornaments which are located on left side of chord.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created articulation or null
     */
    public static OrnamentInter createValidAdded (Glyph glyph,
                                                  Shape shape,
                                                  double grade,
                                                  SystemInfo system,
                                                  List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP OrnamentInter create {} as {}", glyph, shape);
        }

        OrnamentInter orn = new OrnamentInter(glyph, shape, grade);
        Link link = orn.lookupLink(systemHeadChords);

        if (link != null) {
            system.getSig().addVertex(orn);
            link.applyTo(orn);

            return orn;
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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupLink(systemHeadChords);

        if (link == null) {
            return Collections.emptyList();
        }

        if (doit) {
            link.applyTo(this);
        }

        return Collections.singleton(link);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this ornament instance and a HeadChord nearby.
     *
     * @param systemHeadChords ordered collection of head chords in system
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemHeadChords)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(ChordOrnamentRelation.getXOutGapMaximum(manual));
        final int maxDy = scale.toPixels(ChordOrnamentRelation.getYGapMaximum(manual));
        final Rectangle ornamentBox = getBounds();
        final Point ornamentCenter = getCenter();
        final Rectangle luBox = new Rectangle(ornamentCenter);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        ChordOrnamentRelation bestRel = null;
        Inter bestChord = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter chord : chords) {
            Rectangle chordBox = chord.getBounds();

            // The ornament cannot intersect the chord
            if (chordBox.intersects(ornamentBox)) {
                continue;
            }

            Point center = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            int yRef = (ornamentCenter.y > center.y)
                    ? (chordBox.y + chordBox.height) : chordBox.y;
            double xGap = Math.abs(center.x - ornamentCenter.x);
            double yGap = Math.abs(yRef - ornamentCenter.y);
            ChordOrnamentRelation rel = new ChordOrnamentRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > yGap)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestYGap = yGap;
                }
            }
        }

        if (bestRel != null) {
            return new Link(bestChord, bestRel, false);
        }

        return null;
    }
}
