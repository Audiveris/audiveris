//----------------------------------------------------------------------------//
//                                                                            //
//                          T e m p l a t e F a c t o r y                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.WellKnowns;

import omr.glyph.Shape;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Class {@code TemplateFactory} builds needed instances of
 * {@link Template} and keeps a catalog per desired size and shape.
 *
 * @author Hervé Bitteur
 */
public class TemplateFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            TemplateFactory.class);

    /** Singleton. */
    private static TemplateFactory INSTANCE = new TemplateFactory();

    //~ Instance fields --------------------------------------------------------
    //
    /** Catalog of templates already allocated. */
    private final Map<Integer, Map<Shape, Template>> allSizes;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // TemplateFactory //
    //-----------------//
    /**
     * Creates a new TemplateFactory object.
     */
    private TemplateFactory ()
    {
        allSizes = new HashMap<Integer, Map<Shape, Template>>();
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static TemplateFactory getInstance ()
    {
        return INSTANCE;
    }

    //-------------//
    // getTemplate //
    //-------------//
    /**
     * Return a template for shape at desired interline value
     *
     * @param shape     specific shape
     * @param interline desired interline
     * @return the ready-to-use template
     */
    public Template getTemplate (Shape shape,
                                 int interline)
    {
        Map<Shape, Template> catalog = allSizes.get(interline);

        if (catalog == null) {
            catalog = new EnumMap<Shape, Template>(Shape.class);
            allSizes.put(interline, catalog);
        }

        Template template = catalog.get(shape);

        if (template == null) {
            template = createTemplate(shape, interline);
            catalog.put(shape, template);
        }

        return template;
    }

    //----------------//
    // createTemplate //
    //----------------//
    /**
     * Build a template for desired shape and size, based on MusicFont.
     * TODO: Implement a better way to select representative key points
     * perhaps using a skeleton for foreground and another skeleton for holes?
     *
     * @param shape     desired shape
     * @param interline desired size
     * @return the brand new template
     */
    private Template createTemplate (Shape shape,
                                     int interline)
    {
        final BufferedImage img = MusicFont.buildImage(shape, interline, false);
        final List<Point> holeSeeds = new ArrayList<Point>();

        switch (shape) {
        case VOID_EVEN:
        case WHOLE_EVEN:
            // We have a ledger in the middle, with holes above and below
            holeSeeds.add(
                    new Point(
                    (int) Math.rint(img.getWidth() * 0.6),
                    (int) Math.rint(img.getHeight() * 0.33)));
            holeSeeds.add(
                    new Point(
                    (int) Math.rint(img.getWidth() * 0.4),
                    (int) Math.rint(img.getHeight() * 0.67)));

            break;

        case VOID_ODD:
        case WHOLE_ODD:
            // We have no ledger, just a big hole in the center
            holeSeeds.add(new Point(img.getWidth() / 2, img.getHeight() / 2));

            break;

        default:
        }

        // Fill the holes if any
        FloodFiller floodFiller = new FloodFiller(img);

        for (Point seed : holeSeeds) {
            floodFiller.fill(seed.x, seed.y, 0x00000000, 0xFF00FF00);
        }

        // Make a copy on disk for visual check
        try {
            ImageIO.write(
                    img,
                    "png",
                    new File(WellKnowns.TEMP_FOLDER, shape + ".png"));
        } catch (IOException ex) {
            logger.warn("Error storing template", ex);
        }

        // Retrieve (foreground) reference points
        int width = img.getWidth();
        int height = img.getHeight();
        boolean[][] refs = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pix = img.getRGB(x, y);
                int alpha = pix >> 24;

                if (alpha < 0) {
                    alpha += 256;
                }

                int green = (pix & 0x0000FF00) >> 8;

                if (green < 0) {
                    green += 256;
                }

                //                logger.info(
                //                    String.format(
                //                        "x:%2d y:%2d pix:%h alpha:%d green:%d",
                //                        x,
                //                        y,
                //                        pix,
                //                        alpha,
                //                        green));
                if (alpha == 255) {
                    // Foreground?
                    if (green == 0) {
                        refs[x][y] = true;
                    }
                }
            }
        }

        // Compute template distance transform
        double[][] distances = new ChamferDistance().compute(refs);

        // Generate key points (both foreground and hole background)
        List<PixDistance> keyPoints = new ArrayList<PixDistance>();
        int fgCount = 0;
        int bgCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pix = img.getRGB(x, y);
                int alpha = pix >> 24;

                if (alpha < 0) {
                    alpha += 256;
                }

                int green = (pix & 0x0000FF00) >> 8;

                if (green < 0) {
                    green += 256;
                }

                if (alpha == 255) {
                    if (green == 0) {
                        // Foreground
                        if ((fgCount++ % 1) == 0) {
                            keyPoints.add(new PixDistance(x, y, 0));
                        }
                    } else {
                        // Hole background 
                        if ((bgCount++ % 1) == 0) {
                            keyPoints.add(
                                    new PixDistance(x, y, distances[x][y]));
                        }
                    }
                }
            }
        }

        // Generate the template instance
        Template temp = new Template(
                shape.name(),
                width,
                height,
                keyPoints,
                Symbols.getSymbol(shape, false));

        // Add anchor points, if any
        switch (shape) {
        case VOID_EVEN:
        case VOID_ODD:
            temp.setAnchor(Template.Anchor.LEFT_STEM, 0.05, 0.5);
            temp.setAnchor(Template.Anchor.RIGHT_STEM, 0.95, 0.5);

            break;

        default:
        }

        logger.info("Created {}", temp);
        temp.dump();

        return temp;
    }
}
