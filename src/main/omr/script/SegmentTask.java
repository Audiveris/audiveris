//----------------------------------------------------------------------------//
//                                                                            //
//                           S e g m e n t T a s k                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Glyph;

import omr.sheet.Sheet;

import java.util.Collection;

/**
 * Class <code>SegmentTask</code> is a script task which attempts to segment a
 * collection of glyphs (along verticals for the time being)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SegmentTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Are we looking for short verticals? */
    private final boolean isShort;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SegmentTask object.
     *
     * @param isShort true if we are looking for short verticals
     * @param glyphs the collection of glyphs to look up
     */
    public SegmentTask (boolean           isShort,
                        Collection<Glyph> glyphs)
    {
        super(glyphs);
        this.isShort = isShort;
    }

    //-------------//
    // SegmentTask //
    //-------------//
    /** No-arg constructor needed by JAXB */
    private SegmentTask ()
    {
        isShort = false; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        sheet.getSymbolsController()
             .getModel()
             .segmentGlyphSet(getInitialGlyphs(), isShort);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" segment");

        if (isShort) {
            sb.append(" ")
              .append("short");
        }

        return sb + super.internalsString();
    }
}
