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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            TemplateFactory.class);

    /** Singleton. */
    private static TemplateFactory INSTANCE = new TemplateFactory();

    /** Color for foreground pixels. */
    private static final int BLACK = Color.BLACK.getRGB();

    /** Color for hole pixels. */
    private static final int GREEN = Color.GREEN.getRGB();

    /** Color for background pixels (fully transparent). */
    private static final int TRANS = new Color(0, 0, 0, 0).getRGB();

    //~ Instance fields --------------------------------------------------------
    //
    /** Catalog of all templates already allocated. */
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
     * Return a template for shape scaled at desired interline value.
     *
     * @param shape     specific shape
     * @param interline desired interline value
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

    //------------//
    // addAnchors //
    //------------//
    /**
     * Add specific anchors to the template.
     * All templates get a CENTER anchor at construction time, but some may need
     * additional anchors.
     *
     * @param template the template to populate
     */
    private void addAnchors (Template template)
    {
        switch (template.getShape()) {
        case VOID_EVEN:
        case VOID_ODD:
            // Add anchors for potential stems on left and right sides
            template.addAnchor(Template.Anchor.TOP_LEFT_STEM, 0.05, 0.0);
            template.addAnchor(Template.Anchor.LEFT_STEM, 0.05, 0.5);
            template.addAnchor(Template.Anchor.BOTTOM_LEFT_STEM, 0.05, 1.0);

            template.addAnchor(Template.Anchor.TOP_RIGHT_STEM, 0.95, 0.0);
            template.addAnchor(Template.Anchor.RIGHT_STEM, 0.95, 0.5);
            template.addAnchor(Template.Anchor.BOTTOM_RIGHT_STEM, 0.95, 1.0);

            break;

        default:
        }
    }

    //----------//
    // addHoles //
    //----------//
    /**
     * Background pixels inside a given shape must be recognized as
     * such.
     * Such pixels are marked with a specific color (green foreground) so that
     * the template can measure their distance to (black) foreground.
     *
     * @param img   the source image
     * @param shape the template shape
     */
    private void addHoles (BufferedImage img,
                           Shape shape)
    {
        // Identify holes
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

        // Fill the holes if any with green color
        FloodFiller floodFiller = new FloodFiller(img);

        for (Point seed : holeSeeds) {
            // Background (transparent) -> green (foreground)
            floodFiller.fill(seed.x, seed.y, TRANS, GREEN);
        }
    }

    //----------//
    // binarize //
    //----------//
    /**
     * Use only fully black or fully transparent pixels
     *
     * @param img       the source image
     * @param threshold alpha level to separate foreground and background
     */
    private void binarize (BufferedImage img,
                           int threshold)
    {
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                Color pix = new Color(img.getRGB(x, y), true);
                img.setRGB(x, y, (pix.getAlpha() >= threshold) ? BLACK : TRANS);
            }
        }
    }

    //------------------//
    // computeDistances //
    //------------------//
    /**
     * Compute all distances to nearest foreground pixel.
     *
     * @param img   the source image
     * @param shape the template shape
     * @return the table of distances to foreground
     */
    private Table computeDistances (BufferedImage img,
                                    Shape shape)
    {
        // Retrieve foreground pixels (those with alpha = 255)
        final int width = img.getWidth();
        final int height = img.getHeight();
        final boolean[][] fore = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pix = new Color(img.getRGB(x, y), true);

                if (pix.getAlpha() == 255) {
                    fore[x][y] = true;
                }
            }
        }

        // Compute template distance transform
        final Table distances = new ChamferDistance.Short().compute(fore);

        if (logger.isDebugEnabled()) {
            distances.dump(shape + "  distances");
        }

        return distances;
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
        // Get a B&W image (no gray)
        final BufferedImage img = MusicFont.buildImage(shape, interline, false);
        binarize(img, 100);

        // Distances to foreground
        final Table distances = computeDistances(img, shape);

        // Add holes if any
        addHoles(img, shape);

        // Store a copy on disk for visual check?
        if (constants.keepTemplates.isSet()) {
            try {
                File file = new File(
                        WellKnowns.TEMP_FOLDER,
                        shape + ".tpl.png");
                ImageIO.write(img, "png", file);
            } catch (IOException ex) {
                logger.warn("Error storing template", ex);
            }
        }

        // Generate key points (for both foreground and holes)
        final List<PixelDistance> keyPoints = getKeyPoints(img, distances);

        // Generate the template instance
        Template template = new Template(
                shape,
                img.getWidth(),
                img.getHeight(),
                keyPoints,
                Symbols.getSymbol(shape, false));

        // Add specific anchor points, if any
        addAnchors(template);

        if (logger.isDebugEnabled()) {
            logger.info("Created {}", template);
            template.dump();
        }

        return template;
    }

    //--------------//
    // getKeyPoints //
    //--------------//
    /**
     * Build the collection of key points to be used for matching
     * tests.
     * These are the locations where the image distance value will be checked
     * against the recorded template distance value.
     * TODO: We could carefully select a subset of these locations?
     *
     * @param img       the template source image
     * @param distances the template distances
     * @return the collection of key locations, with their corresponding
     *         distance value
     */
    private List<PixelDistance> getKeyPoints (BufferedImage img,
                                              Table distances)
    {
        // Generate key points (both foreground and holes background)
        List<PixelDistance> keyPoints = new ArrayList<PixelDistance>();

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                Color pix = new Color(img.getRGB(x, y), true);

                // Select only foreground pixels (black or green)
                if (pix.getAlpha() == 255) {
                    if (pix.getGreen() == 0) {
                        // True foreground, so dist to nearest foreground is 0
                        keyPoints.add(new PixelDistance(x, y, 0));
                    } else {
                        // Hole pixel, so use dist to nearest foreground
                        keyPoints.add(
                                new PixelDistance(x, y, distances.getValue(x, y)));
                    }
                }
            }
        }

        return keyPoints;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean keepTemplates = new Constant.Boolean(
                false,
                "Should we keep the templates images?");

    }
}
