//-----------------------------------------------------------------------//
//                                                                       //
//                           X m l M a p p e r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import java.io.*;

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

/**
 * Class <code>XmlMapper</code> handles the marshalling and unmarshalling
 * of any entity with an external XML support. This is implemented on top
 * of JiBX utility.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class XmlMapper
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(XmlMapper.class);

    /** Standard indentation value for xml formatting */
    public static final int XML_INDENT = 4;

    /** Standard encoding for xml file */
     public static final String XML_ENCODING = "UTF-8";

    //~ Instance variables ------------------------------------------------

    // The JiBX factory for loading and storing from/to an XML file
    private final IBindingFactory factory;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // XmlMapper //
    //-----------//
    /**
     * Create an XML Mapper
     *
     * @param entityClass the class of the mapped entity
     */
    public XmlMapper (Class entityClass)
    {
        try {
            factory = BindingDirectory.getFactory(entityClass);
        } catch (JiBXException ex) {
            logger.warning("Cannot get a JiBX factory for " + entityClass);
            logger.warning(ex.toString());
            throw new RuntimeException(ex);
        }
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // load //
    //------//
    /**
     * Unmarshall the provided XML file to allocate the corresponding
     * entity
     *
     * @param xmlFile the file that contains the Entity data in XML format
     *
     * @return the allocated entity or throw an exception
     */
    public Object load (File xmlFile)
        throws Exception
    {
        InputStream is = null;

        try {
            is = new FileInputStream(xmlFile);
            Object entity = load(is);

            if (logger.isFineEnabled()) {
                logger.fine("Entity loaded from " + xmlFile);
            }

            return entity;
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot Unmarshall from " + xmlFile);

            throw ex;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    //------//
    // load //
    //------//
    /**
     * Unmarshall data from the provided input stream to allocate the
     * corresponding entity
     *
     * @param is the input stream that contains the Entity data in XML format
     *
     * @return the allocated entity or throws an exception
     */
    public Object load (InputStream is)
        throws Exception
    {
        try {
            IUnmarshallingContext uctx = factory.createUnmarshallingContext();
            Object entity = uctx.unmarshalDocument(is, null);

            if (logger.isFineEnabled()) {
                logger.fine("Entity loaded from input stream " + is);
            }

            return entity;
        } catch (Exception ex) {
            ex.printStackTrace ();
            logger.warning(ex.toString());
            logger.warning("Cannot Unmarshall from input stream " + is);

            throw ex;
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Marshall the provided Entity to its dedicated XML file
     *
     * @param entity  the entity to be marshalled
     * @param xmlFile the XML file to be written
     */
    public void store (Object entity,
                       File xmlFile)
        throws Exception
    {
        OutputStream os = null;

        try {
            os = new FileOutputStream(xmlFile);
            store(entity, os);

            if (logger.isFineEnabled()) {
                logger.fine("Entity written to file " + xmlFile);
            }
        } catch (Exception ex) {
            logger.warning("Cannot marshall entity to file " + xmlFile);

            throw ex;
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Marshall the entity to the provided output stream
     *
     * @param entity  the entity to be marshalled
     * @param os      the chosen output stream
     */
    public void store (Object       entity,
                       OutputStream os)
        throws Exception
    {
        try {
            IMarshallingContext mctx = factory.createMarshallingContext();
            mctx.setIndent(XML_INDENT);
            mctx.marshalDocument(entity, XML_ENCODING, null, os);

            if (logger.isFineEnabled()) {
                logger.fine("Entity written to output stream " + os);
            }
        } catch (Exception ex) {
            logger.warning(ex.toString());
            logger.warning("Cannot marshall entity to output stream  " + os);

            throw ex;
        }
    }
}
