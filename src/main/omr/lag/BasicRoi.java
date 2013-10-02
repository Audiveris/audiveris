//----------------------------------------------------------------------------//
//                                                                            //
//                              B a s i c R o i                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code BasicRoi} implements an Roi
 *
 * @author Hervé Bitteur
 */
public class BasicRoi
        implements Roi
{
    //~ Instance fields --------------------------------------------------------

    /** Region of interest with absolute coordinates */
    final Rectangle absContour;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // BasicRoi //
    //----------//
    /**
     * Define a region of interest
     *
     * @param absoluteContour the absolute contour of the region of interest,
     *                        specified in the usual (x, y, width, height) form.
     */
    public BasicRoi (Rectangle absoluteContour)
    {
        this.absContour = absoluteContour;
    }

    //~ Methods ----------------------------------------------------------------
    //--------------------//
    // getAbsoluteContour //
    //--------------------//
    @Override
    public Rectangle getAbsoluteContour ()
    {
        return new Rectangle(absContour);
    }

    //-------------------//
    // getGlyphHistogram //
    //-------------------//
    @Override
    public Histogram<Integer> getGlyphHistogram (Orientation projection,
                                                 Collection<Glyph> glyphs)
    {
        return getSectionHistogram(
                projection,
                Glyphs.sectionsOf(glyphs));
    }

    //-----------------//
    // getRunHistogram //
    //-----------------//
    @Override
    public Histogram<Integer> getRunHistogram (Orientation projection,
                                               RunsTable table)
    {
        final Orientation tableOrient = table.getOrientation();
        final boolean alongTheRuns = projection == tableOrient;
        final Histogram<Integer> histo = new Histogram<>();
        final Rectangle tableContour = new Rectangle(
                table.getDimension());
        final Rectangle inter = new Rectangle(
                absContour.intersection(tableContour));
        final Rectangle oriInter = tableOrient.oriented(inter);
        final int minPos = oriInter.y;
        final int maxPos = (oriInter.y + oriInter.height) - 1;
        final int minCoord = oriInter.x;
        final int maxCoord = (oriInter.x + oriInter.width) - 1;

        for (int pos = minPos; pos <= maxPos; pos++) {
            List<Run> seq = table.getSequence(pos);

            for (Run run : seq) {
                final int cMin = Math.max(minCoord, run.getStart());
                final int cMax = Math.min(maxCoord, run.getStop());

                // Clipping on coord
                if (cMin <= cMax) {
                    if (alongTheRuns) {
                        // Along the runs
                        histo.increaseCount(pos, cMax - cMin + 1);
                    } else {
                        // Across the runs
                        for (int i = cMin; i <= cMax; i++) {
                            histo.increaseCount(i, 1);
                        }
                    }
                }
            }
        }

        return histo;
    }

    //---------------------//
    // getSectionHistogram //
    //---------------------//
    @Override
    public Histogram<Integer> getSectionHistogram (Orientation projection,
                                                   Collection<Section> sections)
    {
        // Split the sections into 2 populations along & across wrt projection
        List<Section> along = new ArrayList<>();
        List<Section> across = new ArrayList<>();

        for (Section section : sections) {
            if (section.isVertical() == projection.isVertical()) {
                along.add(section);
            } else {
                across.add(section);
            }
        }

        final Histogram<Integer> histo = new Histogram<>();
        populate(histo, projection, along, true);
        populate(histo, projection.opposite(), across, false);

        return histo;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Roi " + getAbsoluteContour();
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate an histo with a collection of sections
     *
     * @param histo              the histo to populate
     * @param sectionOrientation orientation of the sections
     * @param sections           the collections of (parallel) sections
     * @param alongTheRuns       true if sections are parallel to projection
     */
    private void populate (Histogram<Integer> histo,
                           Orientation sectionOrientation,
                           List<Section> sections,
                           boolean alongTheRuns)
    {
        final Rectangle oriContour = sectionOrientation.oriented(absContour);
        final int minPos = oriContour.y;
        final int maxPos = (oriContour.y + oriContour.height) - 1;
        final int minCoord = oriContour.x;
        final int maxCoord = (oriContour.x + oriContour.width) - 1;

        for (Section section : sections) {
            int pos = section.getFirstPos() - 1;

            for (Run run : section.getRuns()) {
                pos++;

                // Clipping on pos
                if ((pos < minPos) || (pos > maxPos)) {
                    continue;
                }

                final int cMin = Math.max(minCoord, run.getStart());
                final int cMax = Math.min(maxCoord, run.getStop());

                // Clipping on coord
                if (cMin <= cMax) {
                    if (alongTheRuns) {
                        // Along the runs
                        histo.increaseCount(pos, cMax - cMin + 1);
                    } else {
                        // Across the runs
                        for (int i = cMin; i <= cMax; i++) {
                            histo.increaseCount(i, 1);
                        }
                    }
                }
            }
        }
    }
}
