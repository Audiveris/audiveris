//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S c h e m a G e n e r a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;

/**
 * Class <code>SchemaGeneration</code>
 *
 * @author Hervé Bitteur
 */
public class SchemaGeneration
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SchemaGeneration.class);

    //~ Constructors -------------------------------------------------------------------------------
    private SchemaGeneration ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    /**
     * Generate schema for provided root class.
     *
     * @param args full class name, full output file name
     */
    public static void main (String... args)
    {
        if ((args == null) || (args.length != 2)) {
            logger.error("Expected 2 arguments");

            return;
        }

        final String className = args[0];
        final String outputFileName = args[1];

        try {
            final Class<?> classe = Class.forName(className);
            final Method getJaxbContext = classe.getDeclaredMethod("getJaxbContext", new Class[0]);
            final JAXBContext context = (JAXBContext) getJaxbContext.invoke(
                    null, new Object[]{});
            final SchemaOutputResolver sor = new Jaxb.OmrSchemaOutputResolver(outputFileName);
            context.generateSchema(sor);
        } catch (IOException |
                 ClassNotFoundException |
                 IllegalAccessException |
                 IllegalArgumentException |
                 NoSuchMethodException |
                 SecurityException |
                 InvocationTargetException ex) {
            logger.error("Error processing schema for class {}", className, ex);
        }
    }
}
