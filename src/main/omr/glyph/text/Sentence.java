//----------------------------------------------------------------------------//
//                                                                            //
//                              S e n t e n c e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.*;
import omr.glyph.Glyph;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.math.Population;

import omr.score.Score;
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

import java.util.*;

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
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sentence.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing system */
    private final SystemInfo systemInfo;

    /** The containing system part */
    private final SystemPart systemPart;

    /** The sentence id */
    private final String id;

    /** Abscissa-ordered collection of text items */
    private final SortedSet<Glyph> items = new TreeSet<Glyph>();

    /** Role of this text sentence */
    private TextType type;

    /** Max vertical pixel distance between a text item and the sentence */
    private final int maxItemDy;

    /** Max horizontal pixel distance between a glyph item and a sentence */
    private final int maxItemDx;

    // Following are cached data, invalidated whenever items is modified -------

    /** The sentence starting point (in units) within the containing system */
    private SystemPoint location;

    /** The text area for this sentence */
    private TextArea textArea;

    /** The mean text baseline ordinate in pixels */
    private Integer y;

    /** Are we within staves height, thus impacted by first barline border */
    private Boolean withinStaves;

    /** The bounding box */
    private PixelRectangle contourBox;

    /** The string value of the sentence, if any, assigned manually or by OCR */
    private String content;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Sentence //
    //----------//
    /**
     * Creates a new Sentence object.
     *
     * @param systemInfo The containing system
     * @param glyph A first glyph that gives birth to this sentence
     * @param id A unique ID within the containing system
     */
    Sentence (SystemInfo systemInfo,
              Glyph      glyph,
              int        id)
    {
        this.systemInfo = systemInfo;
        this.id = systemInfo.getId() + ":" + id;

        addItem(glyph);

        ScoreSystem system = systemInfo.getScoreSystem();
        systemPart = system.getPartAt(
            system.toSystemPoint(items.first().getCentroid()));

        // Compute parameters one for all
        Scale scale = systemInfo.getScoreSystem()
                                .getScale();
        maxItemDy = scale.toPixels(constants.maxItemDy);
        maxItemDx = scale.toPixels(constants.maxItemDx);

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
        StringBuilder sb = null;

        // Use each item string
        for (Glyph item : items) {
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
        for (Glyph item : items) {
            item.setTranslation(entity);
        }
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the ID of this instance
     * @return the id
     */
    public String getId ()
    {
        return id;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the system-based starting point of this sentence
     * @return the starting point (x: left side, y: baseline)
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
        return systemPart;
    }

    //------------//
    // getContent //
    //------------//
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

    //---------//
    // addItem //
    //---------//
    /**
     * Add a glyph to this sentence
     * @param item the glyph to add
     * @return true if item did not already exist in the glyphs set
     */
    public boolean addItem (Glyph item)
    {
        item.getTextInfo()
            .setSentence(this);
        invalidateCache();

        return items.add(item);
    }

    //------------//
    // removeItem //
    //------------//
    /**
     * Remove the provided item from this sentence
     * @param glyph the glyph to remove
     */
    public void removeItem (Glyph glyph)
    {
        items.remove(glyph);
        invalidateCache();

        glyph.getTextInfo()
             .setSentence(null);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Sentence #");
        sb.append(getId());

        try {
            sb.append(" y:");
            getY();
            sb.append(y);
        } catch (Exception ex) {
            sb.append("unknown");
        }

        sb.append(" ")
          .append(Glyph.toString("items", items));

        if (content != null) {
            sb.append(" content:")
              .append('"')
              .append(content)
              .append('"');
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this sentence (and could
     * thus be part of it)
     * @param glyph the provided glyph to check wrt this sentence
     * @param fatBox the fat sentence contour box for the sentence
     * @return true if close enough vertically and horizontally
     */
    boolean isCloseTo (Glyph          glyph,
                       PixelRectangle fatBox)
    {
        return fatBox.contains(glyph.getAreaCenter()) &&
               !acrossEntryBarline(glyph);
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this sentence (and could
     * thus be part of it)
     * @param glyph the provided glyph to check wrt this sentence
     * @return true if close enough vertically and horizontally
     */
    boolean isCloseTo (Glyph glyph)
    {
        PixelRectangle fatBox = getContourBox();
        fatBox.grow(maxItemDx, maxItemDy);

        return isCloseTo(glyph, fatBox);
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the other sentence is close to this sentence (and could
     * thus be merged)
     * @param other the provided other sentence
     * @return true if close enough vertically and horizontally
     */
    boolean isCloseTo (Sentence other)
    {
        PixelRectangle fatBox = getContourBox();
        fatBox.grow(maxItemDx, 0);

        return fatBox.intersects(other.getContourBox()) &&
               !acrossEntryBarline(other);
    }

    //---------------//
    // includeAliens //
    //---------------//
    /**
     * Try to extend the line (which is made of only text items so far) with
     * 'alien' non-text shape items, but which together could make text lines
     */
    void includeAliens ()
    {
        Collection<Glyph> aliens = getAliens();

        // Pre-insert all candidates in proper place in the glyphs sequence
        items.addAll(aliens);

        Glyph alien;

        while ((alien = getFirstAlien()) != null) {
            if (!resolveAlien(alien)) {
                removeItem(alien);
            }
        }
    }

    //--------------------//
    // mergeEnclosedTexts //
    //--------------------//
    /**
     * If a text glyph overlaps with another text glyph, make it one glyph
     */
    void mergeEnclosedTexts ()
    {
        boolean done = false;

        while (!done) {
            done = true;

            innerLoop: 
            for (Glyph inner : items) {
                PixelRectangle innerBox = inner.getContourBox();

                for (Glyph outer : items) {
                    if (outer == inner) {
                        continue;
                    }

                    PixelRectangle outerBox = outer.getContourBox();

                    if (outerBox.intersects(innerBox)) {
                        Glyph compound = systemInfo.buildCompound(
                            Arrays.asList(outer, inner));
                        compound = systemInfo.addGlyph(compound);
                        systemInfo.computeGlyphFeatures(compound);
                        compound.setShape(Shape.TEXT);

                        addItem(compound);

                        if (outer != compound) {
                            removeItem(outer);
                        }

                        if (inner != compound) {
                            removeItem(inner);
                        }

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "System#" + systemInfo.getId() + " text#" +
                                inner.getId() + " merged with text#" +
                                outer.getId() + " to create text#" +
                                compound.getId());
                        }

                        done = false;

                        break innerLoop;
                    }
                }
            }
        }
    }

    //---------//
    // mergeOf //
    //---------//
    /**
     * Report the glyph built from the merge with the other sentence
     * @param other
     * @return the resulting glyph
     */
    Glyph mergeOf (Sentence other)
    {
        List<Glyph> allGlyphs = new ArrayList<Glyph>(items);
        allGlyphs.addAll(other.items);

        return systemInfo.buildCompound(allGlyphs);
    }

    //-----------//
    // recognize //
    //-----------//
    void recognize ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(this + " recognize");
        }

        // Make sure these parameters are computed
        getTextType();
        getTextHeight();

        if (logger.isFineEnabled()) {
            logger.fine(this + " type:" + type + " height:" + getTextHeight());
        }

        // Default language
        Score  score = systemInfo.getScoreSystem()
                                 .getScore();
        String language = score.getLanguage();

        if (language == null) {
            language = constants.defaultLanguageCode.getValue();
        }

        Glyph glyph = null;

        if (items.size() > 1) {
            Glyph compound = systemInfo.buildCompound(items);
            glyph = systemInfo.addGlyph(compound);
            glyph.setShape(Shape.TEXT, Evaluation.ALGORITHM);
            items.clear();
            items.add(glyph);
        } else {
            glyph = items.first();
        }

        if (glyph.getTextInfo()
                 .getContent() == null) {
            try {
                content = TesseractOCR.getInstance()
                                      .recognize(glyph.getImage(), language)
                                      .get(0);
                logger.info(this.toString());
                glyph.getTextInfo()
                     .setOcrContent(content);
            } catch (Exception ex) {
                logger.warning("OCR error with glyph #" + glyph.getId(), ex);
            }
        }

        addItem(glyph);
    }

    //-----------//
    // getAliens //
    //-----------//
    /**
     * Gather all the collection of non-text glyphs that could actually be part
     * of the final text line
     * @return the found collection of (non text assigned, yet compatible)
     * candidates
     */
    private Collection<Glyph> getAliens ()
    {
        final Collection<Glyph> candidates = new ArrayList<Glyph>();
        final PixelRectangle    fatBox = getContourBox();
        fatBox.grow(maxItemDx, maxItemDy);

        // Check alien glyphs aligned with this line
        for (Glyph glyph : systemInfo.getGlyphs()) {
            Shape shape = glyph.getShape();

            // Be rather permissive regarding shape
            if (!glyph.isKnown() ||
                (!glyph.isManualShape() &&
                ((shape == Shape.CLUTTER) || (shape == Shape.DOT) ||
                (shape == Shape.COMBINING_STEM) || (shape == Shape.WHOLE_NOTE) ||
                (shape == Shape.STRUCTURE)))) {
                // Check that sentence fat box contains the alien glyph
                if (isCloseTo(glyph, fatBox)) {
                    candidates.add(glyph);
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(this + " " + Glyph.toString("aliens", candidates));
        }

        return candidates;
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Report the sentence contour, as the union of contours of all its items
     * @return the global sentence contour
     */
    private PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            for (Glyph item : items) {
                if (contourBox == null) {
                    contourBox = new PixelRectangle(item.getContourBox());
                } else {
                    contourBox.add(item.getContourBox());
                }
            }
        }

        if (contourBox != null) {
            return new PixelRectangle(contourBox); // Return a copy (safer...)
        } else {
            return null;
        }
    }

    //---------------//
    // getFirstAlien //
    //---------------//
    /**
     * Report the first alien glyph in the current sequence of line items
     * @return the first occurrence of non-text glyph in the line items
     */
    private Glyph getFirstAlien ()
    {
        for (Glyph glyph : items) {
            if ((glyph.getShape() != Shape.NO_LEGAL_SHAPE) && !glyph.isText()) {
                return glyph;
            }
        }

        return null;
    }

    //--------------//
    // getItemAfter //
    //--------------//
    /**
     * Report the item right after the provided one in the sequence of line
     * items
     * @param start the item whose successor is desired, or null if the very
     * first item is desired
     * @return the item that follow 'start', or null if none exists
     */
    private Glyph getItemAfter (Glyph start)
    {
        boolean started = start == null;

        for (Glyph glyph : items) {
            if (started) {
                return glyph;
            }

            if (glyph == start) {
                started = true;
            }
        }

        return null;
    }

    //---------------//
    // getItemBefore //
    //---------------//
    /**
     * Report the item right before the provided stop item in the line sequence
     * of items
     * @param stop the item whose preceding instance is desired
     * @return the very last item found before the 'stop' item, or null
     */
    private Glyph getItemBefore (Glyph stop)
    {
        Glyph prev = null;

        for (Glyph glyph : items) {
            if (glyph == stop) {
                return prev;
            }

            prev = glyph;
        }

        return prev;
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
                systemInfo,
                null,
                systemInfo.getSheet().getVerticalLag().createAbsoluteRoi(
                    getContourBox()),
                new HorizontalOrientation());
        }

        return textArea;
    }

    //----------------//
    // isWithinStaves //
    //----------------//
    private boolean isWithinStaves ()
    {
        if (withinStaves == null) {
            if (!items.isEmpty()) {
                ScoreSystem system = systemInfo.getScoreSystem();
                SystemPoint first = system.toSystemPoint(
                    items.first().getLocation());
                withinStaves = system.getStaffPosition(first) == StaffPosition.within;
            }
        }

        return withinStaves;
    }

    //------//
    // getY //
    //------//
    /**
     * Report the baseline pixel ordinate of this line
     *
     * @return the mean line pixel ordinate, or null (this is the case when the
     * line contains no glyph)
     */
    private int getY ()
    {
        if (y == null) {
            Population population = new Population();

            for (Glyph item : items) {
                population.includeValue(item.getLocation().y);
            }

            if (population.getCardinality() > 0) {
                y = (int) Math.rint(population.getMeanValue());
            }
        }

        return y;
    }

    //--------------------//
    // acrossEntryBarline //
    //--------------------//
    /**
     * Check whether this and the other sentence are separated by the border of
     * the entry barline
     * @param other the other sentence
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (Sentence other)
    {
        if (!isWithinStaves() && !other.isWithinStaves()) {
            return false;
        }

        ScoreSystem system = systemInfo.getScoreSystem();
        SystemPoint itemPt = system.toSystemPoint(items.first().getLocation());
        SystemPoint otherPt = system.toSystemPoint(
            other.items.first().getLocation());

        return system.isLeftOfStaves(itemPt) != system.isLeftOfStaves(otherPt);
    }

    //--------------------//
    // acrossEntryBarline //
    //--------------------//
    /**
     * Check whether the provided glyph and this sentence are separated by the
     * border of the entry barline
     * @param glyph the provided glyph
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (Glyph glyph)
    {
        if (!isWithinStaves()) {
            return false;
        }

        ScoreSystem system = systemInfo.getScoreSystem();
        SystemPoint itemPt = system.toSystemPoint(items.first().getLocation());
        SystemPoint glyphPt = system.toSystemPoint(glyph.getLocation());

        return system.isLeftOfStaves(itemPt) != system.isLeftOfStaves(glyphPt);
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
        int     staffDy = Math.abs(staff.getPageTopLeft().y - pageBox.y);
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
        y = null;
        withinStaves = null;
        contourBox = null;
        content = null;
    }

    //--------------//
    // resolveAlien //
    //--------------//
    /**
     * Make every possible effort to include the provided candidate as a true
     * member of the line collection of items.
     * @param candidate the candidate (non-text) glyph
     * @return true if successful, false otherwise
     */
    private boolean resolveAlien (Glyph alien)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Resolving alien #" + alien.getId());
        }

        // Going both ways, stopping at line ends
        Glyph first = alien;

        while (first != null) {
            Glyph last = alien;

            // Extending to the end
            while (last != null) {
                // Test on crossing the barline
                if (!acrossEntryBarline(last)) {
                    Merge merge = new Merge(first, last);

                    if (merge.isOk()) {
                        merge.insert();

                        Glyph glyph = merge.compound;

                        if (glyph.getId() == 0) {
                            glyph = systemInfo.addGlyph(glyph);
                        }

                        systemInfo.computeGlyphFeatures(glyph);
                        glyph.setShape(merge.vote.shape, merge.vote.doubt);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Candidate #" + alien.getId() +
                                " solved from #" + first.getId() + " to " +
                                last.getId() + " as #" +
                                merge.compound.getId());
                        }

                        return true;
                    }
                }

                last = getItemAfter(last);
            }

            // Extending to the beginning
            first = getItemBefore(first);
        }

        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction  maxItemDy = new Scale.Fraction(
            0,
            "Maximum vertical distance between a text line and a text item");
        Scale.Fraction  maxItemDx = new Scale.Fraction(
            20,
            "Maximum horizontal distance between an alien and a text item");
        Scale.Fraction  maxRightDx = new Scale.Fraction(
            2,
            "Maximum horizontal distance on the right end of the staff");
        Scale.Fraction  maxCenterDx = new Scale.Fraction(
            30,
            "Maximum horizontal distance around center of page");
        Scale.Fraction  maxShortLength = new Scale.Fraction(
            30,
            "Maximum length for a short sentence (no lyrics)");
        Scale.Fraction  maxStaffDy = new Scale.Fraction(
            7,
            "Maximum distance above staff for a direction");
        Scale.Fraction  minTitleHeight = new Scale.Fraction(
            3,
            "Minimum height for a title text");
        Constant.String defaultLanguageCode = new Constant.String(
            "deu",
            "3-letter code for the default sheet language");
    }

    //-------//
    // Merge //
    //-------//
    private class Merge
    {
        //~ Instance fields ----------------------------------------------------

        private List<Glyph> parts = new ArrayList<Glyph>();
        private Glyph       compound;
        private Evaluation  vote;

        //~ Constructors -------------------------------------------------------

        public Merge (Glyph first,
                      Glyph last)
        {
            boolean started = false;

            for (Glyph glyph : items) {
                if (glyph == first) {
                    started = true;
                }

                if (started) {
                    parts.add(glyph);
                }

                if (glyph == last) {
                    break;
                }
            }
        }

        //~ Methods ------------------------------------------------------------

        public boolean isOk ()
        {
            if (parts.size() > 1) {
                compound = systemInfo.buildCompound(parts);
            } else if (parts.size() == 1) {
                compound = parts.get(0);
            } else {
                compound = null;

                return false;
            }

            Evaluator evaluator = GlyphNetwork.getInstance();

            vote = evaluator.vote(compound, GlyphInspector.getTextMaxDoubt());

            return (vote != null) && vote.shape.isText();
        }

        public Merge add (Glyph glyph)
        {
            parts.add(glyph);
            compound = null;

            return this;
        }

        public void insert ()
        {
            items.removeAll(parts);
            items.add(compound);
        }

        public Merge remove (Glyph glyph)
        {
            parts.remove(glyph);
            compound = null;

            return this;
        }
    }
}
