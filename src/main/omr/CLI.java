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

import omr.log.LogUtil;

import omr.script.ExportTask;
import omr.script.PrintTask;
import omr.script.SaveTask;

import omr.sheet.Book;
import omr.sheet.BookManager;

import omr.step.ProcessingCancellationException;
import omr.step.RunClass;
import omr.step.Step;

import omr.util.Dumping;
import omr.util.FileUtil;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.spi.StopOptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Any item starting with the '&#64;' character is interpreted as referring to a file, whose content
 * is expanded in line.
 * <p>
 * NOTA: each line of such referred file is taken as a whole and interpreted as a single item,
 * hence please make sure to put only one item per line.
 * Note also that a blank line is interpreted as an empty ("") item.
 * <p>
 * The command line options can be (order is not relevant):
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
 * <dt><b>-run className</b></dt>
 * <dd>Specifies a class to run on each valid sheet</dd>
 *
 * <dt><b>-save</b></dt>
 * <dd>Saves book</dd>
 *
 * <dt><b>-saveAs FILE</b></dt>
 * <dd>Saves book to specific file</dd>
 *
 * <dt><b>-saveDir DIR</b></dt>
 * <dd>Saves book to specific folder (ignored if -saveAs is used)</dd>
 *
 * <dt><b>-sheets N...</b></dt>
 * <dd>Selects specific sheets numbers and ranges (like 1-9)</dd>
 *
 * <dt><b>-step STEP</b></dt>
 * <dd>Defines a specific transcription step (to be performed on each input referenced from the
 * command line)</dd>
 *
 * <dt><b>--</b></dt>
 * <dd>This optional item marks the end of options and indicates that all following items are
 * plain file arguments.</dd>
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
     * Prepare the collection of CLI tasks (inputs, books, scripts).
     *
     * @return the collection of tasks
     */
    public List<CliTask> getCliTasks ()
    {
        List<CliTask> tasks = new ArrayList<CliTask>();

        // Task kind is fully determined by argument extension
        for (Path argument : params.arguments) {
            String str = argument.toString().trim().replace('\\', '/');

            if (!str.isEmpty()) {
                final Path path = Paths.get(str);

                if (str.endsWith(OMR.SCRIPT_EXTENSION)) {
                    tasks.add(new ScriptTask(path));
                } else if (str.endsWith(OMR.BOOK_EXTENSION)) {
                    tasks.add(new BookTask(path));
                } else {
                    tasks.add(new InputTask(path));
                }
            }
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
        logger.info("CLI args: {}", Arrays.toString(args));
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

        // Print syntax
        buf.append("\n");
        buf.append("\nSyntax:");
        buf.append("\n   audiveris [OPTIONS] [INPUT_FILES]\n");

        buf.append("\nOptions:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        buf.append("\nInput file extensions:");
        buf.append("\n   .omr        : book file");
        buf.append("\n   .script.xml : script file");
        buf.append("\n   [any other] : image file");
        buf.append("\n");

        // Print all steps
        buf.append("\nSheet steps are in order:");

        for (Step step : Step.values()) {
            buf.append(String.format("%n   %-10s : %s", step.toString(), step.getDescription()));
        }

        buf.append("\n");
        logger.info(buf.toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // CliTask //
    //---------//
    /**
     * Define a CLI task on a book (input, book or script).
     */
    public abstract static class CliTask
            implements Callable<Void>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Source file path. */
        public final Path path;

        /** Radix. */
        private final String radix;

        //~ Constructors ---------------------------------------------------------------------------
        public CliTask (Path path)
        {
            this.path = path;

            String nameSansExt = FileUtil.getNameSansExtension(path);
            String alias = BookManager.getInstance().getAlias(nameSansExt);
            radix = (alias != null) ? alias : nameSansExt;
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

        /**
         * @return the radix
         */
        public String getRadix ()
        {
            return radix;
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

    //--------------------//
    // ClassOptionHandler //
    //--------------------//
    /**
     * Argument handler for a class name.
     */
    public static class ClassOptionHandler
            extends OptionHandler<Properties>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ClassOptionHandler (CmdLineParser parser,
                                   OptionDef option,
                                   Setter<? super Properties> setter)
        {
            super(parser, option, setter);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getDefaultMetaVariable ()
        {
            return "qualified class name";
        }

        @Override
        public int parseArguments (org.kohsuke.args4j.spi.Parameters params)
                throws CmdLineException
        {
            String className = params.getParameter(0).trim();

            if (!className.isEmpty()) {
                try {
                    Class runClass = Class.forName(className);
                    FieldSetter fs = setter.asFieldSetter();
                    fs.addValue(runClass);
                } catch (Throwable ex) {
                    throw new CmdLineException(owner, ex);
                }
            }

            return 1;
        }
    }

    //-----------------------//
    // IntArrayOptionHandler //
    //-----------------------//
    /**
     * Argument handler for an array of positive integers.
     * <p>
     * It also accepts a range of integers, such as: 3-10 to mean: 3 4 5 6 7 8 9 10.
     * Restriction: the range cannot contain space if not quoted:
     * 3-10 is OK, 3 - 10 is not, though "3 - 10" is OK.
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

                int minusPos = param.indexOf("-");

                if (minusPos != -1) {
                    // " a - b " or a-b
                    String str1 = param.substring(0, minusPos).trim();
                    String str2 = param.substring(minusPos + 1).trim();
                    int i1 = Integer.parseInt(str1);
                    int i2 = Integer.parseInt(str2);

                    for (int i = i1; i <= i2; i++) {
                        setter.addValue(i);
                    }
                } else {
                    for (String p : param.split(" ")) {
                        if (!p.isEmpty()) {
                            setter.addValue(Integer.parseInt(p));
                        }
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

        /** Force step re-processing. */
        @Option(name = "-force", usage = "Force step reprocessing")
        boolean force;

        @Option(name = "-run", usage = "Class to run on valid sheets", handler = ClassOptionHandler.class)
        Class runClass;

        /** The map of application options. */
        @Option(name = "-option", usage = "Defines an application constant", handler = PropertyOptionHandler.class)
        Properties options;

        /** The set of sheet IDs to load. */
        @Option(name = "-sheets", usage = "Selects specific sheets numbers and ranges (like 1-9)", handler = IntArrayOptionHandler.class)
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
        @Option(name = "-save", usage = "Saves book")
        boolean save;

        /** Full target file for save. */
        @Option(name = "-saveAs", usage = "Saves book to specific file", metaVar = "<book-file>")
        Path saveAs;

        /** Target directory for save. */
        @Option(name = "-saveDir", usage = "Saves book to specific folder"
                                           + " (ignored if -saveAs is used)", metaVar = "<book-folder>")
        Path saveFolder;

        /** Final arguments, with optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)
        List<Path> arguments = new ArrayList<Path>();

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

    //----------//
    // BookTask //
    //----------//
    /**
     * CLI task to process a book file.
     */
    private class BookTask
            extends ProcessingTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public BookTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "Book " + path;
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.engine.loadBook(path);
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
            return OMR.engine.loadInput(path);
        }
    }

    //----------------//
    // ProcessingTask //
    //----------------//
    /**
     * Processing common to both input (images) and books.
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
            final Path folder = BookManager.getDefaultBookPath(book).getParent();
            boolean cancelled = false;

            try {
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }

                if (OMR.gui == null) {
                    LogUtil.addAppender(book.getRadix(), folder);
                }

                LogUtil.start(book);

                // Specific sheets to process?
                final SortedSet<Integer> sheetIds = params.getSheetIds();

                // Make sure stubs are available
                if (book.getStubs().isEmpty()) {
                    book.createStubs(sheetIds);

                    // Save book to disk (global book info)
                    if (OMR.gui == null) {
                        book.store(BookManager.getDefaultBookPath(book), false);
                    }
                }

                if (OMR.gui != null) {
                    Integer focus = (sheetIds != null) ? sheetIds.first() : null;
                    book.createStubsTabs(focus); // Tabs are now accessible
                }

                // Specific step to reach on all sheets in the book?
                if (params.step != null) {
                    boolean ok = book.reachBookStep(params.step, params.force, sheetIds);

                    if (!ok) {
                        return;
                    }
                }

                // Specific class to run?
                if (params.runClass != null) {
                    try {
                        Constructor cons = params.runClass.getConstructor(
                                new Class[]{Book.class, SortedSet.class});
                        RunClass instance = (RunClass) cons.newInstance(book, sheetIds);
                        instance.process();
                    } catch (Throwable ex) {
                        logger.warn("Error running {} {}", params.runClass, ex.toString(), ex);
                    }
                }

                // Book print?
                if (params.print || (params.printAs != null) || (params.printFolder != null)) {
                    logger.debug("Print output");
                    new PrintTask(params.printAs, params.printFolder).core(book);
                }

                // Book export?
                if (params.export || (params.exportAs != null) || (params.exportFolder != null)) {
                    logger.debug("Export output");
                    new ExportTask(params.exportAs, params.exportFolder).core(book);
                }

                // Book save?
                if (params.save || (params.saveAs != null) || (params.saveFolder != null)) {
                    logger.debug("Save book");
                    new SaveTask(params.saveAs, params.saveFolder).core(book);
                }
            } catch (ProcessingCancellationException pce) {
                logger.warn("Cancelled " + book);
                cancelled = true;
                throw pce;
            } catch (Throwable ex) {
                logger.warn("Exception occurred " + ex, ex);
                throw new RuntimeException(ex);
            } finally {
                // Close (when in batch mode only)
                if (OMR.gui == null) {
                    if (cancelled) {
                        // Make a backup if needed, then save book "in its current status"
                        book.store(BookManager.getDefaultBookPath(book), true);
                    } else {
                        book.store(BookManager.getDefaultBookPath(book), false);
                    }

                    book.close();
                }

                LogUtil.stopBook();

                if (OMR.gui == null) {
                    LogUtil.removeAppender(book.getRadix());
                }
            }
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
            return OMR.engine.loadScript(path);
        }
    }
}
