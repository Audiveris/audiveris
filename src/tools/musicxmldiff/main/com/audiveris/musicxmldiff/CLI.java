//----------------------------------------------------------------------------//
//                                                                            //
//                                   C L I                                    //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class {@code CLI} parses and holds the parameters of the command
 * line interface.
 *
 * <p> The command line parameters can be (order and case are not relevant):
 * <dl>
 *
 * <dt><b>-help</b> </dt> <dd>(optional) Prints a quick usage help and
 * leaves the application. </dd>
 *
 * <dt><b>-filter FILENAME</b></dt>
 * <dd>(optional) Defines a filter to drive comparison.
 * If none, no filtering will be performed</dd>
 *
 * <dt><b>-output FILENAME</b></dt>
 * <dd>(optional) Defines a file where differences must be written.
 * If none, console will be used</dd>
 *
 * <dt><b>-control FILENAME</b></dt>
 * <dd>Defines the XML input to be used as reference.</dd>
 *
 * <dt><b>-test FILENAME</b></dt>
 * <dd>Defines the XML input to be compared to the reference.</dd>
 *
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class CLI
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    //~ Enumerations -----------------------------------------------------------

    /** For handling mandatory or optional occurrence. */
    private static enum Occurrence {
        //~ Enumeration constant initializers ----------------------------------

        OPTIONAL,MANDATORY;
    }

    /** For handling cardinality of command parameters. */
    private static enum Card {
        //~ Enumeration constant initializers ----------------------------------


        /**
         * No parameter expected
         */
        NONE,
        /**
         * Exactly one parameter is expected
         */
        SINGLE, 
        /**
         * One or several parameters are expected
         */
        MULTIPLE;
    }

    /**
     * To drive command analysis.
     */
    private static enum Command {

        //~ Enumeration constant initializers ----------------------------------

        HELP(
            Occurrence.OPTIONAL,
            "Prints help on commands then stops",
            Card.NONE,
            null),
        FILTER(
            Occurrence.OPTIONAL,
            "Filter for comparison",
            Card.SINGLE,
            "FILENAME"), 
        OUTPUT(
            Occurrence.OPTIONAL,
            "Where to write results",
            Card.SINGLE,
            "FILENAME"), 
        CONTROL(
            Occurrence.MANDATORY,
            "XML input used as reference",
            Card.SINGLE,
            "FILENAME"), 
        TEST(
            Occurrence.MANDATORY,
            "XML input compared to reference input",
            Card.SINGLE,
            "FILENAME");
        //~ Instance fields ----------------------------------------------------

        /** Mandatory or optional. */
        public final Occurrence occurrence;

        /** Info about command itself. */
        public final String description;

        /** Cardinality of the expected parameters. */
        public final Card card;

        /** Info about expected command parameters. */
        public final String params;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a Command object.
         *
         * @param occurrence  mandatory or optional
         * @param description description of the command
         * @param card        cardinality of expected parameters
         * @param params      description of command expected parameters, or
         *                    null
         */
        Command (Occurrence occurrence,
                 String     description,
                 Card       card,
                 String     params)
        {
            this.occurrence = occurrence;
            this.description = description;
            this.card = card;
            this.params = params;
        }
    }

    //~ Instance fields --------------------------------------------------------

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
     * @param args the CLI arguments
     */
    public CLI (final String... args)
    {
        this.args = Arrays.copyOf(args, args.length);
        logger.debug("CLI args: {}", Arrays.toString(args));

        parameters = parse();
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
            sb.append(" ")
              .append(arg);
        }

        return sb.toString();
    }

    //---------//
    // addItem //
    //---------//
    /**
     * Add an item to a provided list.
     *
     * @param item the item to add
     * @param list the collection of items to be augmented
     */
    private void addItem (String       item,
                          List<String> list)
    {
        if (item.length() > 0) {
            list.add(item);
        }
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
        boolean    paramNeeded = false; // Are we expecting a param?
        boolean    paramForbidden = true; // Are we not expecting a param?
        Command    command = null;
        Parameters params = new Parameters();

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
                // to recognize command
                token = token.substring(1)
                             .toUpperCase(Locale.ENGLISH);

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
                case HELP : {
                    stopUsage(null);

                    return null;
                }
                }
            } else {
                // This is a parameter for the current command
                // Check we can accept a parameter
                if (paramForbidden) {
                    printCommandLine();
                    stopUsage("Unexpected parameter '" + token + "'");

                    return null;
                }

                if (command != null) {
                    switch (command) {
                    case FILTER :
                        params.filterPath = token;

                        break;

                    case OUTPUT :
                        params.outputPath = token;

                        break;

                    case CONTROL :
                        params.controlPath = token;

                        break;

                    case TEST :
                        params.testPath = token;

                        break;

                    default :
                    }

                    paramNeeded = false;
                    paramForbidden = command.card == Card.SINGLE;
                }
            }
        }

        // Additional error checking
        if (paramNeeded) {
            printCommandLine();
            stopUsage("Expecting a token after command '" + command + "'");

            return null;
        }

        // Results
        params.debug();

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

        // Print standard command line syntax
        buf.append("\nCommand line syntax:");
        buf.append("\n   java -jar <musicxmldiff.jar> parameters+");
        buf.append("\nParameters syntax (order & case are non relevant):");

        for (Command command : Command.values()) {
            buf.append(
                String.format(
                    "%n  %-36s %s",
                    String.format(
                        (command.occurrence == Occurrence.OPTIONAL)
                                                ? " [-%s%s]" : " -%s%s",
                        command.toString().toLowerCase(Locale.ENGLISH),
                        ((command.params != null) ? (" " + command.params) : "")),
                    command.description));
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

        /** Which filter to use */
        String filterPath = null;

        /** Where output is to be saved */
        String outputPath = null;

        /** Input used as control */
        String controlPath = null;

        /** Input used as test */
        String testPath = null;

        //~ Methods ------------------------------------------------------------

        public void debug ()
        {
            logger.debug("filter:  {}", filterPath);
            logger.debug("output:  {}", outputPath);
            logger.debug("control: {}", controlPath);
            logger.debug("test:    {}", testPath);
        }
    }
}
