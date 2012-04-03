//----------------------------------------------------------------------------//
//                                                                            //
//                       F i l e D r o p H a n d l e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BasicTask;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 * Class {@code FileDropHandler} handles the dropping of files onto the
 * Audiveris GUI
 *
 * @author Hervé Bitteur
 */
public class FileDropHandler
    extends TransferHandler
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        FileDropHandler.class);

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // canImport //
    //-----------//
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canImport (TransferSupport support)
    {
        /* For the time being, only support drops (not clipboard paste) */
        if (!support.isDrop()) {
            return false;
        }

        /* Check that the drop contains a list of files */
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }

        /* Check to see if the source actions contains the COPY action
         */
        boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

        /* If COPY is supported, choose COPY and accept the transfer */
        if (copySupported) {
            support.setDropAction(COPY);

            return true;
        }

        /* COPY isn't supported, so reject the transfer */
        return false;
    }

    //----------------//
    // getDefaultStep //
    //----------------//
    /**
     * Report the current default step on DnD
     * @return the current default step
     */
    public static Step getDefaultStep ()
    {
        return constants.defaultStep.getValue();
    }

    //------------//
    // importData //
    //------------//
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importData (TransferSupport support)
    {
        if (!canImport(support)) {
            return false;
        }

        /* Fetch the Transferable */
        Transferable trsf = support.getTransferable();

        try {
            /* Fetch data */
            Object data = trsf.getTransferData(DataFlavor.javaFileListFlavor);
            java.util.List fileList = (java.util.List) data;

            /* Loop through the files */
            for (Object obj : fileList) {
                new DropTask((File) obj, getDefaultStep()).execute();
            }
        } catch (UnsupportedFlavorException ex) {
            logger.warning("Unsupported flavor in drag & drop", ex);

            return false;
        } catch (IOException ex) {
            logger.warning("IO Exception in drag & drop", ex);

            return false;
        }

        return true;
    }

    //----------------//
    // setDefaultStep //
    //----------------//
    /**
     * Assign the new default step on DnD
     * @param step the new default step
     */
    public static void setDefaultStep (Step step)
    {
        if (step != getDefaultStep()) {
            logger.info("Default drop step is now " + step);
            constants.defaultStep.setValue(step);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Steps.Constant defaultStep = new Steps.Constant(
            Steps.valueOf(Steps.LOAD),
            "Default step executed when a file is dropped");
    }

    //----------//
    // DropTask //
    //----------//
    private static class DropTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;
        private final Step target;

        //~ Constructors -------------------------------------------------------

        public DropTask (File file,
                         Step target)
        {
            this.file = file;
            this.target = target;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws Exception
        {
            logger.info("Dropping file " + file);

            Score score = new Score(file);
            Stepping.processScore(Collections.singleton(target), score);

            return null;
        }
    }
}
