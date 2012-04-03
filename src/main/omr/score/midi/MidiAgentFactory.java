//----------------------------------------------------------------------------//
//                                                                            //
//                      M i d i A g e n t F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.log.Logger;

import omr.util.OmrExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Class {@code MidiAgentFactory} handles access to the actual MidiAgent
 * instance, and avoids instantiation unless necessary.
 *
 * @author Hervé Bitteur
 */
public class MidiAgentFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        MidiAgentFactory.class);

    /** A future which reflects whether Midi Agent has been initialized **/
    private static Future<Void> loading;

    /** The at most single instance */
    private static MidiAgent agent;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // MidiAgentFactory //
    //------------------//
    /**
     * Not meant to be instantiated
     */
    private MidiAgentFactory ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getAgent //
    //----------//
    /**
     * Report the single instance of MidiAgent class (after creating it if
     * necessary)
     * @return the single instance of MidiAgent
     */
    public static synchronized MidiAgent getAgent ()
    {
        if (loading == null) {
            preload();
        }

        try {
            // Wait until loading is completed, if needed
            loading.get();
        } catch (Throwable ex) {
            logger.severe("Cannot load Midi Agent", ex);
            throw new RuntimeException(ex);
        }

        return agent;
    }

    //----------//
    // hasAgent //
    //----------//
    /**
     * Just check if the agent is available
     * @return true if the agent is available
     */
    public static synchronized boolean hasAgent ()
    {
        return agent != null;
    }

    //---------//
    // preload //
    //---------//
    /**
     * Launch the preloading of the MidiAgent instance
     */
    public static synchronized void preload ()
    {
        loading = OmrExecutors.getCachedLowExecutor()
                              .submit(
            new Callable<Void>() {
                    public Void call ()
                        throws Exception
                    {
                        try {
                            agent = new MidiAgent();
                        } catch (Exception ex) {
                            logger.warning(
                                "Could not preload the Midi Agent",
                                ex);
                            throw ex;
                        }

                        return null;
                    }
                });
    }
}
