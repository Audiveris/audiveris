//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
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
import omr.text.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code WordInter} represents a text word.
 *
 * @author Hervé Bitteur
 */
public class WordInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(WordInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Word text content. */
    private final String value;

    /** Detected font attributes. */
    private final FontInfo fontInfo;

    /** Precise word starting point. */
    private final Point location;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code WordInter} object.
     *
     * @param textWord the OCR'ed text word
     */
    public WordInter (TextWord textWord)
    {
        super(
                textWord.getGlyph(),
                textWord.getBounds(),
                Shape.TEXT,
                textWord.getConfidence() * Inter.intrinsicRatio);
        value = textWord.getValue();
        fontInfo = textWord.getFontInfo();
        location = textWord.getLocation();
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

    //-------------//
    // getEnsemble //
    //-------------//
    /**
     * Report the sentence that contains this word.
     *
     * @return the containing sentence
     */
    @Override
    public SentenceInter getEnsemble ()
    {
        return (SentenceInter) ensemble;
    }

    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
     *
     * @return the fontInfo
     */
    public FontInfo getFontInfo ()
    {
        return fontInfo;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * @return the location
     */
    public Point getLocation ()
    {
        return location;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //-------------//
    // setEnsemble //
    //-------------//
    public void setEnsemble (SentenceInter sentence)
    {
        this.ensemble = sentence;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "WORD_\"" + value + "\"";
    }
}
