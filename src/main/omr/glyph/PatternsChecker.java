//----------------------------------------------------------------------------//
//                                                                            //
//                       P a t t e r n s C h e c k e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.log.Logger;

import omr.sheet.SystemInfo;

/**
 * Class <code>PatternsChecker</code> gathers a series of specific patterns to
 * process (verify, recognize, fix, ...) glyphs in their environment.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class PatternsChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        PatternsChecker.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // PatternsChecker //
    //-----------------//
    /**
     * Creates a new PatternsChecker object.
     *
     * @param system the dedicated system
     */
    public PatternsChecker (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // runPatterns //
    //-------------//
    /**
     * Run the whole series of pattern on the glyphs of the dedicated system
     *
     * @return true if some progress has been made
     */
    public boolean runPatterns ()
    {
        int clefModifs = 0;
        int alterModifs = 0;
        int stemModifs = 0;
        int slurModifs = 0;
        int textModifs = 0;

        // Clefs
        system.removeInactiveGlyphs();
        clefModifs = system.runClefPattern();

        // Close Stems (sharps & naturals)
        system.removeInactiveGlyphs();
        alterModifs = system.runAlterPattern();

        // Stems
        system.removeInactiveGlyphs();
        stemModifs = system.runStemPattern();

        // Slurs
        system.removeInactiveGlyphs();
        system.retrieveGlyphs();
        slurModifs = system.runSlurPattern();

        // Texts
        system.removeInactiveGlyphs();
        system.retrieveGlyphs();
        textModifs = system.runTextPattern();

        if (logger.isFineEnabled()) {
            logger.fine(
                "System#" + system.getId() + " clef:" + clefModifs + " alter:" +
                alterModifs + " stems:" + stemModifs + " slurs:" + slurModifs +
                " texts:" + textModifs);
        }

        // Progress made?
        return (clefModifs + alterModifs + stemModifs + slurModifs +
               textModifs) > 0;
    }
}
