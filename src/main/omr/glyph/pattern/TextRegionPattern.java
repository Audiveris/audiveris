//----------------------------------------------------------------------------//
//                                                                            //
//                     T e x t R e g i o n P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;
import omr.glyph.text.Language;
import omr.glyph.text.OcrLine;
import omr.glyph.text.Sentence;
import omr.glyph.text.TextInfo;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;
import omr.math.Population;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.LineInfo;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemBoundary.Side;
import omr.sheet.SystemInfo;

import omr.util.BrokenLine;
import omr.util.Implement;
import omr.util.WrappedBoolean;
import omr.util.XmlUtilities;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.*;

/**
 * Class {@code TextRegionPattern} directly uses OCR on regions around staves.
 *
 * <p>We define near-rectangular zones above and below staves, and on left and
 * right sides of the system. For each zone, all contained glyphs are merged
 * into one transient compound which is then handed over to the OCR utility.
 * The OcrLines returned by the OCR are used to retrieve real text portions.
 *
 * @author Hervé Bitteur
 */
public class TextRegionPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TextRegionPattern.class);

    //~ Instance fields --------------------------------------------------------

    /** The system contour box (just the union of staves) */
    private PixelRectangle systemBox;

    // Scale dependent constants
    private int glyphMinHeight;
    private int maxFontSize;
    private int smallXMargin;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // TextRegionPattern //
    //-------------------//
    /**
     * Creates a new TextRegionPattern object.
     * @param system the containing system
     */
    public TextRegionPattern (SystemInfo system)
    {
        super("TextRegion", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        // System contour box
        systemBox = new PixelRectangle(
            system.getLeft(),
            system.getTop(),
            system.getWidth(),
            system.getBottom() - system.getTop());

        int   successNb = 0;
        Scale scale = system.getScoreSystem()
                            .getScale();
        Sheet sheet = system.getSheet();

        glyphMinHeight = scale.toPixels(constants.glyphMinHeight);
        maxFontSize = scale.toPixels(constants.maxFontSize);
        smallXMargin = scale.toPixels(constants.smallXMargin);

        int             staffMarginAbove = scale.toPixels(
            constants.staffMarginAbove);
        int             staffMarginBelow = scale.toPixels(
            constants.staffMarginBelow);

        // Define the regions to handle
        final int       left = system.getLeft();
        final int       right = system.getRight();
        BrokenLine      topLine = system.getBoundary()
                                        .getLimit(Side.NORTH);
        BrokenLine      botLine = system.getBoundary()
                                        .getLimit(Side.SOUTH);
        List<StaffInfo> staves = system.getStaves();

        StaffInfo       firstStaff = staves.get(0);
        LineInfo        firstLine = firstStaff.getFirstLine();
        PixelPoint      nw = new PixelPoint(0, firstLine.yAt(0));
        PixelPoint      ne = new PixelPoint(
            sheet.getWidth(),
            firstLine.yAt(sheet.getWidth()));
        // Move up a bit
        nw.y -= staffMarginAbove;
        ne.y -= staffMarginAbove;

        StaffInfo  lastStaff = staves.get(staves.size() - 1);
        LineInfo   lastLine = lastStaff.getLastLine();
        PixelPoint sw = new PixelPoint(0, lastLine.yAt(0));
        PixelPoint se = new PixelPoint(
            sheet.getWidth(),
            lastLine.yAt(sheet.getWidth()));
        // Move down a bit
        sw.y += staffMarginBelow;
        se.y += staffMarginBelow;

        Polygon north = buildPolygon(topLine.getPoints(), ne, nw);
        Polygon south = buildPolygon(botLine.getPoints(), se, sw);
        Polygon west = buildPolygon(
            nw,
            new PixelPoint(left, firstLine.yAt(left)),
            new PixelPoint(left, lastLine.yAt(left)),
            sw);

        Polygon east = buildPolygon(
            new PixelPoint(right, firstLine.yAt(right)),
            ne,
            se,
            new PixelPoint(right, lastLine.yAt(right)));

        // Inspect each well-known region
        processRegion("North", north);
        processRegion("South", south);
        processRegion("West", west);
        processRegion("East", east);

        //        // Inspect each inter-staff region
        //        StaffInfo prevStaff = null;
        //
        //        for (StaffInfo staff : staves) {
        //            if (prevStaff != null) {
        //                LineInfo top = prevStaff.getLastLine();
        //                LineInfo bot = staff.getFirstLine();
        //                Polygon  region = buildPolygon(
        //                    new Point(left, top.yAt(left)),
        //                    new Point(right, top.yAt(right)),
        //                    new Point(right, bot.yAt(right)),
        //                    new Point(left, bot.yAt(left)));
        //                processRegion("Center", region);
        //            }
        //
        //            prevStaff = staff;
        //        }
        return successNb;
    }

    //--------------//
    // buildPolygon //
    //--------------//
    private Polygon buildPolygon (List<Point> list,
                                  Point... points)
    {
        List<Point> all = new ArrayList<Point>(list);
        all.addAll(Arrays.asList(points));

        return buildPolygon(all);
    }

    //--------------//
    // buildPolygon //
    //--------------//
    private Polygon buildPolygon (List<Point> list)
    {
        Polygon polygon = new Polygon();

        for (Point point : list) {
            polygon.addPoint(point.x, point.y);
        }

        return polygon;
    }

    //--------------//
    // buildPolygon //
    //--------------//
    private Polygon buildPolygon (Point... points)
    {
        return buildPolygon(Arrays.asList(points));
    }

    //---------//
    // callOCR //
    //---------//
    private OcrLine callOCR (Glyph  compound,
                             String language)
    {
        List<OcrLine> lines = Language.getOcr()
                                      .recognize(
            compound.getImage(),
            language,
            "g" + ".");

        // Debug
        if (logger.isFineEnabled()) {
            int i = 0;

            for (OcrLine ocrLine : lines) {
                i++;

                String         value = ocrLine.value;
                float          fontSize = ocrLine.fontSize;
                PixelRectangle box = ocrLine.getContourBox();

                if (logger.isFineEnabled()) {
                    logger.fine(
                        i + " " + box + " " + ocrLine.toString() + " w:" +
                        (box.width / (fontSize * value.length())) + " h:" +
                        (box.height / fontSize) + " aspect:" +
                        (((float) box.height * value.length()) / box.width));
                }
            }
        }

        // Tests on OCR results
        if (lines.size() < 1) {
            if (logger.isFineEnabled()) {
                logger.fine("No line found");
            }

            return null;
        }

        if (lines.size() > 1) {
            if (logger.isFineEnabled()) {
                logger.fine("More than 1 line found");
            }

            return null;
        }

        OcrLine ocrLine = lines.get(0);
        // Convert from glyph-based to absolute coordinates
        ocrLine.translate(
            compound.getContourBox().x,
            compound.getContourBox().y);

        String         value = ocrLine.value;
        float          fontSize = ocrLine.fontSize;
        PixelRectangle box = ocrLine.getContourBox();

        if (box.height == 0) {
            if (logger.isFineEnabled()) {
                logger.fine("OCR found nothing");
            }

            return null;
        }

        if (fontSize > maxFontSize) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Font size " + fontSize + " exceeds maximum " +
                    maxFontSize);
            }

            return null;
        }

        float        aspect = ((float) box.height * value.length()) / box.width;

        final double minAspect = constants.minAspect.getValue();

        if (aspect < minAspect) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Char aspect " + aspect + " lower than minimum " +
                    minAspect);
            }

            return null;
        }

        final double maxAspect = constants.maxAspect.getValue();

        if (aspect > maxAspect) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Char aspect " + aspect + " exceeds maximum " + maxAspect);
            }

            return null;
        }

        // All tests are OK
        return ocrLine;
    }

    //-----------//
    // checkText //
    //-----------//
    /**
     * Check whether the constructed blob is actually a line of text
     * @param blob the blob to check
     * @return true if OK
     */
    private boolean checkText (Blob blob)
    {
        String language = system.getScoreSystem()
                                .getScore()
                                .getLanguage();

        Glyph  compound = (blob.glyphs.size() > 1)
                          ? system.buildTransientCompound(blob.glyphs)
                          : blob.glyphs.get(0);

        // Check that this glyph is not forbidden as text
        Glyph original = system.getSheet()
                               .getVerticalLag()
                               .getOriginal(compound);

        if (original != null) {
            compound = original;
        }

        if (compound.isShapeForbidden(Shape.TEXT)) {
            if (logger.isFineEnabled()) {
                logger.fine("Shape TEXT blacklisted");
            }

            return false;
        }

        // Call the OCR?
        if (Sentence.useOCR()) {
            OcrLine ocrLine = null;

            if (compound.getTextInfo()
                        .getContent() == null) {
                ocrLine = callOCR(compound, language);

                if (ocrLine == null) {
                    return false;
                }

                // Record the OCR information
                TextInfo ti = compound.getTextInfo();
                ti.setOcrInfo(language, ocrLine);
                ti.setTextRole(ti.getTextRole());
            } else {
                ocrLine = compound.getTextInfo()
                                  .getOcrLine();
            }

            // Check this is not a tuplet
            if (ocrLine.value.equals("3") &&
                (compound.getShape() == Shape.TUPLET_THREE)) {
                if (logger.isFineEnabled()) {
                    logger.fine("This text is a tuplet 3");
                }

                return false;
            }

            if (ocrLine.value.equals("6") &&
                (compound.getShape() == Shape.TUPLET_SIX)) {
                if (logger.isFineEnabled()) {
                    logger.fine("This text is a tuplet 6");
                }

                return false;
            }

            // Check for abnormal characters
            WrappedBoolean stripped = new WrappedBoolean(false);
            XmlUtilities.stripNonValidXMLCharacters(ocrLine.value, stripped);

            if (stripped.isSet()) {
                system.getScoreSystem()
                      .addError(
                    blob.glyphs.get(0),
                    "Illegal character found in " + ocrLine.value);

                return false;
            }

            // OK
            compound = system.addGlyph(compound);
            system.computeGlyphFeatures(compound);
            compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Glyph#" + compound.getId() + "=>\"" + ocrLine.value +
                    "\"");
            }

            return true;
        } else {
            return true; // By default (with no OCR call...)
        }
    }

    //------------------//
    // filterCandidates //
    //------------------//
    private void filterCandidates (Set<Glyph> candidates)
    {
        // We remove candidates that are stuck to a stem that goes into a staff
        // Because these glyphs are not likely to be text items
        for (Iterator<Glyph> it = candidates.iterator(); it.hasNext();) {
            Glyph   glyph = it.next();
            Glyph[] stems = new Glyph[] {
                                glyph.getLeftStem(), glyph.getRightStem()
                            };

            for (Glyph stem : stems) {
                if ((stem != null) &&
                    stem.getContourBox()
                        .intersects(systemBox)) {
                    it.remove();

                    break;
                }
            }
        }
    }

    //-------------------//
    // insertSmallGlyphs //
    //-------------------//
    /**
     * Re-insert the small glyphs that had been left aside when initially
     * building the blobs.
     * @param smallGlyphs the collection of small glyphs to re-insert
     * @param blobs the collection of blobs to update
     */
    private void insertSmallGlyphs (List<Glyph> smallGlyphs,
                                    List<Blob>  blobs)
    {
        for (Glyph glyph : smallGlyphs) {
            if (logger.isFineEnabled()) {
                logger.fine("Trying to insert small glyph#" + glyph.getId());
            }

            // Look for a suitable blob
            for (Blob blob : blobs) {
                if (blob.tryToInsert(glyph)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Small glyph#" + glyph.getId() + " inserted into " +
                            blob);
                    }

                    break;
                }
            }
        }
    }

    //---------------//
    // processRegion //
    //---------------//
    private void processRegion (String  region,
                                Polygon polygon)
    {
        // Retrieve all system glyphs contained by the polygon, except those
        // stuck to a stem that goes into the staff.
        // Use glyph height to filter out glyphs too tall or too small
        // Sort them by abscissa
        // Organize them into blobs (line candidates)

        // Build one compound per blob and hand it over to OCR
        // Then retrieve the sections that are contained in char boxes
        // And make text glyphs out of these sections
        SortedSet<Glyph> glyphs = new TreeSet<Glyph>(Glyph.globalComparator);
        glyphs.addAll(Glyphs.lookupGlyphs(system.getGlyphs(), polygon));

        // Remove non candidates
        filterCandidates(glyphs);

        if (logger.isFineEnabled()) {
            logger.fine(
                "S#" + system.getId() + " Region " + region + " Polygon:" +
                polygon.getBounds() + " glyphs:" + glyphs.size());
        }

        if (glyphs.isEmpty()) {
            return;
        }

        // Populate blobs, incrementally
        List<Blob>  passedBlobs = new ArrayList<Blob>();
        List<Blob>  activeBlobs = new ArrayList<Blob>();
        List<Glyph> smallGlyphs = new ArrayList<Glyph>();
        int         blobIndex = 0;

        glyphLoop: 
        for (Glyph glyph : glyphs) {
            if (logger.isFineEnabled()) {
                logger.fine("Glyph#" + glyph.getId());
            }

            PixelRectangle gBox = glyph.getContourBox();

            // Discard glyphs whose height is obviously too small or too large
            if (gBox.height < glyphMinHeight) {
                if (logger.isFineEnabled()) {
                    logger.fine("Too small");
                }

                smallGlyphs.add(glyph);

                continue glyphLoop;
            }

            // Find the first compatible blob, if any
            for (ListIterator<Blob> it = activeBlobs.listIterator();
                 it.hasNext();) {
                Blob blob = it.next();

                if (blob.canAppend(glyph)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Adding to " + blob);
                    }

                    blob.append(glyph);

                    continue glyphLoop;
                } else if ((gBox.x - blob.getRight()) > blob.getMaxWordGap()) {
                    it.remove();
                    passedBlobs.add(blob);

                    if (logger.isFineEnabled()) {
                        logger.fine("Ending " + blob);
                    }
                }
            }

            // Here we have found no compatible blob, we create a specific one
            Blob blob = new Blob(++blobIndex);
            blob.append(glyph);

            if (logger.isFineEnabled()) {
                logger.fine("Created " + blob);
            }

            activeBlobs.add(blob);
        }

        // Terminate all blobs
        if (logger.isFineEnabled()) {
            for (Blob blob : activeBlobs) {
                logger.fine("Finishing " + blob);
            }
        }

        passedBlobs.addAll(activeBlobs);

        // Re-insert small glyphs into proper blobs
        if (logger.isFineEnabled()) {
            logger.fine("Small glyphs count: " + smallGlyphs.size());
        }

        insertSmallGlyphs(smallGlyphs, passedBlobs);

        for (Blob blob : passedBlobs) {
            checkText(blob);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction glyphMinHeight = new Scale.Fraction(
            0.8,
            "Minimum height for characters");
        Constant.Ratio maxWidthRatio = new Constant.Ratio(
            1.0,
            "Ratio for maximum horizontal gap versus character width");
        Constant.Ratio minOverlapRatio = new Constant.Ratio(
            0.5,
            "Ratio for minimum vertical overlap beween blob and glyph");
        Scale.Fraction maxFontSize = new Scale.Fraction(
            1.8,
            "Maximum value for text font size");
        Constant.Ratio minAspect = new Constant.Ratio(
            1.0,
            "Minimum aspect of chars (height / width)");
        Constant.Ratio maxAspect = new Constant.Ratio(
            3.0,
            "Maximum aspect of chars (height / width)");
        Scale.Fraction staffMarginAbove = new Scale.Fraction(
            1.0,
            "Minimum distance avove staff");
        Scale.Fraction staffMarginBelow = new Scale.Fraction(
            3.0,
            "Minimum distance below staff");
        Scale.Fraction smallXMargin = new Scale.Fraction(
            1.0,
            "Maximum abscissa gap for small glyphs");
        Constant.Ratio smallYRatio = new Constant.Ratio(
            1.0,
            "Maximum ordinate gap for small glyphs (as ratio of mean height)");
    }

    /**
     * Class {@code Blob} handles a growing sequence of glyphs that could form
     * one text line
     */
    private class Blob
    {
        //~ Instance fields ----------------------------------------------------

        // Id for debug
        final int         id;

        // Glyphs added so far
        final List<Glyph> glyphs = new ArrayList<Glyph>();

        // Most important lines for a text area
        final Line     average = new BasicLine();

        // Population of glyphs tops & glyphs bottoms
        Population     tops = new Population();
        Population     bottoms = new Population();

        // Lastest values
        Integer        blobTop;
        Integer        blobBottom;

        // Global blob contour
        PixelRectangle blobBox = null;

        // Median glyph width
        Integer medianWidth = null;

        // Median glyph height
        Integer medianHeight = null;

        //~ Constructors -------------------------------------------------------

        public Blob (int id)
        {
            this.id = id;
        }

        //~ Methods ------------------------------------------------------------

        public int getMaxWordGap ()
        {
            return (int) Math.rint(
                getMedianHeight() * constants.maxWidthRatio.getValue());
        }

        public void append (Glyph glyph)
        {
            // Include the glyph in our blob
            glyphs.add(glyph);

            // Incrementally extend the lines
            PixelRectangle gBox = glyph.getContourBox();
            int            top = gBox.y;
            int            bottom = top + gBox.height;

            // Adjust values
            switch (glyphs.size()) {
            case 1 :
                // Start with reasonable values & Fall through
                blobBox = new PixelRectangle(gBox);

            // Fall-through
            case 2 :
                blobBox = blobBox.union(gBox);
                // Cumulate tops & bottoms
                tops.includeValue(top);
                bottoms.includeValue(bottom);
                blobTop = blobBox.y;
                blobBottom = blobBox.y + blobBox.height;

                break;

            default :
                blobBox = blobBox.union(gBox);
                tops.includeValue(top);
                bottoms.includeValue(bottom);
                blobTop = (int) Math.rint(tops.getMeanValue());
                blobBottom = (int) Math.rint(bottoms.getMeanValue());

                break;
            }

            //
            medianHeight = medianWidth = null;

            Line gLine = ((Stick) glyph).getLine();
            average.includeLine(gLine);
        }

        /**
         * Check whether the provided glyph can be appended to the blob
         * @param glyph the candidate glyph
         * @return true if successful
         */
        public boolean canAppend (Glyph glyph)
        {
            PixelRectangle gBox = glyph.getContourBox();
            int            left = gBox.x;

            // Check abscissa gap
            int dx = left - getRight();
            int maxDx = getMaxWordGap();

            if (dx > maxDx) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "B#" + id + " Too far on right " + dx + " vs " + maxDx);
                }

                return false;
            }

            // Check ordinate overlap
            int    top = gBox.y;
            int    bot = top + gBox.height;
            double overlap = Math.min(bot, blobBottom) -
                             Math.max(top, blobTop);
            double overlapRatio = overlap / (blobBottom - blobTop);

            if (overlapRatio < constants.minOverlapRatio.getValue()) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "B#" + id + " Too low overlapRatio " + overlapRatio +
                        " vs " + constants.minOverlapRatio.getValue());
                }

                return false;
            }

            return true;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{B#");
            sb.append(id);

            if (blobTop != null) {
                sb.append(" top:")
                  .append(blobTop);
            }

            if (blobBottom != null) {
                sb.append(" bot:")
                  .append(blobBottom);
            }

            sb.append(Glyphs.toString(" glyphs", glyphs));
            sb.append("}");

            return sb.toString();
        }

        int getMedianHeight ()
        {
            if (medianHeight == null) {
                List<Integer> heights = new ArrayList<Integer>();

                for (Glyph glyph : glyphs) {
                    heights.add(glyph.getContourBox().height);
                }

                Collections.sort(heights);
                medianHeight = heights.get(heights.size() - 1);
            }

            return medianHeight;
        }

        int getMedianWidth ()
        {
            if (medianWidth == null) {
                List<Integer> widths = new ArrayList<Integer>();

                for (Glyph glyph : glyphs) {
                    widths.add(glyph.getContourBox().width);
                }

                Collections.sort(widths);
                medianWidth = widths.get(widths.size() - 1);
            }

            return medianWidth;
        }

        /**
         * Report the right abscissa of the blob
         * @return the right abscissa
         */
        private int getRight ()
        {
            return blobBox.x + blobBox.width;
        }

        /**
         * Try to insert the provided small glyph (punctuation, diacritical ...)
         * @param glyph the provided small glyph
         * @return true if we have successfully inserted the glyph into the blob
         */
        private boolean tryToInsert (Glyph glyph)
        {
            // Check whether the glyph is not too far from the blob
            PixelRectangle fatBox = glyph.getContourBox();
            fatBox.grow(
                smallXMargin,
                (int) Math.rint(
                    getMedianHeight() * constants.smallYRatio.getValue()));

            // x check
            if (!fatBox.intersects(blobBox)) {
                return false;
            }

            // y check (Beware these are vertical glyphs, so use xAt for yAt)
            Line2D.Double l2D = new Line2D.Double(
                blobBox.x,
                average.xAt(blobBox.x),
                blobBox.x + blobBox.width,
                average.xAt(blobBox.x + blobBox.width));

            if (l2D.intersects(fatBox.x, fatBox.y, fatBox.width, fatBox.height)) {
                glyphs.add(glyph);

                return true;
            }

            return false;
        }
    }
}
