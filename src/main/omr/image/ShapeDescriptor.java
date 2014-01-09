//----------------------------------------------------------------------------//
//                                                                            //
//                        S h a p e D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.NOTEHEAD_VOID;
import static omr.glyph.Shape.WHOLE_NOTE;

import omr.image.Anchored.Anchor;
import omr.image.Template.Key;
import omr.image.Template.Lines;
import omr.image.Template.Stems;
import static omr.image.Template.Stems.*;

import omr.ui.symbol.MusicFont;
import static omr.ui.symbol.MusicFont.getFont;
import omr.ui.symbol.TemplateSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * Class {@code ShapeDescriptor} handles all the templates for a shape
 * at a given global interline value.
 * <p>
 * It gathers all relevant template variants (they may depend on lines and stems
 * configurations).
 * All these variants share the same physical dimension (width * height).
 * <p>
 * TODO: Support could be added for slightly different widths, if so needed.
 *
 * @author Hervé Bitteur
 */
public class ShapeDescriptor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            ShapeDescriptor.class);

    /** Color for foreground pixels. */
    private static final int BLACK = Color.BLACK.getRGB();

    /** Color for background pixels. */
    private static final int WHITE = Color.WHITE.getRGB();

    /** Color for hole pixels. */
    private static final int GREEN = Color.GREEN.getRGB();

    /** Color for background pixels (fully transparent). */
    private static final int TRANS = new Color(0, 0, 0, 0).getRGB();

    //~ Instance fields --------------------------------------------------------
    private final Shape shape;

    private final boolean isSmall;

    private final int interline;

    private final Map<Key, Template> variants = new HashMap<Key, Template>();

    private int width = -1;

    private int height = -1;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ShapeDescriptor object.
     *
     * @param shape     the described shape
     * @param interline global scale value
     */
    public ShapeDescriptor (Shape shape,
                            int interline)
    {
        this.shape = shape;
        this.interline = interline;
        isSmall = shape.isSmall();

        buildAllVariants();
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // evaluate //
    //----------//
    /**
     * Try all defined templates at specified location and report
     * the best distance found.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    location WRT template
     * @param distances table of distances
     * @param lines     specific lines configuration, if any
     * @return the best distance found
     */
    public double evaluate (int x,
                            int y,
                            Anchor anchor,
                            DistanceTable distances,
                            Lines lines)
    {
        double best = Double.MAX_VALUE;

        for (Entry<Key, Template> entry : variants.entrySet()) {
            if ((lines == null) || (lines == entry.getKey().lines)) {
                best = Math.min(
                        best,
                        entry.getValue().evaluate(x, y, anchor, distances));
            }
        }

        return best;
    }

    //-------------//
    // getBoundsAt //
    //-------------//
    public Rectangle getBoundsAt (int x,
                                  int y,
                                  Anchored.Anchor anchor)
    {
        for (Template tpl : variants.values()) {
            final Point offset = tpl.getOffset(anchor);

            return new Rectangle(x - offset.x, y - offset.y, width, height);
        }

        return null;
    }

    //-----------//
    // getHeight //
    //-----------//
    public int getHeight ()
    {
        return height;
    }

    //-----------//
    // getOffset //
    //-----------//
    public Point getOffset (Anchored.Anchor anchor)
    {
        for (Template tpl : variants.values()) {
            return tpl.getOffset(anchor);
        }

        return null;
    }

    //----------//
    // getShape //
    //----------//
    public Shape getShape ()
    {
        return shape;
    }

    //-------------//
    // getTemplate //
    //-------------//
    public Template getTemplate (Key key)
    {
        return variants.get(key);
    }

    //----------//
    // getWidth //
    //----------//
    public int getWidth ()
    {
        return width;
    }

    //-------------//
    // putTemplate //
    //-------------//
    public void putTemplate (Key key,
                             Template tpl)
    {
        variants.put(key, tpl);
        width = tpl.getWidth();
        height = tpl.getHeight();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(shape);
        sb.append("-");
        sb.append(interline);
        sb.append("(")
                .append(width)
                .append("x")
                .append(height)
                .append(")");

        return sb.toString();
    }

    //------------------//
    // computeDistances //
    //------------------//
    /**
     * Compute all distances to nearest foreground pixel.
     *
     * @param img the source image
     * @param key the template specs
     * @return the table of distances to foreground
     */
    private static Table computeDistances (BufferedImage img,
                                           Key key)
    {
        // Retrieve foreground pixels (those with pix == 0)
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
            distances.dump(key + "  distances");
        }

        return distances;
    }

    //---------//
    // getCode //
    //---------//
    private static int getCode (Key key)
    {
        switch (key.shape) {
        case NOTEHEAD_BLACK:
        case NOTEHEAD_BLACK_SMALL:
            return 207;

        case NOTEHEAD_VOID:
        case NOTEHEAD_VOID_SMALL:
            return 250;

        case WHOLE_NOTE:
        case WHOLE_NOTE_SMALL:
            return 119;
        }

        logger.error(key.shape + " is not supported!");

        return 0;
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
    private static List<PixelDistance> getKeyPoints (BufferedImage img,
                                                     Table distances)
    {
        // Generate key points
        List<PixelDistance> keyPoints = new ArrayList<PixelDistance>();

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                Color pix = new Color(img.getRGB(x, y), true);

                // Select only relevant pixels (black or green)
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

    //------------//
    // addAnchors //
    //------------//
    /**
     * Add specific anchors to the template.
     * All templates get basic anchors at construction time, but some may need
     * additional anchors.
     *
     * @param template the template to populate
     */
    private void addAnchors (Template template)
    {
        switch (template.getKey().shape) {
        case NOTEHEAD_VOID:
        case NOTEHEAD_VOID_SMALL:
        case NOTEHEAD_BLACK:
        case NOTEHEAD_BLACK_SMALL:
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

    //    //----------//
    //    // binarize //
    //    //----------//
    //    /**
    //     * Use only fully black or fully white pixels
    //     *
    //     * @param img       the source image
    //     * @param threshold level to separate foreground and background
    //     */
    //    private static void binarize (BufferedImage img,
    //                                  int threshold)
    //    {
    //        for (int y = 0, h = img.getHeight(); y < h; y++) {
    //            for (int x = 0, w = img.getWidth(); x < w; x++) {
    //                Color pix = new Color(img.getRGB(x, y), true);
    //                img.setRGB(x, y, (pix.getRed() < threshold) ? BLACK : WHITE);
    //            }
    //        }
    //    }
    //----------//
    // addHoles //
    //----------//
    /**
     * Background pixels inside a given shape must be recognized as
     * such.
     * <p>
     * Nota: This feature is limited to standard size shapes.
     * Such pixels are marked with a specific color (green foreground) so that
     * the template can measure their distance to (black) foreground.
     *
     * @param img   the source image
     * @param shape the template shape
     * @param lines the lines configuration
     */
    private void addHoles (BufferedImage img,
                           Shape shape,
                           Lines lines)
    {
        if (EnumSet.of(NOTEHEAD_VOID, WHOLE_NOTE)
                .contains(shape)) {
            // Identify holes
            final List<Point> holeSeeds = new ArrayList<Point>();

            if (lines == Lines.LINE_MIDDLE) {
                // We have a ledger in the middle, with holes above and below
                holeSeeds.add(
                        new Point(
                                (int) Math.rint(img.getWidth() * 0.6),
                                (int) Math.rint(img.getHeight() * 0.33)));
                holeSeeds.add(
                        new Point(
                                (int) Math.rint(img.getWidth() * 0.4),
                                (int) Math.rint(img.getHeight() * 0.67)));
            } else {
                // We have no ledger, just a big hole in the center
                holeSeeds.add(
                        new Point(img.getWidth() / 2, img.getHeight() / 2));
            }

            // Fill the holes if any with green color
            FloodFiller floodFiller = new FloodFiller(img);

            for (Point seed : holeSeeds) {
                // Background (transparent) -> green (foreground)
                floodFiller.fill(seed.x, seed.y, TRANS, GREEN);
            }
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
        //TODO: this won't work for small shapes because of their background
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                Color pix = new Color(img.getRGB(x, y), true);
                img.setRGB(x, y, (pix.getAlpha() >= threshold) ? BLACK : TRANS);
            }
        }
    }

    //------------------//
    // buildAllVariants //
    //------------------//
    private void buildAllVariants ()
    {
        final MusicFont font = getFont(this.interline);

        if (isSmall) {
            // For small size, we need to play with lines
            // and perhaps with stems as well
            for (Stems stems : Template.Stems.values()) {
                // No stem for whole notes
                if ((shape == Shape.WHOLE_NOTE) && (stems != STEM_NONE)) {
                    continue;
                }

                for (Lines lines : EnumSet.complementOf(
                        EnumSet.of(Lines.LINE_NONE))) {
                    Key key = new Key(shape, lines, stems);
                    Template tpl = createTemplate(key, font);
                    putTemplate(key, tpl);
                }
            }
        } else {
            // For standard size, we don't use stems
            // and just middle line or none
            Stems stems = Stems.STEM_NONE;

            for (Lines lines : EnumSet.of(Lines.LINE_NONE, Lines.LINE_MIDDLE)) {
                Key key = new Key(shape, lines, stems);
                Template tpl = createTemplate(key, font);
                putTemplate(key, tpl);
            }
        }
    }

    //----------------//
    // createTemplate //
    //----------------//
    /**
     * Build a template for desired shape and size, based on
     * MusicFont.
     * TODO: Implement a better way to select representative key points
     * perhaps using a skeleton for foreground and another skeleton for
     * holes?
     *
     * @param key  full identification of the template
     * @param font the underlying music font properly scaled
     * @return the brand new template
     */
    private Template createTemplate (Key key,
                                     MusicFont font)
    {
        final TemplateSymbol symbol = new TemplateSymbol(key, getCode(key));

        // Get a B&W image (no gray)
        final BufferedImage img = symbol.buildImage(font);
        binarize(img, 100);

        // Distances to foreground
        final Table distances = computeDistances(img, key);

        // Add holes if any
        addHoles(img, shape, key.lines);

        // Store a copy on disk for visual check?
        if (constants.keepTemplates.isSet()) {
            try {
                File file = new File(WellKnowns.TEMP_FOLDER, key + ".tpl.png");
                ImageIO.write(img, "png", file);
            } catch (IOException ex) {
                logger.warn("Error storing template", ex);
            }
        }

        // Generate key points (for both foreground and holes)
        final List<PixelDistance> keyPoints = getKeyPoints(img, distances);

        // Generate the template instance
        Template template = new Template(
                key,
                img.getWidth(),
                img.getHeight(),
                keyPoints);

        // Add specific anchor points, if any
        addAnchors(template);

        if (logger.isDebugEnabled()) {
            logger.info("Created {}", template);
            template.dump();
        }

        return template;
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
