//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r i p t M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.log.Logger;

import omr.Main;

import omr.score.Score;

import omr.step.ProcessingCancellationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Class {@code ScriptManager} is in charge of handling the storing and
 * loading of scripts.
 *
 * @author Hervé Bitteur
 */
public class ScriptManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScriptManager.class);

    /** File extension for script files */
    public static final String SCRIPT_EXTENSION = ".script.xml";

    /** Un/marshalling context for use with JAXB */
    private static volatile JAXBContext jaxbContext;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // ScriptManager //
    //---------------//
    /**
     * Not meant to be instantiated
     */
    private ScriptManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of ScriptManager in the application.
     *
     * @return the instance
     */
    public static ScriptManager getInstance ()
    {
        return Holder.INSTANCE;
    }

    //------//
    // load //
    //------//
    /**
     * Load a script from an input stream.
     *
     * @param input the input stream to be read
     * @return the loaded script, or null if failed
     */
    public Script load (InputStream input)
    {
        try {
            Unmarshaller um = getJaxbContext().createUnmarshaller();

            return (Script) um.unmarshal(input);
        } catch (JAXBException ex) {
            logger.warning("Cannot unmarshal script", ex);

            return null;
        }
    }

    //------------//
    // loadAndRun //
    //------------//
    public void loadAndRun (File file)
    {
        Script script = null;
        try {
            long start = System.currentTimeMillis();
            logger.info("Loading script file {0} ...", file);
            try (FileInputStream fis = new FileInputStream(file)) {
                script = load(fis);
            }
            script.run();
            long stop = System.currentTimeMillis();
            logger.info("Script file {0} run in {1} ms", file, stop - start);
        } catch (ProcessingCancellationException pce) {
            Score score = script.getScore();
            logger.warning("Cancelled " + score, pce);

            if (score != null) {
                score.getBench().recordCancellation();
            }
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot find script file {0}", file);
        } catch (Exception ex) {
            logger.warning("Exception occurred", ex);
        } finally {
            // Close when in batch mode
            if ((Main.getGui() == null) && (script != null)) {
                Score score = script.getScore();

                if (score != null) {
                    score.close();
                }
            }
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store a script into an output stream.
     *
     * @param script the script to store
     * @param output the output stream to be written
     * @throws JAXBException
     */
    public void store (Script script,
                       OutputStream output)
            throws JAXBException
    {
        logger.fine("Storing {0}", script);

        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(script, output);

        // Flag the script with this event
        script.setModified(false);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            synchronized (this) {
                if (jaxbContext == null) {
                    jaxbContext = JAXBContext.newInstance(Script.class);
                }
            }
        }

        return jaxbContext;
    }

    //~ Inner Interfaces -------------------------------------------------------
    //--------//
    // Holder //
    //--------//
    private static interface Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final ScriptManager INSTANCE = new ScriptManager();

    }
}
