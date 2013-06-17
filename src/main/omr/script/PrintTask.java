//----------------------------------------------------------------------------//
//                                                                            //
//                             P r i n t T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.ScoresManager;

import omr.sheet.Sheet;

import java.io.File;

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
    //~ Instance fields --------------------------------------------------------

    /** The file used for print */
    @XmlAttribute
    private String path;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // PrintTask //
    //------------//
    /**
     * Create a task to print the score to a PDF file
     *
     * @param path the full path of the PDF file
     */
    public PrintTask (String path)
    {
        this.path = path;
    }

    //------------//
    // PrintTask //
    //------------//
    /** No-arg constructor needed by JAXB */
    private PrintTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        ScoresManager.getInstance()
                .writePhysicalPdf(
                sheet.getScore(),
                (path != null) ? new File(path) : null);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " print " + path + super.internalsString();
    }
}
