//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t e p s                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.plugin.Plugin;
import omr.plugin.PluginsManager;

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
 *
 * @author Hervé Bitteur
 */
public class Steps
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Steps.class);

    // Mandatory step names
    public static final String LOAD = "LOAD";

    public static final String SCALE = "SCALE";

    public static final String GRID = "GRID";

    public static final String SYSTEMS = "SYSTEMS";

    public static final String MEASURES = "MEASURES";

    public static final String TEXTS = "TEXTS";

    public static final String STICKS = "STICKS";

    public static final String SYMBOLS = "SYMBOLS";

    public static final String PAGES = "PAGES";

    public static final String SCORE = "SCORE";

    // Optional step names
    public static final String PRINT = "PRINT";

    public static final String EXPORT = "EXPORT";

    public static final String PLUGIN = "PLUGIN";

    /** Ordered sequence of steps */
    private static final List<Step> steps = new ArrayList<>();

    /** Map of defined steps */
    private static final Map<String, Step> stepMap = new HashMap<>();

    static {
        // Mandatory steps in proper order
        // -------------------------------
        addStep(new LoadStep());
        addStep(new ScaleStep());
        addStep(new GridStep());
        addStep(new SystemsStep());
        addStep(new MeasuresStep());
        addStep(new TextsStep());
        addStep(new SticksStep());
        addStep(new SymbolsStep());
        addStep(new PagesStep());
        addStep(new ScoreStep());

        // Optional steps, in whatever order
        // ---------------------------------
        addStep(new PrintStep());
        addStep(new ExportStep());

        // Plugin step depends on default plugin
        Plugin plugin = PluginsManager.getInstance()
                .getDefaultPlugin();

        if (plugin != null) {
            addStep(new PluginStep(plugin));
        }
    }

    /** Compare steps WRT their position in the sequence of defined steps */
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
    /** First step */
    public static final Step first = steps.iterator()
            .next();

    /** Last step */
    public static final Step last = steps.listIterator(steps.size())
            .previous();

    //-------//
    // Steps // Not meant to be instantiated
    //-------//
    private Steps ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // compare //
    //---------//
    /**
     * Compare two steps wrt their position in steps sequence
     *
     * @param left  one step
     * @param right other step
     * @return -1,0,+1
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
     * Report the step right after the provided one
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
     * Report the step right before the provided one
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
     * Report the range of steps from 'left' to 'right' inclusive
     *
     * @param left  the first step of the range
     * @param right the last step of the range
     * @return the step sequence (which is empty if left > right)
     */
    static SortedSet<Step> range (Step left,
                                  Step right)
    {
        List<Step> stepList = new ArrayList<>();
        boolean started = false;

        for (Step step : steps) {
            if (step == left) {
                started = true;
            }

            if (started) {
                stepList.add(step);
            }

            if (step == right) {
                break;
            }
        }

        SortedSet<Step> sorted = new TreeSet<>(comparator);
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

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a subclass of
     * {@link omr.constant.Constant}, meant to store a {@link Step} value.
     */
    public static class Constant
            extends omr.constant.Constant
    {
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
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
