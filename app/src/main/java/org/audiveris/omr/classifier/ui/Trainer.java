//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T r a i n e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Class <code>Trainer</code> handles a User Interface dedicated to the
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
    //~ Static fields/initializers -----------------------------------------------------------------

    static {
        // We need class WellKnowns to be elaborated before anything else (when in standalone mode)
        WellKnowns.ensureLoaded();
    }

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

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Related frame.
     * We need a frame rather than a dialog because this class can be run in standalone.
     */
    private JFrame frame;

    /** Panel for selection in repository. */
    private final SelectionPanel selectionPanel;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an instance of Glyph Trainer. (there should be just one)
     */
    private Trainer ()
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

    //~ Methods ------------------------------------------------------------------------------------

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

        final FormLayout layout = new FormLayout("pref, 10dlu, pref", "pref, 10dlu, pref");
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(new Panel());

        int r = 1; // --------------------------------
        builder.addRaw(selectionPanel.getComponent()).xyw(1, r, 3, "center, fill");

        r += 2; // --------------------------------
        builder.addRaw(definePanel(ShapeClassifier.getInstance())).xy(1, r);
        //        builder.addRaw(definePanel(ShapeClassifier.getSecondInstance())).xy(3, r));
        //
        frame.add(builder.getPanel());

        // Resource injection
        ResourceMap resources = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resources.injectComponents(frame);
        frame.setIconImage(OmrGui.getApplication().getMainFrame().getIconImage());

        return frame;
    }

    //-------------//
    // definePanel //
    //-------------//
    private JPanel definePanel (Classifier classifier)
    {
        final String pi = Panel.getPanelInterline();
        final FormLayout layout = new FormLayout("pref", "pref," + pi + ",pref," + pi + ",pref");
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(
                new TitledPanel(classifier.getName()));
        final Task task = new Task(classifier);

        int r = 1; // --------------------------------
        builder.addRaw(new TrainingPanel(task, selectionPanel).getComponent()).xy(1, r);

        r += 2; // --------------------------------
        builder.addRaw(new ValidationPanel(task, selectionPanel, true).getComponent()).xy(1, r);

        r += 2; // --------------------------------
        builder.addRaw(new ValidationPanel(task, selectionPanel, false).getComponent()).xy(1, r);

        return builder.getPanel();
    }

    //--------------//
    // displayFrame //
    //--------------//
    void displayFrame ()
    {
        frame.toFront();
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

    //~ Static Methods -----------------------------------------------------------------------------

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
     * (Re)activate the trainer tool.
     */
    public static void launch ()
    {
        if (!standAlone) {
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //------//
    // Task //
    //------//
    public static class Task
    {
        /** Change listeners. */
        private final PropertyChangeSupport listeners;

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
            listeners = new PropertyChangeSupport(this);
        }

        public void addPropertyChangeListener (PropertyChangeListener pcl)
        {
            listeners.addPropertyChangeListener(pcl);
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

        public void removePropertyChangeListener (PropertyChangeListener pcl)
        {
            listeners.removePropertyChangeListener(pcl);
        }

        /**
         * Assign a new current activity and notify all listeners.
         *
         * @param activity the activity to be assigned
         */
        public void setActivity (Activity activity)
        {
            final Activity old = this.activity;
            this.activity = activity;
            listeners.firePropertyChange("activity", old, activity);
        }

        /**
         * Enum <code>Activity</code> defines all activities in training.
         */
        public static enum Activity
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
