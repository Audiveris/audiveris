//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S c r i p t M a n a g e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.script;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.step.ProcessingCancellationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Class {@code ScriptManager} is in charge of handling storing and loading of scripts.
 *
 * @author Hervé Bitteur
 */
public class ScriptManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be publicly instantiated.
     */
    private ScriptManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
            logger.warn("Cannot unmarshal script", ex);

            return null;
        }
    }

    //------------//
    // loadAndRun //
    //------------//
    /**
     * Load and run the script described by the provided file.
     *
     * @param file           the provided script file
     * @param closeBookOnEnd true do close book at script end (when running in batch)
     * @return the book created
     */
    public Book loadAndRun (File file,
                            boolean closeBookOnEnd)
    {
        Script script = null;

        try {
            long start = System.currentTimeMillis();
            logger.info("Loading script file {} ...", file);

            FileInputStream fis = null;

            try {
                fis = new FileInputStream(file);
                script = load(fis);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }

            BookManager.getInstance().getScriptHistory().add(file.toPath());
            script.run();

            long stop = System.currentTimeMillis();
            logger.info("Script file {} run in {} ms", file, stop - start);
        } catch (ProcessingCancellationException pce) {
            if (script != null) {
                Book book = script.getBook();
                logger.warn("Cancelled " + book, pce);
            }
        } catch (FileNotFoundException ex) {
            logger.warn("Cannot find script file {}", file);
        } catch (Exception ex) {
            logger.warn(ex.toString(), ex);
        } finally {
            // Close when in batch mode?
            if ((OMR.gui == null) && (script != null) && closeBookOnEnd) {
                Book book = script.getBook();

                if (book != null) {
                    book.close();
                }
            }
        }

        if (script == null) {
            return null;
        }

        return script.getBook();
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
        logger.debug("Storing {}", script);

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

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //--------//
    // Holder //
    //--------//
    private static interface Holder
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static final ScriptManager INSTANCE = new ScriptManager();
    }
}
