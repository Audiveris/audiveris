//----------------------------------------------------------------------------//
//                                                                            //
//                       P a t t e r n s C h e c k e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Grades;
import omr.glyph.text.SentencePattern;

import omr.log.Logger;

import omr.sheet.SystemInfo;

/**
 * Class {@code PatternsChecker} gathers for a gien system a series of
 * specific patterns to process (verify, recognize, fix, ...) glyphs in their
 * sheet environment.
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

    /** Sequence of patterns to runPattern */
    private final GlyphPattern[] patterns;

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
        patterns = new GlyphPattern[] {
                       
        //
        new BeamHookPattern(system),
                       
        new FortePattern(system),
                       
        new HiddenSlurPattern(system),
                       
        new SplitPattern(system),
                       
        new LedgerPattern(system),
                       
        new AlterPattern(system),
                       
        system.getSlurInspector(),
                       
        new BassPattern(system),
                       
        new ClefPattern(system),
                       
        new TimePattern(system), 
        //
        // Text patterns
        new TextBorderPattern(system), // Glyphs -> Text
        new TextGreedyPattern(system), // Glyphs -> Text
        new TextAreaPattern(system), //   Glyphs -> Text
        new SentencePattern(system), // Text -> sentences
                                     //
                                     ///new ArticulationPattern(system),
        new LeftOverPattern(system)
                   };
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

        for (GlyphPattern pattern : patterns) {
            if (logger.isFineEnabled()) {
                logger.finest("Starting " + pattern);
            }

            system.inspectGlyphs(Grades.leafMinGrade);

            try {
                int modifs = pattern.runPattern();

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

        system.inspectGlyphs(Grades.leafMinGrade);

        if ((totalModifs > 0) && logger.isFineEnabled()) {
            logger.fine("S#" + system.getId() + " Patterns" + sb);
        }

        return totalModifs != 0;
    }
}
