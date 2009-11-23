//----------------------------------------------------------------------------//
//                                                                            //
//                                  S k e w                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;


/**
 * Class <code>Skew</code> handles the skew angle of a given sheet picture.
 *
 * @see SkewBuilder
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Skew
{
    //~ Instance fields --------------------------------------------------------

    /** Skew angle as computed */
    private double angle;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Skew //
    //------//
    /**
     * This is meant to generate a skew entity, when its key informations (the
     * skew angle) is already known.
     *
     * @param angle the skew angle
     */
    public Skew (double angle)
    {
        this.angle = angle;
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // angle //
    //-------//
    /**
     * Report the skew angle
     *
     * @return the angle value, expressed in radians
     */
    public double angle ()
    {
        return angle;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Skew angle=" + angle + "}";
    }
}
