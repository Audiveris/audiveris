//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c E x c l u s i o n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code BasicExclusion}
 *
 * @author Hervé Bitteur
 */
public class BasicExclusion
        extends BasicRelation
        implements Exclusion
{
    //~ Instance fields --------------------------------------------------------

    public final Cause cause;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicExclusion object.
     *
     * @param cause root cause of this exclusion
     */
    public BasicExclusion (Cause cause)
    {
        this.cause = cause;
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public String getName ()
    {
        return "Exclusion";
    }

    @Override
    protected String internals ()
    {
        return super.internals() + cause;
    }
}
