//----------------------------------------------------------------------------//
//                                                                            //
//                                P l u g i n                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.plugin;

import omr.log.Logger;

import omr.score.Score;

import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BasicTask;
import omr.util.FileUtil;

import org.jdesktop.application.Task;

import java.io.*;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import omr.WellKnowns;

/**
 * Class {@code Plugin} describes a plugin instance, encapsulating the
 * relationship with the underlying javascript file.
 *
 * <p>A plugin is meant to describe the connection between Audiveris and an
 * external program, which will consume the MusicXML file exported by
 * Audiveris.</p>
 *
 * <p>A plugin is a javascript file, meant to export:
 * <dl>
 * <dt>pluginTitle</dt>
 * <dd>(string) The title to appear in Plugins pull-down menu</dd>
 * <dt>pluginTip</dt>
 * <dd>(string) A description text to appear as a user tip in Plugins menu</dd>
 * <dt>pluginCli</dt>
 * <dd>(function) A javascript function which returns the precise list of
 * arguments used when calling the external program. Note that the actual call
 * is not made by the javascript code, but by Audiveris itself for an easier
 * handling of input and output streams.</dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class Plugin
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Plugin.class);

    //~ Instance fields --------------------------------------------------------
    /** Related javascript file */
    private final File file;

    /** Related engine */
    private ScriptEngine engine;

    /** Plugin title */
    private String title;

    /** Description used for tool tip */
    private String tip;

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Plugin //
    //--------//
    /**
     * Creates a new Plugin object.
     *
     * @param file related javascript file
     */
    public Plugin (File file)
    {
        this.file = file;

        evaluateScript();

        logger.fine("Created {0}", this);
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a descriptive sentence for this plugin.
     *
     * @return a sentence meant for tool tip
     */
    public String getDescription ()
    {
        if (tip != null) {
            return tip;
        } else {
            // Default value
            return getId();
        }
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report a unique ID for this plugin.
     *
     * @return plugin unique ID
     */
    public String getId ()
    {
        return FileUtil.getNameSansExtension(file);
    }

    //---------//
    // getTask //
    //---------//
    /**
     * Report the asynchronous plugin task on provided score.
     *
     * @param score the score to process through this plugin
     */
    public Task<Void, Void> getTask (Score score)
    {
        return new PluginTask(score);
    }

    //----------//
    // getTitle //
    //----------//
    /**
     * Report a title meant for user interface.
     *
     * @return a title for this plugin
     */
    public String getTitle ()
    {
        if (title != null) {
            return title;
        } else {
            return getId();
        }
    }

    //-----------//
    // runPlugin //
    //-----------//
    public Void runPlugin (Score score)
    {
        // Make sure we have the export file
        Stepping.ensureScoreStep(Steps.valueOf(Steps.EXPORT), score);

        final File exportFile = score.getExportFile();

        if (exportFile == null) {
            logger.warning("Could not get export file");

            return null;
        }

        // Retrieve proper sequence of command items
        List<String> args;

        try {
            logger.fine("{0} doInBackground on {1}", new Object[]{
                        Plugin.this, exportFile});

            Invocable inv = (Invocable) engine;
            Object obj = inv.invokeFunction(
                    "pluginCli",
                    exportFile.getAbsolutePath());

            if (obj instanceof List) {
                args = (List<String>) obj; // Unchecked by compiler
                logger.fine("{0} command args: {1}", Plugin.this, args);
            } else {
                return null;
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            logger.warning(Plugin.this + " error invoking javascript", ex);

            return null;
        }

        // Spawn the command
        logger.info("Launching {0} on {1}",
                    new Object[]{Plugin.this.getTitle(), score.getRadix()});

        ProcessBuilder pb = new ProcessBuilder(args);
        pb = pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is,
                                                          WellKnowns.encoding);
            BufferedReader br = new BufferedReader(isr);

            // Consume process output
            String line;

            while ((line = br.readLine()) != null) {
                logger.fine(line);
            }

            // Wait to get exit value
            try {
                int exitValue = process.waitFor();

                if (exitValue != 0) {
                    logger.warning("{0} exited with value {1}",
                                   new Object[]{Plugin.this, exitValue});
                } else {
                    logger.fine("{0} exit value is {1}",
                                new Object[]{Plugin.this, exitValue});
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            logger.warning(Plugin.this + " error launching editor", ex);
        }

        return null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ").append(getId());

        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // evaluateScript //
    //----------------//
    /**
     * Evaluate the plugin script to get precise information built.
     */
    private void evaluateScript ()
    {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("JavaScript");

        try {
            InputStream is = new FileInputStream(file);
            Reader reader = new InputStreamReader(is, WellKnowns.encoding);
            engine.eval(reader);

            // Retrieve information from script
            title = (String) engine.get("pluginTitle");
            tip = (String) engine.get("pluginTip");
        } catch (FileNotFoundException | UnsupportedEncodingException |
                 ScriptException ex) {
            logger.warning(this + " error", ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // PluginTask //
    //------------//
    /**
     * Handles the processing defined by the underlying javascript.
     * The lifecycle of this instance is limited to the duration of the task.
     */
    private class PluginTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final Score score;

        //~ Constructors -------------------------------------------------------
        public PluginTask (Score score)
        {
            this.score = score;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        @SuppressWarnings("unchecked")
        protected Void doInBackground ()
                throws InterruptedException
        {
            return Plugin.this.runPlugin(score);
        }
    }
}
