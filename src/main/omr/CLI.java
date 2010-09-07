//----------------------------------------------------------------------------//
//                                                                            //
//                                   C L I                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.log.Logger;

import omr.step.Step;

import java.io.*;
import java.util.*;

/**
 * Class <code>CLI</code> handles the parameters of the command line interface
 *
 * @author Hervé Bitteur
 */
public class CLI
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CLI.class);

    //~ Enumerations -----------------------------------------------------------

    /** For parameters analysis */
    private static enum Status {
        //~ Enumeration constant initializers ----------------------------------

        STEP,OPTION, SHEET,
        SCRIPT;
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
     * @param args the CLI arguments
     */
    public CLI (final String    toolName,
                final String... args)
    {
        this.toolName = toolName;
        this.args = Arrays.copyOf(args, args.length);

        parameters = parse();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getParameters //
    //---------------//
    /**
     * Parse the CLI arguments and return the populated parameters structure
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
     * Add an item to a provided list, while handling indirections if needed
     * @param item the item to add, which can be a plain string (which is
     * simply added to the list) or an indirection (a string starting by the '@'
     * character) which denotes a file of items to be recursively added
     * @param list the collection of items to be augmented
     */
    private void addItem (String       item,
                          List<String> list)
    {
        // The item may be a plain string or the name of a pack that lists
        // item(s). This is signalled by a starting '@' character in string
        if (item.startsWith("@")) {
            // File with other items inside
            String         pack = item.substring(1);

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
                    logger.warning(
                        "IO error while reading file '" + pack + "'");
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file '" + pack + "'");
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
     * Retrieve properties out of the flat sequence of "key = value" pairs
     * @param constantPairs the flat sequence of key = value pairs
     * @return the resulting constant properties
     */
    private Properties decodeConstants (List<String> constantPairs)
        throws IOException
    {
        Properties    props = new Properties();

        // Use a simple string buffer in memory
        StringBuilder sb = new StringBuilder();

        for (String pair : constantPairs) {
            sb.append(pair)
              .append("\n");
        }

        props.load(new StringReader(sb.toString()));

        return props;
    }

    //-------//
    // parse //
    //-------//
    /**
     * Parse the CLI arguments and populate the parameters structure
     *
     * @return the populated parameters structure, or null if failed
     */
    private Parameters parse ()
    {
        // Status of the finite state machine
        boolean      paramNeeded = false; // Are we expecting a param?
        Status       status = Status.SHEET; // By default
        String       currentCommand = null;
        Parameters   params = new Parameters();
        List<String> optionPairs = new ArrayList<String>();

        // Parse all arguments from command line
        for (int i = 0; i < args.length; i++) {
            String token = args[i];

            if (token.startsWith("-")) {
                // This is a command
                // Check that we were not expecting param(s)
                if (paramNeeded) {
                    printCommandLine();
                    stopUsage(
                        "Found no parameter after command '" + currentCommand +
                        "'");

                    return null;
                }

                if (token.equalsIgnoreCase("-help")) {
                    stopUsage(null);

                    return null;
                } else if (token.equalsIgnoreCase("-batch")) {
                    params.batchMode = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-bench")) {
                    params.benchFlag = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-step")) {
                    status = Status.STEP;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-option")) {
                    status = Status.OPTION;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-sheet")) {
                    status = Status.SHEET;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-script")) {
                    status = Status.SCRIPT;
                    paramNeeded = true;
                } else {
                    printCommandLine();
                    stopUsage("Unknown command '" + token + "'");

                    return null;
                }

                // Remember the current command
                currentCommand = token;
            } else {
                // This is a parameter
                switch (status) {
                case STEP :

                    try {
                        // Read a step name
                        params.targetStep = Step.valueOf(token.toUpperCase());

                        // By default, sheets are now expected
                        status = Status.SHEET;
                        paramNeeded = false;
                    } catch (Exception ex) {
                        printCommandLine();
                        stopUsage(
                            "Step name expected, found '" + token +
                            "' instead");

                        return null;
                    }

                    break;

                case OPTION :
                    addItem(token, optionPairs);
                    paramNeeded = false;

                    break;

                case SHEET :
                    addItem(token, params.sheetNames);
                    paramNeeded = false;

                    break;

                case SCRIPT :
                    addItem(token, params.scriptNames);
                    paramNeeded = false;

                    break;
                }
            }
        }

        // Additional error checking
        if (paramNeeded) {
            printCommandLine();
            stopUsage(
                "Expecting a parameter after command '" + currentCommand + "'");

            return null;
        }

        // Decode option pairs
        try {
            params.constants = decodeConstants(optionPairs);
        } catch (Exception ex) {
            logger.warning("Error decoding -option ", ex);
        }

        // Results
        if (logger.isFineEnabled()) {
            logger.fine("CLI parameters:" + params);
        }

        return params;
    }

    //------------------//
    // printCommandLine //
    //------------------//
    /**
     * Printout the command line with its actual parameters
     */
    private void printCommandLine ()
    {
        System.err.println(toolName);
        System.err.println(this);
    }

    //-----------//
    // stopUsage //
    //-----------//
    /**
     * Printout a message if any, followed by the general syntax for the
     * command line
     * @param msg the message to print if non null
     */
    private void stopUsage (String msg)
    {
        // Print message if any
        if (msg != null) {
            logger.warning(msg);
        }

        StringBuilder buf = new StringBuilder();

        // Print standard command line syntax
        buf.append(toolName + " options syntax:")
           .append(" [-help]")
           .append(" [-batch]")
           .append(" [-step STEPNAME]")
           .append(" [-option (KEY=VALUE|@OPTIONLIST)+]")
           .append(" [-sheet (SHEETNAME|@SHEETLIST)+]")
           .append(" [-script (SCRIPTNAME|@SCRIPTLIST)+]");

        // Print all allowed step names
        buf.append("\n      Known step names are in order")
           .append(" (non case-sensitive) :");

        for (Step step : Step.values()) {
            buf.append(
                String.format(
                    "%n%-11s : %s",
                    step.toString().toUpperCase(),
                    step.description));
        }

        logger.info(buf.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Parameters //
    //------------//
    /**
     * A structure that collects the various parameters parsed out of the
     * command line
     */
    public static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Flag that indicates a batch mode */
        boolean batchMode = false;

        /** Flag that indicates bench data is to be saved */
        boolean benchFlag = false;

        /** The desired step if any (option: -step stepName) */
        Step targetStep = Step.LOAD;

        /** The map of constants */
        Properties constants = null;

        /** The list of sheet file names to load */
        final List<String> sheetNames = new ArrayList<String>();

        /** The list of script file names to execute */
        final List<String> scriptNames = new ArrayList<String>();

        //~ Constructors -------------------------------------------------------

        private Parameters ()
        {
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("\nbatchMode=")
              .append(batchMode);
            sb.append("\nbenchFlag=")
              .append(benchFlag);
            sb.append("\ntargetStep=")
              .append(targetStep);
            sb.append("\noptions=")
              .append(constants);
            sb.append("\nsheetNames=")
              .append(sheetNames);
            sb.append("\nscriptNames=")
              .append(scriptNames);

            return sb.toString();
        }
    }
}
