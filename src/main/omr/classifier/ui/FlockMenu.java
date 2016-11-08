//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F l o c k M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier.ui;

import omr.classifier.Flock;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.sheet.Sheet;

import omr.ui.OmrGui;
import omr.ui.util.SeparableMenu;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ApplicationActionMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code FlockMenu}
 *
 * @author Hervé Bitteur
 */
public class FlockMenu
        extends SeparableMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Selected glyph. */
    private final Glyph glyph;

    /** Containing sheet. */
    private final Sheet sheet;

    /** Related sample sheet, if any. */
    private SampleSheet sampleSheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FlockMenu} object.
     *
     * @param glyph the selected glyph
     * @param sheet the containing sheet
     */
    public FlockMenu (Glyph glyph,
                      Sheet sheet)
    {
        this.glyph = glyph;
        this.sheet = sheet;

        final SampleRepository repository = SampleRepository.getLoadedInstance(false);
        sampleSheet = repository.pokeSampleSheet(sheet);

        populateMenu();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addGood //
    //---------//
    @Action
    public void addGood (ActionEvent e)
    {
        if (sampleSheet == null) {
            final SampleRepository repository = SampleRepository.getLoadedInstance(false);
            sampleSheet = repository.findSampleSheet(sheet);
        }

        final Flock currentFlock = sampleSheet.getCurrentFlock(); // Cannot be null
        final Shape shape = currentFlock.getBest().getShape();
        final Glyph g = sheet.getGlyphIndex().registerOriginal(glyph);
        final Sample good = new Sample(g, sheet.getInterline(), shape, null);
        currentFlock.addGood(good);
        sampleSheet.setModified(true);
        logger.info("Added good {} to {}", good, currentFlock);
    }

    //----------//
    // addOther //
    //----------//
    @Action
    public void addOther (ActionEvent e)
    {
        if (sampleSheet == null) {
            final SampleRepository repository = SampleRepository.getLoadedInstance(false);
            sampleSheet = repository.findSampleSheet(sheet);
        }

        final Flock currentFlock = sampleSheet.getCurrentFlock(); // Cannot be null
        final Shape shape = currentFlock.getBest().getShape();
        final Glyph g = sheet.getGlyphIndex().registerOriginal(glyph);
        final Sample other = new Sample(g, sheet.getInterline(), shape, null);
        currentFlock.addOther(other);
        sampleSheet.setModified(true);
        logger.info("Added other {} to {}", other, currentFlock);
    }

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

    //--------------//
    // populateMenu //
    //--------------//
    /**
     * Build the flock menu, based on selected glyph.
     */
    private void populateMenu ()
    {
        setText(Integer.toString(glyph.getId()));

        ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(this);

        // Best: Start a new flock with the glyph? Using manual shape selection
        add(new SelectMenu());

        Flock currentFlock = (sampleSheet != null) ? sampleSheet.getCurrentFlock() : null;

        if (currentFlock != null) {
            // Good: Add compatible glyph to current flock
            add(new JMenuItem((ApplicationAction) actionMap.get("addGood")));

            // Other: Add sub-optimal glyph to current flock
            add(new JMenuItem((ApplicationAction) actionMap.get("addOther")));
        }
    }

    //------------//
    // selectBest //
    //------------//
    private void selectBest (Shape shape)
    {
        if (sampleSheet == null) {
            final SampleRepository repository = SampleRepository.getLoadedInstance(false);
            sampleSheet = repository.findSampleSheet(sheet);
        }

        final Glyph g = sheet.getGlyphIndex().registerOriginal(glyph);
        final Sample best = new Sample(g, sheet.getInterline(), shape, null);

        sampleSheet.getFlock(best);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // SelectMenu //
    //------------//
    private class SelectMenu
            extends JMenu
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SelectMenu ()
        {
            super("Set as Best");

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
                    selectBest(shape);
                }
            });
        }
    }
}
