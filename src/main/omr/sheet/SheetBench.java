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

import omr.score.Score;
import omr.score.ScoreManager;

import omr.step.Step;

import java.io.*;
import java.util.*;

/**
 * Class {@code SheetBench} is in charge of recording all important information
 * related to the processing of a music sheet
 *
 * @author Hervé Bitteur
 */
public class SheetBench
{
    //~ Instance fields --------------------------------------------------------

    /** The set of properties */
    private final Properties props = new Properties();

    /** The related sheet */
    private final Sheet sheet;

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
        setProp("date", date.toString());
        setProp("program", Main.getToolName());
        setProp("version", Main.getToolVersion());
        setProp("revision", Main.getToolBuild());
        setProp("image", path);

        flushBench();
    }

    //~ Methods ----------------------------------------------------------------

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
        setProp("whole.cancelled", "true");
    }

    //----------------------//
    // recordImageDimension //
    //----------------------//
    public void recordImageDimension (int width,
                                      int height)
    {
        setProp("image.width", "" + width);
        setProp("image.height", "" + height);
    }

    //-----------------//
    // recordPartCount //
    //-----------------//
    public void recordPartCount (int partCount)
    {
        setProp("parts", "" + partCount);
    }

    //-------------//
    // recordScale //
    //-------------//
    public void recordScale (Scale scale)
    {
        setProp("scale.mainBack", "" + scale.mainBack());
        setProp("scale.mainFore", "" + scale.mainFore());
        setProp("scale.interline", "" + scale.interline());
    }

    //------------//
    // recordSkew //
    //------------//
    public void recordSkew (double skew)
    {
        setProp("skew", "" + skew);
    }

    //------------------//
    // recordStaveCount //
    //------------------//
    public void recordStaveCount (int staveCount)
    {
        setProp("staves", "" + staveCount);
    }

    //------------//
    // recordStep //
    //------------//
    public void recordStep (Step step)
    {
        long now = System.currentTimeMillis();
        setProp(
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
        setProp("systems", "" + systemCount);
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
        // Finalize this bench?
        if (complete) {
            long wholeDuration = System.currentTimeMillis() - startTime;
            setProp("whole.duration", "" + wholeDuration);

            cleanupProps();
        }

        //        script = sheet.getScript();

        // Sort and store to file
        SortedSet<String> keys = new TreeSet<String>();

        for (Object obj : props.keySet()) {
            String key = (String) obj;
            keys.add(key);
        }

        PrintWriter writer = new PrintWriter(output);

        for (String key : keys) {
            writer.println(key + " = " + props.getProperty(key));
        }

        writer.flush();
    }

    //---------//
    // setProp //
    //---------//
    /**
     * This is a specific setProperty functionality, that creates new keys by
     * appending numbered suffixes
     * @param radix
     * @param value
     */
    private void setProp (String radix,
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
     * Clean up the current properties, radix by radix, playing with the
     * key suffixes
     */
    private void cleanupProps ()
    {
        // Retrieve key radices
        SortedSet<String> radices = new TreeSet<String>();

        for (Object obj : props.keySet()) {
            String key = (String) obj;
            int    dot = key.lastIndexOf(".");
            String radix = key.substring(0, dot);
            radices.add(radix);
        }

        for (String radix : radices) {
            // Do we have just 1 property?
            if (!props.containsKey(keyOf(radix, 2))) {
                String key1 = keyOf(radix, 1);
                props.setProperty(radix, props.getProperty(key1));
                props.remove(key1);
            }
        }

        // Special case for step: we sum the durations
        for (String radix : radices) {
            if (radix.startsWith("step.") && radix.endsWith(".duration")) {
                // Sum all the values into the radix property
                int sum = 0;

                for (int index = 1;; index++) {
                    String key = keyOf(radix, index);
                    String str = props.getProperty(key);

                    if (str == null) {
                        break;
                    } else {
                        sum += Integer.parseInt(str);
                    }
                }

                props.setProperty(radix, "" + sum);
            }
        }
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
