//----------------------------------------------------------------------------//
//                                                                            //
//                          D e a s s i g n T a s k                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.Glyph;
import static omr.script.ScriptRecording.*;

import omr.sheet.Sheet;

import omr.step.StepException;
import static omr.util.Synchronicity.*;

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
        throws StepException
    {
        super.run(sheet);
        sheet.getSymbolsModel()
             .deassignSetShape(SYNC, glyphs, RECORDING);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " deassign" + super.internalsString();
    }
}
