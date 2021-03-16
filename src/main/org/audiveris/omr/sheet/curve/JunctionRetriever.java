//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                J u n c t i o n R e t r i e v e r                               //
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
package org.audiveris.omr.sheet.curve;

import ij.process.ByteProcessor;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;
import static org.audiveris.omr.sheet.curve.Skeleton.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code JunctionRetriever} scans all image pixels to retrieve junction pixels
 * and flag them as such with a specific color.
 * <p>
 * A point is a junction point if it has more than 2 immediate neighbors in the 8 peripheral cells
 * of the 3x3 square centered on the point.
 * We use pixel color to detect if a given point has already been visited.
 *
 * @author Hervé Bitteur
 */
public class JunctionRetriever
{

    private static final Logger logger = LoggerFactory.getLogger(JunctionRetriever.class);

    /** Skeleton buffer. */
    private final ByteProcessor buf;

    /** Vicinity of current pixel. */
    private final Vicinity vicinity = new Vicinity();

    /**
     * Creates a new JunctionRetriever object.
     *
     * @param skeleton the underlying skeleton to work upon
     */
    public JunctionRetriever (Skeleton skeleton)
    {
        buf = skeleton.buf;
    }

    //-----------//
    // scanImage //
    //-----------//
    /**
     * Scan the whole image.
     */
    public void scanImage ()
    {
        for (int x = 1, w = buf.getWidth(); x < w; x++) {
            for (int y = 1, h = buf.getHeight(); y < h; y++) {
                int pix = buf.get(x, y);

                if ((pix == FOREGROUND) // Basic pixel, not yet processed
                            || isJunction(pix)) { // Junction, perhaps not the best
                    checkJunction(x, y);
                }
            }
        }
    }

    //---------------//
    // checkJunction //
    //---------------//
    /**
     * Check whether the point at (x, y) is a junction.
     * The point needs to have more than 2 neighbors, and no other junction point side-connected
     * with higher grade.
     *
     * @param x point abscissa
     * @param y point ordinate
     */
    private void checkJunction (int x,
                                int y)
    {
        // Neighbors
        int n = vicinityOf(x, y);

        if (n > 2) {
            int grade = vicinity.getGrade();
            buf.set(x, y, JUNCTION + grade);

            int sideGrade = sideGrade(x, y);

            if (sideGrade > grade) {
                buf.set(x, y, ARC);
            }
        } else {
            buf.set(x, y, ARC);
        }
    }

    //--------------//
    // dirNeighbors //
    //--------------//
    /**
     * Count the immediate neighbors in the provided directions.
     *
     * @param x    center abscissa
     * @param y    center ordinate
     * @param dirs which directions to scan
     * @return the number of non-foreground pixels found
     */
    private int dirNeighbors (int x,
                              int y,
                              int[] dirs)
    {
        int n = 0;

        for (int dir : dirs) {
            int pix = buf.get(x + dxs[dir], y + dys[dir]);

            if (pix != BACKGROUND) {
                n++;
            }
        }

        return n;
    }

    //-----------//
    // sideGrade //
    //-----------//
    /**
     * Look for side-connected junction pixels and return their highest junction grade.
     *
     * @param x center abscissa
     * @param y center ordinate
     * @return the highest junction grade found
     */
    private int sideGrade (int x,
                           int y)
    {
        int bestGrade = 0;

        for (int dir : sideDirs) {
            int nx = x + dxs[dir];
            int ny = y + dys[dir];
            int pix = buf.get(nx, ny);

            if (isJunction(pix)) {
                // Point already evaluated
                bestGrade = Math.max(bestGrade, pix - JUNCTION);
            } else if (pix == FOREGROUND) {
                int n = vicinityOf(nx, ny);

                if (n > 2) {
                    int grade = vicinity.getGrade();
                    buf.set(nx, ny, JUNCTION + grade);
                    bestGrade = Math.max(bestGrade, grade);
                } else {
                    buf.set(nx, ny, ARC);
                }
            }
        }

        return bestGrade;
    }

    //------------//
    // vicinityOf //
    //------------//
    /**
     * Count the immediate neighbors in all directions.
     * Details are written in structure "vicinity".
     *
     * @param x center abscissa
     * @param y center ordinate
     * @return the count of all neighbors.
     */
    private int vicinityOf (int x,
                            int y)
    {
        vicinity.verts = dirNeighbors(x, y, vertDirs);
        vicinity.horis = dirNeighbors(x, y, horiDirs);
        vicinity.diags = dirNeighbors(x, y, diagDirs);

        return vicinity.getCount();
    }

    //----------//
    // Vicinity //
    //----------//
    /**
     * Gathers the number of immediate neighbors of a pixel and characterizes the links.
     */
    private static class Vicinity
    {

        int verts; // Number of neighbors vertically connected

        int horis; // Number of neighbors horizontally connected

        int diags; // Number of neighbors diagonally connected

        public int getCount ()
        {
            return verts + horis + diags;
        }

        public int getGrade ()
        {
            return (2 * verts) + (2 * horis) + (((verts > 0) && (horis > 0)) ? 1 : 0);
        }
    }
}
