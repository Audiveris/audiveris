//----------------------------------------------------------------------------//
//                                                                            //
//                        S e l e c t i o n P a n e l                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphRegression;
import static omr.glyph.ui.GlyphTrainer.Task.Activity.*;

import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

/**
 * Class <code>SelectionPanel</code> handles a user panel to select names from
 * glyph repository, either the whole population or a core set of glyphs. This
 * class is a dedicated companion of {@link GlyphTrainer}.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class SelectionPanel
    extends Panel
    implements GlyphRepository.Monitor, Observer
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants  constants = new Constants();
    private static final Logger     logger = Logger.getLogger(
        GlyphRepository.class);

    //~ Instance fields --------------------------------------------------------

    /** Current activity */
    private final GlyphTrainer.Task task;

    /** Underlying repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** For asynchronous execution of the glyph selection */
    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Visual progression of the selection */
    protected JProgressBar progressBar = new JProgressBar();

    /** To refresh the application wrt to the training material on disk */
    RefreshAction refreshAction = new RefreshAction();

    /** To select a core out of whole base */
    SelectAction selectAction = new SelectAction();

    /** Counter on loaded glyphs */
    private int nbLoaded;

    /** Input/output on maximum number of glyphs with same shape */
    LIntegerField similar = new LIntegerField(
        "Max Similar",
        "Max number of similar shapes");

    /** Displayed counter on existing glyph files */
    LIntegerField totalFiles = new LIntegerField(
        false,
        "Total",
        "Total number of glyph files");

    /** Displayed counter on loaded glyphs */
    LIntegerField nbLoadedFiles = new LIntegerField(
        false,
        "Loaded",
        "Number of glyph files loaded so far");

    /** Displayed counter on selected glyphs */
    LIntegerField nbSelectedFiles = new LIntegerField(
        false,
        "Selected",
        "Number of selected glyph files to load");

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SelectionPanel //
    //-----------------//
    /**
     * Creates a new SelectionPanel object.
     * @param task the common training task object
     * @param standardWidth standard width to be used for fields & buttons
     */
    public SelectionPanel (GlyphTrainer.Task task,
                           String            standardWidth)
    {
        this.task = task;
        task.addObserver(this);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("ENTER"), "readParams");
        getActionMap()
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
    public Collection<String> getBase (boolean whole)
    {
        nbLoaded = 0;
        progressBar.setValue(nbLoaded);

        if (whole) {
            return repository.getWholeBase(this);
        } else {
            return repository.getCoreBase(this);
        }
    }

    //-------------------//
    // setSelectedGlyphs //
    //-------------------//
    /**
     * Notify the number of glyphs selected
     *
     * @param selected number of selected glyphs
     */
    @Implement(GlyphRepository.Monitor.class)
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
    @Implement(GlyphRepository.Monitor.class)
    public void setTotalGlyphs (int total)
    {
        totalFiles.setValue(total);
        progressBar.setMaximum(total);
    }

    //-------------//
    // loadedGlyph //
    //-------------//
    /**
     * Call-back when a glyph has just been loaded
     *
     * @param glyph the loaded glyph
     */
    @Implement(GlyphRepository.Monitor.class)
    public void loadedGlyph (Glyph glyph)
    {
        nbLoadedFiles.setValue(++nbLoaded);
        progressBar.setValue(nbLoaded);
    }

    //--------//
    // update //
    //--------//
    @Implement(Observer.class)
    public void update (Observable obs,
                        Object     unused)
    {
        switch (task.getActivity()) {
        case INACTIVE :
            selectAction.setEnabled(true);

            break;

        case SELECTING :
            selectAction.setEnabled(false);

            break;

        case TRAINING :
            selectAction.setEnabled(false);

            break;
        }
    }

    //------------//
    // defineCore //
    //------------//
    private void defineCore ()
    {
        class NotedGlyph
        {
            String gName;
            Glyph  glyph;
            double grade;
        }
        // What for ? TBD
        inputParams();

        // Train regression on them
        GlyphRegression    regression = GlyphRegression.getInstance();
        Collection<String> gNames = getBase(true); // use whole
        List<Glyph>        glyphs = new ArrayList<Glyph>();

        // Actually load each glyph description, if not yet done
        for (String gName : gNames) {
            Glyph glyph = repository.getGlyph(gName);

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        // QUickly train the regression evaluator
        regression.train(glyphs, null, Evaluator.StartingMode.SCRATCH);

        // Measure every glyph
        List<NotedGlyph> palmares = new ArrayList<NotedGlyph>(gNames.size());

        for (String gName : gNames) {
            NotedGlyph ng = new NotedGlyph();
            ng.gName = gName;
            ng.glyph = repository.getGlyph(gName);

            if (ng.glyph != null) {
                ng.grade = regression.measureDistance(
                    ng.glyph,
                    ng.glyph.getShape());
                palmares.add(ng);
            }
        }

        // Sort the palmares, shape by shape, by decreasing grade, so that
        // the worst glyphs are found first
        Collections.sort(
            palmares,
            new Comparator<NotedGlyph>() {
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

        final int maxSimilar = (similar.getValue() + 1) / 2;

        // Keep only MaxSimilar/2 of each WORST shape
        for (NotedGlyph ng : palmares) {
            int index = ng.glyph.getShape()
                                .ordinal();

            if (++counters[index] <= maxSimilar) {
                set.add(ng);
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        String.format("%.5f worst Core %s", ng.grade, ng.gName));
                }
            }
        }

        // Keep only MaxSimilar/2 of each BEST shape
        // We just have to browse backward
        Arrays.fill(counters, 0);

        for (ListIterator<NotedGlyph> it = palmares.listIterator(
            palmares.size() - 1); it.hasPrevious();) {
            NotedGlyph ng = it.previous();
            int        index = ng.glyph.getShape()
                                       .ordinal();

            if (++counters[index] <= maxSimilar) {
                set.add(ng);
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        String.format("%.5f best Core %s", ng.grade, ng.gName));
                }
            }
        }

        // Build the core base
        List<String> base = new ArrayList<String>(set.size());

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
        FormLayout      layout = Panel.makeFormLayout(
            3,
            4,
            "",
            standardWidth,
            standardWidth);
        PanelBuilder    builder = new PanelBuilder(layout, this);
        CellConstraints cst = new CellConstraints();
        JButton         refreshButton = new JButton(refreshAction);
        JButton         selectButton = new JButton(selectAction);
        refreshButton.setToolTipText("Refresh trainer with disk information");
        selectButton.setToolTipText(
            "Build core selection out of whole glyph base");
        builder.setDefaultDialogBorder();

        int r = 1; // ----------------------------
        builder.addSeparator("Selection", cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));

        r += 2; // ----------------------------
        builder.add(refreshButton, cst.xy(3, r));

        builder.add(similar.getLabel(), cst.xy(9, r));
        builder.add(similar.getField(), cst.xy(11, r));

        builder.add(totalFiles.getLabel(), cst.xy(13, r));
        builder.add(totalFiles.getField(), cst.xy(15, r));

        r += 2; // ----------------------------
        builder.add(selectButton, cst.xy(3, r));
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
    private static class Constants
        extends ConstantSet
    {
        Constant.Integer maxSimilar = new Constant.Integer(
            20,
            "Absolute maximum number of instances for the same shape" +
            " used in training");

        Constants ()
        {
            initialize();
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        // Purpose is just to read and remember the data from the various
        // input fields. Triggered when user presses Enter in one of these
        // fields.
        @Implement(ActionListener.class)
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
        public RefreshAction ()
        {
            super("Disk Refresh");
        }

        @Implement(ActionListener.class)
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
        public SelectAction ()
        {
            super("Select Core");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(
                new Runnable() {
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
