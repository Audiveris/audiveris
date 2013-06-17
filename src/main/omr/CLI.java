//----------------------------------------------------------------------------//
//                                                                            //
//                                   C L I                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.step.Step;
import omr.step.Steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code CLI} parses and holds the parameters of the command
 * line interface.
 *
 * <p> The command line parameters can be (order and case are not relevant):
 * <dl>
 *
 * <dt> <b>-help</b> </dt> <dd> to print a quick usage help and leave the
 * application. </dd>
 *
 * <dt> <b>-batch</b> </dt> <dd> to run in batch mode, with no user
 * interface. </dd>
 *
 * <dt> <b>-step (STEPNAME | &#64;STEPLIST)+</b> </dt> <dd> to run all the
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
 * <strike>
 * <dt> <b>-midi (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to MIDI file (or directory). Same note as for -bench.</dd>
 * </strike>
 *
 * <dt> <b>-print (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to PDF file (or directory). Same note as for -bench.</dd>
 *
 * <dt> <b>-export (DIRNAME | FILENAME)</b> </dt> <dd> to define an output
 * path to MusicXML file (or directory). Same note as for -bench.</dd>
 *
 * </dd> </dl>
 *
 * @author Hervé Bitteur
 */
public class CLI
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    //~ Enumerations -----------------------------------------------------------
    /** For handling cardinality of command parameters */
    private static enum Card
    {
        //~ Enumeration constant initializers ----------------------------------

        /** No parameter expected */
        NONE,
        /** Just a single parameter is
         * expected */
        SINGLE,
        /** One or several
         * parameters are expected */
        MULTIPLE;

    }

    /** For command analysis */
    private static enum Command
    {
        //~ Enumeration constant initializers ----------------------------------

        HELP(
        "Prints help about application arguments and stops",
        Card.NONE,
        null),
        BATCH(
        "Specifies to run with no graphic user interface",
        Card.NONE,
        null),
        STEP(
        "Defines a series of target steps",
        Card.MULTIPLE,
        "(STEPNAME|@STEPLIST)+"),
        OPTION(
        "Defines a series of key=value constant pairs",
        Card.MULTIPLE,
        "(KEY=VALUE|@OPTIONLIST)+"),
        SCRIPT(
        "Defines a series of script files to run",
        Card.MULTIPLE,
        "(SCRIPTNAME|@SCRIPTLIST)+"),
        INPUT(
        "Defines a series of input image files to process",
        Card.MULTIPLE,
        "(FILENAME|@FILELIST)+"),
        PAGES(
        "Defines a set of specific pages to process",
        Card.MULTIPLE,
        "(PAGE|@PAGELIST)+"),
        BENCH(
        "Defines an output path to bench data file (or directory)",
        Card.SINGLE,
        "(DIRNAME|FILENAME)"),
        //        MIDI(
        //            "Defines an output path to MIDI file (or directory)",
        //            Card.SINGLE,
        //            "(DIRNAME|FILENAME)"), 
        PRINT(
        "Defines an output path to PDF file (or directory)",
        Card.SINGLE,
        "(DIRNAME|FILENAME)"),
        EXPORT(
        "Defines an output path to MusicXML file (or directory)",
        Card.SINGLE,
        "(DIRNAME|FILENAME)");
        //~ Instance fields ----------------------------------------------------

        /** Info about command itself */
        public final String description;

        /** Cardinality of the expected parameters */
        public final Card card;

        /** Info about expected command parameters */
        public final String params;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a Command object.
         *
         * @param description description of the command
         * @param params      description of command expected parameters, or
         *                    null
         */
        Command (String description,
                 Card card,
                 String params)
        {
            this.description = description;
            this.card = card;
            this.params = params;
        }
    }

    //~ Instance fields --------------------------------------------------------
    /** Name of the program */
    private final String toolName;

    /** The CLI arguments */
    private final String[] args;

    /** The parameters to fill */
    private final Parameters parameters;

    //~ Constructors -----------------------------------------------------------
    //-----//
    // CLI //
    //-----//
    /**
     * Creates a new CLI object.
     *
     * @param toolName the program name
     * @param args     the CLI arguments
     */
    public CLI (final String toolName,
                final String... args)
    {
        this.toolName = toolName;
        this.args = Arrays.copyOf(args, args.length);
        logger.debug("CLI args: {}", Arrays.toString(args));

        parameters = parse();

        if (parameters != null) {
            parameters.setImpliedSteps();
        }
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // getParameters //
    //---------------//
    /**
     * Parse the CLI arguments and return the populated parameters
     * structure.
     *
     * @return the parsed parameters, or null if failed
     */
    public Parameters getParameters ()
    {
        return parameters;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            sb.append(" ").append(arg);
        }

        return sb.toString();
    }

    //---------//
    // addItem //
    //---------//
    /**
     * Add an item to a provided list, while handling indirections if
     * needed.
     *
     * @param item the item to add, which can be a plain string (which is
     *             simply added to the list) or an indirection
     *             (a string starting by the '&#64;' character)
     *             which denotes a file of items to be recursively added
     * @param list the collection of items to be augmented
     */
    private void addItem (String item,
                          List<String> list)
    {
        // The item may be a plain string or the name of a pack that lists
        // item(s). This is signalled by a starting '@' character in string
        if (item.startsWith("@")) {
            // File with other items inside
            String pack = item.substring(1);
            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(pack));

                String newRef;

                try {
                    while ((newRef = br.readLine()) != null) {
                        addItem(newRef.trim(), list);
                    }

                    br.close();
                } catch (IOException ex) {
                    logger.warn("IO error while reading file ''{}''", pack);
                }
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find file ''{}''", pack);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (item.length() > 0) {
            // Plain item
            list.add(item);
        }
    }

    //-----------------//
    // decodeConstants //
    //-----------------//
    /**
     * Retrieve properties out of the flat sequence of "key = value"
     * pairs.
     *
     * @param constantPairs the flat sequence of key = value pairs
     * @return the resulting constant properties
     */
    private Properties decodeConstants (List<String> constantPairs)
            throws IOException
    {
        Properties props = new Properties();

        // Use a simple string buffer in memory
        StringBuilder sb = new StringBuilder();

        for (String pair : constantPairs) {
            sb.append(pair).append("\n");
        }

        props.load(new StringReader(sb.toString()));

        return props;
    }

    //-------//
    // parse //
    //-------//
    /**
     * Parse the CLI arguments and populate the parameters structure.
     *
     * @return the populated parameters structure, or null if failed
     */
    private Parameters parse ()
    {
        // Status of the finite state machine
        boolean paramNeeded = false; // Are we expecting a param?
        boolean paramForbidden = false; // Are we not expecting a param?
        Command command = Command.INPUT; // By default
        Parameters params = new Parameters();
        List<String> optionPairs = new ArrayList<>();
        List<String> stepStrings = new ArrayList<>();
        List<String> pageStrings = new ArrayList<>();

        // Parse all arguments from command line
        for (int i = 0; i < args.length; i++) {
            String token = args[i];

            if (token.startsWith("-")) {
                // This is a new command
                // Check that we were not expecting param(s)
                if (paramNeeded) {
                    printCommandLine();
                    stopUsage(
                            "Found no parameter after command '" + command + "'");

                    return null;
                }

                // Remove leading minus sign and switch to uppercase
                // To recognize command
                token = token.substring(1).toUpperCase(Locale.ENGLISH);

                boolean found = false;

                for (Command cmd : Command.values()) {
                    if (token.equals(cmd.name())) {
                        command = cmd;
                        paramNeeded = command.card != Card.NONE;
                        paramForbidden = !paramNeeded;
                        found = true;

                        break;
                    }
                }

                // No command recognized
                if (!found) {
                    printCommandLine();
                    stopUsage("Unknown command '-" + token + "'");

                    return null;
                }

                // Commands with no parameters
                switch (command) {
                case HELP: {
                    stopUsage(null);

                    return null;
                }

                case BATCH:
                    params.batchMode = true;

                    break;
                }
            } else {
                // This is a parameter for the current command
                // Check we can accept a parameter
                if (paramForbidden) {
                    printCommandLine();
                    stopUsage(
                            "Extra parameter '" + token
                            + "' found after command '" + command + "'");

                    return null;
                }

                switch (command) {
                case STEP:
                    addItem(token, stepStrings);

                    break;

                case OPTION:
                    addItem(token, optionPairs);

                    break;

                case SCRIPT:
                    addItem(token, params.scriptNames);

                    break;

                case INPUT:
                    addItem(token, params.inputNames);

                    break;

                case PAGES:
                    addItem(token, pageStrings);

                    break;

                case BENCH:
                    params.benchPath = token;

                    break;

                case EXPORT:
                    params.exportPath = token;

                    break;

                //                case MIDI :
                //                    params.midiPath = token;
                //
                //                    break;
                case PRINT:
                    params.printPath = token;

                    break;

                default:
                }

                paramNeeded = false;
                paramForbidden = command.card == Card.SINGLE;
            }
        }

        // Additional error checking
        if (paramNeeded) {
            printCommandLine();
            stopUsage("Expecting a token after command '" + command + "'");

            return null;
        }

        // Decode option pairs
        try {
            params.options = decodeConstants(optionPairs);
        } catch (Exception ex) {
            logger.warn("Error decoding -option ", ex);
        }

        // Check step names
        for (String stepString : stepStrings) {
            try {
                // Read a step name
                params.desiredSteps.add(
                        Steps.valueOf(stepString.toUpperCase()));
            } catch (Exception ex) {
                printCommandLine();
                stopUsage(
                        "Step name expected, found '" + stepString + "' instead");

                return null;
            }
        }

        // Check page ids
        for (String pageString : pageStrings) {
            try {
                // Read a page id (counted from 1)
                int id = Integer.parseInt(pageString);
                if (params.pages == null) {
                    params.pages = new TreeSet<>();
                }
                params.pages.add(id);
            } catch (Exception ex) {
                printCommandLine();
                stopUsage(
                        "Page id expected, found '" + pageString + "' instead");

                return null;
            }
        }

        // Results
        logger.debug(Main.dumping.dumpOf(params));

        return params;
    }

    //------------------//
    // printCommandLine //
    //------------------//
    /**
     * Printout the command line with its actual parameters.
     */
    private void printCommandLine ()
    {
        StringBuilder sb = new StringBuilder("Command line parameters: ");

        if (toolName != null) {
            sb.append(toolName).append(" ");
        }

        sb.append(this);
        logger.info(sb.toString());
    }

    //-----------//
    // stopUsage //
    //-----------//
    /**
     * Printout a message if any, followed by the general syntax for
     * the command line.
     *
     * @param msg the message to print if non null
     */
    private void stopUsage (String msg)
    {
        // Print message if any
        if (msg != null) {
            logger.warn(msg);
        }

        StringBuilder buf = new StringBuilder();

        // Print version
        buf.append("\nVersion:");
        buf.append("\n   ").append(WellKnowns.TOOL_REF);

        // Print arguments syntax
        buf.append("\nArguments syntax:");

        for (Command command : Command.values()) {
            buf.append(
                    String.format(
                    "%n  %-36s %s",
                    String.format(
                    " [-%s%s]",
                    command.toString().toLowerCase(Locale.ENGLISH),
                    ((command.params != null) ? (" " + command.params) : "")),
                    command.description));
        }

        // Print all allowed step names
        buf.append("\n\nKnown step names are in order").append(
                " (non case-sensitive):");

        for (Step step : Steps.values()) {
            buf.append(
                    String.format(
                    "%n   %-11s : %s",
                    step.toString().toUpperCase(),
                    step.getDescription()));
        }

        logger.info(buf.toString());
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // Parameters //
    //------------//
    /**
     * A structure that collects the various parameters parsed out of
     * the command line.
     */
    public static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Flag that indicates a batch mode */
        boolean batchMode = false;

        /** The set of desired steps */
        final Set<Step> desiredSteps = new LinkedHashSet<>();

        /** The map of options */
        Properties options = null;

        /** The list of script file names to execute */
        final List<String> scriptNames = new ArrayList<>();

        /** The list of input image file names to load */
        final List<String> inputNames = new ArrayList<>();

        /** The set of page ids to load */
        SortedSet<Integer> pages = null;

        /** Where log data is to be saved */
        ///String logPath = null;
        /** Where bench data is to be saved */
        String benchPath = null;

        /** Where exported score data (MusicXML) is to be saved */
        String exportPath = null;

        /** Where MIDI data is to be saved */
        String midiPath = null;

        /** Where printed score (PDF) is to be saved */
        String printPath = null;

        //~ Constructors -------------------------------------------------------
        private Parameters ()
        {
        }

        //~ Methods ------------------------------------------------------------
        //-----------------//
        // setImpliedSteps //
        //-----------------//
        /**
         * Some output parameters require their related step to be set.
         */
        private void setImpliedSteps ()
        {
            if (exportPath != null) {
                desiredSteps.add(Steps.valueOf(Steps.EXPORT));
            }

            if (printPath != null) {
                desiredSteps.add(Steps.valueOf(Steps.PRINT));
            }

            //            if (midiPath != null) {
            //                desiredSteps.add(Steps.valueOf(Steps.MIDI));
            //            }
        }
    }
}
