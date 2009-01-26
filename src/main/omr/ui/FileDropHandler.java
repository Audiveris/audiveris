//----------------------------------------------------------------------------//
//                                                                            //
//                       F i l e D r o p H a n d l e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.ui;

import omr.log.Logger;

import omr.sheet.ui.SheetActions;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 * Class <code>FileDropHandler</code> handles the dropping of files onto the
 * Audiveris GUI
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class FileDropHandler
    extends TransferHandler
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        FileDropHandler.class);

    //~ Methods ----------------------------------------------------------------

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

    //------------//
    // importData //
    //------------//
    @Override
    public boolean importData (TransferSupport support)
    {
        if (!canImport(support)) {
            return false;
        }

        /* fetch the Transferable */
        Transferable trsf = support.getTransferable();

        try {
            /* Fetch data */
            Object data = trsf.getTransferData(DataFlavor.javaFileListFlavor);
            java.util.List fileList = (java.util.List) data;

            /* Loop through the files */
            for (Object obj : fileList) {
                File file = (File) obj;
                logger.info("Dropping file " + file);
                new SheetActions.OpenTask(file).execute();
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
}
