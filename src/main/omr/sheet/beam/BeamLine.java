//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B e a m L i n e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.util.Vip;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code BeamLine} represents a sequence of aligned BeamItem instances.
 * There may be one or several BeamLine instances in a BeamStructure, one for each index.
 *
 * @author Hervé Bitteur
 */
public class BeamLine
        implements Vip
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Items that compose the line, from left to right. */
    private final List<BeamItem> items = new ArrayList<BeamItem>();

    /** The median line from left item to right item. */
    final Line2D median;

    /** The constant height of the line. */
    final double height;

    /** VIP flag. */
    private boolean vip;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamLine object.
     *
     * @param median the global line
     * @param height constant height for the line
     */
    public BeamLine (Line2D median,
                     double height)
    {
        this.median = median;
        this.height = height;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the items
     */
    public List<BeamItem> getItems ()
    {
        return items;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        for (BeamItem item : items) {
            sb.append(" ").append(item);
        }

        return sb.toString();
    }
}
