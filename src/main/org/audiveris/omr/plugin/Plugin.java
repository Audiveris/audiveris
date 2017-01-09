//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P l u g i n                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.plugin;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Class {@code Plugin} describes a plugin instance, encapsulating the relationship
 * with the underlying javascript file.
 * <p>
 * A plugin is meant to describe the connection between Audiveris and an external program, which
 * will consume the MusicXML file exported by Audiveris.</p>
 * <p>
 * A plugin is a javascript file, meant to export:
 * <dl>
 * <dt>pluginTitle</dt>
 * <dd>(string) The title to appear in Plugins pull-down menu</dd>
 * <dt>pluginTip</dt>
 * <dd>(string) A description text to appear as a user tip in Plugins menu</dd>
 * <dt>pluginCli</dt>
 * <dd>(function) A javascript function which returns the precise list of arguments used when
 * calling the external program. Note that the actual call is not made by the javascript code, but
 * by Audiveris itself for an easier handling of input and output streams.</dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class Plugin
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Plugin.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related javascript file. */
    private final File file;

    /** Related engine. */
    private ScriptEngine engine;

    /** Plugin title. */
    private String title;

    /** Description used for tool tip. */
    private String tip;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Plugin object.
     *
     * @param file related javascript file
     * @throws JavascriptUnavailableException
     */
    public Plugin (File file)
            throws JavascriptUnavailableException
    {
        this.file = file;

        evaluateScript();

        logger.debug("Created {}", this);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * @param book the book to process through this plugin
     */
    public Task<Void, Void> getTask (Book book)
    {
        return new PluginTask(book);
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
    public Void runPlugin (Book book)
    {
        // Make sure all sheets have been transcribed
        for (SheetStub stub : book.getStubs()) {
            stub.reachStep(Step.PAGE, false);
        }

        // Make sure we have the export file
        ///TODO: Stepping.ensureBookStep(Steps.valueOf(Steps.EXPORT_BOOK), book);
        final Path exportPathSansExt = book.getExportPathSansExt();

        if (exportPathSansExt == null) {
            logger.warn("Could not get export path");

            return null;
        }

        // Retrieve proper sequence of command items
        List<String> args;

        try {
            logger.debug("{} doInBackground on {}", Plugin.this, exportPathSansExt);

            Invocable inv = (Invocable) engine;
            Object obj = inv.invokeFunction(
                    "pluginCli",
                    exportPathSansExt + OMR.COMPRESSED_SCORE_EXTENSION);

            if (obj instanceof List) {
                args = (List<String>) obj; // Unchecked by compiler
                logger.debug("{} command args: {}", this, args);
            } else {
                return null;
            }
        } catch (Exception ex) {
            logger.warn(this + " error invoking javascript", ex);

            return null;
        }

        // Spawn the command
        logger.info("Launching {} on {}", getTitle(), book.getRadix());

        ProcessBuilder pb = new ProcessBuilder(args);
        pb = pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, WellKnowns.FILE_ENCODING);
            BufferedReader br = new BufferedReader(isr);

            // Consume process output
            String line;

            while ((line = br.readLine()) != null) {
                logger.debug(line);
            }

            // Wait to get exit value
            try {
                int exitValue = process.waitFor();

                if (exitValue != 0) {
                    logger.warn("{} exited with value {}", Plugin.this, exitValue);
                } else {
                    logger.debug("{} exit value is {}", Plugin.this, exitValue);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            logger.warn(Plugin.this + " error launching editor", ex);
        }

        return null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

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
            throws JavascriptUnavailableException
    {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("JavaScript");

        if (engine != null) {
            try {
                InputStream is = new FileInputStream(file);
                Reader reader = new InputStreamReader(is, WellKnowns.FILE_ENCODING);
                engine.eval(reader);

                // Retrieve information from script
                title = (String) engine.get("pluginTitle");
                tip = (String) engine.get("pluginTip");
            } catch (Exception ex) {
                logger.warn("{} error", this, ex);
            }
        } else {
            throw new JavascriptUnavailableException();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // PluginTask //
    //------------//
    /**
     * Handles the processing defined by the underlying javascript.
     * The life-cycle of this instance is limited to the duration of the task.
     */
    private class PluginTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public PluginTask (Book book)
        {
            this.book = book;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        @SuppressWarnings("unchecked")
        protected Void doInBackground ()
                throws InterruptedException
        {
            return Plugin.this.runPlugin(book);
        }
    }
}
