//----------------------------------------------------------------------------//
//                                                                            //
//                    D i r e c t i o n S t a t e m e n t                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.text.Sentence;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>DirectionStatement</code> represents a direction in the score
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class DirectionStatement
    extends AbstractDirection
{
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
     * @param measure measure that contains this mark
     * @param location location of mark
     * @param chord the chord related to the mark, if any
     * @param sentence the underlying sentence
     * @param text the sentence text
     */
    public DirectionStatement (Measure            measure,
                               SystemPoint        location,
                               Chord              chord,
                               Sentence           sentence,
                               Text.DirectionText text)
    {
        super(measure, location, chord, sentence.getGlyphs().first());
        this.text = text;

        // Force the point as being, not a computed center, but the reference
        // location
        setPoint(location);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getText //
    //---------//
    public Text.DirectionText getText ()
    {
        return text;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
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
