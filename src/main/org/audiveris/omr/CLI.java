//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             C L I                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.FileUtil;

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

    /** Sequence of (trimmed) arguments for this run. */
    private String[] trimmedArgs;

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
     * Prepare the collection of CLI tasks (inputs, books, samples).
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

                if (str.endsWith(OMR.BOOK_EXTENSION)) {
                    tasks.add(new BookTask(path));
                } else if (str.endsWith("-" + SampleRepository.SAMPLES_FILE_NAME)) {
                    tasks.add(new SamplesTask(path));
                } else {
                    // Everything else is considered as an image input file
                    tasks.add(new InputTask(path));
                }
            }
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

    //-----------------//
    // getOutputFolder //
    //-----------------//
    /**
     * Report the output folder if present on the CLI
     *
     * @return the CLI output path, or null
     */
    public Path getOutputFolder ()
    {
        return params.outputFolder;
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

    //------------//
    // isHelpMode //
    //------------//
    /**
     * Report whether we are running in help mode (to stop immediately).
     *
     * @return true for help mode
     */
    public boolean isHelpMode ()
    {
        return params.helpMode;
    }

    //--------//
    // isSave //
    //--------//
    /**
     * Report whether we save every step result.
     *
     * @return true for saving every step
     */
    public boolean isSave ()
    {
        return params.save;
    }

    //-----------------//
    // parseParameters //
    //-----------------//
    /**
     * Parse the CLI arguments and return the populated parameters structure.
     *
     * @param args the CLI arguments
     * @return the parsed parameters, or null if failed
     * @throws org.kohsuke.args4j.CmdLineException if error found in arguments
     */
    public Parameters parseParameters (final String... args)
            throws CmdLineException
    {
        logger.info("CLI args: {}", Arrays.toString(args));

        // Bug fix if an arg is made of spaces
        trimmedArgs = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            trimmedArgs[i] = args[i].trim();
        }

        parser.parseArgument(trimmedArgs);

        if (logger.isDebugEnabled()) {
            new Dumping().dump(params);
        }

        checkParams();

        return params;
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

        sb.append(Arrays.toString(trimmedArgs));
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
        buf.append("\n    audiveris [OPTIONS] [--] [INPUT_FILES]\n");

        buf.append("\n@file:");
        buf.append("\n    Content of file to be extended in line");
        buf.append("\n");

        buf.append("\nOptions:\n");

        StringWriter writer = new StringWriter();
        parser.printUsage(writer, null);
        buf.append(writer.toString());

        buf.append("\nInput file extensions:");
        buf.append("\n    .omr        : book file  (input/output)");
        buf.append("\n    [any other] : image file (input)");
        buf.append("\n");

        // Print all steps
        buf.append("\nSheet steps are in order:");

        for (Step step : Step.values()) {
            buf.append(String.format("%n    %-10s : %s", step.toString(), step.getDescription()));
        }

        buf.append("\n");
        logger.info(buf.toString());
    }

    //-------------//
    // checkParams //
    //-------------//
    private void checkParams ()
            throws CmdLineException
    {
        if (params.transcribe) {
            if ((params.step != null) && (params.step != Step.last())) {
                String msg = "'-transcribe' option not compatible with '-step " + params.step
                             + "' option";
                throw new CmdLineException(parser, msg);
            }

            params.step = Step.last();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // CliTask //
    //---------//
    /**
     * Define a CLI task on a book (input or book).
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
                String msg = "Could not find file \"" + path + "\"";
                logger.warn(msg);
                throw new RuntimeException(msg);
            }

            // Obtain the book instance
            final Book book = loadBook(path);

            if (book != null) {
                processBook(book); // Process the book instance
            }

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
            return "<qualified-class-name>";
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
     * Option handler for an array of positive integers.
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

                int minusPos = param.indexOf('-');

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

        /** Should symbols annotations be produced?. */
        @Option(name = "-annotate", usage = "(advanced) Annotate book symbols")
        boolean annotate;

        /** Batch mode. */
        @Option(name = "-batch", usage = "Run with no graphic user interface")
        boolean batchMode;

        /** Should MusicXML data be produced?. */
        @Option(name = "-export", usage = "Export MusicXML")
        boolean export;

        /** Force step re-processing. */
        @Option(name = "-force", usage = "Force step/transcribe re-processing")
        boolean force;

        /** Help mode. */
        @Option(name = "-help", help = true, usage = "Display general help then stop")
        boolean helpMode;

        /** The map of application options. */
        @Option(name = "-option", usage = "Define an application constant", handler = PropertyOptionHandler.class)
        Properties options;

        /** Output directory. */
        @Option(name = "-output", usage = "Define base output folder", metaVar = "<output-folder>")
        Path outputFolder;

        /** Should book be printed?. */
        @Option(name = "-print", usage = "Print out book")
        boolean print;

        /** Ability to run a class on each valid sheet. */
        @Option(name = "-run", usage = "(advanced) Run provided class on valid sheets", handler = ClassOptionHandler.class)
        Class runClass;

        /** Should samples be produced?. */
        @Option(name = "-sample", usage = "(advanced) Sample all book symbols")
        boolean sample;

        /** Should book be saved on every successful batch step?. */
        @Option(name = "-save", usage = "Save book on every successful batch step")
        boolean save;

        /** The set of sheet IDs to load. */
        @Option(name = "-sheets", usage = "Select specific sheets numbers and ranges (like 2-5)", handler = IntArrayOptionHandler.class)
        private ArrayList<Integer> sheets;

        /** Specific step. */
        @Option(name = "-step", usage = "Define a specific target step")
        Step step;

        /** Should book be transcribed?. */
        @Option(name = "-transcribe", usage = "Transcribe whole book")
        boolean transcribe;

        /** Should a sheet mix be exported after transcription? */
        @Option(name = "-export-sheet-mix", usage = "Export sheet mix after transcription")
        boolean export_sheet_mix;

        /** Optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)
        /** Final arguments. */
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

            return new TreeSet<Integer>(sheets);
        }
    }

    //-----------------------//
    // PropertyOptionHandler //
    //-----------------------//
    /**
     * Option handler for a property definition.
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
            return "Book \"" + path + "\"";
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
            return "Input \"" + path + "\"";
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
            final Path folder = BookManager.getDefaultBookFolder(book);
            boolean cancelled = false;

            try {
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }

                // In batch, log into specific log file within book folder
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
                        book.store(BookManager.getDefaultSavePath(book), false);
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

                if (params.transcribe) {
                    book.reduceScores();
                }

                if (params.export_sheet_mix) {
                    // TODO: Make this cleaner
                    for (SheetStub stub : book.getStubs()) {
                        final Path defaultBookPath = BookManager.getDefaultPrintPath(book);
                        final Path bookSansExt = FileUtil.avoidExtensions(defaultBookPath, OMR.PRINT_EXTENSION);
                        final String sheetSuffix = book.isMultiSheet() ? (OMR.SHEET_SUFFIX + stub.getNumber()) : "";
                        final Path defaultSheetPath = Paths.get(bookSansExt + sheetSuffix + ".png");
                        stub.getSheet().printMix(defaultSheetPath);
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

                // Book export?
                if (params.export) {
                    logger.debug("Export book");
                    book.export();
                }

                // Book sample?
                if (params.sample) {
                    logger.debug("Sample book");
                    book.sample();
                }

                // Book annotate?
                if (params.annotate) {
                    logger.debug("Annotate book");
                    book.annotate();
                }

                // Book print?
                if (params.print) {
                    logger.debug("Print book");
                    book.print();
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
                        book.store(BookManager.getDefaultSavePath(book), true);
                    } else {
                        book.store(BookManager.getDefaultSavePath(book), false);
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

    //-------------//
    // SamplesTask //
    //-------------//
    /**
     * Processing a samples file.
     */
    private static class SamplesTask
            extends CliTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SamplesTask (Path path)
        {
            super(path);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "Samples \"" + path + "\"";
        }

        @Override
        protected Book loadBook (Path path)
        {
            SampleRepository global = SampleRepository.getGlobalInstance();
            global.includeSamplesFile(path);

            return null;
        }
    }
}
