//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t L i n e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.BasicRoi;
import omr.lag.Section;

import omr.log.Logger;

import omr.math.Population;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.ScoreSystem;
import omr.score.entity.ScoreSystem.StaffPosition;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Navigable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>TextLine</code> encapsulates a growing ordered set of text
 * glyphs (loosely similar to words) that represents a whole expression.
 *
 * <p>Since TextLine instances are recreated from scratch in the PATTERNS step,
 * whereas glyphs never die, content and role information should be stored in
 * the TextInfo of their first and only glyph.
 *
 * <h4>Textual glyph Data Model:<br/>
 *    <img src="doc-files/TextLine.jpg"/>
 * </h4>
 *
 * <h4>Processing of textual glyphs:<br/>
 *    <img src="doc-files/TextProcessing.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
public class TextLine
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextLine.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing system */
    @Navigable(false)
    private final SystemInfo system;

    /** Abscissa-ordered collection of text glyphs */
    private final SortedSet<Glyph> glyphs = Glyphs.sortedSet();

    // Scaled parameters
    //--------------------------------------------------------------------------

    /** Max vertical pixel distance between a text item and the text line */
    private final int maxItemDy;

    /** Max horizontal pixel distance between a glyph item and a text line */
    private final int maxItemDx;

    /** Maximum vertical pixel distance between two text line chunks */
    private final int maxTextLineDy;

    // Cached data, invalidated whenever items are modified
    //--------------------------------------------------------------------------

    /** The text line starting point */
    private PixelPoint location;

    /** The text area for this text line */
    private TextArea textArea;

    /** The mean text baseline ordinate in pixels */
    private Integer y;

    /** Are we within staves height, thus impacted by first barline border */
    private Boolean withinStaves;

    /** The bounding box */
    private PixelRectangle contourBox;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // TextLine //
    //----------//
    /**
     * Creates a new TextLine object.
     *
     * @param systemInfo The containing system
     * @param glyph A first glyph that gives birth to this text line
     */
    public TextLine (SystemInfo systemInfo,
                     Glyph      glyph)
    {
        this.system = systemInfo;

        addGlyph(glyph);

        // Compute parameters once for all
        Scale scale = systemInfo.getScoreSystem()
                                .getScale();
        maxItemDy = scale.toPixels(constants.maxItemDy);
        maxTextLineDy = scale.toPixels(constants.maxTextLineDy);

        // Inter-word gap is different if we are between staves (lyrics ...)
        // or if we are on system peripheral regions
        ScoreSystem   scoreSystem = systemInfo.getScoreSystem();
        PixelPoint    center = glyphs.first()
                                     .getCentroid();
        StaffPosition staffPosition = scoreSystem.getStaffPosition(center);

        if (staffPosition == StaffPosition.WITHIN_STAVES) {
            maxItemDx = scale.toPixels(constants.maxLyricItemDx);
        } else {
            maxItemDx = scale.toPixels(constants.maxItemDx);
        }

        if (logger.isFineEnabled()) {
            logger.fine("Created " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getFirstGlyph //
    //---------------//
    public Glyph getFirstGlyph ()
    {
        return glyphs.first();
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size of this text line
     * @return the font size
     */
    public Float getFontSize ()
    {
        return glyphs.first()
                     .getTextInfo()
                     .getFontSize();
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the x-ordered collection of glyphs in this text line
     * @return the collection of glyphs (words generally)
     */
    public SortedSet<Glyph> getGlyphs ()
    {
        return glyphs;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the system-based starting point of this text line
     * @return the starting point (x: left side, y: baseline)
     */
    public PixelPoint getLocation ()
    {
        if (location == null) {
            PixelRectangle firstBox = glyphs.first()
                                            .getContourBox();
            location = new PixelPoint(firstBox.x, getTextArea().getBaseline());
        }

        return location;
    }

    //------------------//
    // getSystemContour //
    //------------------//
    /**
     * Report the rectangular contour of this text line
     * @return the text line contour
     */
    public PixelRectangle getSystemContour ()
    {
        return getContourBox();
    }

    //----------------//
    // getTextContent //
    //----------------//
    /**
     * Report the string content of this text line, as computed by OCR or entered
     * manually for example
     * @return the text interpretation of the text line
     */
    public String getTextContent ()
    {
        return glyphs.first()
                     .getTextInfo()
                     .getContent();
    }

    //---------------//
    // getTextHeight //
    //---------------//
    /**
     * Determine the uniform character height for the whole text line
     * @return the standard character height in pixels
     */
    public int getTextHeight ()
    {
        return getTextArea()
                   .getBaseline() - getTextArea()
                                        .getMedianLine();
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a glyph to this text line
     * @param glyph the glyph to add
     * @return true if glyph did not already exist in the glyphs set
     */
    public final boolean addGlyph (Glyph glyph)
    {
        invalidateCache();

        return glyphs.add(glyph);
    }

    //------------------//
    // extractSentences //
    //------------------//
    /**
     * Retrieve the text sentences out of this text line, we usually get one
     * sentence per text line, sometimes none, sometimes several.
     * <p>We use the OCR utility to assign a content to this text line if
     * needed, and we check the validity of this OCR'ed content WRT the
     * underlying glyph.</p>
     * @param language the current language
     */
    public List<Sentence> extractSentences (String language)
    {
        // Sentences created from this text line
        List<Sentence> sentences = new ArrayList<Sentence>();

        // Make sure these parameters are computed
        getTextHeight();

        // The (only) glyph for the text line
        Glyph glyph;

        if (glyphs.size() > 1) {
            Glyph compound = system.buildTransientCompound(glyphs);

            // Check that this glyph is not forbidden as text
            if (compound.isShapeForbidden(Shape.TEXT)) {
                return sentences;
            }

            glyph = system.addGlyph(compound);
            glyph.setShape(Shape.TEXT, Evaluation.ALGORITHM);
            glyphs.clear();
            addGlyph(glyph);
        } else {
            glyph = glyphs.first();

            if (!glyph.isActive()) {
                glyph = system.addGlyph(glyph);
            }
        }

        TextInfo textInfo = glyph.getTextInfo();

        // Use OCR only if no text has been manually defined for the line, and
        // if we have no content or if a new language is being used for OCR
        if (useOCR() &&
            (textInfo.getManualContent() == null) &&
            ((textInfo.getOcrContent() == null) ||
            !language.equals(textInfo.getOcrLanguage()))) {
            sentences.addAll(callLineOcr(glyph, language));
        } else {
            // Use the existing content
            sentences.add(createSentence(glyph));
        }

        return sentences;
    }

    //---------//
    // mergeOf //
    //---------//
    /**
     * Report the glyph built from the merge with the other text line
     * @param other
     * @return the resulting glyph
     */
    public Glyph mergeOf (TextLine other)
    {
        List<Glyph> allGlyphs = new ArrayList<Glyph>(glyphs);
        allGlyphs.addAll(other.glyphs);

        return system.buildTransientCompound(allGlyphs);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{TextLine");

        try {
            sb.append(" y:");
            getY();
            sb.append(y);
        } catch (Exception ex) {
            sb.append("unknown");
        }

        sb.append(" ")
          .append(Glyphs.toString(glyphs));

        if (getFontSize() != null) {
            sb.append(" size:")
              .append(getFontSize());
        }

        if (getTextContent() != null) {
            sb.append(" content:")
              .append('"')
              .append(getTextContent())
              .append('"');
        }

        sb.append("}");

        return sb.toString();
    }

    //--------//
    // useOCR //
    //--------//
    /**
     * Can we use the OCR companion program?
     * @return true if the allow access to the OCR program (Tesseract)
     */
    public static boolean useOCR ()
    {
        return constants.useOCR.getValue();
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this text line (and could
     * thus be part of it)
     * @param glyph the provided glyph to isValid wrt this text line
     * @return true if close enough vertically and horizontally
     */
    public boolean isCloseTo (Glyph glyph)
    {
        PixelRectangle fatBox = getContourBox();
        fatBox.grow(maxItemDx, maxItemDy);

        return isCloseTo(glyph, fatBox);
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the other text line is close to this text line (and could
     * thus be merged)
     * @param other the provided other text line
     * @return true if close enough vertically and horizontally
     */
    public boolean isCloseTo (TextLine other)
    {
        PixelRectangle fatBox = getContourBox();
        fatBox.grow(maxItemDx, 0);

        return fatBox.intersects(other.getContourBox()) &&
               (Math.abs(other.getY() - this.getY()) <= maxTextLineDy) &&
               !acrossEntryBarline(other);
    }

    //---------------//
    // includeAliens //
    //---------------//
    /**
     * Try to extend the line (which is made of only text items so far) with
     * 'alien' non-text shape items, but which together could make text lines
     */
    public void includeAliens ()
    {
        Collection<Glyph> aliens = getAliens();

        // Pre-insert all candidates in proper place in the glyphs sequence
        glyphs.addAll(aliens);

        // It is safer to use an upper limit to the following loop
        int   aliensCount = aliens.size();

        Glyph alien;

        while (((alien = getFirstAlien()) != null) && (--aliensCount >= -1)) {
            // TODO
            //            if (aliensCount < 0) {
            //                logger.severe("includeAliens: endless loop for " + this);
            //            }
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
    public void mergeEnclosedTexts ()
    {
        boolean done = false;

        while (!done) {
            done = true;

            innerLoop: 
            for (Glyph inner : glyphs) {
                PixelRectangle innerBox = inner.getContourBox();

                for (Glyph outer : glyphs) {
                    if (outer == inner) {
                        continue;
                    }

                    PixelRectangle outerBox = outer.getContourBox();

                    if (outerBox.intersects(innerBox)) {
                        Glyph compound = system.buildTransientCompound(
                            Arrays.asList(outer, inner));
                        compound = system.addGlyph(compound);
                        compound.setShape(Shape.TEXT);

                        addGlyph(compound);

                        if (outer != compound) {
                            removeItem(outer);
                        }

                        if (inner != compound) {
                            removeItem(inner);
                        }

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "System#" + system.getId() + " text#" +
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

    //----------------//
    // splitIntoWords //
    //----------------//
    /**
     * Split the long glyph of this (Lyrics) text line into word glyphs
     */
    public void splitIntoWords ()
    {
        // Make sure the split hasn't been done yet
        if (glyphs.size() > 1) {
            return;
        } else if (glyphs.isEmpty()) {
            logger.severe("splitIntoWords. TextLine with no items: " + this);
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("Splitting lyrics of " + this);
            }

            Collection<Glyph> words = glyphs.first()
                                            .getTextInfo()
                                            .splitIntoWords();

            // Replace the single long item by this collection of word items
            if ((words != null) && !words.isEmpty()) {
                glyphs.clear();
                glyphs.addAll(words);
            }
        }
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
        for (Glyph glyph : system.getGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == Shape.GLYPH_PART) ||
                glyphs.contains(glyph) ||
                glyph.isShapeForbidden(Shape.TEXT)) {
                continue;
            }

            // Be rather permissive regarding shape
            if (!glyph.isKnown() ||
                (!glyph.isManualShape() ||
                ((shape == Shape.TEXT) || (shape == Shape.CHARACTER)))) {
                // Check that text line fat box is close to the alien glyph
                if (isCloseTo(glyph, fatBox)) {
                    candidates.add(glyph);
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(this + " " + Glyphs.toString("aliens", candidates));
        }

        return candidates;
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this text line (and could
     * thus be part of it)
     * @param glyph the provided glyph to check wrt this text line
     * @param fatBox the fat text line contour box for the text line
     * @return true if close enough vertically and horizontally
     */
    private boolean isCloseTo (Glyph          glyph,
                               PixelRectangle fatBox)
    {
        return fatBox.contains(glyph.getAreaCenter()) &&
               !acrossEntryBarline(glyph);
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Report the text line contour, as the union of contours of all its items
     * @return the global text line contour
     */
    private PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            for (Glyph item : glyphs) {
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
        for (Glyph glyph : glyphs) {
            if ((glyph.getShape() != Shape.GLYPH_PART) && !glyph.isText()) {
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

        for (Glyph glyph : glyphs) {
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

        for (Glyph glyph : glyphs) {
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
     * text line contour, so that textual characteristics, such as baseline or
     * x-height can be computed
     * @return the related text area
     */
    private TextArea getTextArea ()
    {
        if (textArea == null) {
            textArea = new TextArea(
                system,
                null,
                new BasicRoi(getContourBox()),
                Orientation.HORIZONTAL);
        }

        return textArea;
    }

    //----------------//
    // isWithinStaves //
    //----------------//
    private boolean isWithinStaves ()
    {
        if (withinStaves == null) {
            if (!glyphs.isEmpty()) {
                PixelPoint first = glyphs.first()
                                         .getLocation();
                withinStaves = system.getScoreSystem()
                                     .getStaffPosition(first) == StaffPosition.WITHIN_STAVES;
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

            for (Glyph item : glyphs) {
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
     * Check whether this and the other text line are separated by the border of
     * the entry barline
     * @param other the other text line
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (TextLine other)
    {
        if (!isWithinStaves() && !other.isWithinStaves()) {
            return false;
        }

        PixelPoint  itemPt = glyphs.first()
                                   .getLocation();
        PixelPoint  otherPt = other.glyphs.first()
                                          .getLocation();
        ScoreSystem scoreSystem = system.getScoreSystem();

        return scoreSystem.isLeftOfStaves(itemPt) != scoreSystem.isLeftOfStaves(
            otherPt);
    }

    //--------------------//
    // acrossEntryBarline //
    //--------------------//
    /**
     * Check whether the provided glyph and this text line are separated by the
     * border of the entry barline
     * @param glyph the provided glyph
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (Glyph glyph)
    {
        if (!isWithinStaves()) {
            return false;
        }

        PixelPoint  itemPt = glyphs.first()
                                   .getLocation();
        PixelPoint  glyphPt = glyph.getLocation();
        ScoreSystem scoreSystem = system.getScoreSystem();

        return scoreSystem.isLeftOfStaves(itemPt) != scoreSystem.isLeftOfStaves(
            glyphPt);
    }

    //-------------//
    // callLineOcr //
    //-------------//
    /**
     * Report the sentence(s) created from the use of OCR on the provided glyph
     * @param glyph the provided glyph
     * @param language the current language
     * @return the collection (perhaps empty) of retrieved sentences
     */
    private List<Sentence> callLineOcr (Glyph  glyph,
                                        String language)
    {
        /** Initial collection of sections */
        SortedSet<Section> allSections = new TreeSet<Section>(
            glyph.getMembers());
        TextInfo           textInfo = glyph.getTextInfo();
        List<OcrLine>      lines = Language.getOcr()
                                           .recognize(
            glyph.getImage(),
            language,
            "g" + glyph.getId() + ".");

        ///OCR logger.warning("Texte OCR " + glyph + " " + lines);
        List<Sentence>     sentences = new ArrayList<Sentence>();

        if ((lines != null) && !lines.isEmpty()) {
            for (OcrLine ocrLine : lines) {
                // Convert from glyph-based to absolute coordinates
                ocrLine.translate(
                    glyph.getContourBox().x,
                    glyph.getContourBox().y);

                // Isolate proper line glyph from its enclosed sections
                SortedSet<Section> sections = textInfo.retrieveSectionsFrom(
                    ocrLine.getChars());

                if (!sections.isEmpty()) {
                    allSections.removeAll(sections);

                    Glyph lineGlyph = system.buildTransientGlyph(sections);

                    // Validate ocr content
                    if (OcrTextVerifier.isValid(lineGlyph, ocrLine)) {
                        lineGlyph = system.addGlyph(lineGlyph);
                        lineGlyph.setShape(Shape.TEXT);

                        // Build the TextInfo for this glyph
                        TextInfo ti = lineGlyph.getTextInfo();
                        ti.setOcrInfo(language, ocrLine);

                        // Allocate a text line for this glyph
                        sentences.add(createSentence(lineGlyph));

                        // Free all the glyphs pointed by sections left over
                        for (Section section : allSections) {
                            Glyph g = section.getGlyph();

                            if ((g != null) && (g.getShape() != null)) {
                                g.setShape(null);
                            }
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Invalid line " + ocrLine + " " + glyph);
                        }

                        glyph.setShape(null);
                        glyph.forbidShape(Shape.TEXT);
                        sentences.clear();

                        return sentences;
                    }
                }
            }
        } else {
            logger.fine("No OCR line for glyph #" + glyph.getId());
            glyph.setShape(null);
            glyph.forbidShape(Shape.TEXT);
        }

        return sentences;
    }

    //----------------//
    // createSentence //
    //----------------//
    private Sentence createSentence (Glyph glyph)
    {
        Sentence sentence = new Sentence(
            system,
            new TextLine(system, glyph),
            system.getNewSentenceId());
        TextInfo ti = glyph.getTextInfo();
        ti.setSentence(sentence);

        return sentence;
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
    }

    //------------//
    // removeItem //
    //------------//
    /**
     * Remove the provided item from this text line
     * @param glyph the glyph to remove
     */
    private void removeItem (Glyph glyph)
    {
        glyphs.remove(glyph);
        invalidateCache();

        //        glyph.getTextInfo()
        //             .setTextLine(null);
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
                        system.computeGlyphFeatures(glyph);
                        glyph = system.addGlyph(glyph);
                        glyph.setEvaluation(merge.vote);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Candidate #" + alien.getId() +
                                " solved from #" + first.getId() + " to #" +
                                last.getId() + " as #" + glyph.getId());
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

        Scale.Fraction   maxItemDy = new Scale.Fraction(
            0,
            "Maximum vertical distance between a text line and a text item");
        Scale.Fraction   maxItemDx = new Scale.Fraction(
            5,
            "Maximum horizontal distance between two words");
        Scale.Fraction   maxLyricItemDx = new Scale.Fraction(
            20,
            "Maximum horizontal distance between two (lyric) words");
        Scale.Fraction   maxTextLineDy = new Scale.Fraction(
            0.6,
            "Maximum vertical distance between two text line chunks");
        Constant.Boolean useOCR = new Constant.Boolean(
            true,
            "Should we use the OCR feature?");
    }

    //-------//
    // Merge //
    //-------//
    /**
     * This utility class tries to operate a merge of all text line items from
     *  a first item to a last item inclusive, while checking whether the
     * resulting compound glyph would be assigned a TEXT shape.
     */
    private class Merge
    {
        //~ Instance fields ----------------------------------------------------

        private List<Glyph> parts = new ArrayList<Glyph>();
        private Glyph       compound;
        private Evaluation  vote;

        //~ Constructors -------------------------------------------------------

        /**
         * Remember the sequence of text line items between first and last
         * @param first beginning of the sequence
         * @param last end of the sequence
         */
        public Merge (Glyph first,
                      Glyph last)
        {
            boolean started = false;

            for (Glyph glyph : glyphs) {
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

        /**
         * Check whether the current sequence could be assigned the TEXT shape
         * @return true if evaluated as TEXT
         */
        public boolean isOk ()
        {
            if (parts.size() > 1) {
                compound = system.buildTransientCompound(parts);
            } else if (parts.size() == 1) {
                compound = parts.get(0);
            } else {
                compound = null;

                return false;
            }

            GlyphEvaluator evaluator = GlyphNetwork.getInstance();
            vote = evaluator.vote(compound, Grades.textMinGrade, system);

            return (vote != null) && vote.shape.isText();
        }

        /**
         * Replace, in the TextLine set of items, the sequence from first to
         * last by the text compound.
         */
        public void insert ()
        {
            glyphs.removeAll(parts);
            glyphs.add(compound);
        }
    }
}
