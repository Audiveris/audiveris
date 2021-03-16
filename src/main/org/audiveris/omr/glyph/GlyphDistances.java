//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   G l y p h D i s t a n c e s                                  //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.image.ChamferDistance;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;

import java.awt.Rectangle;
import java.util.Iterator;

/**
 * Class {@code GlyphDistances} handles distances around a glyph.
 * It uses a chamfer distance table computed around the given glyph.
 *
 * @author Hervé Bitteur
 */
public class GlyphDistances
{

    /** Table of distances around the glyph. */
    private final DistanceTable distTable;

    /** Table bounds. (Generally somewhat larger than glyph bounds) */
    private final Rectangle tableBox;

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

    //------------//
    // distanceTo //
    //------------//
    /**
     * Report the distance (from this glyph) to the provided other glyph.
     *
     * @param other the other glyph
     * @return the minimum distance measured between the two glyph instances
     */
    public double distanceTo (Glyph other)
    {
        final RunTable otherTable = other.getRunTable();
        final int xOffset = other.getLeft();
        final int yOffset = other.getTop();
        int bestDist = Integer.MAX_VALUE;

        if (otherTable.getOrientation().isVertical()) {
            // Vertical runs
            for (int iSeq = 0, iBreak = otherTable.getSize(); iSeq < iBreak; iSeq++) {
                final int x = xOffset + iSeq;

                if (x < tableBox.x) {
                    continue;
                }

                if (x >= (tableBox.x + tableBox.width)) {
                    break;
                }

                for (Iterator<Run> it = otherTable.iterator(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    final int runStart = yOffset + run.getStart();

                    for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                        final int y = runStart + ic;

                        if (tableBox.contains(x, y)) {
                            final int dist = distTable.getValue(x - tableBox.x, y - tableBox.y);

                            if (dist < bestDist) {
                                bestDist = dist;
                            }
                        }
                    }
                }
            }
        } else {
            // Horizontal runs
            for (int iSeq = 0, iBreak = otherTable.getSize(); iSeq < iBreak; iSeq++) {
                final int y = yOffset + iSeq;

                if (y < tableBox.y) {
                    continue;
                }

                if (y >= (tableBox.y + tableBox.height)) {
                    break;
                }

                for (Iterator<Run> it = otherTable.iterator(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    final int runStart = xOffset + run.getStart();

                    for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                        final int x = runStart + ic;

                        if (tableBox.contains(x, y)) {
                            final int dist = distTable.getValue(x - tableBox.x, y - tableBox.y);

                            if (dist < bestDist) {
                                bestDist = dist;
                            }
                        }
                    }
                }
            }
        }

        return (double) bestDist / distTable.getNormalizer();
    }

    //-----------//
    // Distances //
    //-----------//
    private static class Distances
            extends ChamferDistance.Short
    {

        public DistanceTable compute (Glyph glyph,
                                      Rectangle box)
        {
            DistanceTable output = allocateOutput(box.width, box.height, 3);

            // Initialize with glyph data (0 for glyph, -1 for other pixels)
            output.fill(-1);

            final int xOffset = glyph.getLeft();
            final int yOffset = glyph.getTop();
            final RunTable runTable = glyph.getRunTable();

            if (runTable.getOrientation().isVertical()) {
                // Vertical runs
                for (int iSeq = 0, iBreak = runTable.getSize(); iSeq < iBreak; iSeq++) {
                    final int x = xOffset + iSeq;

                    for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                        final Run run = it.next();
                        final int runStart = yOffset + run.getStart();

                        for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                            output.setValue(x - box.x, (runStart + ic) - box.y, 0);
                        }
                    }
                }
            } else {
                // Horizontal runs
                for (int iSeq = 0, iBreak = runTable.getSize(); iSeq < iBreak; iSeq++) {
                    final int y = yOffset + iSeq;

                    for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                        final Run run = it.next();
                        final int runStart = xOffset + run.getStart();

                        for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                            output.setValue((runStart + ic) - box.x, y - box.y, 0);
                        }
                    }
                }
            }

            process(output);

            return output;
        }
    }
}
