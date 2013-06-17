//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

/**
 * Class {@code GlyphPattern} describes a specific pattern applied on
 * glyphs of a given system.
 *
 * @author Hervé Bitteur
 */
public abstract class GlyphPattern
{
    //~ Instance fields --------------------------------------------------------

    /** Name for debugging */
    public final String name;

    /** Related system */
    protected final SystemInfo system;

    /** System scale */
    protected final Scale scale;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // GlyphPattern //
    //--------------//
    /**
     * Creates a new GlyphPattern object.
     *
     * @param name   the unique name for this pattern
     * @param system the related system
     */
    public GlyphPattern (String name,
                         SystemInfo system)
    {
        this.name = name;
        this.system = system;

        scale = system.getSheet()
                .getScale();
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    /**
     * This method runs the pattern and reports the number of modified
     * glyphs.
     *
     * @return the number of modified glyphs
     */
    public abstract int runPattern ();

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "S" + system.getId() + " pattern:" + name;
    }
}
