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
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.image.PixelFilter;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LRadioButton;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import ij.process.ByteProcessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;

/**
 * Class <code>BinarizationBoard</code> is a board meant to display the
 * context of binarization for a given pixel location.
 *
 * @author Hervé Bitteur
 */
public class BinarizationAdjustBoard
        extends Board
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BinarizationAdjustBoard.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]
    { LocationEvent.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    private final Sheet sheet;

    private final LIntegerField globalThresholdValue = new LIntegerField("Threshold", "Gray threshold for pixels");
    
    private final LDoubleField adaptiveMeanValue = new LDoubleField("Mean coeff", "Coefficient for mean value");
    
    private final LDoubleField adaptiveStdDevValue = new LDoubleField("Std Dev coeff", "Coefficient for standard deviation value");

    private final JButton applyButton = new JButton("Apply");

    private final LRadioButton globalFilterRadioButton = new LRadioButton("Use Global Filter", "Converts to black and white by a simply threshold value");

    private final LRadioButton adaptiveFilterRadioButton = new LRadioButton("Use Adaptive Filter", "Converts to black and white by calculating the mean and standard deviation value of a group of pixels");

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

        applyButton.addActionListener(this);

        adaptiveFilterRadioButton.getField().setActionCommand("showGray");
        adaptiveFilterRadioButton.addActionListener(this);
        adaptiveFilterRadioButton.getField().setSelected(true);

        globalFilterRadioButton.getField().setActionCommand("showBinary");
        globalFilterRadioButton.addActionListener(this);


        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout layout = Panel.makeFormLayout(4, 3);
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());
        // PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        // CellConstraints cst = new CellConstraints();
        
        ButtonGroup imageButtonGroup = new ButtonGroup();

        imageButtonGroup.add(adaptiveFilterRadioButton.getField());
        imageButtonGroup.add(globalFilterRadioButton.getField());

        int r = 1;

        builder.addRaw(adaptiveFilterRadioButton.getField()).xy(1, r);
        builder.addRaw(adaptiveFilterRadioButton.getLabel()).xy(3, r);

        r += 2;

        builder.addRaw(globalFilterRadioButton.getField()).xy(1, r);
        builder.addRaw(globalFilterRadioButton.getLabel()).xy(3, r);

        r += 2;

        builder.addRaw(globalThresholdValue.getLabel()).xy(1, r);
        builder.addRaw(globalThresholdValue.getField()).xy(3, r);

        builder.addRaw(adaptiveMeanValue.getLabel()).xy(5, r);
        builder.addRaw(adaptiveMeanValue.getField()).xy(7, r);

        builder.addRaw(adaptiveStdDevValue.getLabel()).xy(9, r);
        builder.addRaw(adaptiveStdDevValue.getField()).xy(11, r);

        r += 2;

        builder.addRaw(applyButton).xy(1, r);


    }



    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == applyButton) {

            Picture picture = sheet.getPicture();
            ByteProcessor source = picture.getSource(SourceKey.GRAY);

            FilterParam filterParam = new FilterParam(this);

            if (adaptiveFilterRadioButton.getField().isSelected()) {
                double mean = adaptiveMeanValue.getValue();
                double stdDev = adaptiveStdDevValue.getValue();
                filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));

            } else if (globalFilterRadioButton.getField().isSelected()) {
                int value = globalThresholdValue.getValue();
                filterParam.setSpecific(new GlobalDescriptor(value));
            }
                
            sheet.getStub().setBinarizationFilterParam(filterParam);

            PixelFilter filter = sheet.getStub().getBinarizationFilter().getFilter(source);
            ByteProcessor filteredImage = filter.filteredImage();
            
            RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
            RunTable wholeVertTable = vertFactory.createTable(filteredImage);
            picture.setTable(Picture.TableKey.BINARY, wholeVertTable, true);

        }

        // else if (e.getSource() == adaptiveFilterRadioButton.getField()) {




        //     Picture picture = sheet.getPicture();
        //     ByteProcessor source = picture.getSource(SourceKey.GRAY);

        //     System.out.println(source);
            
        //     RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
        //     RunTable wholeVertTable = vertFactory.createTable(source);
        //     picture.setTable(Picture.TableKey.BINARY, wholeVertTable, true);

        //     applyButton.setEnabled(false);

        // } else if (e.getSource() == globalFilterRadioButton.getField()) {

        //     Picture picture = sheet.getPicture();
        //     ByteProcessor source = picture.getSource(SourceKey.BINARY);
            
        //     RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
        //     RunTable wholeVertTable = vertFactory.createTable(source);
        //     picture.setTable(Picture.TableKey.BINARY, wholeVertTable, true);

        //     applyButton.setEnabled(true);
            
        // }

        sheet.getStub().getAssembly().getCurrentView().getComponent().repaint();

    }




    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        // try {
        //     // Ignore RELEASING
        //     if (event.movement == MouseMovement.RELEASING) {
        //         return;
        //     }

        //     logger.debug("BinarizationAdjustBoard: {}", event);

        //     if (event instanceof LocationEvent) {
        //         handleLocationEvent((LocationEvent) event);
        //     }
        // } catch (Exception ex) {
        //     logger.warn(getClass().getName() + " onEvent error", ex);
        // }
    }


}
