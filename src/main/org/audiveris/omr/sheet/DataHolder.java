//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D a t a H o l d e r                                      //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardOpenOption.CREATE;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DataHolder} is a place holder for sheet internal data.
 * <p>
 * It handles: <ul>
 * <li>The data itself, if any.</li>
 * <li>A path to disk where data can be unmarshalled from.</li>
 * </ul>
 *
 * @param <T> specific type for data handled
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "holder")
public abstract class DataHolder<T>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            DataHolder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Direct access to data, if any. */
    protected T data;

    /** Path to data on disk. */
    @XmlAttribute(name = "path")
    protected final String pathString;

    /** To avoid useless marshalling to disk. */
    protected boolean modified = false;

    /** To avoid multiple load attempts. */
    protected boolean hasNoData = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DataHolder} object.
     *
     * @param pathString dedicated path within sheet internals
     */
    public DataHolder (String pathString)
    {
        this.pathString = pathString;
    }

    /**
     * Creates a new {@code DataHolder} object.
     */
    protected DataHolder ()
    {
        this.pathString = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    /**
     * Return the handled data.
     *
     * @param stub the related stub instance
     * @return the data, ready to use
     */
    public T getData (SheetStub stub)
    {
        if (data == null) {
            if (hasNoData) {
                return null;
            }

            try {
                stub.getBook().getLock().lock();

                if (data == null) {
                    // Open book file system
                    Path path = stub.getBook().openSheetFolder(stub.getNumber()).resolve(
                            pathString);
                    logger.debug("path: {}", path);

                    if (Files.exists(path)) {
                        InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
                        data = load(is);
                        is.close();
                        logger.debug("Loaded {}", path);
                    } else {
                        hasNoData = true;
                        logger.info("No {}", path);
                    }

                    path.getFileSystem().close(); // Close book file system
                    modified = false;
                }
            } catch (Exception ex) {
                logger.warn("Error reading data from " + pathString, ex);
            } finally {
                stub.getBook().getLock().unlock();
            }
        }

        return data;
    }

    //---------//
    // hasData //
    //---------//
    public boolean hasData ()
    {
        return data != null;
    }

    //-----------//
    // hasNoData //
    //-----------//
    public boolean hasNoData ()
    {
        return hasNoData;
    }

    //------------//
    // isModified //
    //------------//
    public boolean isModified ()
    {
        return modified;
    }

    //---------//
    // setData //
    //---------//
    public void setData (T data,
                         boolean modified)
    {
        this.data = data;
        setModified(modified);
    }

    //-------------//
    // setModified //
    //-------------//
    public void setModified (boolean bool)
    {
        modified = bool;
    }

    //-----------//
    // storeData //
    //-----------//
    public void storeData (Path sheetFolder,
                           Path oldSheetFolder)
    {
        final Path path = sheetFolder.resolve(pathString);
        OutputStream os = null;

        try {
            if (!hasData()) {
                if (oldSheetFolder != null) {
                    // Copy from old book file to new
                    Path oldPath = oldSheetFolder.resolve(pathString);
                    Files.copy(oldPath, path);
                    logger.info("Copied {}", path);
                }
            } else if (isModified()) {
                try {
                    Files.deleteIfExists(path);

                    os = Files.newOutputStream(path, CREATE);
                    store(os);
                    setModified(false);
                    logger.info("Stored {}", path);
                } finally {
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in storeData " + ex, ex);
        }
    }

    //------//
    // load //
    //------//
    protected abstract T load (InputStream is)
            throws Exception;

    //-------//
    // store //
    //-------//
    protected abstract void store (OutputStream os)
            throws Exception;
}
