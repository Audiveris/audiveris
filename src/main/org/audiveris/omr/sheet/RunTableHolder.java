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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code RunTableHolder} holds the reference to a run table, at least the path
 * to its marshalled data on disk, and (on demand) the unmarshalled run table itself.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class RunTableHolder
{

    private static final Logger logger = LoggerFactory.getLogger(RunTableHolder.class);

    /** Direct access to data, if any. */
    private RunTable data;

    /** Path to data on disk. */
    @XmlAttribute(name = "path")
    private final String pathString;

    /** To avoid useless marshalling to disk. */
    private boolean modified = false;

    /**
     * Creates a new {@code RunTableHolder} object.
     *
     * @param key table key
     */
    public RunTableHolder (TableKey key)
    {
        pathString = key + ".xml";
    }

    /** No-arg constructor needed for JAXB. */
    private RunTableHolder ()
    {
        pathString = null;
    }

    //---------//
    // getData //
    //---------//
    /**
     * Return the handled data.
     *
     * @param stub the related stub instance
     * @return the data, ready to use
     */
    public RunTable getData (SheetStub stub)
    {
        if (data == null) {
            try {
                stub.getBook().getLock().lock();

                if (data == null) {
                    // Open book file system
                    Path dataFolder = stub.getBook().openSheetFolder(stub.getNumber());
                    Path dataFile = dataFolder.resolve(pathString);
                    logger.debug("path to file: {}", dataFile);
                    data = RunTable.unmarshal(dataFile);
                    dataFile.getFileSystem().close(); // Close book file system
                    modified = false;
                    logger.debug("Loaded {}", dataFile);
                }
            } catch (IOException ex) {
                logger.warn("Error unmarshalling from {}", pathString, ex);
            } finally {
                stub.getBook().getLock().unlock();
            }
        }

        return data;
    }

    //---------//
    // hasData //
    //---------//
    /**
     * Tell whether run table data is present.
     *
     * @return true if loaded
     */
    public boolean hasData ()
    {
        return data != null;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Tell whether it has been modified.
     *
     * @return true if modified
     */
    public boolean isModified ()
    {
        return modified;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Assign modified indicator.
     *
     * @param bool new value for modified
     */
    public void setModified (boolean bool)
    {
        modified = bool;
    }

    //---------//
    // setData //
    //---------//
    /**
     * Assign the table data (table just created) and modified flag.
     *
     * @param data     the table data
     * @param modified modified flag
     */
    public void setData (RunTable data,
                         boolean modified)
    {
        this.data = data;
        setModified(modified);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("RunTableHolder{");

        if (pathString != null) {
            sb.append(pathString);
        }

        sb.append('}');

        return sb.toString();
    }

}
