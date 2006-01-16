//-----------------------------------------------------------------------//
//                                                                       //
//                        G l y p h T r a i n e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;
import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphRegression;
import omr.glyph.Shape;
import omr.math.NeuralNetwork;
import omr.ui.LDoubleField;
import omr.ui.LField;
import omr.ui.LIntegerField;
import omr.ui.Panel;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Class <code>GlyphTrainer</code> handles a User Interface dedicated to
 * the training and testing of two different evaluators
 *
 * <p>The frame is divided vertically in 3 parts:
 * <ol>
 * <li>The repository of known glyphs ({@link GlyphRepository})
 * <li>The neural network evaluator ({@link GlyphNetwork})
 * <li>The regression evaluator ({@link GlyphRegression})
 * </ol>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphTrainer
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphTrainer.class);
    private static final Constants constants = new Constants();

    // The single instance of this class
    private static GlyphTrainer INSTANCE;

    // Width of the various vertical panels of the display
    private static final int SAME_WIDTH = 450;

    // To differentiate the exit action, according to the launch context
    private static boolean standAlone = false;

    private static final String standardWidth = "50dlu";
    private static final String frameTitle = "Trainer";

    //~ Instance variables ------------------------------------------------

    // Related frame
    private final JFrame frame;

    // The evaluators
    private GlyphNetwork network = GlyphNetwork.getInstance();
    private GlyphRegression regression = GlyphRegression.getInstance();

    // Repository of known glyphs
    private final GlyphRepository repository = GlyphRepository.getInstance();

    // The various panels
    private final RepositoryPanel repositoryPanel;
    private final NetworkPanel    networkPanel;
    private final RegressionPanel regressionPanel;

    // Counter on loaded glyphs
    private int nbLoaded;

    // Counter on trained glyphs
    private int glyphNb;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // GlyphTrainer //
    //--------------//
    /**
     * Create an instance of Glyph Trainer (there should be just one)
     */
    private GlyphTrainer()
    {
        frame = new JFrame();

        frame.setTitle(frameTitle);

        repositoryPanel = new RepositoryPanel();
        networkPanel = new NetworkPanel(network);
        regressionPanel = new RegressionPanel(regression);

        frame.add(createGlobalPanel());
        frame.pack();
        frame.setVisible(true);

        if (standAlone) {
            frame.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing (WindowEvent e)
                    {
                        // Store latest constant values
                        ConstantManager.storeResource();

                        // That's all folks !
                        System.exit(0);
                    }
                });
        }
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the UI frame of glyph trainer
     *
     * @return the related frame
     */
    public JFrame getFrame()
    {
        return frame;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class, after creating it if
     * needed
     *
     * @return the single instance
     */
    public static GlyphTrainer getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphTrainer();
        }
        return INSTANCE;
    }

    //---------------//
    // setFrameTitle //
    //---------------//
    private void setFrameTitle (double error)
    {
        frame.setTitle(String.format("%.5f - %s", error, frameTitle));
    }

    //------//
    // main //
    //------//
    /**
     * Just to allow stand-alone testing of this class
     *
     * @param args not used
     */
    public static void main (String... args)
    {
        standAlone = true;
        new GlyphTrainer();
    }

    //-------------------//
    // createGlobalPanel //
    //-------------------//
    private JPanel createGlobalPanel()
    {
        final String panelInterline = Panel.getPanelInterline();
        FormLayout layout = new FormLayout
            ("pref",
             "pref," + panelInterline + "," +
             "pref");

        Panel panel = new Panel();
        //panel.setNoInsets();

        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.add(repositoryPanel,    cst.xy(1,  r));

        r += 2;                         // --------------------------------
        builder.add(networkPanel,       cst.xy(1,  r));

        return builder.getPanel();
    }

    //~ Class -------------------------------------------------------------

    //-----------------//
    // RepositoryPanel //
    //-----------------//
    private class RepositoryPanel
        extends Panel
        implements GlyphRepository.Monitor
    {
        protected ExecutorService executor =
            Executors.newSingleThreadExecutor();

        protected JProgressBar progressBar = new JProgressBar();

        // To select a core out of whole base
        SelectAction selectAction = new SelectAction();
        JButton selectButton = new JButton(selectAction);

        int filesToselect = 0;

        LIntegerField similar = new LIntegerField
            ("Max Similar", "Max number of similar shapes");

        LIntegerField totalFiles = new LIntegerField
            (false, "Total", "Total number of glyph files");

        LIntegerField nbSelectedFiles = new LIntegerField
            (false, "Selected", "Number of selected glyph files to load");

        LIntegerField nbLoadedFiles = new LIntegerField
            (false, "Loaded", "Number of glyph files loaded so far");

        //~ Constructors --------------------------------------------------

        public RepositoryPanel()
        {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"),
                     "readParams");
            getActionMap().put("readParams",
                               new ParamAction());

            displayParams();

            selectButton.setToolTipText
                ("Build core selection out of whole glyph base");

            defineLayout();
        }

        //~ Methods -------------------------------------------------------

        private void defineLayout()
        {
            FormLayout layout = Panel.makeFormLayout
                (3, 4, "", standardWidth, standardWidth);
            PanelBuilder builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder();
            CellConstraints cst = new CellConstraints();

            int r = 1;                      // ----------------------------
            builder.addSeparator("Repository",  cst.xyw(1,  r, 7));
            builder.add(progressBar,            cst.xyw(9,  r, 7));

            r += 2;                         // ----------------------------
            builder.add(selectButton,           cst.xy (3,  r));

            builder.add(similar.getLabel(),     cst.xy (9,  r));
            builder.add(similar.getField(),     cst.xy (11, r));

            builder.add(totalFiles.getLabel(),  cst.xy (13, r));
            builder.add(totalFiles.getField(),  cst.xy (15, r));

            r += 2;                         // ----------------------------
            builder.add(nbSelectedFiles.getLabel(), cst.xy (9,  r));
            builder.add(nbSelectedFiles.getField(), cst.xy (11, r));

            builder.add(nbLoadedFiles.getLabel(), cst.xy (13, r));
            builder.add(nbLoadedFiles.getField(), cst.xy (15, r));
        }

        private Collection<String> getBase (boolean whole)
        {
            nbLoaded = 0;
            progressBar.setValue(nbLoaded);
            if (whole) {
                return repository.getWholeBase(this);
            } else {
                return repository.getCoreBase(this);
            }
        }

        private void defineCore()
        {
            class NotedGlyph
            {
                String gName;
                Glyph  glyph;
                double grade;
            }

            inputParams();              // What for ? TBD

            // Train regression on them
            regressionPanel.trainAction.train(/* whole => */ true);

            // Measure every glyph
            Collection<String> gNames = getBase(/* whole => */ true);
            List<NotedGlyph> palmares =
                new ArrayList<NotedGlyph>(gNames.size());
            for (String gName : gNames){
                NotedGlyph ng = new NotedGlyph();
                ng.gName = gName;
                ng.glyph = repository.getGlyph(gName);
                ng.grade = regression.measureDistance(ng.glyph,
                                                      ng.glyph.getShape());
                palmares.add(ng);
            }

            // Sort the palmares, shape by shape, by decreasing grade, so
            // that the worst glyphs are found first
            Collections.sort(palmares,
                             new Comparator<NotedGlyph>()
                             {
                                 public int compare (NotedGlyph ng1,
                                                     NotedGlyph ng2)
                                 {
                                     if (ng1.grade < ng2.grade) {
                                         return +1;
                                     }
                                     if (ng1.grade > ng2.grade) {
                                         return -1;
                                     }
                                     return 0;
                                 }
                             });

            // Set of chosen shapes
            Set<NotedGlyph> set = new HashSet<NotedGlyph>();

            // Allocate shape-based counters
            int[] counters = new int[Evaluator.outSize];
            Arrays.fill(counters, 0);
            final int maxSimilar = (similar.getValue() +1) /2;

            // Keep only MaxSimilar/2 of each WORST shape
            for (NotedGlyph ng : palmares) {
                int index = ng.glyph.getShape().ordinal();
                if (++counters[index] <= maxSimilar) {
                    set.add(ng);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%.5f worst Core %s",
                                                   ng.grade, ng.gName));
                    }
                }
            }

            // Keep only MaxSimilar/2 of each BEST shape
            // We just have to browse backward
            Arrays.fill(counters, 0);
            for (ListIterator<NotedGlyph> it
                     = palmares.listIterator(palmares.size() -1);
                 it.hasPrevious();) {
                NotedGlyph ng = it.previous();
                int index = ng.glyph.getShape().ordinal();
                if (++counters[index] <= maxSimilar) {
                    set.add(ng);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%.5f best Core %s",
                                                   ng.grade, ng.gName));
                    }
                }
            }

            // Build the core base
            List<String> base = new ArrayList<String>(set.size());
            for (NotedGlyph ng : set) {
                base.add(ng.gName);
            }

            repository.setCoreBase(base);
            repositoryPanel.setSelectedGlyphs(base.size());
        }

        public void enableSelect()
        {
            if (networkPanel.onTraining || regressionPanel.onTraining) {
                selectAction.setEnabled(false);
            } else {
                selectAction.setEnabled(true);
            }
        }

        public void loadedGlyph (Glyph glyph)
        {
            nbLoadedFiles.setValue(++nbLoaded);
            progressBar.setValue(nbLoaded);
        }

        public void setSelectedGlyphs (int selected)
        {
            nbSelectedFiles.setValue(selected);
        }

        public void setTotalGlyphs (int total)
        {
            totalFiles.setValue(total);
            progressBar.setMaximum(total);
        }

        private void inputParams()
        {
            constants.maxSimilar.setValue(similar.getValue());
        }

        private void displayParams()
        {
            similar.setValue(constants.maxSimilar.getValue());
        }

        protected class ParamAction
            extends AbstractAction
        {
            // Purpose is just to read and remember the data from the
            // various input fields. Triggered when user presses Enter in
            // one of these fields.
            public void actionPerformed (ActionEvent e)
            {
                inputParams();
                displayParams();
            }
        }

        private class SelectAction
            extends AbstractAction
        {
            public SelectAction ()
            {
                super("Select Core");
            }

            public void actionPerformed (ActionEvent e)
            {
                executor.execute(new Runnable()
                    {
                        public void run()
                        {
                            // Disable some actions
                            networkPanel.enableTraining(false);
                            regressionPanel.enableTraining(false);
                            selectAction.setEnabled(false);

                            // Define Core from Whole
                            defineCore();
                            repository.storeCoreBase();

                            // Enable some actions
                            networkPanel.enableTraining(true);
                            regressionPanel.enableTraining(true);
                            enableSelect();
                        }
                    });
            }
        }
    }

    //~ Class -------------------------------------------------------------

    //----------------//
    // EvaluatorPanel //
    //----------------//
    private class EvaluatorPanel
        extends Panel
        implements Evaluator.Monitor
    {
        // Flag to signal that a training is currently going on
        protected volatile boolean onTraining = false;

        protected ExecutorService executor = Executors.newSingleThreadExecutor();
        protected Evaluator evaluator;
        protected JProgressBar progressBar = new JProgressBar();

        protected List<String> negatives = new ArrayList<String>();
        protected List<String> falsePositives = new ArrayList<String>();

        protected Panel trainingPanel = new Panel();
        protected TrainAction trainAction;

        protected Panel validationPanel = new Panel();
        protected ValidateAction validateAction = new ValidateAction();
        protected LIntegerField positiveValue = new LIntegerField
            (false, "Glyphs OK", "Number of glyphs correctly recognized");
        protected LDoubleField pcValue = new LDoubleField
            (false, "% OK", "Percentage of recognized glyphs");
        protected LIntegerField negativeValue = new LIntegerField
            (false, "Negative", "Number of glyphs not recognized");
        protected LIntegerField falsePositiveValue = new LIntegerField
            (false, "False Pos.", "Number of glyphs incorrectly recognized");

        protected NegativeAction negativeAction = new NegativeAction();
        protected FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

        // Radio buttons.
        protected boolean useWhole = true;
        protected ButtonGroup group = new ButtonGroup();
        protected JRadioButton wholeButton =
            new JRadioButton(new WholeAction());
        protected JRadioButton coreButton =
            new JRadioButton(new CoreAction());

        protected FormLayout      layout;
        protected PanelBuilder    builder;
        protected CellConstraints cst = new CellConstraints();

        //~ Constructors --------------------------------------------------

        public EvaluatorPanel(Evaluator evaluator,
                              int       rows,
                              int       validationRow)
        {
            this.evaluator = evaluator;

            layout = Panel.makeFormLayout
                (rows, 4, "", standardWidth, standardWidth);

            builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder(); // Useful ?

            negativeAction.setEnabled(negatives.size() > 0);
            falsePositiveAction.setEnabled(falsePositives.size() > 0);

            //Group the radio buttons.
            group.add(wholeButton);
            wholeButton.setToolTipText("Use the whole glyph base for any action");
            group.add(coreButton);
            coreButton.setToolTipText("Use only the core glyph base for any action");
            wholeButton.setSelected(true);

            defineLayout(validationRow);
        }

        //~ Methods -------------------------------------------------------

        protected void defineLayout(int validationRow)
        {
            // Evaluator Title & Progress Bar
            int r = 1;                      // ----------------------------
            builder.addSeparator(evaluator.getName(),   cst.xyw(1, r, 7));
            builder.add(progressBar,                    cst.xyw(9, r, 7));

            r += 2;                         // ----------------------------
            builder.add(wholeButton,                    cst.xy(1,  r));

            r += 2;                         // ----------------------------
            builder.add(coreButton,                     cst.xy(1,  r));

            // Validation part
            r = validationRow;
            builder.addSeparator("Validation",          cst.xyw(3, r, 13));

            r += 2;                         // ----------------------------
            JButton validateButton = new JButton(validateAction);
            validateButton.setToolTipText
                ("Validate the evaluator on current base of glyphs");

            builder.add(validateButton,                 cst.xy(3,  r));
            builder.add(positiveValue.getLabel(),       cst.xy(5,  r));
            builder.add(positiveValue.getField(),       cst.xy(7,  r));
            builder.add(negativeValue.getLabel(),       cst.xy(9,  r));
            builder.add(negativeValue.getField(),       cst.xy(11, r));
            builder.add(falsePositiveValue.getLabel(),  cst.xy(13, r));
            builder.add(falsePositiveValue.getField(),  cst.xy(15, r));

            r += 2;                         // ----------------------------
            JButton negativeButton = new JButton(negativeAction);
            negativeButton.setToolTipText
                ("Display the impacted glyphs for verification");

            JButton falsePositiveButton = new JButton(falsePositiveAction);
            falsePositiveButton.setToolTipText
                ("Display the impacted glyphs for verification");

            builder.add(pcValue.getLabel(),     cst.xy(5,  r));
            builder.add(pcValue.getField(),     cst.xy(7,  r));
            builder.add(negativeButton,         cst.xy(11, r));
            builder.add(falsePositiveButton,    cst.xy(15, r));
        }

        //---------------//
        // runValidation //
        //---------------//
        private void runValidation ()
        {
            logger.info("Validating " + evaluator.getName() +
                        " evaluator on " +
                        (useWhole ? "whole" : "core") + " base ...");

            // Empty the display
            positiveValue.setText("");
            pcValue.setText("");
            negativeValue.setText("");
            falsePositiveValue.setText("");
            negativeAction.setEnabled(false);
            falsePositiveAction.setEnabled(false);

            negatives.clear();
            falsePositives.clear();

            int positives = 0;
            final double maxGrade = constants.maxGrade.getValue();
            Collection<String> gNames = repositoryPanel.getBase(useWhole);
            for (String gName : gNames) {
                Glyph glyph = repository.getGlyph(gName);
                if (glyph != null) {
                    Shape vote = evaluator.vote(glyph, maxGrade);
                    if (vote == glyph.getShape()) {
                        positives++;
                    } else if (vote == null) {
                        negatives.add(gName);
                        System.out.printf
                            ("%-35s: Not recognized%n", gName);
                    } else {
                        falsePositives.add(gName);
                        System.out.printf
                            ("%-35s: Mistaken as %s%n", gName, vote);
                    }
                }
            }

            int total = gNames.size();
            double pc = (double) positives * 100 / (double) total;
            String pcStr = String.format(" %5.2f%%", pc);
            logger.info(evaluator.getName() + "Evaluator. Ratio=" + pcStr
                        + " : " + positives + "/" + total);
            positiveValue.setValue(positives);
            pcValue.setValue(pc, " %5.2f%%");
            negativeValue.setValue(negatives.size());
            falsePositiveValue.setValue(falsePositives.size());
        }

        public void enableTraining(boolean bool)
        {
            trainAction.setEnabled(bool);
            negativeAction.setEnabled(negatives.size() > 0);
            falsePositiveAction.setEnabled(falsePositives.size() > 0);
        }

        protected class WholeAction
            extends AbstractAction
        {
            public WholeAction ()
            {
                super("Whole");
            }

            public void actionPerformed (ActionEvent e)
            {
                useWhole = true;
            }
        }

        protected class CoreAction
            extends AbstractAction
        {
            public CoreAction ()
            {
                super("Core");
            }

            public void actionPerformed (ActionEvent e)
            {
                useWhole = false;
            }
        }

        protected class DumpAction
            extends AbstractAction
        {
            public DumpAction ()
            {
                super("Dump");
            }

            public void actionPerformed (ActionEvent e)
            {
                evaluator.dump();
            }
        }

        protected class TrainAction
            extends AbstractAction
        {
            // Specific training starting mode
            protected Evaluator.StartingMode mode
                = Evaluator.StartingMode.SCRATCH;

            protected boolean confirmationRequired = true;

            public TrainAction (String title)
            {
                super(title);
            }

            public void actionPerformed (ActionEvent e)
            {
                // Ask user confirmation
                if (confirmationRequired) {
                    if (JOptionPane.showConfirmDialog
                        (EvaluatorPanel.this,
                         "Do you really want to retrain from scratch ?")
                        != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                class Worker
                    extends Thread
                {
                    public void run()
                    {
                        train(useWhole);
                    }
                }

                Worker worker = new Worker();
                worker.setPriority(Thread.MIN_PRIORITY);
                worker.start();
            }

            protected void prologue()
            {
            }

            protected void epilogue()
            {
            }

            public void train (boolean useWhole)
            {
                onTraining = true;
                repositoryPanel.enableSelect();
                EvaluatorPanel.this.enableTraining(false);

                glyphNb = 0;
                prologue();

                Collection<String> gNames = repositoryPanel.getBase(useWhole);
                progressBar.setValue(0);
                progressBar.setMaximum(gNames.size());

                List<Glyph> glyphs = new ArrayList<Glyph>();
                for (String gName : gNames) {
                    glyphs.add(repository.getGlyph(gName));
                }

                evaluator.train(glyphs, EvaluatorPanel.this, mode);

                EvaluatorPanel.this.enableTraining(true);
                epilogue();
                onTraining = false;
                repositoryPanel.enableSelect();
            }
        }

        protected class ValidateAction
            extends AbstractAction
        {
            public ValidateAction ()
            {
                super("Validate");
            }

            public void actionPerformed (ActionEvent e)
            {
                executor.execute(new Runnable()
                    {
                        public void run()
                        {
                            setEnabled(false);
                            runValidation();
                            negativeAction.setEnabled
                                (negatives.size() > 0);
                            falsePositiveAction.setEnabled
                                (falsePositives.size() > 0);
                            setEnabled(true);
                        }
                    });
            }
        }

        public void trainingStarted (final int    epochIndex,
                                     final double mse)
        {
        }

        public void glyphProcessed (final Glyph glyph)
        {
        }

        public void epochEnded (int    epochIndex,
                                double mse)
        {
        }

        protected class NegativeAction
            extends AbstractAction
        {
            public NegativeAction()
            {
                super("Verify");
            }

            public void actionPerformed (ActionEvent e)
            {
                GlyphVerifier.getInstance().verify(negatives);
                GlyphVerifier.getInstance().getFrame().setVisible(true);
            }
        }

        protected class FalsePositiveAction
            extends AbstractAction
        {
            public FalsePositiveAction()
            {
                super("Verify");
            }

            public void actionPerformed (ActionEvent e)
            {
                GlyphVerifier.getInstance().verify(falsePositives);
                GlyphVerifier.getInstance().getFrame().setVisible(true);
            }
        }
    }

    //~ Class -------------------------------------------------------------

    //-----------------//
    // RegressionPanel //
    //-----------------//
    private class RegressionPanel
        extends EvaluatorPanel
    {
        protected LIntegerField trainIndex = new LIntegerField
            (false, "Index", "Index of last shape used for training");

        //~ Constructors --------------------------------------------------

        public RegressionPanel(Evaluator evaluator)
        {
            super(evaluator, 5, 5);

            trainAction = new TrainAction("Re-Train");
            enableTraining(true);

            defineSpecificLayout();
        }

        //~ Methods -------------------------------------------------------

        private void defineSpecificLayout()
        {
            int r = 3;

            JButton dumpButton = new JButton(new DumpAction());
            dumpButton.setToolTipText("Dump the evaluator internals");

            JButton trainButton = new JButton(trainAction);
            trainButton.setToolTipText("Re-Train the evaluator from scratch");

            builder.add(dumpButton,             cst.xy( 3, r));
            builder.add(trainButton,            cst.xy( 5, r));
            builder.add(trainIndex.getLabel(),  cst.xy(13, r));
            builder.add(trainIndex.getField(),  cst.xy(15, r));
        }

        @Override
        public void glyphProcessed (final Glyph glyph)
        {
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run ()
                    {
                        trainIndex.setValue(++glyphNb);
                        progressBar.setValue(glyphNb);
                        repaint();
                    }
                });
        }
    }

    //~ Class -------------------------------------------------------------

    //--------------//
    // NetworkPanel //
    //--------------//
    private class NetworkPanel
        extends EvaluatorPanel
    {
        IncrementalTrainAction incrementalTrainAction;

        LField eta = new LField("ETA", "Estimated time for end of training");
        LDoubleField momentum = new LDoubleField
            ("Momentum", "Momentum value for the neural network");
        LDoubleField learningRate = new LDoubleField
            ("Learning Rate", "Learning rate of the neural network");
        LIntegerField listEpochs = new LIntegerField
            ("Epochs", "Maximum number of iterations to perform");
        LDoubleField maxError = new LDoubleField
            ("Max Error", "Error threshold to stop learning");

        StopAction stopAction = new StopAction();
        SnapAction snapAction = new SnapAction();
        LastAction lastAction = new LastAction();

        LDoubleField trainError = new LDoubleField
            (false, "Last Error", "Last value of remaining error");
        LDoubleField snapError = new LDoubleField
            (false, "Snap Error", "Best recorded value of remaining error");
        LIntegerField trainIndex = new LIntegerField
            (false, "Last Index", "Number of iterations performed so far");
        LIntegerField snapIndex = new LIntegerField
            (false, "Snap Index", "Index of best configuration so far");

        double bestMse;
        double lastMse;
        NeuralNetwork.Backup bestSnap;
        NeuralNetwork.Backup lastSnap;

        long startTime;                 // Training start time
        DateFormat dateFormat = DateFormat.getDateTimeInstance
            (DateFormat.MEDIUM,         // Date
             DateFormat.MEDIUM);        // Time

        //~ Constructors --------------------------------------------------

        public NetworkPanel(GlyphNetwork evaluator)
        {
            super(evaluator, 9, 13);

            eta.getField().setEditable(false); // ETA is just an output

            getInputMap
                (JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "readParams");
            getActionMap().put("readParams",
                               new ParamAction());

            trainAction = new TrainAction("Re-Train");
            incrementalTrainAction = new IncrementalTrainAction("Inc-Train");
            stopAction.setEnabled(false);
            lastAction.setEnabled(false);
            snapAction.setEnabled(false);
            enableTraining(true);

            defineSpecificLayout();
            displayParams();
        }

        //~ Methods -------------------------------------------------------

        private void defineSpecificLayout()
        {
            int r = 3;
            // ETA field
            builder.add(eta.getLabel(),         cst.xy(  9, r));
            builder.add(eta.getField(),         cst.xyw(11, r, 5));

            // Neural network parameters
            r += 2;                         // ----------------------------
            builder.add(momentum.getLabel(),    cst.xy(9,  r));
            builder.add(momentum.getField(),    cst.xy(11, r));

            builder.add(learningRate.getLabel(),cst.xy(13, r));
            builder.add(learningRate.getField(),cst.xy(15, r));

            r += 2;                         // ----------------------------
            builder.add(maxError.getLabel(),    cst.xy(9,  r));
            builder.add(maxError.getField(),    cst.xy(11, r));

            builder.add(listEpochs.getLabel(),  cst.xy(13, r));
            builder.add(listEpochs.getField(),  cst.xy(15, r));

            // Training entities
            r += 2;                         // ----------------------------

            JButton dumpButton = new JButton(new DumpAction());
            dumpButton.setToolTipText("Dump the evaluator internals");

            JButton trainButton = new JButton(trainAction);
            trainButton.setToolTipText("Re-Train the evaluator from scratch");

            JButton snapButton = new JButton(snapAction);
            snapButton.setToolTipText("Use the weights of best snap");

            builder.add(dumpButton,             cst.xy(3,  r));
            builder.add(trainButton,            cst.xy(5,  r));
            builder.add(snapButton,             cst.xy(7,  r));

            builder.add(snapError.getLabel(),   cst.xy(9,  r));
            builder.add(snapError.getField(),   cst.xy(11, r));

            builder.add(snapIndex.getLabel(),   cst.xy(13, r));
            builder.add(snapIndex.getField(),   cst.xy(15, r));

            r += 2;                         // ----------------------------

            JButton stopButton = new JButton(stopAction);
            stopButton.setToolTipText("Stop the training of the evaluator");

            JButton lastButton = new JButton(lastAction);
            lastButton.setToolTipText("Use the last weights");

            JButton incTrainButton = new JButton(incrementalTrainAction);
            incTrainButton.setToolTipText
                ("Incrementally train the evaluator");

            builder.add(stopButton,             cst.xy(3,  r));
            builder.add(incTrainButton,         cst.xy(5,  r));
            builder.add(lastButton,             cst.xy(7,  r));

            builder.add(trainError.getLabel(),  cst.xy(9,  r));
            builder.add(trainError.getField(),  cst.xy(11, r));

            builder.add(trainIndex.getLabel(),  cst.xy(13, r));
            builder.add(trainIndex.getField(),  cst.xy(15, r));
        }

        @Override
        public void enableTraining(boolean bool)
        {
            super.enableTraining(bool);
            incrementalTrainAction.setEnabled(bool);
            snapAction.setEnabled(false);
            lastAction.setEnabled(false);
        }

        private void inputParams()
        {
            GlyphNetwork network = (GlyphNetwork) evaluator;
            network.setListEpochs(listEpochs.getValue());
            network.setLearningRate(learningRate.getValue());
            network.setMomentum(momentum.getValue());
            network.setMaxError(maxError.getValue());

            progressBar.setMaximum(network.getListEpochs());
        }

        private void displayParams()
        {
            GlyphNetwork network = (GlyphNetwork) evaluator;
            listEpochs.setValue(network.getListEpochs());
            learningRate.setValue(network.getLearningRate());
            momentum.setValue(network.getMomentum());
            maxError.setValue(network.getMaxError());
        }

        @Override
        public void trainingStarted (final int    epochIndex,
                                     final double mse)
        {
            // This part is run on trainer thread
            final int index = epochIndex +1;
            NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
            bestSnap = network.backup();
            bestMse = mse;

            SwingUtilities.invokeLater(new Runnable()
                {
                    // This part is run on swing thread
                    public void run ()
                    {
                        // Update snap values
                        snapIndex.setValue(index);
                        snapError.setValue(mse);
                        setFrameTitle(mse);

                        // Remember starting time
                        startTime = System.currentTimeMillis();
                    }
                });
        }

        @Override
        public void epochEnded (final int    epochIndex,
                                final double mse)
        {
            // This part is run on trainer thread
            final int index = epochIndex +1;
            lastMse = mse;
            boolean snap = false;

            if (mse < bestMse) {
                bestMse = mse;
                if (bestMse <= constants.maxSnapError.getValue()) {
                    // This is a good result, take a snap
                    NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
                    bestSnap = network.backup();
                    snap = true;
                }
            }
            final boolean snapTaken = snap;

            SwingUtilities.invokeLater(new Runnable()
                {
                    // This part is run on swing thread
                    public void run ()
                    {
                        // Update current values
                        trainIndex.setValue(index);
                        trainError.setValue(mse);

                        // Update snap values
                        if (snapTaken) {
                            snapIndex.setValue(index);
                            snapError.setValue(mse);
                            setFrameTitle(mse);
                        }

                        // Update progress bar ?
                        progressBar.setValue(index);

                        // Compute ETA
                        long sofar = System.currentTimeMillis() - startTime;
                        long total = network.getListEpochs() * sofar / index;
                        Date etaDate = new Date(startTime + total);
                        eta.setText(dateFormat.format(etaDate));

                        repaint();
                    }
                });
        }

        protected class ParamAction
            extends AbstractAction
        {
            // Purpose is just to read and remember the data from the
            // various input fields. Triggered when user presses Enter in
            // one of these fields.
            public void actionPerformed (ActionEvent e)
            {
                inputParams();
                displayParams();
            }
        }

        protected class IncrementalTrainAction
            extends EvaluatorPanel.TrainAction
        {
            public IncrementalTrainAction (String title)
            {
                super(title);
                mode = Evaluator.StartingMode.INCREMENTAL;
                confirmationRequired = false;
            }

            @Override
            protected void prologue()
            {
                inputParams();
                displayParams();
                stopAction.setEnabled(true);
                bestMse = Double.MAX_VALUE;
                bestSnap = null;
                snapAction.setEnabled(false);
                lastAction.setEnabled(false);
            }

            @Override
            protected void epilogue()
            {
                stopAction.setEnabled(false);
                NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
                lastSnap = network.backup();

                // By default, use snap stuff
                if (bestMse <= lastMse) {
                    System.out.println("snapAction best=" + bestMse + " lastMse=" + lastMse);
                    snapAction.actionPerformed(null);
                }
            }
        }

        protected class TrainAction
            extends IncrementalTrainAction
        {
            public TrainAction (String title)
            {
                super(title);
                mode = Evaluator.StartingMode.SCRATCH;
                confirmationRequired = true;
            }

            @Override
            protected void prologue()
            {
                super.prologue();

                bestMse = Double.MAX_VALUE;
            }
        }

        protected class StopAction
            extends AbstractAction
        {
            public StopAction ()
            {
                super("Stop");
            }

            public void actionPerformed (ActionEvent e)
            {
                evaluator.stop();
            }
        }

        protected class SnapAction
            extends AbstractAction
        {
            public SnapAction ()
            {
                super("Use Snap");
            }

            public void actionPerformed (ActionEvent e)
            {
                NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
                network.restore(bestSnap);
                setEnabled(false);
                lastAction.setEnabled(true);
            }
        }

        protected class LastAction
            extends AbstractAction
        {
            public LastAction ()
            {
                super("Use Last");
            }

            public void actionPerformed (ActionEvent e)
            {
                if (JOptionPane.showConfirmDialog
                    (NetworkPanel.this,
                     "Do you want to switch to this non-optimal network ?")
                    != JOptionPane.YES_OPTION) {
                    return;
                }

                NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
                network.restore(lastSnap);
                setEnabled(false);
                snapAction.setEnabled(true);
            }
        }
    }

    //~ Class -------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.Double maxGrade = new Constant.Double
                (1.2,
                 "Maximum acceptance grade");

        Constant.Integer maxSimilar = new Constant.Integer
                (20,
                 "Absolute maximum number of instances for the same shape used in training");

        Constant.Double maxSnapError = new Constant.Double
                (0.3,
                 "Maximum error level to record Network snapshots");

        Constants ()
        {
            initialize();
        }
    }
}
