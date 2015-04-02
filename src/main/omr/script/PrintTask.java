//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P r i n t T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Book;
import omr.sheet.Sheet;

import java.io.File;
import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code PrintTask} prints a score to a PDFfile
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PrintTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The folder used for print. */
    @XmlAttribute(name = "folder")
    private File folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to print the score to a PDF file
     *
     * @param folder the full path of the PDF file folder
     */
    public PrintTask (File folder)
    {
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
        Path bookPath = (folder != null) ? new File(folder, book.getRadix()).toPath() : null;
        book.setPrintPath(bookPath);
        ///BookManager.getInstance().printBook(sheet.getBook(), bookPath);
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

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
