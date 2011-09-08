//----------------------------------------------------------------------------//
//                                                                            //
//                  G l y p h S e c t i o n s B u i l d e r                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.lag.JunctionPolicy;
import omr.lag.SectionsBuilder;

/**
 * Class {@code GlyphSectionsBuilder} is the SectionsBuilder meant for
 * populating a GlyphLag with GlyphSections instances.
 *
 * @author Hervé Bitteur
 */
public class GlyphSectionsBuilder
    extends SectionsBuilder<GlyphLag, GlyphSection>
{
    //~ Constructors -----------------------------------------------------------

    //----------------------//
    // GlyphSectionsBuilder //
    //----------------------//
    /**
     * Creates a new GlyphSectionsBuilder object.
     *
     * @param lag The GlyphLag to populate
     * @param junctionPolicy The policy to use to detect functions
     */
    public GlyphSectionsBuilder (GlyphLag       lag,
                                 JunctionPolicy junctionPolicy)
    {
        super(lag, junctionPolicy);
    }
}
