//----------------------------------------------------------------------------//
//                                                                            //
//                              T e s t W a r p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.sheet.picture.PictureLoader;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;

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
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TestWarp.class);

    /** Identity */
    private static final AffineTransform identity = new AffineTransform();

    //~ Instance fields --------------------------------------------------------

    private RenderedImage srcImage;
    private RenderedImage dstImage;
    private Dimension     dimension;

    //~ Constructors -----------------------------------------------------------

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
        int            xStep = 500;
        int            xNumCells = 2;
        int            yStep = 500;
        int            yNumCells = 1;
        float[]        warpPositions = new float[] {
                                           -100f, 0f, 500f, 100f, 1000f, 0f, // top line
        0f, 500f, 500f, 500f, 1000f, 500f
                                       }; // bot line
        Warp           warp = new WarpGrid(
            0,
            xStep,
            xNumCells,
            0,
            yStep,
            yNumCells,
            warpPositions);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(invert(srcImage));
        pb.add(warp);
        pb.add(new InterpolationBilinear());
        dstImage = invert(JAI.create("warp", pb));
        ((PlanarImage) dstImage).getTiles();
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        JFrame      frame = new JFrame("TestWarp");
        TestWarp    panel = new TestWarp("examples/Baroque.png");
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
        int           width = cols * dx;
        int           height = rows * dy;
        BufferedImage img = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D    g = (Graphics2D) img.getGraphics();
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
            new ParameterBlock().addSource(image).add(null).add(null).add(null).add(
                null).add(null),
            null);
    }
}
