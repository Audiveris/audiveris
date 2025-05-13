//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A r t i c u l a t i o n I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>ArticulationInter</code> represents an articulation sign.
 * <p>
 * Supported shapes are ACCENT, MARCATO, MARCATO_BELOW, STACCATO, TENUTO, STACCATISSIMO
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
     * No-argument constructor meant for JAXB.
     */
    private ArticulationInter ()
    {
    }

    /**
     * Creates a new <code>ArticulationInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (ACCENT, MARCATO, MARCATO_BELOW, STACCATO, TENUTO, STACCATISSIMO)
     * @param grade the interpretation quality
     */
    public ArticulationInter (Glyph glyph,
                              Shape shape,
                              Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

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

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, ChordArticulationRelation.class)) {
                return staff = sig.getOppositeInter(this, rel).getStaff();
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

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this articulation instance and a HeadChord nearby.
     * <p>
     * Vertically, the lookup should stop at the top line of the staff above
     * and at the bottom line of the staff below.
     * <p>
     * Plus, for articulation exclusively above or below, such as MARCATO and MARCATO_BELOW,
     * the ordinate search should focus only on below or above the sign respectively.
     *
     * @param systemHeadChords collection of head chords in system, ordered by abscissa
     * @param profile          desired profile level
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemHeadChords,
                             int profile)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(ChordArticulationRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(ChordArticulationRelation.getYGapMaximum(profile));
        final int minDy = scale.toPixels(ChordArticulationRelation.getYGapMinimum(profile));

        final Rectangle articBox = getBounds();
        final Point pt = getCenter();
        final List<Staff> stavesAround = system.getStavesAround(pt);

        // Adjustment for MARCATO (above): look for chord only *below* the sign
        // Adjustment for MARCATO_BELOW: look for chord only *above* the sign
        final int minY;
        final int maxY;

        if (stavesAround.size() == 2) {
            // The sign is located between 2 staves of the system
            minY = shape.isAbove() ? pt.y : stavesAround.get(0).getLine(TOP).yAt(pt.x);
            maxY = shape.isBelow() ? pt.y : stavesAround.get(1).getLine(BOTTOM).yAt(pt.x);
        } else {
            // The sign is related to a single staff
            final Staff theStaff = stavesAround.get(0);

            if (theStaff.isPointAbove(pt)) {
                // The sign is located above staff
                if (shape.isBelow()) {
                    return null;
                }
                minY = shape.isAbove() ? pt.y : pt.y - maxDy;
                maxY = theStaff.getLine(BOTTOM).yAt(pt.x);
            } else if (theStaff.isPointBelow(pt)) {
                // The sign is located below staff
                if (shape.isAbove()) {
                    return null;
                }
                minY = theStaff.getLine(TOP).yAt(pt.x);
                maxY = shape.isBelow() ? pt.y : pt.y + maxDy;
            } else {
                // The sign is located within staff height
                minY = shape.isAbove() ? pt.y : theStaff.getLine(TOP).yAt(pt.x);
                maxY = shape.isBelow() ? pt.y : theStaff.getLine(BOTTOM).yAt(pt.x);
            }
        }

        final Rectangle luBox = new Rectangle(pt.x - maxDx, minY, 2 * maxDx + 1, maxY - minY + 1);
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
            int yRef = (pt.y > center.y) ? (chordBox.y + chordBox.height) : chordBox.y;
            double absXGap = Math.abs(center.x - pt.x);
            double yGap = (pt.y > center.y) ? (pt.y - yRef) : (yRef - pt.y);

            if (yGap < minDy) {
                continue;
            }

            double absYGap = Math.abs(yGap);
            ChordArticulationRelation rel = new ChordArticulationRelation();
            rel.setOutGaps(scale.pixelsToFrac(absXGap), scale.pixelsToFrac(absYGap), profile);

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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupLink(systemHeadChords, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordArticulationRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

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
                                                      Double grade,
                                                      SystemInfo system,
                                                      List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP ArticulationInter create {} as {}", glyph, shape);
        }

        final ArticulationInter articulation = new ArticulationInter(glyph, shape, grade);
        final Link link = articulation.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(articulation);
            link.applyTo(articulation);

            return articulation;
        }

        return null;
    }
}
