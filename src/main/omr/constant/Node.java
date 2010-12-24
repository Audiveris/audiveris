//----------------------------------------------------------------------------//
//                                                                            //
//                                  N o d e                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import java.util.Comparator;

/**
 * Abstract class <code>Node</code> represents a node in the hierarchy of
 * packages and units (aka classes).
 *
 * @author Hervé Bitteur
 */
public abstract class Node
{
    //~ Static fields/initializers ---------------------------------------------

    /** For comparing Node instances according to their name */
    public static final Comparator<Node> nameComparator = new Comparator<Node>() {
        public int compare (Node n1,
                            Node n2)
        {
            return n1.getName()
                     .compareTo(n2.getName());
        }
    };


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
     * name, this method returns the last path component of the node, in other
     * words the non-qualified name.
     *
     * @return the non-qualified node name
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

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
