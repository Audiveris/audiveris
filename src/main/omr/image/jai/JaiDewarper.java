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

import omr.image.Picture;
import omr.image.Picture.ImageKey;

import omr.sheet.Sheet;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpGrid;

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

    /** The related sheet. */
    private final Sheet sheet;

    /** The dewarp grid */
    private Warp dewarpGrid;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new JaiDewarper object.
     *
     * @param sheet the related sheet
     */
    public JaiDewarper (Sheet sheet)
    {
        this.sheet = sheet;
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
    public BufferedImage dewarpImage ()
    {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(Picture.invert(sheet.getPicture().getImage(ImageKey.INITIAL)));
        pb.add(dewarpGrid);
        pb.add(new InterpolationBilinear());

        BufferedImage dewarpedImage = Picture.invert(
                JAI.create("warp", pb).getAsBufferedImage());

        ///((PlanarImage) dewarpedImage).getTiles();
        return dewarpedImage;
    }
}
