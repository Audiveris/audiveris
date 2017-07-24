//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F i l e D r o p H a n d l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.Param;
import org.audiveris.omr.util.VoidTask;

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

                if (fileName.endsWith(OMR.BOOK_EXTENSION)) {
                    new DropBookTask(file).execute();
                } else if (fileName.endsWith("-" + SampleRepository.SAMPLES_FILE_NAME)) {
                    new DropSamplesTask(file).execute();
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

    //--------------//
    // DropBookTask //
    //--------------//
    private static class DropBookTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        private Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public DropBookTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            book = OMR.engine.loadBook(file.toPath());
            book.createStubsTabs(null); // Tabs are now accessible

            return null;
        }

        @Override
        protected void finished ()
        {
            // Focus UI on book just dropped
            SheetStub firstValid = book.getFirstValidStub();

            if (firstValid != null) {
                StubsController.getInstance().selectAssembly(firstValid);
            }
        }
    }

    //---------------//
    // DropInputTask //
    //---------------//
    private static class DropInputTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        private final Step dropStep;

        private Book book;

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
                book = OMR.engine.loadInput(file.toPath());
                LogUtil.start(book);
                book.createStubs(null);
                book.createStubsTabs(null); // Tabs are now accessible

                // If a specific drop target is specified, run it on book as a whole
                // Otherwise run the early target on first stub only.
                if (dropStep != null) {
                    book.reachBookStep(dropStep, false, null);
                }

                return null;
            } finally {
                LogUtil.stopBook();
            }
        }

        @Override
        protected void finished ()
        {
            // Focus UI on input just dropped
            SheetStub firstValid = book.getFirstValidStub();

            if (firstValid != null) {
                StubsController.getInstance().selectAssembly(firstValid);
            }
        }
    }

    //-----------------//
    // DropSamplesTask //
    //-----------------//
    private static class DropSamplesTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        public DropSamplesTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            SampleRepository global = SampleRepository.getGlobalInstance();
            global.includeSamplesFile(file.toPath());

            return null;
        }
    }
}
