//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   G l y p h D i s t a n c e s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.image.ChamferDistance;
import omr.image.DistanceTable;

import omr.lag.Section;

import omr.run.Orientation;
import omr.run.Run;

import java.awt.Rectangle;

/**
 * Class {@code GlyphDistances} handles distances around a glyph.
 * It uses a chamfer distance table computed around the given glyph.
 *
 * @author Hervé Bitteur
 */
public class GlyphDistances
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Table of distances around the glyph. */
    private final DistanceTable distTable;

    /** Table bounds. (Generally somewhat larger than glyph bounds) */
    private final Rectangle tableBox;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GlyphDistances object, around a provided glyph.
     *
     * @param glyph    the provided glyph
     * @param tableBox the desired rectangle around the glyph
     */
    public GlyphDistances (Glyph glyph,
                           Rectangle tableBox)
    {
        this.tableBox = tableBox;
        distTable = new Distances().compute(glyph, tableBox);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // distanceTo //
    //------------//
    /**
     * Report the distance (from this glyph) to the provided other glyph.
     *
     * @param other the other glyph
     * @return the minimum distance measured between the two glyphs
     */
    public double distanceTo (Glyph other)
    {
        int bestDist = Integer.MAX_VALUE;

        for (Section section : other.getMembers()) {
            Orientation orientation = section.getOrientation();
            int p = section.getFirstPos();

            for (Run run : section.getRuns()) {
                final int start = run.getStart();

                for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                    final int x = (orientation == Orientation.HORIZONTAL) ? (start + ic) : p;
                    final int y = (orientation == Orientation.HORIZONTAL) ? p : (start + ic);

                    if (tableBox.contains(x, y)) {
                        int dist = distTable.getValue(x - tableBox.x, y - tableBox.y);

                        if (dist < bestDist) {
                            bestDist = dist;
                        }
                    }
                }

                p++;
            }
        }

        return (double) bestDist / distTable.getNormalizer();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Distances //
    //-----------//
    private static class Distances
            extends ChamferDistance.Short
    {
        //~ Methods --------------------------------------------------------------------------------

        public DistanceTable compute (Glyph glyph,
                                      Rectangle box)
        {
            DistanceTable output = allocateOutput(box.width, box.height, 3);

            // Initialize with glyph data (0 for glyph, -1 for other pixels)
            output.fill(-1);

            for (Section section : glyph.getMembers()) {
                Orientation orientation = section.getOrientation();
                int p = section.getFirstPos();

                for (Run run : section.getRuns()) {
                    final int start = run.getStart();

                    for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                        if (orientation == Orientation.HORIZONTAL) {
                            output.setValue((start + ic) - box.x, p - box.y, 0);
                        } else {
                            output.setValue(p - box.x, (start + ic) - box.y, 0);
                        }
                    }

                    p++;
                }
            }

            process(output);

            return output;
        }
    }
}
