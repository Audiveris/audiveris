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

import omr.step.Step;
import omr.step.Steps;

import omr.util.Dumping;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code CLI} parses and holds the parameters of the command line interface.
 *
 * <p>
 * The command line parameters can be (order and case are not relevant):
 * <dl>
 *
 * <dt> <b>-help</b> </dt> <dd> to print a quick usage help and leave the
 * application. </dd>
 *
 * <dt> <b>-batch</b> </dt> <dd> to run in batch mode, with no user
 * interface. </dd>
 *
 * <dt> <b>-steps (STEPNAME | &#64;STEPLIST)+</b> </dt> <dd> to run all the
 * specified steps (including the steps which are mandatory to get to the
 * specified ones). 'STEPNAME' can be any one of the step names (the case is
 * irrelevant) as defined in the {@link Steps} class. These steps will
 * be performed on each sheet referenced from the command line.</dd>
 *
 * <dt> <b>-option (KEY=VALUE | &#64;OPTIONLIST)+</b> </dt> <dd> to specify
 * the value of some application parameters (that can also be set via the
 * pull-down menu "Tools|Options"), either by stating the key=value pair or by
 * referencing (flagged by a &#64; sign) a file that lists key=value pairs (or
 * even other files list recursively).
 * A list file is a simple text file, with one key=value pair per line.
 * <b>Nota</b>: The syntax used is the Properties syntax, so for example
 * back-slashes must be escaped.</dd>
 *
 * <dt> <b>-script (SCRIPTNAME | &#64;SCRIPTLIST)+</b> </dt> <dd> to specify
 * some scripts to be read, using the same mechanism than input command belows.
 * These script files contain actions generally recorded during a previous run.
 * </dd>
 *
 * <dt> <b>-input (FILENAME | &#64;FILELIST)+</b> </dt> <dd> to specify some
 * image files to be read, either by naming the image file or by referencing
 * (flagged by a &#64; sign) a file that lists image files (or even other files
 * list recursively).
 * A list file is a simple text file, with one image file name per line.</dd>
 *
 * <dt> <b>-pages (PAGE | &#64;PAGELIST)+</b> </dt> <dd> to specify some
 * specific pages, counted from 1, to be loaded out of the input file.</dd>
 *
 * <dt> <b>-bench (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to bench data file (or directory).
 * <b>Nota</b>: If the path refers to an existing directory, each processed
 * score will output its bench data to a score-specific file created in the
 * provided directory. Otherwise, all bench data, whatever its related score,
 * will be written to the provided single file.</dd>
 *
 * <dt> <b>-print (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to PDF file (or directory). Same note as for -bench.</dd>
 *
 * <dt> <b>-export (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to MusicXML file (or directory). Same note as for -bench.</dd>
 *
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

        // Print all mandatory steps
        buf.append("\nMandatory steps are in order (non case-sensitive):");

        for (Step step : Steps.values()) {
            if (step.isMandatory()) {
                buf.append(
                        String.format("%n   %-16s : %s", step.toString(), step.getDescription()));
            }
        }

        // Print all optional steps
        buf.append("\n\nOptional steps are (non case-sensitive):");

        for (Step step : Steps.values()) {
            if (!step.isMandatory()) {
                buf.append(
                        String.format("%n   %-16s : %s", step.toString(), step.getDescription()));
            }
        }

        buf.append("\n");
        logger.info(buf.toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------------//
    // IntArrayOptionHandler //
    //-----------------------//
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

        /** List of specific steps. */
        @Option(name = "-steps", usage = "Defines specific transcription steps", handler = StepArrayOptionHandler.class)
        private final List<Step> steps = new ArrayList<Step>();

        /** The map of application options. */
        @Option(name = "-option", usage = "Defines an application constant", handler = PropertyOptionHandler.class)
        Properties options;

        /** The list of script files to execute. */
        @Option(name = "-script", usage = "Runs the provided script file", metaVar = "<script-file>")
        final List<File> scriptFiles = new ArrayList<File>();

        /** The list of input image file names to load. */
        @Option(name = "-input", usage = "Reads the provided input image file", metaVar = "<input-file>")
        final List<File> inputFiles = new ArrayList<File>();

        /** The set of page IDs to load. */
        @Option(name = "-pages", usage = "Defines specific pages (counted from 1) to process", handler = IntArrayOptionHandler.class)
        private final List<Integer> pages = new ArrayList<Integer>();

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
        // getPagesIds //
        //-------------//
        /**
         * Report the set of page ids if present on the CLI
         *
         * @return the CLI page ids, perhaps empty but not null
         */
        public SortedSet<Integer> getPageIds ()
        {
            return new TreeSet(pages);
        }

        //----------//
        // getSteps //
        //----------//
        /**
         * Report the set of steps
         *
         * @return the set of steps, perhaps empty but not null
         */
        public Set<Step> getSteps ()
        {
            Set<Step> allSteps = new LinkedHashSet<Step>(steps);

            //
            //            if ((exportFolder != null) || (printFolder != null)) {
            //                allSteps.add(Steps.valueOf(Steps.PAGE));
            //            }
            //
            return allSteps;
        }
    }

    //-----------------------/:
    // PropertyOptionHandler //
    //-----------------------/:
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

    //------------------------//
    // StepArrayOptionHandler //
    //------------------------//
    public static class StepArrayOptionHandler
            extends OptionHandler<Step>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public StepArrayOptionHandler (CmdLineParser parser,
                                       OptionDef option,
                                       Setter<Step> setter)
        {
            super(parser, option, setter);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getDefaultMetaVariable ()
        {
            return "Step[]";
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
                    String s = p.replaceAll("-", "_");
                    Step value = Steps.valueOf(s.toUpperCase());

                    if (value == null) {
                        throw new CmdLineException(
                                owner,
                                Messages.ILLEGAL_OPERAND,
                                params.getParameter(-1),
                                s);
                    }

                    setter.addValue(value);
                }
            }

            return counter;
        }
    }
}
