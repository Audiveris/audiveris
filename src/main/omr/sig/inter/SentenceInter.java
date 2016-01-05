//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e n t e n c e I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.text.FontInfo;
import omr.text.TextLine;
import omr.text.TextRole;
import omr.text.TextWord;

import omr.ui.symbol.TextFont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import omr.sheet.SystemInfo;

/**
 * Class {@code SentenceInter} represents a full sentence of words.
 * <p>
 * This class is used for any text role other than Lyrics (Title, Direction, Number, PartName,
 * Creator et al, Rights, ChordName, UnknownRole)
 * For Lyrics role , the specific subclass {@link LyricLineInter} is used.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sentence")
public class SentenceInter
        extends AbstractInter
        implements InterMutableEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SentenceInter.class);

    /** For ordering sentences by their de-skewed ordinate. */
    public static Comparator<SentenceInter> byOrdinate = new Comparator<SentenceInter>()
    {
        @Override
        public int compare (SentenceInter s1,
                            SentenceInter s2)
        {
            Point2D dsk1 = s1.getSig().getSystem().getSkew().deskewed(s1.getLocation());
            Point2D dsk2 = s2.getSig().getSystem().getSkew().deskewed(s2.getLocation());

            return Double.compare(dsk1.getY(), dsk2.getY());
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of sentence words. */
    @XmlIDREF
    @XmlElement(name = "word")
    protected final List<WordInter> words;

    /** Average font for the sentence. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected final FontInfo meanFont;

    /** Role of this sentence. */
    @XmlAttribute
    protected final TextRole role;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SentenceInter} object.
     *
     * @param bounds   the bounding bounds
     * @param grade    the interpretation quality
     * @param meanFont the font averaged on whole text line
     * @param role     text role for the line
     * @param words    the sequence of words
     */
    protected SentenceInter (Rectangle bounds,
                             double grade,
                             FontInfo meanFont,
                             TextRole role,
                             List<WordInter> words)
    {
        super(null, bounds, Shape.TEXT, grade);

        this.meanFont = meanFont;
        this.role = role;
        this.words = words;

        for (WordInter word : words) {
            word.setEnsemble(this);
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected SentenceInter ()
    {
        super(null, null, null, null);
        this.words = null;
        this.meanFont = null;
        this.role = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        List<WordInter> wordInters = new ArrayList<WordInter>();

        for (TextWord word : line.getWords()) {
            WordInter wordInter = new WordInter(word);
            wordInters.add(wordInter);
        }

        SentenceInter sentence = new SentenceInter(
                line.getBounds(),
                line.getConfidence() * Inter.intrinsicRatio,
                line.getMeanFont(),
                line.getRole(),
                wordInters);

        return sentence;
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
        throw new UnsupportedOperationException("Not supported yet.");
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
    public Rectangle getBounds ()
    {
        Rectangle bounds = null;

        for (WordInter word : words) {
            if (bounds == null) {
                bounds = word.getBounds();
            } else {
                bounds.add(word.getBounds());
            }
        }

        return bounds;
    }

    //---------------------//
    // getExportedFontSize //
    //---------------------//
    /**
     * Report the font size to be exported for this text
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
        if (words.isEmpty()) {
            return null;
        }

        return words.get(0);
    }

    //-------------//
    // getLastWord //
    //-------------//
    public WordInter getLastWord ()
    {
        if (words.isEmpty()) {
            return null;
        }

        return words.get(words.size() - 1);
    }

    //-------------//
    // getLocation //
    //-------------//
    public Point getLocation ()
    {
        return words.get(0).getLocation();
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
    public List<? extends Inter> getMembers ()
    {
        return words;
    }

    //---------//
    // getRole //
    //---------//
    /**
     * Report the line role.
     *
     * @return the roleInfo
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
        for (WordInter word : words) {
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

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (member instanceof WordInter) {
            WordInter word = (WordInter) member;
            logger.info("{} about to remove {}", this, word);
            words.remove(word);

            if (words.isEmpty()) {
                logger.info("Deleting empty {}", this);
                delete();
            }
        } else {
            throw new IllegalArgumentException("Only WordInter can be removed from Sentence");
        }
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "SENTENCE_\"" + getValue() + "\"";
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

    //-------------//
    // assignStaff //
    //-------------//
    public void assignStaff (SystemInfo system)
    {
        if (staff != null) {
            return;
        }

        final Point loc = getLocation();

        if (role != TextRole.ChordName) {
            staff = system.getStaffAtOrAbove(loc);
        }

        if (staff == null) {
            staff = system.getStaffAtOrBelow(loc);
        }

        if (staff != null) {
            for (Inter wInter : getMembers()) {
                WordInter wordInter = (WordInter) wInter;
                wordInter.setStaff(staff);
            }
        }
    }
}
