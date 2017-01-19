//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S e l e c t i o n P a n e l                                   //
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleSource;
import static org.audiveris.omr.classifier.ui.Trainer.Task.Activity.*;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SelectionPanel} handles a user panel to select samples from repository.
 * This class is a dedicated companion of {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class SelectionPanel
        implements SampleSource, SampleRepository.Monitor, Observer, ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Swing component. */
    private final Panel component;

    /** Current activity. */
    private final Trainer.Task task;

    /** Underlying repository of samples. */
    private final SampleRepository repository = SampleRepository.getGlobalInstance(true);

    /** For asynchronous execution of the sample selection. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection. */
    private final JProgressBar progressBar = new JProgressBar();

    /** To dump the current selection of samples used for training/validation */
    private final DumpAction dumpAction = new DumpAction();

    /** To refresh the application WRT the training material. */
    private final RefreshAction refreshAction = new RefreshAction();

    /** To select repository samples. */
    private final SelectAction selectAction = new SelectAction();

    /** Counter on loaded samples */
    private int nbLoaded;

    /** Displayed counter on existing samples. */
    private final LLabel totalSamples = new LLabel("Total:", "Total number of samples");

    /** Displayed counter on selected samples. */
    private final LLabel nbSelectedSamples = new LLabel(
            "Selected:",
            "Number of selected samples to load");

    /** Sample collection for training. */
    private List<Sample> trains;

    /** Sample collection for testing. */
    private List<Sample> tests;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SelectionPanel object.
     *
     * @param task the common training task object
     */
    public SelectionPanel (Trainer.Task task)
    {
        this.task = task;
        task.addObserver(this);

        component = new Panel();
        component.setNoInsets();

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "readParams");
        component.getActionMap().put("readParams", new ParamAction());

        displayParams();

        defineLayout();

        repository.addListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the encapsulated swinb component
     *
     * @return the user panel
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //----------------//
    // getTestSamples //
    //----------------//
    /**
     * Retrieve the samples selected for validation.
     *
     * @return the collection of selected samples
     */
    @Override
    public List<Sample> getTestSamples ()
    {
        if (tests == null) {
            getTrainSamples();
        }

        return tests;
    }

    //-----------------//
    // getTrainSamples //
    //-----------------//
    /**
     * Retrieve the samples selected for training.
     *
     * @return the collection of selected samples
     */
    @Override
    public List<Sample> getTrainSamples ()
    {
        if (trains == null) {
            trains = new ArrayList<Sample>();
            tests = new ArrayList<Sample>();

            if (!repository.isLoaded()) {
                progressBar.setValue(0);
                repository.loadRepository();
            }

            final int minCount = constants.minShapeSampleCount.getValue();
            final int maxCount = constants.maxShapeSampleCount.getValue();
            repository.splitTrainAndTest(trains, tests, minCount, maxCount);
            nbLoaded = trains.size();
            ///setTotalSamples(nbLoaded);
            setSelectedSamples(trains.size());
            progressBar.setValue(nbLoaded);
        }

        return trains;
    }

    //------------------------//
    // getMinShapeSampleCount //
    //------------------------//
    public static int getMinShapeSampleCount ()
    {
        return constants.minShapeSampleCount.getValue();
    }

    //--------------//
    // loadedSample //
    //--------------//
    @Override
    public void loadedSample (Sample sample)
    {
        progressBar.setValue(nbLoaded);
    }

    //--------------------//
    // setSelectedSamples //
    //--------------------//
    @Override
    public void setSelectedSamples (int selected)
    {
        nbSelectedSamples.setText(Integer.toString(selected));
    }

    //-----------------//
    // setTotalSamples //
    //-----------------//
    /**
     * Notify the total number of samples in the base
     *
     * @param total the total number of samples available
     */
    @Override
    public void setTotalSamples (int total)
    {
        totalSamples.setText(Integer.toString(total));
        progressBar.setMaximum(total);
    }

    //--------------//
    // stateChanged //
    //--------------//
    @Override
    public void stateChanged (ChangeEvent e)
    {
        // Called from repository
        setTotalSamples(repository.getAllSamples().size());
    }

    //--------//
    // update //
    //--------//
    /**
     * Method triggered whenever the activity changes
     *
     * @param obs    the new current task activity
     * @param unused not used
     */
    @Override
    public void update (Observable obs,
                        Object unused)
    {
        selectAction.setEnabled(task.getActivity() == INACTIVE);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        progressBar.setForeground(Colors.PROGRESS_BAR);

        FormLayout layout = Panel.makeFormLayout(
                3,
                3,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();

        ///builder.setDefaultDialogBorder();
        int r = 1; // ----------------------------
        builder.addSeparator("Selection", cst.xyw(1, r, 3));
        builder.add(progressBar, cst.xyw(5, r, 7));

        r += 2; // ----------------------------
        //        builder.add(new JButton(dumpAction), cst.xy(3, r));
        //        builder.add(new JButton(refreshAction), cst.xy(5, r));
        //

        builder.add(totalSamples.getLabel(), cst.xy(9, r));
        builder.add(totalSamples.getField(), cst.xy(11, r));

        r += 2; // ----------------------------
        builder.add(new JButton(selectAction), cst.xy(3, r));
        builder.add(nbSelectedSamples.getLabel(), cst.xy(9, r));
        builder.add(nbSelectedSamples.getField(), cst.xy(11, r));
    }

    //---------------//
    // displayParams //
    //---------------//
    private void displayParams ()
    {
        ///similar.setValue(constants.maxSimilar.getValue());
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        ///constants.maxSimilar.setValue(similar.getValue());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer maxShapeSampleCount = new Constant.Integer(
                "samples",
                100,
                "Maximum sample count per shape for training");

        private final Constant.Integer minShapeSampleCount = new Constant.Integer(
                "samples",
                10,
                "Minimum sample count per shape for training");
    }

    //--------------//
    // GradedSample //
    //--------------//
    /**
     * Handle a sample together with its grade.
     */
    private static class GradedSample
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** For comparing GradedSample instances in reverse grade order. */
        static final Comparator<GradedSample> reverseGradeComparator = new Comparator<GradedSample>()
        {
            @Override
            public int compare (GradedSample gs1,
                                GradedSample gs2)
            {
                return Double.compare(gs2.grade, gs1.grade);
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        final Sample sample;

        final double grade;

        //~ Constructors ---------------------------------------------------------------------------
        public GradedSample (Sample sample,
                             double grade)
        {
            this.sample = sample;
            this.grade = grade;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "GradedSample{" + sample + " " + grade + "}";
        }
    }

    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public DumpAction ()
        {
            super("Dump");
            putValue(Action.SHORT_DESCRIPTION, "Dump the current sample selection");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.warn("Not implemented");

            //            List<String> gNames = getTrainSamples(trainingPanel.useWhole());
            //            System.out.println(
            //                    "Content of " + (trainingPanel.useWhole() ? "whole" : "core") + " population ("
            //                    + gNames.size() + "):");
            //            Collections.sort(gNames, SampleRepository.shapeComparator);
            //
            //            int sampleNb = 0;
            //            String prevName = null;
            //
            //            for (String gName : gNames) {
            //                if (prevName != null) {
            //                    if (!SampleRepository.shapeNameOf(gName).equals(prevName)) {
            //                        System.out.println(String.format("%4d %s", sampleNb, prevName));
            //                        sampleNb = 1;
            //                    }
            //                }
            //
            //                sampleNb++;
            //                prevName = SampleRepository.shapeNameOf(gName);
            //            }
            //
            //            System.out.println(String.format("%4d %s", sampleNb, prevName));
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        // Purpose is just to read and remember the data from the various input fields.
        // Triggered when user presses Enter in one of these fields.
        @Override
        public void actionPerformed (ActionEvent e)
        {
            inputParams();
            displayParams();
        }
    }

    //---------------//
    // RefreshAction //
    //---------------//
    private class RefreshAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public RefreshAction ()
        {
            super("Disk Refresh");
            putValue(Action.SHORT_DESCRIPTION, "Refresh trainer with disk information");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            //            repository.refreshBases();
        }
    }

    //--------------//
    // SelectAction //
    //--------------//
    private class SelectAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SelectAction ()
        {
            super("Select");
            putValue(Action.SHORT_DESCRIPTION, "Build samples selection");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    task.setActivity(SELECTION);

                    getTrainSamples();

                    ///repository.storeCoreBase();
                    task.setActivity(INACTIVE);
                }
            });
        }
    }
}
