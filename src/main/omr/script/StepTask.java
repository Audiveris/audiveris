//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p T a s k                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
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
    extends ScriptTask
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

    //------//
    // core //
    //------//
    @Override
    public void core (final Sheet sheet)
        throws StepException
    {
        if (!sheet.getSheetSteps()
                  .isDone(step)) {
            step.performUntil(sheet);
        } else if (logger.isFineEnabled()) {
            logger.fine(this + " already done");
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " step " + step + super.internalsString();
    }

    //--------------//
    // isRecordable //
    //--------------//
    /**
     * This is an implementation trick, because of a "chicken and egg problem"
     * to allow to run the LOAD step while the sheet does not exist yet!
     * @return false!
     */
    @Override
    boolean isRecordable ()
    {
        return false;
    }
}
