//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S a m p l e V e r i f i e r                                   //
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

import omr.WellKnowns;

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.Flock;
import omr.classifier.NeuralClassifier;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleRepository.AdditionEvent;
import omr.classifier.SampleRepository.RemovalEvent;
import omr.classifier.SampleRepository.SheetRemovalEvent;
import omr.classifier.SampleSheet;
import omr.classifier.SampleSource.ConstantSource;
import omr.classifier.ShapeDescription;
import omr.classifier.SheetContainer.Descriptor;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.ui.EvaluationBoard;

import omr.ui.BoardsPane;
import omr.ui.OmrGui;
import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;
import omr.ui.util.FixedWidthIcon;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;
import omr.ui.util.UIUtil;

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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
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
 * Class {@code SampleVerifier} provides a user interface to browse through all samples
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
public class SampleVerifier
        extends SingleFrameApplication
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleVerifier.class);

    /** The unique class instance. */
    private static volatile SampleVerifier INSTANCE;

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    /** Events that can be published on the local sample service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{EntityListEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Repository of training samples. */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** The dedicated frame. */
    private JFrame frame;

    /** Local service for sample events. */
    private final EntityService<Sample> sampleService;

    /** View of sample in sheet context. */
    private final SampleContext sampleContext = new SampleContext();

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
     * Create an instance of {@code SampleVerifier}.
     */
    private SampleVerifier ()
    {
        sampleService = new EntityService<Sample>("sample service", null, eventsAllowed);
        sampleContext.connect(sampleService);
        sampleModel = new SampleModel(repository, sampleService);
        sampleController = new SampleController(sampleModel);

        // Connect selectors (sheets -> shapes -> samples)
        sampleListing = new SampleListing(this);
        connectSelectors(true);

        // Stay informed of repository dynamic updates
        repository.addListener(this);

        if (!standAlone) {
            if (!repository.isLoaded()) {
                repository.loadRepository(true);
            }

            frame = defineLayout(new JFrame());
        } else {
            INSTANCE = this;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // checkFlocks //
    //-------------//
    /**
     * Check how sample flocks are classified
     *
     * @param e the event which triggered this action, perhaps null
     */
    @Action
    public void checkFlocks (ActionEvent e)
    {
        final NeuralClassifier classifier = NeuralClassifier.getInstance();

        for (Descriptor descriptor : repository.getAllDescriptors()) {
            final SampleSheet sampleSheet = repository.getSampleSheet(descriptor);
            final List<Flock> flocks = sampleSheet.getFlocks();

            if (!flocks.isEmpty()) {
                logger.info("Sheet {}", sampleSheet);

                for (Flock flock : flocks) {
                    final Sample best = flock.getBest();
                    final int iline = best.getInterline();
                    final int ord = best.getShape().ordinal();

                    final Evaluation[] bestEvals = classifier.getNaturalEvaluations(best, iline);
                    final Evaluation bestEval = bestEvals[ord];
                    logger.info("Flock best: {} {}", bestEval, best);

                    for (Sample good : flock.getGoods()) {
                        Evaluation[] evals = classifier.getNaturalEvaluations(good, iline);
                        logger.info("      good: {} {}", evals[ord], good);
                    }

                    for (Sample other : flock.getOthers()) {
                        Evaluation[] evals = classifier.getNaturalEvaluations(other, iline);
                        final Evaluation eval = evals[ord];

                        if (eval.grade >= bestEval.grade) {
                            logger.warn("     other: {} {} ABNORMAL", eval, other);
                        } else {
                            logger.info("     other: {} {}", eval, other);
                        }
                    }
                }
            }
        }
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

    //------------//
    // displayAll //
    //------------//
    /**
     * Focus the verifier on a whole collection of samples, bypassing the usual
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
        Set<Descriptor> descSet = new HashSet<Descriptor>();

        for (Sample sample : samples) {
            descSet.add(repository.getSheetDescriptor(sample));
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
     * Generate a file (format csv) to be used by deep learning software,
     * populated by samples features.
     *
     * @param e unused
     * @throws FileNotFoundException
     */
    @Action
    public void exportFeatures (ActionEvent e)
            throws FileNotFoundException
    {
        Path path = WellKnowns.TRAIN_FOLDER.resolve("features.csv");
        OutputStream os = new FileOutputStream(path.toFile());
        final PrintWriter out = getPrintWriter(os);

        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        for (Sample sample : samples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());

            for (double in : ins) {
                out.print((float) in);
                out.print(",");
            }

            ///out.println(sample.getShape().getPhysicalShape());
            out.println(sample.getShape().getPhysicalShape().ordinal());
        }

        out.flush();
        out.close();
        logger.info("Samples features saved in " + path.toAbsolutePath());

        final List<String> names = Arrays.asList(ShapeSet.getPhysicalShapeNames());

        // Shape names
        StringBuilder sb = new StringBuilder("{ //\n");

        for (int i = 0; i < names.size(); i++) {
            String comma = (i < (names.size() - 1)) ? "," : "";
            sb.append(String.format("\"%-18s // %3d\n", names.get(i) + "\"" + comma, i));
        }

        sb.append("};");
        System.out.println(sb.toString());
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Give access to the single instance of this class.
     *
     * @return the SampleVerifier instance
     */
    public static SampleVerifier getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SampleVerifier();
        }

        return INSTANCE;
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
        SampleRepository.getInstance().loadRepository(true);

        // Set UI Look and Feel
        UILookAndFeel.setUI(null);
        Locale.setDefault(Locale.ENGLISH);

        // Off we go...
        Application.launch(SampleVerifier.class, args);
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
            final Descriptor descriptor = repository.getSheetDescriptor(addition.sample);
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
                    NeuralClassifier.getInstance(),
                    new ConstantSource(sheetSelector.getTestSamples()));
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
        logger.debug("SampleVerifier. 1/initialize");
    }

    //-------//
    // ready // Method called only when in stand-alone mode
    //-------//
    @Override
    protected void ready ()
    {
        logger.debug("SampleVerifier. 3/ready");

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
        logger.debug("SampleVerifier. 2/startup");

        frame = defineLayout(getMainFrame());

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

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (OutputStream os)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(os, WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            logger.warn("Error creating PrintWriter " + ex, ex);

            return null;
        }
    }

    //--------------//
    // buildMenuBar //
    //--------------//
    /**
     * Build the menu bar for SampleVerifier frame.
     *
     * @return the populated menu bar
     */
    private JMenuBar buildMenuBar ()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu repoMenu = new JMenu();
        repoMenu.setName("SampleVerifierRepoMenu");
        menuBar.add(repoMenu);

        ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(this);

        // Refresh viewer
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("refresh")));

        // Launch trainer
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("launchTrainer")));

        // Save repository
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("save")));

        repoMenu.addSeparator(); // -----------------------

        // Load sheet images
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("loadImages")));

        // Check for sample duplicates/conflicts
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("checkRepository")));

        if (SampleRepository.USE_FLOCKS) {
            // Check for flocks classification
            repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("checkFlocks")));
        }

        repoMenu.addSeparator(); // -----------------------

        // Export to CSV file
        repoMenu.add(new JMenuItem((ApplicationAction) actionMap.get("exportFeatures")));

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
        frame.setName("SampleVerifierFrame"); // For SAF life cycle

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

    //------------//
    // loadImages //
    //------------//
    /**
     * Action to load sheet images
     *
     * @param e the event which triggered this action
     */
    @Action
    private void loadImages (ActionEvent e)
    {
        repository.loadSheetImages();
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-----------//
    // Removable //
    //-----------//
    private interface Removable<E>
    {
        //~ Methods --------------------------------------------------------------------------------

        String getTip ();

        void remove (List<E> entities);
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
            super(true, null, NeuralClassifier.getInstance(), controller, true);
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
                        SampleVerifier.this);

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
            List<Descriptor> sheets = list.getSelectedValuesList();
            List<Sample> samples = new ArrayList<Sample>();

            for (Descriptor sheet : sheets) {
                for (Shape shape : repository.getShapes(sheet)) {
                    samples.addAll(repository.getSamples(sheet.id, shape));
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
                        SampleVerifier.this);

                add(new JMenuItem((ApplicationAction) actionMap.get("removeSheets")));
                add(new JMenuItem((ApplicationAction) actionMap.get("validateSheets")));
            }
        }
    }
}
