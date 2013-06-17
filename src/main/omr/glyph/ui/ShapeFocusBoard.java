//----------------------------------------------------------------------------//
//                                                                            //
//                       S h a p e F o c u s B o a r d                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphRegression;
import omr.glyph.Shape;
import omr.glyph.ShapeDescription;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.math.LinearEvaluator.Printer;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.field.SpinnerUtil;
import static omr.ui.field.SpinnerUtil.*;
import omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code ShapeFocusBoard} handles a user iteration within a
 * collection of glyphs.
 * The collection may be built from glyphs of a given shape,
 * or from glyphs similar to a given glyph, etc.
 *
 * @author Hervé Bitteur
 */
public class ShapeFocusBoard
        extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ShapeFocusBoard.class);

    /** Events this board is interested in */
    private static final Class<?>[] eventsRead = new Class<?>[]{GlyphEvent.class};

    //~ Enumerations -----------------------------------------------------------
    /** Filter on which symbols should be displayed */
    private static enum Filter
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Display all symbols */
        ALL,
        /** Display only known symbols */
        KNOWN,
        /** Display only unknown symbols */
        UNKNOWN,
        /** Display only translated
         * symbols */
        TRANSLATED,
        /** Display only untranslated
         * symbols */
        UNTRANSLATED;

    }

    //~ Instance fields --------------------------------------------------------
    private final Sheet sheet;

    /** Browser on the collection of glyphs */
    private Browser browser = new Browser();

    /** Button to select the shape focus */
    private JButton selectButton = new JButton();

    /** Filter for known / unknown symbol display */
    private JComboBox<Filter> filterButton = new JComboBox<>(
            Filter.values());

    /** Popup menu to allow shape selection */
    private JPopupMenu pm = new JPopupMenu();

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // ShapeFocusBoard //
    //-----------------//
    /**
     * Create the instance to handle the shape focus, with pointers to
     * needed companions.
     *
     * @param sheet          the related sheet
     * @param controller     the related glyph controller
     * @param filterListener the action linked to filter button
     */
    public ShapeFocusBoard (Sheet sheet,
                            GlyphsController controller,
                            ActionListener filterListener,
                            boolean expanded)
    {
        super(
                Board.FOCUS,
                controller.getNest().getGlyphService(),
                eventsRead,
                false,
                expanded);

        this.sheet = sheet;

        // Tool Tips
        selectButton.setToolTipText("Select candidate shape");
        selectButton.setHorizontalAlignment(SwingConstants.LEFT);
        selectButton.addActionListener(
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                pm.show(
                        selectButton,
                        selectButton.getX(),
                        selectButton.getY());
            }
        });

        // Filter
        filterButton.addActionListener(filterListener);
        filterButton.setToolTipText(
                "Select displayed glyphs according to their current state");

        // Popup menu for shape selection
        JMenuItem noFocus = new JMenuItem("No Focus");
        noFocus.setToolTipText("Cancel any focus");
        noFocus.addActionListener(
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                setCurrentShape(null);
            }
        });
        pm.add(noFocus);
        ShapeSet.addAllShapes(
                pm,
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                setCurrentShape(Shape.valueOf(source.getText()));
            }
        });

        defineLayout();

        // Initially, no focus
        setCurrentShape(null);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // isDisplayed //
    //-------------//
    /**
     * Report whether the glyph at hand is to be displayed, according to
     * the current filter
     *
     * @param glyph the glyph at hand, perhaps null
     * @return true if to be displayed
     */
    public boolean isDisplayed (Glyph glyph)
    {
        switch ((Filter) filterButton.getSelectedItem()) {
        case KNOWN:
            return (glyph != null) && glyph.isKnown();

        case UNKNOWN:
            return (glyph == null) || !glyph.isKnown();

        case TRANSLATED:
            return (glyph != null) && glyph.isKnown() && glyph.isTranslated();

        case UNTRANSLATED:
            return (glyph != null) && glyph.isKnown() && !glyph.isTranslated();

        default:
        case ALL:
            return true;
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification about selection objects.
     * We used to use it on a just modified glyph, to set the new shape focus
     * But this conflicts with the ability to browse a collection of similar
     * glyphs and assign them on the fly
     *
     * @param event the notified event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        // Empty
    }

    //-----------------//
    // setCurrentShape //
    //-----------------//
    /**
     * Define the new current shape.
     *
     * @param currentShape the shape to be considered as current
     */
    public void setCurrentShape (Shape currentShape)
    {
        browser.resetIds();

        if (currentShape != null) {
            // Update the shape button
            selectButton.setText(currentShape.toString());
            selectButton.setIcon(currentShape.getDecoratedSymbol());

            // Count the number of glyphs assigned to current shape
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.getShape() == currentShape) {
                    browser.addId(glyph.getId());
                }
            }

            setSelected(true);
            setVisible(true);
        } else {
            // Void the shape button
            selectButton.setText("- No Focus -");
            selectButton.setIcon(null);
        }

        browser.refresh();
    }

    //-----------------//
    // setSimilarGlyph //
    //-----------------//
    /**
     * Define the glyphs collection as all glyphs whose physical
     * appearance is "similar" to the appearance of the provided glyph
     * example.
     *
     * @param example the provided example
     */
    public void setSimilarGlyph (Glyph example)
    {
        browser.resetIds();

        if (example != null) {
            GlyphRegression evaluator = GlyphRegression.getInstance();
            double[] pattern = ShapeDescription.features(example);
            List<DistIdPair> pairs = new ArrayList<>();

            // Retrieve the glyphs similar to the example
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                double dist = evaluator.measureDistance(glyph, pattern);
                pairs.add(new DistIdPair(dist, glyph.getId()));
            }

            Collections.sort(pairs, DistIdPair.distComparator);

            for (DistIdPair pair : pairs) {
                browser.addId(pair.id);
            }

            // To get a detailed table of the distances (debugging)
            if (constants.printDistances.getValue()) {
                Printer printer = evaluator.getEngine().new Printer(11);
                String indent = "                  ";
                System.out.println(indent + printer.getDefaults());
                System.out.println(indent + printer.getNames());
                System.out.println(indent + printer.getDashes());

                for (DistIdPair pair : pairs) {
                    Glyph glyph = sheet.getVerticalsController()
                            .getGlyphById(pair.id);
                    double[] gPat = ShapeDescription.features(glyph);
                    Shape shape = glyph.getShape();
                    System.out.printf(
                            "%18s",
                            (shape != null) ? shape.toString() : "");
                    System.out.println(printer.getDeltas(gPat, pattern));
                    System.out.printf("g#%04d d:%9f", pair.id, pair.dist);
                    System.out.println(
                            printer.getWeightedDeltas(gPat, pattern));
                }
            }

            // Update the shape button
            selectButton.setText("Glyphs similar to #" + example.getId());
            selectButton.setIcon(null);

            setSelected(true);
            setVisible(true);
        } else {
            // Void the shape button
            selectButton.setText("- No Focus -");
            selectButton.setIcon(null);
        }

        browser.refresh();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final String fieldInterline = Panel.getFieldInterline();

        String colSpec = Panel.makeColumns(3);
        FormLayout layout = new FormLayout(
                colSpec,
                "pref," + fieldInterline + "," + "pref");

        PanelBuilder builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(browser.count, cst.xy(1, r));
        builder.add(browser.spinner, cst.xy(3, r));
        builder.add(selectButton, cst.xywh(7, r, 5, 3));

        r += 2; // --------------------------------
        builder.add(filterButton, cst.xyw(1, r, 3));
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // DistIdPair //
    //------------//
    /**
     * Needed to sort glyphs id according to their distance
     */
    private static class DistIdPair
    {
        //~ Static fields/initializers -----------------------------------------

        private static final Comparator<DistIdPair> distComparator = new Comparator<DistIdPair>()
        {
            @Override
            public int compare (DistIdPair o1,
                                DistIdPair o2)
            {
                return Double.compare(o1.dist, o2.dist);
            }
        };

        //~ Instance fields ----------------------------------------------------
        final double dist;

        final int id;

        //~ Constructors -------------------------------------------------------
        public DistIdPair (double dist,
                           int id)
        {
            this.dist = dist;
            this.id = id;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "dist:" + dist + " glyph#" + id;
        }
    }

    //---------//
    // Browser //
    //---------//
    private class Browser
            implements ChangeListener
    {
        //~ Instance fields ----------------------------------------------------

        // Spinner on these glyphs
        ArrayList<Integer> ids = new ArrayList<>();

        // Number of glyphs
        JLabel count = new JLabel("", SwingConstants.RIGHT);

        JSpinner spinner = new JSpinner(new SpinnerListModel());

        //~ Constructors -------------------------------------------------------
        //---------//
        // Browser //
        //---------//
        public Browser ()
        {
            resetIds();
            spinner.addChangeListener(this);
            SpinnerUtil.setList(spinner, ids);
            refresh();
        }

        //~ Methods ------------------------------------------------------------
        //-------//
        // addId //
        //-------//
        public void addId (int id)
        {
            ids.add(id);
        }

        //---------//
        // refresh //
        //---------//
        public void refresh ()
        {
            if (ids.size() > 1) { // To skip first NO_VALUE item
                count.setText(0 + "/" + (ids.size() - 1));
                spinner.setEnabled(true);
            } else {
                count.setText("");
                spinner.setEnabled(false);
            }

            spinner.setValue(NO_VALUE);
        }

        //----------//
        // resetIds //
        //----------//
        public void resetIds ()
        {
            ids.clear();
            ids.add(NO_VALUE);
        }

        //--------------//
        // stateChanged //
        //--------------//
        @Override
        public void stateChanged (ChangeEvent e)
        {
            int id = (Integer) spinner.getValue();

            int index = ids.indexOf(id);
            count.setText(index + "/" + (ids.size() - 1));

            if (id != NO_VALUE) {
                getSelectionService()
                        .publish(
                        new GlyphIdEvent(this, SelectionHint.GLYPH_INIT, null, id));
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean printDistances = new Constant.Boolean(
                false,
                "Should we print out distance details when looking for similar glyphs?");

    }
}
