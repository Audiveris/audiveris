//----------------------------------------------------------------------------//
//                                                                            //
//                              S l u r T a s k                               //
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code SlurTask} attempts to fix a slur glyph (by extracting
 * a new glyph out of some sections of the old glyph).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SlurTask
        extends GlyphUpdateTask
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SlurTask object.
     *
     * @param sheet  the sheet impacted
     * @param glyphs the collection of glyphs to process
     */
    public SlurTask (Sheet sheet,
                     Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
    }

    //----------//
    // SlurTask //
    //----------//
    /** No-arg constructor for JAXB only */
    private SlurTask ()
    {
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
                .trimSlurs(getInitialGlyphs());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" slur");

        return sb.toString();
    }
}
