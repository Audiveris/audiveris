//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             B a s i c A d m i n i s t r a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;

/**
 * Class {@code BasicAdministration} is a basic implementation of glyph
 * administration facet.
 *
 * @author Hervé Bitteur
 */
class BasicAdministration
        extends BasicFacet
        implements GlyphAdministration
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The precise layer. */
    protected final GlyphLayer layer;

    /** The containing glyph nest. */
    protected GlyphNest nest;

    /** Glyph instance identifier. (Unique in the containing nest) */
    protected int id;

    /** Flag to remember processing has been done. */
    private boolean processed = false;

    /** VIP flag. */
    protected boolean vip;

    /** Related id string. (pre-built once for all) */
    protected String idString = "glyph#" + 0;

    //~ Constructors -------------------------------------------------------------------------------
    //---------------------//
    // BasicAdministration //
    //---------------------//
    /**
     * Create a new BasicAdministration object
     *
     * @param glyph our glyph
     */
    public BasicAdministration (Glyph glyph,
                                GlyphLayer layer)
    {
        super(glyph);
        this.layer = layer;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // idString //
    //----------//
    @Override
    public final String idString ()
    {
        return idString;
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(
                String.format(
                        "Glyph: %s@%s%n",
                        glyph.getClass().getName(),
                        Integer.toHexString(glyph.hashCode())));
        sb.append(String.format("   id=%d%n", getId()));

        if (isVip()) {
            sb.append(String.format("   vip%n", getId()));
        }

        sb.append(String.format("   layer=%s%n", getLayer()));
        sb.append(String.format("   nest=%s%n", getNest()));

        return sb.toString();
    }

    //-------//
    // getId //
    //-------//
    @Override
    public int getId ()
    {
        return id;
    }

    //-------//
    // getId //
    //-------//
    @Override
    public GlyphLayer getLayer ()
    {
        return layer;
    }

    //---------//
    // getNest //
    //---------//
    @Override
    public GlyphNest getNest ()
    {
        return nest;
    }

    //-------------//
    // isProcessed //
    //-------------//
    @Override
    public boolean isProcessed ()
    {
        return processed;
    }

    //-------------//
    // isTransient //
    //-------------//
    @Override
    public boolean isTransient ()
    {
        return nest == null;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //-----------//
    // isVirtual //
    //-----------//
    @Override
    public boolean isVirtual ()
    {
        return false;
    }

    //-------//
    // setId //
    //-------//
    @Override
    public void setId (int id)
    {
        this.id = id;
        idString = "glyph#" + id;
    }

    //---------//
    // setNest //
    //---------//
    @Override
    public void setNest (GlyphNest nest)
    {
        this.nest = nest;
    }

    //--------------//
    // setProcessed //
    //--------------//
    @Override
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }
}
