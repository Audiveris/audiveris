//----------------------------------------------------------------------------//
//                                                                            //
//                          A b s t r a c t S t e p                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Abstract class {@code AbstractStep} provides a convenient basis for {@link
 * Step} implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStep
    implements Step
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(AbstractStep.class);

    //~ Instance fields --------------------------------------------------------

    /** Step name */
    protected final String name;

    /** Score level only, or sheet level possible? */
    protected final Level level;

    /** Is the step mandatory? */
    protected final Mandatory mandatory;

    /** Is the step repeatable at will? */
    protected final Redoable redoable;

    /** Related short label */
    protected final String label;

    /** Description of the step */
    protected final String description;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AbstractStep object.
     *
     * @param name the step name
     * @param level score level only or sheet level
     * @param mandatory step must be done before any output
     * @param redoable step can be redone at will
     * @param label The title of the related (or most relevant) view tab
     * @param description A step description for the end user
     */
    public AbstractStep (String    name,
                         Level     level,
                         Mandatory mandatory,
                         Redoable  redoable,
                         String    label,
                         String    description)
    {
        this.name = name;
        this.level = level;
        this.mandatory = mandatory;
        this.redoable = redoable;
        this.label = label;
        this.description = description;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a description of the step
     * @return a short description
     */
    public String getDescription ()
    {
        return description;
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Check whether this task has been done
     * @return true if started/done, false otherwise
     */
    public boolean isDone (Sheet sheet)
    {
        return sheet.isDone(this);
    }

    //-------------//
    // isMandatory //
    //-------------//
    /** Is the step mandatory? */
    public boolean isMandatory ()
    {
        return mandatory == Step.Mandatory.MANDATORY;
    }

    //---------//
    // getName //
    //---------//
    /** Name of the step */
    public String getName ()
    {
        return name;
    }

    //------------//
    // isRedoable //
    //------------//
    /** Is the step repeatable at will? */
    public boolean isRedoable ()
    {
        return redoable == Step.Redoable.REDOABLE;
    }

    //--------------//
    // isScoreLevel //
    //--------------//
    public boolean isScoreLevel ()
    {
        return level == Step.Level.SCORE_LEVEL;
    }

    //-----------//
    // isStarted //
    //-----------//
    /**
     * Check whether this task has started
     * @return true if started, false otherwise
     */
    public boolean isStarted (Sheet sheet,
                              Score score)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Related short tab label */
    public String getTab ()
    {
        return label;
    }

    //-----------//
    // displayUI //
    //-----------//
    /** Make the related user interface visible for this step */
    public void displayUI (Sheet sheet)
    {
        // Void by default
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Run the step
     * @param systems systems to process (null means all systems)
     * @throws StepException raised if processing failed
     */
    public void doStep (Collection<SystemInfo> systems,
                        Sheet                  sheet)
        throws StepException
    {
        if (logger.isFineEnabled()) {
            logger.fine(sheet.getLogPrefix() + "Starting " + this);
        }

        started(sheet);
        doit(systems, sheet);
        done(sheet);

        if (logger.isFineEnabled()) {
            logger.fine(sheet.getLogPrefix() + "Finished " + this);
        }
    }

    //------//
    // done //
    //------//
    /**
     * Flag this step as done
     */
    public void done (Sheet sheet)
    {
        sheet.done(this);
    }

    //---------//
    // started //
    //---------//
    /**
     * Flag this step as started
     */
    public void started (Sheet sheet)
    {
        sheet.setCurrentStep(this);
    }

    //--------------//
    // toLongString //
    //--------------//
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder("{Step");
        sb.append(" ")
          .append(name);
        sb.append(" ")
          .append(level);
        sb.append(" ")
          .append(mandatory);
        sb.append(" ")
          .append(redoable);
        sb.append(" label:")
          .append(label);
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return name;
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step. This method must be defined for any concrete
     * Step.
     * @param systems the collection of systems to process, or null
     * @param sheet the related sheet
     * @throws StepException raised if processing failed
     */
    protected abstract void doit (Collection<SystemInfo> systems,
                                  Sheet                  sheet)
        throws StepException;
}
