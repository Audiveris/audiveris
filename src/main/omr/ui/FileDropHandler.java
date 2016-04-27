//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F i l e D r o p H a n d l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.OMR;

import omr.constant.ConstantSet;

import omr.log.LogUtil;

import omr.script.ScriptManager;

import omr.sheet.Book;

import omr.step.Step;

import omr.util.BasicTask;
import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 * Class {@code FileDropHandler} handles the dropping of files onto the
 * main application window.
 *
 * @author Hervé Bitteur
 */
public class FileDropHandler
        extends TransferHandler
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FileDropHandler.class);

    /** Default step to be run on dropped image. */
    public static final Param<Step> defaultStep = new Default();

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // canImport //
    //-----------//
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
                final String fileName = file.getName();

                if (fileName.endsWith(OMR.SCRIPT_EXTENSION)) {
                    new DropScriptTask(file).execute();
                } else if (fileName.endsWith(OMR.PROJECT_EXTENSION)) {
                    new DropProjectTask(file).execute();
                } else {
                    new DropInputTask(file, defaultStep.getTarget()).execute();
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Step.Constant dropStep = new Step.Constant(
                null,
                "Default step launched when an image file is dropped");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Step>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Step getSpecific ()
        {
            return constants.dropStep.getValue();
        }

        @Override
        public boolean setSpecific (Step specific)
        {
            final Step oldSpecific = getSpecific();

            if (((oldSpecific == null) && (specific != null)) || !oldSpecific.equals(specific)) {
                constants.dropStep.setValue(specific);
                logger.info("Default drop step is now ''{}''", specific);

                return true;
            }

            return false;
        }
    }

    //---------------//
    // DropInputTask //
    //---------------//
    private static class DropInputTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        private final Step dropStep;

        //~ Constructors ---------------------------------------------------------------------------
        public DropInputTask (File file,
                              Step dropStep)
        {
            this.file = file;
            this.dropStep = dropStep;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            logger.info("Dropping input file {}", file);

            try {
                final Book book = OMR.engine.loadInput(file.toPath());
                LogUtil.start(book);
                book.createStubs(null);
                book.createStubsTabs(); // Tabs are now accessible

                // If a specific drop target is specified, run it on book as a whole
                // Otherwise run the early target on first stub only.
                if (dropStep != null) {
                    book.doStep(dropStep, null);
                }

                return null;
            } finally {
                LogUtil.stopBook();
            }
        }
    }

    //-----------------//
    // DropProjectTask //
    //-----------------//
    private static class DropProjectTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        public DropProjectTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            Book book = OMR.engine.loadProject(file.toPath());
            book.createStubsTabs(); // Tabs are now accessible

            return null;
        }
    }

    //----------------//
    // DropScriptTask //
    //----------------//
    private static class DropScriptTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        public DropScriptTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            ScriptManager.getInstance().loadAndRun(file, false);

            return null;
        }
    }
}
