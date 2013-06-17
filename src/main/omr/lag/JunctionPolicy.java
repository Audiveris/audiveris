//----------------------------------------------------------------------------//
//                                                                            //
//                        J u n c t i o n P o l i c y                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Run;

/**
 * Interface {@code JunctionPolicy} encapsulates the policy that
 * decides if a run can extend a given section.
 * If not, the run is part of a new section, linked to the previous one by a
 * junction.
 *
 * @author Hervé Bitteur
 */
public interface JunctionPolicy
{
    //~ Methods ----------------------------------------------------------------

    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check if provided run is consistent with the section defined
     * so far.
     *
     * @param run     the candidate run for section extension
     * @param section the to-be extended section
     * @return true is extension is compatible with the defined junction policy
     */
    public abstract boolean consistentRun (Run run,
                                           Section section);
}
