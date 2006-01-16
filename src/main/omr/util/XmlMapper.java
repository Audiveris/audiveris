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

import org.jibx.runtime.*;
import org.jibx.runtime.impl.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

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
     * @return the allocated entity or null if load has failed.
     */
    public Object load (File xmlFile)
        throws Exception
    {
        try {
            IUnmarshallingContext uctx = factory.createUnmarshallingContext();
            Object entity = uctx.unmarshalDocument
                (new FileInputStream(xmlFile), null);

            if (logger.isDebugEnabled()) {
                logger.debug("Entity loaded from " + xmlFile);
            }

            return entity;
        } catch (Exception ex) {
            logger.warning("Cannot Unmarshall from " + xmlFile);
            logger.warning(ex.toString());

            return null;
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
        try {
            IMarshallingContext mctx = factory.createMarshallingContext();
            mctx.setIndent(XML_INDENT);
            mctx.marshalDocument(entity, XML_ENCODING, null,
                                 new FileOutputStream(xmlFile));
            if (logger.isDebugEnabled()) {
                logger.debug("Entity written to " + xmlFile);
            }
        } catch (Exception ex) {
            logger.warning("Cannot marshall entity to file " + xmlFile);
            logger.warning(ex.toString());
            throw ex;
        }
    }
}
