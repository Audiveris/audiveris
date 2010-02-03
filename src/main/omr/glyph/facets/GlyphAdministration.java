//----------------------------------------------------------------------------//
//                                                                            //
//                   G l y p h A d m i n i s t r a t i o n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphLag;

/**
 * Interface {@code GlyphAdministration} defines the administration facet of a
 * glyph, handling the glyph id and its related containing lag.
 *
 * @author Hervé Bitteur
 */
interface GlyphAdministration
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //-------//
    // setId //
    //-------//
    /**
     * Assign a unique ID to the glyph
     *
     * @param id the unique id
     */
    void setId (int id);

    //-------//
    // getId //
    //-------//
    /**
     * Report the unique glyph id
     *
     * @return the glyph id
     */
    int getId ();

    //--------//
    // setLag //
    //--------//
    /**
     * The setter for glyph lag. Used with care, by {@link GlyphLag} and
     * {@link omr.glyph.ui.GlyphVerifier}
     *
     * @param lag the containing lag
     */
    void setLag (GlyphLag lag);

    //--------//
    // getLag //
    //--------//
    /**
     * Report the containing lag
     *
     * @return the containing lag
     */
    GlyphLag getLag ();

    //-------------//
    // isTransient //
    //-------------//
    /**
     * Test whether the glyph is transient (not yet inserted into lag)
     * @return true if transient
     */
    boolean isTransient ();
}
