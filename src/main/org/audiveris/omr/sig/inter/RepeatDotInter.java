//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e p e a t D o t I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.sig.relation.RepeatDotPairRelation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.math.PointUtil;

/**
 * Class {@code RepeatDotInter} represents a repeat dot, near a bar line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "repeat-dot")
public class RepeatDotInter
        extends AbstractPitchedInter
{

    /**
     * Creates a new RepeatDotInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch dot pitch
     */
    public RepeatDotInter (Glyph glyph,
                           Double grade,
                           Staff staff,
                           Double pitch)
    {
        super(glyph, null, Shape.REPEAT_DOT, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private RepeatDotInter ()
    {
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // checkPitch //
    //------------//
    /**
     * Verify that the dot pitch corresponds to a valid value (close to +1 or -1).
     *
     * @return true if OK
     */
    public boolean checkPitch ()
    {
        if (pitch == null) {
            return false;
        }

        final double pitchDif = Math.abs(Math.abs(pitch) - 1);
        final double maxDif = RepeatDotBarRelation.getYGapMaximum(getProfile()).getValue();

        return pitchDif <= (2 * maxDif);
    }

    //------------//
    // checkPitch //
    //------------//
    /**
     * Verify that the provided dot center corresponds to a valid pitch value
     * (close to +1 or -1).
     *
     *
     * @param system  containing system
     * @param center  dot center
     * @param profile desired profile level
     * @return true if OK
     */
    public static boolean checkPitch (SystemInfo system,
                                      Point2D center,
                                      int profile)
    {
        final double pp = system.estimatedPitch(center);
        final double pitchDif = Math.abs(Math.abs(pp) - 1);
        final double maxDif = RepeatDotBarRelation.getYGapMaximum(profile).getValue();

        return pitchDif <= (2 * maxDif);
    }

    //-------------//
    // getDotLuBox //
    //-------------//
    /**
     * Report the lookup box to retrieve the sibling dot of this repeat dot.
     *
     * @param system  containing system
     * @param profile desired profile level (currently ineffective)
     * @return the lookup dot box
     */
    public Rectangle getDotLuBox (SystemInfo system,
                                  int profile)
    {
        final Scale scale = system.getSheet().getScale();
        final int dotPitch = getIntegerPitch();
        final Rectangle dotLuBox = getBounds();

        // TODO: Perhaps we could grow dotLuBox based on profile level?
        //
        dotLuBox.y -= (scale.getInterline() * dotPitch);

        return dotLuBox;
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check pitch
        // Check bar relation
        // Check sibling dot relation
        setAbnormal(!checkPitch()
                            || !sig.hasRelation(this, RepeatDotBarRelation.class)
                            || !sig.hasRelation(this, RepeatDotPairRelation.class));

        return isAbnormal();
    }

    //---------------//
    // lookupBarLink //
    //---------------//
    /**
     * Look for the related barline.
     *
     * @param system     containing system
     * @param systemBars barlines in the system
     * @param profile    desired profile level
     * @return link to the related barline or null
     */
    public Link lookupBarLink (SystemInfo system,
                               List<Inter> systemBars,
                               int profile)
    {
        // Check vertical pitch position within the staff: close to +1 or -1
        final Rectangle dotBounds = getBounds();
        final Point2D dotPt = GeoUtil.center2D(dotBounds);

        staff = system.getClosestStaff(dotPt); // Staff is OK

        // Look for barline
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(RepeatDotBarRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(RepeatDotBarRelation.getYGapMaximum(profile));
        final Rectangle barLuBox = new Rectangle(PointUtil.rounded(dotPt));
        barLuBox.grow(maxDx, maxDy);

        final List<Inter> bars = Inters
                .intersectedInters(systemBars, GeoOrder.BY_ABSCISSA, barLuBox);

        if (bars.isEmpty()) {
            return null;
        }

        RepeatDotBarRelation bestRel = null;
        Inter bestBar = null;
        double bestXGap = Double.MAX_VALUE;

        for (Inter barInter : bars) {
            BarlineInter bar = (BarlineInter) barInter;
            Rectangle box = bar.getBounds();
            Point2D barCenter = bar.getCenter2D();

            // Select proper bar reference point (left or right side and proper vertical side)
            int yDir = (dotPt.getY() > barCenter.getY()) ? 1 : -1;
            double barY = barCenter.getY() + ((box.height / 8.0) * yDir);
            double yGap = Math.abs(barY - dotPt.getY());

            int xDir = (dotPt.getX() > barCenter.getX()) ? 1 : -1;
            double barX = LineUtil.xAtY(bar.getMedian(), barY) + ((bar.getWidth() / 2.0) * xDir);
            double xGap = Math.abs(barX - dotPt.getX());

            RepeatDotBarRelation rel = new RepeatDotBarRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestXGap > xGap)) {
                    bestRel = rel;
                    bestBar = bar;
                    bestXGap = xGap;
                }
            }
        }

        if (bestRel != null) {
            return new Link(bestBar, bestRel, true);
        }
        return null;
    }

    //---------------//
    // lookupDotLink //
    //---------------//
    /**
     * Look for a sibling repeat dot.
     *
     * @param system  containing system
     * @param profile desired profile level
     * @return link to the sibling repeat dot or null
     */
    public Link lookupDotLink (SystemInfo system,
                               int profile)
    {
        final Rectangle dotLuBox = getDotLuBox(system, profile);
        final List<Inter> dots = system.getSig().inters((Inter inter) -> (inter
                                                                                  != RepeatDotInter.this)
                                                                         && !inter
                        .isRemoved()
                                                                                 && (inter
                        .getShape() == Shape.REPEAT_DOT)
                                                                                 && (inter
                        .getBounds().intersects(dotLuBox)));

        if (!dots.isEmpty()) {
            return new Link(dots.get(0), new RepeatDotPairRelation(), true);
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        final List<Link> links = new ArrayList<>();

        // Sanity check on pitch
        if (!checkPitch(system, getCenter(), profile)) {
            setAbnormal(true);

            return links;
        }

        // We can have a link to bar and a link to repeat dot above or below
        final List<Inter> systemBars = system.getSig().inters(BarlineInter.class);
        Collections.sort(systemBars, Inters.byAbscissa);

        final Link barLink = lookupBarLink(system, systemBars, profile);
        if (barLink != null) {
            links.add(barLink);
        }

        final Link dotLink = lookupDotLink(system, profile);
        if (dotLink != null) {
            links.add(dotLink);
        }

        setAbnormal(barLink == null || dotLink == null);

        return links;
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, RepeatDotBarRelation.class, RepeatDotPairRelation.class);
    }

}
