//----------------------------------------------------------------------------//
//                                                                            //
//                             C o n t a i n e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

/**
 * Class {@code Container} is meant for intermediate containers with
 * no values by themselves. Thus, there are shown in the score tree, only if
 * have children.
 *
 * @author Hervé Bitteur
 */
public class Container
        extends VisitableNode
{
    //~ Instance fields --------------------------------------------------------

    /** Container name meant for debugging mainly */
    private final String name;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // Container //
    //-----------//
    /**
     * Creates a new Container object.
     *
     * @param container the parent in the score hierarchy
     * @param name      a name for this container
     */
    public Container (VisitableNode container,
                      String name)
    {
        super(container);
        this.name = name;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return name;
    }
}
