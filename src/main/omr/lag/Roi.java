//----------------------------------------------------------------------------//
//                                                                            //
//                                   R o i                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.facets.Glyph;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.RunsTable;

import java.awt.Rectangle;
import java.util.Collection;

/**
 * Interface {@code Roi} defines aan absolute rectangular region of
 * interest, on which histograms can be computed vertically and
 * horizontally.
 *
 * @author Hervé Bitteur
 */
public interface Roi
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the rectangular contour, in absolute coordinates
     *
     * @return the absolute contour
     */
    Rectangle getAbsoluteContour ();

    /**
     * Report the histogram obtained in the provided projection orientation
     * of the runs contained in the provided glyphs
     *
     * @param projection the orientation of the projection
     * @param glyphs     the provided glyphs (which can contain sections of
     *                   various
     *                   orientations)
     * @return the computed histogram
     */
    Histogram<Integer> getGlyphHistogram (Orientation projection,
                                          Collection<Glyph> glyphs);

    /**
     * Report the histogram obtained in the provided projection orientation
     * of the runs contained in the provided runs table
     *
     * @param projection the orientation of the projection
     * @param table      the runs table
     * @return the computed histogram
     */
    Histogram<Integer> getRunHistogram (Orientation projection,
                                        RunsTable table);

    /**
     * Report the histogram obtained in the provided projection orientation
     * of the runs contained in the provided sections
     *
     * @param projection the orientation of the projection
     * @param sections   the provided sections (which can be of various
     *                   orientations)
     * @return the computed histogram
     */
    Histogram<Integer> getSectionHistogram (Orientation projection,
                                            Collection<Section> sections);
}
