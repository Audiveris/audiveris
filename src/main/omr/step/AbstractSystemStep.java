//----------------------------------------------------------------------------//
//                                                                            //
//                    A b s t r a c t S y s t e m S t e p                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Class {@code AbstractSystemStep} is a basis for any step working in
 * parallel on the sheet systems.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractSystemStep
        extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AbstractSystemStep.class);

    //~ Constructors -----------------------------------------------------------
    //--------------------//
    // AbstractSystemStep //
    //--------------------//
    /**
     * Creates a new AbstractSystemStep object.
     *
     * @param level       score level only or sheet level
     * @param mandatory   step must be done before any output
     * @param label       The title of the related (or most relevant) view tab
     * @param description A step description for the end user
     */
    public AbstractSystemStep (String name,
                               Level level,
                               Mandatory mandatory,
                               String label,
                               String description)
    {
        super(name, level, mandatory, label, description);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------//
    // clearErrors //
    //-------------//
    @Override
    public void clearErrors (Sheet sheet)
    {
        // Void, since this is done system per system
    }

    //-------------------//
    // clearSystemErrors //
    //-------------------//
    /**
     * Clear the errors of just the provided system
     *
     * @param system the system to clear of errors
     */
    protected void clearSystemErrors (SystemInfo system)
    {
        if (Main.getGui() != null) {
            system.getSheet().getErrorsEditor().clearSystem(this, system.getId());
        }
    }

    //----------//
    // doSystem //
    //----------//
    /**
     * Actually perform the step on the given system. This method must be
     * actually defined for any concrete system step.
     *
     * @param system the system to process
     * @throws StepException raised if processing failed
     */
    public abstract void doSystem (SystemInfo system)
            throws StepException;

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method is run when this step is explicitly selected
     *
     * @param systems systems to process (null means all systems)
     * @param sheet   the sheet to process
     * @throws StepException raised if processing failed
     */
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        // Preliminary actions
        doProlog(systems, sheet);

        // Processing system per system
        doitPerSystem(systems, sheet);

        // Final actions
        doEpilog(systems, sheet);
    }

    //----------//
    // doEpilog //
    //----------//
    /**
     * Final processing for this step, once all systems have been
     * processed.
     *
     * @param systems the systems which have been updated
     * @throws StepException raised if processing failed
     */
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        // Empty by default
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * Do preliminary common work before all systems processings are
     * launched in parallel.
     *
     * @param systems the systems which will be updated
     * @throws StepException raised if processing failed
     */
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        // Empty by default
    }

    //---------------//
    // doitPerSystem //
    //---------------//
    /**
     * Launch the system processing in parallel, one task per system
     *
     * @param systems the systems to process
     * @param sheet   the containing sheet
     */
    private void doitPerSystem (Collection<SystemInfo> systems,
                                final Sheet sheet)
    {
        try {
            Collection<Callable<Void>> tasks = new ArrayList<>();

            if (systems == null) {
                systems = sheet.getSystems();
            }

            for (SystemInfo info : systems) {
                final SystemInfo system = info;
                tasks.add(
                        new Callable<Void>()
                {
                    @Override
                    public Void call ()
                            throws Exception
                    {
                        try {
                            logger.debug("{} doSystem #{}",
                                    AbstractSystemStep.this,
                                    system.idString());

                            doSystem(system);
                        } catch (Exception ex) {
                            logger.warn(sheet.getLogPrefix()
                                        + "Interrupt on "
                                        + system.idString(),
                                    ex);
                        }

                        return null;
                    }
                });
            }

            // Launch all system tasks in parallel and wait for their completion
            OmrExecutors.getLowExecutor().invokeAll(tasks);
        } catch (InterruptedException ex) {
            logger.warn("doitPerSystem got interrupted");
            throw new ProcessingCancellationException(ex);
        }
    }
}
