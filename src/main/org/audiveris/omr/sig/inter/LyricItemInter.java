//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L y r i c I t e m I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.text.TextWord;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code LyricItemInter} is specific subclass of Text, meant for one
 * item of a lyrics line (Syllable, Hyphen, Extension, Elision)
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "lyric-item")
public class LyricItemInter
        extends WordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LyricItemInter.class);

    /** String equivalent of Character used for elision. (undertie) */
    public static final String ELISION_STRING = new String(Character.toChars(8255));

    /** String equivalent of Character used for extension. (underscore) */
    public static final String EXTENSION_STRING = "_";

    /** String equivalent of Character used for hyphen. */
    public static final String HYPHEN_STRING = "-";

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
    @XmlAttribute(name = "kind")
    private ItemKind itemKind;

    /** Characteristics of the lyrics syllable, if any. */
    @XmlAttribute(name = "syllabic")
    private SyllabicType syllabicType;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LyricItemInter object.
     *
     * @param textWord the OCR'ed text word
     */
    public LyricItemInter (TextWord textWord)
    {
        super(textWord, Shape.LYRICS);

        if (value.equals(ELISION_STRING)) {
            itemKind = ItemKind.Elision;
        } else if (value.equals(EXTENSION_STRING)) {
            itemKind = ItemKind.Extension;
        } else if (value.equals(HYPHEN_STRING)) {
            itemKind = ItemKind.Hyphen;
        } else {
            itemKind = ItemKind.Syllable;
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LyricItemInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

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
        } else if ((nextItem != null) && (nextItem.itemKind == ItemKind.Hyphen)) {
            syllabicType = SyllabicType.BEGIN;
        } else {
            syllabicType = SyllabicType.SINGLE;
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

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordSyllableRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //-------------//
    // isSeparator //
    //-------------//
    /**
     * Predicate to detect a separator.
     *
     * @param str the character to check
     *
     * @return true if this is a separator
     */
    public static boolean isSeparator (String str)
    {
        return str.equals(EXTENSION_STRING) || str.equals(ELISION_STRING)
               || str.equals(HYPHEN_STRING);
    }

    //------------//
    // mapToChord //
    //------------//
    public void mapToChord ()
    {
        // We map only syllables
        if (itemKind != ItemKind.Syllable) {
            return;
        }

        // Already mapped?
        if (sig.hasRelation(this, ChordSyllableRelation.class)) {
            return;
        }

        // Left is too far on left, middle is too far on right, we use width/4 (???)
        int centerX = getLocation().x + (getBounds().width / 4);
        SystemInfo system = getSig().getSystem();
        boolean lookAbove = true;
        Staff relatedStaff = system.getStaffAtOrAbove(location);

        if (relatedStaff == null) {
            relatedStaff = system.getStaffAtOrBelow(location);
            lookAbove = false;
        }

        setStaff(relatedStaff);

        Part part = relatedStaff.getPart();
        int maxDx = part.getSystem().getSheet().getScale()
                .toPixels(constants.maxItemDx);

        // A word can start in a measure and finish in the next measure
        // Look for best aligned head-chord in proper staff
        int bestDx = Integer.MAX_VALUE;
        AbstractChordInter bestChord = null;

        for (Measure measure : part.getMeasures()) {
            // Select only possible measures
            if ((measure.getAbscissa(LEFT, staff) - maxDx) > centerX) {
                break;
            }

            if ((measure.getAbscissa(RIGHT, staff) + maxDx) < centerX) {
                continue;
            }

            if (lookAbove) {
                for (AbstractChordInter chord : measure.getHeadChordsAbove(getLocation())) {
                    if (chord instanceof HeadChordInter
                        && (chord.getBottomStaff() == relatedStaff)) {
                        int dx = Math.abs(chord.getHeadLocation().x - centerX);

                        if (bestDx > dx) {
                            bestDx = dx;
                            bestChord = chord;
                        }
                    }
                }
            } else {
                for (AbstractChordInter chord : measure.getHeadChordsBelow(getLocation())) {
                    if (chord instanceof HeadChordInter && (chord.getTopStaff() == relatedStaff)) {
                        int dx = Math.abs(chord.getHeadLocation().x - centerX);

                        if (bestDx > dx) {
                            bestDx = dx;
                            bestChord = chord;
                        }
                    }
                }
            }
        }

        if (bestDx <= maxDx) {
            sig.addEdge(bestChord, this, new ChordSyllableRelation());

            return;
        }

        logger.info("No head-chord for {}", this);
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
                5,
                "Maximum horizontal distance between a note and its lyric item");
    }
}
