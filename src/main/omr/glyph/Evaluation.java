//----------------------------------------------------------------------------//
//                                                                            //
//                            E v a l u a t i o n                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;


/**
 * Class <code>Evaluation</code> gathers a pair composed of a glyph shape and
 * its doubt.
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Evaluation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Absolutely no doubt for shape manually assigned */
    public static final double MANUAL_NO_DOUBT = -1;

    /** No doubt for shape structurally assigned */
    public static final double NO_DOUBT = 0;

    //~ Instance fields --------------------------------------------------------

    /** The evaluated shape */
    public Shape shape;

    /** The evaluation doubt (smaller is better) */
    public double doubt;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // Evaluation //
    //------------//
    /**
     * Create an initialized evaluation
     *
     *
     * @param shape the shape this evaluation measures
     * @param doubt the measurement result (smaller is better)
     */
    public Evaluation (Shape  shape,
                       double doubt)
    {
        this.shape = shape;
        this.doubt = doubt;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Just a readable output
     *
     * @return an ascii description of the evaluation
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(shape);
        sb.append("(")
          .append((float) doubt)
          .append(")");

        return sb.toString();
    }
}
