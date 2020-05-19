//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F i l e D r o p H a n d l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.Step;
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

    private static final Logger logger = LoggerFactory.getLogger(FileDropHandler.class);

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
            @SuppressWarnings("unchecked")
            java.util.List<File> fileList = (java.util.List<File>) data;

            /* Loop through the files */
            for (File file : fileList) {
                final String fileName = file.getName();

                if (fileName.endsWith(OMR.BOOK_EXTENSION)) {
                    new DropBookTask(file).execute();
                } else if (fileName.endsWith("-" + SampleRepository.SAMPLES_FILE_NAME)) {
                    new DropSamplesTask(file).execute();
                } else {
                    new DropInputTask(file, StubsController.getEarlyStep()).execute();
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

    //--------------//
    // DropBookTask //
    //--------------//
    private static class DropBookTask
            extends VoidTask
    {

        private final File file;

        private Book book;

        DropBookTask (File file)
        {
            this.file = file;
        }

        @Override
        protected Void doInBackground ()
                throws Exception
        {
            book = OMR.engine.loadBook(file.toPath());

            if (book != null) {
                book.createStubsTabs(null); // Tabs are now accessible
            }

            return null;
        }

        @Override
        protected void finished ()
        {
            if (book != null) {
                // Focus UI on book just dropped
                SheetStub firstValid = book.getFirstValidStub();

                if (firstValid != null) {
                    StubsController.getInstance().selectAssembly(firstValid);
                }
            }
        }
    }

    //---------------//
    // DropInputTask //
    //---------------//
    private static class DropInputTask
            extends VoidTask
    {

        private final File file;

        private final Step dropStep;

        private Book book;

        DropInputTask (File file,
                       Step dropStep)
        {
            this.file = file;
            this.dropStep = dropStep;
        }

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

        private final File file;

        DropSamplesTask (File file)
        {
            this.file = file;
        }

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
