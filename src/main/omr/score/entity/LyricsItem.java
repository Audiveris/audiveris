//----------------------------------------------------------------------------//
//                                                                            //
//                            L y r i c s I t e m                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.text.Sentence;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.TreeNode;

import java.util.Comparator;

/**
 * Class <code>LyricsItem</code> is specific subclass of Text, meant for one
 * item of a lyrics line (Syllable, Hyphen, Extension, Elision)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LyricsItem
    extends Text
    implements Comparable<LyricsItem>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LyricsItem.class);

    /** Character used for elision */
    private static final char ELISION_CHAR = '?'; // TBD

    /** Character used for extension */
    private static final char EXTENSION_CHAR = '_'; // TBD

    /** Character used for hyphen */
    private static final char HYPHEN_CHAR = '-';

    /** Comparator based on line number */
    public static Comparator<LyricsItem> numberComparator = new Comparator<LyricsItem>() {
        public int compare (LyricsItem o1,
                            LyricsItem o2)
        {
            return o1.lyricsLine.getId() - o2.lyricsLine.getId();
        }
    };


    //~ Enumerations -----------------------------------------------------------

    /** Describes the kind of this lyrics item */
    public static enum ItemKind {
        //~ Enumeration constant initializers ----------------------------------


        /** Just an elision */
        Elision,
        /** Just an extension */
        Extension, 
        /** A hyphen between syllables */
        Hyphen, 
        /** A real syllable */
        Syllable;
    }

    /** Describes more precisely a syllable inside a word */
    public static enum SyllabicType {
        //~ Enumeration constant initializers ----------------------------------


        /** Single-syllable word */
        SINGLE,
        /** Syllable that begins a word */
        BEGIN, 
        /** Syllable at the middle of a word */
        MIDDLE, 
        /** Syllable that ends a word */
        END;
    }

    //~ Instance fields --------------------------------------------------------

    /** Lyrics kind */
    private ItemKind itemKind;

    /** Characteristics of the lyrisc syllable, if any */
    private SyllabicType syllabicType;

    /** The containing lyrics line */
    private LyricsLine lyricsLine;

    /**
     * The carried text for this item
     * (only a part of the containing sentence, as opposed to other texts)
     */
    private String content;

    /** Width (in units) of the item */
    private final int width;

    /** The glyph which contributed to the creation of this lyrics item */
    private final Glyph seed;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // LyricsItem //
    //------------//
    /**
     * Creates a new LyricsItem object.
     *
     * @param sentence The containing sentence
     * @param location The starting point (left side, base line) wrt system
     * @param seed The glyph was initiated the creation of this lyrics item
     * @param width The width (in units) of the related item
     * @param content The underlying text for this lyrics item
     */
    private LyricsItem (Sentence    sentence,
                        SystemPoint location,
                        Glyph       seed,
                        int         width,
                        String      content)
    {
        super(sentence, location);
        this.seed = seed;
        this.width = width;
        this.content = content;

        // Kind can be infered from content?
        char c = content.charAt(0);

        if (c == ELISION_CHAR) {
            itemKind = ItemKind.Elision;
        } else if (c == EXTENSION_CHAR) {
            itemKind = ItemKind.Extension;
        } else if (c == HYPHEN_CHAR) {
            itemKind = ItemKind.Hyphen;
        } else {
            itemKind = ItemKind.Syllable;
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getContent //
    //------------//
    /**
     * Report the current string value of this text (just the lyrics item)
     * @return the string value of this text
     */
    @Override
    public String getContent ()
    {
        return content;
    }

    //---------------//
    // getLyricsLine //
    //---------------//
    public LyricsLine getLyricsLine ()
    {
        return lyricsLine;
    }

    //-----------------//
    // setSyllabicType //
    //-----------------//
    public void setSyllabicType (SyllabicType syllabicType)
    {
        this.syllabicType = syllabicType;
    }

    //-----------------//
    // getSyllabicType //
    //-----------------//
    public SyllabicType getSyllabicType ()
    {
        return syllabicType;
    }

    //-------------------//
    // createLyricsItems //
    //-------------------//
    /**
     * With the provided content, create one or several lyrics items
     *
     * @param sentence the containing sentence
     * @param glyph the underlying glyph
     * @param location The starting point (left side, base line) wrt system
     * @param content Text of the glyph, which may comprise several syllables
     * @param box The bounding box of the glyph
     */
    public static void createLyricsItems (Sentence        sentence,
                                          Glyph           glyph,
                                          SystemPoint     location,
                                          String          content,
                                          SystemRectangle box)
    {
        // Scaling ratio
        double    ratio = (double) box.width / content.length();

        // Parse the content string
        MyScanner scanner = new MyScanner(content);
        glyph.clearTranslations();

        while (scanner.hasNext()) {
            int             start = scanner.getWordStart();
            String          word = scanner.next();
            SystemPoint     loc = new SystemPoint(
                location.x + (int) Math.rint(start * ratio),
                location.y);
            SystemRectangle itemBox = new SystemRectangle(
                box.x + (int) Math.rint(start * ratio),
                box.y,
                (int) Math.rint(word.length() * ratio),
                box.height);

            if (logger.isFineEnabled()) {
                logger.fine(word + " at " + loc + " start=" + start);
            }

            glyph.addTranslation(
                new LyricsItem(sentence, loc, glyph, itemBox.width, word));
        }
    }

    //-------------//
    // setItemKind //
    //-------------//
    public void setItemKind (ItemKind itemKind)
    {
        this.itemKind = itemKind;
    }

    //-------------//
    // getItemKind //
    //-------------//
    public ItemKind getItemKind ()
    {
        return itemKind;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-----------//
    // compareTo //
    //-----------//
    public int compareTo (LyricsItem other)
    {
        if (this == other) {
            return 0;
        }

        // Comparison is based on abscissa only
        return Integer.signum(location.x - other.location.x);
    }

    //--------------------//
    // defineSyllabicType //
    //--------------------//
    public void defineSyllabicType (LyricsItem prevItem,
                                    LyricsItem nextItem)
    {
        if ((prevItem != null) && (prevItem.itemKind == ItemKind.Hyphen)) {
            if ((nextItem != null) && (nextItem.itemKind == ItemKind.Hyphen)) {
                syllabicType = SyllabicType.MIDDLE;
            } else {
                syllabicType = SyllabicType.END;
            }
        } else {
            if ((nextItem != null) && (nextItem.itemKind == ItemKind.Hyphen)) {
                syllabicType = SyllabicType.BEGIN;
            } else {
                syllabicType = SyllabicType.SINGLE;
            }
        }
    }

    //-----------//
    // mapToNote //
    //----------//
    public void mapToNote ()
    {
        // We connect only syllables
        if (itemKind != ItemKind.Syllable) {
            return;
        }

        int        centerX = location.x + (width / 2);
        SystemPart part = lyricsLine.getPart();
        int        maxDx = part.getScale()
                               .toUnits(constants.maxItemDx);

        for (TreeNode mNode : part.getMeasures()) {
            Measure measure = (Measure) mNode;

            // Select only possible measures
            if ((measure.getLeftX() - maxDx) > centerX) {
                break;
            }

            if ((measure.getBarline()
                        .getRightX() + maxDx) < centerX) {
                continue;
            }

            // Look for best aligned chord in proper staff
            int   bestDx = Integer.MAX_VALUE;
            Chord bestChord = null;

            for (Chord chord : measure.getChordsAbove(location)) {
                if (chord.getStaff() == lyricsLine.getStaff()) {
                    int dx = Math.abs(chord.getHeadLocation().x - centerX);

                    if (bestDx > dx) {
                        bestDx = dx;
                        bestChord = chord;
                    }
                }
            }

            if (bestDx <= maxDx) {
                for (TreeNode nNode : bestChord.getNotes()) {
                    Note note = (Note) nNode;

                    if (!note.isRest()) {
                        note.addSyllable(this);
                    }

                    return;
                }
            }
        }

        addError(seed, "Could not find note for " + this);
    }

    //---------------------//
    // determineFontSize //
    //---------------------//
    /**
     * For lyrics items, we force the size of the related font
     */
    @Override
    protected void determineFontSize ()
    {
        font = lyricsFont;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (itemKind != null) {
            sb.append(" ")
              .append(itemKind);
        }

        if (getSyllabicType() != null) {
            sb.append(" ")
              .append(getSyllabicType());
        }

        return sb.toString();
    }

    //--------------//
    // setLyricLine //
    //--------------//
    void setLyricLine (LyricsLine line)
    {
        this.lyricsLine = line;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxItemDx = new Scale.Fraction(
            3,
            "Maximum horizontal distance between a note and its lyrics item");
    }

    //-----------//
    // MyScanner //
    //-----------//
    /**
     * A specific scanner to scan lyrics content, since we need to know the
     * current position within the initial string, to infer the proper location
     */
    private static class MyScanner
    {
        //~ Instance fields ----------------------------------------------------

        private String        data;
        private int           pos = -1;
        private StringBuilder sb = new StringBuilder();
        private String        word;
        private int           wordStart = 0;

        //~ Constructors -------------------------------------------------------

        public MyScanner (String data)
        {
            this.data = data;
        }

        //~ Methods ------------------------------------------------------------

        public int getWordStart ()
        {
            return wordStart;
        }

        public boolean hasNext ()
        {
            if (word == null) {
                word = getNextWord();
            }

            return word != null;
        }

        public String next ()
        {
            String oldWord = word;
            word = null;

            return oldWord;
        }

        private String getNextWord ()
        {
            sb.setLength(0);

            for (pos = pos + 1; pos < data.length(); pos++) {
                char c = data.charAt(pos);

                // white space
                if (Character.isWhitespace(c)) {
                    if (sb.length() > 0) {
                        return sb.toString();
                    } else {
                        continue;
                    }
                } else if ((c == EXTENSION_CHAR) || // Special character
                           (c == ELISION_CHAR)) { // Special character

                    if (sb.length() > 0) {
                        pos--;
                    } else {
                        wordStart = pos;
                        sb.append(c);
                    }

                    return sb.toString();
                } else {
                    if (sb.length() == 0) {
                        wordStart = pos;
                    }

                    sb.append(c);
                }
            }

            // We have reached data end
            if (sb.length() > 0) {
                return sb.toString();
            } else {
                return null;
            }
        }
    }
}
