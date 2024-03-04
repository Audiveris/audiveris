//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B i n a r i z a t i o n B o a r d                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.AdaptiveFilter.AdaptiveContext;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.image.PixelFilter;
import org.audiveris.omr.image.RandomFilter;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.ButtonTabComponent;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.awt.event.ItemEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;

/**
 * Class <code>BinarizationBoard</code> is a board meant to display the
 * context of binarization for a given pixel location.
 *
 * @author Hervé Bitteur
 */
public class BinarizationAdjustBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BinarizationAdjustBoard.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]
    { LocationEvent.class };

    /** Format used for every double field. */
    private static final String format = "%.2f";

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    private final Sheet sheet;

    /** Mean level in neighborhood. */
    private final LDoubleField mean = new LDoubleField(false, "MYMEAN!", "Mean value", format);

    /** Standard deviation in neighborhood. */
    private final LDoubleField stdDev = new LDoubleField(
            false,
            "HELLO!",
            "Standard deviation value",
            format);

    /** Computed threshold. */
    private final LDoubleField threshold = new LDoubleField(false, "Thres.", "Threshold", format);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BinarizationBoard object.
     *
     * @param sheet related sheet
     */
    public BinarizationAdjustBoard (Sheet sheet)
    {
        super(
                Board.BINARIZATIONADJUST,
                sheet.getLocationService(),
                eventClasses,
                false,
                false,
                false,
                false);

        this.sheet = sheet;

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout layout = Panel.makeFormLayout(5, 3);
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        LIntegerField globalFilterValue = new LIntegerField("Threshold", "Gray threshold for pixels");

        JCheckBox checkbox = new JCheckBox("Show original gray image: ");
        checkbox.addItemListener((e) -> {
            
            ByteProcessor source;
            Picture picture = sheet.getPicture();
            System.out.println("Selected: " + e.getStateChange());

            if (e.getStateChange() == ItemEvent.SELECTED) {
                source = picture.getSource(SourceKey.GRAY);
            } else {
                source = picture.getSource(SourceKey.BINARY);
            }

            RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
            RunTable wholeVertTable = vertFactory.createTable(source);
            picture.setTable(Picture.TableKey.BINARY, wholeVertTable, false);

        });
        
        JButton button = new JButton("Apply");
        button.addActionListener((e) -> {
            int value = globalFilterValue.getValue();
            System.out.println(value);
            sheet.getStub().getBinarizationFilterParam().setSpecific(
                new GlobalDescriptor(value)
            );
            try {
                OmrStep.BINARY.doit(sheet);
            } catch (StepException e1) {
                System.out.println("step exception thrown");
            }
        });

        builder.add(checkbox, cst.xy(1, 1));

        builder.add(globalFilterValue.getLabel(), cst.xy(1, 3));
        builder.add(globalFilterValue.getField(), cst.xy(3, 3));

        builder.add(button, cst.xy(1, 5));



        



    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Interest in LocationEvent
     *
     * @param sheetLocation location
     */
    protected void handleLocationEvent (LocationEvent sheetLocation)
    {
        // Display rectangle attributes
        Rectangle rect = sheetLocation.getData();

        if (rect != null) {
            FilterDescriptor desc = sheet.getStub().getBinarizationFilter();
            ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.GRAY);

            if (source != null) {
                PixelFilter filter = desc.getFilter(source);

                if (filter == null) {
                    filter = new RandomFilter(
                            source,
                            AdaptiveDescriptor.getDefaultMeanCoeff(),
                            AdaptiveDescriptor.getDefaultStdDevCoeff());
                }

                PixelFilter.Context context = filter.getContext(rect.x, rect.y);

                if (context != null) {
                    if (context instanceof AdaptiveContext ctx) {
                        mean.setValue(ctx.mean);
                        stdDev.setValue(ctx.standardDeviation);
                    } else {
                        mean.setText("");
                        stdDev.setText("");
                    }

                    threshold.setValue(context.threshold);

                    return;
                }
            } else {
                logger.info("No GRAY source available");
            }
        }

        mean.setText("");
        stdDev.setText("");
        threshold.setText("");
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("BinarizationAdjustBoard: {}", event);

            if (event instanceof LocationEvent) {
                handleLocationEvent((LocationEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }
}
