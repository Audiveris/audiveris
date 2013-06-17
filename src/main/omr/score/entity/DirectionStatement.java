//----------------------------------------------------------------------------//
//                                                                            //
//                    D i r e c t i o n S t a t e m e n t                     //
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
 * Class {@code DirectionStatement} represents a direction in the score
 *
 * @author Hervé Bitteur
 */
public class DirectionStatement
        extends AbstractDirection
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DirectionStatement.class);

    //~ Instance fields --------------------------------------------------------
    /** The underlying text */
    private Text.DirectionText text;

    //~ Constructors -----------------------------------------------------------
    //--------------------//
    // DirectionStatement //
    //--------------------//
    /**
     * Creates a new instance of DirectionStatement event
     *
     * @param measure        measure that contains this mark
     * @param referencePoint the reference location of the mark
     * @param chord          the chord related to the mark, if any
     * @param text           the sentence text
     */
    public DirectionStatement (Measure measure,
                               Point referencePoint,
                               Chord chord,
                               Text.DirectionText text)
    {
        super(
                measure,
                referencePoint,
                chord,
                text.getSentence().getFirstWord().getGlyph());
        this.text = text;
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
    // getText //
    //---------//
    public Text.DirectionText getText ()
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
        return text.toString();
    }
}
