//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E x t r a c t i o n M e n u                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.OMR;

import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;
import omr.ui.view.LocationDependent;
import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.JMenuItem;

/**
 * Class {@code ExtractionMenu} allows to save the current sheet image, or a rectangular
 * portion of it, to disk, usually for later analysis.
 *
 * @author Hervé Bitteur
 */
public class ExtractionMenu
        extends LocationDependentMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ExtractionMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the extraction menu
     *
     * @param sheet the related sheet
     */
    public ExtractionMenu (Sheet sheet)
    {
        super("Extraction ...");
        this.sheet = sheet;

        add(new JMenuItem(new WholeAction())); // Save the whole sheet
        add(new JMenuItem(new AreaAction())); // Save just a rectangle of sheet
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // save //
    //------//
    private void save (BufferedImage img)
            throws IOException
    {
        // Let the user select an output file
        final Book book = sheet.getStub().getBook();
        final Path bookPath = BookManager.getDefaultBookPath(book);
        final Path bookFolder = bookPath.getParent();
        final File file = UIUtil.fileChooser(
                true,
                OMR.gui.getFrame(),
                new File(bookFolder.toFile(), sheet.getId() + "-ext.png"),
                new OmrFileFilter(".png images", new String[]{".png"}));

        if (file == null) {
            return;
        }

        File folder = new File(file.getParent());

        if (folder.mkdirs()) {
            logger.info("Creating folder {}", folder);
        }

        ImageIO.write(img, "png", file);

        Picture.setDefaultExtractionDirectory(file.getParent());
        logger.info("Extraction stored as {}", file);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // AreaAction //
    //------------//
    /**
     * Save the current area image to disk.
     */
    private class AreaAction
            extends AbstractAction
            implements LocationDependent
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Clamped area. */
        private Rectangle area;

        //~ Constructors ---------------------------------------------------------------------------
        public AreaAction ()
        {
            putValue(SHORT_DESCRIPTION, "Save the selected area to disk");
            setEnabled(false); // By default
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            try {
                // Extract the area selected from initial image
                save(sheet.getPicture().getImage(area));
            } catch (Exception ex) {
                logger.warn("Error in area extraction, " + ex, ex);
            }
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            area = null;

            if ((rect != null) && (rect.width != 0) && (rect.height != 0)) {
                // Limit area within image bounds
                area = rect.intersection(new Rectangle(0, 0, sheet.getWidth(), sheet.getHeight()));
            }

            setEnabled(area != null);

            if (area != null) {
                putValue(NAME, String.format("Area %dx%d ...", area.width, area.height));
            } else {
                putValue(NAME, "no area selected");
            }
        }
    }

    //-------------//
    // WholeAction //
    //-------------//
    /**
     * Save the whole sheet image to disk.
     */
    private class WholeAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public WholeAction ()
        {
            putValue(NAME, "Whole sheet");
            putValue(SHORT_DESCRIPTION, "Save the whole sheet to disk");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            try {
                // Extract the whole initial image
                save(sheet.getPicture().getImage(null));
            } catch (Exception ex) {
                logger.warn("Error in sheet extraction, " + ex, ex);
            }
        }
    }
}
