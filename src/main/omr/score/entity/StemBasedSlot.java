//----------------------------------------------------------------------------//
//                                                                            //
//                         S t e m B a s e d S l o t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.common.PixelPoint;

/**
 * Class {@code StemBasedSlot} is a slot whose position is based on the
 * abscissa of the contained chord stems.
 *
 * @author Hervé Bitteur
 */
public class StemBasedSlot
        extends Slot
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // StemBasedSlot //
    //---------------//
    /**
     * Create a new StemBasedSlot object.
     *
     * @param measure  the containing measure
     * @param refPoint the slot reference point (the stem location)
     */
    public StemBasedSlot (Measure measure,
                          PixelPoint refPoint)
    {
        super(measure);

        this.refPoint = new PixelPoint(refPoint);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------//
    // getX //
    //------//
    @Override
    public int getX ()
    {
        return refPoint.x;
    }
}
