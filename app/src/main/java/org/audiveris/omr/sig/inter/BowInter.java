//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B o w I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.relation.ChordBowRelation;
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
 * Class <code>BowInter</code>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bow")
public class BowInter
        extends AbstractDirectionInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BowInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private BowInter ()
    {
        super(null, null, null, 0.0);
    }

    /**
     * Creates a new <code>BowInter</code> object.
     *
     * @param glyph the pedal glyph
     * @param shape either BOW_UP or BOW_DOWN
     * @param grade the interpretation quality
     */
    public BowInter (Glyph glyph,
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
        setAbnormal(!sig.hasRelation(this, ChordBowRelation.class));

        return isAbnormal();
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the related chord, if any.
     *
     * @return the related chord, or null
     */
    public AbstractChordInter getChord ()
    {
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, ChordBowRelation.class)) {
                AbstractChordInter chord = (AbstractChordInter) sig.getOppositeInter(this, rel);

                return chord;
            }
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
            if (sig != null) {
                staff = sig.getSystem().getStaffAtOrBelow(getCenter());
            }
        }

        return staff;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this bow instance and a HeadChord nearby.
     *
     * @param systemHeadChords abscissa-ordered collection of head chords in system
     * @param profile          desired profile level
     * @return the link found or null
     */
    protected Link lookupLink (List<Inter> systemHeadChords,
                               int profile)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(ChordBowRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(ChordBowRelation.getYGapMaximum(profile));
        final Rectangle bowBox = getBounds();
        final Point bowCenter = getCenter();
        final Rectangle luBox = new Rectangle(bowCenter);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        ChordBowRelation bestRel = null;
        Inter bestChord = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter chord : chords) {
            Rectangle chordBox = chord.getBounds();

            // The bow cannot intersect the chord
            if (chordBox.intersects(bowBox)) {
                continue;
            }

            Point center = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            int yRef = (bowCenter.y > center.y) ? (chordBox.y + chordBox.height) : chordBox.y;
            double xGap = Math.abs(center.x - bowCenter.x);
            double yGap = Math.abs(yRef - bowCenter.y);
            ChordBowRelation rel = new ChordBowRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        Link link = null;

        try {
            final Point location = getCenter();
            final MeasureStack stack = system.getStackAt(location);

            if (stack != null) {
                final Rectangle box = getBounds();
                final AbstractChordInter chordBelow = stack.getStandardChordBelow(location, box);

                if (chordBelow != null) {
                    link = new Link(chordBelow, new ChordBowRelation(), false);
                }
            }
        } catch (Exception ignored) {}

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordBowRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid BowInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created bow or null
     */
    public static BowInter createValidAdded (Glyph glyph,
                                             Shape shape,
                                             double grade,
                                             SystemInfo system,
                                             List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP BowInter create {} as {}", glyph, shape);
        }

        final BowInter bow = new BowInter(glyph, shape, grade);
        final Link link = bow.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(bow);
            link.applyTo(bow);

            return bow;
        }

        return null;
    }

}
