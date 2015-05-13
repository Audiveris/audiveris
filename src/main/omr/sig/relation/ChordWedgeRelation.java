//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C h o r d W e d g e R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.util.HorizontalSide;

/**
 * Class {@code ChordWedgeRelation} represents a support relation between a chord and
 * a wedge nearby.
 *
 * @author Hervé Bitteur
 */
public class ChordWedgeRelation
        extends AbstractSupport
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left or right side of the wedge. */
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
