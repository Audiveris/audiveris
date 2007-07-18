//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p T a s k                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.sheet.Sheet;

import omr.step.Step;
import omr.step.StepException;

import javax.xml.bind.annotation.*;

/**
 * Class <code>StepTask</code> is a script task which performs a step on a sheet
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class StepTask
    extends Task
{
    //~ Instance fields --------------------------------------------------------

    /** The step launched */
    @XmlAttribute(name = "name")
    private Step step;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // StepTask //
    //----------//
    /**
     * Create a task to apply a given step to the related sheet
     *
     * @param step the step to apply
     */
    public StepTask (Step step)
    {
        this.step = step;
    }

    //----------//
    // StepTask //
    //----------//
    /** No-arg constructor needed by JAXB */
    private StepTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    @Override
    public void run (Sheet sheet)
        throws StepException
    {
        super.run(sheet);

        if (!sheet.getSheetSteps()
                  .isDone(step)) {
            //step.performSerial(sheet, null);
            sheet.getSheetSteps()
                 .doit(step);
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " step " + step;
    }
}
