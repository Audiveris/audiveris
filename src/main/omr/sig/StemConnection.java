//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t e m C o n n e c t i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code StemConnection} is the basis for connections to a stem.
 *
 * @author Hervé Bitteur
 */
public abstract class StemConnection
        extends AbstractConnection
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Which part of stem is used?. */
    protected StemPortion stemPortion;

    //----------------//
    // getStemPortion //
    //----------------//
    /**
     * @return the stem Portion
     */
    public StemPortion getStemPortion ()
    {
        return stemPortion;
    }

    //----------------//
    // setStemPortion //
    //----------------//
    /**
     * @param stemPortion the stem portion to set
     */
    public void setStemPortion (StemPortion stemPortion)
    {
        this.stemPortion = stemPortion;
    }

}
