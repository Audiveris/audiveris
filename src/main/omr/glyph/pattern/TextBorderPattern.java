//----------------------------------------------------------------------------//
//                                                                            //
//                     T e x t B o r d e r P a t t e r n                      //
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
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.text.Language;
import omr.glyph.text.OcrLine;
import omr.glyph.text.OcrTextVerifier;
import omr.glyph.text.TextBlob;
import omr.glyph.text.TextInfo;
import omr.glyph.text.TextLine;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.grid.LineInfo;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.grid.StaffInfo;
import omr.sheet.SystemBoundary.Side;
import omr.sheet.SystemInfo;

import omr.util.BrokenLine;

import java.awt.Polygon;
import java.util.*;

/**
 * Class {@code TextBorderPattern} directly uses OCR on specific regions around
 * staves of a given system to retrieve text-shaped glyphs.
 * <ol>
 * <li>We define near-rectangular zones above and below staves, and on left and
 * right sides of the system.</li>
 * <li>For each zone, all contained glyphs are filtered and gathered into blobs
 * (each blob being likely to contain one sentence).</li>
 * <li>NOTA: By-passing the neural network,each blob is directly handed to the
 * OCR engine.</li>
 * <li>Suitable OcrLines returned by the OCR engine are then used to assign
 * the TEXT shape to the corresponding glyphs.</li>
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class TextBorderPattern
    extends AbstractBlobPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TextBorderPattern.class);

    //~ Instance fields --------------------------------------------------------

    /** The system contour box (just the union of staves) */
    private final PixelRectangle systemBox;

    // Scale dependent constants
    private final int glyphMinHeight;
    private final int maxFontSize;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // TextBorderPattern //
    //-------------------//
    /**
     * Creates a new TextBorderPattern object.
     * @param system the containing system
     */
    public TextBorderPattern (SystemInfo system)
    {
        super("TextBorder", system);

        glyphMinHeight = scale.toPixels(constants.glyphMinHeight);
        maxFontSize = scale.toPixels(constants.maxFontSize);

        // System contour box
        systemBox = new PixelRectangle(
            system.getLeft(),
            system.getTop(),
            system.getWidth(),
            system.getBottom() - system.getTop());
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // buildRegions //
    //--------------//
    @Override
    protected List<Region> buildRegions ()
    {
        Sheet           sheet = system.getSheet();
        int             staffMarginAbove = scale.toPixels(
            constants.staffMarginAbove);
        int             staffMarginBelow = scale.toPixels(
            constants.staffMarginBelow);

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

        List<Region> regions = new ArrayList<Region>();
        regions.add(
            new MyRegion("North", buildPolygon(topLine.getPoints(), ne, nw)));
        regions.add(
            new MyRegion("South", buildPolygon(botLine.getPoints(), se, sw)));
        regions.add(
            new MyRegion(
                "West",
                buildPolygon(
                    nw,
                    new PixelPoint(left, firstLine.yAt(left)),
                    new PixelPoint(left, lastLine.yAt(left)),
                    sw)));
        regions.add(
            new MyRegion(
                "East",
                buildPolygon(
                    new PixelPoint(right, firstLine.yAt(right)),
                    ne,
                    se,
                    new PixelPoint(right, lastLine.yAt(right)))));

        return regions;
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
                        "ocrLine:" + i + " " + box + " " + ocrLine.toString() +
                        " w:" + (box.width / (fontSize * value.length())) +
                        " h:" + (box.height / fontSize) + " aspect:" +
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
        // Convert ocrLine from glyph-based to absolute coordinates
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
    }

    //----------//
    // MyRegion //
    //----------//
    private class MyRegion
        extends Region
    {
        //~ Constructors -------------------------------------------------------

        public MyRegion (String  name,
                         Polygon polygon)
        {
            super(name, polygon);
        }

        //~ Methods ------------------------------------------------------------

        //---------//
        // isSmall //
        //---------//
        @Override
        protected boolean isSmall (Glyph glyph)
        {
            // Add a test on weight? TODO
            return glyph.getContourBox().height < glyphMinHeight;
        }

        //-----------//
        // checkBlob //
        //-----------//
        protected boolean checkBlob (TextBlob blob,
                                     Glyph    compound)
        {
            // Call the OCR?
            if (TextLine.useOCR()) {
                OcrLine ocrLine = null;

                if (compound.getTextInfo()
                            .getContent() == null) {
                    String language = system.getScoreSystem()
                                            .getScore()
                                            .getLanguage();
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

                if (!OcrTextVerifier.isValid(compound, ocrLine)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Invalid blob " + ocrLine + " " + compound);
                    }

                    return false;
                }

                // OK
                compound = system.addGlyph(compound);
                system.computeGlyphFeatures(compound);
                compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Border Glyph#" + compound.getId() + "=>\"" +
                        ocrLine.value + "\"");
                }

                return true;
            } else {
                return true; // By default (with no OCR call...)
            }
        }

        //----------------//
        // checkCandidate //
        //----------------//
        @Override
        protected boolean checkCandidate (Glyph glyph)
        {
            // Respect user choice!
            if (glyph.isManualShape()) {
                return false;
            }

            // Don't go with blacklisted text
            if (glyph.isShapeForbidden(Shape.TEXT)) {
                return false;
            }

            // We remove candidates that are stuck to a stem that goes into a 
            // staff because these glyphs are not likely to be text items
            Glyph[] stems = new Glyph[] {
                                glyph.getLeftStem(), glyph.getRightStem()
                            };

            for (Glyph stem : stems) {
                if ((stem != null) &&
                    stem.getContourBox()
                        .intersects(systemBox)) {
                    return false;
                }
            }

            return true;
        }
    }
}
