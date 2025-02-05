//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e n t e n c e I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import static org.audiveris.omr.text.TextRole.ChordName;
import static org.audiveris.omr.text.TextRole.Direction;
import static org.audiveris.omr.text.TextRole.EndingNumber;
import static org.audiveris.omr.text.TextRole.EndingText;
import static org.audiveris.omr.text.TextRole.Lyrics;
import static org.audiveris.omr.text.TextRole.Metronome;
import static org.audiveris.omr.text.TextRole.PartName;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>SentenceInter</code> represents a full sentence of words.
 * <p>
 * Contained words are members of the sentence ensemble, and thus linked to this instance by
 * {@link Containment} relations.
 * <p>
 * This SentenceInter class is used for any text role other than Lyrics (Title, Direction, Number,
 * PartName, Creator et al, Rights, ChordName, EndingNumber, EndingText, UnknownRole).
 * <p>
 * For Lyrics role, the specific subclass {@link LyricLineInter} is used.
 * <p>
 * For ChordName role, SentenceInter class is used, but the contained (single) word is an
 * instance of ChordNameInter.
 * <p>
 * <img alt="Sentence diagram" src="doc-files/Sentence_Hierarchy.png">
 * <p>
 * NOTA: We could have decided to use separate classes for each different sentence role.
 * This is not the current implementation, hence caution is needed when changing sentence role.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sentence")
public class SentenceInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SentenceInter.class);

    /** For ordering sentences by their de-skewed ordinate. */
    public static final Comparator<SentenceInter> byOrdinate = (s1,
                                                                s2) -> {
        final Skew skew = s1.getSig().getSystem().getSkew();

        return Double.compare(
                skew.deskewed(s1.getLocation()).getY(),
                skew.deskewed(s2.getLocation()).getY());
    };

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Average font for the sentence. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.JaxbAdapter.class)
    protected FontInfo meanFont;

    /** Role of this sentence. */
    @XmlAttribute
    protected TextRole role;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected SentenceInter ()
    {
        super(null, null, null, (Double) null);
    }

    /**
     * Creates a new <code>SentenceInter</code> object.
     *
     * @param bounds   the bounding box
     * @param grade    the interpretation quality
     * @param meanFont the font averaged on whole text line
     * @param role     text role for the line
     */
    public SentenceInter (Rectangle bounds,
                          Double grade,
                          FontInfo meanFont,
                          TextRole role)
    {
        super(null, bounds, null, grade);

        this.meanFont = meanFont;
        this.role = role;
    }

    /**
     * Creates a new <code>SentenceInter</code> object, meant for user handling of glyph.
     *
     * @param role  the sentence role, if known, null otherwise
     * @param grade the interpretation quality
     */
    public SentenceInter (TextRole role,
                          Double grade)
    {
        this(null, grade, null, null);
        this.role = role;
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

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof WordInter)) {
            throw new IllegalArgumentException("Only WordInter can be added to Sentence");
        }

        EnsembleHelper.addMember(this, member);
    }

    //-------------//
    // assignStaff //
    //-------------//
    /**
     * Determine the related staff for this sentence.
     *
     * @param system   containing system
     * @param location sentence location
     * @return the related staff if found, null otherwise
     */
    public Staff assignStaff (SystemInfo system,
                              Point2D location)
    {
        if ((staff == null) && (role != TextRole.ChordName)) {
            staff = system.getStaffAtOrAbove(location);
        }

        if (staff == null) {
            staff = system.getStaffAtOrBelow(location);
        }

        return staff;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return (bounds != null) ? new Rectangle(bounds) : null;
    }

    //---------------------//
    // getExportedFontSize //
    //---------------------//
    /**
     * Report the font size to be exported (to MusicXML) for this text.
     *
     * @return the exported font size
     */
    public int getExportedFontSize ()
    {
        return (int) Math.rint(getMeanFont().pointsize * TextFont.TO_POINT);
    }

    //--------------//
    // getFirstWord //
    //--------------//
    /**
     * Report the first word in this sentence.
     *
     * @return first word or null if sentence is empty
     */
    public WordInter getFirstWord ()
    {
        final List<? extends Inter> words = getMembers();

        if (words.isEmpty()) {
            return null;
        }

        return (WordInter) words.get(0);
    }

    //-------------//
    // getLastWord //
    //-------------//
    /**
     * Report the last word in this sentence
     *
     * @return last word or null if empty
     */
    public WordInter getLastWord ()
    {
        final List<? extends Inter> words = getMembers();

        if (words.isEmpty()) {
            return null;
        }

        return (WordInter) words.get(words.size() - 1);
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the total number of characters in the sentence.
     *
     * @return count of characters, inter-word spaces excluded.
     */
    public int getLength ()
    {
        int length = 0;

        for (Inter word : getMembers()) {
            length += ((WordInter) word).getValue().length();
        }

        return length;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Return the starting point of the first word.
     *
     * @return starting point
     */
    public Point2D getLocation ()
    {
        final Inter first = getFirstWord();

        if (first == null) {
            return null;
        }

        return ((WordInter) first).getLocation();
    }

    //-------------//
    // getMeanFont //
    //-------------//
    /**
     * Report the sentence mean font.
     *
     * @return the mean Font
     */
    public FontInfo getMeanFont ()
    {
        return meanFont;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byAbscissa);
    }

    //---------//
    // getRole //
    //---------//
    /**
     * Report the sentence role.
     *
     * @return the sentence role
     */
    public TextRole getRole ()
    {
        return role;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "SENTENCE";
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the sentence text content, built out of the contained words.
     *
     * @return text content
     */
    public String getValue ()
    {
        return getMembers().stream().map(w -> ((WordInter) w).getValue()) //
                .collect(Collectors.joining(" "));
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals()) //
                .append(' ').append((meanFont != null) ? "mFont:" + meanFont.getMnemo() : "NO_FONT") //
                .append(' ').append((role != null) ? role : "NO_ROLE") //
                .toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Invalidate cached information. (following the addition or removal of a word)
     */
    @Override
    public void invalidateCache ()
    {
        bounds = null;

        if (meanFont == null) {
            List<Inter> members = getMembers();

            if (!members.isEmpty()) {
                WordInter firstWord = (WordInter) members.get(0);
                meanFont = new FontInfo(firstWord.getFontInfo(), firstWord.getFontInfo().pointsize);
            }
        }

        // TODO: should we update sentence grade?
    }

    //------//
    // link //
    //------//
    /**
     * Try to link this sentence, based on its role.
     *
     * @param system the related system
     */
    public void link (SystemInfo system)
    {
        try {
            if (isVip()) {
                logger.info("VIP link {}", this);
            }

            if (role == null) {
                logger.info("No role for {}", this);
                return;
            }

            final Point2D location = getLocation();
            getBounds();
            final Scale scale = system.getSheet().getScale();

            switch (role) {
                case Lyrics -> {
                    // Map each syllable with proper chord, in assigned staff
                    for (Inter wInter : getMembers()) {
                        final LyricItemInter item = (LyricItemInter) wInter;
                        final int profile = Math.max(item.getProfile(), system.getProfile());
                        item.mapToChord(profile);
                    }
                }

                case Direction -> {
                    if (!sig.hasRelation(this, ChordSentenceRelation.class)) {
                        // Map sentence with proper chord, preferably above for a direction
                        final MeasureStack stack = system.getStackAt(location);

                        if (stack == null) {
                            logger.info("No measure stack for {} {}", this, getValue());
                        } else {
                            final int xGapMax = scale.toPixels(ChordSentenceRelation.getXGapMax());
                            final Rectangle box = new Rectangle(bounds);
                            box.grow(xGapMax, 0);

                            final AbstractChordInter chord = stack.getEventChord(
                                    location,
                                    box,
                                    true);

                            if (chord != null) {
                                sig.addEdge(chord, this, new ChordSentenceRelation());
                            } else {
                                logger.info("No chord near {} {}", this, getValue());
                            }
                        }
                    }
                }

                case PartName -> {
                    // Assign part name to proper part
                    staff = system.getClosestStaff(getCenter());
                    part = staff.getPart();
                    part.setName(this);
                }

                case ChordName -> {
                    // Map each word with proper chord, in assigned staff
                    for (Inter wInter : getMembers()) {
                        final ChordNameInter word = (ChordNameInter) wInter;
                        final Link link = word.lookupLink(system);

                        if (link == null) {
                            logger.info("No chord below {}", word);
                        } else {
                            link.applyTo(wInter);
                        }
                    }
                }

                case EndingNumber, EndingText -> {
                    // Look for related ending
                    final Link link = lookupEndingLink(system);

                    if ((link != null) && (null == sig.getRelation(
                            link.partner,
                            this,
                            EndingSentenceRelation.class))) {
                        sig.addEdge(link.partner, this, link.relation);
                    }
                }
            }

            // Roles UnknownRole, Title, Number, Creator*, Rights stand by themselves
            // and thus need no link.

        } catch (Exception ex) {
            logger.warn("Error in link {} {}", this, ex.toString(), ex);
        }
    }

    //------------------//
    // lookupEndingLink //
    //------------------//
    /**
     * Try to detect link from a suitable containing ending.
     *
     * @param system surrounding system
     * @return detected link or null
     */
    public Link lookupEndingLink (SystemInfo system)
    {
        final Rectangle textBox = getBounds();
        final SIGraph theSig = system.getSig();
        final List<Inter> endings = theSig.inters(EndingInter.class);

        for (Inter ending : endings) {
            if (textBox.intersects(ending.getBounds())) {
                return new Link(ending, new EndingSentenceRelation(), false);
            }
        }

        return null;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        if (role == TextRole.PartName) {
            // Set pointer from part to this partName
            staff.getPart().setName(this);
        }

        // Standard addition task for this sentence
        return super.preAdd(cancel, toPublish);
    }

    //-----------//
    // preRemove //
    //-----------//
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        if (role == TextRole.PartName) {
            // Remove pointer from part to this partName
            staff.getPart().removeName(this);
        }

        return super.preRemove(cancel);
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof WordInter)) {
            throw new IllegalArgumentException("Only WordInter can be removed from Sentence");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        if (role != TextRole.EndingNumber && role != TextRole.EndingText) {
            return super.searchLinks(system);
        }

        // Look for a suitable EndingInter
        final Link link = lookupEndingLink(system);

        return (link != null) ? Collections.singleton(link) : Collections.emptySet();
    }

    //-------------//
    // setMeanFont //
    //-------------//
    /**
     * Assign the sentence mean font.
     *
     * @param meanFont the new mean Font
     */
    public void setMeanFont (FontInfo meanFont)
    {
        this.meanFont = meanFont;
    }

    //---------//
    // setRole //
    //---------//
    /**
     * Assign a new role.
     *
     * @param role the new role
     */
    public void setRole (TextRole role)
    {
        this.role = role;
    }

    //--------//
    // unlink //
    //--------//
    /**
     * Unlink the sentence, according to its role, with its related entity if any.
     *
     * @param oldRole the role this sentence had
     */
    public void unlink (TextRole oldRole)
    {
        try {
            if (isVip()) {
                logger.info("VIP unlink for {}", this);
            }

            switch (oldRole) {
                case null -> logger.info("Null old role for {}", this);
                default -> {}

                case Lyrics -> getMembers().forEach(
                        wInter -> sig.getRelations(wInter, ChordSyllableRelation.class).forEach(
                                rel -> sig.removeEdge(rel)));

                case Direction, Metronome -> sig.getRelations(this, ChordSentenceRelation.class)
                        .forEach(rel -> sig.removeEdge(rel));

                case PartName -> {
                    // Look for proper part
                    staff = sig.getSystem().getClosestStaff(getCenter());
                    part = staff.getPart();
                    part.setName((SentenceInter) null);
                }

                case ChordName -> getMembers().forEach(
                        wInter -> sig.getRelations(wInter, ChordNameRelation.class).forEach(
                                rel -> sig.removeEdge(rel)));

                case EndingNumber, EndingText -> //
                        sig.getRelations(this, EndingSentenceRelation.class).forEach(
                                rel -> sig.removeEdge(rel));
            }
        } catch (Exception ex) {
            logger.warn("Error in unlink for {} {}", this, ex.toString(), ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * Create a <code>SentenceInter</code> from a TextLine.
     *
     * @param line the OCR'd text line
     * @return the sentence inter
     */
    public static SentenceInter create (TextLine line)
    {
        SentenceInter sentence = new SentenceInter(
                line.getBounds(),
                line.getGrade(),
                line.getMeanFont(),
                line.getRole());

        return sentence;
    }
}
