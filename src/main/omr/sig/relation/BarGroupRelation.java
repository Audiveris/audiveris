//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B a r G r o u p R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarGroupRelation} groups 2 bar lines.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bar-group")
public class BarGroupRelation
        extends AbstractRelation
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Horizontal white gap (in interline) between the two bar lines. */
    private final double xGap;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarGroupRelation object.
     *
     * @param xGap white gap between the two grouped bar lines
     */
    public BarGroupRelation (double xGap)
    {
        this.xGap = xGap;
    }

    // For JAXB
    private BarGroupRelation ()
    {
        this.xGap = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append("@(").append(String.format("%.2f", xGap)).append(")");

        return sb.toString();
    }
}
