//----------------------------------------------------------------------------//
//                                                                            //
//                              S e n t e n c e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.BasicGlyphChain;
import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeEvaluator;
import static omr.glyph.ShapeSet.*;
import omr.glyph.facets.Glyph;
import static omr.glyph.text.Sentence.Stripe.Kind.*;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.log.Logger;

import omr.math.Population;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemNode.StaffPosition;
import omr.score.entity.SystemPart;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.Scale;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.util.Navigable;
import omr.util.Vip;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code Sentence} encapsulates an ordered set of text glyphs
 * (loosely similar to words) that represents a whole text expression.
 *
 * <p>Life cycle: <ol>
 * <li>A "physical" sentence instance can be created and expanded by gradually
 * adding glyphs which are geometrically compatible with the current collection
 * of glyphs.
 * Such physical instance plays the role of a collection of glyphs.</li>
 * <li>One or several "logical" sentence instances can be created from as many
 * OcrLine instances returned by the OCR running on the compound glyph of a
 * physical sentence.
 * Only the logical instances are kept for translation to score entities.
 * </li>
 * </ol>
 * Each such logical instance is created with a single underlying glyph
 * built out of the corresponding sections.
 * In the special case of a (long) Lyrics sentence instance, the underlying
 * compound is actually decomposed in many "syllable" glyphs, to allow precise
 * syllable positioning.
 *
 * @author Hervé Bitteur
 */
public class Sentence
        extends BasicGlyphChain
        implements Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sentence.class);

    /** To sort sentences by increasing ordinate */
    public static final Comparator<Sentence> ordinateComparator = new Comparator<Sentence>()
    {

        @Override
        public int compare (Sentence o1,
                            Sentence o2)
        {
            return Double.compare(o1.getY(), o2.getY());
        }
    };

    /** Prefix for ids of Lyrics attachments */
    static final String ATT_PREFIX = "Lyrics-";

    /** Shapes excluded for Lyrics candidates. */
    private static final EnumSet<Shape> lyricsExclusion = EnumSet.copyOf(
            shapesOf(
            shapesOf(SLUR, FERMATA, FERMATA_BELOW, STEM),
            shapesOf(
            NoteHeads.getShapes(),
            Barlines.getShapes(),
            Beams.getShapes())));

    /** Shapes excluded for aliens candidates. */
    private static final EnumSet<Shape> alienExclusion = EnumSet.copyOf(
            shapesOf(shapesOf(BRACE, BRACKET), shapesOf(StemSymbols)));

    /** For comparing sentences according to their decreasing weight */
    public static final Comparator<Sentence> reverseWeightComparator = new Comparator<Sentence>()
    {

        @Override
        public int compare (Sentence o1,
                            Sentence o2)
        {
            return o2.getCompound().getWeight() - o1.getCompound().getWeight();
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** The containing system */
    @Navigable(false)
    protected final SystemInfo system;

    /** Unique Id within the containing systm. */
    protected final String id;

    /** VIP flag */
    private boolean vip;

    /** Flag to remember processing has been done */
    private boolean processed = false;

    /** The containing system part */
    @Navigable(false)
    protected final SystemPart systemPart;

    /** Series of 3 horizontal Stripes. */
    private final Map<Stripe.Kind, Stripe> stripes = new EnumMap(
            Stripe.Kind.class);

    /** Related staff */
    private StaffInfo staff;

    // Scaled parameters
    //--------------------------------------------------------------------------
    /** Max vertical pixel distance between a text item and the sentence */
    private final int maxItemDy;

    /** Max horizontal pixel distance between a glyph item and a sentence */
    private final int maxItemDx;

    /** Maximum vertical pixel distance between two sentence chunks */
    private final int maxTextLineDy;

    /** Max ordinate delta between a lyrics item and its lyrics line */
    private final int maxLyricsDy;

    /** Min x-height for representative text item */
    private final int minTextHeight;

    // Cached data, invalidated whenever items are modified
    //--------------------------------------------------------------------------
    /** The sentence starting point */
    private PixelPoint location;

    /** The mean text baseline deskewed ordinate */
    private Double dskY;

    /** Are we within staves height, thus impacted by first barline border */
    private Boolean withinStaves;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // Sentence //
    //----------//
    /**
     * Creates a new Sentence object, with the whole sequence of glyphs.
     *
     * @param glyphs The sequence of glyphs, which cannot be null or empty.
     */
    public Sentence (List<Glyph> glyphs)
    {
        super(glyphs);

        Glyph first = glyphs.get(0);

        this.system = first.getSystem();

        id = system.getId() + "." + system.getNewSentenceId();

        PixelPoint center = first.getCentroid();

        // Compute parameters once for all
        Scale scale = system.getScoreSystem().getScale();
        maxItemDy = scale.toPixels(constants.maxItemDy);
        maxTextLineDy = scale.toPixels(constants.maxTextLineDy);
        maxLyricsDy = scale.toPixels(constants.maxLyricsDy);
        minTextHeight = scale.toPixels(constants.minTextHeight);

        // Inter-word gap is different if we are between staves (lyrics ...)
        // or if we are on system peripheral regions
        ScoreSystem scoreSystem = system.getScoreSystem();
        SystemPart part = scoreSystem.getPartAt(center);
        StaffPosition systemPosition = scoreSystem.getStaffPosition(center);
        StaffPosition partPosition = part.getStaffPosition(center);

        if ((systemPosition == StaffPosition.WITHIN_STAVES)
                && (partPosition == StaffPosition.BELOW_STAVES)) {
            maxItemDx = scale.toPixels(constants.maxLyricItemDx);
        } else {
            maxItemDx = scale.toPixels(constants.maxItemDx);
        }

        // Choose carefully Staff (& then Part )
        Staff staff = scoreSystem.getTextStaff(first.getTextRole(), center);
        systemPart = staff.getPart();

        logger.fine("Multi-glyph {0}", this);
    }

    //----------//
    // Sentence //
    //----------//
    /**
     * Creates a new Sentence object with a collection of glyphs.
     *
     * @param glyphs the collection of glyphs
     */
    public Sentence (Glyph... glyphs)
    {
        this(Arrays.asList(glyphs));
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // buildStripes //
    //--------------//
    /**
     * Build the ascent / core / descent stripes for this sentence.
     * This is meant to intersect potential candidates.
     */
    public void buildStripes ()
    {
        Skew skew = system.getSkew();

        // Mean x-height
        int xHeight = getMeanTextHeight();

        ///logger.info("xHeight: " + xHeight);

        // Inferred ascender height
        int ascent = (int) Math.rint(
                xHeight * constants.ascentRatio.getValue());

        // Inferred descender height
        int descent = (int) Math.rint(
                xHeight * constants.descentRatio.getValue());

        // Baseline extrema: left and right
        Glyph first = getFirstItem();
        Glyph last = getLastItem();
        Point left = first.getLocation();

        ///logger.info("left: " + left + " from " + first.idString());
        int toSide = system.getLeft() - left.x;
        left.x += toSide;
        left.y += (int) Math.rint(skew.getSlope() * toSide);

        Point right = last.getLocation();
        ///logger.info("right: " + right + " from " + last.idString());
        toSide = system.getRight() - right.x;
        right.x += toSide;
        right.y += (int) Math.rint(skew.getSlope() * toSide);

        // Allocate stripes
        staff = system.getStaffAt(first.getLocation());
        stripes.put(
                ASCENT,
                new Stripe(
                ASCENT,
                getId(),
                staff,
                new Point(left.x, left.y - xHeight - ascent),
                new Point(right.x, right.y - xHeight - ascent),
                ascent));

        stripes.put(
                CORE,
                new Stripe(
                CORE,
                getId(),
                staff,
                new Point(left.x, left.y - xHeight),
                new Point(right.x, right.y - xHeight),
                xHeight));
        stripes.put(
                DESCENT,
                new Stripe(DESCENT, getId(), staff, left, right, descent));
    }

    //-------------------//
    // checkCommonGlyphs //
    //-------------------//
    /**
     * Check for common glyphs between this and that instances.
     * Try to keep core glyphs.
     * Use also glyphs from ascent or descent only if they do not collide
     * with the other line
     *
     * @param that the other (following)instance
     */
    public void checkCommonGlyphs (Sentence that)
    {
        List<Glyph> common = new ArrayList<>();
        common.addAll(this.getStripe(DESCENT).intersected);
        common.retainAll(that.getStripe(ASCENT).intersected);

        if (!common.isEmpty()) {
            logger.fine("{0}&{1}{2}", new Object[]{getId(), that.getId(),
                                                   Glyphs.toString(" common:",
                                                                   common)});
            this.getStripe(DESCENT).intersected.removeAll(common);
            that.getStripe(ASCENT).intersected.removeAll(common);
        }
    }

    //-----------------//
    // extractLogicals //
    //-----------------//
    /**
     * Retrieve the logical sentences out of this physical sentence.
     * We usually get one logical per physical, sometimes none,
     * and several when the physical glyph contains several text lines.
     * <p>We use the OCR utility to assign a content to this sentence if
     * needed, and we check the validity of this OCR'ed content WRT the
     * underlying glyph.</p>
     *
     * @param language the current language
     */
    public List<Sentence> extractLogicals (String language)
    {
        logger.fine("extractLogicals from {0}", this);

        Glyph compound = getCompound();

        // Make sure these parameters are computed
        getTextHeight();

        // Use OCR only if no text has been manually defined for the line
        // (and if the OCR engine is available)
        if (Language.getOcr().isAvailable()
                && (getCompound().getManualValue() == null)) {
            List<Sentence> sentences = new ArrayList<>();

            /** Initial collection of sections */
            for (Section section : getMembers()) {
                section.setProcessed(false);
            }

            // Try to reuse existing OCR output
            List<OcrLine> lines = compound.getOcrLines(language);

            if (lines == null) {
                // No existing OCR result, so do call the OCR engine
                lines = compound.retrieveOcrLines(language);
            }

            if ((lines != null) && !lines.isEmpty()) {
                for (OcrLine ocrLine : lines) {
                    // Build line glyph from its enclosed sections
                    SortedSet<Section> sections = compound.retrieveSections(
                            ocrLine.getChars());

                    if (!sections.isEmpty()) {
                        Glyph lineGlyph = system.buildTransientGlyph(sections);

                        // Validate ocr content
                        if (OcrTextVerifier.isValid(lineGlyph, ocrLine)) {
                            lineGlyph = system.addGlyph(lineGlyph);

                            if (lineGlyph == getCompound()) {
                                // No need to allocate a different sentence
                                sentences.add(this);
                            } else {
                                // The compound is larger than the line glyph
                                // Typically this occurs in multi-line case,
                                // so allocate one sentence per logical line
                                Sentence sentence = new Sentence(lineGlyph);
                                sentence.getCompound().setOcrLines(
                                        language,
                                        Arrays.asList(ocrLine));
                                sentences.add(sentence);
                            }

                            lineGlyph.setShape(Shape.TEXT);
                        } else {
                            logger.fine("Invalid line {0} {1}", new Object[]{
                                        ocrLine, this});
                            return Collections.emptyList();
                        }
                    }
                }
            }

            if (sentences.isEmpty()) {
                logger.fine("No OCR line for {0}", idString());
            }

            return sentences;
        } else {
            // Use this instance directly
            return Collections.singletonList(this);
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    public PixelRectangle getBounds ()
    {
        return getCompound().getBounds();
    }

    //---------------//
    // getCandidates //
    //---------------//
    /**
     * Report the set of glyphs retained for this line.
     *
     * @return the exact set of candidate glyphs
     */
    public List<Glyph> getCandidates ()
    {
        SortedSet<Glyph> candidates = Glyphs.sortedSet(getItems());
        candidates.addAll(stripes.get(CORE).intersected);
        candidates.addAll(stripes.get(ASCENT).intersected);
        candidates.addAll(stripes.get(DESCENT).intersected);

        return new ArrayList<>(candidates);
    }

    //--------------------//
    // getDetailedContent //
    //--------------------//
    /**
     * Report the current detailed string content of this text line.
     * This is meant for the developer.
     *
     * @return the detailed content of sentence
     */
    public String getDetailedContent ()
    {
        String value = getTextValue();

        if (value != null) {
            return value;
        } else {
            if (getItemCount() == 1) {
                return getFirstItem().getTextValue();
            } else {
                StringBuilder sb = new StringBuilder();
                boolean started = false;

                for (Glyph glyph : getItems()) {
                    if (started) {
                        sb.append('+');
                    }

                    sb.append(glyph.getTextValue());
                    started = true;
                }

                return sb.toString();
            }
        }
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size of this text line.
     *
     * @return the font size
     */
    public Float getFontSize ()
    {
        return getCompound().getFontSize();
    }

    //-------//
    // getId //
    //-------//
    public String getId ()
    {
        return id;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the starting point of this text line.
     *
     * @return a COPY of the starting point (x: left side, y: baseline)
     */
    public PixelPoint getLocation ()
    {
        if (location == null) {
            PixelRectangle firstBox = getCompound().getBounds();
            location = new PixelPoint(
                    firstBox.x,
                    getCompound().getTextArea().getBaseline());
        }

        return new PixelPoint(location);
    }

    //------------//
    //  getStripe //
    //------------//
    public Stripe getStripe (Stripe.Kind kind)
    {
        return stripes.get(kind);
    }

    //---------------//
    // getSystemPart //
    //---------------//
    /**
     * Report the system part that contains this sentence
     *
     * @return the containing system part
     */
    public SystemPart getSystemPart ()
    {
        return systemPart;
    }

    //----------------//
    // getTextContent //
    //----------------//
    /**
     * Report the string content of this text line, as computed by OCR
     * or entered manually.
     * This is meant for the end user.
     *
     * @return the text interpretation of the text line
     */
    public String getTextContent ()
    {
        String value = getTextValue();

        if (value != null) {
            return value;
        } else {
            return getValueFromGlyphs();
        }
    }

    //---------------//
    // getTextHeight //
    //---------------//
    /**
     * Determine the uniform character height for the whole text line.
     *
     * @return the standard character height in pixels
     */
    public int getTextHeight ()
    {
        TextArea textArea = getCompound().getTextArea();

        return textArea.getBaseline() - textArea.getMedianLine();
    }

    //-------------//
    // getTextRole //
    //-------------//
    /**
     * Report the text role of the sentence within the score
     *
     * @return the role of this sentence
     */
    public TextRole getTextRole ()
    {
        return getCompound().getTextRole();
    }

    //-------------//
    // getTextType //
    //-------------//
    /**
     * Report the text type of the sentence if any
     *
     * @return the type of this sentence
     */
    public CreatorType getTextType ()
    {
        return getCompound().getCreatorType();
    }

    //--------------//
    // getTextValue //
    //--------------//
    public String getTextValue ()
    {
        return getCompound().getTextValue();
    }

    //--------------------//
    // getValueFromGlyphs //
    //--------------------//
    /**
     * Determine the sentence value out of the items individual strings
     *
     * @return the concatenation of all glyphs strings
     */
    public String getValueFromGlyphs ()
    {
        StringBuilder sb = null;

        // Use each item string
        for (Glyph item : getItems()) {
            String str = item.getTextValue();

            if (str == null) {
                str = item.getPseudoValue();

                if (str == null) {
                    logger.fine("Flat sentence {0}", this);
                    continue;
                }
            }

            if (sb == null) {
                sb = new StringBuilder(str);
            } else {
                sb.append(" ").append(str);
            }
        }

        if (sb == null) {
            return "";
        } else {
            return sb.toString();
        }
    }

    //----------//
    // idString //
    //----------//
    public String idString ()
    {
        return "sentence#" + getId();
    }

    //---------------//
    // includeAliens //
    //---------------//
    /**
     * Try to extend the line (which is made of only text items so far)
     * with 'alien' non-text shape items, but which together could make
     * text lines.
     */
    public void includeAliens ()
    {
        Collection<Glyph> aliens = getAliens();

        // Pre-insert all candidates in proper place in the glyphs sequence
        addAllItems(aliens);

        // It is safer to use an upper limit to the following loop
        int aliensCount = aliens.size();

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

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Nullify cached data that depends on the collection of items.
     */
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();

        location = null;
        dskY = null;
        withinStaves = null;
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this text line
     * (and could thus be part of it).
     *
     * @param glyph the provided glyph to isValid wrt this text line
     * @return true if close enough vertically and horizontally
     */
    public boolean isCloseTo (Glyph glyph)
    {
        PixelRectangle fatBox = getBounds();
        fatBox.grow(maxItemDx, maxItemDy);

        return isCloseTo(glyph, fatBox);
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the other text line is close to this text line
     * (and could thus be merged).
     *
     * @param other the provided other text line
     * @return true if close enough vertically and horizontally
     */
    public boolean isCloseTo (Sentence other)
    {
        PixelRectangle fatBox = getBounds();
        fatBox.grow(maxItemDx, 0);

        return fatBox.intersects(other.getBounds())
                && (Math.abs(other.getY() - this.getY()) <= maxTextLineDy)
                && !acrossEntryBarline(other);
    }

    //-------------//
    // isProcessed //
    //-------------//
    public boolean isProcessed ()
    {
        return processed;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //-----------------------//
    // lookupCandidateGlyphs //
    //-----------------------//
    public void lookupCandidateGlyphs (Collection<Glyph> noLyrics)
    {
        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isActive()) {
                continue;
            }

            // Filter shapes
            Shape shape = glyph.getShape();

            if (glyph.isManualShape() && !shape.isText()) {
                continue;
            }

            if (lyricsExclusion.contains(shape)) {
                continue;
            }

            if (noLyrics.contains(glyph)) {
                continue;
            }

            for (Stripe.Kind kind : Stripe.Kind.values()) {
                getStripe(kind).filter(glyph);
            }
        }
    }

    //---------//
    // matches //
    //---------//
    public boolean matches (Sentence item)
    {
        Skew skew = system.getSkew();
        double dy = Math.abs(getY() - skew.deskewed(item.getLocation()).getY());

        return dy <= maxLyricsDy;
    }

    //--------------------//
    // mergeEnclosedTexts //
    //--------------------//
    /**
     * If a text glyph overlaps with another text glyph, make it one
     * glyph.
     */
    public void mergeEnclosedTexts ()
    {
        boolean done = false;

        while (!done) {
            done = true;

            innerLoop:
            for (Glyph inner : getItems()) {
                PixelRectangle innerBox = inner.getBounds();

                for (Glyph outer : getItems()) {
                    if (outer == inner) {
                        continue;
                    }

                    PixelRectangle outerBox = outer.getBounds();

                    if (outerBox.intersects(innerBox)) {
                        Glyph compound = system.buildTransientCompound(
                                Arrays.asList(outer, inner));
                        compound = system.addGlyph(compound);
                        compound.setShape(Shape.TEXT);

                        addItem(compound);

                        if (outer != compound) {
                            removeItem(outer);
                        }

                        if (inner != compound) {
                            removeItem(inner);
                        }

                        logger.fine(
                                "System#{0} text#{1} merged with text#{2} to create text#{3}",
                                new Object[]{system.getId(), inner.getId(),
                                             outer.getId(), compound.getId()});

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
     * Report the glyph built from the merge with the other sentence.
     *
     * @param other
     * @return the resulting glyph
     */
    public Glyph mergeOf (Sentence other)
    {
        Set<Glyph> allGlyphs = new HashSet<>(getItems());
        allGlyphs.addAll(other.getItems());

        return system.buildTransientCompound(allGlyphs);
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    public void removeAttachments ()
    {
        staff.removeAttachments(ATT_PREFIX + getId());
    }

    //----------------------//
    // setGlyphsTranslation //
    //----------------------//
    /**
     * Forward the provided translation to all the items that compose this
     * sentence
     *
     * @param entity the same translation entit for all sentence items
     */
    public void setGlyphsTranslation (Object entity)
    {
        for (Glyph glyph : getItems()) {
            glyph.setTranslation(entity);
        }
    }

    //--------------//
    // setProcessed //
    //--------------//
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //-------------//
    // setTextRole //
    //-------------//
    /**
     * Assign the text role of the sentence within the score
     *
     * @param role the role of this sentence
     */
    public void setTextRole (TextRole role)
    {
        getCompound().setTextRole(role);
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //------//
    // getY //
    //------//
    /**
     * Report the baseline pixel deskewed ordinate of this line.
     *
     * @return the mean line pixel ordinate, or null (this is the case when the
     * line contains no glyph)
     */
    protected double getY ()
    {
        if (dskY == null) {
            Skew skew = system.getSkew();
            Population population = new Population();

            for (Glyph item : getItems()) {
                population.includeValue(
                        skew.deskewed(item.getLocation()).getY());
            }

            if (population.getCardinality() > 0) {
                dskY = population.getMeanValue();
            }
        }

        return dskY;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        try {
            sb.append(" role:");

            TextRole role = getTextRole();

            if (role != null) {
                sb.append(role);
            }
        } catch (Exception ex) {
            sb.append("INVALID");
        }

        try {
            sb.append(" y:");
            sb.append((float) getY());
        } catch (Exception ex) {
            sb.append("INVALID");
        }

        try {
            sb.append(" font:");

            if (getFontSize() != null) {
                sb.append((int) Math.rint(getFontSize()));
            }
        } catch (Exception ex) {
            sb.append("INVALID");
        }

        if (getDetailedContent() != null) {
            sb.append(" content:").append('"').append(getDetailedContent()).
                    append('"');
        }

        return sb.toString();
    }

    //----------------//
    // retrieveWordGlyphs //
    //----------------//
    /**
     * Split the long glyph of this (Lyrics) sentence into word glyphs.
     */
    void splitSentenceIntoWords ()
    {
        // Make sure the split hasn't been done yet
        if (getItemCount() > 1) {
            return;
        } else if (getItems().isEmpty()) {
            logger.severe("splitIntoWords. Sentence with no items: {0}", this);
        } else {
            logger.fine("Splitting lyrics of {0}", this);
            List<Glyph> words = getCompound().retrieveWordGlyphs();

            // Replace the single long item by this collection of word items
            if ((words != null) && !words.isEmpty()) {
                setItems(words);
            }
        }
    }

    //--------------------//
    // acrossEntryBarline //
    //--------------------//
    /**
     * Check whether this and the other text line are separated by the
     * border of the entry barline.
     *
     * @param other the other text line
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (Sentence other)
    {
        if (!isWithinStaves() && !other.isWithinStaves()) {
            return false;
        }

        PixelPoint itemPt = getCompound().getLocation();
        PixelPoint otherPt = other.getCompound().getLocation();
        ScoreSystem scoreSystem = system.getScoreSystem();

        return scoreSystem.isLeftOfStaves(itemPt) != scoreSystem.isLeftOfStaves(
                otherPt);
    }

    //--------------------//
    // acrossEntryBarline //
    //--------------------//
    /**
     * Check whether the provided glyph and this text line are separated
     * by the border of the entry barline.
     *
     * @param glyph the provided glyph
     * @return true if they are separated
     */
    private boolean acrossEntryBarline (Glyph glyph)
    {
        if (!isWithinStaves()) {
            return false;
        }

        PixelPoint itemPt = getCompound().getLocation();
        PixelPoint glyphPt = glyph.getLocation();
        ScoreSystem scoreSystem = system.getScoreSystem();

        return scoreSystem.isLeftOfStaves(itemPt) != scoreSystem.isLeftOfStaves(
                glyphPt);
    }

    //-----------//
    // getAliens //
    //-----------//
    /**
     * Gather all the collection of non-text glyphs that could actually
     * be part of the final text line.
     *
     * @return the found collection of (non text assigned, yet compatible)
     * candidates
     */
    private Collection<Glyph> getAliens ()
    {
        final Collection<Glyph> candidates = new ArrayList<>();
        final PixelRectangle fatBox = getBounds();
        fatBox.grow(maxItemDx, maxItemDy);

        // Check alien glyphs aligned with this line
        for (Glyph glyph : system.getGlyphs()) {
            Shape shape = glyph.getShape();

            if ((glyph == this)
                    || (shape == Shape.GLYPH_PART)
                    || (glyph.isProcessed())
                    || (glyph.isManualShape() && !glyph.isText())
                    || glyph.isShapeForbidden(Shape.TEXT)
                    || alienExclusion.contains(shape)
                    || getItems().contains(glyph)) {
                continue;
            }

            // Check that text line fat box is close to the alien glyph
            if (isCloseTo(glyph, fatBox)) {
                candidates.add(glyph);
            }
        }

        logger.fine("{0} for {1}", new Object[]{Glyphs.toString("aliens",
                                                                candidates),
                                                this});

        return candidates;
    }

    //---------------//
    // getFirstAlien //
    //---------------//
    /**
     * Report the first alien glyph in the current sequence of line
     * items.
     *
     * @return the first occurrence of non-text glyph in the line items
     */
    private Glyph getFirstAlien ()
    {
        for (Glyph glyph : getItems()) {
            if ((glyph.getShape() != Shape.GLYPH_PART) && !glyph.isText()) {
                return glyph;
            }
        }

        return null;
    }

    //-------------------//
    // getMeanTextHeight //
    //-------------------//
    /**
     * Use mean text height of lyrics items.
     *
     * @return the average text height
     */
    private int getMeanTextHeight ()
    {
        double total = 0;
        double weight = 0;

        for (Glyph glyph : getItems()) {
            int itemHeight = glyph.getTextHeight();

            if (itemHeight >= minTextHeight) {
                total += (itemHeight * glyph.getWeight());
                weight += glyph.getWeight();
            }
        }

        return (int) Math.rint(total / weight);
    }

    //-----------//
    // isCloseTo //
    //-----------//
    /**
     * Check whether the provided glyph is close to this text line
     * (and could thus be part of it).
     *
     * @param glyph  the provided glyph to check wrt this text line
     * @param fatBox the fat text line contour box for the text line
     * @return true if close enough vertically and horizontally
     */
    private boolean isCloseTo (Glyph glyph,
                               PixelRectangle fatBox)
    {
        return fatBox.contains(glyph.getAreaCenter())
                && !acrossEntryBarline(glyph);
    }

    //----------------//
    // isWithinStaves //
    //----------------//
    private boolean isWithinStaves ()
    {
        if (withinStaves == null) {
            if (!getItems().isEmpty()) {
                PixelPoint first = getCompound().getLocation();
                withinStaves = system.getScoreSystem().getStaffPosition(first) == StaffPosition.WITHIN_STAVES;
            }
        }

        return withinStaves;
    }

    //--------------//
    // resolveAlien //
    //--------------//
    /**
     * Make every possible effort to include the provided candidate as
     * a true member of the line collection of items.
     *
     * @param candidate the candidate (non-text) glyph
     * @return true if successful, false otherwise
     */
    private boolean resolveAlien (Glyph alien)
    {
        logger.fine("Resolving alien #{0}", alien.getId());

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

                        //                        Glyph glyph = merge.compound;
                        //                        system.computeGlyphFeatures(glyph);
                        //                        glyph = system.addGlyph(glyph);
                        //                        glyph.setEvaluation(merge.vote);
                        //
                        //                        if (logger.isFineEnabled()) {
                        //                            logger.fine(
                        //                                "Candidate #" + alien.getId() +
                        //                                " solved from #" + first.getId() + " to #" +
                        //                                last.getId() + " as #" + glyph.getId());
                        //                        }
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
    //--------//
    // Stripe //
    //--------//
    /**
     * Handles a nearly horizontal stripe around a line of text, either
     * the text core, or the ascent or descent parts.
     */
    public static class Stripe
    {
        //~ Enumerations -------------------------------------------------------

        /** Kinds of horizontal stripes of the text font. */
        public static enum Kind
        {
            //~ Enumeration constant initializers ------------------------------

            /** Ascent stripe (above the median line) */
            ASCENT('a'),
            /** Core stripe (between baseline and median line) */
            CORE('x'),
            /** Descent stripe (below baseline) */
            DESCENT('d');
            //~ Instance fields ------------------------------------------------

            /** Unique initial. */
            final char initial;

            //~ Constructors ---------------------------------------------------
            Kind (char initial)
            {
                this.initial = initial;
            }
        }

        //~ Instance fields ----------------------------------------------------
        /** Stripe kind. */
        final Kind kind;

        /** The polygon that define the stripe limits. */
        final Polygon poly = new Polygon();

        /** The glyphs candidates intersected by the stripe. */
        final List<Glyph> intersected = new ArrayList<>();

        //~ Constructors -------------------------------------------------------
        /**
         * Create a Stripe.
         *
         * @param kind   Stripe kind
         * @param id     instance index
         * @param staff  related staff
         * @param left   top left corner
         * @param right  top right corner
         * @param height stripe height
         */
        public Stripe (Kind kind,
                       String id,
                       StaffInfo staff,
                       Point left,
                       Point right,
                       int height)
        {
            this.kind = kind;

            // Build polygon
            poly.addPoint(left.x, left.y);
            poly.addPoint(left.x, left.y + height);
            poly.addPoint(right.x, right.y + height);
            poly.addPoint(right.x, right.y);

            // Display just the core stripe
            if (kind == CORE) {
                staff.addAttachment(ATT_PREFIX + id + kind.initial, poly);
            }
        }

        //~ Methods ------------------------------------------------------------
        public boolean filter (Glyph glyph)
        {
            if (poly.intersects(glyph.getBounds())) {
                intersected.add(glyph);

                return true;
            }

            return false;
        }

        public boolean intersects (Stripe that)
        {
            if (!poly.intersects(that.poly.getBounds())) {
                return false;
            }

            // More precise check (still to be improved...)

            // That poly to contain a point from this?
            for (int i = 0; i < poly.npoints; i++) {
                if (that.poly.contains(poly.xpoints[i], poly.ypoints[i])) {
                    return true;
                }
            }

            // This poly to contain a point of that?
            for (int i = 0; i < that.poly.npoints; i++) {
                if (poly.contains(that.poly.xpoints[i], that.poly.ypoints[i])) {
                    return true;
                }
            }

            return false;
        }

        public String polyString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(" tl[").append(poly.xpoints[0]).append(',').append(
                    poly.ypoints[0]).append(']');
            sb.append(",bl[").append(poly.xpoints[1]).append(',').append(
                    poly.ypoints[1]).append(']');
            sb.append(",br[").append(poly.xpoints[2]).append(',').append(
                    poly.ypoints[2]).append(']');
            sb.append(",tr[").append(poly.xpoints[3]).append(',').append(
                    poly.ypoints[3]).append(']');

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxItemDy = new Scale.Fraction(
                0,
                "Maximum vertical distance between a text line and a text item");

        Scale.Fraction maxItemDx = new Scale.Fraction(
                5,
                "Maximum horizontal distance between two words");

        Scale.Fraction maxLyricItemDx = new Scale.Fraction(
                2, // HB was 20,
                "Maximum horizontal distance between two (lyric) words");

        Scale.Fraction maxTextLineDy = new Scale.Fraction(
                0.8,
                "Maximum vertical distance between two text line chunks");

        Scale.Fraction maxLyricsDy = new Scale.Fraction(
                0.7,
                "Maximum vertical distance between a lyrics line and a lyrics item");

        Constant.Ratio ascentRatio = new Constant.Ratio(
                0.4,
                "Ratio of ascender height WRT character x-height");

        Constant.Ratio descentRatio = new Constant.Ratio(
                0.4,
                "Ratio of descender height WRT character x-height");

        Scale.Fraction minTextHeight = new Scale.Fraction(
                0.75,
                "Minimum x-height for representative text");
    }

    //-------//
    // Merge //
    //-------//
    /**
     * This utility class tries to operate a merge of all sentence
     * items from a first item to a last item inclusive, while checking
     * whether the resulting compound glyph would be assigned a TEXT
     * shape.
     */
    private class Merge
    {
        //~ Instance fields ----------------------------------------------------

        /** Sequence of items from first to last inclusive */
        private List<Glyph> parts = new ArrayList<>();

        //~ Constructors -------------------------------------------------------
        /**
         * Remember the sequence of text line items between first and
         * last.
         *
         * @param first beginning of the sequence
         * @param last  end of the sequence
         */
        public Merge (Glyph first,
                      Glyph last)
        {
            boolean started = false;

            for (Glyph glyph : getItems()) {
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
         * Replace, in the Sentence set of items, the sequence from first
         * to last by the text compound.
         */
        public void insert ()
        {
            addAllItems(parts);
        }

        /**
         * Check whether the current sequence could be assigned the TEXT
         * shape.
         *
         * @return true if evaluated as TEXT
         */
        public boolean isOk ()
        {
            if (parts.isEmpty()) {
                return false;
            }

            Glyph compound;

            if (parts.size() > 1) {
                compound = system.buildTransientCompound(parts);
            } else if (parts.size() == 1) {
                compound = parts.get(0);
            } else {
                return false;
            }

            ShapeEvaluator evaluator = GlyphNetwork.getInstance();

            Evaluation vote = evaluator.vote(
                    compound,
                    system,
                    Grades.textMinGrade);

            return (vote != null) && vote.shape.isText();
        }
    }
}
