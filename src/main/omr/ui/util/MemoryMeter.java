//--------------------------------------------------------------------------//
//                                                                          //
//                          M e m o r y M e t e r                           //
//                                                                          //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.             //
//  This software is released under the terms of the GNU General Public     //
//  License. Please contact the author at herve.bitteur@laposte.net         //
//  to report bugs & suggestions.                                           //
//--------------------------------------------------------------------------//

package omr.ui.util;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import omr.util.Memory;

/**
 * Class <code>MemoryMeter</code> encapsulates the display of a linear memory
 * meter in KB (both used and total), together with a garbage-collection
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
    //~ Static variables/initializers -------------------------------------

    /** Default display period, in milli-seconds : {@value} */
    public static final int DEFAULT_DISPLAY_PERIOD = 2000;

    /** Default alarm threshold, in KB : {@value} */
    public static final int DEFAULT_ALARM_THRESHOLD = 40960;

    //~ Instance variables ------------------------------------------------

    // Related concrete component
    private JPanel component;

    // Progress bar
    private JProgressBar progressBar = new JProgressBar();

    // Current display period
    private int displayPeriod = DEFAULT_DISPLAY_PERIOD;

    // Current alarm threshold
    private int alarmThreshold = DEFAULT_ALARM_THRESHOLD;

    // Last values for used and total memory, in order to save on display
    private int lastUsed;
    private int lastTotal;

    // Default foreground color, when under alarm threshold
    private Color defaultForeground;

    // Flag on monitoring activity
    private volatile boolean monitoring;

    // Runnable that displays the memory usage
    private Runnable displayer;

    //~ Constructors ------------------------------------------------------

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
        this(buttonIcon,
             DEFAULT_DISPLAY_PERIOD,
             DEFAULT_ALARM_THRESHOLD);
    }

    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Memory Meter, with all parameters.
     *
     * @param buttonIcon     Specific icon for garbage collector
     * @param displayPeriod  Time period, in milliseconds, between displays
     * @param alarmThreshold Memory threshold to trigger alarm
     */
    public MemoryMeter (Icon buttonIcon,
                        int  displayPeriod,
                        int  alarmThreshold)
    {
        component = new JPanel();

        setDisplayPeriod(displayPeriod);
        setAlarmThreshold(alarmThreshold);

        try {
            defineUI(buttonIcon);
            initialize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent()
    {
        return component;
    }

    //-------------------//
    // setAlarmThreshold //
    //-------------------//
    /**
     * Modify the alarm threshold
     *
     * @param alarmThreshold the new alarm threshold, expressed in KB
     */
    public void setAlarmThreshold (int alarmThreshold)
    {
        this.alarmThreshold = alarmThreshold;
    }

    //-------------------//
    // getAlarmThreshold //
    //-------------------//
    /**
     * Report the current alarm threshold
     *
     * @return the current alarm threshold
     */
    public int getAlarmThreshold ()
    {
        return alarmThreshold;
    }

    //------------------//
    // setDisplayPeriod //
    //------------------//
    /**
     * Modify the current display period
     *
     * @param displayPeriod the new display period, expressed in
     * milliseconds
     */
    public void setDisplayPeriod (int displayPeriod)
    {
        this.displayPeriod = displayPeriod;
    }

    //------------------//
    // getDisplayPeriod //
    //------------------//
    /**
     * Report the current display period
     *
     * @return the current display period
     */
    public int getDisplayPeriod ()
    {
        return displayPeriod;
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

    //------------//
    // initialize //
    //------------//
    private void initialize ()
    {
        // Displayer
        displayer = new Runnable()
        {
            public void run ()
            {
                int total = (int) Memory.total()      / 1024;
                int used  = (int) (Memory.occupied()) / 1024;

                if ((total != lastTotal) || (used != lastUsed)) {
                    progressBar.setMaximum(total);
                    progressBar.setValue(used);
                    progressBar.setString(String.format("%,d KB / %,d KB",
                                                        used, total));
                    lastTotal = total;
                    lastUsed = used;

                    if (used > getAlarmThreshold()) {
                        progressBar.setForeground(Color.red);
                    } else {
                        progressBar.setForeground(defaultForeground);
                    }
                }
            }
        };

        // Monitoring thread
        Thread monitorThread = new Thread()
            {
                public void run ()
                {
                    monitoring = true;

                    while (monitoring) {
                        displayMemory();

                        try {
                            sleep(displayPeriod);
                        } catch (InterruptedException ex1) {
                            monitoring = false;
                        }
                    }
                }
            };
        monitorThread.setPriority(Thread.MIN_PRIORITY);
        monitorThread.start();
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

        button.addActionListener
            (new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        System.gc();
                        System.runFinalization();
                        System.gc();
                        displayMemory();
                    }
                }
             );
        component.add(button, BorderLayout.EAST);

        // Remember the default foreground color
        defaultForeground = progressBar.getForeground();
    }
}
