//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e F a c t o r y                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.BasicStroke;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.TableUtil;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.NOTEHEAD_VOID;
import static org.audiveris.omr.glyph.Shape.NOTEHEAD_VOID_SMALL;
import static org.audiveris.omr.glyph.Shape.WHOLE_NOTE;
import static org.audiveris.omr.glyph.Shape.WHOLE_NOTE_SMALL;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.sheet.ui.TemplateView;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.symbol.TemplateSymbol;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code TemplateFactory} builds needed instances of {@link Template} class
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
    private static final EnumSet shapesWithHoles = EnumSet.of(NOTEHEAD_VOID,
                                                              NOTEHEAD_VOID_SMALL,
                                                              WHOLE_NOTE,
                                                              WHOLE_NOTE_SMALL);

    /** Color for foreground pixels. */
    private static final int FORE = Color.BLACK.getRGB();

    /** Color for background pixels. */
    private static final int BACK = Color.RED.getRGB();

    /** Color for hole pixels. */
    private static final int HOLE = Color.PINK.getRGB();

    /** Color for irrelevant pixels. */
    private static final int IRRELEVANT = new Color(0, 0, 0, 0).getRGB(); // Fully transparent

    //~ Instance fields ----------------------------------------------------------------------------
    /** All catalogs allocated so far, mapped by point size. */
    private final Map<Integer, Catalog> catalogs;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * (Private) Creates the singleton object.
     */
    private TemplateFactory ()
    {
        catalogs = new TreeMap<>();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // getCatalog //
    //------------//
    /**
     * Report the template catalog dedicated to the provided pointSize.
     *
     * @param pointSize provided point size
     * @return the catalog of all templates for the point size value
     */
    public Catalog getCatalog (int pointSize)
    {
        Catalog catalog = catalogs.get(pointSize);

        if (catalog == null) {
            synchronized (catalogs) {
                catalog = catalogs.get(pointSize);

                if (catalog == null) {
                    catalogs.put(pointSize, catalog = new Catalog(pointSize));
                }
            }
        }

        return catalog;
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

    //------//
    // main //
    //------//
    /**
     * An entry point to allow generation and visual check of a bunch of templates.
     *
     * @param args minimum and maximum pointSize values
     */
    public static void main (String... args)
    {
        final int min;
        final int max;

        if ((args == null) || (args.length == 0)) {
            min = max = constants.defaultDecoratedPointSize.getValue();
        } else {
            min = Integer.decode(args[0]);
            max = (args.length > 1) ? Integer.decode(args[1]) : min;
        }

        logger.info("minPointSize:{} maxPointSize{}", min, max);

        final TemplateFactory factory = TemplateFactory.getInstance();

        for (int pointSize = min; pointSize <= max; pointSize++) {
            factory.getCatalog(pointSize);
        }
    }

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
    private void addAnchors (Template template,
                             Rectangle slimBox)
    {
        final Point2D center = GeoUtil.center2D(slimBox);

        // Define common basic anchors
        template.putOffset(Anchor.CENTER, center.getX(), center.getY());
        template.putOffset(Anchor.MIDDLE_LEFT, slimBox.x, center.getY());
        template.putOffset(Anchor.MIDDLE_RIGHT, slimBox.x + slimBox.width, center.getY());

        // WHOLE_NOTE & WHOLE_NOTE_SMALL are not concerned further
        if (ShapeSet.StemLessHeads.contains(template.getShape())) {
            return;
        }

        // Add anchors for potential stems on left and right sides
        // NOTA: dx is negative inside head bounds and positive outside
        final double dx = constants.stemDx.getValue() * slimBox.width;
        final double left = slimBox.x - dx;
        final double right = slimBox.x + slimBox.width + dx;

        // NOTA: dy is negative inside head bounds and positive outside
        final double dy = (template.getShape() == Shape.NOTEHEAD_CROSS) ? 0
                : (constants.stemDy.getValue() * slimBox.height);
        final double top = slimBox.y - dy;
        final double bottom = slimBox.y + slimBox.height + dy;

        template.putOffset(Anchor.LEFT_STEM, left, center.getY());
        template.putOffset(Anchor.BOTTOM_LEFT_STEM, left, bottom);

        template.putOffset(Anchor.TOP_RIGHT_STEM, right, top);
        template.putOffset(Anchor.RIGHT_STEM, right, center.getY());
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
     * @param img the source image
     * @param box bounds of symbol relative to image
     */
    private void addHoles (BufferedImage img,
                           Rectangle box)
    {
        // Identify holes
        final List<Point> holeSeeds = new ArrayList<>();

        // We have no ledger, just a big hole in the symbol center
        holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 2)));

        // Fill the holes if any with HOLE color
        FloodFiller floodFiller = new FloodFiller(img);

        for (Point seed : holeSeeds) {
            // Background (BACK) -> interior background (HOLE)
            // Hole seeds are very coarse with low interline value, so try points nearby
            Neighborhood:
            for (int iy = 0; iy <= 1; iy++) {
                for (int ix = 0; ix <= 1; ix++) {
                    if (isBackground(img, seed.x + ix, seed.y + iy)) {
                        floodFiller.fill(seed.x + ix, seed.y + iy, BACK, HOLE);

                        break Neighborhood;
                    }
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
    private void binarize (BufferedImage img,
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
     * These are the locations where the image distance value will be checked against the recorded
     * template distance value.
     *
     * @param img       the template colored image
     * @param distances the template distances (extended on each direction)
     * @return the collection of key locations, with their corresponding distance value
     */
    private static List<PixelDistance> buildKeyPoints (BufferedImage img,
                                                       DistanceTable distances)
    {
        final List<PixelDistance> keyPoints = new ArrayList<>();

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                final int pix = img.getRGB(x, y);

                // We consider only relevant pixels
                if (pix != IRRELEVANT) {
                    // For hole, use *NEGATIVE* dist to nearest foreground
                    // For background, use dist to nearest foreground
                    // For foreground, dist to nearest foreground is 0 by definition
                    final int val = distances.getValue(x, y);
                    final int d = (pix == HOLE) ? (-val) : ((pix == BACK) ? val : 0);
                    keyPoints.add(new PixelDistance(x, y, d));
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
     * <p>
     * For this we work as if there was a foreground rectangle right around the image.
     * Similarly, the non-relevant pixels are assumed to be foreground.
     * This is to allow the detection of reliable background key points.
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
        final boolean[][] fore = new boolean[width + 2][height + 2];

        // Fill with img foreground pixels and irrelevant pixels
        for (int y = 1; y < (height + 1); y++) {
            for (int x = 1; x < (width + 1); x++) {
                Color pix = new Color(img.getRGB(x - 1, y - 1), true);

                if (pix.equals(Color.BLACK) || (pix.getAlpha() == 0)) {
                    fore[x][y] = true;
                }
            }
        }

        // Surround with a rectangle of foreground pixels
        for (int y = 0; y < (height + 2); y++) {
            fore[0][y] = true;
            fore[width + 1][y] = true;
        }

        for (int x = 0; x < (width + 2); x++) {
            fore[x][0] = true;
            fore[x][height + 1] = true;
        }

        // Compute template distance transform
        final DistanceTable distances = new ChamferDistance.Short().compute(fore);

        if (logger.isDebugEnabled()) {
            TableUtil.dump(shape + "  distances", distances);
        }

        // Trim the distance table of its surrounding rectangle
        final Rectangle roi = new Rectangle(1, 1, width, height);
        final DistanceTable trimmed = (DistanceTable) distances.getView(roi);

        return trimmed;
    }

    //---------------------//
    // buildDecoratedImage //
    //---------------------//
    /**
     * For easy visual checking, build a magnified template image with decorations.
     * <p>
     * TODO: improve this method with g.setTransform() instead of scaling each and every item!
     *
     * @param src source colored image
     * @param tpl the template at hand
     * @return decorated magnified image
     */
    private BufferedImage buildDecoratedImage (Template tpl,
                                               BufferedImage src
    )
    {
        final int r = constants.magnificationRatio.getValue(); // (rather arbitrary ratio)
        final int width = src.getWidth();
        final int height = src.getHeight();
        final Rectangle slim = tpl.getSlimBounds();

        TextFont textFont = new TextFont("SansSerif", Font.PLAIN, (int) Math.round(r / 3.0));

        BufferedImage img = new BufferedImage(width * r, height * r, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width * r, height * r);

        // Draw template
        final AffineTransform at = AffineTransform.getScaleInstance(r, r);
        g.drawImage(src, at, null);

        // Paint shape symbol
        Composite oldComposite = g.getComposite();
        g.setComposite(TemplateView.templateComposite);
        MusicFont musicFont = MusicFont.getPointFont(r * tpl.getPointSize(), 0);
        final Point2D center = GeoUtil.center2D(slim);
        center.setLocation(r * center.getX(), r * center.getY());
        ShapeSymbol symbol = Symbols.getSymbol(tpl.getShape());
        symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
        g.setComposite(oldComposite);

        // Draw distances
        for (PixelDistance pix : tpl.getKeyPoints()) {
            int d = (int) Math.round(pix.d);

            if (d != 0) {
                g.drawRect(pix.x * r, pix.y * r, r, r);

                String str = Integer.toString(d);
                TextLayout layout = textFont.layout(str);
                Point2D location = new Point2D.Double(r * (pix.x + 0.5), r * (pix.y + 0.5));
                OmrFont.paint(g, layout, location, Alignment.AREA_CENTER);
            }
        }

        g.setStroke(new BasicStroke(3f));
        g.setColor(Color.GREEN);

        // Draw trimmed bounds
        g.drawRect(r * slim.x, r * slim.y, r * slim.width, r * slim.height);

        // Anchor Point locations, shown as rounded rectangles
        for (Entry<Anchor, Point2D> entry : tpl.getOffsets().entrySet()) {
            final Anchor anchor = entry.getKey();
            final Point pt = tpl.getOffset(anchor);
            g.drawRoundRect(pt.x * r, pt.y * r, r, r, r / 2, r / 2);
        }

        // Anchor Point2D locations, shown as small circles with anchor abbreviation
        g.setStroke(new BasicStroke(1f));
        g.setXORMode(Color.BLACK);
        for (Entry<Anchor, Point2D> entry : tpl.getOffsets().entrySet()) {
            final Anchor anchor = entry.getKey();
            final Point2D pt2D = entry.getValue();
            g.draw(new Ellipse2D.Double(r * (pt2D.getX() - 0.25), r * (pt2D.getY() - 0.25),
                                        r * 0.5, r * 0.5));

            final String str = anchor.abbreviation().toLowerCase(Locale.US);
            final TextLayout layout = textFont.layout(str);
            final Point2D location = new Point2D.Double(r * pt2D.getX(), r * pt2D.getY());
            OmrFont.paint(g, layout, location, Alignment.AREA_CENTER);
        }

        g.dispose();

        // Put everything within a frame with x and y values
        BufferedImage frm = new BufferedImage(
                (width + 2) * r, // + 2 to cope with ordinate display on left and right
                (height + 2) * r, // +2 to cope with abscissa display on top and bottom
                BufferedImage.TYPE_INT_RGB);
        g = frm.createGraphics();

        // Fill frame background
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, frm.getWidth(), frm.getHeight());

        g.drawImage(img, null, r, r);

        // Draw coordinate values
        g.setColor(Color.BLACK);

        for (int x = 0; x <= width; x++) {
            String str = Integer.toString(x);
            TextLayout layout = textFont.layout(str);
            Point north = new Point(r * (1 + x), (r / 2));
            OmrFont.paint(g, layout, north, Alignment.AREA_CENTER);

            Point south = new Point(r * (1 + x), frm.getHeight() - (r / 2));
            OmrFont.paint(g, layout, south, Alignment.AREA_CENTER);
        }

        for (int y = 0; y <= height; y++) {
            String str = Integer.toString(y);
            TextLayout layout = textFont.layout(str);
            Point west = new Point((r / 2), r * (1 + y));
            OmrFont.paint(g, layout, west, Alignment.AREA_CENTER);

            Point east = new Point(frm.getWidth() - (r / 2), r * (1 + y));
            OmrFont.paint(g, layout, east, Alignment.AREA_CENTER);
        }

        return frm;
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
                                    int pointSize)
    {
        int interline = (pointSize + 2) / 4; // Approximate value
        MusicFont font = MusicFont.getPointFont(pointSize, interline);

        // Get symbol image painted on template rectangle
        final TemplateSymbol symbol = new TemplateSymbol(shape, getCode(shape));
        final BufferedImage img = symbol.buildImage(font); // Gray pixels

        binarize(img, constants.binarizationThreshold.getValue()); // B&W pixels + IRRELEVANT

        // Distances to foreground
        final DistanceTable distances = computeDistances(img, shape);

        final Rectangle fatBounds = symbol.getFatBounds(font);

        // Add holes if any
        if (shapesWithHoles.contains(shape)) {
            addHoles(img, fatBounds); // B=FORE & W=BACK + HOLE pixels
        }

        // Flag non-relevant pixels
        flagIrrelevantPixels(img, distances); // B=FORE & W=BACK + HOLE pixels + IRRELEVANT pixels

        // Generate key points for relevant pixels only (fore, holes or back)
        final List<PixelDistance> keyPoints = buildKeyPoints(img, distances);

        final Rectangle slimBounds = getSlimBounds(img, fatBounds);

        // Generate the template instance
        final Template tpl = new Template(shape,
                                          pointSize,
                                          img.getWidth(),
                                          img.getHeight(),
                                          keyPoints,
                                          slimBounds);

        // Add specific anchor points, if any
        addAnchors(tpl, slimBounds);

        // Store a copy on disk for visual check?
        if (constants.keepTemplates.isSet()) {
            BufferedImage output = buildDecoratedImage(tpl, img);
            ImageUtil.saveOnDisk(output, shape + ".tpl-" + pointSize);
        }

        logger.debug("{} size:{} \n{}", shape, pointSize, tpl);

        return tpl;
    }

    //----------------------//
    // flagIrrelevantPixels //
    //----------------------//
    /**
     * Some pixels in (non-hole) background regions must be set as non relevant.
     * <p>
     * They are roughly the "exterior" half of these regions, where the distance to foreground would
     * be impacted by the presence of nearby stem or staff / ledger line.
     * Only the "interior" half of such region is relevant, for its distance to foreground is not
     * dependent upon the presence of stem or line.
     *
     * @param img the image to modify
     */
    private void flagIrrelevantPixels (BufferedImage img,
                                       DistanceTable distances)
    {
        // First browse from each foreground pixel in the 4 directions to first non-foreground pixel
        // If this pixel is relevant and non-hole, flag it as a border pixel.
        final Set<Point> borderPoints = getBorderPoints(img);

        // Then, extend each border pixel in the 4 directions as long as the
        // distance read for the pixel increases.
        final Table extensions = getExtensions(borderPoints, img, distances);

        // The border pixels and extensions compose the relevant part of (non-hole background) regions.
        // Flag the other (non-hole) background pixels as irrelevant
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                // Check (non-hole) background pixel
                if (img.getRGB(x, y) == BACK) {
                    if (extensions.getValue(x, y) != 1) {
                        img.setRGB(x, y, IRRELEVANT);
                    }
                }
            }
        }
    }

    //-----------------//
    // getBorderPoints //
    //-----------------//
    /**
     * Report the set of border points in provided image, that is any FORE pixel next to
     * a BACK pixel (HOLE pixels are not considered).
     *
     * @param img the provided image
     * @return the set of border points
     */
    private Set<Point> getBorderPoints (BufferedImage img)
    {
        final Set<Point> borders = new LinkedHashSet<>();

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                // Check foreground pixel
                if (img.getRGB(x, y) == FORE) {
                    // North
                    for (int ny = y - 1; ny >= 0; ny--) {
                        int pix = img.getRGB(x, ny);

                        if (pix != FORE) {
                            if (pix == BACK) {
                                borders.add(new Point(x, ny));
                            }

                            break;
                        }
                    }

                    // South
                    for (int ny = y + 1; ny < h; ny++) {
                        int pix = img.getRGB(x, ny);

                        if (pix != FORE) {
                            if (pix == BACK) {
                                borders.add(new Point(x, ny));
                            }

                            break;
                        }
                    }

                    // West
                    for (int nx = x - 1; nx >= 0; nx--) {
                        int pix = img.getRGB(nx, y);

                        if (pix != FORE) {
                            if (pix == BACK) {
                                borders.add(new Point(nx, y));
                            }

                            break;
                        }
                    }

                    // East
                    for (int nx = x + 1; nx < w; nx++) {
                        int pix = img.getRGB(nx, y);

                        if (pix != FORE) {
                            if (pix == BACK) {
                                borders.add(new Point(nx, y));
                            }

                            break;
                        }
                    }
                }
            }
        }

        return borders;
    }

    //---------//
    // getCode //
    //---------//
    private static int getCode (Shape shape)
    {
        switch (shape) {
        case NOTEHEAD_CROSS:
            return 192;

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

        logger.error(shape + " is not supported!");

        return 0;
    }

    //---------------//
    // getExtensions //
    //---------------//
    /**
     * Extend each border pixel in the 4 directions as long as the distance read for the
     * pixel increases.
     *
     * @param borderPoints all points detected to be on border
     * @param img          the colored image
     * @param distances    table of distances
     * @return the table (same dimension as image) filled with 1's at extension locations
     */
    private Table getExtensions (Set<Point> borderPoints,
                                 BufferedImage img,
                                 DistanceTable distances)
    {
        final Table ext = new Table.UnsignedByte(img.getWidth(), img.getHeight());
        ext.fill(0);

        final int w = img.getWidth();
        final int h = img.getHeight();

        for (Point p : borderPoints) {
            ext.setValue(p.x, p.y, 1);

            int dist = distances.getValue(p.x, p.y);

            // North
            for (int ny = p.y - 1; ny >= 0; ny--) {
                int d = distances.getValue(p.x, ny);

                if (d > dist) {
                    dist = d;
                    ext.setValue(p.x, ny, 1);
                } else {
                    break;
                }
            }

            // South
            dist = distances.getValue(p.x, p.y);

            for (int ny = p.y + 1; ny < h; ny++) {
                int d = distances.getValue(p.x, ny);

                if (d > dist) {
                    dist = d;
                    ext.setValue(p.x, ny, 1);
                } else {
                    break;
                }
            }

            // West
            dist = distances.getValue(p.x, p.y);

            for (int nx = p.x - 1; nx >= 0; nx--) {
                int d = distances.getValue(nx, p.y);

                if (d > dist) {
                    dist = d;
                    ext.setValue(nx, p.y, 1);
                } else {
                    break;
                }
            }

            // East
            dist = distances.getValue(p.x, p.y);

            for (int nx = p.x + 1; nx < w; nx++) {
                int d = distances.getValue(nx, p.y);

                if (d > dist) {
                    dist = d;
                    ext.setValue(nx, p.y, 1);
                } else {
                    break;
                }
            }
        }

        return ext;
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
        final int minCells = 2;

        // Symbol bounds taken as default values
        int x1 = fatBox.x;
        int x2 = fatBox.x + fatBox.width - 1;
        int y1 = fatBox.y;
        int y2 = fatBox.y + fatBox.height - 1;

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Catalog //
    //---------//
    /**
     * Handles all templates for a given pointSize value.
     */
    public class Catalog
    {

        /** Point size value for this catalog. */
        final int pointSize;

        /** Map of all templates for this catalog. */
        final Map<Shape, Template> templates = new EnumMap<>(Shape.class);

        /**
         * Create a {@code Catalog} object.
         *
         * @param pointSize provided pointSize value
         */
        public Catalog (int pointSize)
        {
            this.pointSize = pointSize;
            buildAllTemplates();
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

        //-------------------//
        // buildAllTemplates //
        //-------------------//
        private void buildAllTemplates ()
        {
            for (Shape shape : ShapeSet.getTemplateNotes(null)) {
                templates.put(shape, buildTemplate(shape, pointSize));
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean keepTemplates = new Constant.Boolean(
                false,
                "Should we save the templates images to disk?");

        private final Constant.Integer binarizationThreshold = new Constant.Integer(
                "pixel",
                140,
                "Binarization threshold for building head templates");

        private final Constant.Ratio stemDx = new Constant.Ratio(
                0.05,
                "(Ratio) abscissa of stem anchor WRT symbol width (negative for inside)");

        private final Constant.Ratio stemDy = new Constant.Ratio(
                -0.375,
                "(Ratio) ordinate of stem anchor WRT symbol height (negative for inside)");

        private final Constant.Integer magnificationRatio = new Constant.Integer(
                "ratio",
                50,
                "Magnification ratio of template images for visual check");

        private final Constant.Integer defaultDecoratedPointSize = new Constant.Integer(
                "pointSize",
                69,
                "Default point size used to generate decorated images");
    }
}
