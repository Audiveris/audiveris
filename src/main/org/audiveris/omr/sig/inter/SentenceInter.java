//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e n t e n c e I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SentenceInter} represents a full sentence of words.
 * <p>
 * This class is used for any text role other than Lyrics (Title, Direction, Number, PartName,
 * Creator et al, Rights, ChordName, UnknownRole).
 * <p>
 * For Lyrics role, the specific subclass {@link LyricLineInter} is used.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sentence")
public class SentenceInter
        extends AbstractInter
        implements InterEnsemble
{

    private static final Logger logger = LoggerFactory.getLogger(SentenceInter.class);

    /** For ordering sentences by their de-skewed ordinate. */
    public static final Comparator<SentenceInter> byOrdinate = new Comparator<SentenceInter>()
    {
        @Override
        public int compare (SentenceInter s1,
                            SentenceInter s2)
        {
            final Skew skew = s1.getSig().getSystem().getSkew();

            return Double.compare(skew.deskewed(s1.getLocation()).getY(), skew.deskewed(s2
                                  .getLocation()).getY());
        }
    };

    //
    // Persistent data
    //----------------
    //
    /** Average font for the sentence. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected final FontInfo meanFont;

    /** Role of this sentence. */
    @XmlAttribute
    protected TextRole role;

    /**
     * Creates a new {@code SentenceInter} object.
     *
     * @param bounds   the bounding bounds
     * @param grade    the interpretation quality
     * @param meanFont the font averaged on whole text line
     * @param role     text role for the line
     */
    public SentenceInter (Rectangle bounds,
                          double grade,
                          FontInfo meanFont,
                          TextRole role)
    {
        super(null, bounds, null, grade);

        this.meanFont = meanFont;
        this.role = role;
    }

    /**
     * Creates a new {@code SentenceInter} object, meant for user handling of glyph.
     *
     * @param grade the interpretation quality
     */
    public SentenceInter (double grade)
    {
        super(null, null, Shape.LYRICS, grade);

        this.meanFont = null;
        this.role = TextRole.Lyrics;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected SentenceInter ()
    {
        super(null, null, null, null);
        this.meanFont = null;
        this.role = null;
    }

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
                              Point location)
    {
        if ((staff == null) && (role != TextRole.ChordName)) {
            staff = system.getStaffAtOrAbove(location);
        }

        if (staff == null) {
            staff = system.getStaffAtOrBelow(location);
        }

        return staff;
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a {@code SentenceInter} from a TextLine.
     *
     * @param line the OCR'ed text line
     * @return the sentence inter
     */
    public static SentenceInter create (TextLine line)
    {
        SentenceInter sentence = new SentenceInter(line.getBounds(), line.getConfidence()
                                                                             * Grades.intrinsicRatio,
                                                   line
                                                           .getMeanFont(), line.getRole());

        return sentence;
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

        return new Rectangle(bounds);
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
        return (int) Math.rint(meanFont.pointsize * TextFont.TO_POINT);
    }

    //--------------//
    // getFirstWord //
    //--------------//
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
    public WordInter getLastWord ()
    {
        final List<? extends Inter> words = getMembers();

        if (words.isEmpty()) {
            return null;
        }

        return (WordInter) words.get(words.size() - 1);
    }

    //-------------//
    // getLocation //
    //-------------//
    public Point getLocation ()
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

    //----------//
    // getValue //
    //----------//
    public String getValue ()
    {
        StringBuilder sb = null;

        // Use each word value
        for (Inter word : getMembers()) {
            String str = ((WordInter) word).getValue();

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

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "SENTENCE";
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(' ').append(meanFont.getMnemo());
        sb.append(' ').append(role);

        return sb.toString();
    }
}
