//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t L i n e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.SystemInfo;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code TextLine} defines a non-mutable structure to report all
 * information on one OCR-decoded line.
 *
 * @author Hervé Bitteur
 */
public class TextLine
        extends TextBasedItem
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextLine.class);

    /** Line comparator by deskewed abscissa. */
    public static final Comparator<TextLine> byAbscissa = new Comparator<TextLine>()
    {
        @Override
        public int compare (TextLine o1,
                            TextLine o2)
        {
            return Double.compare(o1.getDskOrigin().getX(),
                                  o2.getDskOrigin().getX());
        }
    };

    /** Line comparator by deskewed ordinate. */
    public static final Comparator<TextLine> byOrdinate = new Comparator<TextLine>()
    {
        @Override
        public int compare (TextLine o1,
                            TextLine o2)
        {
            return Double.compare(o1.getDskOrigin().getY(),
                                  o2.getDskOrigin().getY());
        }
    };

    //~ Instance fields --------------------------------------------------------
    //
    /** Containing system. */
    private final SystemInfo system;

    /** Words that compose this line. */
    private final List<TextWord> words = new ArrayList<>();

    /** Unmodifiable view of the words sequence. */
    private final List<TextWord> wordsView = Collections.unmodifiableList(words);

    /** Deskewed origin. */
    private Point2D dskOrigin;

    /**
     * Role of this text line.
     * Lazily computed, since it depends for a part on the contained words.
     */
    private TextRoleInfo roleInfo;

    /** Temporary processed flag. */
    private boolean processed;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // TextLine //
    //----------//
    /**
     * Creates a new TextLine object.
     *
     * @param system the containing system
     * @param words  the sequence of words
     */
    public TextLine (SystemInfo system,
                     List<TextWord> words)
    {
        this(system);

        this.words.addAll(words);

        for (TextWord word : words) {
            word.setTextLine(this);
        }
    }

    //----------//
    // TextLine //
    //----------//
    /**
     * Creates a new TextLine object, without its contained words which
     * are assumed to be added later.
     *
     * @param system the containing system
     */
    public TextLine (SystemInfo system)
    {
        super(null, null, null, null);

        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // appendWord //
    //------------//
    /**
     * Append a word at the end of the word sequence of the line.
     *
     * @param word the word to append
     */
    public void appendWord (TextWord word)
    {
        words.add(word);
        word.setTextLine(this);
        invalidateCache();
    }

    //----------//
    // addWords //
    //----------//
    /**
     * Add a few words.
     *
     * @param words the words to add
     */
    public void addWords (Collection<TextWord> words)
    {
        this.words.addAll(words);

        for (TextWord word : words) {
            word.setTextLine(this);
        }

        Collections.sort(this.words, TextWord.byAbscissa);

        invalidateCache();
    }

    //------//
    // dump //
    //------//
    /**
     * Print out internals.
     */
    public void dump ()
    {
        logger.info("{0}", this);
        for (TextWord word : words) {
            logger.info("   {0}", word);
        }
    }

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Overridden to recompute baseline from contained words
     *
     * @return the line baseline
     */
    @Override
    public Line2D getBaseline ()
    {
        if (super.getBaseline() == null) {
            if (words.isEmpty()) {
                return null;
            } else {
                setBaseline(baselineOf(words));
            }
        }

        return super.getBaseline();
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Overridden to recompute the bounds from contained words.
     *
     * @return the line bounds
     */
    @Override
    public PixelRectangle getBounds ()
    {
        if (super.getBounds() == null) {
            setBounds(boundsOf(getWords()));
        }

        return super.getBounds();
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of chars descriptors (of words).
     *
     * @return the chars
     */
    public List<TextChar> getChars ()
    {
        List<TextChar> chars = new ArrayList<>();

        for (TextWord word : words) {
            chars.addAll(word.getChars());
        }

        return chars;
    }

    //---------------//
    // getConfidence //
    //---------------//
    /**
     * Overridden to recompute the confidence from contained words.
     *
     * @return the line confidence
     */
    @Override
    public Integer getConfidence ()
    {
        if (super.getConfidence() == null) {
            setConfidence(confidenceOf(getWords()));
        }

        return super.getConfidence();
    }

    //--------------//
    // getDskOrigin //
    //--------------//
    /**
     * Report the deskewed origin of this text line
     *
     * @return the deskewed origin
     */
    public Point2D getDskOrigin ()
    {
        if (dskOrigin == null) {
            Line2D base = getBaseline();
            if (base != null) {
                dskOrigin = system.getSkew().deskewed(base.getP1());
            }
        }

        return dskOrigin;
    }

    //--------------//
    // getFirstWord //
    //--------------//
    /**
     * Report the first word of the sentence.
     *
     * @return the first word
     */
    public TextWord getFirstWord ()
    {
        if (!words.isEmpty()) {
            return words.get(0);
        } else {
            return null;
        }
    }

    //-----------------//
    // getMeanFontSize //
    //-----------------//
    /**
     * Compute a mean font size on representative words.
     *
     * @return the mean font size
     */
    public float getMeanFontSize ()
    {
        float sumSize = 0;
        int wordCount = 0;

        for (TextWord word : words) {
            // Discard one-char words, they are not reliable
            if (word.getChars().size() > 1) {
                wordCount++;
                sumSize += word.getPreciseFontSize();
            }
        }

        if (wordCount > 0) {
            return sumSize / wordCount;
        } else {
            return getFirstWord().getFontInfo().pointsize;
        }
    }

    //---------//
    // getRole //
    //---------//
    /**
     * Lazily compute the line role.
     *
     * @return the roleInfo
     */
    public TextRoleInfo getRole ()
    {
        if (roleInfo == null) {
            // Guess role
            roleInfo = TextRole.guessRole(
                    getBounds(),
                    system,
                    system.getTextBuilder().isMainlyItalic(this));
        }

        return roleInfo;
    }

    //---------------//
    // getSystemPart //
    //---------------//
    /**
     * Report the containing system part.
     *
     * @return the containing system part
     */
    public SystemPart getSystemPart ()
    {
        final TextRole role = getRole().role;
        final PixelPoint location = getFirstWord().getLocation();
        final Staff staff = system.getScoreSystem().getTextStaff(role, location);

        return staff.getPart();
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Overridden to return the concatenation of word values.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        StringBuilder sb = null;

        // Use each word value
        for (TextWord word : words) {
            String str = word.getValue();

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

    //---------------//
    // getWordGlyphs //
    //---------------//
    /**
     * Report the sequence of glyphs (parallel to the sequence of words)
     *
     * @return the sequence of word glyphs
     */
    public List<Glyph> getWordGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<>(words.size());

        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();
            if (glyph != null) {
                glyphs.add(glyph);
            } else {
                logger.warning("Word {0} with no related glyph", word);
            }
        }

        return glyphs;
    }

    //----------//
    // getWords //
    //----------//
    /**
     * Report an <b>unmodifiable</b> view of the sequence of words.
     *
     * @return the words view
     */
    public List<TextWord> getWords ()
    {
        return wordsView;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * {@inheritDoc}
     */
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        if (getDskOrigin() != null) {
            sb.append(String.format(
                    " dsk[%.0f,%.0f]",
                    getDskOrigin().getX(), getDskOrigin().getY()));
        }

        if (getRole() != null) {
            sb.append(getRole());
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        setBounds(null);

        setBaseline(null);
        setConfidence(null);

        dskOrigin = null;
        roleInfo = null;
    }

    //----------//
    // isLyrics //
    //----------//
    /**
     * Report whether this line is flagged as a Lyrics line
     *
     * @return true for lyrics line
     */
    public boolean isLyrics ()
    {
        return getRole().role == TextRole.Lyrics;
    }

    //-------------//
    // isProcessed //
    //-------------//
    public boolean isProcessed ()
    {
        return processed;
    }

    //-------------//
    // removeWords //
    //-------------//
    /**
     * Remove a few words
     *
     * @param words the words to remove
     */
    public void removeWords (Collection<TextWord> words)
    {
        this.words.removeAll(words);
        invalidateCache();
    }

    //----------------------//
    // setGlyphsTranslation //
    //----------------------//
    /**
     * Forward the informationto all the words that compose this line.
     *
     * @param entity the same score entity for all sentence items
     */
    public void setGlyphsTranslation (Object entity)
    {
        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();

            if (glyph != null) {
                glyph.setTranslation(entity);
            }
        }
    }

    //--------------//
    // setProcessed //
    //--------------//
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //---------//
    // setRole //
    //---------//
    /**
     * Assign role information.
     *
     * @param roleInfo the roleInfo to set
     */
    public void setRole (TextRoleInfo roleInfo)
    {
        this.roleInfo = roleInfo;
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of words descriptors.
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    @Override
    public void translate (int dx,
                           int dy)
    {
        // Translate line bounds and baseline
        super.translate(dx, dy);

        // Update the deskewed origin
        getDskOrigin().setLocation(system.getSkew().deskewed(
                getBaseline().getP1()));

        // Translate contained descriptors
        for (TextWord word : words) {
            word.translate(dx, dy);
        }
    }
}
