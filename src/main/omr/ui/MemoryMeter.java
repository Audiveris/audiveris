//----------------------------------------------------------------------------//
//                                                                            //
//                           M e m o r y M e t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Memory;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import java.awt.*;

import javax.swing.*;

/**
 * Class <code>MemoryMeter</code> encapsulates the display of a linear memory
 * meter in MB (both used and total), together with a garbage-collection
 * button.
 *
 * <P>There is a alarm threshold that triggers a color switch to red whenever
 * the used memory exceeds the threshold.
 *
 * @author Hervé Bitteur
 */
public class MemoryMeter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** A kilo as 2**10 */
    private static final int KILO = 1024;

    //~ Instance fields --------------------------------------------------------

    /** Default foreground color, when under alarm threshold */
    private Color defaultForeground;

    /** Related concrete component */
    private JPanel component;

    /** Progress bar */
    private JProgressBar progressBar = new JProgressBar();

    /** Runnable that displays the memory usage */
    private Runnable displayer;

    /** Flag on monitoring activity */
    private volatile boolean monitoring;

    /** Last value for global memory */
    private int lastTotal = 0;

    /** Last value for used memory, in order to save on display */
    private int lastUsed = 0;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Basic Memory Meter, with default alarm threshold and display period.
     */
    public MemoryMeter ()
    {
        component = new JPanel();

        try {
            defineUI();
            initialize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //----------------//
    // collectGarbage //
    //----------------//
    @Action
    public void collectGarbage ()
    {
        System.gc();
        System.runFinalization();
        System.gc();
        displayMemory();
    }

    //---------------//
    // displayMemory //
    //---------------//
    /**
     * Trigger an immediate memory display
     */
    public void displayMemory ()
    {
        SwingUtilities.invokeLater(displayer);
    }

    //------//
    // stop //
    //------//
    /**
     * Stop the memory monitoring
     */
    public void stop ()
    {
        monitoring = false;
    }

    //----------//
    // defineUI //
    //----------//
    private void defineUI ()
    {
        ApplicationContext applicationContext = MainGui.getInstance()
                                                       .getContext();
        component.setLayout(new BorderLayout());

        // Progress bar
        progressBar.setName("progressBar");
        progressBar.setToolTipText("Current used memory from global memory");
        progressBar.setString("0KB / 0KB");
        progressBar.setStringPainted(true);
        component.add(progressBar, BorderLayout.CENTER);

        // Garbage collector button
        JButton button = new JButton(
            applicationContext.getActionMap(this).get("collectGarbage"));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        component.add(button, BorderLayout.EAST);

        // Remember the default foreground color
        defaultForeground = progressBar.getForeground();

        // Resource injection
        ResourceMap resource = applicationContext.getResourceMap(getClass());
        resource.injectComponents(component);
    }

    //------------//
    // initialize //
    //------------//
    private void initialize ()
    {
        // Displayer
        displayer = new Runnable() {
                public void run ()
                {
                    int totalKB = (int) (Memory.total() / KILO);
                    int usedKB = (int) (Memory.occupied() / KILO);

                    if ((totalKB != lastTotal) || (usedKB != lastUsed)) {
                        progressBar.setMaximum(totalKB);
                        progressBar.setValue(usedKB);
                        progressBar.setString(
                            String.format(
                                "%,.1f/%,.0f MB",
                                (float) usedKB / KILO,
                                (float) totalKB / KILO));
                        lastTotal = totalKB;
                        lastUsed = usedKB;

                        if (((float) usedKB / KILO) > constants.alarmThreshold.getValue()) {
                            progressBar.setForeground(Color.red);
                        } else {
                            progressBar.setForeground(defaultForeground);
                        }
                    }
                }
            };

        // Monitoring thread
        Thread monitorThread = new Thread() {
            @Override
            public void run ()
            {
                monitoring = true;

                while (monitoring) {
                    displayMemory();

                    try {
                        sleep(constants.displayPeriod.getValue());
                    } catch (InterruptedException ex1) {
                        monitoring = false;
                    }
                }
            }
        };

        monitorThread.setName(getClass().getName());
        monitorThread.setPriority(Thread.MIN_PRIORITY);
        monitorThread.start();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Display period */
        Constant.Integer displayPeriod = new Constant.Integer(
            "MilliSeconds",
            2000,
            "Memory display period");

        /** Alarm threshold */
        Constant.Integer alarmThreshold = new Constant.Integer(
            "MegaBytes",
            100,
            "Memory alarm threshold");
    }
}
