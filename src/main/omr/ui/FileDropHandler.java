//----------------------------------------------------------------------------//
//                                                                            //
//                       F i l e D r o p H a n d l e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.constant.ConstantSet;

import omr.score.Score;

import omr.script.ScriptManager;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BasicTask;
import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(
            FileDropHandler.class);

    /** Default parameter. */
    public static final Param<Step> defaultStep = new Default();

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

        /* Check to see if the source actions contains the COPY action */
        boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;

        /* If COPY is supported, choose COPY and accept the transfer */
        if (copySupported) {
            support.setDropAction(COPY);

            return true;
        }

        /* COPY isn't supported, so reject the transfer */
        return false;
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
            java.util.List<File> fileList = (java.util.List<File>) data;

            /* Loop through the files */
            for (File file : fileList) {
                if (file.getName()
                        .endsWith(ScriptManager.SCRIPT_EXTENSION)) {
                    new DropScriptTask(file).execute();
                } else {
                    new DropImageTask(file, defaultStep.getTarget()).execute();
                }
            }
        } catch (UnsupportedFlavorException ex) {
            logger.warn("Unsupported flavor in drag & drop", ex);

            return false;
        } catch (IOException ex) {
            logger.warn("IO Exception in drag & drop", ex);

            return false;
        }

        return true;
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
                "Default step launched when an image file is dropped");

    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Step>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Step getSpecific ()
        {
            return constants.defaultStep.getValue();
        }

        @Override
        public boolean setSpecific (Step specific)
        {
            if (!getSpecific()
                    .equals(specific)) {
                constants.defaultStep.setValue(specific);
                logger.info("Default drop step is now ''{}''", specific);

                return true;
            }

            return false;
        }
    }

    //---------------//
    // DropImageTask //
    //---------------//
    private static class DropImageTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;

        private final Step target;

        //~ Constructors -------------------------------------------------------
        public DropImageTask (File file,
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
            logger.info("Dropping image file {}", file);

            Score score = new Score(file);
            final Step loadStep = Steps.valueOf(Steps.LOAD);

            if (target.equals(loadStep)) {
                Stepping.processScore(Collections.EMPTY_SET, null, score);
            } else {
                Stepping.processScore(
                        Collections.singleton(target),
                        null,
                        score);
            }

            return null;
        }
    }

    //----------------//
    // DropScriptTask //
    //----------------//
    private static class DropScriptTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;

        //~ Constructors -------------------------------------------------------
        public DropScriptTask (File file)
        {
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            ScriptManager.getInstance()
                    .loadAndRun(file);

            return null;
        }
    }
}
