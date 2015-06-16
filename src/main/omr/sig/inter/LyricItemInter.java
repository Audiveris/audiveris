//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L y r i c I t e m I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.ConstantSet;

import omr.glyph.facets.GlyphContent;

import omr.sheet.Part;
import omr.sheet.Scale;
import omr.sheet.rhythm.Measure;

import omr.sig.relation.ChordSyllableRelation;

import omr.text.TextWord;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code LyricItemInter} is specific subclass of Text, meant for one
 * item of a lyrics line (Syllable, Hyphen, Extension, Elision)
 *
 * @author Hervé Bitteur
 */
public class LyricItemInter
        extends WordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LyricItemInter.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * Describes the kind of this lyrics item.
     */
    public static enum ItemKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Just an elision */
        Elision,
        /** Just an extension */
        Extension,
        /** A hyphen between syllables */
        Hyphen,
        /** A real syllable */
        Syllable;
    }

    /**
     * Describes more precisely a syllable inside a word.
     */
    public static enum SyllabicType
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Single-syllable word */
        SINGLE,
        /** Syllable that begins a word */
        BEGIN,
        /** Syllable at the middle of a word */
        MIDDLE,
        /** Syllable that ends a word */
        END;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Lyrics kind. */
    private ItemKind itemKind;

    /** Characteristics of the lyrics syllable, if any. */
    private SyllabicType syllabicType;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //    /** The containing lyrics line. */
    //    private LyricLineInter lyricsLine;
    //
    //    /**
    //     * The carried text for this item.
    //     * (only a part of the containing sentence, as opposed to other texts)
    //     */
    //    private final String content;
    //
    //    /** The exact corresponding word. */
    //    private final TextWord word;
    //
    //    /** Width of the item. */
    //    private final int width;
    //
    //    /** The glyph which contributed to this lyrics item. */
    //    private final Glyph seed;
    //
    //    /** Mapped note/chord, if any. */
    //    private OldChord mappedChord;
    //
    /**
     * Creates a new LyricItemInter object.
     *
     * @param textWord the OCR'ed text word
     */
    public LyricItemInter (TextWord textWord)
    {
        super(textWord);

        if (value.equals(GlyphContent.ELISION_STRING)) {
            itemKind = ItemKind.Elision;
        } else if (value.equals(GlyphContent.EXTENSION_STRING)) {
            itemKind = ItemKind.Extension;
        } else if (value.equals(GlyphContent.HYPHEN_STRING)) {
            itemKind = ItemKind.Hyphen;
        } else {
            itemKind = ItemKind.Syllable;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // defineSyllabicType //
    //--------------------//
    public void defineSyllabicType (LyricItemInter prevItem,
                                    LyricItemInter nextItem)
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

    //-------------//
    // getItemKind //
    //-------------//
    public ItemKind getItemKind ()
    {
        return itemKind;
    }

    //--------------//
    // getLyricLine //
    //--------------//
    public LyricLineInter getLyricLine ()
    {
        return (LyricLineInter) getEnsemble();
    }

    //-----------------//
    // getSyllabicType //
    //-----------------//
    public SyllabicType getSyllabicType ()
    {
        return syllabicType;
    }

    //------------//
    // mapToChord //
    //------------//
    public void mapToChord ()
    {
        // We connect only syllables
        if (itemKind != ItemKind.Syllable) {
            return;
        }

        // Left is too far on left, middle is too far on right, we use width/4 (???)
        int centerX = getLocation().x + (getBounds().width / 4);

        Part part = getLyricLine().getPart();
        int maxDx = part.getSystem().getSheet().getScale().toPixels(constants.maxItemDx);

        for (Measure measure : part.getMeasures()) {
            // Select only possible measures
            if ((measure.getAbscissa(HorizontalSide.LEFT, staff) - maxDx) > centerX) {
                break;
            }

            if ((measure.getAbscissa(HorizontalSide.RIGHT, staff) + maxDx) < centerX) {
                continue;
            }

            // Look for best aligned head-chord in proper staff
            int bestDx = Integer.MAX_VALUE;
            ChordInter bestChord = null;

            for (ChordInter chord : measure.getChordsAbove(getLocation())) {
                if (chord instanceof HeadChordInter
                    && (chord.getStaff() == getLyricLine().getStaff())) {
                    int dx = Math.abs(chord.getHeadLocation().x - centerX);

                    if (bestDx > dx) {
                        bestDx = dx;
                        bestChord = chord;
                    }
                }
            }

            if (bestDx <= maxDx) {
                sig.addEdge(bestChord, this, new ChordSyllableRelation());

                return;
            }
        }

        logger.warn("No head-chord above lyric {}", this);
    }

    //-------------//
    // setItemKind //
    //-------------//
    public void setItemKind (ItemKind itemKind)
    {
        this.itemKind = itemKind;
    }

    //-----------------//
    // setSyllabicType //
    //-----------------//
    public void setSyllabicType (SyllabicType syllabicType)
    {
        this.syllabicType = syllabicType;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (itemKind != null) {
            sb.append(" ").append(itemKind);
        }

        if (getSyllabicType() != null) {
            sb.append(" ").append(getSyllabicType());
        }

        //
        //        if (mappedChord != null) {
        //            sb.append(" mappedTo:Ch#").append(mappedChord.getId());
        //        }
        //
        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxItemDx = new Scale.Fraction(
                4,
                "Maximum horizontal distance between a note and its lyrics item");
    }
}
