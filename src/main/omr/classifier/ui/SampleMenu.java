//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S a m p l e M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.sheet.Book;
import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.sig.inter.Inter;

import omr.ui.util.SeparableMenu;

import omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code SampleMenu} is a menu dedicated to picking a glyph as a shape sample.
 *
 * @author Hervé Bitteur
 */
public class SampleMenu
        extends SeparableMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing sheet. */
    private final Sheet sheet;

    /** Selected glyph. */
    private final Glyph glyph;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleMenu} object.
     *
     * @param glyph the selected glyph
     * @param sheet the containing sheet
     */
    public SampleMenu (Glyph glyph,
                       Sheet sheet)
    {
        this.sheet = sheet;
        this.glyph = glyph;

        populateMenu();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getGlyph //
    //----------//
    /**
     * @return the glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //-----------//
    // addSample //
    //-----------//
    private void addSample (Shape shape)
    {
        shape = Sample.getRecordableShape(shape);

        final Sample sample = new Sample(glyph, sheet.getInterline(), shape);
        logger.debug("addSample {}", sample);

        final SampleRepository repository = SampleRepository.getInstance();

        if (!repository.isLoaded()) {
            repository.loadRepository(false);
        }

        // Handle long name if any
        final Book book = sheet.getStub().getBook();
        String longSheetName = null;

        if (book.getAlias() != null) {
            longSheetName = FileUtil.getNameSansExtension(book.getInputPath());

            if (book.isMultiSheet()) {
                longSheetName = longSheetName + "#" + sheet.getStub().getNumber();
            }
        }

        final SampleSheet sampleSheet = repository.findSheet(
                sheet.getId(),
                longSheetName,
                sheet.getPicture().getTable(Picture.TableKey.BINARY));

        repository.addSample(sample, sampleSheet);
    }

    //-----------//
    // getShapes //
    //-----------//
    private Set<Shape> getShapes ()
    {
        final Set<Shape> shapes = EnumSet.noneOf(Shape.class);

        for (Inter inter : sheet.getInterIndex().getEntityService().getSelectedEntityList()) {
            if (inter.getGlyph() == glyph) {
                shapes.add(inter.getShape());
            }
        }

        return shapes;
    }

    //--------------//
    // populateMenu //
    //--------------//
    private void populateMenu ()
    {
        setText(Integer.toString(glyph.getId()));

        // Glyph interpretations
        Set<Shape> shapes = getShapes();

        if (!shapes.isEmpty()) {
            add(new AssignMenu(shapes));
        }

        // Manual shape selection
        add(new SelectMenu());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // AssignMenu //
    //------------//
    private class AssignMenu
            extends JMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final ActionListener listener = new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                Shape shape = Shape.valueOf(source.getText());
                addSample(shape);
            }
        };

        //~ Constructors ---------------------------------------------------------------------------
        public AssignMenu (Set<Shape> shapes)
        {
            super("Assign sample ...");

            populate(shapes);
        }

        //~ Methods --------------------------------------------------------------------------------
        private void populate (Set<Shape> shapes)
        {
            for (Shape shape : shapes) {
                JMenuItem menuItem = new JMenuItem(shape.toString(), shape.getDecoratedSymbol());
                menuItem.addActionListener(listener);
                add(menuItem);
            }
        }
    }

    //------------//
    // SelectMenu //
    //------------//
    private class SelectMenu
            extends JMenu
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SelectMenu ()
        {
            super("Select sample ...");

            populate();
        }

        //~ Methods --------------------------------------------------------------------------------
        private void populate ()
        {
            ShapeSet.addAllShapes(
                    this,
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    JMenuItem source = (JMenuItem) e.getSource();
                    Shape shape = Shape.valueOf(source.getText());
                    addSample(shape);
                }
            });
        }
    }
}
