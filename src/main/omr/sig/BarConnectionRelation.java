//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            B a r C o n n e c t i o n R e l a t i o n                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code BarConnectionRelation} records the relation between two bar lines
 * connected across staves, so that they can support each other.
 *
 * @author Hervé Bitteur
 */
public class BarConnectionRelation
        extends BasicSupport
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BarConnectionRelation object.
     *
     * @param impacts quality of relation
     */
    public BarConnectionRelation (GradeImpacts impacts)
    {
        super(impacts.getGrade());
        setImpacts(impacts);
    }
}
