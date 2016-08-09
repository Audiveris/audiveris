//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S c o r e T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.script;

import omr.log.LogUtil;

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
        Book book = sheet.getStub().getBook();

        try {
            LogUtil.start(book);

            for (SheetStub stub : book.getValidStubs()) {
                LogUtil.start(stub);
                stub.reachStep(Step.PAGE, false);
                LogUtil.stopStub();
            }

            book.buildScores();
        } catch (Exception ex) {
            logger.warn("Could not build book score(s), {}", ex, ex);
        } finally {
            LogUtil.stopBook();
        }
    }
}
