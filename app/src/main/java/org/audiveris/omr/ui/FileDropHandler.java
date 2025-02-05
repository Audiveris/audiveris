//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F i l e D r o p H a n d l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sheet.ui.BookActions.LoadBookTask;
import org.audiveris.omr.sheet.ui.BookActions.LoadImageTask;
import org.audiveris.omr.util.PathTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.TransferHandler;

/**
 * Class <code>FileDropHandler</code> handles the dropping of files onto the
 * main application window.
 *
 * @author Hervé Bitteur
 */
public class FileDropHandler
        extends TransferHandler
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FileDropHandler.class);

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
        if (!copySupported) {
            return false;
        }

        support.setDropAction(COPY);

        return true;
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
                final Path path = file.toPath();
                final String fileName = file.getName();

                if (fileName.endsWith(OMR.BOOK_EXTENSION)) {
                    new LoadBookTask(path).execute();
                } else if (fileName.endsWith("-" + SampleRepository.SAMPLES_FILE_NAME)) {
                    new DropSamplesTask(path).execute();
                } else {
                    new LoadImageTask(path).execute();
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

    //-----------------//
    // DropSamplesTask //
    //-----------------//
    private static class DropSamplesTask
            extends PathTask<Void, Void>
    {
        DropSamplesTask (Path path)
        {
            super(path);
        }

        @Override
        protected Void doInBackground ()
            throws Exception
        {
            SampleRepository global = SampleRepository.getGlobalInstance();
            global.includeSamplesFile(path);

            return null;
        }
    }
}
