//----------------------------------------------------------------------------//
//                                                                            //
//                           S k e l e t o n i z e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Shape;

import omr.image.BufferedSource;
import omr.image.GlobalFilter;
import omr.image.ImageLoader;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;

import omr.score.Score;
import omr.score.ui.PageEraser;

import omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Class {@code Skeletonizer} retrieves the skeleton of sheet image.
 *
 * @author Hervé Bitteur
 */
public class Skeletonizer
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Skeletonizer.class);

    /** Shapes to be erased from skeleton. */
    private static final Set<Shape> shapes = EnumSet.copyOf(
            Arrays.asList(
                    Shape.NOTEHEAD_BLACK,
                    Shape.NOTEHEAD_BLACK_SMALL,
                    Shape.BEAM,
                    Shape.BEAM_HOOK,
                    Shape.BEAM_SMALL,
                    Shape.BEAM_HOOK_SMALL));

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new Skeletonizer object.
     *
     * @param sheet the underlying sheet
     */
    public Skeletonizer (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    public PixelBuffer buildSkeleton ()
    {
        // For this prototype, we simply read the skeleton image from disk
        // and make the buffer available in sheet Picture.
        logger.info("{}buildSkeleton", sheet.getLogPrefix());

        Score score = sheet.getScore();
        File imageFile = score.getImageFile();
        String name = FileUtil.getNameSansExtension(imageFile);
        File sklFile = new File(
                imageFile.getParent(),
                name + ".skl.png");
        BufferedImage img = ImageLoader.loadImages(sklFile, null)
                .get(1);

        // Erase goodies of each system
        Graphics2D g = img.createGraphics();
        PageEraser eraser = new PageEraser(g, sheet);
        eraser.erase(shapes);

        // Draw a rectangle image border
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);

        PixelFilter filter = new GlobalFilter(new BufferedSource(img), 127);
        PixelBuffer buf = new PixelBuffer(filter);

        return buf;
    }
}
