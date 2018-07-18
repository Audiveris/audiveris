//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R u n T a b l e H o l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture.TableKey;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code RunTableHolder} holds the reference to a run table, at least the path
 * to its marshalled data on disk, and (on demand) the unmarshalled run table itself.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class RunTableHolder
        extends DataHolder<RunTable>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunTableHolder.class);

    private static JAXBContext jaxbContext;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunTableHolder} object.
     *
     * @param key table key
     */
    public RunTableHolder (TableKey key)
    {
        super(key + ".xml");
    }

    /** No-arg constructor needed for JAXB. */
    private RunTableHolder ()
    {
        super();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // load //
    //------//
    @Override
    protected RunTable load (InputStream is)
            throws Exception
    {
        return (RunTable) Jaxb.unmarshal(is, getJaxbContext());
    }

    //-------//
    // store //
    //-------//
    @Override
    protected void store (OutputStream os)
            throws Exception
    {
        Jaxb.marshal(data, os, getJaxbContext());
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private JAXBContext getJaxbContext ()
    {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(RunTable.class);
            } catch (JAXBException ex) {
                logger.error("Cannot build JAXB context " + ex, ex);
            }
        }

        return jaxbContext;
    }
}
