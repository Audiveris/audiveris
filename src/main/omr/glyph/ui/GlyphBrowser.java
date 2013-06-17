//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h B r o w s e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.BasicNest;
import omr.glyph.GlyphRepository;
import omr.glyph.GlyphSignature;
import omr.glyph.GlyphsModel;
import omr.glyph.Nest;
import omr.glyph.facets.Glyph;

import omr.lag.BasicLag;
import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Orientation;

import omr.selection.GlyphEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.field.LTextField;
import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.BlackList;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code GlyphBrowser} gathers a navigator to move between
 * selected glyphs, a glyph board for glyph details, and a display for
 * graphical glyph view.
 * This is a (package private) companion of {@link SampleVerifier}.
 *
 * @author Hervé Bitteur
 */
class GlyphBrowser
        implements ChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GlyphBrowser.class);

    /** Events that can be published on internal service (TODO: Check this!) */
    private static final Class<?>[] locEvents = new Class<?>[]{
        LocationEvent.class
    };

    /**
     * Field constant {@code NO_INDEX} is a specific value {@value} to
     * indicate absence of index
     */
    private static final int NO_INDEX = -1;

    //~ Instance fields --------------------------------------------------------
    /** The concrete Swing component */
    private JPanel component = new JPanel();

    /** Reference of SampleVerifier */
    private final SampleVerifier verifier;

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** Size of the lag display */
    private Dimension modelSize;

    /** Contour of the lag display */
    private Rectangle modelRectangle;

    /** Population of glyphs file names */
    private List<String> names = Collections.emptyList();

    /** Navigator instance to navigate through all glyphs names */
    private Navigator navigator;

    /** Left panel : navigator, glyphboard, evaluator */
    private JPanel leftPanel;

    /** Composite display (view + zoom slider) */
    private Display display;

    /** Basic location event service */
    private SelectionService locationService;

    /** Hosting Nest */
    private Nest tNest;

    /** Vertical Lag */
    private Lag vtLag;

    /** Horizontal Lag */
    private Lag htLag;

    /** Basic glyph model */
    private GlyphsController controller;

    /** The glyph view */
    private NestView view;

    /** Glyph board with ability to delete a training glyph */
    private GlyphBoard glyphBoard;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // GlyphBrowser //
    //--------------//
    /**
     * Create an instance, with back-reference to SampleVerifier.
     *
     * @param verifier ref back to verifier
     */
    public GlyphBrowser (SampleVerifier verifier)
    {
        this.verifier = verifier;

        // Layout
        component.setLayout(new BorderLayout());
        resetBrowser();
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JPanel getComponent ()
    {
        return component;
    }

    //----------------//
    // loadGlyphNames //
    //----------------//
    /**
     * Programmatic use of Load action in Navigator: load the glyph names as
     * selected, and focus on first glyph.
     */
    public void loadGlyphNames ()
    {
        navigator.loadAction.actionPerformed(null);
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Called when a new selection has been made in SampleVerifier
     * companion.
     *
     * @param e not used
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        int selNb = verifier.getGlyphCount();
        navigator.loadAction.setEnabled(selNb > 0);
    }

    //----------------//
    // buildLeftPanel //
    //----------------//
    /**
     * Build a panel composed vertically of a Navigator, a GlyphBoard
     * and an EvaluationBoard.
     *
     * @return the UI component, ready to be inserted in Swing hierarchy
     */
    private JPanel buildLeftPanel ()
    {
        navigator = new Navigator();

        // Specific glyph board
        glyphBoard = new MyGlyphBoard(controller);

        glyphBoard.connect();
        glyphBoard.getDeassignAction()
                .setEnabled(false);

        // Passive evaluation board
        EvaluationBoard evalBoard = new EvaluationBoard(controller, true);
        evalBoard.connect();

        // Layout
        FormLayout layout = new FormLayout("pref", "pref,pref,pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cst = new CellConstraints();
        builder.setDefaultDialogBorder();

        builder.add(navigator.getComponent(), cst.xy(1, 1));
        builder.add(glyphBoard.getComponent(), cst.xy(1, 2));
        builder.add(evalBoard.getComponent(), cst.xy(1, 3));

        return builder.getPanel();
    }

    //-------------//
    // removeGlyph //
    //-------------//
    private void removeGlyph ()
    {
        int index = navigator.getIndex();

        if (index >= 0) {
            // Delete glyph designated by index
            String gName = names.get(index);
            Glyph glyph = navigator.getGlyph(gName);

            // User confirmation is required ?
            if (constants.confirmDeletions.getValue()) {
                if (JOptionPane.showConfirmDialog(
                        component,
                        "Remove glyph '" + gName + "' ?") != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Shrink names list
            names.remove(index);

            // Update model & display
            repository.removeGlyph(gName);

            for (Section section : glyph.getMembers()) {
                section.delete();
            }

            // Update the Glyph selector also !
            verifier.deleteGlyphName(gName);

            // Perform file deletion
            if (repository.isIcon(gName)) {
                new SymbolsBlackList().add(new File(gName));
            } else {
                File file = new File(WellKnowns.TRAIN_FOLDER, gName);
                new BlackList(file.getParentFile()).add(new File(gName));
            }

            logger.info("Removed {}", gName);

            // Set new index ?
            if (index < names.size()) {
                navigator.setIndex(index, GLYPH_INIT); // Next
            } else {
                navigator.setIndex(index - 1, GLYPH_INIT); // Prev/None
            }
        } else {
            logger.warn("No selected glyph to remove!");
        }
    }

    //--------------//
    // resetBrowser //
    //--------------//
    private void resetBrowser ()
    {
        // Reset model
        tNest = new NoSigNest("tNest", null);
        htLag = new BasicLag("htLag", Orientation.HORIZONTAL);
        vtLag = new BasicLag("vtLag", Orientation.VERTICAL);

        locationService = new SelectionService("BrowserLocation", locEvents);
        controller = new BasicController(tNest, locationService);
        tNest.setServices(locationService);

        // Reset left panel
        if (leftPanel != null) {
            component.remove(leftPanel);
        }

        leftPanel = buildLeftPanel();
        component.add(leftPanel, BorderLayout.WEST);

        // Reset display
        if (display != null) {
            component.remove(display);
        }

        display = new Display();
        component.add(display, BorderLayout.CENTER);

        // TODO: Check if all this is really needed ...
        component.invalidate();
        component.validate();
        component.repaint();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------------//
    // BasicController //
    //-----------------//
    /**
     * A very basic glyphs controller, with a sheet-less location service.
     */
    private class BasicController
            extends GlyphsController
    {
        //~ Instance fields ----------------------------------------------------

        /** A specific location service, not tied to a sheet */
        private final SelectionService locationService;

        //~ Constructors -------------------------------------------------------
        public BasicController (Nest nest,
                                SelectionService locationService)
        {
            super(new BasicModel(nest));
            this.locationService = locationService;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public SelectionService getLocationService ()
        {
            return this.locationService;
        }
    }

    //------------//
    // BasicModel //
    //------------//
    /**
     * A very basic glyphs model, used to handle the deletion of glyphs.
     */
    private class BasicModel
            extends GlyphsModel
    {
        //~ Constructors -------------------------------------------------------

        public BasicModel (Nest nest)
        {
            super(null, nest, null);
        }

        //~ Methods ------------------------------------------------------------
        // Certainly not called ...
        @Override
        public void deassignGlyph (Glyph glyph)
        {
            removeGlyph();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean confirmDeletions = new Constant.Boolean(
                true,
                "Should user confirm each glyph deletion"
                + " from training material");

    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DeassignAction ()
        {
            super("Remove");
            putValue(
                    Action.SHORT_DESCRIPTION,
                    "Remove that glyph from training material");
        }

        //~ Methods ------------------------------------------------------------
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed (ActionEvent e)
        {
            removeGlyph();
        }
    }

    //---------//
    // Display //
    //---------//
    private class Display
            extends JPanel
    {
        //~ Instance fields ----------------------------------------------------

        LogSlider slider;

        Rubber rubber;

        ScrollView slv;

        Zoom zoom;

        //~ Constructors -------------------------------------------------------
        public Display ()
        {
            view = new MyView(controller);
            view.setLocationService(locationService);
            view.subscribe();
            modelRectangle = new Rectangle();
            modelSize = new Dimension(0, 0);
            slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 4, 0);
            zoom = new Zoom(slider, 1); // Default ratio set to 1
            rubber = new Rubber(view, zoom);
            rubber.setMouseMonitor(view);
            view.setZoom(zoom);
            view.setRubber(rubber);
            slv = new ScrollView(view);

            // Layout
            setLayout(new BorderLayout());
            add(slider, BorderLayout.WEST);
            add(slv.getComponent(), BorderLayout.CENTER);
        }
    }

    //------------//
    // LoadAction //
    //------------//
    private class LoadAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public LoadAction ()
        {
            super("Load");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Get a (shrinkable, to allow deletions) list of glyph names
            names = verifier.getGlyphNames();

            // Reset lag & display
            resetBrowser();

            // Set navigator on first glyph, if any
            if (!names.isEmpty()) {
                navigator.setIndex(0, GLYPH_INIT);
            } else {
                if (e != null) {
                    logger.warn("No glyphs selected in Glyph Selector");
                }

                navigator.all.setEnabled(false);
                navigator.prev.setEnabled(false);
                navigator.next.setEnabled(false);
            }
        }
    }

    //--------------//
    // MyGlyphBoard //
    //--------------//
    private class MyGlyphBoard
            extends SymbolGlyphBoard
    {
        //~ Constructors -------------------------------------------------------

        public MyGlyphBoard (GlyphsController controller)
        {
            super(controller, false, true);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Action getDeassignAction ()
        {
            if (deassignAction == null) {
                deassignAction = new DeassignAction();
            }

            return deassignAction;
        }
    }

    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (GlyphsController controller)
        {
            super(tNest, controller, Arrays.asList(htLag, vtLag));
            setName("GlyphBrowser-View");
            subscribe();
        }

        //~ Methods ------------------------------------------------------------
        //---------//
        // onEvent //
        //---------//
        /**
         * Call-back triggered from (local) selection objects.
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Keep normal view behavior (rubber, etc...)
                super.onEvent(event);

                // Additional tasks
                if (event instanceof LocationEvent) {
                    LocationEvent sheetLocation = (LocationEvent) event;

                    if (sheetLocation.hint == SelectionHint.LOCATION_INIT) {
                        Rectangle rect = sheetLocation.getData();

                        if ((rect != null)
                            && (rect.width == 0)
                            && (rect.height == 0)) {
                            // Look for pointed glyph
                            int index = glyphLookup(rect);
                            navigator.setIndex(index, sheetLocation.hint);
                        }
                    }
                } else if (event instanceof GlyphEvent) {
                    GlyphEvent glyphEvent = (GlyphEvent) event;

                    if (glyphEvent.hint == GLYPH_INIT) {
                        Glyph glyph = glyphEvent.getData();

                        // Display glyph contour
                        if (glyph != null) {
                            locationService.publish(
                                    new LocationEvent(
                                    this,
                                    glyphEvent.hint,
                                    null,
                                    glyph.getBounds()));
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics2D g)
        {
            // Mark the current glyph
            int index = navigator.getIndex();

            if (index >= 0) {
                String gName = names.get(index);
                Glyph glyph = navigator.getGlyph(gName);
                g.setColor(Color.black);
                g.setXORMode(Color.darkGray);
                renderGlyphArea(glyph, g);
            }
        }

        //-------------//
        // glyphLookup //
        //-------------//
        /**
         * Lookup for a glyph that is pointed by rectangle location. This is a
         * very specific glyph lookup, for which we cannot rely on Nest
         * usual features. So we simply browse through the collection of glyphs
         * (names).
         *
         * @param rect location (upper left corner)
         * @return index in names collection if found, NO_INDEX otherwise
         */
        private int glyphLookup (Rectangle rect)
        {
            int index = -1;

            for (String gName : names) {
                index++;

                if (repository.isLoaded(gName)) {
                    Glyph glyph = navigator.getGlyph(gName);

                    if (glyph.getNest() == tNest) {
                        for (Section section : glyph.getMembers()) {
                            if (section.contains(rect.x, rect.y)) {
                                return index;
                            }
                        }
                    }
                }
            }

            return NO_INDEX; // Not found
        }
    }

    //-----------//
    // Navigator //
    //-----------//
    /**
     * Class {@code Navigator} handles the navigation through the
     * collection of glyphs (names).
     */
    private final class Navigator
            extends Board
    {
        //~ Instance fields ----------------------------------------------------

        /** Current index in names collection (NO_INDEX if none) */
        private int nameIndex = NO_INDEX;

        // Navigation actions & buttons
        LoadAction loadAction = new LoadAction();

        JButton load = new JButton(loadAction);

        JButton all = new JButton("All");

        JButton next = new JButton("Next");

        JButton prev = new JButton("Prev");

        LTextField nameField = new LTextField("", "File where glyph is stored");

        //~ Constructors -------------------------------------------------------
        //-----------//
        // Navigator //
        //-----------//
        Navigator ()
        {
            super(Board.SAMPLE, null, null, false, true);

            defineLayout();

            all.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    // Load all (non icon) glyphs
                    int index = -1;

                    for (String gName : names) {
                        index++;

                        if (!repository.isIcon(gName)) {
                            setIndex(index, GLYPH_INIT);
                        }
                    }

                    // Load & point to first icon
                    setIndex(0, GLYPH_INIT);
                }
            });

            prev.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    setIndex(nameIndex - 1, GLYPH_INIT); // To prev
                }
            });

            next.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    setIndex(nameIndex + 1, GLYPH_INIT); // To next
                }
            });

            load.setToolTipText("Load the selected glyphs");
            all.setToolTipText("Display all glyphs");
            prev.setToolTipText("Go to previous glyph");
            next.setToolTipText("Go to next glyph");

            loadAction.setEnabled(false);
            all.setEnabled(false);
            prev.setEnabled(false);
            next.setEnabled(false);
        }

        //~ Methods ------------------------------------------------------------
        //----------//
        // getGlyph //
        //----------//
        public Glyph getGlyph (String gName)
        {
            Glyph glyph = repository.getGlyph(gName, null);

            if (glyph == null) {
                return null;
            }

            if (glyph.getNest() != tNest) {
                tNest.addGlyph(glyph);

                Color color = glyph.getShape()
                        .getColor();

                for (Section section : glyph.getMembers()) {
                    Lag lag = section.isVertical() ? vtLag : htLag;

                    lag.addVertex(section); // Trick!
                    section.setGraph(lag);

                    section.setColor(color);
                }
            }

            return glyph;
        }

        //----------//
        // getIndex //
        //----------//
        /**
         * Report the current glyph index in the names collection.
         *
         * @return the current index, which may be NO_INDEX
         */
        public final int getIndex ()
        {
            return nameIndex;
        }

        // Just to please the Board interface
        @Override
        public void onEvent (UserEvent event)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        //----------//
        // setIndex //
        //----------//
        /**
         * Only method allowed to designate a glyph.
         *
         * @param index index of new current glyph
         * @param hint  related processing hint
         */
        public void setIndex (int index,
                              SelectionHint hint)
        {
            Glyph glyph = null;

            if (index >= 0) {
                String gName = names.get(index);
                nameField.setText(gName);

                // Special case for icon : if we point to an icon, we have to
                // get rid of all other icons (standard glyphs can be kept)
                // Otherwise, they would all be displayed on top of the other
                if (repository.isIcon(gName)) {
                    repository.unloadIconsFrom(names);
                }

                // Load the desired glyph if needed
                glyph = getGlyph(gName);

                if (glyph == null) {
                    return;
                }

                // Extend view model size if needed
                Rectangle box = glyph.getBounds();
                modelRectangle = modelRectangle.union(box);

                Dimension newSize = modelRectangle.getSize();

                if (!newSize.equals(modelSize)) {
                    modelSize = newSize;
                    view.setModelSize(modelSize);
                }
            } else {
                nameField.setText("");
            }

            nameIndex = index;

            tNest.getGlyphService()
                    .publish(new GlyphEvent(this, hint, null, glyph));

            // Enable buttons according to glyph selection
            all.setEnabled(!names.isEmpty());
            prev.setEnabled(index > 0);
            next.setEnabled((index >= 0) && (index < (names.size() - 1)));
        }

        //--------------//
        // defineLayout //
        //--------------//
        private void defineLayout ()
        {
            CellConstraints cst = new CellConstraints();
            FormLayout layout = Panel.makeFormLayout(4, 3);
            PanelBuilder builder = new PanelBuilder(layout, super.getBody());
            builder.setDefaultDialogBorder();

            int r = 1; // --------------------------------
            builder.add(load, cst.xy(11, r));

            r += 2; // --------------------------------
            builder.add(all, cst.xy(3, r));
            builder.add(prev, cst.xy(7, r));
            builder.add(next, cst.xy(11, r));

            r += 2; // --------------------------------

            JLabel file = new JLabel("File", SwingConstants.RIGHT);
            builder.add(file, cst.xy(1, r));

            nameField.getField()
                    .setHorizontalAlignment(JTextField.LEFT);
            builder.add(nameField.getField(), cst.xyw(3, r, 9));
        }
    }

    //-----------//
    // NoSigNest //
    //-----------//
    /**
     * A specific glyph nest, with no handling of signature.
     */
    private static class NoSigNest
            extends BasicNest
    {
        //~ Constructors -------------------------------------------------------

        public NoSigNest (String name,
                          Sheet sheet)
        {
            super(name, sheet);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Glyph getOriginal (GlyphSignature signature)
        {
            return null;
        }
    }
}
