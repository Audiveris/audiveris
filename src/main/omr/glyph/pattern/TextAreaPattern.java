//----------------------------------------------------------------------------//
//                                                                            //
//                       T e x t A r e a P a t t e r n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.text.TextArea;

import omr.lag.BasicRoi;

import omr.log.Logger;

import omr.run.Orientation;

import omr.sheet.SystemInfo;

/**
 * Class {@code TextAreaPattern} subdivides a system into sub-areas to
 * find out TEXT glyphs, as detected by the neural network evaluation.
 *
 * @author Hervé Bitteur
 */
public class TextAreaPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TextAreaPattern.class);

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // TextAreaPattern //
    //-----------------//
    /**
     * Creates a new TextAreaPattern object.
     *
     * @param system The dedicated system
     */
    public TextAreaPattern (SystemInfo system)
    {
        super("TextArea", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Use system area subdivision, to try to retrieve additional series of
     * glyphs that could represent text portions in the system at hand
     * @return the number of text glyphs built
     */
    @Override
    public int runPattern ()
    {
        // Create a TextArea on the whole system
        TextArea area = new TextArea(
            system,
            null,
            new BasicRoi(system.getBounds()),
            Orientation.HORIZONTAL);

        // Find and build additional text glyphs (words most likely)
        area.subdivide();

        return 0; // TODO: improve this!
    }
}
