//----------------------------------------------------------------------------//
//                                                                            //
//                                   C L I                                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr;

import omr.step.Step;

import omr.util.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>CLI</code> handles the parameters of the command line interface
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CLI
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CLI.class);

    //~ Instance fields --------------------------------------------------------

    /** Name of the program */
    private final String toolName;

    /** The CLI arguments */
    private final String[] args;

    /** The parameters to fill */
    private final Parameters parameters = new Parameters();

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
    public CLI (String    toolName,
                String... args)
    {
        this.toolName = toolName;
        this.args = args;
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // parse //
    //-------//
    /**
     * Parese the CLI arguments and return the filled parameters
     *
     * @return the filled parameters, or null if failed
     */
    public Parameters parse ()
    {
        // Status of the finite state machine
        final int STEP = 0;
        final int SHEET = 1;
        final int SCRIPT = 2;
        boolean   paramNeeded = false; // Are we expecting a param?
        int       status = SHEET; // By default
        String    currentCommand = null;

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
                    parameters.batchMode = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-step")) {
                    status = STEP;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-sheet")) {
                    status = SHEET;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-script")) {
                    status = SCRIPT;
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
                    // Read a step name
                    parameters.targetStep = Step.valueOf(token.toUpperCase());

                    if (parameters.targetStep == null) {
                        printCommandLine();
                        stopUsage(
                            "Step name expected, found '" + token +
                            "' instead");

                        return null;
                    }

                    // By default, sheets are now expected
                    status = SHEET;
                    paramNeeded = false;

                    break;

                case SHEET :
                    addRef(token, parameters.sheetNames);
                    paramNeeded = false;

                    break;

                case SCRIPT :
                    addRef(token, parameters.scriptNames);
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

        // Results
        if (logger.isFineEnabled()) {
            logger.fine("CLI parameters:" + parameters);
        }

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

    //--------//
    // addRef //
    //--------//
    private void addRef (String       ref,
                         List<String> list)
    {
        // The ref may be a plain file name or the name of a pack that lists
        // ref(s). This is signalled by a starting '@' character in ref
        if (ref.startsWith("@")) {
            // File with other refs inside
            String pack = ref.substring(1);

            try {
                BufferedReader br = new BufferedReader(new FileReader(pack));
                String         newRef;

                try {
                    while ((newRef = br.readLine()) != null) {
                        addRef(newRef.trim(), list);
                    }

                    br.close();
                } catch (IOException ex) {
                    logger.warning(
                        "IO error while reading file '" + pack + "'");
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file '" + pack + "'");
            }
        } else
        // Plain file name
        if (ref.length() > 0) {
            list.add(ref);
        }
    }

    //------------------//
    // printCommandLine //
    //------------------//
    private void printCommandLine ()
    {
        System.err.println(toolName);
        System.err.println(this);
    }

    //-----------//
    // stopUsage //
    //-----------//
    private void stopUsage (String msg)
    {
        // Print message if any
        if (msg != null) {
            logger.warning(msg);
        }

        StringBuilder buf = new StringBuilder();

        // Print standard command line syntax
        buf.append("usage: java ")
           .append(toolName)
           .append(" [-help]")
           .append(" [-batch]")
           .append(" [-step STEPNAME]")
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
                    step.getDescription()));
        }

        logger.info(buf.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Parameters //
    //------------//
    public static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        boolean      batchMode = false;
        Step         targetStep = Step.LOAD;
        List<String> sheetNames = new ArrayList<String>();
        List<String> scriptNames = new ArrayList<String>();

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("\nbatchMode=")
              .append(batchMode);
            sb.append("\ntargetStep=")
              .append(targetStep);
            sb.append("\nsheetNames=")
              .append(sheetNames);
            sb.append("\nscriptNames=")
              .append(scriptNames);

            return sb.toString();
        }
    }
}
