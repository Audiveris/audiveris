//----------------------------------------------------------------------------//
//                                                                            //
//                       P a t t e r n s C h e c k e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.log.Logger;

import omr.sheet.SystemInfo;

/**
 * Class <code>PatternsChecker</code> gathers a series of specific patterns
 * to process (verify, recognize, fix, ...) glyphs in their sheet environment.
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

    /** Sequence of patterns to run */
    private static Pattern[] patterns = new Pattern[] {
                                            
    //
    new Pattern("Clef") {
            public int run (SystemInfo system)
            {
                return system.runClefPattern();
            }
        }
    ,
                                            
    new Pattern("Alter") {
            public int run (SystemInfo system)
            {
                return system.runAlterPattern();
            }
        }
    ,
                                            
    new Pattern("Stem") {
            public int run (SystemInfo system)
            {
                return system.runStemPattern();
            }
        }
    ,
                                            
    new Pattern("Slur") {
            public int run (SystemInfo system)
            {
                return system.runSlurPattern();
            }
        }
    ,
                                            
    new Pattern("Text") {
            public int run (SystemInfo system)
            {
                return system.runTextPattern();
            }
        }
                                        };

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
    public PatternsChecker (final SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // runPatterns //
    //-------------//
    /**
     * Run the sequence of pattern on the dedicated system
     * @return the number of modifications made
     */
    public boolean runPatterns ()
    {
        int           totalModifs = 0;
        StringBuilder sb = new StringBuilder();

        for (Pattern pattern : patterns) {
            try {
                system.inspectGlyphs(GlyphInspector.getLeafMaxDoubt());

                int modifs = pattern.run(system);

                if (logger.isFineEnabled()) {
                    sb.append(" ")
                      .append(pattern.name)
                      .append(":")
                      .append(modifs);
                }

                totalModifs += modifs;
            } catch (Throwable ex) {
                logger.warning(
                    "System #" + system.getId() + " error running pattern " +
                    pattern.name,
                    ex);
            }
        }

        if ((totalModifs > 0) && logger.isFineEnabled()) {
            logger.fine("S#" + system.getId() + " SheetPatterns" + sb);
        }

        return totalModifs != 0;
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Pattern //
    //---------//
    private abstract static class Pattern
    {
        //~ Instance fields ----------------------------------------------------

        public final String name;

        //~ Constructors -------------------------------------------------------

        public Pattern (String name)
        {
            this.name = name;
        }

        //~ Methods ------------------------------------------------------------

        public abstract int run (SystemInfo system);

        @Override
        public String toString ()
        {
            return "pattern:" + name;
        }
    }
}
