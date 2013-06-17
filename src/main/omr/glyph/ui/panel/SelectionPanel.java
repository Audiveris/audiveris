//----------------------------------------------------------------------------//
//                                                                            //
//                        S e l e c t i o n P a n e l                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui.panel;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.EvaluationEngine;
import omr.glyph.GlyphRegression;
import omr.glyph.GlyphRepository;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import static omr.glyph.ui.panel.GlyphTrainer.Task.Activity.*;

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
 * Class {@code SelectionPanel} handles a user panel to select <B>names</B>
 * from glyph repository, either the whole population or a core set of
 * glyphs.
 * This class is a dedicated companion of {@link GlyphTrainer}.
 *
 * @author Hervé Bitteur
 */
class SelectionPanel
        implements GlyphRepository.Monitor, Observer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SelectionPanel.class);

    //~ Instance fields --------------------------------------------------------
    /** Reference of network panel companion (TBI) */
    private TrainingPanel trainingPanel;

    /** Swing component */
    private final Panel component;

    /** Current activity */
    private final GlyphTrainer.Task task;

    /** Underlying repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** For asynchronous execution of the glyph selection */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection */
    private JProgressBar progressBar = new JProgressBar();

    /** To dump the current selection of glyphs used for training/validation */
    private DumpAction dumpAction = new DumpAction();

    /** To refresh the application wrt to the training material on disk */
    private RefreshAction refreshAction = new RefreshAction();

    /** To select a core out of whole base */
    private SelectAction selectAction = new SelectAction();

    /** Counter on loaded glyphs */
    private int nbLoaded;

    /** Input/output on maximum number of glyphs with same shape */
    private LIntegerField similar = new LIntegerField(
            "Max Similar",
            "Max number of similar shapes");

    /** Displayed counter on existing glyph files */
    private LIntegerField totalFiles = new LIntegerField(
            false,
            "Total",
            "Total number of glyph files");

    /** Displayed counter on loaded glyphs */
    private LIntegerField nbLoadedFiles = new LIntegerField(
            false,
            "Loaded",
            "Number of glyph files loaded so far");

    /** Displayed counter on selected glyphs */
    private LIntegerField nbSelectedFiles = new LIntegerField(
            false,
            "Selected",
            "Number of selected glyph files to load");

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // SelectionPanel //
    //----------------//
    /**
     * Creates a new SelectionPanel object.
     *
     * @param task          the common training task object
     * @param standardWidth standard width to be used for fields & buttons
     */
    public SelectionPanel (GlyphTrainer.Task task,
                           String standardWidth)
    {
        this.task = task;
        task.addObserver(this);

        component = new Panel();
        component.setNoInsets();

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "readParams");
        component.getActionMap()
                .put("readParams", new ParamAction());

        displayParams();

        defineLayout(standardWidth);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getBase //
    //---------//
    /**
     * Retrieve the selected collection of glyph names
     *
     * @param whole indicate whether the whole population is to be selected, or
     *              just the core
     * @return the collection of selected glyphs names
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
     * Call-back when a glyph has just been loaded
     *
     * @param gName the normalized glyph name
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
     * Notify the number of glyphs selected
     *
     * @param selected number of selected glyphs
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
     * Notify the total number of glyphs in the base
     *
     * @param total the total number of glyphs available
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
        GlyphRegression regression = GlyphRegression.getInstance();
        Collection<String> gNames = getBase(true); // use whole
        List<Glyph> glyphs = new ArrayList<>();

        // Actually load each glyph description, if not yet done
        for (String gName : gNames) {
            Glyph glyph = repository.getGlyph(gName, this);

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        // Quickly train the regression evaluator (on the whole base)
        regression.train(glyphs, null, EvaluationEngine.StartingMode.SCRATCH);

        // Measure all glyphs of each shape
        Map<Shape, List<NotedGlyph>> palmares = new HashMap<>();

        for (String gName : gNames) {
            Glyph glyph = repository.getGlyph(gName, this);

            if (glyph != null) {
                try {
                    Shape shape = glyph.getShape();
                    double grade = regression.measureDistance(
                            glyph,
                            shape);
                    List<NotedGlyph> shapeNotes = palmares.get(shape);

                    if (shapeNotes == null) {
                        shapeNotes = new ArrayList<>();
                        palmares.put(shape, shapeNotes);
                    }

                    shapeNotes.add(new NotedGlyph(gName, glyph, grade));
                } catch (Exception ex) {
                    logger.warn("Cannot evaluate {}", glyph);
                }
            }
        }

        // Set of chosen shapes
        final Set<NotedGlyph> set = new HashSet<>();
        final int maxSimilar = similar.getValue();

        // Sort the palmares, shape by shape, by (decreasing) grade
        for (List<NotedGlyph> shapeNotes : palmares.values()) {
            Collections.sort(shapeNotes, NotedGlyph.reverseGradeComparator);

            // Take a sample equally distributed on instances of this shape
            final int size = shapeNotes.size();
            final float delta = ((float) (size - 1)) / (maxSimilar - 1);

            for (int i = 0; i < maxSimilar; i++) {
                int idx = Math.min(size - 1, Math.round(i * delta));
                NotedGlyph ng = shapeNotes.get(idx);

                if (ng.glyph.getShape()
                        .isTrainable()) {
                    set.add(ng);
                }
            }
        }

        // Build the core base
        List<String> base = new ArrayList<>(set.size());

        for (NotedGlyph ng : set) {
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
        FormLayout layout = Panel.makeFormLayout(
                3,
                4,
                "",
                standardWidth,
                standardWidth);
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();
        builder.setDefaultDialogBorder();

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

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer maxSimilar = new Constant.Integer(
                "Glyphs",
                10,
                "Absolute maximum number of instances for the same shape"
                + " used in training");

    }

    //------------//
    // NotedGlyph //
    //------------//
    /**
     * Handle a glyph together with its name and grade
     */
    private static class NotedGlyph
    {
        //~ Static fields/initializers -----------------------------------------

        /** For comparing NotedGlyph instance in reverse grade order */
        static final Comparator<NotedGlyph> reverseGradeComparator = new Comparator<NotedGlyph>()
        {
            @Override
            public int compare (NotedGlyph ng1,
                                NotedGlyph ng2)
            {
                return Double.compare(ng2.grade, ng1.grade);
            }
        };

        //~ Instance fields ----------------------------------------------------
        final String gName;

        final Glyph glyph;

        final double grade;

        //~ Constructors -------------------------------------------------------
        public NotedGlyph (String gName,
                           Glyph glyph,
                           double grade)
        {
            this.gName = gName;
            this.glyph = glyph;
            this.grade = grade;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{NotedGlyph " + gName + " " + grade + "}";
        }
    }

    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpAction ()
        {
            super("Dump");
            putValue(
                    Action.SHORT_DESCRIPTION,
                    "Dump the current glyph selection");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            List<String> gNames = getBase(trainingPanel.useWhole());
            System.out.println(
                    "Content of "
                    + (trainingPanel.useWhole() ? "whole" : "core")
                    + " population (" + gNames.size() + "):");
            Collections.sort(gNames, GlyphRepository.shapeComparator);

            int glyphNb = 0;
            String prevName = null;

            for (String gName : gNames) {
                if (prevName != null) {
                    if (!GlyphRepository.shapeNameOf(gName)
                            .equals(prevName)) {
                        System.out.println(
                                String.format("%4d %s", glyphNb, prevName));
                        glyphNb = 1;
                    }
                }

                glyphNb++;
                prevName = GlyphRepository.shapeNameOf(gName);
            }

            System.out.println(String.format("%4d %s", glyphNb, prevName));
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public RefreshAction ()
        {
            super("Disk Refresh");
            putValue(
                    Action.SHORT_DESCRIPTION,
                    "Refresh trainer with disk information");
        }

        //~ Methods ------------------------------------------------------------
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
        //~ Constructors -------------------------------------------------------

        public SelectAction ()
        {
            super("Select Core");
            putValue(
                    Action.SHORT_DESCRIPTION,
                    "Build core selection out of whole glyph base");
        }

        //~ Methods ------------------------------------------------------------
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
