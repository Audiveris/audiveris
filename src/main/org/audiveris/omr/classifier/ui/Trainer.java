//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T r a i n e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.ConstantManager;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UILookAndFeel;
import org.audiveris.omr.ui.util.UIUtil;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Class {@code Trainer} handles a User Interface dedicated to the
 * training and testing of a glyph classifier.
 * <p>
 * The frame is divided vertically in several parts:
 * <ul>
 * <li>The selection of samples ({@link SelectionPanel})
 * <li>For each classifier:
 * <ul>
 * <li>Training ({@link TrainingPanel})
 * <li>Validation on train set ({@link ValidationPanel})
 * <li>Validation on test set ({@link ValidationPanel})
 * </ul>
 * </ul>
 * This class can be launched as a stand-alone program.
 *
 * @author Hervé Bitteur
 */
public class Trainer
        extends SingleFrameApplication
{

    // Don't move this statement
    // @formatter:off
    static {
        // We need class WellKnowns to be elaborated before anything else (when in standalone mode)
        WellKnowns.ensureLoaded();
    }
    // @formatter:on

    private static final Logger logger = LoggerFactory.getLogger(Trainer.class);

    /** The single instance of this class. */
    private static volatile Trainer INSTANCE;

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    /** An adapter triggered on window closing. */
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

    /** Standard width for labels in DLUs. */
    static final String LABEL_WIDTH = "50dlu";

    /** Standard width for fields/buttons in DLUs. */
    static final String FIELD_WIDTH = "30dlu";

    /** Related frame. */
    private JFrame frame;

    /** Panel for selection in repository. */
    private final SelectionPanel selectionPanel;

    /**
     * Create an instance of Glyph Trainer (there should be just one)
     */
    public Trainer ()
    {
        // Create the companions
        selectionPanel = new SelectionPanel();

        // Specific ending if stand alone
        if (!standAlone) {
            frame = defineLayout(new JFrame());
        } else {
            INSTANCE = this;
        }
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
     */
    private JFrame defineLayout (final JFrame frame)
    {
        frame.setName("TrainerFrame"); // For SAF life cycle

        // +=============================+
        // | . . . . . . . . . . . . . . |
        // | . Selection . . . . . . . . |
        // | . . . . . . . . . . . . . . |
        // |-----------------------------|
        // | . . . . . . . . . . . . . . |
        // | . Training. . . . . . . . . |
        // | . . . . . . . . . . . . . . |
        // |-----------------------------|
        // | . . . . . . . . . . . . . . |
        // | . Validation [train set]. . |
        // | . . . . . . . . . . . . . . |
        // |-----------------------------|
        // | . . . . . . . . . . . . . . |
        // | . Validation [test set] . . |
        // | . . . . . . . . . . . . . . |
        // +=============================+
        //
        FormLayout layout = new FormLayout("pref, 10dlu, pref", "pref, 10dlu, pref");
        CellConstraints cst = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout, new Panel());

        int r = 1; // --------------------------------
        builder.add(selectionPanel.getComponent(), cst.xyw(1, r, 3, "center, fill"));

        r += 2; // --------------------------------
        builder.add(definePanel(ShapeClassifier.getInstance()), cst.xy(1, r));
        //        builder.add(definePanel(ShapeClassifier.getSecondInstance()), cst.xy(3, r));
        //
        frame.add(builder.getPanel());

        // Resource injection
        ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resource.injectComponents(frame);

        return frame;
    }

    private JPanel definePanel (Classifier classifier)
    {
        final String pi = Panel.getPanelInterline();
        FormLayout layout = new FormLayout("pref", "pref," + pi + ",pref," + pi + ",pref");

        CellConstraints cst = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout, new TitledPanel(classifier.getName()));
        Task task = new Task(classifier);

        int r = 1; // --------------------------------
        builder.add(new TrainingPanel(task, selectionPanel).getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(new ValidationPanel(task, selectionPanel, true).getComponent(), cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(new ValidationPanel(task, selectionPanel, false).getComponent(), cst.xy(1, r));

        return builder.getPanel();
    }

    //--------------//
    // displayFrame //
    //--------------//
    void displayFrame ()
    {
        frame.toFront();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton.
     *
     * @return the single Trainer instance
     */
    public static synchronized Trainer getInstance ()
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
            final JFrame frame = getInstance().frame;
            OmrGui.getApplication().show(frame);
            UIUtil.unMinimize(frame);
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

    public static class Task
            extends Observable
    {

        /** Managed classifier. */
        public final Classifier classifier;

        /** Current activity. */
        private Activity activity = Activity.INACTIVE;

        /**
         * The shape classifier to use
         *
         * @param classifier selected classifier
         */
        public Task (Classifier classifier)
        {
            this.classifier = classifier;
        }

        /**
         * Report the current training activity
         *
         * @return current activity
         */
        public Activity getActivity ()
        {
            return activity;
        }

        /**
         * Assign a new current activity and notify all observers
         *
         * @param activity the activity to be assigned
         */
        public void setActivity (Activity activity)
        {
            this.activity = activity;
            setChanged();
            notifyObservers();
        }

        /**
         * Enum {@code Activity} defines all activities in training.
         */
        static enum Activity
        {
            /** No ongoing activity */
            INACTIVE,
            /** Training on samples */
            TRAINING,
            /** Validating classifier */
            VALIDATION;
        }
    }

    //-------------//
    // TitledPanel //
    //-------------//
    private static class TitledPanel
            extends Panel
    {

        TitledPanel (String title)
        {
            setBorder(
                    BorderFactory.createTitledBorder(
                            new EtchedBorder(),
                            title,
                            TitledBorder.CENTER,
                            TitledBorder.TOP));
            setInsets(30, 10, 10, 10); // TLBR
        }
    }
}
