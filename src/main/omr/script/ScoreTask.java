//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S c o r e T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;

import omr.step.Step;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ScoreTask} builds the score(s) of a book.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "score")
public class ScoreTask
        extends ScriptTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /** No-arg constructor needed by JAXB. */
    public ScoreTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        Book book = sheet.getBook();

        try {
            for (SheetStub stub : book.getValidStubs()) {
                stub.ensureStep(Step.PAGE);
            }

            book.buildScores();
        } catch (Exception ex) {
            logger.warn("Could not build score(s) for book, " + ex, ex);
        }
    }
}
