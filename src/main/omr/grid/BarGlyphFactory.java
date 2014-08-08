//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B a r G l y p h F a c t o r y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition;

import omr.lag.Section;

import omr.math.BasicLine;
import omr.math.GeoUtil;
import omr.math.Line;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code BarGlyphFactory} builds the underlying glyph of a bar line candidate.
 * <p>
 * As opposed to {@link FilamentsFactory}, this class focused on the building of a single glyph
 * defined by the bar line rectangular core area.
 * <p>
 * It allows to detect if a bar goes beyond staff height.
 * It also allows to evaluate glyph straightness and thus discard peaks due to braces.
 *
 * @author Hervé Bitteur
 */
public class BarGlyphFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BarGlyphFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related scale. */
    private final Scale scale;

    /** Where filaments are to be stored. */
    private final GlyphNest nest;

    /** Which related layer. */
    private final GlyphLayer layer;

    /** Factory orientation. */
    private final Orientation orientation;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BarGlyphFactory} object.
     *
     * @param scale       the related scale
     * @param nest        the nest to host created filaments
     * @param layer       precise glyph layer
     * @param orientation the target orientation
     */
    public BarGlyphFactory (Scale scale,
                            GlyphNest nest,
                            GlyphLayer layer,
                            Orientation orientation)
    {
        this.scale = scale;
        this.nest = nest;
        this.layer = layer;
        this.orientation = orientation;
        params = new Parameters(scale);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // retrieveBarGlyph //
    //------------------//
    /**
     * Aggregate sections into one filament guided by the provided core rectangle.
     *
     * @param source  the collection of candidate input sections
     * @param absCore the core absolute rectangle
     * @return the retrieved filament, or null
     */
    public Filament retrieveBarGlyph (Collection<Section> source,
                                      Rectangle absCore)
    {
        StopWatch watch = new StopWatch("retrieveBarGlyph");

        try {
            // Aggregate long sections that intersect line core onto skeleton line
            watch.start("populateCore");

            Rectangle core = orientation.oriented(absCore);
            Filament fil = populateCore(source, core);

            if (fil == null) {
                return null;
            }

            // Expand with sections left over, when they touch already included ones
            watch.start("expandFilament");
            expandFilament(fil, core, source);

            // (Re)register filament with its (final) signature
            fil = (Filament) nest.registerGlyph(fil);

            return fil;
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveLineFilament", ex);

            return null;
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //----------//
    // canMerge //
    //----------//
    private boolean canMerge (Glyph fil,
                              Rectangle core,
                              Section section)
    {
        // A section must always touch one of fil current member sections
        if (!fil.touches(section)) {
            return false;
        }

        // If this does not increase thickness beyond core, it's OK
        // Otherwise, this is limited to rather long section only
        Rectangle oSct = orientation.oriented(section.getBounds());

        if (core.union(oSct).height <= core.height) {
            return true;
        }

        return oSct.width >= params.minCoreSectionLength;
    }

    //----------------//
    // expandFilament //
    //----------------//
    private void expandFilament (Glyph fil,
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

                    fil.addSection(section, GlyphComposition.Linking.NO_LINK);
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
        Filament fil = new Filament(scale, layer);

        for (Section section : source) {
            section.setProcessed(false);
            section.resetFat(); // ????????????

            Rectangle sectRect = orientation.oriented(section.getBounds());

            // Section with significant length, intersecting core and centroid in core alignment?
            if (sectRect.width >= params.minCoreSectionLength) {
                if (sectRect.intersects(core)) {
                    Point oCentroid = orientation.oriented(section.getCentroid());

                    if (GeoUtil.yEmbraces(core, oCentroid.y)) {
                        fil.addSection(section, GlyphComposition.Linking.NO_LINK);
                        section.setProcessed(true);
                    }
                }
            }
        }

        if (!fil.getMembers().isEmpty()) {
            if (constants.registerEachAndEveryGlyph.isSet()) {
                return (Filament) nest.registerGlyph(fil); // Not really useful, but eases debug
            } else {
                return fil;
            }
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

        Constant.Boolean registerEachAndEveryGlyph = new Constant.Boolean(
                false,
                "(Debug) should we register each and every glyph?");

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Scale.Fraction minCoreSectionLength = new Scale.Fraction(
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
                Main.dumping.dump(this);
            }
        }
    }
}
