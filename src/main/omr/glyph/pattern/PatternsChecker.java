//----------------------------------------------------------------------------//
//                                                                            //
//                       P a t t e r n s C h e c k e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Grades;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.text.TextCheckerPattern;
import omr.text.TextPattern;

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
    private static final Logger logger = Logger.getLogger(
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
            new CaesuraPattern(system),
            new BeamHookPattern(system),
            // Refresh ...
            new RefreshPattern(system),
            new DoubleBeamPattern(system),
            new FermataDotPattern(system),
            new FlagPattern(system),
            new FortePattern(system),
            new HiddenSlurPattern(system),
            new SplitPattern(system),
            new LedgerPattern(system),
            new AlterPattern(system),
            system.getSlurInspector(),
            new BassPattern(system),
            new ClefPattern(system),
            new TimePattern(system),
            // Refresh ...
            new RefreshPattern(system),
            //
            // Text patterns
            //            new TextBorderPattern(system), // Glyphs -> Text
            //            new TextGreedyPattern(system), // Glyphs -> Text
            //            new TextAreaPattern(system), //   Glyphs -> Text
            //            new SentencePattern(system), // Text -> sentences
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

        system.inspectGlyphs(Grades.symbolMinGrade);

        for (GlyphPattern pattern : patterns) {
            logger.fine("Starting {0}", pattern);

            system.removeInactiveGlyphs();

            try {
                int modifs = pattern.runPattern();

                if (logger.isFineEnabled()) {
                    sb.append(" ").append(pattern.name).append(":").append(
                            modifs);
                }

                totalModifs += modifs;
            } catch (Throwable ex) {
                logger.warning(system.getLogPrefix() + 
                               " error running pattern " + pattern.name, ex);
            }
        }

        system.inspectGlyphs(Grades.symbolMinGrade);

        if (totalModifs > 0) {
            logger.fine("S#{0} Patterns{1}", system.getId(), sb);
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
        //~ Constructors -------------------------------------------------------

        public RefreshPattern (SystemInfo system)
        {
            super("Refresh", system);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int runPattern ()
        {
            system.inspectGlyphs(Grades.symbolMinGrade);

            return 0;
        }
    }
}
