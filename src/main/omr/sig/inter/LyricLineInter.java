//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L y r i c L i n e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.text.FontInfo;
import omr.text.TextLine;
import omr.text.TextRole;
import omr.text.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import omr.sheet.Part;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

/**
 * Class {@code LyricLineInter} gathers one line of lyrics.
 * A lyric line is composed of instances of LyricItem, which can be Syllables, Hyphens, Extensions
 * or Elisions
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "lyric-line")
public class LyricLineInter
        extends SentenceInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LyricLineInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The line number. */
    @XmlAttribute
    private int number;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LyricLine object.
     *
     * @param bounds   the bounding box
     * @param grade    the interpretation quality
     * @param meanFont the font averaged on whole text line
     * @param words    the sequence of words (LyricItemInter's actually)
     */
    private LyricLineInter (Rectangle bounds,
                            double grade,
                            FontInfo meanFont,
                            List<WordInter> words)
    {
        super(bounds, grade, meanFont, TextRole.Lyrics, words);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LyricLineInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create a {@code LyricLineInter} from a TextLine.
     *
     * @param line the OCR'ed text line
     * @return the LyricLine inter
     */
    public static LyricLineInter create (TextLine line)
    {
        List<WordInter> wordInters = new ArrayList<WordInter>();

        for (TextWord word : line.getWords()) {
            LyricItemInter item = new LyricItemInter(word);
            wordInters.add(item);
        }

        LyricLineInter lyricLine = new LyricLineInter(
                line.getBounds(),
                line.getConfidence() * Inter.intrinsicRatio,
                line.getMeanFont(),
                wordInters);

        return lyricLine;
    }

    //------------------//
    // getFollowingLine //
    //------------------//
    /**
     * Retrieve the corresponding lyric line in the following part, if any
     *
     * @return the following lyric line, or null
     */
    public LyricLineInter getFollowingLine ()
    {
        LyricLineInter nextLine = null;

        // Check existence of similar line in following system part (within the same page)
        SystemInfo system = sig.getSystem();
        Staff staffAbove = system.getStaffAtOrAbove(this.getFirstWord().getLocation());
        Part nextPart = staffAbove.getPart().getFollowingInPage();

        if (nextPart != null) {
            // Retrieve the same lyrics line in the next (system) part
            if (nextPart.getLyrics().size() >= number) {
                nextLine = (LyricLineInter) nextPart.getLyrics().get(number - 1);
            }
        }

        return nextLine;
    }

    //-----------//
    // getNumber //
    //-----------//
    /**
     * Report the (1-based) number of this line within the part lyric lines.
     *
     * @return the line number
     */
    public int getNumber ()
    {
        return number;
    }

    //------------------//
    // getPrecedingLine //
    //------------------//
    /**
     * Retrieve the corresponding lyrics line in the preceding part, if any
     *
     * @return the preceding lyrics line, or null
     */
    public LyricLineInter getPrecedingLine ()
    {
        // Check existence of similar line in preceding system part
        SystemInfo system = sig.getSystem();
        Staff staffAbove = system.getStaffAtOrAbove(this.getFirstWord().getLocation());
        Part prevPart = staffAbove.getPart().getPrecedingInPage();

        if ((prevPart != null) && (prevPart.getLyrics().size() >= number)) {
            return (LyricLineInter) prevPart.getLyrics().get(number - 1);
        }

        return null;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    public String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" number:").append(number); ///.append(" items:").append(getMembers()).append("}");

        return sb.toString();
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    public void refineLyricSyllables ()
    {
        // Last item of preceding line is any
        LyricItemInter precedingItem = null;
        LyricLineInter precedingLine = getPrecedingLine();

        if (precedingLine != null) {
            precedingItem = (LyricItemInter) precedingLine.getLastWord();
        }

        // Now browse sequentially all our line items
        for (int i = 0; i < words.size(); i++) {
            LyricItemInter item = (LyricItemInter) words.get(i);

            // Following item (perhaps to be found in following line if needed)
            LyricItemInter followingItem = null;

            if (i < (words.size() - 1)) {
                followingItem = (LyricItemInter) words.get(i + 1);
            } else {
                LyricLineInter followingLine = getFollowingLine();

                if (followingLine != null) {
                    followingItem = (LyricItemInter) followingLine.getFirstWord();
                }
            }

            // We process only syllable items
            if (item.getItemKind() == LyricItemInter.ItemKind.Syllable) {
                item.defineSyllabicType(precedingItem, followingItem);
            }

            precedingItem = item;
        }
    }

    //-----------//
    // setNumber //
    //-----------//
    public void setNumber (int number)
    {
        this.number = number;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "LYRICS_\"" + getValue() + "\"";
    }
}
