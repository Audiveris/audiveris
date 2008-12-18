//----------------------------------------------------------------------------//
//                                                                            //
//                              S e n t e n c e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.constant.ConstantSet;

import omr.glyph.*;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.score.common.PageRectangle;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.ScoreSystem;
import omr.score.entity.ScoreSystem.StaffPosition;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>Sentence</code> encapsulates a consistent ordered set of text
 * glyphs (loosely similar to words) that represents a whole expression.
 *
 * <h4>Textual glyph Data Model:<br/>
 *    <img src="doc-files/Sentence.jpg"/>
 * </h4>
 *
 * <h4>Processing of textual glyphs:<br/>
 *    <img src="doc-files/TextProcessing.jpg"/>
 * </h4>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Sentence
    implements Comparable<Sentence>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sentence.class);

    //~ Instance fields --------------------------------------------------------

    /** Containing system info */
    private final SystemInfo systemInfo;

    /** The containing system part */
    private SystemPart systemPart;

    /** The containing text glyph line */
    private final TextGlyphLine line;

    /** Ordered collection of text items */
    private final SortedSet<Glyph> items = new TreeSet<Glyph>();

    /** Type of this text sentence */
    private TextType type;

    /** The string value of the sentence, if any, assigned manually or by OCR */
    private String content;

    /** The text area for this sentence */
    private TextArea textArea;

    /** The starting point (in units) within the containing system */
    private SystemPoint location;

    /** Minimum horizontal gap between 2 sentences (cached for optimization) */
    private Integer minSentenceGap;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Sentence object.
     *
     * @param systemInfo The containing system
     * @param glyph A first glyph that gives birth to this sentence
     */
    Sentence (SystemInfo    systemInfo,
              TextGlyphLine line,
              Glyph         glyph)
    {
        this.systemInfo = systemInfo;
        this.line = line;

        addGlyph(glyph);

        if (logger.isFineEnabled()) {
            logger.fine("Created " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //---------------------//
    // getContentFromItems //
    //---------------------//
    /**
     * Determine the sentence string out of the items individual strings
     * @return the concatenation of all items strings
     */
    public String getContentFromItems ()
    {
        //        Scale          scale = systemInfo.getScoreSystem()
        //                                         .getScale();
        //        PixelRectangle lastBox = items.last()
        //                                      .getContourBox();
        //        PixelRectangle firstBox = items.first()
        //                                       .getContourBox();
        //        int            sentenceWidth = scale.pixelsToUnits(
        //            (lastBox.x + lastBox.width) - firstBox.x);
        StringBuilder sb = null;

        // Use each item string at proper location
        for (Glyph item : getGlyphs()) {
            ///PixelRectangle box = item.getContourBox();
            String str = item.getTextInfo()
                             .getContent();

            if (str == null) {
                str = item.getTextInfo()
                          .getPseudoContent();
            }

            if (sb == null) {
                sb = new StringBuilder(str);
            } else {
                sb.append(" ")
                  .append(str);
            }
        }

        if (sb == null) {
            return "";
        } else {
            return sb.toString();
        }
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the x-ordered collection of glyphs in this sentence
     * @return the collection of glyphs (words generally)
     */
    public SortedSet<Glyph> getGlyphs ()
    {
        return items;
    }

    //----------------------//
    // setGlyphsTranslation //
    //----------------------//
    /**
     * Forward the provided translation to all the items that compose this
     * sentence
     * @param entity the same translation entit for all sentence items
     */
    public void setGlyphsTranslation (Object entity)
    {
        for (Glyph item : getGlyphs()) {
            item.setTranslation(entity);
        }
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the system-based starting point of this sentence
     * @return the staring point (x: left side, y: baseline)
     */
    public SystemPoint getLocation ()
    {
        if (location == null) {
            PixelRectangle firstBox = items.first()
                                           .getContourBox();
            location = systemInfo.getScoreSystem()
                                 .toSystemPoint(
                new PixelPoint(firstBox.x, getTextArea().getBaseline()));
        }

        return location;
    }

    //------------------//
    // getSystemContour //
    //------------------//
    /**
     * Report the system-based rectangular contour of this sentence
     * @return the sentence contour, system-based
     */
    public SystemRectangle getSystemContour ()
    {
        return systemInfo.getScoreSystem()
                         .toSystemRectangle(getContourBox());
    }

    //---------------//
    // getSystemPart //
    //---------------//
    /**
     * Report the system part that contains this sentence
     * @return the containing system part
     */
    public SystemPart getSystemPart ()
    {
        if (systemPart == null) {
            ScoreSystem system = systemInfo.getScoreSystem();
            systemPart = system.getPartAt(
                system.toSystemPoint(items.first().getCentroid()));
        }

        return systemPart;
    }

    //----------------//
    // getContent //
    //----------------//
    /**
     * Report the string content of this sentence, as computed by OCR or entered
     * manually for example
     * @return the text interpretation of the sentence
     */
    public String getTextContent ()
    {
        return content;
    }

    //---------------//
    // getTextHeight //
    //---------------//
    /**
     * Determine the uniform character height for the whole sentence
     * @return the standard character height in pixels
     */
    public int getTextHeight ()
    {
        return getTextArea()
                   .getBaseline() - getTextArea()
                                        .getMedianLine();
    }

    //-------------//
    // setTextType //
    //-------------//
    /**
     * Force the text type (role) of the sentence within the score
     * @param type the role of this sentence
     */
    public void setTextType (TextType type)
    {
        this.type = type;

        for (Glyph item : getGlyphs()) {
            item.getTextInfo()
                .resetPseudoContent();
        }
    }

    //-------------//
    // getTextType //
    //-------------//
    /**
     * Report the text type (role) of the sentence within the score
     * @return the role of this sentence
     */
    public TextType getTextType ()
    {
        if (type == null) {
            type = guessType();
        }

        return type;
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement an x-ordered comparison
     * @param other the other sentence to be compared to
     * @return -1,0,+1 according to the comparison result
     */
    @Implement(Comparable.class)
    public int compareTo (Sentence other)
    {
        return items.first()
                    .compareTo(other.items.first());
    }

    //------------//
    // removeItem //
    //------------//
    /**
     * Remove the provided glyph from this sentence
     * @param glyph the glyph to removeItem
     */
    public void removeItem (Glyph glyph)
    {
        if ((items.size() == 1) && (items.first() == glyph)) {
            line.removeSentence(this);
        } else {
            items.remove(glyph);
            invalidateCache();
        }

        glyph.getTextInfo()
             .setSentence(null);
        line.removeItem(glyph);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Sentence" + Glyph.toString(items) + "}";
    }

    //------//
    // feed //
    //------//
    /**
     * Try to add the provided glyph as one of the sentence items (based on the
     * distance between the glyph and the sentence)
     * @param glyph the provided glyph to add if possible
     * @return true if the provided glyph has been successfully added
     */
    boolean feed (Glyph glyph)
    {
        if (canIncludeGlyph(glyph)) {
            addGlyph(glyph);

            return true;
        } else {
            return false;
        }
    }

    //---------------//
    // getContourBox //
    //---------------//
    private PixelRectangle getContourBox ()
    {
        PixelRectangle box = null;

        for (Glyph item : getGlyphs()) {
            if (box == null) {
                box = item.getContourBox();
            } else {
                box = box.union(item.getContourBox());
            }
        }

        return box;
    }

    //-------------------//
    // getMinSentenceGap //
    //-------------------//
    private int getMinSentenceGap ()
    {
        if (minSentenceGap == null) {
            Scale scale = systemInfo.getScoreSystem()
                                    .getScale();
            minSentenceGap = scale.toPixels(constants.minSentenceGap);
        }

        return minSentenceGap;
    }

    //-------------//
    // getTextArea //
    //-------------//
    /**
     * Report (and build if needed) the text area that corresponds to the
     * sentence contour, so that textual characteristics, such as baseline or
     * x-height can be computed
     * @return the related text area
     */
    private TextArea getTextArea ()
    {
        if (textArea == null) {
            textArea = new TextArea(
                null,
                systemInfo.getSheet().getVerticalLag().createAbsoluteRoi(
                    getContourBox()),
                new HorizontalOrientation());
        }

        return textArea;
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a (textual) glyph to this sentence
     * @param glyph the textual glyph to addGlyph
     */
    private void addGlyph (Glyph glyph)
    {
        items.add(glyph);
        glyph.getTextInfo()
             .setSentence(this);
        invalidateCache();
    }

    //-------------------//
    // areInSameSentence //
    //-------------------//
    /**
     * Check whether the two glyphs provided can be considered as two portions
     * in a row of the same sentence (their order is not relevant)
     * @param g1 first glyph
     * @param g2 secong glyph
     * @return true if they are close enough
     */
    private boolean areInSameSentence (Glyph g1,
                                       Glyph g2)
    {
        int x1 = g1.getTextInfo()
                   .getTextStart().x;
        int x2 = g2.getTextInfo()
                   .getTextStart().x;

        if (x1 < x2) {
            x1 += g1.getContourBox().width;
        } else {
            x2 += g2.getContourBox().width;
        }

        return (Math.abs(x1 - x2)) < getMinSentenceGap();
    }

    //-----------------//
    // canIncludeGlyph //
    //-----------------//
    /**
     * Check whether the sentence could include the provided glyph as one of its
     * items
     * @param glyph the provided glyph to check for inclusion
     * @return true if the provided glyph is compatible with the sentence
     */
    private boolean canIncludeGlyph (Glyph glyph)
    {
        final int x = glyph.getAreaCenter().x;

        // Already within the sentence width?
        if ((x >= items.first().getAreaCenter().x) &&
            (x <= items.last().getAreaCenter().x)) {
            return true;
        }

        // Close to left side?
        if (areInSameSentence(glyph, items.first())) {
            return true;
        }

        // Close to right side?
        if (areInSameSentence(items.last(), glyph)) {
            return true;
        }

        return false;
    }

    //-----------//
    // guessType //
    //-----------//
    /**
     * Try to infer the role of this sentence. For the time being, this is a
     * simple algorithm based on sentence location within the page,
     * but perhaps a neural network approach would better fit this task.
     */
    private TextType guessType ()
    {
        Sheet           sheet = systemInfo.getSheet();
        ScoreSystem     system = systemInfo.getScoreSystem();
        Scale           scale = system.getScale();
        PageRectangle   pageBox = scale.toUnits(getContourBox());
        SystemRectangle box = getSystemContour();
        SystemPoint     left = new SystemPoint(box.x, box.y + (box.height / 2));
        SystemPoint     right = new SystemPoint(
            box.x + box.width,
            box.y + (box.height / 2));

        // First system in page?
        boolean       firstSystem = system.getId() == 1;

        // Last system in page?
        boolean       lastSystem = sheet.getSystems()
                                        .size() == system.getId();

        // Vertical position wrt staves
        StaffPosition position = system.getStaffPosition(left);

        // Vertical distance from staff
        Staff   staff = system.getStaffAt(left);
        int     staffDy = Math.abs(staff.getTopLeft().y - pageBox.y);
        boolean closeToStaff = staffDy <= scale.toUnits(constants.maxStaffDy);

        // Begins before the part?
        boolean leftOfStaves = system.isLeftOfStaves(left);

        // At the center of page width?
        int     maxCenterDx = scale.toUnits(constants.maxCenterDx);
        int     pageCenter = scale.pixelsToUnits(sheet.getWidth() / 2);
        boolean pageCentered = Math.abs(
            (pageBox.x + (pageBox.width / 2)) - pageCenter) <= maxCenterDx;

        // Right aligned with staves
        int     maxRightDx = scale.toUnits(constants.maxRightDx);
        boolean rightAligned = Math.abs(right.x - system.getDimension().width) <= maxRightDx;

        // Short Sentence?
        int     maxShortLength = scale.toUnits(constants.maxShortLength);
        boolean shortSentence = pageBox.width <= maxShortLength;

        int     minTitleHeight = scale.toUnits(constants.minTitleHeight);
        boolean highText = box.height >= minTitleHeight;

        if (logger.isFineEnabled()) {
            logger.fine(
                this + " firstSystem=" + firstSystem + " lastSystem=" +
                lastSystem + " position=" + position + " closeToStaff=" +
                closeToStaff + " leftOfStaves=" + leftOfStaves +
                " pageCentered=" + pageCentered + " rightAligned=" +
                rightAligned + " shortSentence=" + shortSentence +
                " highText=" + highText);
        }

        // Decisions ...
        switch (position) {
        case above : // Title, Number, Creator, Direction (Accord)

            if (leftOfStaves || rightAligned) {
                return TextType.Creator;
            } else if (closeToStaff) {
                return TextType.Direction;
            } else if (pageCentered) { // Title, Number

                if (highText) {
                    return TextType.Title;
                } else {
                    return TextType.Number;
                }
            }

            break;

        case within : // Name, Lyrics, Direction

            if (leftOfStaves) {
                return TextType.Name;
            } else if (shortSentence) {
                return TextType.Direction;
            } else {
                return TextType.Lyrics;
            }

        case below :

            if (pageCentered && shortSentence && lastSystem) {
                return TextType.Rights;
            }
        }

        return null;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Nullify cached data that depends on the collection of items
     */
    private void invalidateCache ()
    {
        location = null;
        textArea = null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction minSentenceGap = new Scale.Fraction(
            20,
            "Minimum horizontal distance between two sentences on the same line");
        Scale.Fraction maxRightDx = new Scale.Fraction(
            2,
            "Maximum horizontal distance on the right end of the staff");
        Scale.Fraction maxCenterDx = new Scale.Fraction(
            30,
            "Maximum horizontal distance around center of page");
        Scale.Fraction maxShortLength = new Scale.Fraction(
            30,
            "Maximum length for a short sentence (no lyrics)");
        Scale.Fraction maxStaffDy = new Scale.Fraction(
            7,
            "Maximum distance above staff for a direction");
        Scale.Fraction minTitleHeight = new Scale.Fraction(
            3,
            "Minimum height for a title text");
    }
}
