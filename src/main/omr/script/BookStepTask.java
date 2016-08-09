//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B o o k S t e p T a s k                                    //
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

import omr.sheet.Book;
import omr.sheet.Sheet;

import omr.step.Step;
import omr.step.StepException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BookStepTask} performs a step on a whole book.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "book-step")
public class BookStepTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The step launched */
    @XmlAttribute(name = "name")
    private Step step;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to apply a given step to the related book.
     *
     * @param step the step to apply
     */
    public BookStepTask (Step step)
    {
        this.step = step;
    }

    /** No-arg constructor needed by JAXB. */
    private BookStepTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (final Sheet sheet)
            throws StepException
    {
        final Book book = sheet.getStub().getBook();
        book.reachBookStep(step, false, null);
        logger.info("End of book step {}", step);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" book-step ").append(step);

        return sb.toString();
    }
}
