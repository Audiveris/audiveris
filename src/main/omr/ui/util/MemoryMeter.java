//--------------------------------------------------------------------------//
//                                                                          //
//                          M e m o r y M e t e r                           //
//                                                                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.             //
//  This software is released under the terms of the GNU General Public     //
//  License. Please contact the author at herve.bitteur@laposte.net         //
//  to report bugs & suggestions.                                           //
//--------------------------------------------------------------------------//
package omr.ui.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Memory;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Class <code>MemoryMeter</code> encapsulates the display of a linear memory
 * meter in MB (both used and total), together with a garbage-collection
 * button.
 *
 * <P>There is a alarm threshold that triggers a color switch to red whenever
 * the used memory exceeds the threshold.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class MemoryMeter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

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
    private int lastTotal;

    /** Last value for used memory, in order to save on display */
    private int lastUsed;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Basic Memory Meter, with default alarm threshold and display period.
     */
    public MemoryMeter ()
    {
        this(null);
    }

    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Basic Memory Meter, with default alarm threshold and display period,
     * and specific Icon for garbage collectore button
     *
     * @param buttonIcon Specific icon for garbage collector
     */
    public MemoryMeter (Icon buttonIcon)
    {
        component = new JPanel();

        try {
            defineUI(buttonIcon);
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
    private void defineUI (Icon buttonIcon)
    {
        component.setLayout(new BorderLayout());

        // Progress bar
        progressBar.setToolTipText("Used memory of total memory");
        progressBar.setString("0KB / 0KB");
        progressBar.setStringPainted(true);
        component.add(progressBar, BorderLayout.CENTER);

        // Garbage button
        JButton button = new JButton();
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setToolTipText("Run Garbage Collector");

        if (buttonIcon != null) {
            button.setIcon(buttonIcon);
        } else {
            button.setText("GC");
        }

        button.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        System.gc();
                        System.runFinalization();
                        System.gc();
                        displayMemory();
                    }
                });
        component.add(button, BorderLayout.EAST);

        // Remember the default foreground color
        defaultForeground = progressBar.getForeground();
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
                    int total = ((int) Memory.total() * 10) / (1024 * 1024);
                    int used = ((int) (Memory.occupied()) * 10) / (1024 * 1024);

                    if ((total != lastTotal) || (used != lastUsed)) {
                        progressBar.setMaximum(total);
                        progressBar.setValue(used);
                        progressBar.setString(
                            String.format(
                                "%,.1f/%,.1f MB",
                                used / 10f,
                                total / 10f));
                        lastTotal = total;
                        lastUsed = used;

                        if (used > (10 * constants.alarmThreshold.getValue())) {
                            progressBar.setForeground(Color.red);
                        } else {
                            progressBar.setForeground(defaultForeground);
                        }
                    }
                }
            };

        // Monitoring thread
        Thread monitorThread = new Thread() {
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
            "Display period");

        /** Alarm threshold */
        Constant.Integer alarmThreshold = new Constant.Integer(
            "MegaBytes",
            70,
            "Alarm threshold");
    }
}
