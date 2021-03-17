//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T e s t S t e p                                        //
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
package org.audiveris.omr.step;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.Inter;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Class {@code TestStep} is an attempt to add a pseudo step for specific tests.
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class TestStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code TestStep} object.
     */
    public TestStep ()
    {
        //        super(
        //            Steps.TEST,
        //            DATA_TAB,
        //            "Placeholder for specific tests");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void doit (Sheet aSheet)
            throws StepException
    {
        final Book book = aSheet.getStub().getBook();

        for (SheetStub stub : book.getStubs()) {
            boolean sheetStarted = false;

            for (SystemInfo system : stub.getSheet().getSystems()) {
                boolean systemStarted = false;
                SIGraph sig = system.getSig();
                List<Inter> brackets = sig.inters(BracketInter.class);

                if (!brackets.isEmpty()) {
                    for (Inter inter : brackets) {
                        if (!sheetStarted) {
                            Holder.writer.println("Sheet " + stub.getId());
                            sheetStarted = true;
                        }

                        if (!systemStarted) {
                            Holder.writer.println("  System#" + system.getId());
                            systemStarted = true;
                        }

                        BracketInter bracket = (BracketInter) inter;
                        Holder.writer.format("    %s%n", getInfo(bracket));
                    }
                }
            }

            if (sheetStarted) {
                // Flush sheet printed material
                Holder.writer.println();
                Holder.writer.flush();
            }
        }
    }

    //---------//
    // getInfo //
    //---------//
    private String getInfo (Inter inter)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(inter);
        sb.append(" ").append(inter.getBounds());

        if (!inter.getDetails().isEmpty()) {
            sb.append(" ").append(inter.getDetails());
        }

        return sb.toString();
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (Path path)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(path.toFile()),
                            WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (FileNotFoundException |
                 UnsupportedEncodingException ex) {
            System.err.println("Error creating " + path + ex);

            return null;
        }
    }

    //-------------//
    // getTestFile //
    //-------------//
    private static Path getTestFile ()
    {
        String stamp = getTimeStamp();

        return WellKnowns.TEMP_FOLDER.resolve("TestFile " + stamp + ".txt");
    }

    //--------------//
    // getTimeStamp //
    //--------------//
    private static String getTimeStamp ()
    {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

        return formatter.format(now);
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //--------//
    // Holder //
    //--------//
    private static interface Holder
    {

        public static final PrintWriter writer = getPrintWriter(getTestFile());
    }
}
