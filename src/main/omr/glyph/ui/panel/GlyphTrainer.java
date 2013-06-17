//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h T r a i n e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui.panel;

import omr.constant.ConstantManager;

import omr.glyph.GlyphNetwork;

import omr.ui.MainGui;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code GlyphTrainer} handles a User Interface dedicated to the
 * training and testing of a glyph evaluator.
 * This class can be launched as a stand-alone program.
 *
 * <p>The frame is divided vertically in 4 parts:
 * <ol>
 * <li>The selection in repository of known glyphs ({@link SelectionPanel})
 * <li>The training of the neural network evaluator ({@link TrainingPanel})
 * <li>The validation of the neural network evaluator ({@link ValidationPanel})
 * <li>The training of the linear evaluator ({@link RegressionPanel})
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class GlyphTrainer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GlyphTrainer.class);

    /** The single instance of this class */
    private static volatile GlyphTrainer INSTANCE;

    /** To differentiate the exit action, according to the launch context */
    private static boolean standAlone = false;

    /** Standard width for fields/buttons in DLUs */
    private static final String standardWidth = "50dlu";

    /** An adapter trigerred on window closing */
    private static final WindowAdapter windowCloser = new WindowAdapter()
    {
        @Override
        public void windowClosing (WindowEvent e)
        {
            // Store latest constant values
            ConstantManager.getInstance()
                    .storeResource();

            // That's all folks !
            System.exit(0);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Related frame */
    private final JFrame frame;

    /** Panel for selection in repository */
    private final SelectionPanel selectionPanel;

    /** Panel for Neural network training */
    private final NetworkPanel networkPanel;

    /** Panel for Neural network validation */
    private final ValidationPanel validationPanel;

    /** Panel for Regression training */
    private final TrainingPanel regressionPanel;

    /** Current task */
    private final Task task = new Task();

    /** Frame title */
    private String frameTitle;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // GlyphTrainer //
    //--------------//
    /**
     * Create an instance of Glyph Trainer (there should be just one)
     */
    private GlyphTrainer ()
    {
        if (standAlone) {
            // UI Look and Feel
            UILookAndFeel.setUI(null);
        }

        frame = new JFrame();
        frame.setName("trainerFrame");

        // Listener on remaining error
        ChangeListener errorListener = new ChangeListener()
        {
            @Override
            public void stateChanged (ChangeEvent e)
            {
                frame.setTitle(
                        String.format(
                        "%.5f - %s",
                        networkPanel.getBestError(),
                        frameTitle));
            }
        };

        // Create the three companions
        selectionPanel = new SelectionPanel(task, standardWidth);
        networkPanel = new NetworkPanel(
                task,
                standardWidth,
                errorListener,
                selectionPanel);
        selectionPanel.setTrainingPanel(networkPanel);
        validationPanel = new ValidationPanel(
                task,
                standardWidth,
                GlyphNetwork.getInstance(),
                selectionPanel,
                networkPanel);
        regressionPanel = new RegressionPanel(
                task,
                standardWidth,
                selectionPanel);
        frame.add(createGlobalPanel());

        // Initial state
        task.setActivity(Task.Activity.INACTIVE);

        // Specific ending if stand alone
        if (standAlone) {
            frame.addWindowListener(windowCloser);
        }

        // Resource injection
        ResourceMap resource = MainGui.getInstance()
                .getContext()
                .getResourceMap(getClass());
        resource.injectComponents(frame);
        frameTitle = frame.getTitle();
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // launch //
    //--------//
    /**
     * (Re)activate the trainer tool
     */
    public static void launch ()
    {
        MainGui.getInstance()
                .show(getInstance().frame);
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
        getInstance();
    }

    //-------------//
    // getInstance //
    //-------------//
    private static GlyphTrainer getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphTrainer();
        }

        return INSTANCE;
    }

    //-------------------//
    // createGlobalPanel //
    //-------------------//
    private JPanel createGlobalPanel ()
    {
        final String panelInterline = Panel.getPanelInterline();
        FormLayout layout = new FormLayout(
                "pref",
                "pref," + panelInterline + "," + "pref," + panelInterline + ","
                + "pref," + panelInterline + "," + "pref");

        CellConstraints cst = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout, new Panel());
        builder.setDefaultDialogBorder();

        int r = 1; // --------------------------------
        builder.add(selectionPanel.getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(networkPanel.getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(validationPanel.getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(regressionPanel.getComponent(), cst.xy(1, r));

        return builder.getPanel();
    }

    //~ Inner Classes ----------------------------------------------------------
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
        //~ Enumerations -------------------------------------------------------

        /**
         * Enum {@code Activity} defines the possible activities in
         * training.
         */
        static enum Activity
        {
            //~ Enumeration constant initializers ------------------------------

            /** No ongoing activity */
            INACTIVE,
            /** Selecting
             * glyph to build a population for training */
            SELECTING,
            /** Using the
             * population to train the evaluator */
            TRAINING;

        }

        //~ Instance fields ----------------------------------------------------
        /** Current activity */
        private Activity activity = Activity.INACTIVE;

        //~ Methods ------------------------------------------------------------
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
