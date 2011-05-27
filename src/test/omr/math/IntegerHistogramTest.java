/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.math;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.PrintStream;

/**
 *
 * @author Etiolles
 */
public class IntegerHistogramTest
{
    //~ Instance fields --------------------------------------------------------

    private IntegerHistogram histo;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new IntegerHistogramTest object.
     */
    public IntegerHistogramTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of print method, of class IntegerHistogram.
     */
    @Test
    public void testPrint ()
    {
        System.out.println("print");
        
        

        PrintStream      stream = System.out;
        IntegerHistogram instance = createHistogram();
        instance.print(stream);
    }

    private IntegerHistogram createHistogram ()
    {
        histo = new IntegerHistogram();
        histo.increaseCount(1,1250);
        histo.increaseCount(2,1400);
        histo.increaseCount(3,2000);
        histo.increaseCount(4,1950);
        histo.increaseCount(5,2125);
        histo.increaseCount(6,1800);
        histo.increaseCount(7,1800);
        histo.increaseCount(8,2500);
        histo.increaseCount(9,3000);
        histo.increaseCount(10,20000);
        histo.increaseCount(11,12000);
        histo.increaseCount(12,1100);
        histo.increaseCount(13,1300);
        histo.increaseCount(14,11000);
        histo.increaseCount(15,23800);
        histo.increaseCount(16,3000);
        histo.increaseCount(17,600);
        

        return histo;
    }
}
