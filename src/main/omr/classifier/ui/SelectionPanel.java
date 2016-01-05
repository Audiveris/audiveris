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

import omr.classifier.AbstractClassifier.StartingMode;
import omr.classifier.LinearClassifier;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import static omr.classifier.ui.Trainer.Task.Activity.*;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;

/**
 * Class {@code SelectionPanel} handles a user panel to select <B>names</B> from sample
 * repository, either the whole population or a core set of samples.
 * This class is a dedicated companion of {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class SelectionPanel
        implements SampleRepository.Monitor, Observer
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
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection */
    private JProgressBar progressBar = new JProgressBar();

    /** To dump the current selection of samples used for training/validation */
    private DumpAction dumpAction = new DumpAction();

    /** To refresh the application WRT the training material on disk */
    private RefreshAction refreshAction = new RefreshAction();

    /** To select a core out of whole base */
    private SelectAction selectAction = new SelectAction();

    /** Counter on loaded samples */
    private int nbLoaded;

    /** Input/output on maximum number of samples with same shape */
    private LIntegerField similar = new LIntegerField(
            "Max Similar",
            "Max number of similar shapes");

    /** Displayed counter on existing sample files */
    private LIntegerField totalFiles = new LIntegerField(
            false,
            "Total",
            "Total number of sample files");

    /** Displayed counter on loaded samples */
    private LIntegerField nbLoadedFiles = new LIntegerField(
            false,
            "Loaded",
            "Number of sample files loaded so far");

    /** Displayed counter on selected samples */
    private LIntegerField nbSelectedFiles = new LIntegerField(
            false,
            "Selected",
            "Number of selected sample files to load");

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
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getBase //
    //---------//
    /**
     * Retrieve the selected collection of sample names
     *
     * @param whole indicate whether the whole population is to be selected, or
     *              just the core
     * @return the collection of selected samples names
     */
    public List<String> getBase (boolean whole)
    {
        nbLoaded = 0;
        progressBar.setValue(nbLoaded);

        if (whole) {
            return repository.getWholeBase(this);
        } else {
            return repository.getCoreBase(this);
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

    //-------------//
    // loadedGlyph //
    //-------------//
    /**
     * Call-back when a sample has just been loaded
     *
     * @param gName the normalized sample name
     */
    @Override
    public void loadedGlyph (String gName)
    {
        nbLoadedFiles.setValue(++nbLoaded);
        progressBar.setValue(nbLoaded);
    }

    //-------------------//
    // setSelectedGlyphs //
    //-------------------//
    /**
     * Notify the number of samples selected
     *
     * @param selected number of selected samples
     */
    @Override
    public void setSelectedGlyphs (int selected)
    {
        nbSelectedFiles.setValue(selected);
    }

    //----------------//
    // setTotalGlyphs //
    //----------------//
    /**
     * Notify the total number of samples in the base
     *
     * @param total the total number of samples available
     */
    @Override
    public void setTotalGlyphs (int total)
    {
        totalFiles.setValue(total);
        progressBar.setMaximum(total);
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
    private void defineCore ()
    {
        // What for ? TODO
        inputParams();

        // Train regression on them
        LinearClassifier regression = LinearClassifier.getInstance();
        Collection<String> gNames = getBase(true); // use whole
        List<Sample> samples = new ArrayList<Sample>();

        // Actually load each sample description, if not yet done
        for (String gName : gNames) {
            Sample sample = repository.getSample(gName, this);

            if (sample != null) {
                samples.add(sample);
            }
        }

        // Quickly train the regression evaluator (on the whole base)
        regression.train(samples, null, StartingMode.SCRATCH);

        // Measure all samples of each shape
        Map<Shape, List<NotedSample>> palmares = new HashMap<Shape, List<NotedSample>>();

        for (String gName : gNames) {
            Sample sample = repository.getSample(gName, this);

            if (sample != null) {
                try {
                    Shape shape = sample.getShape();
                    double grade = regression.measureDistance(
                            sample,
                            sample.getInterline(),
                            shape);
                    List<NotedSample> shapeNotes = palmares.get(shape);

                    if (shapeNotes == null) {
                        shapeNotes = new ArrayList<NotedSample>();
                        palmares.put(shape, shapeNotes);
                    }

                    shapeNotes.add(new NotedSample(gName, sample, grade));
                } catch (Exception ex) {
                    logger.warn("Cannot evaluate {}", sample);
                }
            }
        }

        // Set of chosen shapes
        final Set<NotedSample> set = new HashSet<NotedSample>();
        final int maxSimilar = similar.getValue();

        // Sort the palmares, shape by shape, by (decreasing) grade
        for (List<NotedSample> shapeNotes : palmares.values()) {
            Collections.sort(shapeNotes, NotedSample.reverseGradeComparator);

            // Take a sample equally distributed on instances of this shape
            final int size = shapeNotes.size();
            final float delta = ((float) (size - 1)) / (maxSimilar - 1);

            for (int i = 0; i < maxSimilar; i++) {
                int idx = Math.min(size - 1, Math.round(i * delta));
                NotedSample ng = shapeNotes.get(idx);

                if (ng.sample.getShape().isTrainable()) {
                    set.add(ng);
                }
            }
        }

        // Build the core base
        List<String> base = new ArrayList<String>(set.size());

        for (NotedSample ng : set) {
            base.add(ng.gName);
        }

        repository.setCoreBase(base);
        setSelectedGlyphs(base.size());
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String standardWidth)
    {
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

        builder.add(totalFiles.getLabel(), cst.xy(13, r));
        builder.add(totalFiles.getField(), cst.xy(15, r));

        r += 2; // ----------------------------
        builder.add(new JButton(selectAction), cst.xy(3, r));
        builder.add(nbSelectedFiles.getLabel(), cst.xy(9, r));
        builder.add(nbSelectedFiles.getField(), cst.xy(11, r));

        builder.add(nbLoadedFiles.getLabel(), cst.xy(13, r));
        builder.add(nbLoadedFiles.getField(), cst.xy(15, r));
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

    //-------------//
    // NotedSample //
    //-------------//
    /**
     * Handle a sample together with its name and grade
     */
    private static class NotedSample
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** For comparing NotedSample instance in reverse grade order */
        static final Comparator<NotedSample> reverseGradeComparator = new Comparator<NotedSample>()
        {
            @Override
            public int compare (NotedSample ng1,
                                NotedSample ng2)
            {
                return Double.compare(ng2.grade, ng1.grade);
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        final String gName;

        final Sample sample;

        final double grade;

        //~ Constructors ---------------------------------------------------------------------------
        public NotedSample (String gName,
                            Sample sample,
                            double grade)
        {
            this.gName = gName;
            this.sample = sample;
            this.grade = grade;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{NotedSample " + gName + " " + grade + "}";
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
            List<String> gNames = getBase(trainingPanel.useWhole());
            System.out.println(
                    "Content of " + (trainingPanel.useWhole() ? "whole" : "core") + " population ("
                    + gNames.size() + "):");
            Collections.sort(gNames, SampleRepository.shapeComparator);

            int sampleNb = 0;
            String prevName = null;

            for (String gName : gNames) {
                if (prevName != null) {
                    if (!SampleRepository.shapeNameOf(gName).equals(prevName)) {
                        System.out.println(String.format("%4d %s", sampleNb, prevName));
                        sampleNb = 1;
                    }
                }

                sampleNb++;
                prevName = SampleRepository.shapeNameOf(gName);
            }

            System.out.println(String.format("%4d %s", sampleNb, prevName));
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        // Purpose is just to read and remember the data from the various
        // input fields. Triggered when user presses Enter in one of these
        // fields.
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
            repository.refreshBases();
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
                            repository.storeCoreBase();

                            task.setActivity(INACTIVE);
                        }
                    });
        }
    }
}
