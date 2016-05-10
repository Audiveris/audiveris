//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e g m e n t T a s k                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Glyph;

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
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SegmentTask object.
     *
     * @param sheet  the sheet impacted
     * @param glyphs the collection of glyphs to look up
     */
    public SegmentTask (Sheet sheet,
                        Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
    }

    /** No-arg constructor for JAXB only. */
    private SegmentTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().getModel().segmentGlyphs(getInitialGlyphs());
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" segment");

        return sb.toString();
    }
}
