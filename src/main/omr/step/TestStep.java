//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T e s t S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.WellKnowns;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.BracketInter;
import omr.sig.inter.Inter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import omr.sheet.SheetStub;

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
    public void doit (Collection<SystemInfo> systems,
                      Sheet aSheet)
            throws StepException
    {
        final Book book = aSheet.getBook();

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
        } catch (Exception ex) {
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
        //~ Static fields/initializers -------------------------------------------------------------

        public static final PrintWriter writer = getPrintWriter(getTestFile());
    }
}
