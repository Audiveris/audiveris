//----------------------------------------------------------------------------//
//                                                                            //
//                       P a t t e r n s C h e c k e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Grades;

import omr.sheet.SystemInfo;

import omr.text.TextPattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code PatternsChecker} gathers for a given system a series of
 * specific patterns to process (verify, recognize, fix, ...) glyphs
 * in their sheet environment.
 *
 * @author Hervé Bitteur
 */
public class PatternsChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PatternsChecker.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Sequence of patterns to run. */
    private final GlyphPattern[] patterns;

    /** Dedicated system. */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------
    //
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

        patterns = new GlyphPattern[]{
            //
            new CaesuraPattern(system), new BeamHookPattern(system),
            new DotPattern(system),
            // Refresh ...
            new RefreshPattern(system, false), new DoubleBeamPattern(system),
            new FermataDotPattern(system), new FlagPattern(system),
            new FortePattern(system), new HiddenSlurPattern(system),
            new SplitPattern(system), new LedgerPattern(system),
            new AlterPattern(system), new StemPattern(system),
            system.getSlurInspector(), new BassPattern(system),
            new ClefPattern(system), new TimePattern(system),
            // Refresh ...
            new RefreshPattern(system, true),
            //
            new TextPattern(system),
            ///new TextCheckerPattern(system), // Debug stuff

            ///new ArticulationPattern(system),

            ///new SegmentationPattern(system),
            new LeftOverPattern(system)
        };
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------//
    // runPatterns //
    //-------------//
    /**
     * Run the sequence of pattern on the dedicated system
     *
     * @return the number of modifications made
     */
    public boolean runPatterns ()
    {
        int totalModifs = 0;
        StringBuilder sb = new StringBuilder();

        system.inspectGlyphs(Grades.symbolMinGrade, false);

        //        final Step symbolsStep = Steps.valueOf(Steps.SYMBOLS);
        //
        //        // Continuing, update UI
        //        SwingUtilities.invokeLater(
        //                new Runnable()
        //                {
        //                    @Override
        //                    public void run ()
        //                    {
        //                        symbolsStep.displayUI(system.getSheet());
        //                    }
        //                });
        for (GlyphPattern pattern : patterns) {
            logger.debug("Starting {}", pattern);

            system.removeInactiveGlyphs();

            try {
                int modifs = pattern.runPattern();

                if (logger.isDebugEnabled()) {
                    sb.append(" ")
                            .append(pattern.name)
                            .append(":")
                            .append(modifs);
                }

                totalModifs += modifs;
            } catch (Throwable ex) {
                logger.warn(
                        system.getLogPrefix() + " error running pattern "
                        + pattern.name,
                        ex);
            }
        }

        system.inspectGlyphs(Grades.symbolMinGrade, false);

        if (totalModifs > 0) {
            logger.debug("S#{} Patterns{}", system.getId(), sb);
        }

        return totalModifs != 0;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //----------------//
    // RefreshPattern //
    //----------------//
    /**
     * Dummy pattern, just to refresh the system glyphs.
     */
    private static class RefreshPattern
            extends GlyphPattern
    {
        //~ Instance fields ----------------------------------------------------

        private final boolean wide;

        //~ Constructors -------------------------------------------------------
        public RefreshPattern (SystemInfo system,
                               boolean wide)
        {
            super("Refresh", system);
            this.wide = wide;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int runPattern ()
        {
            system.inspectGlyphs(Grades.symbolMinGrade, wide);

            return 0;
        }
    }
}
