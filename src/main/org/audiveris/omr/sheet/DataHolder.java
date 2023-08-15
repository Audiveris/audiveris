//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D a t a H o l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
 * Class <code>DataHolder</code> is a place holder for sheet internal data.
 * <p>
 * It handles:
 * <ul>
 * <li>The data itself, if any.</li>
 * <li>A path to disk where data can be unmarshalled/marshalled from/to.</li>
 * </ul>
 *
 * @param <T> specific type for data handled
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "holder")
public abstract class DataHolder<T>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DataHolder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Path to data on disk (within sheet folder). */
    @XmlAttribute(name = "path")
    protected final String pathString;

    // Transient data
    //---------------

    /** Direct access to data, if any. */
    protected T data;

    /** To avoid useless marshalling to disk. */
    protected boolean modified = false;

    /** To avoid multiple load attempts. */
    protected boolean hasNoData = false;

    /** To discard data. (Removed from disk at store time) */
    protected boolean discarded = false;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor needed for JAXB.
     */
    protected DataHolder ()
    {
        this.pathString = null;
    }

    /**
     * Creates a new <code>DataHolder</code> object.
     *
     * @param pathString dedicated path within sheet internals
     */
    public DataHolder (String pathString)
    {
        this.pathString = pathString;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // discard //
    //---------//
    /**
     * This data will be purged from disk when sheet is stored.
     */
    public void discard ()
    {
        discarded = true;
        hasNoData = true;

        // data is left untouched, to not disturb any view opened on this data
    }

    //---------//
    // getData //
    //---------//
    /**
     * Return the handled data.
     *
     * @param stub the related sheet stub instance (to use book lock)
     * @return the data, ready to use
     */
    public T getData (SheetStub stub)
    {
        if (data == null) {
            if (hasNoData) {
                return null;
            }

            final Book book = stub.getBook();

            try {
                book.getLock().lock();

                if (data == null) {
                    if (book.getBookPath() != null) {
                        // Open book file system
                        Path path = book.openSheetFolder(stub.getNumber()).resolve(pathString);
                        logger.debug("path: {}", path);

                        if (Files.exists(path)) {
                            try (InputStream is = Files.newInputStream(
                                    path,
                                    StandardOpenOption.READ)) {
                                data = load(is);
                                logger.debug("Loaded {}", path);
                            }
                        } else {
                            logger.debug("No {}", path);
                            hasNoData = true;
                        }

                        path.getFileSystem().close(); // Close book file system
                        setModified(false);
                    } else {
                        logger.debug("No bookpath for{}", book);
                        hasNoData = true;
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error reading data from " + pathString, ex);
            } finally {
                book.getLock().unlock();
            }
        }

        return data;
    }

    //--------------//
    // hasDataReady //
    //--------------//
    /**
     * Tell whether data is available in memory.
     *
     * @return true if available
     */
    public boolean hasDataReady ()
    {
        return data != null;
    }

    //-----------//
    // hasNoData //
    //-----------//
    /**
     * Tell whether there is no data at all (even on disk).
     *
     * @return true if no data at all is available
     */
    public boolean hasNoData ()
    {
        return hasNoData;
    }

    //-------------//
    // isDiscarded //
    //-------------//
    /**
     * Report whether disk data should be removed.
     *
     * @return true if discarded
     */
    public boolean isDiscarded ()
    {
        return discarded;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Tell whether the disk value does not exist or is different from memory value.
     *
     * @return true if modified
     */
    public boolean isModified ()
    {
        return modified;
    }

    //------//
    // load //
    //------//
    /**
     * Load data from the provided input stream.
     *
     * @param is provided input stream
     * @return the loaded data
     * @throws Exception if anything goes wrong
     */
    protected abstract T load (InputStream is)
        throws Exception;

    //------------//
    // removeData //
    //------------//
    /**
     * Remove data from book project file.
     * <p>
     * NOTA: This method assumes the containing book is properly locked.
     *
     * @param sheetFolder path to sheet folder
     */
    public void removeData (Path sheetFolder)
    {
        final Path path = sheetFolder.resolve(pathString);

        try {
            if (Files.deleteIfExists(path)) {
                logger.info("Removed {}", path);
            }
        } catch (Exception ex) {
            logger.warn("Error in removeData " + ex, ex);
        }
    }

    //---------//
    // setData //
    //---------//
    /**
     * Assign the data.
     *
     * @param data     the data to be hold
     * @param modified is this data modified with respect to disk version
     */
    public void setData (T data,
                         boolean modified)
    {
        this.data = data;
        setModified(modified);

        if (data != null) {
            hasNoData = false;
        }
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Set the modified value with respect to disk
     *
     * @param bool the new modified value
     */
    public void setModified (boolean bool)
    {
        if (!bool) {
            logger.debug("{} setModified:false", this);
        }

        modified = bool;
    }

    //-------//
    // store //
    //-------//
    /**
     * Store data to the provided output stream.
     *
     * @param os provided output stream
     * @throws Exception if anything goes wrong
     */
    protected abstract void store (OutputStream os)
        throws Exception;

    //-----------//
    // storeData //
    //-----------//
    /**
     * Store data to book project file.
     * <p>
     * NOTA: This method assumes the containing book is properly locked.
     *
     * @param sheetFolder    path to sheet folder
     * @param oldSheetFolder (optional) path to previous sheet folder for retrieval
     * @return true if OK
     */
    public boolean storeData (Path sheetFolder,
                              Path oldSheetFolder)
    {
        final Path path = sheetFolder.resolve(pathString);
        boolean ok = true;

        try {
            if (!hasDataReady()) {
                if (oldSheetFolder != null) {
                    // Copy from old book file to new
                    Path oldPath = oldSheetFolder.resolve(pathString);

                    if (Files.exists(oldPath) && !Files.exists(path)) {
                        Files.copy(oldPath, path);
                        logger.info("Copied {}", path);
                    }
                }
            } else if (modified) {
                Files.deleteIfExists(path);

                try (OutputStream os = Files.newOutputStream(path, CREATE);) {
                    store(os);
                    os.flush();
                    setModified(false);
                    logger.info("Stored {}", path);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in storeData " + ex, ex);
            ok = false;
        }

        return ok;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('{');

        if (pathString != null) {
            sb.append(pathString);
        }

        sb.append(" discarded:").append(discarded);
        sb.append(" hasNoData:").append(hasNoData);
        sb.append(" modified:").append(modified);
        sb.append(" data:").append(data);

        sb.append('}');

        return sb.toString();
    }
}
