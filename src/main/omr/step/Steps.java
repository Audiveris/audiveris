//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S t e p s                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.plugin.Plugin;
import omr.plugin.PluginManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Steps} handles the (ordered) set of all defined steps
 * <p>
 * <img src="doc-files/Activities.png" />
 *
 * @author Hervé Bitteur
 */
public class Steps
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Steps.class);

    // Mandatory step names
    // --------------------
    public static final String LOAD = "LOAD";

    public static final String BINARY = "BINARY";

    public static final String SCALE = "SCALE";

    public static final String GRID = "GRID";

    public static final String HEADERS = "HEADERS";

    public static final String STEM_SEEDS = "STEM_SEEDS";

    public static final String BEAMS = "BEAMS";

    public static final String LEDGERS = "LEDGERS";

    public static final String HEADS = "HEADS";

    public static final String STEMS = "STEMS";

    public static final String REDUCTION = "REDUCTION";

    public static final String CUE_BEAMS = "CUE_BEAMS";

    public static final String TEXTS = "TEXTS";

    public static final String CURVES = "CURVES";

    public static final String CHORDS = "CHORDS";

    public static final String SYMBOLS = "SYMBOLS";

    public static final String MEASURES = "MEASURES";

    public static final String RHYTHM = "RHYTHM";

    public static final String SYMBOL_REDUCTION = "SYMBOL_REDUCTION";

    public static final String PAGE = "PAGE";

    public static final String SCORE = "SCORE";

    // Optional step names
    // -------------------
    public static final String DELTA = "DELTA";

    public static final String PRINT = "PRINT";

    public static final String PAGE_EXPORT = "PAGE_EXPORT";

    public static final String EXPORT = "EXPORT";

    public static final String EXPORT_SHEET = "EXPORT_SHEET";

    public static final String PLUGIN = "PLUGIN";

    public static final String TEST = "TEST";

    /** Ordered sequence of steps. */
    private static final List<Step> steps = new ArrayList<Step>();

    /** Map (Name --> Step) of steps. */
    private static final Map<String, Step> stepMap = new HashMap<String, Step>();

    static {
        /** This is where the sequence of steps is actually defined. */

        // 1/ Mandatory steps in proper order
        // ----------------------------------
        addStep(new LoadStep()); // Sheet (gray) picture
        addStep(new BinaryStep()); // Binarized image
        addStep(new ScaleStep()); // Sheet scale (line thickness, interline, beam thickness)
        addStep(new GridStep()); // Staff lines, bar-lines, systems & parts
        addStep(new HeadersStep()); // Clef, Key & Time signature in system headers
        addStep(new StemSeedsStep()); // Vertical seeds for stems
        addStep(new BeamsStep()); // Beams detection
        addStep(new LedgersStep()); // Ledgers detection
        addStep(new HeadsStep()); // Note heads detection
        addStep(new StemsStep()); // Stems connected to heads and beams
        addStep(new ReductionStep()); // Reduction of conflicting interpretations
        addStep(new CueBeamsStep()); // Cue beams detection
        addStep(new TextsStep()); // OCR for textual items
        addStep(new CurvesStep()); // Slurs, wedges, endings
        addStep(new ChordsStep()); // Head-based chords (including mirror heads & stems)
        addStep(new SymbolsStep()); // Rests, dots, fermata, etc. Rest-based chords.
        addStep(new MeasuresStep()); // Raw measures from grouped bar-lines
        addStep(new RhythmStep()); // Tuplets, slots, time sigs inference, measure adjustments
        addStep(new SymbolReductionStep()); // Final interpretations filtering
        addStep(new PageStep()); // Connections between systems in page (parts, slurs, voices)
        addStep(new ScoreStep()); // Connections between pages in score (parts, slurs, voices)

        // 2/ Optional steps, in any order
        // -------------------------------
        addStep(new DeltaStep());
        addStep(new PrintStep());
        addStep(new ExportSheetStep());
        addStep(new ExportStep());
        addStep(new TestStep());

        // Plugin step depends on default plugin
        Plugin plugin = PluginManager.getInstance().getDefaultPlugin();

        if (plugin != null) {
            addStep(new PluginStep(plugin));
        }
    }

    /** Compare steps according to their position in the sequence of defined steps. */
    public static final Comparator<Step> comparator = new Comparator<Step>()
    {
        @Override
        public int compare (Step s1,
                            Step s2)
        {
            return Steps.compare(s1, s2);
        }
    };

    //--------------------------------------------------------------------------
    /** First step. */
    public static final Step FIRST_STEP = steps.get(0);

    /** Last step. */
    public static final Step LAST_STEP = valueOf(SCORE);

    //~ Constructors -------------------------------------------------------------------------------
    //-------//
    // Steps // Not meant to be instantiated
    //-------//
    private Steps ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // compare //
    //---------//
    /**
     * Compare two steps according to their position in steps sequence
     *
     * @param left  one step
     * @param right other step
     * @return -1, 0, +1
     */
    public static int compare (Step left,
                               Step right)
    {
        return Integer.signum(steps.indexOf(left) - steps.indexOf(right));
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the concrete step for a given name
     *
     * @param str the step name
     * @return the concrete step, or null if not found
     */
    public static Step valueOf (String str)
    {
        Step step = stepMap.get(str);

        if (step == null) {
            String msg = "Step not found: " + str;
            logger.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        return step;
    }

    //--------//
    // values //
    //--------//
    /**
     * Report a non-modifiable view of the step list
     *
     * @return the sequence of steps defined
     */
    public static List<Step> values ()
    {
        return Collections.unmodifiableList(steps);
    }

    //------//
    // next //
    //------//
    /**
     * Report the step just after the provided one
     *
     * @return the following step, or null if none
     */
    static Step next (Step step)
    {
        boolean found = false;

        for (Step s : steps) {
            if (found) {
                return s;
            }

            if (s == step) {
                found = true;
            }
        }

        return null;
    }

    //----------//
    // previous //
    //----------//
    /**
     * Report the step just before the provided one
     *
     * @return the preceding step, or null if none
     */
    static Step previous (Step step)
    {
        Step prev = null;

        for (Step s : steps) {
            if (s == step) {
                return prev;
            }

            prev = s;
        }

        return null;
    }

    //-------//
    // range //
    //-------//
    /**
     * Report the range of steps from 'first' to 'last' inclusive
     *
     * @param first the first step of the range
     * @param last  the last step of the range
     * @return the step sequence (which is empty if first > last)
     */
    static SortedSet<Step> range (Step first,
                                  Step last)
    {
        List<Step> stepList = new ArrayList<Step>();
        boolean started = false;

        for (Step step : steps) {
            if (step == first) {
                started = true;
            }

            if (started) {
                stepList.add(step);
            }

            if (step == last) {
                break;
            }
        }

        SortedSet<Step> sorted = new TreeSet<Step>(comparator);
        sorted.addAll(stepList);

        return sorted;
    }

    //---------//
    // addStep //
    //---------//
    private static void addStep (Step step)
    {
        steps.add(step);
        stepMap.put(step.getName(), step);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a subclass of {@link omr.constant.Constant}, meant to
     * store a {@link Step} value.
     */
    public static class Constant
            extends omr.constant.Constant
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default Step value
         * @param description  the semantic of the constant
         */
        public Constant (Step defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Retrieve the current constant value
         *
         * @return the current Step value
         */
        public Step getValue ()
        {
            return (Step) getCachedValue();
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new Step value
         */
        public void setValue (Step val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        @Override
        protected Step decode (java.lang.String str)
        {
            return valueOf(str);
        }
    }
}
