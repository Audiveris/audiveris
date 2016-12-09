package omr.math;

import org.junit.Test;

import java.io.PrintStream;

/**
 *
 * @author Etiolles
 */
public class IntegerHistogramTest
{
    //~ Instance fields ----------------------------------------------------------------------------

    private IntegerPeakFunction histo;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new IntegerHistogramTest object.
     */
    public IntegerHistogramTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Test of print method, of class IntegerPeakFunction.
     */
    @Test
    public void testPrint ()
    {
        System.out.println("print");

        PrintStream stream = System.out;
        IntegerPeakFunction instance = createHistogram();
        instance.print(stream);
    }

    private IntegerPeakFunction createHistogram ()
    {
        histo = new IntegerPeakFunction("test", 0, 20, 0.05, 0.10, 0.05);
        histo.setValue(1, 1250);
        histo.setValue(2, 1400);
        histo.setValue(3, 2000);
        histo.setValue(4, 1950);
        histo.setValue(5, 2125);
        histo.setValue(6, 1800);
        histo.setValue(7, 1800);
        histo.setValue(8, 2500);
        histo.setValue(9, 3000);
        histo.setValue(10, 20000);
        histo.setValue(11, 12000);
        histo.setValue(12, 1100);
        histo.setValue(13, 1300);
        histo.setValue(14, 11000);
        histo.setValue(15, 23800);
        histo.setValue(16, 3000);
        histo.setValue(17, 600);

        return histo;
    }
}
