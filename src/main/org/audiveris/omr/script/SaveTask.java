//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S a v e T a s k                                        //
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
import org.audiveris.omr.sheet.Sheet;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SaveTask} saves book on disk.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "save")
public class SaveTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The full target file. */
    @XmlAttribute(name = "file")
    private Path file;

    /** Just the target folder. */
    @XmlAttribute(name = "folder")
    private Path folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to save a book, providing either the full target
     * file or just the target folder (and using default file name).
     *
     * @param file   the full target file, or null
     * @param folder just the target folder, or null
     */
    public SaveTask (Path file,
                     Path folder)
    {
        this.file = file;
        this.folder = folder;
    }

    /** No-arg constructor needed by JAXB. */
    private SaveTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    public void core (Book book)
    {
        if (book.isModified()) {
            Path bookPath = (file != null) ? file
                    : ((folder != null)
                            ? Paths.get(folder.toString(), book.getRadix() + OMR.BOOK_EXTENSION)
                            : null);

            if (bookPath == null) {
                bookPath = BookManager.getDefaultSavePath(book);
            }

            book.store(bookPath, false);
        } else {
            logger.info("No need to save {}", book);
        }
    }

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        core(sheet.getStub().getBook());
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" save");

        if (file != null) {
            sb.append(" file=").append(file);
        }

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
