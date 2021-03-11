//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B e a m H o o k I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BeamHookInter} represents a beam hook interpretation.
 *
 * @see BeamInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-hook")
public class BeamHookInter
        extends AbstractBeamInter
{

    private static final Constants constants = new Constants();

    /**
     * Creates a new HookInter object.
     *
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public BeamHookInter (GradeImpacts impacts,
                          Line2D median,
                          double height)
    {
        super(Shape.BEAM_HOOK, impacts, median, height);
    }

    /**
     * Creates manually a new HookInter ghost object
     *
     * @param grade quality grade
     */
    public BeamHookInter (Double grade)
    {
        super(Shape.BEAM_HOOK, grade);
    }

    /**
     * Meant for JAXB.
     */
    private BeamHookInter ()
    {
        super(null, null, null, 0);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    /**
     * Check if this beam hook is connected to a stem.
     *
     * @return true if abnormal
     */
    @Override
    public boolean checkAbnormal ()
    {
        setAbnormal(!sig.hasRelation(this, BeamStemRelation.class));

        return isAbnormal();
    }

    //--------//
    // isHook //
    //--------//
    @Override
    public boolean isHook ()
    {
        return true;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        final List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        final Link link = lookupLink(systemStems, system, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, BeamStemRelation.class);
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((median == null) && (glyph != null)) {
            // Case of manual hook: Compute height and median parameters and area
            Rectangle box = glyph.getBounds();
            height = (int) Math.rint(glyph.getMeanThickness(Orientation.HORIZONTAL));

            Point2D centroid = glyph.getCentroidDouble();
            double slope = 0.0; // Glyph line is not reliable for a short item like a hook!
            Point2D p1 = LineUtil.intersectionAtX(centroid, slope, box.x);
            Point2D p2 = LineUtil.intersectionAtX(centroid, slope, box.x + box.width);
            median = new Line2D.Double(p1.getX(), p1.getY() + 0.5, p2.getX(), p2.getY() + 0.5);

            computeArea();
        }
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this beam hook instance and a stem nearby,
     * either on left or on right side.
     *
     * @param systemStems ordered collection of stems in system
     * @param system      containing system
     * @param profile     desired profile level
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemStems,
                             SystemInfo system,
                             int profile)
    {
        final Scale scale = system.getSheet().getScale();
        final int xMargin = scale.toPixels(
                (Scale.Fraction) constants.getConstant(constants.xMargin, profile));
        final int yMargin = scale.toPixels(
                (Scale.Fraction) constants.getConstant(constants.yMargin, profile));
        final Rectangle luBox = getBounds();
        luBox.grow(xMargin, yMargin);

        final List<Inter> stems = Inters.intersectedInters(
                systemStems,
                GeoOrder.BY_ABSCISSA,
                luBox);

        // Find out the best stem candidate, if any
        double bestDist = Double.MAX_VALUE;
        StemInter bestStem = null;
        BeamStemRelation bestRel = null;

        for (HorizontalSide side : HorizontalSide.values()) {
            Point refPt = (side == LEFT) ? getCenterLeft() : getCenterRight();

            for (Inter stemInter : stems) {
                StemInter stem = (StemInter) stemInter;
                double d2 = stem.getMedian().ptSegDist(refPt);

                if (bestDist > d2) {
                    bestDist = d2;
                    bestStem = stem;
                    bestRel = new BeamStemRelation();
                    bestRel.setExtensionPoint(refPt); // Approximate
                    bestRel.setBeamPortion((side == LEFT) ? BeamPortion.LEFT : BeamPortion.RIGHT);
                }
            }
        }

        if (bestRel == null) {
            return null;
        }

        return new Link(bestStem, bestRel, true);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        Scale.Fraction xMargin = new Scale.Fraction(0.5, "Width of lookup area for stem");

        @SuppressWarnings("unused")
        Scale.Fraction xMargin_p1 = new Scale.Fraction(0.75, "Idem for profile 1");

        Scale.Fraction yMargin = new Scale.Fraction(0.5, "Height of lookup area for stem");

        @SuppressWarnings("unused")
        Scale.Fraction yMargin_p1 = new Scale.Fraction(0.75, "Idem for profile 1");
    }
}
