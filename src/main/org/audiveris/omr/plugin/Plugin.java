//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          P l u g i n                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Plugin} describes a plugin instance, encapsulating the relationship
 * with an external program to consume MusicXML export.
 * <p>
 * A plugin element is an XML fragment, made of:
 * <dl>
 * <dt>id</dt>
 * <dd>(mandatory attribute) Unique name for the plugin</dd>
 * <dt>tip</dt>
 * <dd>(optional attribute) A description text to appear as a user tip</dd>
 * <dt>arg</dt>
 * <dd>(mandatory element) There must be one arg element for each object in the command line.
 * A specific arg value ({}) indicates where to insert at run-time the path to MusicXML file.
 * </dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "plugin")
public class Plugin
{

    private static final Logger logger = LoggerFactory.getLogger(Plugin.class);

    /** Id. */
    @XmlAttribute(name = "id")
    private final String id;

    /** Description used for tool tip. */
    @XmlAttribute(name = "tip")
    private final String tip;

    /** Sequence of arguments. */
    @XmlElement(name = "arg")
    private final List<String> args;

    /**
     * Creates a new {@code Plugin} object.
     *
     * @param id   Unique name
     * @param tip  Description text
     * @param args list of arguments
     */
    public Plugin (String id,
                   String tip,
                   List<String> args)
    {
        this.id = id;
        this.tip = tip;
        this.args = args;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private Plugin ()
    {
        id = null;
        tip = null;
        args = null;
    }

    //-------//
    // check //
    //-------//
    /**
     * Check if the plugin description is correct.
     *
     * @return true if OK
     */
    public boolean check ()
    {
        if ((id == null) || id.isEmpty()) {
            logger.warn("No id for {}", this);

            return false;
        }

        for (String arg : args) {
            if (arg.trim().equals("{}")) {
                return true;
            }
        }

        logger.warn("Missing special '{}' arg in plugin {}", getId());

        return false;
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a descriptive sentence for this plugin.
     *
     * @return a sentence meant for tool tip, perhaps null
     */
    public String getDescription ()
    {
        return tip;
    }

    //---------//
    // getname //
    //---------//
    /**
     * Report a name meant for user interface.
     *
     * @return a name for this plugin
     */
    public String getId ()
    {
        return id;
    }

    //---------//
    // getTask //
    //---------//
    /**
     * Report the asynchronous plugin task on provided score.
     *
     * @param book the book to process through this plugin
     * @return the asynchronous plugin task requested
     */
    public Task<Void, Void> getTask (Book book)
    {
        return new PluginTask(book);
    }

    //-----------//
    // runPlugin //
    //-----------//
    /**
     * Run this plugin on the provided book.
     *
     * @param book provided book
     * @return nothing
     */
    public Void runPlugin (Book book)
    {
        Path exportPath = retrieveExport(book);

        if (exportPath == null) {
            logger.warn("No suitable export");

            return null;
        }

        // Build process
        List<String> cli = buildCli(exportPath);
        logger.info("Process: {}", cli);

        ProcessBuilder pb = new ProcessBuilder(cli);
        pb = pb.redirectErrorStream(true); // Merge process error stream with its output stream

        try {
            final Process process = pb.start(); // Start process

            // Consume process output (process output is an input stream for us)
            final InputStream is = process.getInputStream();
            final InputStreamReader isr = new InputStreamReader(is, WellKnowns.FILE_ENCODING);
            final BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            // Wait to get exit value
            final int exitValue = process.waitFor();
            logger.info("{} exit value: {}", getId(), exitValue);
        } catch (IOException |
                 InterruptedException ex) {
            logger.warn("Error launching {} {}" + this, ex.toString(), ex);
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

        sb.append("id:\"").append(id).append("\"");

        if (tip != null) {
            sb.append(" tip:\"").append(tip).append("\"");
        }

        sb.append(" args:").append(args);
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // buildCli //
    //----------//
    private List<String> buildCli (Path exportPath)
    {
        List<String> cli = new ArrayList<>(args.size());

        for (String arg : args) {
            if (arg.trim().equals("{}")) {
                cli.add(exportPath.toString());
            } else {
                cli.add(arg);
            }
        }

        return cli;
    }

    //----------------//
    // retrieveExport //
    //----------------//
    /**
     * Try to retrieve an up-to-date export file
     *
     * @param book the related book
     * @return path to suitable export, or null
     */
    private Path retrieveExport (Book book)
    {
        try {
            // Make sure we have the export file
            final Path exportPathSansExt = BookManager.getDefaultExportPathSansExt(book);

            if (exportPathSansExt == null) {
                logger.warn("Could not get export path");

                return null;
            }

            final Path exportPath = Paths.get(exportPathSansExt + BookManager.getExportExtension());

            if (Files.exists(exportPath)) {
                FileTime exportTime = Files.getLastModifiedTime(exportPath);

                if (!book.isModified()) {
                    Path bookPath = book.getBookPath();

                    if ((bookPath != null) && Files.exists(bookPath)) {
                        FileTime bookTime = Files.getLastModifiedTime(bookPath);

                        if (exportTime.compareTo(bookTime) > 0) {
                            return exportPath; // We can use this export file
                        }
                    }
                }
            }

            book.export();

            if (Files.exists(exportPath)) {
                return exportPath;
            }

            logger.warn("Cannot find file {}", exportPath);
        } catch (IOException ex) {
            logger.warn("Error getting export file " + ex, ex);
        }

        return null;
    }

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

        private final Book book;

        PluginTask (Book book)
        {
            this.book = book;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Void doInBackground ()
                throws InterruptedException
        {
            return Plugin.this.runPlugin(book);
        }
    }
}
