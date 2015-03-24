//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a g e N o d e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.util.TreeNode;

/**
 * Class {@code PageNode} represents a score entity in a page, so its
 * direct children are {@link ScoreSystem} instances.
 *
 * @author Hervé Bitteur
 */
public abstract class PageNode
        extends ScoreNode
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a node in the tree, given its container
     *
     * @param container the containing node, or null otherwise
     */
    public PageNode (ScoreNode container)
    {
        super(container);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getContextString //
    //------------------//
    @Override
    public String getContextString ()
    {
        StringBuilder sb = new StringBuilder(super.getContextString());

        Page page = getPage();
        Sheet sheet = page.getSheet();

        if (sheet.getBook().isMultiSheet()) {
            sb.append("[#").append(sheet.getIndex());

            if (sheet.getPages().size() > 1) {
                sb.append("-").append(1 + sheet.getPages().indexOf(page));
            }

            sb.append("] ");
        }

        return sb.toString();
    }

    //---------//
    // getPage //
    //---------//
    /**
     * Report the containing page
     *
     * @return the containing page
     */
    public Page getPage ()
    {
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof Page) {
                return (Page) c;
            }
        }

        return null;
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the global scale of this page (and sheet)
     *
     * @return the global page scale
     */
    public Scale getScale ()
    {
        return getPage().getScale();
    }
}
