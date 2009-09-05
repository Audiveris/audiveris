//----------------------------------------------------------------------------//
//                                                                            //
//                          D e a s s i g n T a s k                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.Glyph;

import omr.sheet.Sheet;

import java.util.Collection;

/**
 * Class <code>DeassignTask</code> is a script task which deassigns a collection
 * of glyphs.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class DeassignTask
    extends GlyphTask
{
    //~ Constructors -----------------------------------------------------------

    //--------------//
    // DeassignTask //
    //--------------//
    /**
     * Create a task to deassign glyphs
     *
     * @param glyphs the glyphs to deassign
     */
    public DeassignTask (Collection<Glyph> glyphs)
    {
        super(glyphs);
    }

    //--------------//
    // DeassignTask //
    //--------------//
    /**
     * No-arg constructor needed for JAXB
     */
    private DeassignTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    @Override
    public void run (Sheet sheet)
        throws Exception
    {
        sheet.getSymbolsController()
             .asyncDeassignGlyphs(glyphs)
             .get();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " deassign " + super.internalsString();
    }
}
