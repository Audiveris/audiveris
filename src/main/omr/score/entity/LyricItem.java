//----------------------------------------------------------------------------//
//                                                                            //
//                             L y r i c I t e m                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;

import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.Comparator;

/**
 * Class <code>LyricItem</code> is one item of a lyric line (Syllable, Hyphen,
 * Extension, Elision)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LyricItem
    extends Text
    implements Comparable<LyricItem>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LyricItem.class);

    /** Character used for elision */
    private static final char ELISION_CHAR = '?'; // TBD

    /** Character used for extension */
    private static final char EXTENSION_CHAR = '_'; // TBD

    /** Character used for hyphen */
    private static final char HYPHEN_CHAR = '-';

    /** Comparator based on line number */
    public static Comparator<LyricItem> numberComparator = new Comparator<LyricItem>() {
        public int compare (LyricItem o1,
                            LyricItem o2)
        {
            return o1.line.getId() - o2.line.getId();
        }
    };


    //~ Enumerations -----------------------------------------------------------

    /** Describes the kind of this lyric item */
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

    /** Characteristics of the lyric syllable, if any */
    private SyllabicType syllabicType;

    /** The containing lyric line */
    private LyricLine line;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LyricItem //
    //-----------//
    /**
     * Creates a new LyricItem object.
     *
     * @param systemPart The containing system part
     * @param center The center of this item wrt system
     * @param location The starting point (left side, base line) wrt system
     * @param content The underlying text
     * @param box The bounding box
     */
    public LyricItem (SystemPart      systemPart,
                      SystemPoint     center,
                      SystemPoint     location,
                      String          content,
                      SystemRectangle box)
    {
        super(systemPart, center, location, content, box);

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

    //------------------//
    // createLyricItems //
    //------------------//
    /**
     * With the provided content, create one or several lyric items
     *
     * @param glyph the underlying glyph
     * @param systemPart The containing system part
     * @param location The starting point (left side, base line) wrt system
     * @param content The underlying text, which may comprise several syllables
     * @param box The bounding box of the glyph
     */
    public static void createLyricItems (Glyph           glyph,
                                         SystemPart      systemPart,
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
            SystemPoint     ref = new SystemPoint(
                itemBox.x + (itemBox.width / 2),
                location.y);

            if (logger.isFineEnabled()) {
                logger.fine(word + " at " + loc + " start=" + start);
            }

            glyph.addTranslation(
                new LyricItem(systemPart, ref, loc, word, itemBox));
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

    //---------//
    // getLine //
    //---------//
    public LyricLine getLine ()
    {
        return line;
    }

    //-----------//
    // compareTo //
    //-----------//
    public int compareTo (LyricItem other)
    {
        if (this == other) {
            return 0;
        }

        // Comparison is based on abscissa only
        return Integer.signum(location.x - other.location.x);
    }

    //---------------//
    // connectToNote //
    //---------------//
    public void connectToNote ()
    {
        // We connect only syllables
        if (itemKind != ItemKind.Syllable) {
            return;
        }

        SystemPart part = line.getPart();
        int        maxDx = part.getScale()
                               .toUnits(constants.maxItemDx);

        for (TreeNode mNode : part.getMeasures()) {
            Measure measure = (Measure) mNode;

            // Select only possible measures
            if ((measure.getLeftX() - maxDx) > center.x) {
                break;
            }

            if ((measure.getBarline()
                        .getRightX() + maxDx) < center.x) {
                continue;
            }

            // Look for best aligned chord in proper staff
            int   bestDx = Integer.MAX_VALUE;
            Chord bestChord = null;

            for (Chord chord : measure.getChordsAbove(location)) {
                if (chord.getStaff() == line.getStaff()) {
                    int dx = Math.abs(chord.getHeadLocation().x - center.x);

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

        logger.warning("Could not find note for " + this);
    }

    //--------------------//
    // defineSyllabicType //
    //--------------------//
    public void defineSyllabicType (LyricItem prevItem,
                                    LyricItem nextItem)
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

    //---------------//
    // determineFont //
    //---------------//
    /**
     * For lyric items, we force the size of the related font
     */
    @Override
    protected void determineFont ()
    {
        displayFont = lyricFont;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("lyric");

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

    //---------//
    // setLine //
    //---------//
    void setLine (LyricLine line)
    {
        this.line = line;
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
            "Maximum horizontal distance between a note and its lyric item");
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
