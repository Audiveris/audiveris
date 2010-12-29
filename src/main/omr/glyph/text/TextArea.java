//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t A r e a                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.*;
import omr.glyph.facets.Glyph;

import omr.lag.HorizontalOrientation;
import omr.lag.Oriented;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.math.Histogram;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>TextArea</code> handles a rectangular area likely to contain
 * some text
 *
 * @author Hervé Bitteur
 */
public class TextArea
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextArea.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing system */
    private final SystemInfo system;

    /** The containing sheet, if any */
    private final Sheet sheet;

    /** The parent area, if any */
    private final TextArea parent;

    /** Underlying region of interest */
    private final GlyphLag.Roi roi;

    /**
     * The default orientation for this area
     * - Horizontal: we expect lines or words one below the other in this area
     * - Vertical:   we expect words one beside the other in this area
     */
    private final Oriented orientation;

    /** The horizontal histogram for this area */
    private Histogram<Integer> horizontalHistogram;

    /** The vertical histogram for this area */
    private Histogram<Integer> verticalHistogram;

    /** Sub areas found if any */
    private List<TextArea> subareas;

    /** True if this area is a text leaf */
    private boolean textLeaf;

    /** Computed maximum histogram value, if any */
    private Integer maxValue;

    /** Computed ordinate for text top line, if any */
    private Integer topline;

    /** Computed ordinate for text mean line, if any */
    private Integer medianLine;

    /** Computed ordinate for text baseline, if any */
    private Integer baseline;

    /** Number of text glyphs found in this area */
    private int textGlyphNb = 0;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // TextArea //
    //----------//
    /**
     * Creates a new TextArea object.
     *
     * @param system the containing system, if any
     * @param parent the containing text area if any
     * @param roi the region of interest in the related lag
     * @param orientation the default orientation when splitting this area
     */
    public TextArea (SystemInfo   system,
                     TextArea     parent,
                     GlyphLag.Roi roi,
                     Oriented     orientation)
    {
        this.system = system;

        if (system != null) {
            sheet = system.getSheet();
        } else {
            sheet = null;
        }

        this.parent = parent;
        this.roi = roi;
        this.orientation = orientation;

        if (logger.isFineEnabled()) {
            logger.fine("Creating " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getAbsoluteContour //
    //--------------------//
    /**
     * Report the rectangle that delimits this area
     * @return the area in pixels, from top left of sheet
     */
    public PixelRectangle getAbsoluteContour ()
    {
        return new PixelRectangle(roi.getAbsoluteContour());
    }

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Report the absolute baseline ordinate of the horizontal projection
     * @return the ordinate of the text baseline, in pixels from top of page
     */
    public int getBaseline ()
    {
        if (baseline == null) {
            computeLines();
        }

        return baseline;
    }

    //--------------//
    // getHistogram //
    //--------------//
    /**
     * Get the histogram for this area, in the specified orientation
     * @param orientation specific orientation desired for the histogram
     * @return the histogram of projected pixels
     */
    public Histogram<Integer> getHistogram (Oriented orientation)
    {
        if (orientation.isVertical()) {
            if (verticalHistogram == null) {
                verticalHistogram = roi.getHistogram(orientation);
            }

            return verticalHistogram;
        } else {
            if (horizontalHistogram == null) {
                horizontalHistogram = roi.getHistogram(orientation);
            }

            return horizontalHistogram;
        }
    }

    //--------------//
    // getHistogram //
    //--------------//
    /**
     * Get the histogram for a glyph in this area, in the specified orientation
     * @param orientation specific orientation desired for the histogram
     * @param glyph the provided glyph if any, otherwise the whole area
     * @return the histogram of projected pixels
     */
    public Histogram<Integer> getHistogram (Oriented orientation,
                                            Glyph    glyph)
    {
        Histogram<Integer> histo = null;

        if (glyph == null) {
            histo = getHistogram(orientation);
        } else {
            histo = roi.getHistogram(orientation, glyph.getMembers());
        }

        // Cache the result
        if (orientation.isVertical()) {
            verticalHistogram = histo;
        } else {
            horizontalHistogram = histo;
        }

        return histo;
    }

    //--------------//
    // getHistogram //
    //--------------//
    /** Get the histogram for this area, using the area default orientation
     * @return the area histogram in its default direction
     */
    public Histogram<Integer> getHistogram ()
    {
        return getHistogram(orientation);
    }

    //------------------//
    // getMaxHistoValue //
    //------------------//
    public int getMaxHistoValue ()
    {
        if (maxValue == null) {
            computeLines();
        }

        return maxValue;
    }

    //---------------//
    // getMedianLine //
    //---------------//
    /**
     * Report the absolute ordinate of the median line of the text area
     * @return the x-height limit, counted in pixels from top of page
     */
    public int getMedianLine ()
    {
        if (medianLine == null) {
            computeLines();
        }

        return medianLine;
    }

    //------------//
    // isTextLeaf //
    //------------//
    /**
     * Report whether this area is actually a text glyph
     * @return true if area is a text glyph
     */
    public boolean isTextLeaf ()
    {
        return textLeaf;
    }

    //------------//
    // getTopline //
    //------------//
    /**
     * Report the absolute topline ordinate of the horizontal projection
     * @return the ordinate of the text topline, in pixels from top of page
     */
    public int getTopline ()
    {
        if (topline == null) {
            computeLines();
        }

        return topline;
    }

    //-----------//
    // subdivide //
    //-----------//
    /**
     * Subdivide the area in subareas and evaluate each of the (sub) areas as a
     * potential textual glyph
     */
    public void subdivide ()
    {
        if (splitArea(false) > 1) {
            // The area can still be divided
            splitArea(true);
        } else if (logger.isFineEnabled()) {
            logger.fine(this + " is Leaf ");
        }

        // Recurse
        if (subareas != null) {
            for (TextArea subarea : subareas) {
                subarea.subdivide();
            }
        }

        if (isTextualArea()) {
            if (logger.isFineEnabled()) {
                logger.fine("================ Text found in " + this);
            }

            setTextLeaf(true);

            if (parent != null) {
                parent.textGlyphNb++;
            }
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Area-");

        TextArea p = this;
        int      level = 1;

        while ((p = p.parent) != null) {
            level++;
        }

        sb.append(level);

        if (orientation.isVertical()) {
            sb.append(" vertical  ");
        } else {
            sb.append(" horizontal");
        }

        sb.append(" abs:")
          .append(roi.getAbsoluteContour());
        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // getSubareas //
    //-------------//
    /**
     * Report the collection of subareas, perhaps empty but not null
     * @return the collection of subareas
     */
    private List<TextArea> getSubareas ()
    {
        if (subareas == null) {
            subareas = new ArrayList<TextArea>();
        }

        return subareas;
    }

    //-------------//
    // setTextLeaf //
    //-------------//
    private void setTextLeaf (boolean textLeaf)
    {
        this.textLeaf = textLeaf;
    }

    //---------------//
    // isTextualArea //
    //---------------//
    /**
     * Check whether the content of this area can be recognized as a single text
     * compound, and if so, actually assign the shape to the underlying glyph
     * (either the only glyph in the area, or the compound glyph newly built for
     * this purpose)
     * @return true if check is positive
     */
    private boolean isTextualArea ()
    {
        logger.fine(this + " isTextualArea");

        // We cannot evaluate glyphs that do not belong to a system
        if (system == null) {
            return false;
        }

        // Retrieve glyphs in the provided rectangular area
        Set<Glyph> glyphs = sheet.getVerticalLag()
                                 .lookupGlyphs(roi.getAbsoluteContour());

        // A system is not exactly a rectangular area
        // So some further glyph filtering is needed
        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (sheet.getSystemOf(glyph) != system) {
                it.remove();
            }
        }

        // Purge the glyph collection of unwanted glyphs
        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            // Glyph for which TEXT is forbidden
            if (glyph.isShapeForbidden(Shape.TEXT)) {
                it.remove();
            }

            // TODO: Glyph already member of a sentence ???
        }

        if (glyphs.isEmpty()) {
            if (logger.isFineEnabled()) {
                logger.fine("No glyph found");
            }

            return false;
        }

        Glyph glyph;

        if (glyphs.size() > 1) {
            glyph = system.buildTransientCompound(glyphs);
        } else {
            glyph = glyphs.iterator()
                          .next();
        }

        // First, use the glyph evaluator
        GlyphEvaluator evaluator = GlyphNetwork.getInstance();
        Evaluation     vote = evaluator.vote(
            glyph,
            GlyphInspector.getTextMaxDoubt(),
            system);

        if (vote != null) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "Vote: " + vote.toString() +
                    Glyphs.toString(" for", glyphs));
            }

            if (vote.shape.isText()) {
                return createText(glyph, vote);
            }
        }

        //        // Second, use the OCR engine
        //        String        language = system.getScoreSystem()
        //                                       .getScore()
        //                                       .getLanguage();
        //        List<OcrLine> lines = Language.getOcr()
        //                                      .recognize(
        //            glyph.getImage(),
        //            language,
        //            "g" + glyph.getId() + ".");
        //
        //        final Scale   scale = sheet.getScale();
        //        final int     maxFontSize = scale.toPixels(constants.maxFontSize);
        //
        //        ///logger.info("maxFontSize=" + maxFontSize);
        //
        //        // Debug
        //        if (logger.isFineEnabled()) {
        //            int i = 0;
        //
        //            for (OcrLine ocrLine : lines) {
        //                i++;
        //
        //                String         value = ocrLine.value;
        //                float          fontSize = ocrLine.fontSize;
        //                PixelRectangle box = ocrLine.getContourBox();
        //
        //                if (logger.isFineEnabled()) {
        //                    logger.fine(
        //                        i + " " + box + " " + ocrLine.toString() + " w:" +
        //                        (box.width / (fontSize * value.length())) + " h:" +
        //                        (box.height / fontSize) + " aspect:" +
        //                        (((float) box.height * value.length()) / box.width));
        //                }
        //            }
        //        }
        //
        //        // Tests on OCR results
        //        if (lines.size() < 1) {
        //            logger.warning("No line found");
        //
        //            return false;
        //        }
        //
        //        if (lines.size() > 1) {
        //            logger.warning("More than 1 line found");
        //
        //            return false;
        //        }
        //
        //        OcrLine        ocrLine = lines.get(0);
        //        String         value = ocrLine.value;
        //        float          fontSize = ocrLine.fontSize;
        //        PixelRectangle box = ocrLine.getContourBox();
        //
        //        if (box.height == 0) {
        //            logger.warning("No OCR");
        //
        //            return false;
        //        }
        //
        //        if (fontSize > maxFontSize) {
        //            logger.warning(
        //                "Font size " + fontSize + " exceeds maximum " + maxFontSize);
        //
        //            return false;
        //        }
        //
        //        float        aspect = ((float) box.height * value.length()) / box.width;
        //
        //        final double maxAspect = constants.maxAspect.getValue();
        //
        //        if (aspect > maxAspect) {
        //            logger.warning(
        //                "Char aspect " + aspect + " exceeds maximum " + maxAspect);
        //
        //            return false;
        //        }
        //
        //        // OK
        //        return createText(
        //            glyph,
        //            new Evaluation(Shape.TEXT, Evaluation.ALGORITHM));
        return false;
    }

    //--------------//
    // computeLines //
    //--------------//
    /**
     * Compute the ordinates of the baseline and the x-height line
     */
    private void computeLines ()
    {
        // Get a horizontal histogram
        Histogram<Integer> histo = getHistogram(new HorizontalOrientation());

        // Retrieve max histogram value, and take threshold at half
        maxValue = histo.getMaxCount();

        int                           mainThreshold = (int) Math.rint(
            maxValue * constants.mainHistoThreshold.getValue());

        // Use the main threshold for baseline
        List<Histogram.Pair<Integer>> mainPeaks = histo.getPeaks(mainThreshold);
        List<Histogram.Pair<Integer>> topPeaks = histo.getPeaks(
            (int) Math.rint(maxValue * constants.topHistoThreshold.getValue()));

        topline = topPeaks.get(0).first;
        medianLine = mainPeaks.get(0).first;
        baseline = mainPeaks.get(mainPeaks.size() - 1).second;

        //        int minMedianLine = baseline -
        //                            (int) ((baseline - topline) * constants.maxMedianRatio.getValue());
        //
        // TODO: A REVOIR
        //        if (medianBucket < minMedianBucket) {
        //            medianBucket = minMedianBucket +
        //                           firstBucketAt(
        //                Arrays.copyOfRange(histo, minMedianBucket, histo.length - 1),
        //                (int) Math.rint(
        //                    maxValue * constants.mainHistoThreshold.getValue()));
        //        }
        ///medianLine = roi.getAbsoluteContour().y + medianBucket;
        //        medianLine = medianBucket;
    }

    //---------------//
    // createSubarea //
    //---------------//
    /**
     * Create a sub-area, using the packet that starts at 'packetStart' and ends
     * right before 'gapStart'
     * @param packetStart the index in histo at beginning of packet
     * @param gapStart the index in histo just past the end of the packet
     * @return the created subarea
     */
    private TextArea createSubarea (int packetStart,
                                    int gapStart)
    {
        GlyphLag  lag = (GlyphLag) roi.getLag();
        Rectangle contour = roi.getAbsoluteContour();

        if (orientation.isVertical()) {
            return new TextArea(
                system,
                this,
                lag.createAbsoluteRoi(
                    new Rectangle(
                        contour.x + packetStart,
                        contour.y,
                        gapStart - packetStart,
                        contour.height)),
                new HorizontalOrientation());
        } else {
            return new TextArea(
                system,
                this,
                lag.createAbsoluteRoi(
                    new Rectangle(
                        contour.x,
                        contour.y + packetStart,
                        contour.width,
                        gapStart - packetStart)),
                new VerticalOrientation());
        }
    }

    //------------//
    // createText //
    //------------//
    private boolean createText (Glyph      glyph,
                                Evaluation eval)
    {
        // Check that this glyph is not forbidden as text
        Glyph original = sheet.getVerticalLag()
                              .getOriginal(glyph);

        if ((original != null) && original.isShapeForbidden(Shape.TEXT)) {
            return false;
        } else {
            if (glyph.getId() == 0) {
                glyph = system.addGlyph(glyph);
            }

            system.computeGlyphFeatures(glyph);

            // No! glyph.setTextArea(this);
            glyph.setShape(eval.shape, eval.doubt);

            if (logger.isFineEnabled()) {
                logger.fine("Glyph#" + glyph.getId() + " TEXT recognized");
            }

            return true;
        }
    }

    //-----------//
    // splitArea //
    //-----------//
    /**
     * Try to perform a split of this area using the projection orientation
     * @param building should we actually create the subareas?
     * @return the number of subareas identified (whether they are actually
     * created or not)
     */
    private int splitArea (boolean building)
    {
        logger.fine(this + " splitArea" + " building:" + building);

        // Make sure the histogram is available
        Histogram<Integer> histo = getHistogram();
        int                children = 0;
        Scale              scale = sheet.getScale();
        boolean            spacing = true;
        Integer            packetStart = null;
        int                packetEnd = 0;

        // Minimum gap (vertically between lines, or horizontally between words)
        final int minGap = orientation.isVertical()
                           ? scale.toPixels(constants.minHorizontalGap)
                           : scale.toPixels(constants.minVerticalGap);
        final int minPacket = orientation.isVertical()
                              ? scale.toPixels(constants.minHorizontalPacket)
                              : scale.toPixels(constants.minVerticalPacket);

        ///logger.info(this + " minGap:" + minGap + " minPacket:" + minPacket);
        for (Histogram.Pair<Integer> packet : histo.getPeaks(1)) {
            // Pending packet?
            if (packetStart == null) {
                packetStart = packet.first;
                packetEnd = packet.second;
            } else {
                // Real gap before?
                if ((packet.first - packetEnd) >= minGap) {
                    // Did we have a big enough packet?
                    if ((packetEnd - packetStart) >= minPacket) {
                        children++;

                        if (building) {
                            getSubareas()
                                .add(createSubarea(packetStart, packetEnd));
                        }
                    }

                    packetStart = packet.first;
                } else {
                    // Gap too small
                }

                packetEnd = packet.second;
            }
        }

        // Last packet
        if ((packetStart != null) && ((packetEnd - packetStart) >= minPacket)) {
            children++;

            if (building) {
                getSubareas()
                    .add(createSubarea(packetStart, packetEnd));
            }
        }

        return children;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxFontSize = new Scale.Fraction(
            1.8,
            "Maximum value for text font size");
        Scale.Fraction minVerticalGap = new Scale.Fraction(
            0.1,
            "Minimum value for a vertical gap between histogram packets");
        Scale.Fraction minHorizontalGap = new Scale.Fraction(
            1.4,
            "Minimum value for a horizontal gap between histogram packets");
        Scale.Fraction minVerticalPacket = new Scale.Fraction(
            0.5,
            "Minimum value for a vertical packet");
        Scale.Fraction minHorizontalPacket = new Scale.Fraction(
            0.5,
            "Minimum value for a horizontal packet");
        Constant.Ratio topHistoThreshold = new Constant.Ratio(
            0.1,
            "Threshold to detect top of characters");
        Constant.Ratio mainHistoThreshold = new Constant.Ratio(
            0.5,
            "Main threhold to detect x-height of characters");
        Constant.Ratio maxMedianRatio = new Constant.Ratio(
            0.75,
            "Maximum ratio of x-height part in characters height");
        Constant.Ratio maxAspect = new Constant.Ratio(
            3.0,
            "Maximum aspect of chars (height / width)");
    }
}
