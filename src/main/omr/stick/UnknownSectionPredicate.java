//----------------------------------------------------------------------------//
//                                                                            //
//               U n k n o w n S e c t i o n P r e d i c a t e                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.lag.Section;

import omr.util.Implement;
import omr.util.Predicate;

/**
 * Class <code>UnknownSectionPredicate</code> is a basic predicate on
 * sections used to build sticks, for which we just check if not section
 * isKnown().
 *
 * @author Hervé Bitteur
 */
public class UnknownSectionPredicate
    implements Predicate<Section>
{
    //~ Constructors -----------------------------------------------------------

    //-------------------------//
    // UnknownSectionPredicate //
    //-------------------------//
    /**
     * Creates a new instance of UnknownSectionPredicate
     */
    public UnknownSectionPredicate ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @Implement(Predicate.class)
    public boolean check (Section section)
    {
        // Check if this section is not already assigned to a recognized glyph
        return !section.isKnown();
    }
}
