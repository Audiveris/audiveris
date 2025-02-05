//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e F a c t o r y                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.image;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.TableUtil;
import org.audiveris.omr.sheet.ui.TemplateView;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.TemplateSymbol;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class <code>TemplateFactory</code> builds needed instances of {@link Template} class
 * and keeps a catalog per desired point size and shape.
 * <p>
 * Today there is just a single template per Shape (and pointSize).
 * <p>
 * TODO: investigate the need for template variants:
 * This would apply to NOTEHEAD_VOID_SMALL and NOTEHEAD_BLACK_SMALL for which we could make a
 * difference between the stem side and the opposite (open) side, with background locations on open
 * side and none on stem side.
 * These variants would share the same physical dimension (width * height).
 * <p>
 * For WHOLE_NOTE_SMALL we have a single template with background locations on both sides.
 * <p>
 * All cue notes (*_SMALL shapes) have background locations on upper and lower sides.
 * <p>
 * TODO: Support could be added for slightly different widths, if so needed?
 *
 * @author Hervé Bitteur
 */
public class TemplateFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TemplateFactory.class);

    /** Singleton. */
    private static final TemplateFactory INSTANCE = new TemplateFactory();

    /** All shapes with hole(s). */
    private static final EnumSet shapesWithHoles = EnumSet.of(
            // 2
            BREVE,
            BREVE_SMALL,
            BREVE_CROSS,
            BREVE_DIAMOND,
            BREVE_TRIANGLE_DOWN,
            BREVE_CIRCLE_X,
            // 1
            WHOLE_NOTE,
            WHOLE_NOTE_SMALL,
            WHOLE_NOTE_CROSS,
            WHOLE_NOTE_DIAMOND,
            WHOLE_NOTE_TRIANGLE_DOWN,
            WHOLE_NOTE_CIRCLE_X,
            // 1/2
            NOTEHEAD_VOID,
            NOTEHEAD_VOID_SMALL,
            NOTEHEAD_CROSS_VOID,
            NOTEHEAD_DIAMOND_VOID,
            NOTEHEAD_TRIANGLE_DOWN_VOID,
            NOTEHEAD_CIRCLE_X_VOID,
            // 1/4
            NOTEHEAD_CIRCLE_X);

    /** Color for foreground pixels. */
    private static final int FORE = Color.BLACK.getRGB();

    /** Color for background pixels. */
    private static final int BACK = Color.RED.getRGB();

    /** Color for hole pixels. */
    private static final int HOLE = Color.PINK.getRGB();

    /** Color for irrelevant pixels. */
    private static final int IRRELEVANT = new Color(0, 0, 0, 0).getRGB(); // Fully transparent

    //~ Instance fields ----------------------------------------------------------------------------

    /** All catalogs allocated so far, mapped by font family and point size. */
    private final Map<MusicFamily, Map<Integer, Catalog>> catalogs;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * (Private) Creates the singleton object.
     */
    private TemplateFactory ()
    {
        catalogs = new EnumMap<>(MusicFamily.class);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------------//
    // buildDecoratedImage //
    //---------------------//
    /**
     * For easy visual checking, build a magnified template image with decorations.
     *
     * @param tpl the template at hand
     * @param src source colored image
     * @return decorated magnified image
     */
    private BufferedImage buildDecoratedImage (Template tpl,
                                               BufferedImage src)
    {
        final int width = src.getWidth();
        final int height = src.getHeight();
        final Rectangle slim = tpl.getSlimBounds();

        final int zoom = constants.magnificationRatio.getValue(); // (rather arbitrary ratio)
        final BufferedImage img = new BufferedImage(
                (width + 2) * zoom, // + 2 to cope with ordinate display on left and right
                (height + 2) * zoom, // +2 to cope with abscissa display on top and bottom
                BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        // Fill frame background (with side regions)
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        final AffineTransform at = AffineTransform.getScaleInstance(zoom, zoom);
        at.concatenate(AffineTransform.getTranslateInstance(1, 1));
        g.setTransform(at);

        final Font textFont = new Font("SansSerif", Font.PLAIN, 1).deriveFont(15f / zoom);
        final FontRenderContext frc = g.getFontRenderContext();

        // Draw coordinate values
        g.setColor(Color.BLACK);

        for (int x = 0; x <= width; x++) {
            final String str = Integer.toString(x);
            final TextLayout layout = new TextLayout(str, textFont, frc);
            final Point2D north = new Point2D.Double(x, -0.5);
            OmrFont.paint(g, layout, north, Alignment.AREA_CENTER);

            final Point2D south = new Point2D.Double(x, height + 0.5);
            OmrFont.paint(g, layout, south, Alignment.AREA_CENTER);
        }

        for (int y = 0; y <= height; y++) {
            final String str = Integer.toString(y);
            final TextLayout layout = new TextLayout(str, textFont, frc);
            final Point2D west = new Point2D.Double(-0.5, y);
            OmrFont.paint(g, layout, west, Alignment.AREA_CENTER);

            final Point2D east = new Point2D.Double(width + 0.5, y);
            OmrFont.paint(g, layout, east, Alignment.AREA_CENTER);
        }

        // Fill template background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw template
        g.drawImage(src, null, null);

        // Paint shape symbol
        final Composite oldComposite = g.getComposite();
        g.setComposite(TemplateView.templateComposite);

        MusicFont musicFont = MusicFont.getMusicFont(tpl.getFamily(), tpl.getPointSize());
        final Point2D center = GeoUtil.center2D(slim);
        final TemplateSymbol symbol = new TemplateSymbol(tpl.getShape(), tpl.getFamily());
        symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
        g.setComposite(oldComposite);

        // Draw distances
        UIUtil.setAbsoluteStroke(g, 1f);
        g.setFont(textFont);

        for (PixelDistance pix : tpl.getKeyPoints()) {
            int d = (int) Math.rint(pix.d);

            if (d != 0) {
                g.drawRect(pix.x, pix.y, 1, 1);

                final String str = Integer.toString(d);

                final TextLayout layout = new TextLayout(str, textFont, frc);
                final Point2D location = new Point2D.Double(pix.x + 0.5, pix.y + 0.5);
                OmrFont.paint(g, layout, location, Alignment.AREA_CENTER);
                ///g.drawString(str, pix.x + 0.4f, pix.y + 0.6f);
            }
        }

        g.setColor(Color.GREEN);

        // Draw trimmed bounds
        UIUtil.setAbsoluteStroke(g, 3f);
        g.draw(slim);
        //
        //        // Anchor Point locations, shown as rounded rectangles
        //        for (Entry<Anchor, Point2D> entry : tpl.getOffsets().entrySet()) {
        //            final Anchor anchor = entry.getKey();
        //            final Point pt = tpl.getOffset(anchor);
        //            final RoundRectangle2D rr = new RoundRectangle2D.Double(pt.x, pt.y, 1, 1, 0.5, 0.5);
        //            g.draw(rr);
        //        }

        // Anchor Point2D locations, shown as small circles with anchor abbreviation
        ///g.setXORMode(Color.BLACK);
        for (Entry<Anchor, Point2D> entry : tpl.getOffsets().entrySet()) {
            final Anchor anchor = entry.getKey();
            final Point2D pt2D = entry.getValue();
            final Ellipse2D ellipse = new Ellipse2D.Double(
                    pt2D.getX() - 0.375,
                    pt2D.getY() - 0.375,
                    0.75,
                    0.75);
            g.draw(ellipse);

            final String str = anchor.abbreviation();
            final TextLayout layout = new TextLayout(str, textFont, frc);
            OmrFont.paint(g, layout, pt2D, Alignment.AREA_CENTER);
        }

        g.dispose();

        return img;
    }

    //---------------//
    // buildTemplate //
    //---------------//
    /**
     * Build a template for desired shape and size, based on MusicFont.
     *
     * @param shape     shape of the template
     * @param pointSize precise scaling for font
     * @return the brand new template
     */
    private Template buildTemplate (Shape shape,
                                    MusicFamily family,
                                    int pointSize)
    {
        logger.debug("Building template for {} {} {}", shape, family, pointSize);

        final MusicFont font = MusicFont.getMusicFont(family, pointSize);
        final TemplateSymbol symbol = new TemplateSymbol(shape, family);

        // Check whether we have a usable template symbol
        if (symbol.getDimension(font) == null) {
            logger.info("{} No template for {}", family, shape);

            return null;
        }

        final Rectangle fatBounds = symbol.getFatBounds(font);
        final BufferedImage img = symbol.buildImage(font); // Gray pixels
        binarize(img, constants.binarizationThreshold.getValue()); // B&W pixels + IRRELEVANT
        final Rectangle slimBounds = getSlimBounds(img, fatBounds);

        // Pre-populate template keyPoints?
        final List<PixelDistance> keyPoints = (constants.prePopulateKeyPoints.isSet()
                || constants.saveTemplates.isSet()) ? retrieveKeyPoints(
                        shape,
                        family,
                        pointSize,
                        img,
                        fatBounds) : null;

        // Generate the template instance
        final Template tpl = new Template(
                shape,
                family,
                pointSize,
                img.getWidth(),
                img.getHeight(),
                keyPoints,
                slimBounds);

        // Add specific anchor points, if any
        addAnchors(tpl, slimBounds);

        // Store a copy on disk for visual check?
        if (constants.saveTemplates.isSet()) {
            BufferedImage output = buildDecoratedImage(tpl, img);
            ImageUtil.saveOnDisk(output, "templates-" + family + "-" + pointSize, shape.name());
        }

        logger.debug("{} size:{} \n{}", shape, pointSize, tpl);

        return tpl;
    }

    //------------//
    // getCatalog //
    //------------//
    /**
     * Report the template catalog dedicated to the provided pointSize.
     *
     * @param family    the MusicFont family
     * @param pointSize provided point size
     * @return the catalog of all templates for the point size value
     */
    public Catalog getCatalog (MusicFamily family,
                               int pointSize)
    {
        Map<Integer, Catalog> familyCatalogs = catalogs.get(family);
        Catalog catalog = null;

        if (familyCatalogs == null || (catalog = familyCatalogs.get(pointSize)) == null) {
            synchronized (catalogs) {
                familyCatalogs = catalogs.get(family);

                if (familyCatalogs == null) {
                    catalogs.put(family, familyCatalogs = new TreeMap<>());
                }

                catalog = familyCatalogs.get(pointSize);

                if (catalog == null) {
                    familyCatalogs.put(pointSize, catalog = new Catalog(family, pointSize));
                }
            }
        }

        return catalog;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------//
    // addAnchors //
    //------------//
    /**
     * Add specific anchors to the provided template.
     * <p>
     * All templates get basic anchors at construction time, but some may need additional anchors.
     *
     * @param template the template to populate
     * @param slimBox  the slim symbol bounds within template
     */
    private static void addAnchors (Template template,
                                    Rectangle slimBox)
    {
        final Point2D center = GeoUtil.center2D(slimBox);

        // Define common basic anchors
        template.putOffset(Anchor.CENTER, center.getX(), center.getY());
        template.putOffset(Anchor.MIDDLE_LEFT, slimBox.x, center.getY());
        template.putOffset(Anchor.MIDDLE_RIGHT, slimBox.x + slimBox.width, center.getY());

        // Stem-less heads (i.e. whole's, breve's) are not concerned further
        if (ShapeSet.StemLessHeads.contains(template.getShape())) {
            return;
        }

        // Add anchors for potential stems on left and right sides
        // NOTA: dx is negative inside head bounds and positive outside
        final double dx = constants.stemDx.getValue() * slimBox.width;
        final double left = slimBox.x - dx;
        final double right = slimBox.x + slimBox.width + dx;
        final Shape shape = template.getShape();

        template.putOffset(Anchor.TOP_LEFT_STEM, left, getTop(shape, slimBox, LEFT));
        template.putOffset(Anchor.LEFT_STEM, left, center.getY());
        template.putOffset(Anchor.BOTTOM_LEFT_STEM, left, getBottom(shape, slimBox, LEFT));

        template.putOffset(Anchor.TOP_RIGHT_STEM, right, getTop(shape, slimBox, RIGHT));
        template.putOffset(Anchor.RIGHT_STEM, right, center.getY());
        template.putOffset(Anchor.BOTTOM_RIGHT_STEM, right, getBottom(shape, slimBox, RIGHT));
    }

    //----------//
    // addHoles //
    //----------//
    /**
     * Background pixels inside a given shape must be recognized as such.
     * <p>
     * Such pixels are marked with a specific color (pink foreground) so that the template can
     * measure their distance to (black) foreground.
     *
     * @param shape given shape
     * @param img   the source image
     * @param box   bounds of symbol relative to image
     */
    private static void addHoles (Shape shape,
                                  BufferedImage img,
                                  Rectangle box)
    {
        // Identify holes
        final List<Point> holeSeeds = new ArrayList<>();

        switch (shape) {
            case WHOLE_NOTE_CIRCLE_X, NOTEHEAD_CIRCLE_X_VOID, NOTEHEAD_CIRCLE_X -> {
                // 4 holes on vertical and on horizontal axes
                holeSeeds.add(new Point(box.x + (box.width / 4), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + (3 * box.width / 4), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 4)));
                holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (3 * box.height / 4)));
            }
            case BREVE_CIRCLE_X -> {
                // 4 holes on vertical and on horizontal axes
                holeSeeds.add(new Point(box.x + ((3 * box.width) / 8), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + ((5 * box.width) / 8), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 4)));
                holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (3 * box.height / 4)));
            }
            case BREVE_CROSS -> {
                // 3 holes on horizontal axis
                holeSeeds.add(new Point(box.x + (box.width / 4), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 2)));
                holeSeeds.add(new Point(box.x + (3 * box.width / 4), box.y + (box.height / 2)));
            }
            default ->
                    // Just one hole in the symbol center
                    holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 2)));
        }

        // Fill the holes if any with HOLE color
        FloodFiller floodFiller = new FloodFiller(img);

        for (Point seed : holeSeeds) {
            // Background (BACK) -> interior background (HOLE)
            // Hole seeds are very coarse with low interline value, so try points nearby
            Neighborhood:
            for (int iy = -1; iy <= 1; iy++) {
                for (int ix = -1; ix <= 1; ix++) {
                    if (isBackground(img, seed.x + ix, seed.y + iy)) {
                        floodFiller.fill(seed.x + ix, seed.y + iy, BACK, HOLE);

                        break Neighborhood;
                    }
                }
            }
        }
    }

    //-------------//
    // adjustHoles //
    //------------//
    /**
     * Correct any hole pixel left over.
     *
     * @param img the source image
     * @param box bounds of symbol relative to image
     */
    private static void adjustHoles (BufferedImage img,
                                     Rectangle box)
    {
        final FloodFiller floodFiller = new FloodFiller(img);

        for (int iy = box.y + 1, iyBreak = box.y + box.height - 1; iy < iyBreak; iy++) {
            for (int ix = box.x + 1, ixBreak = box.x + box.width - 1; ix < ixBreak; ix++) {
                if (isBackground(img, ix, iy)) {
                    floodFiller.adjust(ix, iy, HOLE);
                }
            }
        }
    }

    //----------//
    // binarize //
    //----------//
    /**
     * Use only fully black or fully transparent pixels.
     *
     * @param img       the source image
     * @param threshold alpha level to separate relevant from irrelevant pixels
     */
    private static void binarize (BufferedImage img,
                                  int threshold)
    {
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                Color pix = new Color(img.getRGB(x, y), true);

                if (pix.getAlpha() >= threshold) {
                    Color color = new Color(pix.getRGB(), false);

                    if (color.getRed() >= threshold) {
                        img.setRGB(x, y, BACK);
                    } else {
                        img.setRGB(x, y, FORE);
                    }
                } else {
                    img.setRGB(x, y, BACK);
                }
            }
        }
    }

    //----------------//
    // buildKeyPoints //
    //----------------//
    /**
     * Build the collection of key points to be used for matching tests.
     * <p>
     * These are the locations where the image distance value will be checked against the recorded
     * template distance value.
     *
     * @param img       the template colored image
     * @param distances the template distances (extended on each direction)
     * @return the collection of key locations, with their corresponding distance value
     */
    private static List<PixelDistance> buildKeyPoints (BufferedImage img,
                                                       DistanceTable distances,
                                                       int pointSize)
    {
        final List<PixelDistance> keyPoints = new ArrayList<>();
        final int maxDist = maxRawDistanceFromSymbol(pointSize);

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                final int pix = img.getRGB(x, y);

                // We consider only relevant pixels
                if (pix != IRRELEVANT) {
                    // For hole, use *NEGATIVE* distance to nearest foreground
                    // For background, use distance to nearest foreground
                    // For foreground, distance to nearest foreground is 0 by definition
                    final int val = distances.getValue(x, y);
                    final int d = (pix == HOLE) ? (-val) : ((pix == BACK) ? val : 0);

                    if (d <= maxDist) {
                        keyPoints.add(new PixelDistance(x, y, d));
                    }
                }
            }
        }

        return keyPoints;
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
    private static DistanceTable computeDistances (BufferedImage img,
                                                   Shape shape)
    {
        // Retrieve foreground pixels
        final int width = img.getWidth();
        final int height = img.getHeight();
        final boolean[][] fore = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (img.getRGB(x, y) == FORE) {
                    fore[x][y] = true;
                }
            }
        }

        // Compute template distance transform
        final DistanceTable distances = new ChamferDistance.Short().compute(fore);

        if (logger.isTraceEnabled()) {
            TableUtil.dump(shape + "  distances", distances);
        }

        return distances;
    }

    //----------------------//
    // flagIrrelevantPixels //
    //----------------------//
    /**
     * Some pixels in (non-hole) background regions must be set as non relevant.
     * <p>
     * All exterior pixels where distance is larger than maxRawDistanceFromSymbol.
     *
     * @param img the image to modify
     */
    private static void flagIrrelevantPixels (BufferedImage img,
                                              DistanceTable distances,
                                              int pointSize)
    {
        final int maxDist = maxRawDistanceFromSymbol(pointSize);

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                if ((img.getRGB(x, y) == BACK) && (distances.getValue(x, y) > maxDist)) {
                    img.setRGB(x, y, IRRELEVANT);
                }
            }
        }
    }

    //-----------//
    // getBottom //
    //-----------//
    private static double getBottom (Shape shape,
                                     Rectangle slimBox,
                                     HorizontalSide hSide)
    {
        return switch (shape) {
            case NOTEHEAD_BLACK, NOTEHEAD_BLACK_SMALL, NOTEHEAD_VOID, NOTEHEAD_VOID_SMALL -> //
                    switch (hSide) {
                        case LEFT -> slimBox.y + slimBox.height * (1 + constants.stemDy.getValue());
                        case RIGHT -> slimBox.y + 0.5 * slimBox.height;
                    };
            case NOTEHEAD_CROSS -> //
                    switch (hSide) {
                        case LEFT -> slimBox.y + slimBox.height;
                        case RIGHT -> slimBox.y + (1 - 0.2) * slimBox.height;
                    };
            case NOTEHEAD_CROSS_VOID -> slimBox.y + slimBox.height;
            case NOTEHEAD_DIAMOND_FILLED, NOTEHEAD_DIAMOND_VOID -> slimBox.y + slimBox.height / 2.0;
            case NOTEHEAD_TRIANGLE_DOWN_FILLED, NOTEHEAD_TRIANGLE_DOWN_VOID -> slimBox.y;

            default -> slimBox.y + slimBox.height * (1 + constants.stemDy.getValue());
        };
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report this singleton instance.
     *
     * @return the TemplateFactory single instance
     */
    public static TemplateFactory getInstance ()
    {
        return INSTANCE;
    }

    //---------------//
    // getSlimBounds //
    //---------------//
    /**
     * Retrieve the slim bounds of symbol, relative to image.
     *
     * @param img    colored image
     * @param fatBox fat symbol bounds
     * @return slim symbol bounds
     */
    private static Rectangle getSlimBounds (BufferedImage img,
                                            Rectangle fatBox)
    {
        final int imgWidth = img.getWidth();
        final int imgHeight = img.getHeight();

        // Minimum count of foreground cells to include current row or column
        final int minCells = constants.minCellPerSide.getValue();

        // Symbol bounds taken as default values
        int x1 = fatBox.x;
        int x2 = (fatBox.x + fatBox.width) - 1;
        int y1 = fatBox.y;
        int y2 = (fatBox.y + fatBox.height) - 1;

        // West
        for (int x = 0; x < imgWidth; x++) {
            int n = 0;

            for (int y = 0; y < imgHeight; y++) {
                if (img.getRGB(x, y) == FORE) {
                    n++;
                }
            }

            if (n >= minCells) {
                x1 = x;

                break;
            }
        }

        // East
        for (int x = imgWidth - 1; x >= 0; x--) {
            int n = 0;

            for (int y = 0; y < imgHeight; y++) {
                if (img.getRGB(x, y) == FORE) {
                    n++;
                }
            }

            if (n >= minCells) {
                x2 = x;

                break;
            }
        }

        // North
        for (int y = 0; y < imgHeight; y++) {
            int n = 0;

            for (int x = 0; x < imgWidth; x++) {
                if (img.getRGB(x, y) == FORE) {
                    n++;
                }
            }

            if (n >= minCells) {
                y1 = y;

                break;
            }
        }

        // South
        for (int y = imgHeight - 1; y >= 0; y--) {
            int n = 0;

            for (int x = 0; x < imgWidth; x++) {
                if (img.getRGB(x, y) == FORE) {
                    n++;
                }
            }

            if (n >= minCells) {
                y2 = y;

                break;
            }
        }

        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    //--------//
    // getTop //
    //--------//
    private static double getTop (Shape shape,
                                  Rectangle slimBox,
                                  HorizontalSide hSide)
    {
        return switch (shape) {
            case NOTEHEAD_BLACK, NOTEHEAD_BLACK_SMALL, NOTEHEAD_VOID, NOTEHEAD_VOID_SMALL -> //
                    switch (hSide) {
                        case LEFT -> slimBox.y + 0.5 * slimBox.height;
                        case RIGHT -> slimBox.y - constants.stemDy.getValue() * slimBox.height;
                    };
            case NOTEHEAD_CROSS -> //
                    switch (hSide) {
                        case LEFT -> slimBox.y + 0.2 * slimBox.height;
                        case RIGHT -> slimBox.y;
                    };
            case NOTEHEAD_DIAMOND_FILLED, NOTEHEAD_DIAMOND_VOID -> //
                    slimBox.y + slimBox.height / 2.0;
            case NOTEHEAD_CROSS_VOID, NOTEHEAD_TRIANGLE_DOWN_FILLED, NOTEHEAD_TRIANGLE_DOWN_VOID -> //
                    slimBox.y;
            default -> slimBox.y - constants.stemDy.getValue() * slimBox.height;
        };
    }

    //--------------//
    // isBackground //
    //--------------//
    private static boolean isBackground (BufferedImage img,
                                         int x,
                                         int y)
    {
        if ((x >= 0) && (x < img.getWidth()) && (y >= 0) && (y < img.getHeight())) {
            return img.getRGB(x, y) == BACK;
        }

        return false;
    }

    //------//
    // main //
    //------//
    /**
     * An entry point to allow stand alone generation and visual check of a bunch of templates.
     *
     * @param args minimum and maximum pointSize values
     */
    public static void main (String... args)
    {
        final int min;
        final int max;

        if ((args == null) || (args.length == 0)) {
            min = max = constants.defaultPointSize.getValue();
        } else {
            min = Integer.decode(args[0]);
            max = (args.length > 1) ? Integer.decode(args[1]) : min;
        }

        logger.info("minPointSize:{} maxPointSize{}", min, max);

        MusicFont.populateAllSymbols();

        final TemplateFactory factory = TemplateFactory.getInstance();

        for (MusicFamily family : MusicFamily.values()) {
            for (int pointSize = min; pointSize <= max; pointSize++) {
                factory.getCatalog(family, pointSize);
            }
        }
    }

    //-----------------------//
    // maxDistanceFromSymbol //
    //-----------------------//
    /**
     * Report the maximum external distance from symbol foreground for a pixel to be considered.
     *
     * @return the (normalized) maximum distance
     */
    public static double maxDistanceFromSymbol ()
    {
        return constants.maxRawDistanceFromSymbol.getValue()
                / (double) ChamferDistance.DEFAULT_NORMALIZER;
    }

    //--------------------------//
    // maxRawDistanceFromSymbol //
    //--------------------------//
    /**
     * Report the maximum raw distance, adjusted according to provided pointSize,
     * from symbol foreground for a pixel to be considered.
     *
     * @param pointSize font point size
     * @return the (adjusted) maximum raw distance
     */
    private static int maxRawDistanceFromSymbol (int pointSize)
    {
        return (int) Math.rint(
                constants.maxRawDistanceFromSymbol.getValue() //
                        * ((double) pointSize / constants.defaultPointSize.getValue()));
    }

    //-------------------//
    // retrieveKeyPoints //
    //-------------------//
    public static List<PixelDistance> retrieveKeyPoints (Shape shape,
                                                         MusicFamily family,
                                                         int pointSize)
    {
        final MusicFont font = MusicFont.getMusicFont(family, pointSize);
        final TemplateSymbol symbol = new TemplateSymbol(shape, family);
        final Rectangle fatBounds = symbol.getFatBounds(font);
        final BufferedImage img = symbol.buildImage(font); // Gray pixels
        binarize(img, constants.binarizationThreshold.getValue()); // B&W pixels + IRRELEVANT

        return retrieveKeyPoints(shape, family, pointSize, img, fatBounds);
    }

    //-------------------//
    // retrieveKeyPoints //
    //-------------------//
    private static List<PixelDistance> retrieveKeyPoints (Shape shape,
                                                          MusicFamily family,
                                                          int pointSize,
                                                          BufferedImage img,
                                                          Rectangle fatBounds)
    {
        logger.debug("Building template keyPoints for {} {} {}", shape, family, pointSize);

        // Distances to foreground
        final DistanceTable distances = computeDistances(img, shape);

        // Add holes if any
        if (shapesWithHoles.contains(shape)) {
            addHoles(shape, img, fatBounds); // B=FORE & W=BACK + HOLE pixels

            // Safety check
            if (img.getRGB(0, 0) == HOLE) {
                logger.warn(
                        "*** UNUSABLE template *** for shape {} in family {} pointSize {}",
                        shape,
                        family,
                        pointSize);
            }
        }

        // Flag non-relevant pixels: B=FORE & W=BACK + HOLE pixels + IRRELEVANT pixels
        flagIrrelevantPixels(img, distances, pointSize);

        // Adjust holes if any
        if (shapesWithHoles.contains(shape)) {
            adjustHoles(img, fatBounds); // HOLE
        }

        // Generate key points for relevant pixels only (fore, holes or back)
        return buildKeyPoints(img, distances, pointSize);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Catalog //
    //---------//
    /**
     * Handles all templates for a given family and a given pointSize value.
     */
    public class Catalog
    {
        /** Selected music font family. */
        final MusicFamily family;

        /** Point size value for this catalog. */
        final int pointSize;

        /** Map of all templates for this catalog. */
        final Map<Shape, Template> templates = new EnumMap<>(Shape.class);

        /**
         * Create a <code>Catalog</code> object.
         *
         * @param family    the selected MusicFont family
         * @param pointSize provided pointSize value
         */
        public Catalog (MusicFamily family,
                        int pointSize)
        {
            this.family = family;
            this.pointSize = pointSize;
            buildAllTemplates();
        }

        //-------------------//
        // buildAllTemplates //
        //-------------------//
        private void buildAllTemplates ()
        {
            logger.debug(
                    "TemplateFactory building all head templates for {} {}",
                    family,
                    pointSize);

            for (Shape shape : ShapeSet.Heads) {
                templates.put(shape, buildTemplate(shape, family, pointSize));
            }
        }

        //-------------//
        // getTemplate //
        //-------------//
        /**
         * Report the template for the given shape.
         *
         * @param shape desired shape
         * @return the template (not null, since all have been constructed)
         */
        public Template getTemplate (Shape shape)
        {
            return templates.get(shape);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean prePopulateKeyPoints = new Constant.Boolean(
                false,
                "Should we populate keyPoints at template creation?");

        private final Constant.Boolean saveTemplates = new Constant.Boolean(
                false,
                "Should we save the templates images to disk?");

        private final Constant.Integer binarizationThreshold = new Constant.Integer(
                "pixel value",
                140,
                "Binarization threshold for building head templates");

        private final Constant.Ratio stemDx = new Constant.Ratio(
                -0.1,
                "(Ratio) abscissa of stem anchor WRT symbol width (negative for inside)");

        private final Constant.Ratio stemDy = new Constant.Ratio(
                -0.2, // -0.375,
                "(Ratio) ordinate of stem anchor WRT symbol height (negative for inside)");

        private final Constant.Integer magnificationRatio = new Constant.Integer(
                "ratio",
                50,
                "Magnification ratio of template images for visual check");

        private final Constant.Integer defaultPointSize = new Constant.Integer(
                "pointSize",
                80,
                "Default point size");

        private final Constant.Integer minCellPerSide = new Constant.Integer(
                "pixels",
                2,
                "Minimum number of foreground pixels for slim bounds");

        private final Constant.Integer maxRawDistanceFromSymbol = new Constant.Integer(
                "distance",
                8,
                "Maximum raw distance from symbol for a keypoint");
    }
}
