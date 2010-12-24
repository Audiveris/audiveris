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
import omr.step.Steps;

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

        STEP,OPTION, FILE,
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
        Status       status = Status.FILE; // By default
        String       currentCommand = null;
        Parameters   params = new Parameters();
        List<String> optionPairs = new ArrayList<String>();
        List<String> stepStrings = new ArrayList<String>();

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
                } else if (token.equalsIgnoreCase("-file")) {
                    status = Status.FILE;
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
                    addItem(token, stepStrings);
                    paramNeeded = false;

                    break;

                case OPTION :
                    addItem(token, optionPairs);
                    paramNeeded = false;

                    break;

                case FILE :
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

        // At least first step
        if (params.desiredSteps.isEmpty()) {
            params.desiredSteps.add(Steps.first);
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
        if (toolName != null) {
            System.err.println(toolName);
        }

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
        buf.append("\n options syntax:")
           .append("\n [-help]")
           .append("\n [-batch]")
           .append("\n [-bench]")
           .append("\n [-step (STEPNAME|@STEPLIST)+]")
           .append("\n [-option (KEY=VALUE|@OPTIONLIST)+]")
           .append("\n [-file (FILENAME|@FILELIST)+]")
           .append("\n [-script (SCRIPTNAME|@SCRIPTLIST)+]");

        // Print all allowed step names
        buf.append("\n\nKnown step names are in order")
           .append(" (non case-sensitive):");

        for (Step step : Steps.values()) {
            buf.append(
                String.format(
                    "%n%-11s : %s",
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

        /** The set of desired steps (option: -step stepName) */
        final Set<Step> desiredSteps = new LinkedHashSet<Step>();

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
            sb.append("\ndesiredSteps=")
              .append(desiredSteps);
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
