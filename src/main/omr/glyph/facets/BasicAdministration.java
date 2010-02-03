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

import omr.glyph.GlyphLag;

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

    /** The containing glyph lag */
    protected GlyphLag lag;

    /** Glyph instance identifier (Unique in the containing GlyphLag) */
    protected int id;

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // BasicAdministration //
    //---------------------//
    /**
     * Create a new BasicAdministration object
     *
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

    //--------//
    // setLag //
    //--------//
    public void setLag (GlyphLag lag)
    {
        this.lag = lag;
    }

    //--------//
    // getLag //
    //--------//
    public GlyphLag getLag ()
    {
        return lag;
    }

    //-----------//
    // getPrefix //
    //-----------//
    public String getPrefix ()
    {
        return "Glyph";
    }

    //-------------//
    // isTransient //
    //-------------//
    public boolean isTransient ()
    {
        return id == 0;
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
        System.out.println("   lag=" + getLag());
    }
}
