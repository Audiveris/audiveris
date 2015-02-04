//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S u p p o r t I m p a c t s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.sig.BasicImpacts;

/**
 * Class {@code SupportImpacts} handles impacts for a supporting relation.
 *
 * @author Hervé Bitteur
 */
public class SupportImpacts
        extends BasicImpacts
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RelationImpacts object.
     *
     * @param names   array of names
     * @param weights array of weights
     */
    public SupportImpacts (String[] names,
                           double[] weights)
    {
        super(names, weights);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // getIntrinsicRatio //
    //-------------------//
    /**
     * A relation is not supposed to have a contextual grade, so there is no point to
     * leave room for it.
     *
     * @return 1
     */
    @Override
    public double getIntrinsicRatio ()
    {
        return 1;
    }
}
