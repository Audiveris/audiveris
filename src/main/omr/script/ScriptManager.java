//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r i p t M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.log.Logger;

import java.io.*;

import javax.xml.bind.*;

/**
 * Class <code>ScriptManager</code> is in charge of handling the storing and
 * loading of scripts
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
     * Report the single instance of ScriptManager in the application
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
     * Load a script from an input stream
     *
     * @param input the input stream to be read
     * @return the loaded script, or null if failed
     */
    public Script load (InputStream input)
    {
        try {
            Unmarshaller um = getJaxbContext()
                                  .createUnmarshaller();

            return (Script) um.unmarshal(input);
        } catch (JAXBException ex) {
            logger.warning("Cannot unmarshal script", ex);

            return null;
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store a script into an output stream
     *
     * @param script the script to store
     * @param output the output stream to be written
     * @throws JAXBException
     */
    public void store (Script       script,
                       OutputStream output)
        throws JAXBException
    {
        if (logger.isFineEnabled()) {
            logger.fine("Storing " + script);
        }

        Marshaller m = getJaxbContext()
                           .createMarshaller();
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
                    jaxbContext = JAXBContext.newInstance(
                        AssignTask.class,
                        BarlineTask.class,
                        BoundaryTask.class,
                        ExportTask.class,
                        GlyphTask.class,
                        MidiWriteTask.class,
                        ParametersTask.class,
                        PlayTask.class,
                        Script.class,
                        ScriptTask.class,
                        SegmentTask.class,
                        SlurTask.class,
                        StepTask.class,
                        TextTask.class);
                }
            }
        }

        return jaxbContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final ScriptManager INSTANCE = new ScriptManager();
    }
}
