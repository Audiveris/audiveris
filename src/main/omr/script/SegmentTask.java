//----------------------------------------------------------------------------//
//                                                                            //
//                           S e g m e n t T a s k                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import java.util.Collection;

/**
 * Class {@code SegmentTask} attempts to segment a collection of glyphs (along
 * verticals for the time being)
 *
 * @author Hervé Bitteur
 */
public class SegmentTask
    extends GlyphUpdateTask
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
             .segmentGlyphs(getInitialGlyphs(), isShort);
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
