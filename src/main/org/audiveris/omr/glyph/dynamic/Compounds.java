//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C o m p o u n d s                                       //
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.PointsCollector;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class {@code Compounds} is a collection of static convenient methods,
 * providing features related to a collection of SectionCompound instances.
 *
 * @author Hervé Bitteur
 */
public abstract class Compounds
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No meant to be instantiated.
     */
    private Compounds ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // byReverseLength //
    //-----------------//
    /**
     * For comparing glyph instances on decreasing length.
     *
     * @param orientation the desired orientation reference
     * @return the comparator
     */
    public static Comparator<SectionCompound> byReverseLength (final Orientation orientation)
    {
        return (s1, s2) -> s2.getLength(orientation) - s1.getLength(orientation);
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the resulting thickness of the collection of sticks at provided coordinate.
     *
     * @param coord       the desired coordinate
     * @param orientation the desired orientation reference
     * @param scale       global scale
     * @param section     section contributing to the resulting thickness, perhaps null
     * @param compounds   compound instances contributing to the resulting thickness
     * @return the thickness measured, expressed in number of pixels.
     */
    public static double getThicknessAt (double coord,
                                         Orientation orientation,
                                         Scale scale,
                                         Section section,
                                         SectionCompound... compounds)
    {
        if (compounds.length == 0) {
            if (section == null) {
                return 0;
            } else {
                return section.getMeanThickness(orientation);
            }
        }

        // Retrieve global bounds
        Rectangle absBox = null;

        if (section != null) {
            absBox = section.getBounds();
        }

        for (SectionCompound cpd : compounds) {
            if (absBox == null) {
                absBox = cpd.getBounds();
            } else {
                absBox.add(cpd.getBounds());
            }
        }

        Rectangle oBox = orientation.oriented(absBox);
        int intCoord = (int) Math.floor(coord);

        if ((intCoord < oBox.x) || (intCoord >= (oBox.x + oBox.width))) {
            return 0;
        }

        // Use a large-enough collector
        final Rectangle oRoi = new Rectangle(intCoord, oBox.y, 0, oBox.height);
        final int probeHalfWidth = scale.toPixels(Filament.getProbeWidth()) / 2;
        oRoi.grow(probeHalfWidth, 0);

        PointsCollector collector = new PointsCollector(orientation.absolute(oRoi));

        // Collect sections contribution
        for (SectionCompound cpd : compounds) {
            for (Section sct : cpd.getMembers()) {
                sct.cumulate(collector);
            }
        }

        // Contributing section, if any
        if (section != null) {
            section.cumulate(collector);
        }

        // Case of no pixels found
        if (collector.getSize() == 0) {
            return 0;
        }

        // Analyze range of Y values
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        int[] vals = (orientation == Orientation.HORIZONTAL) ? collector.getYValues()
                : collector.getXValues();

        for (int i = 0, iBreak = collector.getSize(); i < iBreak; i++) {
            int val = vals[i];
            minVal = Math.min(minVal, val);
            maxVal = Math.max(maxVal, val);
        }

        return maxVal - minVal + 1;
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the resulting thickness of the collection of sticks at provided coordinate.
     *
     * @param coord       the desired coordinate
     * @param orientation the desired orientation reference
     * @param scale       global scale
     * @param compounds   compound instances contributing to the resulting thickness
     * @return the thickness measured, expressed in number of pixels.
     */
    public static double getThicknessAt (double coord,
                                         Orientation orientation,
                                         Scale scale,
                                         SectionCompound... compounds)
    {
        return getThicknessAt(coord, orientation, scale, null, compounds);
    }

    //------------//
    // sectionsOf //
    //------------//
    /**
     * Report the set of sections contained by the provided collection
     * of compound instances.
     *
     * @param compounds the provided glyph instances
     * @return the set of all member sections
     */
    public static Set<Section> sectionsOf (Collection<SectionCompound> compounds)
    {
        Set<Section> sections = new TreeSet<>();

        for (SectionCompound compound : compounds) {
            sections.addAll(compound.getMembers());
        }

        return sections;
    }
}
