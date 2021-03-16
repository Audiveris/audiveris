//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       J a i D e w a r p e r                                    //
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
package org.audiveris.omr.image.jai;

import org.audiveris.omr.image.ImageUtil;
import static org.audiveris.omr.image.ImageUtil.invert;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;

/**
 * Class {@code JaiDewarper} is meant to keep JAI-based de-warping features separate
 * from the rest of Audiveris application, and thus save on jar download.
 *
 * @author Hervé Bitteur
 */
public class JaiDewarper
{

    /** The dewarp grid. */
    private Warp dewarpGrid;

    /**
     * Creates a new JaiDewarper object.
     */
    public JaiDewarper ()
    {
    }

    //----------------//
    // createWarpGrid //
    //----------------//
    /**
     * @param xStart        starting abscissa
     * @param xStep         cell width
     * @param xNumCells     horizontal number of cells
     * @param yStart        starting ordinate
     * @param yStep         cell height
     * @param yNumCells     vertical number of cells
     * @param warpPositions all source (warped) positions as a flat array of (x, y) float values
     */
    public void createWarpGrid (int xStart,
                                int xStep,
                                int xNumCells,
                                int yStart,
                                int yStep,
                                int yNumCells,
                                float[] warpPositions)
    {
        dewarpGrid = new WarpGrid(
                xStart,
                xStep,
                xNumCells,
                yStart,
                yStep,
                yNumCells,
                warpPositions);
    }

    //-------------//
    // dewarpImage //
    //-------------//
    /**
     * Actually de-warp the provided image.
     *
     * @param image the provided (warped) image
     * @return a de-warped version of input image
     */
    public BufferedImage dewarpImage (BufferedImage image)
    {
        assert dewarpGrid != null : "dewarpGrid not defined";

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(ImageUtil.invert(image));
        pb.add(dewarpGrid);
        pb.add(new InterpolationBilinear());

        BufferedImage dewarpedImage = invert(JAI.create("warp", pb).getAsBufferedImage());

        ///((PlanarImage) dewarpedImage).getTiles();
        return dewarpedImage;
    }
}
