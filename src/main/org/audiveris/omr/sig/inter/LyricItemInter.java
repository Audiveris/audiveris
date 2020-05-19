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

import java.awt.Point;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Part;
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
import static org.audiveris.omr.util.StringUtil.*;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.constant.Constant;

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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        checkAbnormal();
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        if (itemKind == ItemKind.Syllable) {
            // Check if connected to a head chord
            setAbnormal(getHeadChord() == null);
        }

        return isAbnormal();
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

    //--------------//
    // getHeadChord //
    //--------------//
    /**
     * Report the head chord this lyric item relates to, if any.
     *
     * @return the related head chord or null
     */
    public HeadChordInter getHeadChord ()
    {
        for (Relation rel : sig.getRelations(this, ChordSyllableRelation.class)) {
            return (HeadChordInter) sig.getOppositeInter(this, rel);
        }

        return null;
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
        final HeadChordInter chord = getHeadChord();

        if (chord != null) {
            return chord.getVoice();
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
        if (!isSyllable()) {
            return;
        }

        // Already mapped?
        if (sig.hasRelation(this, ChordSyllableRelation.class)) {
            return;
        }

        Link link = lookupLink(staff, null);

        if (link == null) {
            return;
        }

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
                    logger.debug("{} preferred to {} in chord-lyric link.", other, this);

                    // Find a 2nd choice for this syllable
                    link = lookupLink(staff, Arrays.asList(headChord));
                } else {
                    logger.debug("{} preferred to {} in chord-lyric link.", this, other);
                    sig.removeEdge(rel);

                    // Find a 2nd choice for other syllable
                    Link otherLink = other.lookupLink(staff, Arrays.asList(headChord));

                    if (otherLink != null) {
                        otherLink.applyTo(other);
                    }
                }
            }
        }

        if (link != null) {
            link.applyTo(this);
        }
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
        final SystemInfo system = staff.getSystem();
        LyricLineInter line = new TextBuilder(system, true).lookupLyricLine(loc);

        if (line == null) {
            // Create a new lyric line
            line = new LyricLineInter(1);
            line.setManual(true);
            line.setStaff(staff);
            tasks.add(new AdditionTask(system.getSig(), line, getBounds(), Collections.emptySet()));
        }

        // Wrap lyric item into lyric line
        tasks.add(new LinkTask(system.getSig(), line, this, new Containment()));

        return tasks;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this lyric item and a HeadChord nearby.
     *
     * @param theStaff  staff to be looked up
     * @param blackList head chords black-listed, perhaps null
     * @return the link found or null
     */
    public Link lookupLink (Staff theStaff,
                            Collection<HeadChordInter> blackList)
    {
        final double refX = getReferenceAbscissa();
        final double refY = getLocation().getY();
        final boolean lookAbove = theStaff.pitchPositionOf(location) >= 0;

        Part thePart = theStaff.getPart();
        int maxDx = theStaff.getSystem().getSheet().getScale().toPixels(constants.maxItemDx);

        // A word can start in a measure and finish in the next measure
        // Look for head-chords in proper staff that are compatible abscissawise with syllable
        // Then select the closest one, using euclidian distance.
        double bestD2 = Double.MAX_VALUE;
        HeadChordInter bestChord = null;

        for (Measure measure : thePart.getMeasures()) {
            // Select only possible measures
            if ((measure.getAbscissa(LEFT, theStaff) - maxDx) > refX) {
                break;
            }

            if ((measure.getAbscissa(RIGHT, theStaff) + maxDx) < refX) {
                continue;
            }

            if (lookAbove) {
                Collection<HeadChordInter> chords = measure.getHeadChordsAbove(getLocation());
                if (blackList != null) {
                    chords.removeAll(blackList);
                }

                for (HeadChordInter chord : chords) {
                    if (chord.getBottomStaff() == theStaff) {
                        Point chordCenter = chord.getCenter();
                        double dx = Math.abs(chordCenter.x - refX);

                        if (dx <= maxDx) {
                            double d2 = Point2D.distanceSq(refX, refY, chordCenter.x, chordCenter.y);

                            if (d2 < bestD2) {
                                bestD2 = d2;
                                bestChord = chord;
                            }
                        }
                    }
                }
            } else {
                Collection<HeadChordInter> chords = measure.getHeadChordsBelow(getLocation());
                if (blackList != null) {
                    chords.removeAll(blackList);
                }

                for (HeadChordInter chord : chords) {
                    if (chord.getTopStaff() == theStaff) {
                        Point chordCenter = chord.getCenter();
                        double dx = Math.abs(chordCenter.x - refX);

                        if (dx <= maxDx) {
                            double d2 = Point2D.distanceSq(refX, refY, chordCenter.x, chordCenter.y);

                            if (d2 < bestD2) {
                                bestD2 = d2;
                                bestChord = chord;
                            }
                        }
                    }
                }
            }
        }

        if (bestChord == null) {
            return null;
        }

        return new Link(bestChord, new ChordSyllableRelation(), false);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        // We can map only syllables
        if (itemKind != ItemKind.Syllable) {
            return Collections.emptyList();
        }

        Link link = lookupLink(staff, null);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
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
     *
     * @return the x to use to chord link test
     */
    private double getReferenceAbscissa ()
    {
        return getLocation().getX() + (getBounds().width * constants.widthRatio.getValue());
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

    //------------//
    // isSyllable //
    //------------//
    /**
     * Report whether this item is a syllable (rather than hyphen, extension or elision).
     *
     * @return true if so
     */
    public boolean isSyllable ()
    {
        return itemKind == ItemKind.Syllable;
    }

    //-------------//
    // isSeparator //
    //-------------//
    /**
     * Predicate to detect a separator.
     *
     * @param ch the character to check
     * @return true if this is a separator
     */
    public static boolean isSeparator (char ch)
    {
        return (ch == HYPHEN) || (ch == ELISION_CHAR) || (EXTENSIONS.indexOf(ch) != -1);
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
        } else if (EXTENSIONS.contains(value)) {
            return ItemKind.Extension;
        } else if (HYPHEN_STRING.equals(value)) {
            return ItemKind.Hyphen;
        } else {
            return ItemKind.Syllable;
        }
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
                3,
                "Maximum horizontal distance between a note and its lyric item");

        private final Constant.Ratio widthRatio = new Constant.Ratio(
                0.5,
                "Reference abscissa as ratio of item width");
    }
}
