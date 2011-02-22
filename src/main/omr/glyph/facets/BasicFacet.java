//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c F a c e t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.util.Navigable;

/**
 * Class {@code BasicFacet} is the root for implementation on any glyph facet
 *
 * @author Hervé Bitteur
 */
class BasicFacet
    implements GlyphFacet
{
    //~ Instance fields --------------------------------------------------------

    /** Our glyph */
    @Navigable(false)
    protected final Glyph glyph;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BasicFacet //
    //------------//
    /**
     * Create a new BasicFacet object
     *
     * @param glyph the glyph this facet describes
     */
    public BasicFacet (Glyph glyph)
    {
        this.glyph = glyph;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // dump //
    //------//
    public void dump ()
    {
        // void by default
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    public void invalidateCache ()
    {
        // void by default
    }
}
