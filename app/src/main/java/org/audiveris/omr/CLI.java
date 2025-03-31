//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             C L I                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.PlayList;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.RunClass;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.NaturalSpec;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.spi.StopOptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Class <code>CLI</code> parses and holds the parameters of the command line interface.
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
    private final CmdLineParser parser;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new CLI object.
     *
     * @param toolName the program name
     */
    public CLI (final String toolName)
    {
        this.toolName = toolName;

        final Comparator<OptionHandler> noSorter = (OptionHandler o1,
                                                    OptionHandler o2) -> 0;

        final ParserProperties props = ParserProperties.defaults().withAtSyntax(true)
                .withUsageWidth(100).withShowDefaults(false).withOptionSorter(noSorter);

        parser = new CmdLineParser(params, props);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // checkParams //
    //-------------//
    private void checkParams ()
        throws CmdLineException
    {
        if (params.transcribe) {
            if ((params.step != null) && (params.step != OmrStep.last())) {
                String msg = "'-transcribe' option not compatible with '-step " + params.step
                        + "' option";
                throw new CmdLineException(parser, new Throwable(msg));
            }

            params.step = OmrStep.last();
        }
    }

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
        List<CliTask> tasks = new ArrayList<>();

        // PlayList?
        if (params.playListPath != null) {
            tasks.add(new PlayListTask(params.playListPath));
        }

        // Task kind is fully determined by argument extension
        params.arguments.stream().map(argument -> argument.toString().trim().replace('\\', '/'))
                .filter(str -> (!str.isEmpty())).forEachOrdered(str -> {
                    final Path path = Paths.get(str);

                    if (str.endsWith(OMR.BOOK_EXTENSION)) {
                        tasks.add(new BookTask(path));
                    } else if (str.endsWith("-" + SampleRepository.SAMPLES_FILE_NAME)) {
                        tasks.add(new SamplesTask(path));
                    } else {
                        // Everything else is considered as an image input file
                        tasks.add(new InputTask(path));
                    }
                });

        return tasks;
    }

    //--------------//
    // getConstants //
    //--------------//
    /**
     * Report the properties set at the CLI level
     *
     * @return the CLI-defined constant values
     */
    public Properties getConstants ()
    {
        return params.constants;
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

    //-----------------//
    // getPlayListPath //
    //-----------------//
    /**
     * Report the path to playlist if present on CLI
     *
     * @return the PlayList path, or null
     */
    public Path getPlayListPath ()
    {
        return params.playListPath;
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

    //--------//
    // isSwap //
    //--------//
    /**
     * Report whether we swap every sheet after processing.
     *
     * @return true for swapping every sheet
     */
    public boolean isSwap ()
    {
        return params.swap;
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
        // And deprecated "-option" must be replaced by "-constant"
        trimmedArgs = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();

            if (arg.equals("-option")) {
                arg = "-constant";
            }

            trimmedArgs[i] = arg;
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

        for (OmrStep step : OmrStep.values()) {
            buf.append(String.format("%n    %-10s : %s", step.toString(), step.getDescription()));
        }

        buf.append("\n");
        logger.info(buf.toString());
    }

    //---------------------//
    // nullifyOutputFolder //
    //---------------------//
    /**
     * Nullify the output folder on the CLI.
     */
    public void nullifyOutputFolder ()
    {
        params.outputFolder = null;
    }

    //~ Inner classes ------------------------------------------------------------------------------

    //----------//
    // BookTask //
    //----------//
    /**
     * CLI task to process a book file.
     */
    private class BookTask
            extends ProcessingTask
    {
        BookTask (Path path)
        {
            super(path);
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.engine.loadBook(path);
        }

        @Override
        public String toString ()
        {
            return "Book \"" + path + "\"";
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
        /**
         * Create a ClassOptionHandler object.
         *
         * @param parser Command line argument owner
         * @param option Run-time copy of the Option or Argument annotation
         * @param setter Setter interface
         */
        public ClassOptionHandler (CmdLineParser parser,
                                   OptionDef option,
                                   Setter<? super Properties> setter)
        {
            super(parser, option, setter);
        }

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
                } catch (ClassNotFoundException ex) {
                    throw new CmdLineException(owner, ex);
                }
            }

            return 1;
        }
    }

    //---------//
    // CliTask //
    //---------//
    /**
     * Define a CLI task on a book (input or book).
     */
    public abstract static class CliTask
            implements Callable<Void>
    {
        /** Source file path. */
        public final Path path;

        /** Radix. */
        private final String radix;

        /**
         * Create a CliTask object.
         *
         * @param path the path to book file
         */
        public CliTask (Path path)
        {
            this.path = path.toAbsolutePath();

            String nameSansExt = FileUtil.getNameSansExtension(path);
            String alias = BookManager.getInstance().getAlias(nameSansExt);
            radix = (alias != null) ? alias : nameSansExt;
        }

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

        /**
         * Getting the book instance.
         *
         * @param path path to source
         * @return the loaded book
         */
        protected abstract Book loadBook (Path path);

        /**
         * Processing the book instance.
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
        InputTask (Path path)
        {
            super(path);
        }

        @Override
        protected Book loadBook (Path path)
        {
            return OMR.engine.loadInput(path);
        }

        @Override
        protected void openBookDialog (SheetStub stub)
        {
            if ((OMR.gui != null) && BookActions.preOpenBookParameters()) {
                BookActions.applyUserSettings(stub);
            }
        }

        @Override
        public String toString ()
        {
            return "Input \"" + path + "\"";
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
        /**
         * Create an IntArrayOptionHandler object.
         *
         * @param parser Command line argument owner
         * @param option Run-time copy of the Option or Argument annotation
         * @param setter Setter interface
         */
        public IntArrayOptionHandler (CmdLineParser parser,
                                      OptionDef option,
                                      Setter<Integer> setter)
        {
            super(parser, option, setter);
        }

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
                final String param = params.getParameter(counter);

                if (param.startsWith("-")) {
                    break;
                }

                for (int i : NaturalSpec.decode(param, true)) {
                    setter.addValue(i);
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
        /** Help mode. */
        @Option(name = "-help", help = true, usage = "Display general help then stop")
        boolean helpMode;

        /** Batch mode. */
        @Option(name = "-batch", usage = "Run with no graphic user interface")
        boolean batchMode;

        /** The set of sheet IDs to load. */
        @Option(name = "-sheets", usage = "Select sheet numbers and ranges (1 4-5)", handler = IntArrayOptionHandler.class)
        private ArrayList<Integer> sheets;

        /** Should book be transcribed?. */
        @Option(name = "-transcribe", usage = "Transcribe whole book")
        boolean transcribe;

        /** Specific step. */
        @Option(name = "-step", usage = "Define a specific target step")
        OmrStep step;

        /** Force step re-processing. */
        @Option(name = "-force", usage = "Force step/transcribe re-processing")
        boolean force;

        /** Output directory. */
        @Option(name = "-output", usage = "Define base output folder", metaVar = "<output-folder>")
        Path outputFolder;

        /** PlayList path. */
        @Option(name = "-playlist", usage = "Build a compound book from playlist", metaVar = "<file.xml>")
        Path playListPath;

        /** Should MusicXML data be produced?. */
        @Option(name = "-export", usage = "Export MusicXML")
        boolean export;

        /** Should book be printed?. */
        @Option(name = "-print", usage = "Print out book")
        boolean print;

        /** The map of application options. */
        @Option(name = "-constant", usage = "Define an application constant", handler = PropertyOptionHandler.class)
        Properties constants;

        /** Should book file be upgraded?. */
        @Option(name = "-upgrade", usage = "Upgrade whole book file")
        boolean upgrade;

        /** Should book be saved on every successful batch step?. */
        @Option(name = "-save", usage = "In batch, save book on every successful step")
        boolean save;

        /** Should every sheet be swapped after processing?. */
        @Option(name = "-swap", usage = "Swap out every sheet after its processing")
        boolean swap;

        /** Ability to run a class on each valid sheet. */
        @Option(name = "-run", usage = "(advanced) Run provided class on valid sheets", handler = ClassOptionHandler.class)
        Class runClass;

        /** Should samples be produced?. */
        @Option(name = "-sample", usage = "(advanced) Sample all book symbols")
        boolean sample;

        /** Should symbols annotations be produced?. */
        @Option(name = "-annotate", usage = "(advanced) Annotate book symbols")
        boolean annotate;

        /** Optional "--" separator. */
        @Argument
        @Option(name = "--", handler = StopOptionHandler.class)
        /** Final arguments. */
        List<Path> arguments = new ArrayList<>();

        private Parameters ()
        {
        }

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

            return new TreeSet<>(sheets);
        }
    }

    //--------------//
    // PlayListTask //
    //--------------//
    /**
     * Processing a playlist file.
     */
    private static class PlayListTask
            extends CliTask
    {
        public PlayListTask (Path path)
        {
            super(path);
        }

        @Override
        public Void call ()
            throws Exception
        {
            // To check that playlist file does exist
            super.call();

            if (OMR.gui != null) {
                // Just display S&M dialog populated by the playlist
                BookActions.getInstance().splitAndMerge(path);
            } else {
                // Build the compound book according to the playlist
                final PlayList playList = PlayList.load(path);

                if (playList != null) {
                    playList.injectBooks(); // Ensure books are loaded

                    final String compoundName = FileUtil.getNameSansExtension(path)
                            + OMR.BOOK_EXTENSION;
                    final Path targetPath = path.getParent().resolve(compoundName);
                    playList.buildCompound(targetPath);
                }
            }

            return null;
        }

        @Override
        protected Book loadBook (Path path)
        {
            return null;
        }

        @Override
        public String toString ()
        {
            return "PlayList \"" + path + "\"";
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
        ProcessingTask (Path path)
        {
            super(path);
        }

        /**
         * Opening book parameters dialog.
         *
         * @param stub the stub with focus
         */
        protected void openBookDialog (SheetStub stub)
        {
            // Void by default
        }

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

                boolean swap = (OMR.gui == null) || isSwap() || BookActions.swapProcessedSheets();

                // Make sure stubs are available
                if (book.getStubs().isEmpty()) {
                    book.createStubs();

                    // Save book to disk immediately (global book info)
                    final Path bookPath = BookManager.getDefaultSavePath(book);

                    if (OMR.gui == null || (swap && BookActions.isTargetConfirmed(bookPath))) {
                        book.store(bookPath, false);
                    } else {
                        swap = false;
                    }
                }

                // Specific sheets to process?
                final SortedSet<Integer> sheetIds = params.getSheetIds();
                final List<SheetStub> validStubs = Book.getValidStubs(book.getStubs(sheetIds));
                List<Score> scores = book.getScores();

                if (OMR.gui != null) {
                    Integer focus = (sheetIds != null) ? sheetIds.first() : null;
                    StubsController.getInstance().displayStubs(book, focus);
                    openBookDialog(
                            (focus != null) ? book.getStub(focus) : book.getFirstValidStub());
                } else {
                    // Batch: Perform sheets upgrade?
                    if (Book.batchUpgradeBooks() || params.upgrade) {
                        book.upgradeStubs();
                    }

                    if (sheetIds != null) {
                        // Use a temporary array of scores
                        scores = new ArrayList<>();
                    }
                }

                // Specific step to reach on valid selected sheets in the book?
                if (params.step != null) {
                    boolean ok = book.reachBookStep(params.step, params.force, validStubs, swap);

                    if (!ok) {
                        return;
                    }
                }

                // Score(s)
                if (params.transcribe) {
                    book.transcribe(validStubs, scores, swap);
                }

                // Specific class to run?
                if (params.runClass != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Constructor cons = params.runClass.getConstructor(
                                new Class[] { Book.class, SortedSet.class });
                        RunClass instance = (RunClass) cons.newInstance(book, sheetIds);
                        instance.process();
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InstantiationException | NoSuchMethodException | SecurityException
                            | InvocationTargetException ex) {
                        logger.warn("Error running {} {}", params.runClass, ex.toString(), ex);
                    }
                }

                // Book export?
                if (params.export) {
                    logger.debug("Export book");
                    book.export(validStubs, scores);
                }

                // Book sample?
                if (params.sample) {
                    logger.debug("Sample book");
                    book.sample(validStubs);
                }

                // Book annotate?
                if (params.annotate) {
                    logger.debug("Annotate book");
                    book.annotate(validStubs);
                }

                // Book print?
                if (params.print) {
                    logger.debug("Print book");
                    book.print(validStubs);
                }
            } catch (ProcessingCancellationException pce) {
                logger.warn("Cancelled " + book);
                cancelled = true;
                throw pce;
            } catch (IOException ex) {
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

                    book.close(null);
                }

                LogUtil.stopBook();

                if (OMR.gui == null) {
                    LogUtil.removeAppender(book.getRadix());
                }
            }
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
        /**
         * Create a PropertyOptionHandler.
         *
         * @param parser Command line argument owner
         * @param option Run-time copy of the Option or Argument annotation
         * @param setter Setter interface
         */
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
            } catch (IOException ex) {
                throw new CmdLineException(owner, "Error in " + name + " " + pair, ex);
            }

            return 1;
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
        SamplesTask (Path path)
        {
            super(path);
        }

        @Override
        protected Book loadBook (Path path)
        {
            SampleRepository global = SampleRepository.getGlobalInstance();
            global.includeSamplesFile(path);

            return null;
        }

        @Override
        public String toString ()
        {
            return "Samples \"" + path + "\"";
        }
    }
}
