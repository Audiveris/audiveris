//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e n t e n c e I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.text.FontInfo;
import omr.text.TextLine;
import omr.text.TextRoleInfo;
import omr.text.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SentenceInter} represents a full sentence of words.
 *
 * @author Hervé Bitteur
 */
public class SentenceInter
        extends AbstractInter
        implements InterMutableEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SentenceInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Average font for the sentence. */
    private final FontInfo meanFont;

    /** Role of this sentence. */
    private final TextRoleInfo roleInfo;

    /** Sequence of sentence words. */
    private final List<WordInter> words;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SentenceInter} object.
     *
     * @param box   the bounding box
     * @param grade the interpretation quality
     * @param words the sequence of words
     */
    private SentenceInter (Rectangle box,
                           double grade,
                           FontInfo meanFont,
                           TextRoleInfo roleInfo,
                           List<WordInter> words)
    {
        super(null, box, Shape.TEXT, grade);

        this.meanFont = meanFont;
        this.roleInfo = roleInfo;
        this.words = words;

        for (WordInter word : words) {
            word.setEnsemble(this);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void addMember (Inter member)
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public TextRoleInfo getRole ()
    {
        return roleInfo;
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
}
