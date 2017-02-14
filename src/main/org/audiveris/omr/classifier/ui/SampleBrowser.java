//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S a m p l e B r o w s e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.GlyphDescriptor;
import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleRepository.AdditionEvent;
import org.audiveris.omr.classifier.SampleRepository.RemovalEvent;
import org.audiveris.omr.classifier.SampleRepository.SheetRemovalEvent;
import org.audiveris.omr.classifier.SampleSheet;
import org.audiveris.omr.classifier.SampleSource.ConstantSource;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.classifier.SheetContainer.Descriptor;
import org.audiveris.omr.classifier.Tribe;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ui.EvaluationBoard;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.FixedWidthIcon;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UILookAndFeel;
import org.audiveris.omr.ui.util.UIUtil;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class {@code SampleBrowser} provides a user interface to browse through all samples
 * recorded for classifier training, in order to visually check the correctness of their
 * assigned shape, and to remove or reassign spurious samples when necessary.
 * <p>
 * The user can select sheets, shapes and samples:<ul>
 * <li> For the current sample, detailed characteristics are listed, while the top 5 shape
 * evaluations are computed on-line by the shape classifier.
 * <li> If the binary image of containing sheet is available, the sample is displayed with its
 * context.
 * <li> The user is able to remove the sample or to re-assign it to a different shape.
 * </ul>
 * This class can also be launched as a stand-alone program.
 *
 * @author Hervé Bitteur
 */
public class SampleBrowser
        extends SingleFrameApplication
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleBrowser.class);

    /** The unique class instance. */
    private static volatile SampleBrowser INSTANCE;

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    /** Events that can be published on the local sample service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{EntityListEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Repository of training samples. */
    private final SampleRepository repository;

    /** The dedicated frame. */
    private JFrame frame;

    /** Local service for sample events. */
    private final EntityService<Sample> sampleService;

    /** View of sample in sheet context. */
    private final SampleContext sampleContext;

    /** Has repository been checked for duplications and conflicts?. */
    private boolean repoChecked;

    /** Panel for sheets selection. */
    private final SheetSelector sheetSelector = new SheetSelector();

    /** Panel for shapes selection. */
    private final ShapeSelector shapeSelector = new ShapeSelector();

    /** Panel for samples display. */
    private final SampleListing sampleListing;

    /** Model for samples. */
    private final SampleModel sampleModel;

    /** Controller for sample handling. */
    private final SampleController sampleController;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance of {@code SampleBrowser}.
     *
     * @param repository the repository (global or local) to browse
     */
    public SampleBrowser (SampleRepository repository)
    {
        this.repository = repository;
        sampleContext = new SampleContext(repository);

        sampleService = new EntityService<Sample>("sample service", null, eventsAllowed);
        sampleContext.connect(sampleService);
        sampleModel = new SampleModel(repository, sampleService);
        sampleController = new SampleController(sampleModel);

        // Connect selectors (sheets -> shapes -> samples)
        sampleListing = new SampleListing(this, repository);
        connectSelectors(true);

        // Stay informed of repository dynamic updates
        repository.addListener(this);

        if (!standAlone) {
            if (!repository.isLoaded()) {
                repository.loadRepository(null);
            }

            frame = defineLayout(new JFrame());
            frame.setTitle(repository.toString());
            frame.addWindowListener(new ClosingAdapter());
        } else {
            INSTANCE = this;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Give access to the global instance of this class.
     *
     * @return the SampleBrowser instance
     */
    public static synchronized SampleBrowser getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SampleBrowser(SampleRepository.getGlobalInstance());
        }

        return INSTANCE;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Give access to a specific book instance of this class.
     *
     * @param book provided book
     * @return the SampleBrowser instance
     */
    public static SampleBrowser getInstance (Book book)
    {
        SampleRepository repo = book.getSampleRepository();

        return new SampleBrowser(repo);
    }

    //------//
    // main //
    //------//
    /**
     * Just to allow stand-alone running of this class
     *
     * @param args not used
     */
    public static void main (String... args)
    {
        standAlone = true;

        // Load repository, with sheet images
        SampleRepository repo = SampleRepository.getGlobalInstance();
        repo.loadAllImages();

        // Set UI Look and Feel
        UILookAndFeel.setUI(null);
        Locale.setDefault(Locale.ENGLISH);

        // Off we go...
        Application.launch(SampleBrowser.class, args);
    }

    //-----------------//
    // checkRepository //
    //-----------------//
    /**
     * Action to check for sample duplicates or conflicts
     *
     * @param e the event which triggered this action, perhaps null
     */
    @Action
    public void checkRepository (ActionEvent e)
    {
        repoChecked = true;

        repository.purgeOrphanDescriptors();

        Set<Sample> conflictings = new LinkedHashSet<Sample>();
        Set<Sample> redundants = new LinkedHashSet<Sample>();
        repository.checkAllSamples(conflictings, redundants);

        if (conflictings.isEmpty() && redundants.isEmpty()) {
            logger.info("All repository samples are OK.");

            return;
        }

        if (!redundants.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    "Purge repository of " + redundants.size() + " duplication(s)?",
                    "Duplication(s) found in sample repository",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.YES_OPTION) {
                for (Sample sample : redundants) {
                    repository.removeSample(sample);
                }
            } else if (answer != JOptionPane.NO_OPTION) {
                repoChecked = false; // The user made no decision yet
            }
        }

        if (!conflictings.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    "Inspect these conflicting samples?",
                    "BEWARE: " + conflictings.size() + " conflict(s) detected",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.YES_OPTION) {
                displayAll(conflictings);
            } else if (answer != JOptionPane.NO_OPTION) {
                repoChecked = false; // The user made no decision yet
            }
        }
    }

    //-------------//
    // checkTribes //
    //-------------//
    /**
     * Check how sample tribes are classified
     *
     * @param e the event which triggered this action, perhaps null
     */
    @Action
    public void checkTribes (ActionEvent e)
    {
        final Classifier classifier = ShapeClassifier.getInstance();

        for (Descriptor descriptor : repository.getAllDescriptors()) {
            final SampleSheet sampleSheet = repository.getSampleSheet(descriptor);

            if (sampleSheet == null) {
                continue;
            }

            final List<Tribe> tribes = sampleSheet.getTribes();

            if (!tribes.isEmpty()) {
                logger.info("{}", sampleSheet);

                for (Tribe tribe : tribes) {
                    final Sample head = tribe.getHead();
                    final int iline = head.getInterline();
                    final int ord = head.getShape().ordinal();

                    final Evaluation[] bestEvals = classifier.getNaturalEvaluations(head, iline);
                    final Evaluation bestEval = bestEvals[ord];
                    logger.info("   Tribe head: {} {}", bestEval, head);

                    for (Sample good : tribe.getGoods()) {
                        Evaluation[] evals = classifier.getNaturalEvaluations(good, iline);
                        logger.info("         good: {} {}", evals[ord], good);
                    }

                    for (Sample member : tribe.getMembers()) {
                        Evaluation[] evals = classifier.getNaturalEvaluations(member, iline);
                        final Evaluation eval = evals[ord];

                        if (eval.grade >= bestEval.grade) {
                            logger.warn("       member: {} {} ABNORMAL", eval, member);
                        } else {
                            logger.info("       member: {} {}", eval, member);
                        }
                    }

                    logger.info("");
                }
            }
        }
    }

    //------------//
    // displayAll //
    //------------//
    /**
     * Focus the browser on a whole collection of samples, bypassing the usual
     * manual selection of sheets then shapes then samples.
     * <p>
     * (Typically these are the samples that are not correctly recognized by the classifier during
     * its test, or conflicting samples in repository check)
     *
     * @param samples the collection of samples to inspect
     */
    public void displayAll (Collection<Sample> samples)
    {
        connectSelectors(false); // Disable standard triggers: sheets -> shapes -> samples

        // Select proper Sheets
        Set<Descriptor> descSet = new LinkedHashSet<Descriptor>();

        for (Sample sample : samples) {
            descSet.add(repository.getDescriptor(sample));
        }

        sheetSelector.select(descSet);

        // Populate & select proper shapes
        EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

        for (Sample sample : samples) {
            shapeSet.add(sample.getShape());
        }

        shapeSelector.populateWith(shapeSet);
        shapeSelector.select(shapeSet);

        // Populate samples
        List<Sample> sorted = new ArrayList<Sample>(samples);
        Collections.sort(sorted, Sample.byShape); // Must be ordered by shape for listing
        sampleListing.populateWith(sorted);

        connectSelectors(true); // Re-enable standard triggers: sheets -> shapes -> samples

        // Bring the frame to front
        UIUtil.unMinimize(frame);
    }

    //----------------//
    // exportFeatures //
    //----------------//
    /**
     * Generate a file (using CSV format) to be used by deep learning software,
     * populated by all samples features.
     *
     * @param e unused
     */
    @Action
    public void exportFeatures (ActionEvent e)
    {
        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        GlyphDescriptor desc = ShapeClassifier.getInstance().getGlyphDescriptor();
        desc.export("all", samples, true);
    }

    //---------------------//
    // getSampleController //
    //---------------------//
    /**
     * @return the sampleController
     */
    public SampleController getSampleController ()
    {
        return sampleController;
    }

    //---------------//
    // launchTrainer //
    //---------------//
    /**
     * Display the trainer frame.
     *
     * @param e unused
     */
    @Action
    public void launchTrainer (ActionEvent e)
    {
        Trainer.launch();
    }

    //------------//
    // loadImages //
    //------------//
    /**
     * Action to load sheet images
     *
     * @param e the event which triggered this action
     */
    @Action
    public void loadImages (ActionEvent e)
    {
        repository.loadAllImages();
        sampleContext.refresh();
    }

    //-------------//
    // purgeSheets //
    //-------------//
    /**
     * Purge the repository sheets
     *
     * @param e unused
     */
    @Action
    public void purgeSheets (ActionEvent e)
    {
        repository.purgeSheets();
    }

    //----------------//
    // pushRepository //
    //----------------//
    /**
     * Push this (local) repository to the global repository.
     */
    @Action
    public void pushRepository ()
    {
        if (repository.isGlobal()) {
            logger.warn("You cannot push the global repository to itself!");
        } else {
            SampleRepository global = SampleRepository.getGlobalInstance();
            global.includeRepository(repository);
        }
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Action to update viewer with latest repository informations
     *
     * @param e the event which triggered this action
     */
    @Action
    public void refresh (ActionEvent e)
    {
        sheetSelector.stateChanged(null);
    }

    //--------------//
    // removeShapes //
    //--------------//
    /**
     * Remove the selected shapes within the selected sheets.
     *
     * @param e unused
     */
    @Action
    public void removeShapes (ActionEvent e)
    {
        final List<Shape> shapes = shapeSelector.list.getSelectedValuesList();

        if (!shapes.isEmpty()) {
            int shapeNb = shapes.size();
            String shapeStr = (shapeNb > 3) ? (shapeNb + " shapes") : shapes.toString();

            List<Descriptor> descs = sheetSelector.list.getSelectedValuesList();
            int descNb = descs.size();
            String descStr = descNb + " sheet" + ((descNb > 1) ? "s" : "");

            List<Sample> samples = repository.getSamples(descs, shapes);
            int sampleNb = samples.size();
            String sampleStr = sampleNb + " sample" + ((sampleNb > 1) ? "s" : "");

            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    "Remove " + sampleStr + " of " + shapeStr + " from " + descStr + "?",
                    "Removal confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.YES_OPTION) {
                for (Sample sample : samples) {
                    repository.removeSample(sample);
                }
            }
        }
    }

    //--------------//
    // removeSheets //
    //--------------//
    /**
     * Remove the selected sheets.
     *
     * @param e unused
     */
    @Action
    public void removeSheets (ActionEvent e)
    {
        final List<Descriptor> descs = sheetSelector.list.getSelectedValuesList();

        if (!descs.isEmpty()) {
            int descNb = descs.size();
            String descStr = descNb + " sheet" + ((descNb > 1) ? "s" : "");
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    "Remove whole material from " + descStr + "?",
                    "Removal confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.YES_OPTION) {
                for (Descriptor desc : descs) {
                    repository.removeSheet(desc);
                }
            }
        }
    }

    //------//
    // save //
    //------//
    /**
     * Action to save repository
     *
     * @param e the event which triggered this action
     */
    @Action
    public void save (ActionEvent e)
    {
        repository.checkForSave();
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the UI frame visible.
     */
    public void setVisible ()
    {
        OmrGui.getApplication().show(frame);
        UIUtil.unMinimize(frame);

        if (!repoChecked) {
            checkRepository(null);
        }
    }

    //------------------//
    // shrinkRepository //
    //------------------//
    /**
     * Shrink this repository so that no shape has more than maxCount samples.
     *
     * @param e unused
     */
    @Action
    public void shrinkRepository (ActionEvent e)
    {
        repository.shrink(2000);
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Call triggered by a repository update.
     *
     * @param event AdditionEvent or RemovalEvent or SheetRemovalEvent
     */
    @Override
    public void stateChanged (ChangeEvent event)
    {
        if (event instanceof AdditionEvent) {
            AdditionEvent addition = (AdditionEvent) event;

            // If sheet and shape for this sample are currently selected then update sample listing
            final Descriptor descriptor = repository.getDescriptor(addition.sample);
            final List<Descriptor> sheets = getSelectedSheets();

            if (sheets.contains(descriptor)) {
                final List<Shape> shapes = getSelectedShapes();

                if (shapes.contains(addition.sample.getShape())) {
                    sampleListing.addSample(addition.sample);
                }
            }
        } else if (event instanceof RemovalEvent) {
            RemovalEvent removal = (RemovalEvent) event;
            sampleListing.removeSample(removal.sample);
        } else if (event instanceof SheetRemovalEvent) {
            SheetRemovalEvent removal = (SheetRemovalEvent) event;
            sheetSelector.model.removeElement(removal.descriptor);
        } else {
            sheetSelector.stateChanged(event);
        }
    }

    //----------------//
    // validateSheets //
    //----------------//
    @Action
    public void validateSheets (ActionEvent e)
    {
        List<Descriptor> sheets = sheetSelector.list.getSelectedValuesList();
        int size = sheets.size();

        if (size > 0) {
            JFrame frame = new JFrame("Sheets " + sheets);
            ValidationPanel panel = new ValidationPanel(
                    null,
                    ShapeClassifier.getInstance(),
                    new ConstantSource(sheetSelector.getTestSamples()),
                    false); // false => not TRAIN but Test
            Panel comp = (Panel) panel.getComponent();
            comp.setInsets(5, 5, 5, 5); // TLBR
            frame.add(comp);
            frame.pack();
            frame.setVisible(true);
        }
    }

    //------------//
    // initialize // Method called only when in stand-alone mode
    //------------//
    @Override
    protected void initialize (String[] args)
    {
        logger.debug("SampleBrowser. 1/initialize");
    }

    //-------//
    // ready // Method called only when in stand-alone mode
    //-------//
    @Override
    protected void ready ()
    {
        logger.debug("SampleBrowser. 3/ready");

        frame.addWindowListener(
                new WindowAdapter()
        {
            @Override
            public void windowClosing (WindowEvent e)
            {
                System.exit(0); // That's all folks !
            }
        });

        // Set application exit listener
        addExitListener(repository.getExitListener());

        if (!repoChecked) {
            checkRepository(null);
        }
    }

    //---------//
    // startup // Method called only when in stand-alone mode
    //---------//
    @Override
    protected void startup ()
    {
        logger.debug("SampleBrowser. 2/startup");

        frame = defineLayout(getMainFrame());
        frame.setTitle(repository.toString());
        frame.addWindowListener(new ClosingAdapter());

        show(frame); // Here we go...
    }

    /**
     * (Package private) Report the selected shapes
     *
     * @return the selected shapes
     */
    List<Shape> getSelectedShapes ()
    {
        return shapeSelector.list.getSelectedValuesList();
    }

    /**
     * (Package private) Report the selected sheets descriptors
     *
     * @return the selected sheet descriptors
     */
    List<Descriptor> getSelectedSheets ()
    {
        return sheetSelector.list.getSelectedValuesList();
    }

    //---------------//
    // publishSample //
    //---------------//
    void publishSample (Sample sample)
    {
        sampleService.publish(
                new EntityListEvent<Sample>(
                        this,
                        SelectionHint.ENTITY_INIT,
                        MouseMovement.PRESSING,
                        Arrays.asList(sample)));
    }

    //--------------//
    // buildMenuBar //
    //--------------//
    /**
     * Build the menu bar for SampleBrowser frame.
     *
     * @return the populated menu bar
     */
    private JMenuBar buildMenuBar ()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu repoMenu = new JMenu();
        repoMenu.setName("SampleBrowserRepoMenu");
        menuBar.add(repoMenu);

        ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(this);

        // Refresh viewer
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("refresh")));

        // Load sheet images
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("loadImages")));

        repoMenu.addSeparator(); // -----------------------

        // Check for sample duplicates/conflicts
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("checkRepository")));

        if (SampleRepository.USE_TRIBES) {
            // Check for tribes classification
            repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("checkTribes")));
        }

        repoMenu.addSeparator(); // -----------------------

        if (repository.isGlobal()) {
            // Launch trainer
            repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("launchTrainer")));
        } else {
            // Push local repository to global repository
            repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("pushRepository")));
        }

        repoMenu.addSeparator(); // -----------------------

        // Save repository
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("save")));

        // Export to CSV file
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("exportFeatures")));

        repoMenu.addSeparator(); // -----------------------

        // Purge sheets
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("purgeSheets")));

        //        // Shrink the whole repository
        //        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("shrinkRepository")));
        //
        return menuBar;
    }

    //------------------//
    // connectSelectors //
    //------------------//
    private void connectSelectors (boolean bool)
    {
        if (bool) {
            sheetSelector.setListener(shapeSelector);
            shapeSelector.setListener(sampleListing);
        } else {
            sheetSelector.setListener(null);
            shapeSelector.setListener(null);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of components within the provided frame.
     *
     * @param frame the bare frame
     * @return the populated frame
     *
     */
    private JFrame defineLayout (JFrame frame)
    {
        frame.setName("SampleBrowserFrame"); // For SAF life cycle

        /*
         * |- left --||-- center ---|--------------- right ---------------|
         *
         * +=========++=============+=====================================+
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . sheet . . | . . . . . . . . shape 1 pane. . . . |
         * | . . . . || . . . . . . | . . sample. . . . . . . . . . . . . |
         * | . . . . || . selector. | . . . . . . . . shape 2 pane. . . . |
         * | . . . . || . . . . . . | . . listing . . . . . . . . . . . . |
         * | . . . . ||=============| . . . . . . . . shape 3 pane. . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . sample. . |=====================================|
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . board . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | shape . ||-------------| . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | selector|| . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . eval. . . | . . . . sample. . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . board . . | . . . . context . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * +=========++=============+=====================================+
         */
        // Left = shapeSelector
        shapeSelector.setName("shapeSelector");

        // Center
        BoardsPane boardsPane = new BoardsPane(
                new SampleBoard(sampleController),
                new SampleEvaluationBoard(sampleController));
        JSplitPane centerPane = new JSplitPane(
                VERTICAL_SPLIT,
                sheetSelector,
                boardsPane.getComponent());
        centerPane.setBorder(null);
        centerPane.setOneTouchExpandable(true);
        centerPane.setName("centerPane");

        // Right
        JSplitPane rightPane = new JSplitPane(
                VERTICAL_SPLIT,
                sampleListing,
                sampleContext.getComponent());
        rightPane.setBorder(null);
        rightPane.setOneTouchExpandable(true);
        rightPane.setName("rightPane");

        // Center + Right
        JSplitPane centerPlusRightPane = new JSplitPane(HORIZONTAL_SPLIT, centerPane, rightPane);
        centerPlusRightPane.setBorder(null);
        centerPlusRightPane.setOneTouchExpandable(true);
        centerPlusRightPane.setName("centerPlusRightPane");

        // Global
        JSplitPane mainPane = new JSplitPane(HORIZONTAL_SPLIT, shapeSelector, centerPlusRightPane);
        mainPane.setBorder(null);
        mainPane.setOneTouchExpandable(true);
        mainPane.setResizeWeight(0d); // Give all free space to center+right part
        mainPane.setName("mainPane");

        frame.add(mainPane);

        // Menu bar
        frame.setJMenuBar(buildMenuBar());

        // Resource injection
        ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resource.injectComponents(frame);

        // Wiring
        boardsPane.connect();

        // Initialize sheet selector with all repository sheet names
        sheetSelector.stateChanged(null);

        return frame;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // TitledPanel //
    //-------------//
    /**
     * A panel surrounded by an EmptyBorder and a title.
     */
    static class TitledPanel
            extends Panel
    {
        //~ Constructors ---------------------------------------------------------------------------

        public TitledPanel (String title)
        {
            setBorder(
                    BorderFactory.createTitledBorder(
                            new EmptyBorder(20, 5, 0, 0), // TLBR
                            title,
                            TitledBorder.LEFT,
                            TitledBorder.TOP));
            this.setInsets(25, 5, 0, 0);
        }
    }

    //----------------//
    // ClosingAdapter //
    //----------------//
    private class ClosingAdapter
            extends WindowAdapter
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void windowClosing (WindowEvent e)
        {
            // Check for modified repo
            Application.ExitListener exitListener = repository.getExitListener();
            boolean ok = exitListener.canExit(e);

            if (ok) {
                OmrGui.getApplication().removeExitListener(exitListener);

                if (repository.isGlobal()) {
                    INSTANCE = null;
                }

                repository.close();
                frame.dispose(); // Do close
            }
        }
    }

    //----------//
    // Selector //
    //----------//
    /**
     * Class {@code Selector} is the basis for sheet and shape selectors.
     * Each selector is made of a list of names, which can be selected and deselected at will.
     */
    private abstract static class Selector<E>
            extends TitledPanel
            implements ChangeListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        // The title base for this selector
        private final String title;

        // Other entity interested in items selected by this selector
        private ChangeListener listener;

        // Buttons
        protected final JButton selectAll = new JButton("Select All");

        protected final JButton cancelAll = new JButton("Cancel All");

        // Underlying list model
        protected final DefaultListModel<E> model = new DefaultListModel<E>();

        // Underlying list
        protected JList<E> list = new JList<E>(model);

        // ScrollPane around the list
        protected JScrollPane scrollPane = new JScrollPane(list);

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a selector.
         *
         * @param title label for this selector
         */
        public Selector (String title)
        {
            super(title);
            this.title = title;

            setLayout(new BorderLayout());
            setMinimumSize(new Dimension(0, 200));
            setPreferredSize(new Dimension(180, 200));

            // To be informed of mouse (de)selections (not programmatic)
            list.addListSelectionListener(
                    new ListSelectionListener()
            {
                @Override
                public void valueChanged (ListSelectionEvent e)
                {
                    if (!e.getValueIsAdjusting()) {
                        update();
                    }
                }
            });

            // Same action whatever the subclass: select all items
            selectAll.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    selectAll();
                }
            });

            // Same action whatever the subclass: deselect all items
            cancelAll.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    list.clearSelection();
                    update();
                }
            });

            JPanel buttons = new JPanel(new GridLayout(1, 3));

            buttons.add(selectAll);
            buttons.add(cancelAll);

            // All buttons are initially disabled
            selectAll.setEnabled(false);
            cancelAll.setEnabled(false);

            add(scrollPane, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void populateWith (Collection<E> items)
        {
            model.removeAllElements();

            for (E item : items) {
                model.addElement(item);
            }

            update();
        }

        public void select (Collection<E> items)
        {
            list.clearSelection();

            for (int i = 0, iBreak = model.size(); i < iBreak; i++) {
                E item = model.get(i);

                if (items.contains(item)) {
                    list.addSelectionInterval(i, i);
                }
            }
        }

        public void selectAll ()
        {
            list.setSelectionInterval(0, model.size() - 1);
            update();
        }

        /**
         * Set a listener to this instance
         *
         * @param listener listener for changes or null
         */
        public void setListener (ChangeListener listener)
        {
            this.listener = listener;
        }

        protected void update ()
        {
            final int selectionCount = list.getSelectedIndices().length;

            // Title
            final TitledBorder border = (TitledBorder) getBorder();
            border.setTitle(title + ((selectionCount > 0) ? (": " + selectionCount) : ""));
            repaint();

            // Buttons
            selectAll.setEnabled(model.size() > 0);
            cancelAll.setEnabled(selectionCount > 0);

            // Notify listener if any
            if (listener != null) {
                listener.stateChanged(null);
            }
        }
    }

    //---------------//
    // ShapeRenderer //
    //---------------//
    /**
     * Render a shape item within the ShapeSelector list.
     */
    private static class ShapeRenderer
            extends JLabel
            implements ListCellRenderer<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ShapeRenderer ()
        {
            setOpaque(true);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Find the image and text corresponding to the provided shape and returns the
         * label, ready to be displayed.
         */
        @Override
        public Component getListCellRendererComponent (JList<? extends Shape> list,
                                                       Shape shape,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(shape.getColor());
            }

            setFont(list.getFont());
            setText(shape.toString());
            setIcon(new FixedWidthIcon(shape.getDecoratedSymbol()));

            return this;
        }
    }

    //-----------------------//
    // SampleEvaluationBoard //
    //-----------------------//
    /**
     * An evaluation board dedicated to evaluation / reassign of samples.
     */
    private class SampleEvaluationBoard
            extends EvaluationBoard
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SampleEvaluationBoard (SampleController controller)
        {
            super(true, null, ShapeClassifier.getInstance(), controller, true);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void evaluate (Glyph glyph)
        {
            if (glyph == null) {
                // Blank the output
                selector.setEvals(null, null);
            } else {
                selector.setEvals(
                        classifier.evaluate(
                                glyph,
                                ((Sample) glyph).getInterline(),
                                selector.evalCount(),
                                0.0, // minGrade
                                Classifier.NO_CONDITIONS),
                        glyph);
            }
        }
    }

    //---------------//
    // ShapeSelector //
    //---------------//
    /**
     * Display a list of available shapes (within selected sheets) and let user make a
     * selection.
     */
    private class ShapeSelector
            extends Selector<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ShapeSelector ()
        {
            super("Shapes");
            setMinimumSize(new Dimension(100, 0));
            list.setCellRenderer(new ShapeRenderer());
            list.setComponentPopupMenu(new ShapePopup());
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            // Called from sheetSelector: Populate with shape names found in selected sheets
            final EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

            for (Descriptor desc : sheetSelector.list.getSelectedValuesList()) {
                shapeSet.addAll(repository.getShapes(desc));
            }

            populateWith(shapeSet);
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Popup on selected Shape instances (within selected SampleSheet instances).
         */
        private class ShapePopup
                extends JPopupMenu
        {
            //~ Constructors -----------------------------------------------------------------------

            public ShapePopup ()
            {
                super("ShapePopup");

                ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(
                        SampleBrowser.this);

                add(new JMenuItem((ApplicationAction) actionMap.get("removeShapes")));
            }
        }
    }

    //---------------//
    // SheetSelector //
    //---------------//
    /**
     * Display a list of available sheets and let user make a selection.
     */
    private class SheetSelector
            extends Selector<Descriptor>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SheetSelector ()
        {
            super("Sheets");

            list.setComponentPopupMenu(new SheetPopup());
        }

        //~ Methods --------------------------------------------------------------------------------
        public List<Sample> getTestSamples ()
        {
            List<Descriptor> descriptors = list.getSelectedValuesList();
            List<Sample> samples = new ArrayList<Sample>();

            for (Descriptor descriptor : descriptors) {
                for (Shape shape : repository.getShapes(descriptor)) {
                    samples.addAll(repository.getSamples(descriptor.getName(), shape));
                }
            }

            return samples;
        }

        @Override
        public void stateChanged (ChangeEvent e)
        {
            populateWith(repository.getAllDescriptors());
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Popup on selected SampleSheet instances.
         */
        private class SheetPopup
                extends JPopupMenu
        {
            //~ Constructors -----------------------------------------------------------------------

            public SheetPopup ()
            {
                super("SheetPopup");

                ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(
                        SampleBrowser.this);

                add(new JMenuItem((ApplicationAction) actionMap.get("removeSheets")));
                add(new JMenuItem((ApplicationAction) actionMap.get("validateSheets")));
            }
        }
    }
}
