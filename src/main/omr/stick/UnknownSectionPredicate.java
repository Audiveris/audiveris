//-----------------------------------------------------------------------//
//                                                                       //
//             U n k n o w n S e c t i o n P r e d i c a t e             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//
package omr.stick;

import omr.glyph.GlyphSection;

import omr.util.Implement;
import omr.util.Predicate;

/**
 * Class <code>UnknownSectionPredicate</code> is a basic predicate on
 * sections used to build sticks, for which we just check if not section
 * isKnown().
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class UnknownSectionPredicate
    implements Predicate<GlyphSection>
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
    public boolean check (GlyphSection section)
    {
        // Check if this section is not already assigned to a recognized glyph
        return !section.isKnown();
    }
}
