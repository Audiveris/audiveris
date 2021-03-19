//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S e l e c t i o n P a n e l                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.classifier.GlyphDescriptor;
import org.audiveris.omr.classifier.ImgGlyphDescriptor;
import org.audiveris.omr.classifier.MixGlyphDescriptor;
import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleSheet;
import org.audiveris.omr.classifier.SampleSource;
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
 * <p>
 * This class is a dedicated companion of {@link Trainer}.
 * It handles the current selection among samples (both the training set and the test set).
 *
 * @author Hervé Bitteur
 */
class SelectionPanel
        implements SampleSource, SampleRepository.LoadListener, ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Swing component. */
    private final Panel component;

    /** Underlying repository of samples. */
    private final SampleRepository repository = SampleRepository.getGlobalInstance(false);

    /** For asynchronous execution of the sample selection. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection. */
    private final JProgressBar progressBar = new JProgressBar();

    /** To store the current train/test selections of samples as .csv files. */
    private final StoreAction storeAction = new StoreAction();

    /** To select repository samples. */
    private final SelectAction selectAction = new SelectAction();

    /** Displayed counter on existing samples within repository. */
    private final LLabel nbRepoSamples = new LLabel("Total:", "Number of samples in repository");

    /** Displayed counter on train samples. */
    private final LLabel nbTrainSamples = new LLabel("Train set:", "Number of train samples");

    /** Displayed counter on test samples. */
    private final LLabel nbTestSamples = new LLabel("Test set:", "Number of test samples");

    /** Sample collection for training. */
    private List<Sample> trains;

    /** Sample collection for testing. */
    private List<Sample> tests;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SelectionPanel object.
     */
    SelectionPanel ()
    {
        component = new Panel();
        component.setNoInsets();

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "readParams");
        component.getActionMap().put("readParams", new ParamAction());

        displayParams();

        defineLayout();

        repository.addListener(this);

        if (repository.isLoaded()) {
            setTotalSamples(repository.getAllSamples().size());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the encapsulated swing component
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
    public synchronized List<Sample> getTestSamples ()
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
    public synchronized List<Sample> getTrainSamples ()
    {
        if (trains == null) {
            progressBar.setValue(0);
            trains = new ArrayList<>();
            tests = new ArrayList<>();

            if (!repository.isLoaded()) {
                repository.loadRepository(this);
            }

            setTotalSamples(repository.getAllSamples().size());

            final int minCount = constants.minShapeSampleCount.getValue();
            final int maxCount = constants.maxShapeSampleCount.getValue();
            repository.splitTrainAndTest(trains, tests, minCount, maxCount);
            nbTrainSamples.setText(Integer.toString(trains.size()));
            nbTestSamples.setText(Integer.toString(tests.size()));
            progressBar.setValue(progressBar.getMaximum());
        }

        return trains;
    }

    //-------------//
    // loadedSheet //
    //-------------//
    @Override
    public void loadedSheet (SampleSheet sampleSheet)
    {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    //--------------//
    // stateChanged //
    //--------------//
    @Override
    public void stateChanged (ChangeEvent e)
    {
        // Called from repository (?)
        setTotalSamples(repository.getAllSamples().size()); // What for?
    }

    //-------------//
    // totalSheets //
    //-------------//
    @Override
    public void totalSheets (int total)
    {
        progressBar.setMaximum(total);
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

        int r = 1; // ----------------------------

        builder.addSeparator("Selection", cst.xyw(1, r, 3));

        builder.add(progressBar, cst.xyw(5, r, 7));

        r += 2; // ----------------------------

        builder.add(nbRepoSamples.getLabel(), cst.xy(5, r));
        builder.add(nbRepoSamples.getField(), cst.xy(7, r));

        builder.add(nbTrainSamples.getLabel(), cst.xy(9, r));
        builder.add(nbTrainSamples.getField(), cst.xy(11, r));

        r += 2; // ----------------------------

        builder.add(new JButton(selectAction), cst.xy(3, r));

        builder.add(new JButton(storeAction), cst.xy(5, r));

        builder.add(nbTestSamples.getLabel(), cst.xy(9, r));
        builder.add(nbTestSamples.getField(), cst.xy(11, r));
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
        ///constants.maxSimilar.setStringValue(similar.getValue());
    }

    //-----------------//
    // setTotalSamples //
    //-----------------//
    /**
     * Notify the total number of samples in the base
     *
     * @param total the total number of samples available
     */
    private void setTotalSamples (int total)
    {
        nbRepoSamples.setText(Integer.toString(total));
    }

    //------------------------//
    // getMinShapeSampleCount //
    //------------------------//
    public static int getMinShapeSampleCount ()
    {
        return constants.minShapeSampleCount.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

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

        /** For comparing GradedSample instances in reverse grade order. */
        static final Comparator<GradedSample> reverseGradeComparator
                = (GradedSample gs1, GradedSample gs2) -> Double.compare(gs2.grade, gs1.grade);

        final Sample sample;

        final double grade;

        GradedSample (Sample sample,
                      double grade)
        {
            this.sample = sample;
            this.grade = grade;
        }

        @Override
        public String toString ()
        {
            return "GradedSample{" + sample + " " + grade + "}";
        }
    }

    private class ParamAction
            extends AbstractAction
    {

        // Purpose is just to read and remember the data from the various input fields.
        // Triggered when user presses Enter in one of these fields.
        @Override
        public void actionPerformed (ActionEvent e)
        {
            inputParams();
            displayParams();
        }
    }

    private class SelectAction
            extends AbstractAction
    {

        SelectAction ()
        {
            super("Select");
            putValue(Action.SHORT_DESCRIPTION, "Build samples selection");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    trains = null;
                    tests = null;

                    // Get a fresh collection
                    getTrainSamples();
                }
            });
        }
    }

    private class StoreAction
            extends AbstractAction
    {

        StoreAction ()
        {
            super("Store");
            putValue(Action.SHORT_DESCRIPTION, "Store train/test selections as .csv files");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            GlyphDescriptor imgDesc = new ImgGlyphDescriptor();
            imgDesc.export("train", getTrainSamples(), true);
            imgDesc.export("test", getTestSamples(), false);

            GlyphDescriptor mixDesc = new MixGlyphDescriptor();
            mixDesc.export("train", getTrainSamples(), true);
            mixDesc.export("test", getTestSamples(), false);
        }
    }
}
