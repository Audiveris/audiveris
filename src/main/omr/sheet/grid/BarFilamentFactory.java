//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B a r F i l a m e n t F a c t o r y                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.dynamic.CurvedFilament;
import omr.glyph.dynamic.Filament;
import omr.glyph.dynamic.FilamentFactory;

import omr.lag.Section;

import omr.math.GeoUtil;
import static omr.run.Orientation.VERTICAL;

import omr.sheet.Scale;

import omr.util.Dumping;
import omr.util.StopWatch;

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
            logger.warn("FilamentsFactory cannot retrieveLineFilament", ex);

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
        // Otherwise, this is limited to rather long section only
        Rectangle oSct = VERTICAL.oriented(section.getBounds());

        if (core.union(oSct).height <= core.height) {
            return true;
        }

        return oSct.width >= params.minCoreSectionLength;
    }

    //----------------//
    // expandFilament //
    //----------------//
    private void expandFilament (Filament fil,
                                 Rectangle core,
                                 Collection<Section> source)
    {
        final List<Section> sections = new ArrayList<Section>(source);
        boolean expanding;

        do {
            expanding = false;

            for (Iterator<Section> it = sections.iterator(); it.hasNext();) {
                Section section = it.next();

                if (canMerge(fil, core, section)) {
                    if (logger.isDebugEnabled() || fil.isVip() || section.isVip()) {
                        logger.info("VIP merging {} w/ {}", fil, section);
                    }

                    fil.addSection(section, false);
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
        final Filament fil = new CurvedFilament(scale.getInterline());

        for (Section section : source) {
            Rectangle sectRect = VERTICAL.oriented(section.getBounds());

            // Section with significant length, intersecting core and centroid in core alignment?
            if (sectRect.width >= params.minCoreSectionLength) {
                if (sectRect.intersects(core)) {
                    Point oCentroid = VERTICAL.oriented(section.getCentroid());

                    if (GeoUtil.yEmbraces(core, oCentroid.y)) {
                        fil.addSection(section, false);
                    }
                }
            }
        }

        if (!fil.getMembers().isEmpty()) {
            //            if (constants.registerEachAndEveryGlyph.isSet()) {
            //                return (Filament) nest.registerGlyph(fil); // Not really useful, but eases debug
            //            } else {
            //                return fil;
            //            }
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

        private final Constant.Boolean registerEachAndEveryGlyph = new Constant.Boolean(
                false,
                "(Debug) should we register each and every glyph?");

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1,
                "Minimum length for a section to be considered as core");
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

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            minCoreSectionLength = scale.toPixels(constants.minCoreSectionLength);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
