//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E x p o r t T a s k                                       //
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

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.ExportPattern;
import org.audiveris.omr.sheet.Sheet;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ExportTask} exports score entities to a MusicXML file
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "export")
public class ExportTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The full target file used for export. */
    @XmlAttribute
    private Path file;

    /** The target folder used for export. */
    @XmlAttribute
    private Path folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to export the related score entities of a book, providing either
     * the full target file or just the target folder (and using default file name).
     *
     * @param file   the full path of export file, or null
     * @param folder the full path of export folder, or null
     */
    public ExportTask (Path file,
                       Path folder)
    {
        this.file = file;
        this.folder = folder;
    }

    /** No-arg constructor needed by JAXB. */
    private ExportTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    public void core (Book book)
    {
        Path exportPath = (file != null) ? file
                : ((folder != null) ? folder.resolve(book.getRadix()) : null);
        book.setExportPathSansExt(ExportPattern.getPathSansExt(exportPath));
        book.export();
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
        sb.append(" export");

        if (file != null) {
            sb.append(" file=").append(file);
        }

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
