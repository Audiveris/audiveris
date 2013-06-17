//----------------------------------------------------------------------------//
//                                                                            //
//                           S e g m e n t T a s k                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import java.util.Collection;

/**
 * Class {@code SegmentTask} attempts to segment a collection of
 * glyphs (along verticals for the time being).
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
     * @param sheet   the sheet impacted
     * @param isShort true if we are looking for short verticals
     * @param glyphs  the collection of glyphs to look up
     */
    public SegmentTask (Sheet sheet,
                        boolean isShort,
                        Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.isShort = isShort;
    }

    //-------------//
    // SegmentTask //
    //-------------//
    /** No-arg constructor for JAXB only */
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
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" segment");

        if (isShort) {
            sb.append(" ")
                    .append("short");
        }

        return sb.toString();
    }
}
