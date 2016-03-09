//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S e l e c t i o n P a n e l                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import static omr.classifier.ui.Trainer.Task.Activity.*;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.ui.field.LIntegerField;
import omr.ui.field.LLabel;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Collections;
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
 * Class {@code SelectionPanel} handles a user panel to select samples from
 * repository, either the whole population or a core set of samples.
 * This class is a dedicated companion of {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class SelectionPanel
        implements SampleRepository.Monitor, Observer, ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Reference of network panel companion (TBI) */
    private TrainingPanel trainingPanel;

    /** Swing component */
    private final Panel component;

    /** Current activity */
    private final Trainer.Task task;

    /** Underlying repository of samples */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** For asynchronous execution of the sample selection */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection */
    private final JProgressBar progressBar = new JProgressBar();

    /** To dump the current selection of samples used for training/validation */
    private final DumpAction dumpAction = new DumpAction();

    /** To refresh the application WRT the training material on disk */
    private final RefreshAction refreshAction = new RefreshAction();

    /** To select a core out of whole base */
    private final SelectAction selectAction = new SelectAction();

    /** Counter on loaded samples */
    private int nbLoaded;

    /** Input/output on maximum number of samples with same shape. */
    private final LIntegerField similar = new LIntegerField(
            "Max Similar",
            "Max number of similar shapes");

    /** Displayed counter on existing samples. */
    private final LLabel totalSamples = new LLabel("Total:", "Total number of samples");

    /** Displayed counter on loaded samples. */
    private final LLabel nbLoadedSamples = new LLabel("Loaded:", "Number of samples loaded so far");

    /** Displayed counter on selected samples. */
    private final LLabel nbSelectedSamples = new LLabel(
            "Selected:",
            "Number of selected samples to load");

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SelectionPanel object.
     *
     * @param task          the common training task object
     * @param standardWidth standard width to be used for fields & buttons
     */
    public SelectionPanel (Trainer.Task task,
                           String standardWidth)
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

        defineLayout(standardWidth);

        repository.addListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getBase //
    //---------//
    /**
     * Retrieve the selected collection of samples
     *
     * @param whole indicate whether the whole population is to be selected, or just the core
     * @return the collection of selected samples
     */
    public List<Sample> getBase (boolean whole)
    {
        nbLoaded = 0;
        progressBar.setValue(nbLoaded);

        if (whole) {
            if (!repository.isLoaded()) {
                repository.loadRepository(true);
            }

            List<Sample> samples = repository.getAllSamples();

            if (samples.isEmpty()) {
                repository.loadRepository(false);
                samples = repository.getAllSamples();
                setTotalSamples(samples.size());
            }

            return samples;
        } else {
            ///return repository.getCoreBase(this);
            return defineCore();
        }
    }

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

    //--------------//
    // loadedSample //
    //--------------//
    @Override
    public void loadedSample (Sample sample)
    {
        nbLoadedSamples.setText(Integer.toString(++nbLoaded));
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
        switch (task.getActivity()) {
        case INACTIVE:
            selectAction.setEnabled(true);

            break;

        case SELECTING:
        case TRAINING:
            selectAction.setEnabled(false);

            break;
        }
    }

    //------------------//
    // setTrainingPanel //
    //------------------//
    void setTrainingPanel (TrainingPanel trainingPanel)
    {
        this.trainingPanel = trainingPanel;
    }

    //------------//
    // defineCore //
    //------------//
    private List<Sample> defineCore ()
    {
        //        // What for ? TODO
        //        inputParams();
        //
        //        // Train bayesian on them
        //        WekaClassifier bayesian = WekaClassifier.getInstance();
        //        List<Sample> samples = getBase(true); // use whole
        //
        //        // Quickly train the regression evaluator (on the whole base)
        //        bayesian.train(samples, null, StartingMode.SCRATCH);
        //
        //        // Measure all samples for each shape
        //        Map<Shape, List<GradedSample>> palmares = new HashMap<Shape, List<GradedSample>>();
        //
        //        for (Sample sample : samples) {
        //            if (sample != null) {
        //                try {
        //                    Shape shape = sample.getShape();
        //                    double grade = bayesian.measureDistance(
        //                            sample,
        //                            sample.getInterline(),
        //                            shape);
        //                    List<GradedSample> shapeGradedSamples = palmares.get(shape);
        //
        //                    if (shapeGradedSamples == null) {
        //                        palmares.put(shape, shapeGradedSamples = new ArrayList<GradedSample>());
        //                    }
        //
        //                    shapeGradedSamples.add(new GradedSample(sample, grade));
        //                } catch (Exception ex) {
        //                    logger.warn("Cannot evaluate {}", sample);
        //                }
        //            }
        //        }
        //
        //        // Set of chosen shapes
        //        final Set<GradedSample> set = new HashSet<GradedSample>();
        //        final int maxSimilar = similar.getValue();
        //
        //        // Sort the palmares, shape by shape, by (decreasing) grade
        //        for (List<GradedSample> shapeGradedSamples : palmares.values()) {
        //            Collections.sort(shapeGradedSamples, GradedSample.reverseGradeComparator);
        //
        //            // Take a sample equally distributed on instances of this shape
        //            final int size = shapeGradedSamples.size();
        //            final float delta = ((float) (size - 1)) / (maxSimilar - 1);
        //
        //            for (int i = 0; i < maxSimilar; i++) {
        //                int idx = Math.min(size - 1, Math.round(i * delta));
        //                GradedSample ng = shapeGradedSamples.get(idx);
        //
        //                if (ng.sample.getShape().isTrainable()) {
        //                    set.add(ng);
        //                }
        //            }
        //        }
        //
        //        // Build the core base
        //        List<Sample> base = new ArrayList<Sample>(set.size());
        //
        //        for (GradedSample gs : set) {
        //            base.add(gs.sample);
        //        }
        //
        //        ///repository.setCoreBase(base);
        //        setSelectedSamples(base.size());
        //
        //        return base;
        return Collections.EMPTY_LIST;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String standardWidth)
    {
        progressBar.setForeground(Color.ORANGE);

        FormLayout layout = Panel.makeFormLayout(3, 4, "", standardWidth, standardWidth);
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();

        ///builder.setDefaultDialogBorder();
        int r = 1; // ----------------------------
        builder.addSeparator("Selection", cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));

        r += 2; // ----------------------------
        builder.add(new JButton(dumpAction), cst.xy(3, r));
        builder.add(new JButton(refreshAction), cst.xy(5, r));

        builder.add(similar.getLabel(), cst.xy(9, r));
        builder.add(similar.getField(), cst.xy(11, r));

        builder.add(totalSamples.getLabel(), cst.xy(13, r));
        builder.add(totalSamples.getField(), cst.xy(15, r));

        r += 2; // ----------------------------
        builder.add(new JButton(selectAction), cst.xy(3, r));
        builder.add(nbSelectedSamples.getLabel(), cst.xy(9, r));
        builder.add(nbSelectedSamples.getField(), cst.xy(11, r));

        builder.add(nbLoadedSamples.getLabel(), cst.xy(13, r));
        builder.add(nbLoadedSamples.getField(), cst.xy(15, r));
    }

    //---------------//
    // displayParams //
    //---------------//
    private void displayParams ()
    {
        similar.setValue(constants.maxSimilar.getValue());
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        constants.maxSimilar.setValue(similar.getValue());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer maxSimilar = new Constant.Integer(
                "Glyphs",
                10,
                "Absolute maximum number of instances for the same shape" + " used in training");
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
            //            List<String> gNames = getBase(trainingPanel.useWhole());
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
            super("Select Core");
            putValue(Action.SHORT_DESCRIPTION, "Build core selection out of whole sample base");
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
                    task.setActivity(SELECTING);

                    // Define Core from Whole
                    defineCore();
                    ///repository.storeCoreBase();
                    task.setActivity(INACTIVE);
                }
            });
        }
    }
}
