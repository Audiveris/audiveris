//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             C L I                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.script.ExportTask;
import omr.script.PrintTask;

import omr.sheet.Book;

import omr.step.ProcessingCancellationException;
import omr.step.Step;

import omr.util.Dumping;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Class {@code CLI} parses and holds the parameters of the command line interface.
 * <p>
 * At any location in the command line, an item starting with the &#64; character will be
 * interpreted as an indirection to a file, whose content must be expanded in line.
 * <p>
 * The command line parameters can be (order and case are not relevant):
 * <dl>
 *
 * <dt><b>-batch</b></dt>
 * <dd>Runs with no graphic user interface</dd>
 *
 * <dt><b>-bench</b></dt>
 * <dd>Outputs bench data (to default directory)</dd>
 *
 *
 * <dt><b>-benchDir DIR</b></dt>
 * <dd>Outputs bench data to provided directory</dd>
 *
 * <dt><b>-export</b></dt>
 * <dd>Outputs MusicXML data (to default directory)</dd>
 *
 * <dt><b>-exportDir DIR</b></dt>
 * <dd>Outputs MusicXML data to provided directory</dd>
 *
 * <dt><b>-help</b></dt>
 * <dd>Displays help about application then stops</dd>
 *
 * <dt><b>-input FILE</b></dt>
 * <dd>Reads the provided input image file</dd>
 *
 * <dt><b>-option KEY=VALUE</b></dt>
 * <dd>Defines an application constant (that could also be set via the pull-down menu
 * "Tools|Options" in the GUI)</dd>
 *
 * <dt><b>-print</b></dt>
 * <dd>Prints out score (to default directory)</dd>
 *
 * <dt><b>-printDir DIR</b></dt>
 * <dd>Prints out score to provided directory</dd>
 *
 * <dt><b>-project FILE</b></dt>
 * <dd>Reads the provided project file</dd>
 *
 * <dt><b>-script FILE</b></dt>
 * <dd>Runs the provided script file</dd>
 *
 * <dt><b>-sheets N...</b></dt>
 * <dd>Defines specific sheets (counted from 1) to process in input file</dd>
 *
 * <dt><b>-step STEP</b></dt>
 * <dd>Defines a specific transcription target step. This step will be performed on each input
 * referenced from the command line</dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class CLI
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Name of the program. */
    private final String toolName;

    /** Actual sequence of arguments for this run. */
    private String[] actualArgs;

    /** Parameters structure to be populated. */
    private final Parameters params = new Parameters();

    /** CLI parser. */
    private final CmdLineParser parser = new CmdLineParser(params);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CLI object.
     *
     * @param toolName the program name
     */
    public CLI (final String toolName)
    {
        this.toolName = toolName;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getBenchFolder //
    //----------------//
    /**
     * Report the bench folder if present on the CLI
     *
     * @return the CLI bench folder, or null
     */
    public Path getBenchFolder ()
    {
        if (params.benchFolder == null) {
            return null;
        }

        return params.benchFolder.toPath();
    }

    //-----------------//
    // getExportFolder //
    //-----------------//
    /**
     * Report the export folder if present on the CLI
     *
     * @return the CLI export path, or null
     */
    public Path getExportFolder ()
    {
        if (params.exportFolder == null) {
            return null;
        }

        return params.exportFolder.toPath();
    }

    //----------------//
    // getInputsTasks //
    //----------------//
    /**
     * Prepare the processing of input image files listed on command line.
     * On each input file, we apply the actions specified if any via -step, -print, -export.
     *
     * @return the collection of proper callable instances
     */
    public List<Callable<Void>> getInputsTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired step on each book
        for (final File input : params.inputFiles) {
            final Path path = input.toPath();

            tasks.add(
                    new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                        throws Exception
                        {
                            final Step target = params.step;
                            final SortedSet<Integer> sheetIds = params.getSheetIds();

                            if (target != null) {
                                logger.info(
                                        "Launching {} on {} {}",
                                        target,
                                        input,
                                        (!sheetIds.isEmpty()) ? ("sheets " + sheetIds) : "");
                            }

                            if (Files.exists(path)) {
                                final Book book = OMR.getEngine().loadInput(path);

                                try {
                                    // Create book sheets and perform desired steps if any
                                    book.doStep(target, sheetIds);

                                    // Book print output?
                                    if (params.print || (params.printFolder != null)) {
                                        logger.debug("Print output");
                                        new PrintTask(params.printFolder).core(
                                                book.getSheets().get(0));
                                    }

                                    // Book export output?
                                    if (params.export || (params.exportFolder != null)) {
                                        logger.debug("Export output");
                                        new ExportTask(params.exportFolder).core(
                                                book.getSheets().get(0));
                                    }
                                } catch (ProcessingCancellationException pce) {
                                    logger.warn("Cancelled " + book, pce);
                                    throw pce;
                                } catch (Throwable ex) {
                                    logger.warn("Exception occurred", ex);
                                    throw ex;
                                } finally {
                                    // Close (when in batch mode only)
                                    if ((OMR.getGui() == null) /* &&
                                     * constants.closeBookOnEnd.isSet() */) {
                                        book.close();
                                    }
                                }

                                return null;
                            } else {
                                String msg = "Could not find file " + path;
                                logger.warn(msg);
                                throw new RuntimeException(msg);
                            }
                        }

                        @Override
                        public String toString ()
                        {
                            return "Input " + path;
                        }
                    });
        }

        return tasks;
    }

    //------------------//
    // getProjectsTasks //
    //------------------//
    /**
     * Prepare the processing of project files listed on command line.
     * ??? On each project file, we apply the actions specified if any via -step, -print, -export.
     *
     * @return the collection of proper callable instances
     */
    public List<Callable<Void>> getProjectsTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired step on each book
        for (final File project : params.projectFiles) {
            final Path path = project.toPath();

            tasks.add(
                    new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                        throws Exception
                        {
//                            final Step target = params.step;
//                            final SortedSet<Integer> sheetIds = params.getSheetIds();
//
//                            if (target != null) {
//                                logger.info(
//                                        "Launching {} on {} {}",
//                                        target,
//                                        project,
//                                        (!sheetIds.isEmpty()) ? ("sheets " + sheetIds) : "");
//                            }
//
                            if (Files.exists(path)) {
                                final Book book = OMR.getEngine().loadProject(path);
//
//                                try {
//                                    // Create book sheets and perform desired steps if any
//                                    book.doStep(target, sheetIds);
//
//                                    // Book print output?
//                                    if (params.print || (params.printFolder != null)) {
//                                        logger.debug("Print output");
//                                        new PrintTask(params.printFolder).core(
//                                                book.getSheets().get(0));
//                                    }
//
//                                    // Book export output?
//                                    if (params.export || (params.exportFolder != null)) {
//                                        logger.debug("Export output");
//                                        new ExportTask(params.exportFolder).core(
//                                                book.getSheets().get(0));
//                                    }
//                                } catch (ProcessingCancellationException pce) {
//                                    logger.warn("Cancelled " + book, pce);
//                                    throw pce;
//                                } catch (Throwable ex) {
//                                    logger.warn("Exception occurred", ex);
//                                    throw ex;
//                                } finally {
//                                    // Close (when in batch mode only)
//                                    if ((OMR.getGui() == null) /* &&
//                                     * constants.closeBookOnEnd.isSet() */) {
//                                        book.close();
//                                    }
//                                }

                                return null;
                            } else {
                                String msg = "Could not find file " + path;
                                logger.warn(msg);
                                throw new RuntimeException(msg);
                            }
                        }

                        @Override
                        public String toString ()
                        {
                            return "Project " + path;
                        }
                    });
        }

        return tasks;
    }

    //------------//
    // getOptions //
    //------------//
    /**
     * Report the properties set at the CLI level
     *
     * @return the CLI-defined constant values
     */
    public Properties getOptions ()
    {
        if (params == null) {
            return null;
        }

        return params.options;
    }

    //---------------//
    // getParameters //
    //---------------//
    /**
     * Parse the CLI arguments and return the populated parameters structure.
     *
     * @param args the CLI arguments
     * @return the parsed parameters, or null if failed
     * @throws org.kohsuke.args4j.CmdLineException
     */
    public Parameters getParameters (final String... args)
            throws CmdLineException
    {
        logger.debug("CLI args: {}", Arrays.toString(args));
        actualArgs = args;

        parser.parseArgument(args);

        if (logger.isDebugEnabled()) {
            new Dumping().dump(params);
        }

        if (params.helpMode) {
            printUsage();
        }

        return params;
    }

    //----------------//
    // getPrintFolder //
    //----------------//
    /**
     * Report the print folder if present on the CLI
     *
     * @return the CLI print path, or null
     */
    public Path getPrintFolder ()
    {
        if (params.printFolder == null) {
            return null;
        }

        return params.printFolder.toPath();
    }

    //-----------------//
    // getScriptsTasks //
    //-----------------//
    /**
     * Prepare the processing of scripts listed on command line
     *
     * @return the collection of proper script callables
     */
    public List<Callable<Void>> getScriptsTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired scripts in parallel
        for (final File scriptFile : params.scriptFiles) {
            tasks.add(
                    new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                        throws Exception
                        {
                            OMR.getEngine().loadScript(scriptFile.toPath());

                            //                            ScriptManager.getInstance().loadAndRun(
                            //                                    scriptFile,
                            //                                    constants.closeBookOnEnd.isSet());
                            //
                            return null;
                        }

                        @Override
                        public String toString ()
                        {
                            return "Script " + scriptFile;
                        }
                    });
        }

        return tasks;
    }

    //------------------//
    // printCommandLine //
    //------------------//
    /**
     * Printout the command line with its actual parameters.
     */
    public void printCommandLine ()
    {
        StringBuilder sb = new StringBuilder("Command line parameters: ");

        if (toolName != null) {
            sb.append(toolName).append(" ");
        }

        sb.append(actualArgs);
        logger.info(sb.toString());
    }

    //------------//
    // printUsage //
    //------------//
    /**
     * Printout the general syntax for the command line.
     */
    public void printUsage ()
    {
        StringBuilder buf = new StringBuilder();

        // Print version
        buf.append("\n").append(toolName).append(" Version:");
        buf.append("\n   ").append(WellKnowns.TOOL_REF);

        // Print arguments syntax
        buf.append("\nSyntax:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        // Print all steps
        buf.append("\nSteps are in order (non case-sensitive):");

        for (Step step : Step.values()) {
            buf.append(String.format("%n   %-16s : %s", step.toString(), step.getDescription()));
        }

        buf.append("\n");
        logger.info(buf.toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------------//
    // IntArrayOptionHandler //
    //-----------------------//
    /**
     * Argument handler for an array of integers.
     */
    public static class IntArrayOptionHandler
            extends OptionHandler<Integer>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public IntArrayOptionHandler (CmdLineParser parser,
                                      OptionDef option,
                                      Setter<Integer> setter)
        {
            super(parser, option, setter);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getDefaultMetaVariable ()
        {
            return "int[]";
        }

        @Override
        public int parseArguments (org.kohsuke.args4j.spi.Parameters params)
                throws CmdLineException
        {
            int counter = 0;

            for (; counter < params.size(); counter++) {
                String param = params.getParameter(counter);

                if (param.startsWith("-")) {
                    break;
                }

                for (String p : param.split(" ")) {
                    if (!p.isEmpty()) {
                        setter.addValue(Integer.parseInt(p));
                    }
                }
            }

            return counter;
        }
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * The structure that collects the various parameters parsed out of the command line.
     */
    public static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Help mode. */
        @Option(name = "-help", help = true, usage = "Displays help about application then stops")
        boolean helpMode;

        /** Batch mode. */
        @Option(name = "-batch", usage = "Runs with no graphic user interface")
        boolean batchMode;

        /** Specific step. */
        @Option(name = "-step", usage = "Defines a specific transcription target step")
        Step step;

        /** The map of application options. */
        @Option(name = "-option", usage = "Defines an application constant", handler = PropertyOptionHandler.class)
        Properties options;

        /** The list of script files to execute. */
        @Option(name = "-script", usage = "Runs the provided script file", metaVar = "<script-file>")
        final List<File> scriptFiles = new ArrayList<File>();

        /** The list of input image file names to load. */
        @Option(name = "-input", usage = "Reads the provided input image file", metaVar = "<input-file>")
        final List<File> inputFiles = new ArrayList<File>();

        /** The list of project file names to load. */
        @Option(name = "-project", usage = "Reads the provided project file", metaVar = "<project-file>")
        final List<File> projectFiles = new ArrayList<File>();

        /** The set of sheet IDs to load. */
        @Option(name = "-sheets", usage = "Defines specific sheets (counted from 1) to process", handler = IntArrayOptionHandler.class)
        private final List<Integer> sheets = new ArrayList<Integer>();

        /** Should bench data be produced?. */
        @Option(name = "-bench", usage = "Outputs bench data (to default directory)")
        boolean bench;

        /** Target directory for bench data. */
        @Option(name = "-benchDir", usage = "Outputs bench data to provided directory", metaVar = "<bench-folder>")
        File benchFolder;

        /** Should MusicXML data be produced?. */
        @Option(name = "-export", usage = "Outputs MusicXML data (to default directory)")
        boolean export;

        /** Target directory for MusicXML data. */
        @Option(name = "-exportDir", usage = "Outputs MusicXML data to provided directory", metaVar = "<export-folder>")
        File exportFolder;

        /** Should book be printed?. */
        @Option(name = "-print", usage = "Prints out score (to default directory)")
        boolean print;

        /** Target directory for print. */
        @Option(name = "-printDir", usage = "Prints out score to provided directory", metaVar = "<print-folder>")
        File printFolder;

        //~ Constructors ---------------------------------------------------------------------------
        private Parameters ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // getSheetIds //
        //-------------//
        /**
         * Report the set of sheet ids if present on the CLI
         *
         * @return the CLI sheet IDs, perhaps empty but not null
         */
        public SortedSet<Integer> getSheetIds ()
        {
            return new TreeSet(sheets);
        }
    }

    //-----------------------//
    // PropertyOptionHandler //
    //-----------------------//
    /**
     * Argument handler for a property definition.
     */
    public static class PropertyOptionHandler
            extends OptionHandler<Properties>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public PropertyOptionHandler (CmdLineParser parser,
                                      OptionDef option,
                                      Setter<? super Properties> setter)
        {
            super(parser, option, setter);

            if (setter.asFieldSetter() == null) {
                throw new IllegalArgumentException(
                        "PropertyOptionHandler can only work with fields");
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getDefaultMetaVariable ()
        {
            return "key=value";
        }

        @Override
        public int parseArguments (org.kohsuke.args4j.spi.Parameters params)
                throws CmdLineException
        {
            String name = params.getParameter(-1);
            String pair = params.getParameter(0);
            FieldSetter fs = setter.asFieldSetter();
            Properties props = (Properties) fs.getValue();

            if (props == null) {
                props = new Properties();
                fs.addValue(props);
            }

            try {
                props.load(new StringReader(pair));
            } catch (Exception ex) {
                throw new CmdLineException(owner, "Error in " + name + " " + pair, ex);
            }

            return 1;
        }
    }
}
