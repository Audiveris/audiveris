//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             C L I                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.script.ExportTask;
import omr.script.PrintTask;
import omr.script.SaveTask;

import omr.sheet.Book;
import omr.sheet.SheetStub;

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
 * <dd>Runs with no graphical user interface</dd>
 *
 * <dt><b>-export</b></dt>
 * <dd>Exports MusicXML</dd>
 *
 * <dt><b>-exportAs FILE</b></dt>
 * <dd>Exports MusicXML to specific file</dd>
 *
 * <dt><b>-exportDir DIR</b></dt>
 * <dd>Exports MusicXML to specific folder (ignored if -exportAs is used)</dd>
 *
 * <dt><b>-help</b></dt>
 * <dd>Displays general help then stops</dd>
 *
 * <dt><b>-input FILE</b></dt>
 * <dd>Loads the provided input file (image)</dd>
 *
 * <dt><b>-option KEY=VALUE</b></dt>
 * <dd>Defines an application constant (that could also be set via the pull-down menu
 * "Tools|Options" in the GUI)</dd>
 *
 * <dt><b>-print</b></dt>
 * <dd>Prints out book</dd>
 *
 * <dt><b>-printAs FILE</b></dt>
 * <dd>Prints out book to specific file</dd>
 *
 * <dt><b>-printDir DIR</b></dt>
 * <dd>Prints out book to specific folder (ignored if -printAs is used)</dd>
 *
 * <dt><b>-project FILE</b></dt>
 * <dd>Loads the provided project file</dd>
 *
 * <dt><b>-save</b></dt>
 * <dd>Saves project</dd>
 *
 * <dt><b>-saveAs FILE</b></dt>
 * <dd>Saves project to specific file</dd>
 *
 * <dt><b>-saveDir DIR</b></dt>
 * <dd>Saves project to specific folder (ignored if -saveAs is used)</dd>
 *
 * <dt><b>-script FILE</b></dt>
 * <dd>Runs the provided script file</dd>
 *
 * <dt><b>-sheets N...</b></dt>
 * <dd>Selects specific sheets (1-based)</dd>
 *
 * <dt><b>-step STEP</b></dt>
 * <dd>Defines a specific transcription step (to be performed on each input referenced from the
 * command line</dd>
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
    //-------------//
    // getCliTasks //
    //-------------//
    /**
     * Prepare the collection of CLI tasks (inputs, projects, scripts).
     *
     * @return the collection of tasks
     */
    public List<Callable<Void>> getCliTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Inputs
        for (Path input : params.inputFiles) {
            tasks.add(new InputTask(input));
        }

        // Projects
        for (Path project : params.projectFiles) {
            tasks.add(new ProjectTask(project));
        }

        // Scripts
        for (Path script : params.scriptFiles) {
            tasks.add(new ScriptTask(script));
        }

        return tasks;
    }

    //-------------//
    // getExportAs //
    //-------------//
    /**
     * Report the target export file if present on the CLI
     *
     * @return the CLI export file, or null
     */
    public Path getExportAs ()
    {
        return params.exportAs;
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
        return params.exportFolder;
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

    //------------//
    // getPrintAs //
    //------------//
    /**
     * Report the target print file if present on the CLI
     *
     * @return the CLI print file, or null
     */
    public Path getPrintAs ()
    {
        return params.printAs;
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
        return params.printFolder;
    }

    //-----------//
    // getSaveAs //
    //-----------//
    /**
     * Report the target save file if present on the CLI
     *
     * @return the CLI save file, or null
     */
    public Path getSaveAs ()
    {
        return params.saveAs;
    }

    //---------------//
    // getSaveFolder //
    //---------------//
    /**
     * Report the save folder if present on the CLI
     *
     * @return the CLI save path, or null
     */
    public Path getSaveFolder ()
    {
        return params.saveFolder;
    }

    //-------------//
    // isBatchMode //
    //-------------//
    /**
     * Report whether we are running in batch (that is with no UI).
     *
     * @return true for batch mode
     */
    public boolean isBatchMode ()
    {
        return params.batchMode;
    }

    //------------------//
    // printCommandLine //
    //------------------//
    /**
     * Print out the command line with its actual parameters.
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
     * Print out the general syntax for the command line.
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
        buf.append("\nSheet steps are in order:");

        for (Step step : Step.values()) {
            buf.append(String.format("%n   %-10s : %s", step.toString(), step.getDescription()));
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
        @Option(name = "-help", help = true, usage = "Displays general help then stops")
        boolean helpMode;

        /** Batch mode. */
        @Option(name = "-batch", usage = "Runs with no graphic user interface")
        boolean batchMode;

        /** Specific step. */
        @Option(name = "-step", usage = "Defines a specific processing step")
        Step step;

        /** The map of application options. */
        @Option(name = "-option", usage = "Defines an application constant", handler = PropertyOptionHandler.class)
        Properties options;

        /** The list of script files to execute. */
        @Option(name = "-script", usage = "Runs the provided script file", metaVar = "<script-file>")
        final List<Path> scriptFiles = new ArrayList<Path>();

        /** The list of input image file names to load. */
        @Option(name = "-input", usage = "Loads the provided input file", metaVar = "<input-file>")
        final List<Path> inputFiles = new ArrayList<Path>();

        /** The list of project file names to load. */
        @Option(name = "-project", usage = "Loads the provided project file", metaVar = "<project-file>")
        final List<Path> projectFiles = new ArrayList<Path>();

        /** The set of sheet IDs to load. */
        @Option(name = "-sheets", usage = "Selects specific sheets (1-based)", handler = IntArrayOptionHandler.class)
        private ArrayList<Integer> sheets;

        /** Should MusicXML data be produced?. */
        @Option(name = "-export", usage = "Exports MusicXML")
        boolean export;

        /** Full target file for MusicXML data. */
        @Option(name = "-exportAs", usage = "Exports MusicXML to specific file", metaVar = "<export-file>")
        Path exportAs;

        /** Target directory for MusicXML data. */
        @Option(name = "-exportDir", usage = "Exports MusicXML to specific folder"
                                             + " (ignored if -exportAs is used)", metaVar = "<export-folder>")
        Path exportFolder;

        /** Should book be printed?. */
        @Option(name = "-print", usage = "Prints out book")
        boolean print;

        /** Full target file for print. */
        @Option(name = "-printAs", usage = "Prints out book to specific file", metaVar = "<print-file>")
        Path printAs;

        /** Target directory for print. */
        @Option(name = "-printDir", usage = "Prints out book to specific folder"
                                            + " (ignored if -printAs is used)", metaVar = "<print-folder>")
        Path printFolder;

        /** Should book be saved?. */
        @Option(name = "-save", usage = "Saves project")
        boolean save;

        /** Full target file for save. */
        @Option(name = "-saveAs", usage = "Saves project to specific file", metaVar = "<project-file>")
        Path saveAs;

        /** Target directory for save. */
        @Option(name = "-saveDir", usage = "Saves project to specific folder"
                                           + " (ignored if -saveAs is used)", metaVar = "<project-folder>")
        Path saveFolder;

        //~ Constructors ---------------------------------------------------------------------------
        private Parameters ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // getSheetIds //
        //-------------//
        /**
         * Report the set of sheet IDs if present on the CLI.
         * Null means all sheets are taken.
         *
         * @return the CLI sheet IDs, perhaps null
         */
        public SortedSet<Integer> getSheetIds ()
        {
            if (sheets == null) {
                return null;
            }

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

    //---------//
    // CliTask //
    //---------//
    /**
     * Define a CLI task on a book (input, project or script).
     */
    private abstract static class CliTask
            implements Callable<Void>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Source file path. */
        public final Path path;

        //~ Constructors ---------------------------------------------------------------------------
        public CliTask (Path path)
        {
            this.path = path;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public Void call ()
                throws Exception
        {
            // Check source does exist
            if (!Files.exists(path)) {
                String msg = "Could not find file " + path;
                logger.warn(msg);
                throw new RuntimeException(msg);
            }

            // Obtain the book instance
            final Book book = loadBook(path);

            // Process the book instance
            processBook(book);

            return null;
        }

        /** Getting the book instance.
         *
         * @param path path to source
         * @return the loaded book
         */
        protected abstract Book loadBook (Path path);

        /** Processing the book instance.
         *
         * @param book the book to process
         */
        protected void processBook (Book book)
        {
            // Void by default
        }
    }

    //-----------//
    // InputTask //
    //-----------//
    /**
     * CLI task to process an input (image) file.
     */
    private class InputTask
            extends ProcessingTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public InputTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "Input " + path;
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.getEngine().loadInput(path);
        }
    }

    //----------------//
    // ProcessingTask //
    //----------------//
    /**
     * Processing common to both input (images) and projects.
     */
    private abstract class ProcessingTask
            extends CliTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ProcessingTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void processBook (Book book)
        {
            try {
                // Specific sheets to process?
                final SortedSet<Integer> sheetIds = params.getSheetIds();

                // Make sure stubs are available
                if (book.getStubs().isEmpty()) {
                    book.createStubs(sheetIds);
                }

                // Specific step to reach?
                final Step targetStep = params.step;

                if (targetStep != null) {
                    logger.info(
                            "Launching {} on {} {}",
                            targetStep,
                            path,
                            (sheetIds != null) ? ("sheets " + sheetIds) : "");

                    for (SheetStub stub : book.getValidStubs()) {
                        if ((sheetIds == null) || sheetIds.contains(stub.getNumber())) {
                            stub.ensureStep(targetStep);
                        }
                    }
                }

                // Book print?
                if (params.print || (params.printAs != null) || (params.printFolder != null)) {
                    logger.debug("Print output");
                    new PrintTask(params.printAs, params.printFolder).core(
                            book.getFirstValidStub().getSheet());
                }

                // Book export?
                if (params.export || (params.exportAs != null) || (params.exportFolder != null)) {
                    logger.debug("Export output");
                    new ExportTask(params.exportAs, params.exportFolder).core(
                            book.getFirstValidStub().getSheet());
                }

                // Book save?
                if (params.save || (params.saveAs != null) || (params.saveFolder != null)) {
                    logger.debug("Save book");
                    new SaveTask(params.saveAs, params.saveFolder).core(
                            book.getFirstValidStub().getSheet());
                }
            } catch (ProcessingCancellationException pce) {
                logger.warn("Cancelled " + book, pce);
                throw pce;
            } catch (Throwable ex) {
                logger.warn("Exception occurred " + ex, ex);
                throw ex;
            } finally {
                // Close (when in batch mode only)
                if (OMR.getGui() == null) {
                    book.close();
                }
            }
        }
    }

    //-------------//
    // ProjectTask //
    //-------------//
    /**
     * CLI task to process a project file.
     */
    private class ProjectTask
            extends ProcessingTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ProjectTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "Project " + path;
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.getEngine().loadProject(path);
        }
    }

    //------------//
    // ScriptTask //
    //------------//
    /**
     * Processing a script file.
     */
    private static class ScriptTask
            extends CliTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ScriptTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "Script " + path;
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.getEngine().loadScript(path);
        }
    }
}
//
//        /** Should bench data be produced?. */
//        @Option(name = "-bench", usage = "Outputs bench data")
//        boolean bench;
//
//        /** Target directory for bench data. */
//        @Option(name = "-benchDir", usage = "Outputs bench data to specific folder", metaVar = "<bench-folder>")
//        Path benchFolder;
/*
 *
 * <dt><b>-bench</b></dt>
 * <dd>Outputs bench data</dd>
 *
 * <dt><b>-benchDir DIR</b></dt>
 * <dd>Outputs bench data to specific folder</dd>
 */

//    //----------------//
//    // getBenchFolder //
//    //----------------//
//    /**
//     * Report the bench folder if present on the CLI
//     *
//     * @return the CLI bench folder, or null
//     */
//    public Path getBenchFolder ()
//    {
//        if (params.benchFolder == null) {
//            return null;
//        }
//
//        return params.benchFolder.toPath();
//    }
//
