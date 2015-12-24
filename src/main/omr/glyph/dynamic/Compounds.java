//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C o m p o u n d s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.lag.Section;

import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.sheet.Scale;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
        return new Comparator<SectionCompound>()
        {
            @Override
            public int compare (SectionCompound s1,
                                SectionCompound s2)
            {
                return s2.getLength(orientation) - s1.getLength(orientation);
            }
        };
    }

    //-------------//
    // compoundsOf //
    //-------------//
    /**
     * Report the set of compound instances that are pointed back by the
     * provided collection of sections.
     *
     * @param sections the provided sections
     * @return the set of active containing compound instances
     */
    public static Set<SectionCompound> compoundsOf (Collection<Section> sections)
    {
        Set<SectionCompound> compounds = new LinkedHashSet<SectionCompound>();

        for (Section section : sections) {
            SectionCompound compound = section.getCompound();

            if (compound != null) {
                compounds.add(compound);
            }
        }

        return compounds;
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
     * @param section     section contributing to the resulting thickness
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
        Set<Section> sections = new TreeSet<Section>();

        for (SectionCompound compound : compounds) {
            sections.addAll(compound.getMembers());
        }

        return sections;
    }
}
