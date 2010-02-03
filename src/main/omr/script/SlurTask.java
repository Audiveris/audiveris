//----------------------------------------------------------------------------//
//                                                                            //
//                              S l u r T a s k                               //
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class <code>SlurTask</code> is a script task which attempts to fix a slur
 * glyph (by extracting a new glyph out of some sections of the old glyph)
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SlurTask
    extends GlyphTask
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SlurTask object.
     *
     * @param glyphs the collection of glyphs to process
     */
    public SlurTask (Collection<Glyph> glyphs)
    {
        super(glyphs);
    }

    //----------//
    // SlurTask //
    //----------//
    /** No-arg constructor needed by JAXB */
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
             .fixLargeSlurs(getInitialGlyphs());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" slur");

        return sb + super.internalsString();
    }
}
