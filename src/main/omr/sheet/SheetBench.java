//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t B e n c h                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreManager;

import omr.step.Step;

import omr.util.FileUtil;

import java.io.*;
import java.util.*;

/**
 * Class {@code SheetBench} is in charge of recording all important information
 * related to the processing of a music sheet, and producing an output formatted
 * as "key = value" lines of text.
 *
 * <p>In order to cope with possible multiple recordings with the same radix, we
 * always add to a temporary property set, using numbered suffixes (.01, .02,
 * etc) so that no data is ever overwritten. The temporary set contains only
 * lines formatted as "radix.suffix = value".</p>
 *
 * <p>When the recordings are to be flushed, the temporary set is used to
 * produce a clean set of external properties, according to the following
 * rules:<ul>
 *
 * <li>When only the .01 suffix exists for a given radix, then the externals
 * just contains the "radix = value" line, and the .01 suffix is not transferred
 * to the output.</li>
 *
 * <li>When more than the .01 suffix exist for a given radix, then these
 * intermediate "radix.suffix = value" pairs are copied to the externals as they
 * are. The last key/value pair is also used to set the "radix = value" pair in
 * the externals (so that the latest value is always accessible through its
 * simple radix)</li>
 *
 * <li>For a special kind of keys (step.[name].duration), the "radix = value"
 * line does not contain the latest intermediate value, but rather the sum of
 * all intermediate values</li></ul>
 *
 * <p>The recorded data can be flushed to disk on specific occasions, to make
 * sure that no data ever get lost even in the case of step cancellation or
 * program interruption. <br/>In case of step cancellation the line
 * "whole.cancelled = true" is added to the externals. <br/>In case of program
 * interruption the line "whole.interrupted = true" is kept in the
 * externals.</p>
 *
 * @author Hervé Bitteur
 */
public class SheetBench
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetBench.class);

    /** Special key which indicates that an interruption has occurred */
    private static final String INTERRUPTION_KEY = "whole.interrupted";

    //~ Instance fields --------------------------------------------------------

    /** The internal set of properties */
    private final Properties props = new Properties();

    /** The related sheet */
    private final Sheet sheet;

    /** Local sheet radix */
    private final String radix;

    /** Time stamp when this instance was created */
    private final long startTime = System.currentTimeMillis();

    /** Time when we started current step */
    private long stepStartTime = startTime;

    /** Starting date */
    private final Date date = new Date(startTime);

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SheetBench //
    //------------//
    /**
     * Creates a new SheetBench object.
     * @param sheet the related sheet
     * @param path path to the image file
     */
    public SheetBench (Sheet  sheet,
                       String path)
    {
        this.sheet = sheet;

        // To be later removed, but only at normal completion point
        addProp(INTERRUPTION_KEY, "true");

        addProp("date", date.toString());
        addProp("program", Main.getToolName());
        addProp("version", Main.getToolVersion());
        addProp("revision", Main.getToolBuild());
        addProp("image", path);

        radix = FileUtil.getNameSansExtension(new File(path));

        flushBench();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getRadix //
    //----------//
    public String getRadix ()
    {
        return radix;
    }

    //----------//
    // getScore //
    //----------//
    public Score getScore ()
    {
        return sheet.getScore();
    }

    //--------------------//
    // recordCancellation //
    //--------------------//
    public void recordCancellation ()
    {
        addProp("whole.cancelled", "true");
    }

    //----------------------//
    // recordImageDimension //
    //----------------------//
    public void recordImageDimension (int width,
                                      int height)
    {
        addProp("image.width", "" + width);
        addProp("image.height", "" + height);
    }

    //-----------------//
    // recordPartCount //
    //-----------------//
    public void recordPartCount (int partCount)
    {
        addProp("parts", "" + partCount);
    }

    //-------------//
    // recordScale //
    //-------------//
    public void recordScale (Scale scale)
    {
        addProp("scale.mainBack", "" + scale.mainBack());
        addProp("scale.mainFore", "" + scale.mainFore());
        addProp("scale.interline", "" + scale.interline());
    }

    //------------//
    // recordSkew //
    //------------//
    public void recordSkew (double skew)
    {
        addProp("skew", "" + skew);
    }

    //------------------//
    // recordStaveCount //
    //------------------//
    public void recordStaveCount (int staveCount)
    {
        addProp("staves", "" + staveCount);
    }

    //------------//
    // recordStep //
    //------------//
    public void recordStep (Step step)
    {
        long now = System.currentTimeMillis();
        addProp(
            "step." + step.label.toLowerCase() + ".duration",
            "" + (now - stepStartTime));
        stepStartTime = now;
        flushBench();
    }

    //-------------------//
    // recordSystemCount //
    //-------------------//
    public void recordSystemCount (int systemCount)
    {
        addProp("systems", "" + systemCount);
    }

    //-------//
    // store //
    //-------//
    /**
     * Store this bench into an output stream
     *
     * @param output the output stream to be written
     * @param complete true if bench data must be finalized
     * @throws IOException
     */
    public void store (OutputStream output,
                       boolean      complete)
        throws IOException
    {
        // Build external properties
        Properties externals = cleanupProps();

        // What do we do with the script data? and with app constants?
        // TBD

        // Insert global duration (up till now)
        long wholeDuration = System.currentTimeMillis() - startTime;
        externals.setProperty("whole.duration", "" + wholeDuration);

        // Finalize this bench?
        if (complete) {
            externals.remove(INTERRUPTION_KEY);
        }

        // Sort and store to file
        SortedSet<String> keys = new TreeSet<String>();

        for (Object obj : externals.keySet()) {
            String key = (String) obj;
            keys.add(key);
        }

        PrintWriter writer = new PrintWriter(output);

        for (String key : keys) {
            writer.println(key + " = " + externals.getProperty(key));
        }

        writer.flush();
    }

    //---------//
    // addProp //
    //---------//
    /**
     * This is a specific setProperty functionality, that creates unique keys by
     * appending numbered suffixes
     * @param radix the provided radix (to which proper suffix will be appended)
     * @param value the property value
     */
    private void addProp (String radix,
                          String value)
    {
        if ((value == null) || (value.length() == 0)) {
            return;
        }

        String key = null;
        int    index = 0;

        do {
            key = keyOf(radix, ++index);
        } while (props.containsKey(key));

        props.setProperty(key, value);
    }

    //--------------//
    // cleanupProps //
    //--------------//
    /**
     * Build the externals properties, radix by radix, playing with the key
     * suffixes
     */
    private Properties cleanupProps ()
    {
        Properties  externals = new Properties();

        // Retrieve key radices
        Set<String> radices = new HashSet<String>();

        for (Object obj : props.keySet()) {
            String key = (String) obj;
            int    dot = key.lastIndexOf(".");
            String radix = key.substring(0, dot);
            radices.add(radix);
        }

        // Browse radices
        for (String radix : radices) {
            if (!props.containsKey(keyOf(radix, 2))) {
                // We have just 1 property: we rename it as the radix
                String key1 = keyOf(radix, 1);
                externals.setProperty(radix, props.getProperty(key1));
            } else {
                // We have several properties, so we keep all the intermediate values
                // Special case for step radix: we sum up the durations
                // Standard radix case: we use the latest value
                boolean isStep = radix.startsWith("step.") &&
                                 radix.endsWith(".duration");
                int     sum = 0;

                for (int index = 1;; index++) {
                    String key = keyOf(radix, index);
                    String str = props.getProperty(key);

                    if (str == null) {
                        break;
                    } else {
                        // Keep intermediate value
                        externals.setProperty(key, str);

                        if (isStep) {
                            // Sum up step durations
                            sum += Integer.parseInt(str);
                        } else {
                            // Overwrite the radix value
                            externals.setProperty(radix, str);
                        }
                    }
                }

                if (isStep) {
                    // Write the total sum as the radix value
                    externals.setProperty(radix, "" + sum);
                }
            }
        }

        return externals;
    }

    //------------//
    // flushBench //
    //------------//
    /**
     * Flush the current content of bench to disk
     */
    private void flushBench ()
    {
        ScoreManager.getInstance()
                    .storeBench(this, null, false);
    }

    //-------//
    // keyOf //
    //-------//
    private String keyOf (String radix,
                          int    index)
    {
        return String.format("%s.%02d", radix, index);
    }
}
