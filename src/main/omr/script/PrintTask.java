//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P r i n t T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.OMR;

import omr.sheet.Book;
import omr.sheet.Sheet;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PrintTask} prints a score to a PDFfile
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "print")
public class PrintTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The full target file used for print. */
    @XmlAttribute(name = "file")
    private Path file;

    /** The target folder used for print. */
    @XmlAttribute(name = "folder")
    private Path folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to print the score to a PDF file, providing either the full target
     * file or just the target folder (and using default file name).
     *
     * @param file   the full path of the PDF file, or null
     * @param folder the full path of the PDF file folder, or null
     */
    public PrintTask (Path file,
                      Path folder)
    {
        this.file = file;
        this.folder = folder;
    }

    /** No-arg constructor needed by JAXB. */
    private PrintTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        Book book = sheet.getBook();
        Path bookPath = (file != null) ? file
                : ((folder != null) ? folder.resolve(book.getRadix() + OMR.PDF_EXTENSION)
                        : null);
        book.setPrintPath(bookPath);
        book.print();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" print ");

        if (file != null) {
            sb.append(" file=").append(file);
        }

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
