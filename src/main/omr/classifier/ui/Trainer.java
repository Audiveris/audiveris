//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T r a i n e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.OMR;

import omr.classifier.NeuralClassifier;

import omr.constant.ConstantManager;

import omr.ui.OmrGui;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Observable;

import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code Trainer} handles a User Interface dedicated to the
 * training and testing of a glyph classifier.
 * <p>
 * The frame is divided vertically in 4 parts:
 * <ol>
 * <li>The selection in repository of known glyphs ({@link SelectionPanel})
 * <li>The training of the neural network classifier ({@link TrainingPanel})
 * <li>The validation of the neural network classifier ({@link ValidationPanel})
 * <li>The training of the bayesian classifier ({@link BayesianPanel})
 * </ol>
 * This class can be launched as a stand-alone program.
 *
 * @author Hervé Bitteur
 */
public class Trainer
        extends SingleFrameApplication
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Trainer.class);

    /** The single instance of this class */
    private static volatile Trainer INSTANCE;

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    /** Standard width for fields/buttons in DLUs */
    private static final String standardWidth = "50dlu";

    /** An adapter triggered on window closing */
    private static final WindowAdapter windowCloser = new WindowAdapter()
    {
        @Override
        public void windowClosing (WindowEvent e)
        {
            // Store latest constant values
            ConstantManager.getInstance().storeResource();

            // That's all folks !
            System.exit(0);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related frame */
    private JFrame frame;

    /** Panel for selection in repository */
    private final SelectionPanel selectionPanel;

    /** Panel for Neural network training */
    private final NeuralPanel networkPanel;

    /** Panel for Neural network validation */
    private final ValidationPanel validationPanel;
//
//    /** Panel for Bayesian "training" */
//    private final TrainingPanel bayesianPanel;
//
//    /** Panel for Bayesian validation */
//    private final ValidationPanel bayesianValidationPanel;
//

    /** Current task */
    private final Task task = new Task();

    /** Frame title */
    private String frameTitle;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance of Glyph Trainer (there should be just one)
     */
    public Trainer ()
    {
        // Create the 5 companions
        selectionPanel = new SelectionPanel(task, standardWidth);
        networkPanel = new NeuralPanel(task, standardWidth, selectionPanel);
        selectionPanel.setTrainingPanel(networkPanel);
        validationPanel = new ValidationPanel(
                task,
                standardWidth,
                NeuralClassifier.getInstance(),
                selectionPanel);
//        bayesianPanel = new BayesianPanel(task, standardWidth, selectionPanel);
//        bayesianValidationPanel = new ValidationPanel(
//                task,
//                standardWidth,
//                WekaClassifier.getInstance(),
//                selectionPanel);
//
        // Initial state
        task.setActivity(Task.Activity.INACTIVE);

        // Specific ending if stand alone
        if (!standAlone) {
            frame = defineLayout(new JFrame());
        } else {
            INSTANCE = this;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static Trainer getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new Trainer();
        }

        return INSTANCE;
    }

    //--------//
    // launch //
    //--------//
    /**
     * (Re)activate the trainer tool
     */
    public static void launch ()
    {
        if (standAlone) {
        } else {
            OMR.gui.getApplication().show(getInstance().frame);
        }
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

        // Set UI Look and Feel
        UILookAndFeel.setUI(null);
        Locale.setDefault(Locale.ENGLISH);

        // Off we go...
        Application.launch(Trainer.class, args);
    }

    //------------//
    // initialize //
    //------------//
    @Override
    protected void initialize (String[] args)
    {
        logger.debug("Trainer. 1/initialize");
    }

    //-------//
    // ready //
    //-------//
    @Override
    protected void ready ()
    {
        logger.debug("Trainer. 3/ready");

        frame.addWindowListener(windowCloser);

        //
        //        // Set application exit listener
        //        addExitListener(new GuiExitListener());
        //
        //        // Weakly listen to OmrGui Actions parameters
        //        PropertyChangeListener weak = new WeakPropertyChangeListener(this);
        //        GuiActions.getInstance().addPropertyChangeListener(weak);
        //
        //        // Check MusicFont is loaded
        //        MusicFont.checkMusicFont();
        //
        //        // Just in case we already have messages pending
        //        notifyLog();
        //
        //        // Launch inputs, books & scripts
        //        for (Callable<Void> task : Main.getCli().getCliTasks()) {
        //            OmrExecutors.getCachedLowExecutor().submit(task);
        //        }
    }

    //---------//
    // startup //
    //---------//
    @Override
    protected void startup ()
    {
        logger.debug("Trainer. 2/startup");

        frame = defineLayout(getMainFrame());

        show(frame); // Here we go...
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
    private JFrame defineLayout (final JFrame frame)
    {
        frame.setName("TrainerFrame"); // For SAF life cycle

        /*
         * +=============================================================+
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . Selection . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * |-------------------------------------------------------------|
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . Neural classifier . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . Training. . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . Validation. . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * |-------------------------------------------------------------|
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . Bayesian classifier . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . Training. . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * | . . . . Validation. . . . . . . . . . . . . . . . . . . . . |
         * | . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |
         * +=============================================================+
         */
        final String panelInterline = Panel.getPanelInterline();
        FormLayout layout = new FormLayout(
                "pref",
                "pref," + panelInterline + "," + "pref," + panelInterline + "," + "pref,"
                + panelInterline + "," + "pref," + panelInterline + "," + "pref");

        CellConstraints cst = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout, new Panel());

        ///builder.setDefaultDialogBorder();
        int r = 1; // --------------------------------
        builder.add(selectionPanel.getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(networkPanel.getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(validationPanel.getComponent(), cst.xy(1, r));
//
//        r += 2; // --------------------------------
//        builder.add(bayesianPanel.getComponent(), cst.xy(1, r));
//
//        r += 2; // --------------------------------
//        builder.add(bayesianValidationPanel.getComponent(), cst.xy(1, r));
//
        frame.add(builder.getPanel());

        // Resource injection
        ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resource.injectComponents(frame);

        // Listener on remaining error
        frameTitle = frame.getTitle();
        networkPanel.setErrorListener(
                new ChangeListener()
        {
            @Override
            public void stateChanged (ChangeEvent e)
            {
                frame.setTitle(
                        String.format("%.5f - %s", networkPanel.getBestError(), frameTitle));
            }
        });

        return frame;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------//
    // Task //
    //------//
    /**
     * Class {@code Task} handles which activity is currently being carried
     * out, only one being current at any time.
     */
    static class Task
            extends Observable
    {
        //~ Enumerations ---------------------------------------------------------------------------

        /**
         * Enum {@code Activity} defines the possible activities in
         * training.
         */
        static enum Activity
        {
            //~ Enumeration constant initializers --------------------------------------------------

            /** No ongoing activity */
            INACTIVE,
            /** Selecting
             * glyph to build a population for training */
            SELECTING,
            /** Using the
             * population to train the classifier */
            TRAINING;
        }

        //~ Instance fields ------------------------------------------------------------------------
        /** Current activity */
        private Activity activity = Activity.INACTIVE;

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // getActivity //
        //-------------//
        /**
         * Report the current training activity
         *
         * @return current activity
         */
        public Activity getActivity ()
        {
            return activity;
        }

        //-------------//
        // setActivity //
        //-------------//
        /**
         * Assign a new current activity and notify all observers
         *
         * @param activity
         */
        public void setActivity (Activity activity)
        {
            this.activity = activity;
            setChanged();
            notifyObservers();
        }
    }
}
