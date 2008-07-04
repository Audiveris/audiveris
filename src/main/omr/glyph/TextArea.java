//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t A r e a                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.HorizontalOrientation;
import omr.lag.Oriented;
import omr.lag.VerticalOrientation;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>TextArea</code> handles an area likely to contain some text
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextArea
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextArea.class);

    //~ Instance fields --------------------------------------------------------

    /** The parent area, if any */
    private final TextArea parent;

    /** Underlying region of interest */
    private final GlyphLag.Roi roi;

    /** The (default) orientation of projection for this area */
    private final Oriented orientation;

    /** The horizontal histogram for this area */
    private int[] horizontalHistogram;

    /** The vertical histogram for this area */
    private int[] verticalHistogram;

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
     * @param parent the containing text area if any
     * @param roi the region of interest in the related lag
     * @param orientation the default orientation when scanning this area
     */
    public TextArea (TextArea     parent,
                     GlyphLag.Roi roi,
                     Oriented     orientation)
    {
        this.parent = parent;
        this.roi = roi;
        this.orientation = orientation;

        if (logger.isFineEnabled()) {
            logger.fine("Processing " + this);
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
        Rectangle r = roi.getAbsoluteContour();

        return new PixelRectangle(r.x, r.y, r.width, r.height);
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
    public int[] getHistogram (Oriented orientation)
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
    /** Get the histogram for this area, using the area default orientation
     * @return the area histogram in its default direction
     */
    public int[] getHistogram ()
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
     * Subdivide the area in subareas and evaluate each of the (sub) areas
     * @param sheet the related sheet
     */
    public void subdivide (Sheet sheet)
    {
        if (splitArea(sheet, false) > 1) {
            if (logger.isFineEnabled()) {
                logger.fine("Node " + this);
            }

            // The area can still be divided
            splitArea(sheet, true);
        } else {
            // This area is a leaf
            if (logger.isFineEnabled()) {
                logger.fine("Leaf " + this);
            }
        }

        if (evaluateArea(sheet)) {
            if (logger.isFineEnabled()) {
                logger.fine("Text found in " + parent);
            }

            setTextLeaf(true);

            if (parent != null) {
                parent.textGlyphNb++;
            }
        }

        // Recurse
        if (subareas != null) {
            for (TextArea subarea : subareas) {
                subarea.subdivide(sheet);
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

    //--------------//
    // computeLines //
    //--------------//
    /**
     * Compute the ordinates of the baseline and the x-height line
     */
    private void computeLines ()
    {
        // Get a horizontal histogram
        int[] histo = getHistogram(new HorizontalOrientation());

        // Retrieve max histogram value, and take threshold at half
        maxValue = 0;

        for (int val : histo) {
            if (maxValue < val) {
                maxValue = val;
            }
        }

        int mainThreshold = (int) Math.rint(
            maxValue * constants.mainHistoThreshold.getValue());

        // Use the main threshold for baseline
        int baseBucket = lastBucketAt(histo, mainThreshold);
        baseline = roi.getAbsoluteContour().y + baseBucket;

        int topBucket = firstBucketAt(
            histo,
            (int) Math.rint(maxValue * constants.topHistoThreshold.getValue()));
        topline = roi.getAbsoluteContour().y + topBucket;

        int medianBucket = firstBucketAt(
            histo,
            (int) Math.rint(maxValue * constants.mainHistoThreshold.getValue()));

        int minMedianBucket = baseBucket -
                              (int) ((baseBucket - topBucket) * constants.maxMedianRatio.getValue());

        if (medianBucket < minMedianBucket) {
            medianBucket = minMedianBucket +
                           firstBucketAt(
                Arrays.copyOfRange(histo, minMedianBucket, histo.length - 1),
                (int) Math.rint(
                    maxValue * constants.mainHistoThreshold.getValue()));
        }

        medianLine = roi.getAbsoluteContour().y + medianBucket;
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

    //--------------//
    // evaluateArea //
    //--------------//
    /**
     * Check whether the content of this area can be recognized as a single text
     * compound, and if so, actually assign the shape to the underlying glyph
     * (either the only glyph in the area, or the compound glyph newly built for
     * this purpose)
     * @return true if check is positive
     */
    private boolean evaluateArea (Sheet sheet)
    {
        List<Glyph> glyphs = sheet.getVerticalLag()
                                  .lookupGlyphs(roi.getAbsoluteContour());

        // Purge the glyph collection of unwanted glyphs
        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            // Glyph for which TEXT is forbidden
            if (glyph.isShapeForbidden(Shape.TEXT)) {
                it.remove();
            }

            // Glyph already member of a sentence ??? TBD
        }

        if (glyphs.size() > 0) {
            Glyph glyph;

            if (glyphs.size() > 1) {
                glyph = sheet.getGlyphsBuilder()
                             .buildCompound(glyphs);
            } else {
                glyph = glyphs.get(0);
            }

            Evaluator  evaluator = GlyphNetwork.getInstance();
            Evaluation vote = evaluator.vote(
                glyph,
                GlyphInspector.getSymbolMaxDoubt());

            if (vote != null) {
                if (logger.isFineEnabled()) {
                    logger.fine("Vote: " + vote.toString());
                }

                if (vote.shape.isText()) {
                    if (glyph.getId() == 0) {
                        glyph = sheet.getGlyphsBuilder()
                                     .insertGlyph(glyph);
                    }

                    sheet.getGlyphsBuilder()
                         .computeGlyphFeatures(glyph);
                    // No! glyph.setTextArea(this);
                    glyph.setShape(vote.shape, vote.doubt);

                    return true;
                }
            }
        }

        return false;
    }

    //---------------//
    // firstBucketAt //
    //---------------//
    /**
     * Retrieve the first histogram bucket whose value is greater or equal to
     * the given threshold
     * @param histo the histo to search
     * @param threshold the given threshold value
     * @return the index of the bucket found, or -1 if not found
     */
    private int firstBucketAt (int[] histo,
                               int   threshold)
    {
        for (int i = 0; i < histo.length; i++) {
            if (histo[i] >= threshold) {
                return i;
            }
        }

        return -1;
    }

    //---------------//
    // lastBucketAt //
    //---------------//
    /**
     * Retrieve the last histogram bucket whose value is greater or equal to
     * the given threshold
     * @param histo the histo to search
     * @param threshold the given threshold value
     * @return the index of the bucket found, or -1 if not found
     */
    private int lastBucketAt (int[] histo,
                              int   threshold)
    {
        for (int i = histo.length - 1; i >= 0; i--) {
            if (histo[i] >= threshold) {
                return i;
            }
        }

        return -1;
    }

    //-----------//
    // splitArea //
    //-----------//
    /**
     * Try to perform a split in the projection orientation
     * @param sheet the related sheet
     * @param building should we actually create the subareas
     * @return the number of subareas identified (whether thay are actually
     * created or not)
     */
    private int splitArea (Sheet   sheet,
                           boolean building)
    {
        // Make sure the histogram is available
        int[]     histo = getHistogram();

        int       children = 0;
        Scale     scale = sheet.getScale();
        boolean   spacing = true;
        int       packetStart = -1;
        int       packetEnd = 0;

        // Minimum gap (vertically between lines, or horizontally between words)
        final int minGap = orientation.isVertical()
                           ? scale.toPixels(constants.minHorizontalGap)
                           : scale.toPixels(constants.minVerticalGap);

        for (int i = 0; i < histo.length; i++) {
            int val = histo[i];

            if (spacing) {
                if (val == 0) {
                    // Continue with the gap
                } else {
                    // End of the spacing
                    spacing = false;

                    // Pending packet?
                    if (packetStart != -1) {
                        if ((i - packetEnd) >= minGap) {
                            // End of real gap
                            children++;

                            if (building) {
                                getSubareas()
                                    .add(createSubarea(packetStart, packetEnd));
                            }

                            packetStart = i;
                        } else {
                            // Gap too small
                        }
                    } else {
                        packetStart = i;
                    }
                }
            } else {
                if (val == 0) {
                    // Start a gap
                    packetEnd = i;
                    spacing = true;
                } else {
                    // Continue with the packet
                }
            }
        }

        // Ending of a packet?
        if (packetStart != -1) {
            children++;

            if (building) {
                if (packetEnd > packetStart) {
                    getSubareas()
                        .add(createSubarea(packetStart, packetEnd));
                } else {
                    getSubareas()
                        .add(createSubarea(packetStart, histo.length));
                }
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

        Scale.Fraction minVerticalGap = new Scale.Fraction(
            0.25,
            "Minimum value for a vertical gap between histogram packets");
        Scale.Fraction minHorizontalGap = new Scale.Fraction(
            0.50,
            "Minimum value for a horizontal gap between histogram packets");
        Constant.Ratio topHistoThreshold = new Constant.Ratio(
            0.1,
            "Threhold to detect top of characters");
        Constant.Ratio mainHistoThreshold = new Constant.Ratio(
            0.5,
            "Main threhold to detect x-height of characters");
        Constant.Ratio maxMedianRatio = new Constant.Ratio(
            0.75,
            "Maximum ratio of x-height part in characters height");
    }
}
