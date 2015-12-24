//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S a m p l e B r o w s e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.WellKnowns;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphIndex;
import omr.glyph.Glyph;
import omr.glyph.ui.GlyphService;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.EvaluationBoard;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.NestView;
import omr.glyph.ui.SymbolGlyphBoard;
import omr.glyph.ui.SymbolsBlackList;

import omr.lag.BasicLag;
import omr.lag.Lag;
import omr.lag.SectionService;

import omr.run.Orientation;

import omr.selection.EntityListEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import static omr.selection.SelectionHint.*;

import omr.selection.SelectionService;
import omr.selection.UserEvent;

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
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Class {@code SampleBrowser} gathers a navigator to move between selected samples,
 * a sample board for sample details, and a display for graphical sample view.
 * This is a (package private) companion of {@link SampleVerifier}.
 *
 * @author Hervé Bitteur
 */
class SampleBrowser
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SampleBrowser.class);

    /** Events that can be published on internal service (TODO: Check this!) */
    private static final Class<?>[] locEvents = new Class<?>[]{LocationEvent.class};

    /**
     * Field constant {@code NO_INDEX} is a specific value {@value} to
     * indicate absence of index
     */
    private static final int NO_INDEX = -1;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The concrete Swing component */
    private JPanel component = new JPanel();

    /** Reference of SampleVerifier */
    private final SampleVerifier verifier;

    /** Repository of known samples */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** Size of the lag display */
    private Dimension modelSize;

    /** Contour of the lag display */
    private Rectangle modelRectangle;

    /** Population of samples file names */
    private List<String> names = Collections.emptyList();

    /** Navigator instance to navigate through all samples names */
    private Navigator navigator;

    /** Left panel : navigator, sample-board, evaluator */
    private JPanel leftPanel;

    /** Composite display (view + zoom slider) */
    private Display display;

    /** Basic location event service */
    private SelectionService locationService;

    /** Hosting GlyphIndex */
    private GlyphIndex sampleIndex;

    /** Vertical Lag */
    private Lag vtLag;

    /** Horizontal Lag */
    private Lag htLag;

    /** Basic sample model */
    private GlyphsController controller;

    /** The sample view */
    private NestView view;

    /** Glyph board with ability to delete a training sample */
    private GlyphBoard sampleBoard;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance, with back-reference to SampleVerifier.
     *
     * @param verifier ref back to verifier
     */
    public SampleBrowser (SampleVerifier verifier)
    {
        this.verifier = verifier;

        // Layout
        component.setLayout(new BorderLayout());
        resetBrowser();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * Programmatic use of Load action in Navigator: load the sample
     * names as selected, and focus on first sample.
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

        // Specific sample board
        sampleBoard = new MyGlyphBoard(controller);

        sampleBoard.connect();
        sampleBoard.getDeassignAction().setEnabled(false);

        // Passive evaluation board
        EvaluationBoard evalBoard = new EvaluationBoard(controller, true);
        evalBoard.connect();

        // Layout
        FormLayout layout = new FormLayout("pref", "pref,pref,pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cst = new CellConstraints();
        ///builder.setDefaultDialogBorder();
        builder.add(navigator.getComponent(), cst.xy(1, 1));
        builder.add(sampleBoard.getComponent(), cst.xy(1, 2));
        builder.add(evalBoard.getComponent(), cst.xy(1, 3));

        return builder.getPanel();
    }

    //--------------//
    // removeSample //
    //--------------//
    private void removeSample ()
    {
        int index = navigator.getIndex();

        if (index >= 0) {
            // Delete sample designated by index
            String gName = names.get(index);
            Sample sample = navigator.getSample(gName);

            // User confirmation is required ?
            if (constants.confirmDeletions.getValue()) {
                if (JOptionPane.showConfirmDialog(component, "Remove sample '" + gName + "' ?") != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Shrink names list
            names.remove(index);

            // Update model & display
            repository.removeGlyph(gName);
            //
            //            for (Section section : sample.getMembers()) {
            //                section.delete();
            //            }
            //
            // Update the Glyph selector also !
            verifier.deleteGlyphName(gName);

            // Perform file deletion
            if (repository.isIcon(gName)) {
                new SymbolsBlackList().add(Paths.get(gName));
            } else {
                Path path = WellKnowns.TRAIN_FOLDER.resolve(gName);
                new BlackList(path.getParent()).add(Paths.get(gName));
            }

            logger.info("Removed {}", gName);

            // Set new index ?
            if (index < names.size()) {
                navigator.setIndex(index, ENTITY_INIT); // Next
            } else {
                navigator.setIndex(index - 1, ENTITY_INIT); // Prev/None
            }
        } else {
            logger.warn("No selected sample to remove!");
        }
    }

    //--------------//
    // resetBrowser //
    //--------------//
    private void resetBrowser ()
    {
        // Reset model
        locationService = new SelectionService("sampleLocationService", locEvents);

        htLag = new BasicLag("htLag", Orientation.HORIZONTAL);
        htLag.setEntityService(new SectionService(htLag, locationService));

        vtLag = new BasicLag("vtLag", Orientation.VERTICAL);
        vtLag.setEntityService(new SectionService(vtLag, locationService));

        sampleIndex = new GlyphIndex();
        sampleIndex.setEntityService(new GlyphService(sampleIndex, locationService));

        controller = new BasicController(sampleIndex, locationService);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // BasicController //
    //-----------------//
    /**
     * A very basic samples controller, with a sheet-less location service.
     */
    private class BasicController
            extends GlyphsController
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** A specific location service, not tied to a sheet */
        private final SelectionService locationService;

        //~ Constructors ---------------------------------------------------------------------------
        public BasicController (GlyphIndex nest,
                                SelectionService locationService)
        {
            super(new BasicModel(nest));
            this.locationService = locationService;
        }

        //~ Methods --------------------------------------------------------------------------------
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
     * A very basic samples model, used to handle the deletion of samples.
     */
    private class BasicModel
            extends GlyphsModel
    {
        //~ Constructors ---------------------------------------------------------------------------

        public BasicModel (GlyphIndex nest)
        {
            super(null, nest, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        // Certainly not called ...
        @Override
        public void deassignGlyph (Glyph sample)
        {
            removeSample();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean confirmDeletions = new Constant.Boolean(
                true,
                "Should user confirm each sample deletion" + " from training material");
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public DeassignAction ()
        {
            super("Remove");
            putValue(Action.SHORT_DESCRIPTION, "Remove that sample from training material");
        }

        //~ Methods --------------------------------------------------------------------------------
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed (ActionEvent e)
        {
            removeSample();
        }
    }

    //---------//
    // Display //
    //---------//
    private class Display
            extends JPanel
    {
        //~ Instance fields ------------------------------------------------------------------------

        LogSlider slider;

        Rubber rubber;

        ScrollView slv;

        Zoom zoom;

        //~ Constructors ---------------------------------------------------------------------------
        public Display ()
        {
            view = new MyView();
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
        //~ Constructors ---------------------------------------------------------------------------

        public LoadAction ()
        {
            super("Load");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Get a (shrinkable, to allow deletions) list of sample names
            names = verifier.getGlyphNames();

            // Reset lag & display
            resetBrowser();

            // Set navigator on first sample, if any
            if (!names.isEmpty()) {
                navigator.setIndex(0, ENTITY_INIT);
            } else {
                if (e != null) {
                    logger.warn("No samples selected in Glyph Selector");
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
        //~ Constructors ---------------------------------------------------------------------------

        public MyGlyphBoard (GlyphsController controller)
        {
            super(controller, false, true);
        }

        //~ Methods --------------------------------------------------------------------------------
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
        //~ Constructors ---------------------------------------------------------------------------

        public MyView ()
        {
            super(sampleIndex.getEntityService(), Arrays.asList(htLag, vtLag), null);
            setName("GlyphBrowser-View");
            subscribe();
        }

        //~ Methods --------------------------------------------------------------------------------
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

                        if ((rect != null) && (rect.width == 0) && (rect.height == 0)) {
                            // Look for pointed sample
                            int index = sampleLookup(rect);
                            navigator.setIndex(index, sheetLocation.hint);
                        }
                    }
                } else if (event instanceof EntityListEvent) {
                    if (event.hint == ENTITY_INIT) {
                        EntityListEvent<Sample> samplesEvent = (EntityListEvent<Sample>) event;
                        Sample sample = samplesEvent.getEntity();

                        if (sample != null) {
                            // Display sample contour
                            locationService.publish(
                                    new LocationEvent(this, event.hint, null, sample.getBounds()));
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
            // Mark the current sample
            int index = navigator.getIndex();

            if (index >= 0) {
                String gName = names.get(index);
                Sample sample = navigator.getSample(gName);
                g.setColor(Color.black);
                g.setXORMode(Color.darkGray);
                renderBoxArea(sample.getBounds(), g);
            }
        }

        //-------------//
        // sampleLookup //
        //-------------//
        /**
         * Lookup for a sample that is pointed by rectangle location. This is a
         * very specific sample lookup, for which we cannot rely on GlyphIndex
         * usual features. So we simply browse through the collection of samples
         * (names).
         *
         * @param rect location (upper left corner)
         * @return index in names collection if found, NO_INDEX otherwise
         */
        private int sampleLookup (Rectangle rect)
        {
            int index = -1;

            for (String gName : names) {
                index++;

                if (repository.isLoaded(gName)) {
                    Sample sample = navigator.getSample(gName);

                    //
                    //                    if (sample.getIndex() == tNest) {
                    //                        for (Section section : sample.getMembers()) {
                    //                            if (section.contains(rect.x, rect.y)) {
                    //                                return index;
                    //                            }
                    //                        }
                    //                    }
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
     * collection of samples (names).
     */
    private final class Navigator
            extends Board
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Current index in names collection (NO_INDEX if none) */
        private int nameIndex = NO_INDEX;

        // Navigation actions & buttons
        LoadAction loadAction = new LoadAction();

        JButton load = new JButton(loadAction);

        JButton all = new JButton("All");

        JButton next = new JButton("Next");

        JButton prev = new JButton("Prev");

        LTextField nameField = new LTextField("", "File where sample is stored");

        //~ Constructors ---------------------------------------------------------------------------
        Navigator ()
        {
            super(Board.SAMPLE, null, null, false, false, false, true);

            defineLayout();

            all.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    // Load all (non icon) samples
                    int index = -1;

                    for (String gName : names) {
                        index++;

                        if (!repository.isIcon(gName)) {
                            setIndex(index, ENTITY_INIT);
                        }
                    }

                    // Load & point to first icon
                    setIndex(0, ENTITY_INIT);
                }
            });

            prev.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    setIndex(nameIndex - 1, ENTITY_INIT); // To prev
                }
            });

            next.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    setIndex(nameIndex + 1, ENTITY_INIT); // To next
                }
            });

            load.setToolTipText("Load the selected samples");
            all.setToolTipText("Display all samples");
            prev.setToolTipText("Go to previous sample");
            next.setToolTipText("Go to next sample");

            loadAction.setEnabled(false);
            all.setEnabled(false);
            prev.setEnabled(false);
            next.setEnabled(false);
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------//
        // getIndex //
        //----------//
        /**
         * Report the current sample index in the names collection.
         *
         * @return the current index, which may be NO_INDEX
         */
        public final int getIndex ()
        {
            return nameIndex;
        }

        //-----------//
        // getSample //
        //-----------//
        public Sample getSample (String gName)
        {
            Sample sample = repository.getSample(gName, null);

            if (sample == null) {
                return null;
            }

            //
            //            if (sample.getIndex() != tNest) {
            //                tNest.register(sample);
            //
            //                for (Section section : sample.getMembers()) {
            //                    Lag lag = section.isVertical() ? vtLag : htLag;
            //
            //                    lag.addVertex(section); // Trick!
            //                    section.setGraph(lag);
            //                    section.setCompound(sample);
            //                }
            //            }
            //
            return sample;
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
         * Only method allowed to designate a sample.
         *
         * @param index index of new current sample
         * @param hint  related processing hint
         */
        public void setIndex (int index,
                              SelectionHint hint)
        {
            Sample sample = null;

            if (index >= 0) {
                String gName = names.get(index);
                nameField.setText(gName);
                //
                //                // Special case for icon : if we point to an icon, we have to
                //                // get rid of all other icons (standard samples can be kept)
                //                // Otherwise, they would all be displayed on top of the other
                //                if (repository.isIcon(gName)) {
                //                    repository.unloadIconsFrom(names);
                //                }
                //
                // Load the desired sample if needed
                sample = getSample(gName);

                if (sample == null) {
                    return;
                }

                // Extend view model size if needed
                Rectangle box = sample.getBounds();
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

            sampleIndex.getEntityService().publish(
                    new EntityListEvent<Sample>(this, hint, null, Arrays.asList(sample)));

            // Enable buttons according to sample selection
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

            ///builder.setDefaultDialogBorder();
            int r = 1; // --------------------------------
            builder.add(load, cst.xy(11, r));

            r += 2; // --------------------------------
            builder.add(all, cst.xy(3, r));
            builder.add(prev, cst.xy(7, r));
            builder.add(next, cst.xy(11, r));

            r += 2; // --------------------------------

            JLabel file = new JLabel("File", SwingConstants.RIGHT);
            builder.add(file, cst.xy(1, r));

            nameField.getField().setHorizontalAlignment(JTextField.LEFT);
            builder.add(nameField.getField(), cst.xyw(3, r, 9));
        }
    }
}
