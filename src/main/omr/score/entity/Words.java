//----------------------------------------------------------------------------//
//                                                                            //
//                                 W o r d s                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>Words</code> represents a words event
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Words
    extends AbstractDirection
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying text */
    private Text.DirectionText text;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Words //
    //-------//
    /**
     * Creates a new instance of Words event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark, if any
     * @param glyph the underlying glyph
     * @param text the underlying text
     */
    public Words (Measure            measure,
                  SystemPoint        point,
                  Chord              chord,
                  Glyph              glyph,
                  Text.DirectionText text)
    {
        super(measure, point, chord, glyph);
        this.text = text;

        // Force the point as being, not a computed center, but the reference
        // location
        setPoint(point);
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
