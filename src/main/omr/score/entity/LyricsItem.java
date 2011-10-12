//----------------------------------------------------------------------------//
//                                                                            //
//                            L y r i c s I t e m                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;
import omr.glyph.text.Sentence;
import omr.glyph.text.TextInfo;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.TreeNode;

import java.util.Comparator;

/**
 * Class <code>LyricsItem</code> is specific subclass of Text, meant for one
 * item of a lyrics line (Syllable, Hyphen, Extension, Elision)
 *
 * @author Hervé Bitteur
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

    /** Comparator based on line number */
    public static final Comparator<LyricsItem> numberComparator = new Comparator<LyricsItem>() {
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

    /** Width of the item */
    private final int width;

    /** The glyph which contributed to the creation of this lyrics item */
    private final Glyph seed;

    /** Mapped note/chord, if any */
    private Chord mappedChord;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // LyricsItem //
    //------------//
    /**
     * Creates a new LyricsItem object.
     *
     * @param sentence The containing sentence
     * @param location The starting point (left side, base line) wrt system
     * @param seed The glyph that initiated the creation of this lyrics item
     * @param width The width (in units) of the related item
     * @param content The underlying text for this lyrics item
     */
    public LyricsItem (Sentence   sentence,
                       PixelPoint location,
                       Glyph      seed,
                       int        width,
                       String     content)
    {
        super(sentence, location);
        this.seed = seed;
        this.width = width;
        this.content = content;

        if (content.equals(TextInfo.ELISION_STRING)) {
            itemKind = ItemKind.Elision;
        } else if (content.equals(TextInfo.EXTENSION_STRING)) {
            itemKind = ItemKind.Extension;
        } else if (content.equals(TextInfo.HYPHEN_STRING)) {
            itemKind = ItemKind.Hyphen;
        } else {
            itemKind = ItemKind.Syllable;
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getBox //
    //--------//
    @Override
    public PixelRectangle getBox ()
    {
        return seed.getContourBox();
    }

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

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
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
        return Integer.signum(
            getReferencePoint().x - other.getReferencePoint().x);
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

        // Left is too far on left, middle is too far on right, we use width/4
        int        centerX = getReferencePoint().x + (width / 4);

        SystemPart part = lyricsLine.getPart();
        int        maxDx = part.getScale()
                               .toPixels(constants.maxItemDx);

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

            for (Chord chord : measure.getChordsAbove(getReferencePoint())) {
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
                        mappedChord = bestChord;
                    }

                    return;
                }
            }
        }

        addError(seed, "Could not find note for " + this);
    }

    //-----------------------//
    // computeReferencePoint //
    //-----------------------//
    @Override
    protected void computeReferencePoint ()
    {
        PixelRectangle itemBox = seed.getContourBox();
        setReferencePoint(
            new PixelPoint(itemBox.x, getSentence()
                                          .getLocation().y));
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

        if (mappedChord != null) {
            sb.append(" mappedTo:Ch#" + mappedChord.getId());
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
            4,
            "Maximum horizontal distance between a note and its lyrics item");
    }
}
