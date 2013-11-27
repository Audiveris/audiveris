//----------------------------------------------------------------------------//
//                                                                            //
//                             J a i D e w a r p e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2007-2008.                                //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image.jai;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;
import omr.image.ImageUtil;

import static omr.image.ImageUtil.invert;

/**
 * Class {@code JaiDewarper} is meant to keep JAI-based dewarping
 * features separate from the rest of Audiveris application, and thus
 * saving on jar download.
 *
 * @author Hervé Bitteur
 */
public class JaiDewarper
{
    //~ Instance fields --------------------------------------------------------

    /** The dewarp grid. */
    private Warp dewarpGrid;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new JaiDewarper object.
     */
    public JaiDewarper ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------------//
    // createWarpGrid //
    //----------------//
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
    public BufferedImage dewarpImage (BufferedImage image)
    {
        assert dewarpGrid != null : "dewarpGrid not defined";

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(ImageUtil.invert(image));
        pb.add(dewarpGrid);
        pb.add(new InterpolationBilinear());

        BufferedImage dewarpedImage = invert(
                JAI.create("warp", pb).getAsBufferedImage());

        ///((PlanarImage) dewarpedImage).getTiles();
        return dewarpedImage;
    }
}
