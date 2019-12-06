//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L y r i c I t e m I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.TextBuilder;
import org.audiveris.omr.text.TextWord;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code LyricItemInter} is a specific subclass of Text, meant for one
 * item of a {@link LyricLineInter}.
 * <p>
 * Its kind is either Syllable, Hyphen, Extension or Elision.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "lyric-item")
public class LyricItemInter
        extends WordInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LyricItemInter.class);

    /** String equivalent of Character used for elision. (undertie) */
    public static final String ELISION_STRING = new String(Character.toChars(8_255));

    /** String equivalent of Character used for extension. (underscore) */
    public static final String EXTENSION_STRING = "_";

    /** String equivalent of Character used for hyphen. */
    public static final String HYPHEN_STRING = "-";

    /** Lyrics kind. */
    @XmlAttribute(name = "kind")
    private ItemKind itemKind;

    /** Characteristics of the lyrics syllable, if any. */
    @XmlAttribute(name = "syllabic")
    private SyllabicType syllabicType;

    /**
     * Creates a new LyricItemInter object.
     *
     * @param textWord the OCR'ed text word
     */
    public LyricItemInter (TextWord textWord)
    {
        super(textWord, Shape.LYRICS);

        itemKind = inferItemKind();
    }

    /**
     * Creates a new {@code LyricItemInter} object meant for manual assignment.
     *
     * @param grade inter grade
     */
    public LyricItemInter (double grade)
    {
        super(Shape.LYRICS, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LyricItemInter ()
    {
    }

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
    /**
     * Define proper syllabic type for this lyric syllable item, based on previous and
     * next items.
     *
     * @param prevItem previous item
     * @param nextItem next item
     */
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
    /**
     * Report the kind of this lyric item.
     *
     * @return item kind (Syllable, Hyphen, ...)
     */
    public ItemKind getItemKind ()
    {
        return itemKind;
    }

    //-------------//
    // setItemKind //
    //-------------//
    /**
     * Set the item kind.
     *
     * @param itemKind item kind
     */
    public void setItemKind (ItemKind itemKind)
    {
        this.itemKind = itemKind;
    }

    //--------------//
    // getLyricLine //
    //--------------//
    /**
     * Report the containing lyric line.
     *
     * @return containing line
     */
    public LyricLineInter getLyricLine ()
    {
        return (LyricLineInter) getEnsemble();
    }

    //-----------------//
    // getSyllabicType //
    //-----------------//
    /**
     * Report the syllabic type.
     *
     * @return syllabic type
     */
    public SyllabicType getSyllabicType ()
    {
        return syllabicType;
    }

    //-----------------//
    // setSyllabicType //
    //-----------------//
    /**
     * Set the syllabic type.
     *
     * @param syllabicType the syllabic type for this item
     */
    public void setSyllabicType (SyllabicType syllabicType)
    {
        this.syllabicType = syllabicType;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new text value, which may impact the lyric item kind.
     *
     * @param value the new value
     */
    @Override
    public void setValue (String value)
    {
        super.setValue(value);

        ItemKind oldKind = itemKind;
        itemKind = inferItemKind();

        if ((sig != null) && (itemKind != oldKind)) {
            if (itemKind == ItemKind.Syllable) {
                // Try to set relation with chord
                mapToChord();
            } else {
                // Remove any chord relation
                for (Relation rel : sig.getRelations(this, ChordSyllableRelation.class)) {
                    sig.removeEdge(rel);
                }
            }
        }
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

    //------------//
    // mapToChord //
    //------------//
    /**
     * Set a ChordSyllableRelation between this lyric item and proper head chord.
     */
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

        final SystemInfo system = sig.getSystem();
        final Collection<Link> links = searchLinks(system);

        if (links.isEmpty()) {
            return;
        }

        final Link link = links.iterator().next();
        final HeadChordInter headChord = (HeadChordInter) link.partner;

        // Here, headChord is the best acceptable candidate.
        // But it may already be linked to another lyric item and if this other item belongs
        // to the same lyric line, we have a mutual exclusion to fix.
        final int xChord = headChord.getHeadLocation().x;
        final double centerX = getReferenceAbscissa();
        final double bestDx = Math.abs(xChord - centerX);
        final LyricLineInter line = (LyricLineInter) getEnsemble();

        for (Relation rel : sig.getRelations(headChord, ChordSyllableRelation.class)) {
            LyricItemInter other = (LyricItemInter) sig.getOppositeInter(headChord, rel);

            if (other.getEnsemble() == line) {
                double otherX = other.getReferenceAbscissa();
                double otherDx = Math.abs(xChord - otherX);

                if (bestDx >= otherDx) {
                    logger.info("{} preferred to {} in chord-lyric link.", other, this);
                    return;
                } else {
                    logger.info("{} preferred to {} in chord-lyric link.", this, other);
                    sig.removeEdge(rel);
                }
            }
        }

        link.applyTo(this);
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        // Standard addition task for this lyric item
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel));

        // Look for a containing lyric line
        final Point2D loc = getLocation();
        LyricLineInter line = new TextBuilder(sig.getSystem(), true).lookupLyricLine(loc);

        if (line == null) {
            // Create a new lyric line
            line = new LyricLineInter(1);
            line.setManual(true);
            line.setStaff(staff);
            tasks.add(new AdditionTask(sig, line, getBounds(), Collections.EMPTY_SET));
        }

        // Wrap lyric item into lyric line
        tasks.add(new LinkTask(sig, line, this, new Containment()));

        return tasks;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        // We can map only syllables
        if (itemKind != ItemKind.Syllable) {
            return Collections.EMPTY_LIST;
        }

        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupLink(system, systemHeadChords);

        return (link == null) ? Collections.EMPTY_LIST : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordSyllableRelation.class);
    }

    //----------------------//
    // getReferenceAbscissa //
    //----------------------//
    /**
     * Report the reference abscissa of this lyric item to be used for chord link test.
     * <p>
     * Left is too far on left, middle is too far on right, we use width/4 as a good heuristic
     *
     * @return the x to use to chord link test
     */
    private double getReferenceAbscissa ()
    {
        return getLocation().getX() + (getBounds().width / 4.0);
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

    //-------------//
    // isSeparator //
    //-------------//
    /**
     * Predicate to detect a separator.
     *
     * @param str the character to check
     * @return true if this is a separator
     */
    public static boolean isSeparator (String str)
    {
        return str.equals(EXTENSION_STRING) || str.equals(ELISION_STRING)
                       || str.equals(HYPHEN_STRING);
    }

    //---------------//
    // inferItemKind //
    //---------------//
    /**
     * Infer item kind from content.
     */
    private ItemKind inferItemKind ()
    {
        if (ELISION_STRING.equals(value)) {
            return ItemKind.Elision;
        } else if (EXTENSION_STRING.equals(value)) {
            return ItemKind.Extension;
        } else if (HYPHEN_STRING.equals(value)) {
            return ItemKind.Hyphen;
        } else {
            return ItemKind.Syllable;
        }
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this lyric item and a HeadChord nearby.
     *
     * @param system           containing system
     * @param systemHeadChords ordered collection of head chords in system
     * @return the link found or null
     */
    private Link lookupLink (SystemInfo system,
                             List<Inter> systemHeadChords)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final double centerX = getReferenceAbscissa();
        final boolean lookAbove;

        if (staff == null) {
            Staff relatedStaff = system.getStaffAtOrAbove(location);

            if (relatedStaff == null) {
                relatedStaff = system.getStaffAtOrBelow(location);
                lookAbove = false;
            } else {
                lookAbove = true;
            }

            setStaff(relatedStaff);
        } else {
            lookAbove = staff.pitchPositionOf(location) >= 0;
        }

        part = staff.getPart();
        int maxDx = system.getSheet().getScale().toPixels(constants.maxItemDx);

        // A word can start in a measure and finish in the next measure
        // Look for best aligned head-chord in proper staff
        double bestDx = Double.MAX_VALUE;
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
                                && (chord.getBottomStaff() == staff)) {
                        double dx = Math.abs(chord.getHeadLocation().x - centerX);

                        if (bestDx > dx) {
                            bestDx = dx;
                            bestChord = chord;
                        }
                    }
                }
            } else {
                for (AbstractChordInter chord : measure.getHeadChordsBelow(getLocation())) {
                    if (chord instanceof HeadChordInter && (chord.getTopStaff() == staff)) {
                        double dx = Math.abs(chord.getHeadLocation().x - centerX);

                        if (bestDx > dx) {
                            bestDx = dx;
                            bestChord = chord;
                        }
                    }
                }
            }
        }

        if ((bestChord != null) && (bestDx <= maxDx)) {
            return new Link(bestChord, new ChordSyllableRelation(), false);
        }

        return null;
    }

    //----------//
    // ItemKind //
    //----------//
    /**
     * Describes the kind of this lyrics item.
     */
    public static enum ItemKind
    {
        /** Just an elision. */
        Elision,
        /** Just an extension. */
        Extension,
        /** A hyphen between syllables. */
        Hyphen,
        /** A real syllable. */
        Syllable;
    }

    //--------------//
    // SyllabicType //
    //--------------//
    /**
     * Describes more precisely a syllable inside a word.
     */
    public static enum SyllabicType
    {
        /** Single-syllable word */
        SINGLE,
        /** Syllable that begins a word */
        BEGIN,
        /** Syllable at the middle of a word */
        MIDDLE,
        /** Syllable that ends a word */
        END;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxItemDx = new Scale.Fraction(
                5,
                "Maximum horizontal distance between a note and its lyric item");
    }
}
