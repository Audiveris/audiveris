//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k P a n e l                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.ui.util.Panel;

import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>CheckPanel</code> handles a panel to display the results of a
 * check suite.
 *
 * @param <C> the subtype of Checkable-compatible objects used in the
 * homogeneous collection of checks of the suite
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckPanel<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CheckPanel.class);
    private static final Color  GREEN_COLOR = new Color(100, 150, 0);
    private static final Color  ORANGE_COLOR = new Color(255, 150, 0);
    private static final Color  RED_COLOR = new Color(255, 0, 0);
    private static final String LINE_GAP = "1dlu";
    private static final String COLUMN_GAP = "1dlu";
    private static final int    FIELD_WIDTH = 4;

    //~ Instance fields --------------------------------------------------------

    private CheckSuite<C>  suite;
    private JTextField     globalField;
    private Panel          component;
    private JTextField[][] values;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // CheckPanel //
    //------------//
    /**
     * Create a check panel for a given suite
     *
     * @param suite the suite whose results are to be displayed
     */
    public CheckPanel (CheckSuite<C> suite)
    {
        // Global field (for global result)
        globalField = new JTextField(FIELD_WIDTH);
        globalField.setEditable(false);
        globalField.setHorizontalAlignment(JTextField.CENTER);

        setSuite(suite);
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

    //----------//
    // setSuite //
    //----------//
    /**
     * Assign a (new) suite to the check pane
     *
     * @param suite the (new) check suite to be used
     */
    public void setSuite (CheckSuite<C> suite)
    {
        this.suite = suite;

        if (suite != null) {
            createValueFields(); // Assign values
            buildComponent(); // Create/update component
        }

        // Refresh the display
        if (component != null) {
            component.validate();
            component.repaint();
        }
    }

    //----------//
    // passForm //
    //----------//
    /**
     * Pass the whole suite on the provided checkable object, and display the
     * results
     *
     * @param object the object to be checked
     */
    public void passForm (C object)
    {
        // Empty all text fields
        for (Component comp : component.getComponents()) {
            if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                field.setText("");
                field.setToolTipText(null);
            }
        }

        if (object == null) {
            return;
        }

        CheckResult result = new CheckResult();
        double      grade = 0d;
        boolean     failed = false;

        // Fill one row per check
        for (int index = 0; index < suite.getChecks()
                                         .size(); index++) {
            Check<C> check = suite.getChecks()
                                  .get(index);

            // Run this check
            check.pass(object, result, false);
            grade += (result.flag * suite.getWeights()
                                         .get(index));

            // Update proper field to display check result
            JTextField field = null;

            switch (result.flag) {
            case Check.RED :
                failed = true;

                if (check.isCovariant()) {
                    field = values[index][0];
                    field.setToolTipText("Value is too low");
                } else {
                    field = values[index][2];
                    field.setToolTipText("Value is too high");
                }

                field.setForeground(RED_COLOR);

                break;

            case Check.ORANGE :
                field = values[index][1];
                field.setToolTipText("Value is acceptable");
                field.setForeground(ORANGE_COLOR);

                break;

            case Check.GREEN :

                if (check.isCovariant()) {
                    field = values[index][2];
                } else {
                    field = values[index][0];
                }

                field.setToolTipText("Value is OK");
                field.setForeground(GREEN_COLOR);

                break;
            }

            field.setText(String.format("%5.2f", result.value));
        }

        // Global suite result
        if (failed) {
            globalField.setForeground(RED_COLOR);
            globalField.setToolTipText("Check has failed!");
            globalField.setText("Failed");
        } else {
            grade /= suite.getTotalWeight();

            if (grade >= suite.getThreshold()) {
                globalField.setForeground(GREEN_COLOR);
                globalField.setToolTipText("Check has succeeded!");
            } else {
                globalField.setForeground(RED_COLOR);
                globalField.setToolTipText("Check has failed!");
            }

            globalField.setText(String.format("%5.2f", grade));
        }
    }

    //----------------//
    // buildComponent //
    //----------------//
    private void buildComponent ()
    {
        // Either allocate a new Panel or empty the existing one
        if (component == null) {
            component = new Panel();
            component.setNoInsets();
        } else {
            component.removeAll();
        }

        final int    checkNb = suite.getChecks()
                                    .size();
        PanelBuilder b = new PanelBuilder(createLayout(checkNb), component);
        b.setDefaultDialogBorder();

        CellConstraints c = new CellConstraints();

        // Rows
        int ic = -1; // Check index
        int r = -1; // Row index

        for (Check check : suite.getChecks()) {
            ic++;
            r += 2;

            // Covariance label
            JLabel covariantLabel;

            if (check.isCovariant()) {
                covariantLabel = new JLabel("T");
                covariantLabel.setToolTipText("Check is covariant");
            } else {
                covariantLabel = new JLabel("F");
                covariantLabel.setToolTipText("Check is contravariant");
            }

            b.add(covariantLabel, c.xy(1, r));

            // Weight label
            JLabel weightLabel = new JLabel(
                String.format("%s", suite.getWeights().get(ic).intValue()));
            weightLabel.setToolTipText("Relative weight for this check");
            b.add(weightLabel, c.xy(3, r));

            // Name label
            JLabel nameLabel = new JLabel(check.getName());

            if (check.getDescription() != null) {
                nameLabel.setToolTipText(check.getDescription());
            }

            b.add(nameLabel, c.xy(5, r));

            b.add(values[ic][0], c.xy(7, r));

            JLabel lowLabel = new JLabel(
                String.format("%5.2f", check.getLow()));
            lowLabel.setToolTipText("Lower limit of orange zone");
            b.add(lowLabel, c.xy(9, r));

            b.add(values[ic][1], c.xy(11, r));

            JLabel highLabel = new JLabel(
                String.format("%5.2f", check.getHigh()));
            highLabel.setToolTipText("Higher limit of orange zone");
            b.add(highLabel, c.xy(13, r));

            b.add(values[ic][2], c.xy(15, r));
        }

        // Last row for global result
        r += 2;

        JLabel globalLabel = new JLabel("Result");
        globalLabel.setToolTipText("Global check result");
        b.add(globalLabel, c.xy(7, r));
        b.add(globalField, c.xy(11, r));
    }

    //--------------//
    // createLayout //
    //--------------//
    private FormLayout createLayout (int checkNb)
    {
        // Build proper column specification
        StringBuffer sbc = new StringBuffer();
        sbc.append("center:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Covariance
        sbc.append("center:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Weight
        sbc.append(" right:46dlu")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Name
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", ");
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // Low limit
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", ");
        sbc.append(" right:pref")
           .append(", ")
           .append(COLUMN_GAP)
           .append(", "); // High Limit
        sbc.append(" right:pref");

        // Build proper row specification
        StringBuffer sbr = new StringBuffer();

        for (int n = 0; n <= checkNb; n++) {
            if (n != 0) {
                sbr.append(", ")
                   .append(LINE_GAP)
                   .append(", ");
            }

            sbr.append("pref");
        }

        if (logger.isFineEnabled()) {
            logger.fine("sb cols=" + sbc);
            logger.fine("sb rows=" + sbr);
        }

        // Create proper form layout
        return new FormLayout(
            sbc.toString(), //cols
            sbr.toString()); //rows
    }

    //-------------------//
    // createValueFields //
    //-------------------//
    private void createValueFields ()
    {
        // Allocate value fields
        final int checkNb = suite.getChecks()
                                 .size();
        values = new JTextField[checkNb][];

        for (int n = 0; n < checkNb; n++) {
            values[n] = new JTextField[3];

            for (int i = 0; i <= 2; i++) {
                JTextField field = new JTextField(FIELD_WIDTH);
                field.setEditable(false);
                field.setHorizontalAlignment(JTextField.CENTER);
                values[n][i] = field;
            }
        }
    }
}
