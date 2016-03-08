//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a y e s i a n P a n e l                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.WekaClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * Class {@code BayesianPanel} is the user interface dedicated to the bayesian engine.
 *
 * @author Hervé Bitteur
 */
class BayesianPanel
        extends TrainingPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BayesianPanel.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BayesianPanel} object.
     *
     * @param task           the current training activity
     * @param standardWidth  standard width for fields & buttons
     * @param selectionPanel the panel for sample repository
     */
    public BayesianPanel (Trainer.Task task,
                          String standardWidth,
                          SelectionPanel selectionPanel)
    {
        super(task, standardWidth, WekaClassifier.getInstance(), selectionPanel, 4);
        task.addObserver(this);

        trainAction = new TrainAction("Train");

        defineSpecificLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // epochEnded //
    //------------//
    @Override
    public void epochEnded (final int epochIndex,
                            double mse)
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            // This part is run on swing thread
            @Override
            public void run ()
            {
                progressBar.setValue(epochIndex + 1);
            }
        });
    }

    //-----------------//
    // trainingStarted //
    //-----------------//
    @Override
    public void trainingStarted (final int epochIndex,
                                 final int epochMax,
                                 final double mse)
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            // This part is run on swing thread
            @Override
            public void run ()
            {
                progressBar.setMaximum(epochMax);
            }
        });
    }

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    private void defineSpecificLayout ()
    {
        int r = 7;

        // Training entities
        JButton dumpButton = new JButton(new DumpAction());
        dumpButton.setToolTipText("Dump the evaluator internals");

        JButton trainButton = new JButton(trainAction);
        trainButton.setToolTipText("Train the evaluator from scratch");

        builder.add(dumpButton, cst.xy(3, r));
        builder.add(trainButton, cst.xy(5, r));
    }
}
