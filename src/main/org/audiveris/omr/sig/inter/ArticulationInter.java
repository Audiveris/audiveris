//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A r t i c u l a t i o n I n t e r                               //
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
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
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
 * Class {@code ArticulationInter} represents an articulation sign
 * (TENUTO, ACCENT, STACCATO, STACCATISSIMO, MARCATO).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "articulation")
public class ArticulationInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ArticulationInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ArticulationInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (TENUTO, ACCENT, STACCATO, STACCATISSIMO, MARCATO)
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
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No chord linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a chord is connected
        setAbnormal(!sig.hasRelation(this, ChordArticulationRelation.class));

        return isAbnormal();
    }

    //------------------//
    // createValidAdded //
    //------------------//
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
    public static ArticulationInter createValidAdded (Glyph glyph,
                                                      Shape shape,
                                                      double grade,
                                                      SystemInfo system,
                                                      List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP ArticulationInter create {} as {}", glyph, shape);
        }

        ArticulationInter artic = new ArticulationInter(glyph, shape, grade);
        Link link = artic.lookupLink(systemHeadChords);

        if (link != null) {
            system.getSig().addVertex(artic);
            link.applyTo(artic);

            return artic;
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
     * Try to detect a link between this articulation instance and a HeadChord nearby.
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
        final int maxDx = scale.toPixels(
                ChordArticulationRelation.getXOutGapMaximum(manual));
        final int maxDy = scale.toPixels(ChordArticulationRelation.getYGapMaximum(manual));
        final int minDy = scale.toPixels(ChordArticulationRelation.getYGapMinimum(manual));
        final Rectangle articBox = getBounds();
        final Point arcticCenter = getCenter();
        final Rectangle luBox = new Rectangle(arcticCenter);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = Inters.intersectedInters(
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
            if (chordBox.intersects(articBox)) {
                continue;
            }

            Point center = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            int yRef = (arcticCenter.y > center.y) ? (chordBox.y + chordBox.height) : chordBox.y;
            double absXGap = Math.abs(center.x - arcticCenter.x);
            double yGap = (arcticCenter.y > center.y) ? (arcticCenter.y - yRef)
                    : (yRef - arcticCenter.y);

            if (yGap < minDy) {
                continue;
            }

            double absYGap = Math.abs(yGap);
            ChordArticulationRelation rel = new ChordArticulationRelation();
            rel.setOutGaps(scale.pixelsToFrac(absXGap), scale.pixelsToFrac(absYGap), manual);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > absYGap)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestYGap = absYGap;
                }
            }
        }

        if (bestRel != null) {
            return new Link(bestChord, bestRel, false);
        }

        return null;
    }
}
