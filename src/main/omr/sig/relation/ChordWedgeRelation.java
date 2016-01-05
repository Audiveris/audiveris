//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C h o r d W e d g e R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import omr.util.HorizontalSide;

/**
 * Class {@code ChordWedgeRelation} represents a support relation between a chord and
 * a wedge nearby.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ChordWedgeRelation
        extends AbstractSupport
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left or right side of the wedge. */
    @XmlAttribute
    private final HorizontalSide side;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordWedgeRelation} object.
     *
     * @param side which side of the wedge
     */
    public ChordWedgeRelation (HorizontalSide side)
    {
        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ChordWedgeRelation ()
    {
        this.side = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getSide //
    //---------//
    /**
     * @return the side
     */
    public HorizontalSide getSide ()
    {
        return side;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return super.toString() + "/" + side;
    }
}
