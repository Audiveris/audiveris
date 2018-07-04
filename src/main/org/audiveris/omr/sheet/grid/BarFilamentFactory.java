//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B a r F i l a m e n t F a c t o r y                              //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.CurvedFilament;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentFactory;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code BarFilamentFactory} builds the underlying filament of a bar line
 * candidate.
 * <p>
 * As opposed to {@link FilamentFactory}, this class focused on the building of a single filament
 * defined by the bar line rectangular core area.
 * <p>
 * It allows to detect if a bar goes beyond staff height.
 * It also allows to evaluate filament straightness and thus discard peaks due to braces.
 *
 * @author Hervé Bitteur
 */
public class BarFilamentFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BarFilamentFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related scale. */
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BarFilamentFactory} object.
     *
     * @param scale the related scale
     */
    public BarFilamentFactory (Scale scale)
    {
        this.scale = scale;
        params = new Parameters(scale);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // buildBarFilament //
    //------------------//
    /**
     * Aggregate sections into one filament guided by the provided core rectangle.
     *
     * @param source  the collection of candidate input sections
     * @param absCore the core absolute rectangle
     * @return the retrieved filament, or null
     */
    public Filament buildBarFilament (Collection<Section> source,
                                      Rectangle absCore)
    {
        StopWatch watch = new StopWatch("buildBarFilament");

        try {
            // Aggregate long sections that intersect line core onto skeleton line
            watch.start("populateCore");

            Rectangle core = VERTICAL.oriented(absCore);
            Filament fil = populateCore(source, core);

            if (fil == null) {
                return null;
            }

            // Expand with sections left over, when they touch already included ones
            watch.start("expandFilament");
            expandFilament(fil, core, source);

            return fil;
        } catch (Exception ex) {
            logger.warn("BarFilamentFactory cannot buildBarFilament", ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------//
    // canMerge //
    //----------//
    private boolean canMerge (Filament fil,
                              Rectangle core,
                              Section section)
    {
        // A section must always touch one of fil current member sections
        if (!fil.touches(section)) {
            return false;
        }

        // If this does not increase thickness beyond core, it's OK
        Rectangle oSct = VERTICAL.oriented(section.getBounds());

        return core.union(oSct).height <= core.height;
    }

    //----------------//
    // expandFilament //
    //----------------//
    private void expandFilament (Filament fil,
                                 Rectangle core,
                                 Collection<Section> source)
    {
        final List<Section> sections = new ArrayList<Section>(source);
        sections.removeAll(fil.getMembers());

        boolean expanding;

        do {
            expanding = false;

            for (Iterator<Section> it = sections.iterator(); it.hasNext();) {
                Section section = it.next();

                if (canMerge(fil, core, section)) {
                    if (logger.isDebugEnabled() || fil.isVip() || section.isVip()) {
                        logger.info("VIP merging {} w/ {}", fil, section);
                    }

                    fil.addSection(section);
                    it.remove();
                    expanding = true;

                    break;
                }
            }
        } while (expanding);
    }

    //--------------//
    // populateCore //
    //--------------//
    /**
     * Use the long source sections to stick to the provided skeleton and return
     * the resulting filament.
     * <p>
     * Strategy: We use only the long sections that intersect core length (height) and are close
     * enough to the core mid line.
     *
     * @param source the input sections
     * @param core   the oriented core rectangle
     */
    private Filament populateCore (Collection<Section> source,
                                   Rectangle core)
    {
        final Filament fil = new CurvedFilament(scale.getInterline(), params.segmentLength);

        for (Section section : source) {
            Rectangle sectRect = VERTICAL.oriented(section.getBounds());

            // Section with significant length, intersecting core and centroid in core alignment?
            if (sectRect.width >= params.minCoreSectionLength) {
                if (sectRect.intersects(core)) {
                    Point oCentroid = VERTICAL.oriented(section.getCentroid());

                    if (GeoUtil.yEmbraces(core, oCentroid.y)) {
                        fil.addSection(section);
                    }
                }
            }
        }

        if (!fil.getMembers().isEmpty()) {
            return fil;
        } else {
            return null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                0.5,
                "Minimum length for a section to be considered as core");

        private final Scale.Fraction segmentLength = new Scale.Fraction(
                1,
                "Typical length between filament curve intermediate points");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all scale-dependent parameters.
     */
    private class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        public int minCoreSectionLength;

        public int segmentLength;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            minCoreSectionLength = scale.toPixels(constants.minCoreSectionLength);
            segmentLength = scale.toPixels(constants.segmentLength);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
