//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h a p e D e s c r i p t o r                                 //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.TableUtil;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.TemplateSymbol;
import org.audiveris.omr.util.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code ShapeDescriptor} handles all the templates for a shape at a given
 * pointSize value.
 * <p>
 * Today there is just a single template per ShapeDescriptor.
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
public class ShapeDescriptor
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeDescriptor.class);

    /** All shapes with hole(s). */
    private static final EnumSet shapesWithHoles = EnumSet.of(
            NOTEHEAD_VOID,
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

    private final Shape shape;

    private final int pointSize;

    private final Template template;

    /** Symbol width. */
    private int width = -1;

    /** Symbol height. */
    private int height = -1;

    /**
     * Creates a new ShapeDescriptor object.
     *
     * @param shape     the described shape
     * @param pointSize precise scaling value
     */
    public ShapeDescriptor (Shape shape,
                            int pointSize)
    {
        this.shape = shape;
        this.pointSize = pointSize;

        template = createTemplate(shape, pointSize);
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Try the relevant templates at specified location and report best distance found.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    location WRT template
     * @param distances table of distances
     * @return the best distance found
     */
    public double evaluate (int x,
                            int y,
                            Anchor anchor,
                            DistanceTable distances)
    {
        return template.evaluate(x, y, anchor, distances);
    }

    //--------------//
    // evaluateHole //
    //--------------//
    /**
     * Evaluate the ratio of actual white pixels in expected hole.
     *
     * @param x         location abscissa
     * @param y         location ordinate
     * @param anchor    location WRT template
     * @param distances table of distances
     * @return the best distance found
     */
    public double evaluateHole (int x,
                                int y,
                                Anchor anchor,
                                DistanceTable distances)
    {
        return template.evaluateHole(x, y, anchor, distances);
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the descriptor bounds knowing the symbol box.
     *
     * @param symBox the symbol box
     * @return the bounds of containing descriptor
     */
    public Rectangle getBounds (Rectangle symBox)
    {
        return template.getBounds(symBox);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report symbol height.
     *
     * @return symbol height
     */
    public int getHeight ()
    {
        return height;
    }

    //-----------//
    // getOffset //
    //-----------//
    /**
     * Report the offset for desired anchor.
     *
     * @param anchor desired anchor
     * @return point offset
     */
    public Point getOffset (Anchored.Anchor anchor)
    {
        return template.getOffset(anchor);
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the related shape.
     *
     * @return related shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //-------------------//
    // getSymbolBoundsAt //
    //-------------------//
    /**
     * Report the symbol bounds, which may be smaller than the template bounds, because
     * of margins.
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor reference for (x, y)
     * @return the template bounds
     */
    public Rectangle getSymbolBoundsAt (int x,
                                        int y,
                                        Anchored.Anchor anchor)
    {
        return template.getSymbolBoundsAt(x, y, anchor);
    }

    //-------------//
    // getTemplate //
    //-------------//
    /**
     * Report the related template.
     *
     * @return related template
     */
    public Template getTemplate ()
    {
        return template;
    }

    //---------------------//
    // getTemplateBoundsAt //
    //---------------------//
    /**
     * Report the template bounds, which may be larger than the symbol bounds, because
     * of margins.
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor reference for (x, y)
     * @return the template bounds
     */
    public Rectangle getTemplateBoundsAt (int x,
                                          int y,
                                          Anchored.Anchor anchor)
    {
        return template.getBoundsAt(x, y, anchor);
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the symbol width.
     *
     * @return symbol width
     */
    public int getWidth ()
    {
        return width;
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
        sb.append(pointSize);
        sb.append("(").append(width).append("x").append(height).append(")");

        return sb.toString();
    }

    //------------//
    // addAnchors //
    //------------//
    /**
     * Add specific anchors to the template.
     * All templates get basic anchors at construction time, but some may need additional anchors.
     *
     * @param template the template to populate
     * @param img      the colored image
     */
    private void addAnchors (Template template,
                             BufferedImage img)
    {
        // Realistic symbol bounds (relative to template origin)
        final Rectangle sym = realisticBounds(template.getSymbolBounds(), img);
        final Point center = GeoUtil.centerOf(sym);

        // Define common basic anchors
        template.addAnchor(Anchor.CENTER, center.x, center.y);
        template.addAnchor(Anchor.MIDDLE_LEFT, sym.x, center.y);
        template.addAnchor(Anchor.MIDDLE_RIGHT, (sym.x + sym.width) - 1, center.y);

        // WHOLE_NOTE & WHOLE_NOTE_SMALL are not concerned further
        if (ShapeSet.StemLessHeads.contains(template.getShape())) {
            return;
        }

        // Add anchors for potential stems on left and right sides
        final int dx = (int) Math.rint(constants.stemDx.getValue() * sym.width);
        final int left = sym.x + dx;
        final int right = (sym.x + sym.width) - dx - 1;

        final int dy = (template.getShape() == Shape.NOTEHEAD_CROSS) ? 0
                : (int) Math.rint(constants.stemDy.getValue() * sym.height);
        final int top = sym.y + dy;
        final int bottom = (sym.y + sym.height) - dy - 1;

        ///template.addAnchor(Anchor.TOP_LEFT_STEM, left, center.y);
        template.addAnchor(Anchor.LEFT_STEM, left, center.y);
        template.addAnchor(Anchor.BOTTOM_LEFT_STEM, left, bottom);

        template.addAnchor(Anchor.TOP_RIGHT_STEM, right, top);
        template.addAnchor(Anchor.RIGHT_STEM, right, center.y);
        ///template.addAnchor(Anchor.BOTTOM_RIGHT_STEM, right, center.y);
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
        if (shapesWithHoles.contains(shape)) {
            // Identify holes
            final List<Point> holeSeeds = new ArrayList<>();

            // We have no ledger, just a big hole in the symbol center
            holeSeeds.add(new Point(box.x + (box.width / 2), box.y + (box.height / 2)));

            // Fill the holes if any with HOLE color
            FloodFiller floodFiller = new FloodFiller(img);

            for (Point seed : holeSeeds) {
                // Background (BACK) -> interior background (HOLE)
                // Hole seeds are very coarse with low interline value, so try points nearby
                Neiborhood:
                for (int iy = 0; iy <= 1; iy++) {
                    for (int ix = 0; ix <= 1; ix++) {
                        if (isBackground(img, seed.x + ix, seed.y + iy)) {
                            floodFiller.fill(seed.x + ix, seed.y + iy, BACK, HOLE);

                            break Neiborhood;
                        }
                    }
                }
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
                    img.setRGB(x, y, IRRELEVANT);
                }
            }
        }
    }

    //----------------//
    // createTemplate //
    //----------------//
    /**
     * Build a template for desired shape and size, based on MusicFont.
     *
     * @param shape     shape of the template
     * @param pointSize precise scaling for font
     * @return the brand new template
     */
    private Template createTemplate (Shape shape,
                                     int pointSize)
    {
        int interline = (pointSize + 2) / 4; // Approximate value
        MusicFont font = MusicFont.getPointFont(pointSize, interline);

        // Get symbol image painted on template rectangle
        final TemplateSymbol symbol = new TemplateSymbol(shape, getCode(shape));
        final BufferedImage img = symbol.buildImage(font);
        width = img.getWidth();
        height = img.getHeight();

        binarize(img, 175);

        // Distances to foreground
        final DistanceTable distances = computeDistances(img, shape);

        // Add holes if any
        final Rectangle symbolBounds = symbol.getSymbolBounds(font);
        addHoles(img, symbolBounds);

        // Flag non-relevant pixels
        flagIrrelevantPixels(img, distances);

        // Generate key points for relevant pixels (fore, holes or back)
        final List<PixelDistance> keyPoints = getKeyPoints(img, distances);

        // Generate the template instance
        Template tpl = new Template(
                shape,
                pointSize,
                symbol,
                width,
                height,
                keyPoints,
                symbolBounds);

        // Add specific anchor points, if any
        addAnchors(tpl, img);

        // Store a copy on disk for visual check?
        if (constants.keepTemplates.isSet()) {
            BufferedImage output = tpl.buildDecoratedImage(img);
            ImageUtil.saveOnDisk(output, shape + ".tpl" + pointSize);
        }

        return tpl;
    }

    //----------------------//
    // flagIrrelevantPixels //
    //----------------------//
    /**
     * Some pixels in (non-hole) background regions must be set as non relevant.
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
        Set<Point> borders = getBorders(img);

        // Then, extend each border pixel in the 4 directions as long as the
        // distance read for the pixel increases.
        Table extensions = getExtensions(borders, img, distances);

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

    //------------//
    // getBorders //
    //------------//
    private Set<Point> getBorders (BufferedImage img)
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

    //---------------//
    // getExtensions //
    //---------------//
    private Table getExtensions (Set<Point> borders,
                                 BufferedImage img,
                                 DistanceTable distances)
    {
        Table ext = new Table.UnsignedByte(img.getWidth(), img.getHeight());
        ext.fill(0);

        final int w = img.getWidth();
        final int h = img.getHeight();

        for (Point p : borders) {
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
    private boolean isBackground (BufferedImage img,
                                  int x,
                                  int y)
    {
        if ((x >= 0) && (x < img.getWidth()) && (y >= 0) && (y < img.getHeight())) {
            return img.getRGB(x, y) == BACK;
        }

        return false;
    }

    //-----------------//
    // realisticBounds //
    //-----------------//
    /**
     * Retrieve the realistic bounds of the symbol, relative to image.
     *
     * @param img image centered on symbol
     * @return symbol relative rectangle
     */
    private Rectangle realisticBounds (Rectangle sym,
                                       BufferedImage img)
    {
        final int imgWidth = img.getWidth();
        final int imgHeight = img.getHeight();
        final int minCells = 2;

        int x1 = sym.x;
        int x2 = (sym.x + sym.width) - 1;
        int y1 = sym.y;
        int y2 = (sym.y + sym.height) - 1;

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

    //------//
    // main //
    //------//
    /**
     * An entry point to allow generation and visual check of a bunch of templates.
     *
     * @param args minimum and maximum interline values
     */
    public static void main (String... args)
    {
        if ((args == null) || (args.length == 0)) {
            logger.warn("Expected args: minInterline maxInterline");

            return;
        }

        final int min = Integer.decode(args[0]);
        final int max = (args.length > 1) ? Integer.decode(args[1]) : min;

        for (int interline = min; interline <= max; interline++) {
            for (Shape shape : ShapeSet.getTemplateNotes(null)) {
                new ShapeDescriptor(shape, interline);
            }
        }
    }

    //------------------//
    // computeDistances //
    //------------------//
    /**
     * Compute all distances to nearest foreground pixel.
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

    //--------------//
    // getKeyPoints //
    //--------------//
    /**
     * Build the collection of key points to be used for matching tests.
     * These are the locations where the image distance value will be checked against the recorded
     * template distance value.
     *
     * @param img       the template source image
     * @param distances the template distances (extended on each direction)
     * @return the collection of key locations, with their corresponding distance value
     */
    private static List<PixelDistance> getKeyPoints (BufferedImage img,
                                                     DistanceTable distances)
    {
        List<PixelDistance> keyPoints = new ArrayList<>();
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                int pix = img.getRGB(x, y);

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean keepTemplates = new Constant.Boolean(
                false,
                "Should we keep the templates images?");

        private final Constant.Ratio stemDx = new Constant.Ratio(
                0.05,
                "(Ratio) abscissa of stem anchor WRT symbol width");

        private final Constant.Ratio stemDy = new Constant.Ratio(
                0.375, ///0.25,
                "(Ratio) ordinate of stem anchor WRT symbol height");
    }
}
