//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e s t W a r p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.jai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;
import javax.swing.*;

/**
 * Class {@code TestWarp}
 *
 * @author Hervé Bitteur
 */
public class TestWarp
        extends JPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TestWarp.class);

    /** Identity */
    private static final AffineTransform identity = new AffineTransform();

    //~ Instance fields ----------------------------------------------------------------------------
    private RenderedImage srcImage;

    private RenderedImage dstImage;

    private Dimension dimension;

    //~ Constructors -------------------------------------------------------------------------------
    //----------//
    // TestWarp //
    //----------//
    /**
     * Creates a new TestWarp object.
     */
    public TestWarp (String path)
    {
        srcImage = JAI.create("fileload", new ParameterBlock().add(path), null);
        //        srcImage = PictureLoader.loadImages(new File(path), null)
        //                                .get(1);
        //        srcImage = buildPattern(20, 10, 50, 50);
        dimension = new Dimension(srcImage.getWidth(), srcImage.getHeight());
        setPreferredSize(dimension);

        //        float[]        xCoeffs = new float[] { 0f, 1.25f, 0.04f };
        //        float[]        yCoeffs = new float[] { 0f, -0.02f, 1.5f };
        //        Warp           warp = new WarpAffine(xCoeffs, yCoeffs);
        //
        int xStep = 500;
        int xNumCells = 2;
        int yStep = 500;
        int yNumCells = 1;
        float[] warpPositions = new float[]{
            -100f, 0f, 500f, 100f, 1000f, 0f, // top line
            0f, 500f, 500f, 500f, 1000f, 500f
        }; // bot line
        Warp warp = new WarpGrid(0, xStep, xNumCells, 0, yStep, yNumCells, warpPositions);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(invert(srcImage));
        pb.add(warp);
        pb.add(new InterpolationBilinear());
        dstImage = invert(JAI.create("warp", pb));
        ((PlanarImage) dstImage).getTiles();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        JFrame frame = new JFrame("TestWarp");
        TestWarp panel = new TestWarp("examples/Baroque.png");
        JScrollPane scrollPane = new JScrollPane(panel);

        frame.add(scrollPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(panel.dimension.width + 11, panel.dimension.height + 37);
        frame.setVisible(true);
    }

    //----------------//
    // paintComponent //
    //----------------//
    @Override
    public void paintComponent (Graphics g)
    {
        // For background
        super.paintComponent(g);

        // Meant for visual check
        if (dstImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawRenderedImage(dstImage, identity);
        }
    }

    //--------------//
    // buildPattern //
    //--------------//
    private BufferedImage buildPattern (int cols,
                                        int rows,
                                        int dx,
                                        int dy)
    {
        int width = cols * dx;
        int height = rows * dy;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.BLACK);

        for (int ir = 0; ir < rows; ir++) {
            int y = ir * dy;

            for (int ic = 0; ic < cols; ic++) {
                int x = ic * dx;
                g.setColor((((ir + ic) % 2) == 0) ? Color.GRAY : Color.WHITE);
                g.fillRect(x, y, dx, dy);
            }
        }

        return img;
    }

    //--------//
    // invert //
    //--------//
    private static RenderedImage invert (RenderedImage image)
    {
        return JAI.create(
                "Invert",
                new ParameterBlock().addSource(image).add(null).add(null).add(null).add(null).add(null),
                null);
    }
}
