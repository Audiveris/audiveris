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

import omr.sheet.BookManager;
import omr.sheet.Sheet;

import java.nio.file.Path;
import java.nio.file.Paths;

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

    /** The path used for print */
    @XmlAttribute(name = "path")
    private String pathString;

    //~ Constructors -------------------------------------------------------------------------------
    //------------//
    // PrintTask //
    //------------//
    /**
     * Create a task to print the score to a PDF file
     *
     * @param path the full path of the PDF file
     */
    public PrintTask (Path path)
    {
        setPath(path.toString());
    }

    //------------//
    // PrintTask //
    //------------//
    /** No-arg constructor needed by JAXB */
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
        BookManager.getInstance().writePhysicalPdf(sheet.getBook(), getPath());
    }

    public Path getPath ()
    {
        return Paths.get(pathString);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" print ").append(pathString);

        return sb.toString();
    }

    private void setPath (String pathString)
    {
        this.pathString = pathString;
    }
}
