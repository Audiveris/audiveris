//----------------------------------------------------------------------------//
//                                                                            //
//                           C h o r d S y m b o l                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.visitor.ScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code ChordSymbol} represents a chord symbol in the score.
 *
 * @author Chris Lahey
 * @author Hervé Bitteur
 */
public class ChordSymbol
        extends MeasureElement
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ChordSymbol.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Underlying text. */
    private final Text.ChordText text;

    /** The underlying chord information. */
    public ChordInfo info;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // ChordSymbol //
    //-------------//
    /**
     * Creates a new instance of ChordSymbol event
     *
     * @param measure        measure that contains this mark
     * @param referencePoint the reference location of the mark
     * @param chord          the chord related to the mark, if any
     * @param text           the sentence text
     */
    public ChordSymbol (Measure measure,
                        Point referencePoint,
                        Chord chord,
                        Text.ChordText text)
    {
        super(
                measure,
                true,
                referencePoint,
                chord,
                text.getSentence().getFirstWord().getGlyph());

        this.text = text;

        info = ChordInfo.create(text.getContent());

        // Register at its related chord
        if (chord != null) {
            chord.setChordSymbol(this);
        } else {
            addError(getGlyph(), "Chord statement with no related chord");
        }
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------//
    // getInfo //
    //---------//
    public ChordInfo getInfo ()
    {
        return info;
    }

    //---------//
    // getText //
    //---------//
    public Text.ChordText getText ()
    {
        return text;
    }

    //-----------------------//
    // computeReferencePoint //
    //-----------------------//
    @Override
    protected void computeReferencePoint ()
    {
        setReferencePoint(text.getReferencePoint());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return info.toString();
    }
}
