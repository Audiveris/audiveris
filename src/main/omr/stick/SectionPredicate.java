//----------------------------------------------------------------------------//
//                                                                            //
//                      S e c t i o n P r e d i c a t e                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.stick;

import omr.check.SuccessResult;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;

import omr.util.Implement;
import omr.util.Predicate;

import java.util.Map;

/**
 * Class <code>SectionPredicate</code> is a basic predicate on sections used to
 * build sticks.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SectionPredicate
    implements Predicate<GlyphSection>
{
    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SectionPredicate //
    //------------------//
    /** Creates a new instance of SectionPredicate */
    public SectionPredicate ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @Implement(Predicate.class)
    public boolean check (GlyphSection section)
    {
        boolean result;

        // Check whether this section is not already assigned to a
        // recognized glyph
        if (section instanceof StickSection) {
            StickSection  ss = (StickSection) section;
            Glyph         glyph = ss.getGlyph();
            StickRelation relation = ss.getRelation();
            result = (glyph == null) ||
                     ((relation == null) || (relation.role == null)) ||
                     !(glyph.getResult() instanceof SuccessResult);
        } else {
            Glyph glyph = section.getGlyph();
            result = (glyph == null) ||
                     !(glyph.getResult() instanceof SuccessResult);
        }

        //System.out.println(result + " " + section);
        return result;
    }
}
