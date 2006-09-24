//----------------------------------------------------------------------------//
//                                                                            //
//                                  N o d e                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.constant;


/**
 * Abstract class <code>Node</code> represents a node in the hierarchy of
 * packages and units (aka classes).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Node
{
    //~ Instance fields --------------------------------------------------------

    /** (Fully qualified) name of the node */
    private final String name;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Node //
    //------//
    /**
     * Create a new Node.
     *
     * @param name the fully qualified node name
     */
    public Node (String name)
    {
        this.name = name;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    /**
     * Report the fully qualified name for this node
     *
     * @return the fully qualified node name
     */
    public String getName ()
    {
        return name;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Since <code>toString()</code> is used by JTreeTable to display the node
     * name, this method return the last path component of the node, in other
     * words the non-qualified name.
     *
     * @return the non-qualified node name
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer();

        //sb.append("<html><font color=\"#0000FF\">");
        int dot = name.lastIndexOf('.');

        if (dot != -1) {
            sb.append(name.substring(dot + 1));
        } else {
            sb.append(name);
        }

        //sb.append("</font></html>");
        return sb.toString();
    }
}
