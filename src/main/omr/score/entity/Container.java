//----------------------------------------------------------------------------//
//                                                                            //
//                             C o n t a i n e r                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;


/**
 * Class <code>Container</code> is meant for intermediate containers with
 * no values by themselves. Thus, there are shown in the score tree, only if
 * have children.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
     * @param name a name for this container
     */
    public Container (VisitableNode container,
                      String        name)
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
