//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                J u n c t i o n R e t r i e v e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import static omr.image.PixelSource.BACKGROUND;
import static omr.image.PixelSource.FOREGROUND;
import static omr.sheet.curve.Skeleton.*;

import ij.process.ByteProcessor;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(JunctionRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Skeleton buffer. */
    private final ByteProcessor buf;

    /** Vicinity of current pixel. */
    private final Vicinity vicinity = new Vicinity();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new JunctionRetriever object.
     *
     * @param skeleton the underlying skeleton to work upon
     */
    public JunctionRetriever (Skeleton skeleton)
    {
        buf = skeleton.buf;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
            } else {
                if (pix == FOREGROUND) {
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Vicinity //
    //----------//
    /**
     * Gathers the number of immediate neighbors of a pixel and characterizes the links.
     */
    private static class Vicinity
    {
        //~ Instance fields ------------------------------------------------------------------------

        int verts; // Number of neighbors vertically connected

        int horis; // Number of neighbors horizontally connected

        int diags; // Number of neighbors diagonally connected

        //~ Methods --------------------------------------------------------------------------------
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
