//----------------------------------------------------------------------------//
//                                                                            //
//                   B a s i c A d m i n i s t r a t i o n                    //
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

/**
 * Class {@code BasicAdministration} is a basic implementation of glyph
 * administration facet
 *
 * @author Hervé Bitteur
 */
class BasicAdministration
    extends BasicFacet
    implements GlyphAdministration
{
    //~ Instance fields --------------------------------------------------------

    /** The containing glyph scene */
    protected Scene scene;

    /** Glyph instance identifier (Unique in the containing scene) */
    protected int id;

    /** VIP flag */
    protected boolean vip;

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // BasicAdministration //
    //---------------------//
    /**
     * Create a new BasicAdministration object
     * @param glyph our glyph
     */
    public BasicAdministration (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //----------//
    // setScene //
    //----------//
    public void setScene (Scene scene)
    {
        this.scene = scene;
    }

    //----------//
    // getScene //
    //----------//
    public Scene getScene ()
    {
        return scene;
    }

    //-------------//
    // isTransient //
    //-------------//
    public boolean isTransient ()
    {
        return id == 0;
    }

    //--------//
    // setVip //
    //--------//
    public void setVip ()
    {
        vip = true;
    }

    //-------//
    // isVip //
    //-------//
    public boolean isVip ()
    {
        return vip;
    }

    //-----------//
    // isVirtual //
    //-----------//
    public boolean isVirtual ()
    {
        return false;
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println(
            glyph.getClass().getName() + "@" +
            Integer.toHexString(glyph.hashCode()));
        System.out.println("   id=" + getId());
        System.out.println("   scene=" + getScene());
    }
}
