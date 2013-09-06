//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c R e l a t i o n                          //
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
 * Class {@code BasicRelation}
 *
 * @author Hervé Bitteur
 */
public class BasicRelation
    implements Relation
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BasicRelation object.
     */
    public BasicRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString ()
    {
        return getName() + " " + internals();
    }

    protected String internals ()
    {
        return "";
    }
    
    @Override
    public String getName()
    {
        return "Relation";
    }
}
