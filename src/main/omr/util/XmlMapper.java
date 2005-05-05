//-----------------------------------------------------------------------//
//                                                                       //
//                           X m l M a p p e r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.util;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

/**
 * Class <code>XmlMapper</code> handles the marshalling and unmarshalling of
 * any entity with an external XML support. This is implemented on top of
 * Castor utility.
 *
 * @param <E> the precise class for entity to be handled
 */
public class XmlMapper <E>
{
    //~ Static variables/initializers ----------------------------------------

    private static final Logger logger = Logger.getLogger(XmlMapper.class);

    //~ Instance variables ---------------------------------------------------

    // The Castor mapping for loading and storing from/to an XML file
    private final Mapping mapping;

    //~ Constructors ---------------------------------------------------------

    //-----------//
    // XmlMapper //
    //-----------//
    /**
     * Create an XML Mapper, based on its Castor descriptors
     *
     * @param mappingFileName the name of the file which contains the Castor
     *                        mapping descriptors. <b>NOTA</b>The file is
     *                        searched in the config directory of the
     *                        application jar file, so a good example could be
     *                        "/config/castor-glyph-mapping.xml"
     */
    public XmlMapper (String mappingFileName)
            throws Exception
    {
        try {
            mapping = new Mapping();
            URL url = XmlMapper.class.getResource(mappingFileName);
            mapping.loadMapping(url);
        } catch (IOException ex) {
            logger.warning("Cannot load mapping Xml file " + mappingFileName);
            logger.warning(ex.toString());
            throw ex;
        } catch (MappingException ex) {
            logger.warning("Cannot load mapping Xml file " + mappingFileName);
            logger.warning(ex.toString());
            throw ex;
        } catch (NullPointerException ex) {
            logger.warning("Cannot find mapping Xml file " + mappingFileName);
            logger.warning(ex.toString());
            throw ex;
        }
    }

    //~ Methods --------------------------------------------------------------

    //------//
    // load //
    //------//
    /**
     * Unmarshal the provided XML file to allocate the corresponding entity
     *
     * @param xmlFile the file that contains the Entity data in XML format
     *
     * @return the allocated entity or null if load has failed.
     */
    @SuppressWarnings("unchecked")      // This inhibition does not work
                                        // (Java 1.5 bug)
        public E load (File xmlFile)
        throws Exception
    {
        try {
            Unmarshaller unmarshaller = new Unmarshaller(mapping);
            // Unmarshal the data
            Object obj = unmarshaller.unmarshal (new InputSource(new FileReader(xmlFile)));
            E entity = (E) obj;         // Normal compiler warning here

            if (logger.isDebugEnabled()) {
                logger.debug("Entity loaded from " + xmlFile);
            }

            return entity;
        } catch (Exception ex) {
            logger.warning("Cannot Unmarshal from " + xmlFile);
            logger.warning(ex.toString());

            return null;
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Marshal the provided Entity to its dedicated XML file
     *
     * @param entity  the entity to be marshalled
     * @param xmlFile the XML file to be written
     */
    public void store (E entity,
                       File xmlFile)
            throws Exception
    {
        FileWriter writer = null;
        try {
            // Marshal
            writer = new FileWriter(xmlFile);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.marshal(entity);
            if (logger.isDebugEnabled()) {
                logger.debug("Entity written to " + xmlFile);
            }
        } catch (Exception ex) {
            logger.warning("Cannot store entity to file " + xmlFile, ex);
            throw ex;
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
            }
        }
    }
}
