//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A b s t r a c t S y s t e m S t e p                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.step;

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Abstract class {@code AbstractSystemStep} is a basis for any step working on the
 * sheet systems, perhaps in parallel.
 *
 * @param <C> context type
 * @author Hervé Bitteur
 */
public abstract class AbstractSystemStep<C>
        extends AbstractStep
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractSystemStep.class);

    /**
     * Creates a new AbstractSystemStep object.
     */
    public AbstractSystemStep ()
    {
    }

    //-------------//
    // clearErrors //
    //-------------//
    @Override
    public void clearErrors (Step step,
                             Sheet sheet)
    {
        // Void, since this is done system per system
    }

    //----------//
    // doSystem //
    //----------//
    /**
     * Actually perform the step on the given system.
     * This method must be actually defined for any concrete system step.
     *
     * @param system  the system to process
     * @param context the sheet context
     * @throws StepException raised if processing failed
     */
    public abstract void doSystem (SystemInfo system,
                                   C context)
            throws StepException;

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method is run when this step is explicitly selected
     *
     * @param sheet the sheet to process
     * @throws StepException raised if processing failed
     */
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Preliminary actions
        final C context = doProlog(sheet);

        // Processing system per system
        doitPerSystem(sheet, context);

        // Final actions
        doEpilog(sheet, context);
    }

    //-------------------//
    // clearSystemErrors //
    //-------------------//
    /**
     * Clear the errors of just the provided system
     *
     * @param step   the step we are interested in
     * @param system the system to clear of errors
     */
    protected void clearSystemErrors (Step step,
                                      SystemInfo system)
    {
        if (OMR.gui != null) {
            system.getSheet().getErrorsEditor().clearSystem(step, system.getId());
        }
    }

    //----------//
    // doEpilog //
    //----------//
    /**
     * Final processing for this step, once all systems have been processed.
     *
     * @param sheet   the containing sheet
     * @param context the sheet context
     * @throws StepException raised if processing failed
     */
    protected void doEpilog (Sheet sheet,
                             C context)
            throws StepException
    {
        // Empty by default
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * Do preliminary common work before all systems processing are launched in parallel.
     *
     * @param sheet the containing sheet
     * @return the created sheet context
     * @throws StepException raised if processing failed
     */
    protected C doProlog (Sheet sheet)
            throws StepException
    {
        // Empty by default
        return null;
    }

    //---------------//
    // doitPerSystem //
    //---------------//
    /**
     * Launch the system processing (perhaps in parallel, one task per system)
     *
     * @param systems the systems to process
     * @param sheet   the containing sheet
     */
    private void doitPerSystem (final Sheet sheet,
                                final C context)
    {
        try {
            final boolean parallel = Main.processSystemsInParallel();
            final Collection<Callable<Void>> tasks = new ArrayList<>();

            for (final SystemInfo system : sheet.getSystems()) {
                tasks.add(new Callable<Void>()
                {
                    @Override
                    public Void call ()
                            throws Exception
                    {
                        // If run on a separate thread (case of parallel), we have to set/unset log
                        // If not, let's not unset log (it may be needed in following epilog)
                        try {
                            if (parallel) {
                                LogUtil.start(sheet.getStub());
                            }

                            logger.debug(
                                    "{} doSystem #{}",
                                    AbstractSystemStep.this,
                                    system.getId());

                            doSystem(system, context);
                        } catch (StepException ex) {
                            logger.warn(system.getLogPrefix() + ex, ex);
                        } finally {
                            if (parallel) {
                                LogUtil.stopStub();
                            }
                        }

                        return null;
                    }
                });
            }

            // Process all systems
            if (parallel) {
                // In parallel
                OmrExecutors.getLowExecutor().invokeAll(tasks);
            } else {
                // In sequence
                for (Callable<Void> task : tasks) {
                    task.call();
                }
            }
        } catch (InterruptedException ex) {
            logger.warn("doitPerSystem got interrupted for {}", this);
            throw new ProcessingCancellationException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
