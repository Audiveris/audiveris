//----------------------------------------------------------------------------//
//                                                                            //
//                            P l u g i n T y p e                             //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.plugin;

import java.util.*;

/**
 * Used in the Plugin annotation to designate in which section of the GUI the
 * plugin should be shown.
 *
 * @author Brenton Partridge
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum PluginType {
    /** No specific section ??? */
    DEFAULT,
    // Score
    SCORE_IMPORT, SCORE_EDIT, SCORE_EXPORT,

    // Sheet & Script
    SHEET_IMPORT,SHEET_SCRIPT, SHEET_EDIT,
    SHEET_EXPORT,
    SHEET_END,
    
    // Midi
    MIDI_EXPORT,

    // View
    SCORE_VIEW,GLYPH_VIEW, LINE_VIEW,
    LOG_VIEW,

    // Tools
    TRAINING,TOOL, TEST,

    // Help
    HELP;
    //
    //--------------------------------------------------------------------------
    /** Range of types */
    public static class Range
    {
        // Name of this range
        private final String              name;

        // Contained types
        private final EnumSet<PluginType> types;

        //-------//
        // Range //
        //-------//
        /**
         * Create a Range from an EnumSet of types
         *
         * @param name a name for this range (menu)
         * @param types the contained types
         */
        public Range (String              name,
                      EnumSet<PluginType> types)
        {
            this.name = name;
            this.types = types;
            rangeSet.add(this);
        }

        //----------//
        // getTypes //
        //----------//
        /**
         * Exports the set of types in the range
         *
         * @return the proper enum set
         */
        public EnumSet<PluginType> getTypes ()
        {
            return types;
        }

        //---------//
        // getName //
        //---------//
        /**
         * Report the (menu) name for this range
         *
         * @return the range name
         */
        public String getName ()
        {
            return name;
        }
    }

    // Set for all defined ranges
    private static final Set<Range> rangeSet = new LinkedHashSet<Range>();

    // Sheet
    public static final Range SheetTypes = new Range(
        "File",
        EnumSet.range(SHEET_IMPORT, SHEET_END));

    // Step
    public static final Range StepTypes = new Range(
        "Step",
        EnumSet.noneOf(PluginType.class));

    // Score
    public static final Range ScoreTypes = new Range(
        "Score",
        EnumSet.range(SCORE_IMPORT, SCORE_EXPORT));

    // Midi
    public static final Range MidiTypes = new Range(
        "Midi",
        EnumSet.range(MIDI_EXPORT, MIDI_EXPORT));

    // Views
    public static final Range ViewTypes = new Range(
        "Views",
        EnumSet.range(SCORE_VIEW, LOG_VIEW));

    // Tools
    public static final Range ToolTypes = new Range(
        "Tools",
        EnumSet.range(TRAINING, TEST));

    // Help
    public static final Range HelpTypes = new Range("Help", EnumSet.of(HELP));

    //-----------//
    // getRanges //
    //-----------//
    public static Collection<Range> getRanges ()
    {
        return rangeSet;
    }
}
