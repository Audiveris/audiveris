//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t S t e p T a s k                                   //
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

import org.audiveris.omr.log.LogUtil;
import static org.audiveris.omr.script.ScriptTask.logger;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SheetStepTask} performs a step on a single sheet.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sheet-step")
@XmlAccessorType(XmlAccessType.NONE)
public class SheetStepTask
        extends SheetTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The step launched */
    @XmlAttribute(name = "name")
    private Step step;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to apply a given step to the related sheet.
     *
     * @param sheet the sheet to process
     * @param step  the step to apply
     */
    public SheetStepTask (Sheet sheet,
                          Step step)
    {
        super(sheet);
        this.step = step;
    }

    /** No-arg constructor needed by JAXB. */
    private SheetStepTask ()
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
        try {
            LogUtil.start(sheet.getStub());
            sheet.getStub().reachStep(step, true);
            logger.info("End of sheet step {}", step);
            sheet.getStub().getBook().getScript().addTask(this);
        } finally {
            LogUtil.stopStub();
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" sheet-step ").append(step);

        return sb.toString();
    }
}
