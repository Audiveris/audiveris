//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                E n d i n g B a r R e l a t i o n                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.util.HorizontalSide;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code EndingBarRelation} connects an ending side with a bar line.
 *
 * @author Hervé Bitteur
 */
public class EndingBarRelation
        extends AbstractRelation
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Which side of ending is used?. */
    @XmlAttribute(name = "side")
    private final HorizontalSide endingSide;

    /** Horizontal delta (in interline) between bar line and ending side. */
    private final double xDistance;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new EndingBarRelation object.
     *
     * @param endingSide which side of ending
     * @param xDistance  horizontal delta
     */
    public EndingBarRelation (HorizontalSide endingSide,
                              double xDistance)
    {
        this.endingSide = endingSide;
        this.xDistance = xDistance;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private EndingBarRelation ()
    {
        this.endingSide = null;
        this.xDistance = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the endingSide
     */
    public HorizontalSide getEndingSide ()
    {
        return endingSide;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(endingSide).append("@(").append(String.format("%.2f", xDistance)).append(")");

        return sb.toString();
    }
}
