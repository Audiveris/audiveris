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

import omr.glyph.Scene;

import omr.util.Vip;

/**
 * Interface {@code GlyphAdministration} defines the administration facet of a
 * glyph, handling the glyph id and its related containing scene.
 *
 * @author Hervé Bitteur
 */
interface GlyphAdministration
    extends GlyphFacet, Vip
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Assign a unique ID to the glyph
     * @param id the unique id
     */
    void setId (int id);

    /**
     * Report the unique glyph id within its containing scene
     * @return the glyph id
     */
    int getId ();

    /**
     * Report the containing scene
     * @return the containing scene
     */
    Scene getScene ();

    /**
     * The setter for glyph scene. To be used with care
     * @param scene the containing scene
     */
    void setScene (Scene scene);

    /**
     * Test whether the glyph is transient (not yet inserted into the scene)
     * @return true if transient
     */
    boolean isTransient ();

    /**
     * Report whether this glyph is virtual (rather than real)
     * @return true if virtual
     */
    boolean isVirtual ();
}
