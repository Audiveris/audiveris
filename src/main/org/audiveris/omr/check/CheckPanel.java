//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C h e c k P a n e l                                       //
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
package org.audiveris.omr.check;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.NamedDouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Class {@code CheckPanel} handles a panel to display the results of a check suite,
 * run manually.
 *
 * @param <C> the subtype of Checkable objects used in the homogeneous collection of checks of the
 *            suite
 * @author Hervé Bitteur
 */
public class CheckPanel<C>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CheckPanel.class);

    // Colors
    private static final Color GREEN_COLOR = new Color(100, 150, 0);

    private static final Color ORANGE_COLOR = new Color(255, 150, 0);

    private static final Color RED_COLOR = new Color(255, 0, 0);

    // Sizes
    private static final String LINE_GAP = "1dlu";

    private static final String COLUMN_GAP = "1dlu";

    private static final int FIELD_WIDTH = 4;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related check suite (the model). */
    private CheckSuite<C> suite;

    /** The swing component that includes all the fields. */
    private Panel component;

    /** The field for global result. */
    private final JTextField globalField;

    /** Matrix of all value fields. */
    private JTextField[][] values;

    /** Matrix of all bound fields. */
    private JTextField[][] bounds;

    /** Last object checked. */
    private C checkable;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a check panel for a given suite.
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

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //----------//
    // passForm //
    //----------//
    /**
     * Pass the whole suite on the provided checkable object, and
     * display the results.
     *
     * @param checkable the object to be checked
     */
    public void passForm (C checkable)
    {
        // Remember the 'current' checkable object
        this.checkable = checkable;

        resetValues();

        if (checkable == null) {
            return;
        }

        // Run the suite
        GradeImpacts impacts = suite.getImpacts(checkable);

        // Fill one row per check
        List<Check<C>> checks = suite.getChecks();

        for (int index = 0; index < checks.size(); index++) {
            final Check<C> check = checks.get(index);
            final double value = suite.getChecks().get(index).getValue(checkable); ////impacts.getValue(index);

            // Update proper field to display check result
            final int col = (value <= check.getLow()) ? 0 : ((value < check.getHigh()) ? 1 : 2);
            values[index][col].setText(textOf(value));
        }

        // Global suite result
        final double grade = impacts.getGrade();

        if (grade >= suite.getGoodThreshold()) {
            globalField.setForeground(GREEN_COLOR);
            globalField.setToolTipText("Good result!");
        } else if (grade >= suite.getMinThreshold()) {
            globalField.setForeground(ORANGE_COLOR);
            globalField.setToolTipText("Acceptable result!");
        } else {
            globalField.setForeground(RED_COLOR);
            globalField.setToolTipText("Check has totally failed!");
        }

        globalField.setText(textOf(grade));
    }

    //----------//
    // setSuite //
    //----------//
    /**
     * Assign a (new) suite to the check pane.
     *
     * @param suite the (new) check suite to be used
     */
    public final void setSuite (CheckSuite<C> suite)
    {
        this.suite = suite;

        if (suite != null) {
            createValueFields(); // Values
            createBoundFields(); // Bounds
            buildComponent(); // Create/update component
        }

        // Refresh the display
        if (component != null) {
            component.validate();
            component.repaint();
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

            // Needed to process user input when RETURN/ENTER is pressed
            component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke("ENTER"),
                    "ParamAction");
            component.getActionMap().put("ParamAction", new ParamAction());
        } else {
            component.removeAll();
        }

        final int checkNb = suite.getChecks().size();
        PanelBuilder b = new PanelBuilder(createLayout(checkNb + 1), component);

        ///b.setDefaultDialogBorder();
        CellConstraints c = new CellConstraints();

        // Rows
        int ic = -1; // Check index
        int r = -1; // Row index

        for (Check<C> check : suite.getChecks()) {
            ic++;
            r += 2;

            // Covariance label
            JLabel covariantLabel;

            if (check.isCovariant()) {
                covariantLabel = new JLabel(">");
                covariantLabel.setToolTipText("Higher is better");
                values[ic][0].setForeground(RED_COLOR);
                values[ic][1].setForeground(ORANGE_COLOR);
                values[ic][2].setForeground(GREEN_COLOR);
            } else {
                covariantLabel = new JLabel("<");
                covariantLabel.setToolTipText("Lower is better");
                values[ic][0].setForeground(GREEN_COLOR);
                values[ic][1].setForeground(ORANGE_COLOR);
                values[ic][2].setForeground(RED_COLOR);
            }

            b.add(covariantLabel, c.xy(1, r));

            // Name label with proper tooltip
            JLabel nameLabel = new JLabel(check.getName());

            if (check.getDescription() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                sb.append(check.getDescription());
                sb.append("<br/>");

                // Tell data unit if relevant
                String unit = check.getLowDouble().getQuantityUnit();

                if (unit == null) {
                    unit = check.getHighDouble().getQuantityUnit();
                }

                if (unit != null) {
                    sb.append("Unit=").append(unit);
                } else {
                    // Otherwise, simply tell the data type
                    sb.append("Type=").append(check.getHighDouble().getClass().getSimpleName());
                }

                sb.append("</html>");

                nameLabel.setToolTipText(sb.toString());
            }

            b.add(nameLabel, c.xy(3, r));

            // Value & bound fields
            b.add(values[ic][0], c.xy(5, r));
            b.add(bounds[ic][0], c.xy(7, r));
            b.add(values[ic][1], c.xy(9, r));
            b.add(bounds[ic][1], c.xy(11, r));
            b.add(values[ic][2], c.xy(13, r));
        }

        // Last row for global result
        r += 2;

        JLabel globalLabel = new JLabel("Grade [0 .. " + Grades.intrinsicRatio + "]");
        globalLabel.setToolTipText("Global check result");
        b.add(globalLabel, c.xyw(5, r, 3));
        b.add(globalField, c.xy(9, r));
    }

    //-------------------//
    // createBoundFields //
    //-------------------//
    private void createBoundFields ()
    {
        // Allocate bound fields (2 per check)
        final int checkNb = suite.getChecks().size();
        bounds = new JTextField[checkNb][];

        for (int ic = 0; ic < checkNb; ic++) {
            Check<C> check = suite.getChecks().get(ic);
            bounds[ic] = new JTextField[2];

            for (int i = 0; i <= 1; i++) {
                JTextField field = new JTextField(FIELD_WIDTH);
                field.setHorizontalAlignment(JTextField.CENTER);
                bounds[ic][i] = field;

                NamedDouble constant = (i == 0) ? check.getLowDouble() : check.getHighDouble();

                field.setText(textOf(constant.getValue()));
                field.setToolTipText(
                        "<html>" + constant.getName() + "<br/>" + constant.getDescription()
                        + "</html>");
            }
        }
    }

    //--------------//
    // createLayout //
    //--------------//
    private FormLayout createLayout (int checkNb)
    {
        // Build proper column specification
        StringBuilder sbc = new StringBuilder();
        sbc.append("center:pref").append(", ").append(COLUMN_GAP).append(", "); // Covariance
        sbc.append(" right:40dlu").append(", ").append(COLUMN_GAP).append(", "); // Name
        sbc.append(" right:pref").append(", ").append(COLUMN_GAP).append(", ");
        sbc.append(" right:pref").append(", ").append(COLUMN_GAP).append(", "); // Low limit
        sbc.append(" right:pref").append(", ").append(COLUMN_GAP).append(", ");
        sbc.append(" right:pref").append(", ").append(COLUMN_GAP).append(", "); // High Limit
        sbc.append(" right:pref");

        sbc.append(", right:pref");
        sbc.append(", right:pref");

        logger.debug("sb cols={}", sbc);

        // Create proper form layout
        return new FormLayout(
                sbc.toString(), //cols
                Panel.makeRows(checkNb, LINE_GAP)); //rows
    }

    //-------------------//
    // createValueFields //
    //-------------------//
    private void createValueFields ()
    {
        // Allocate value fields (3 per check)
        final int checkNb = suite.getChecks().size();
        values = new JTextField[checkNb][3];

        for (int n = 0; n < checkNb; n++) {
            for (int i = 0; i <= 2; i++) {
                JTextField field = new JTextField(FIELD_WIDTH);
                field.setEditable(false);
                field.setHorizontalAlignment(JTextField.CENTER);
                values[n][i] = field;
            }
        }
    }

    //-------------//
    // resetValues //
    //-------------//
    private void resetValues ()
    {
        for (JTextField[] seq : values) {
            for (JTextField field : seq) {
                field.setText("");
            }
        }
    }

    //--------//
    // textOf //
    //--------//
    private String textOf (double val)
    {
        return String.format(Locale.US, "%5.2f", val);
    }

    //---------//
    // valueOf //
    //---------//
    private double valueOf (String text)
    {
        Scanner scanner = new Scanner(text);
        scanner.useLocale(Locale.US);

        while (scanner.hasNext()) {
            if (scanner.hasNextDouble()) {
                return scanner.nextDouble();
            } else {
                scanner.next();
            }
        }

        // Kludge!
        return Double.NaN;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {

        /**
         * Method run whenever user presses Return/Enter in one of
         * the parameter fields
         */
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Any & several bounds may have been modified by the user
            // Since the same constant can be used in several fields, we have to
            // take a snapshot of all constants values, before modifying any one
            Map<NamedDouble, Double> values = new HashMap<>();

            for (Check<C> check : suite.getChecks()) {
                values.put(check.getLowDouble(), check.getLowDouble().getValue());
                values.put(check.getHighDouble(), check.getHighDouble().getValue());
            }

            boolean modified = false;
            int ic = -1;

            for (Check<C> check : suite.getChecks()) {
                ic++;

                // Check the bounds wrt the corresponding fields
                for (int i = 0; i < 2; i++) {
                    final NamedDouble constant = (i == 0) ? check.getLowDouble()
                            : check.getHighDouble();

                    // Simplistic test to detect modification
                    final JTextField field = bounds[ic][i];
                    final String oldString = textOf(values.get(constant)).trim();
                    final String newString = field.getText().trim();

                    if (!oldString.equals(newString)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Check '").append(check.getName()).append("':");

                        if (i == 0) {
                            sb.append(" Low");
                        } else {
                            sb.append(" High");
                        }

                        sb.append(" bound '").append(constant.getName()).append("'");

                        final String context = sb.toString();

                        // Actually convert the value and update the constant
                        try {
                            constant.setValue(valueOf(newString));
                            modified = true;
                            sb.append(" modified from ").append(oldString).append(" to ")
                                    .append(newString);
                            logger.info(sb.toString());
                        } catch (Exception ex) {
                            logger.warn("Error in {}, {}", context, ex.getLocalizedMessage());
                        }
                    }
                }
            }

            // If at least one modification has been made, update the whole
            // table with both suite parameters and object results
            if (modified) {
                setSuite(suite);
                passForm(checkable);
            }
        }
    }
}
