//----------------------------------------------------------------------------//
//                                                                            //
//                        S t d O u t E r r L e v e l                         //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.log;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.logging.Level;

/**
 * Class {@code StdOutErrorLevel} defines 2 new Logging levels, one for STDOUT,
 * one for STDERR, used when multiplexing STDOUT and STDERR into the same
 * rolling log file via the Java Logging APIs.
 * @since 2.2
 *
 * <br/>
 * <a href="http://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and">See blog of Nick Stephen</a>
 * 
 * @author Nick Stephen
 *
 */
public class StdOutErrLevel
    extends Level
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * Level for STDOUT activity.
     */
    public static Level STDOUT = new StdOutErrLevel(
        "STDOUT",
        Level.INFO.intValue() + 53);

    /**
     * Level for STDERR activity
     */
    public static Level STDERR = new StdOutErrLevel(
        "STDERR",
        Level.INFO.intValue() + 54);

    //~ Constructors -----------------------------------------------------------

    /**
     * private constructor
     * @param name name used in toString
     * @param value integer value, should correspond to something reasonable in default Level class
     */
    private StdOutErrLevel (String name,
                            int    value)
    {
        super(name, value);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Method to avoid creating duplicate instances when deserializing the
     * object.
     * @return the singleton instance of this {@code Level} value in this
     * classloader
     * @throws ObjectStreamException If unable to deserialize
     */
    protected Object readResolve ()
        throws ObjectStreamException
    {
        if (this.intValue() == STDOUT.intValue()) {
            return STDOUT;
        }

        if (this.intValue() == STDERR.intValue()) {
            return STDERR;
        }

        throw new InvalidObjectException("Unknown instance :" + this);
    }
}
