//----------------------------------------------------------------------------//
//                                                                            //
//                                D u m p e r                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Class <code>Dumper</code> is a debugging utility that reports, in a brute
 * force manner, any internal data of a class instance.
 *
 * <p> When used on a class instance, all class internal fields which are
 * considered as "relevant" are printed using their toString() method, then we
 * walk up the inheritance tree and repeat the same actions, until there is no
 * more superclass or until the superclass we have reached is considered as
 * non-relevant. </p>
 *
 * <p> A (super)class is considered "relevant" if the static method
 * <code>isClassRelevant(class)</code> returns true. This method can be
 * overridden in a subclass of Dumper to adapt to local needs. </p>
 *
 * <p> A field is considered "relevant" if the following condition if the method
 * <code>isFieldRelevant(field)</code> returns true. Similarly, the behavior of
 * this predicate can be customized by subclassing the Dumper class. </p>
 *
 * <p> There are several kinds of print outs available through subclassing. Each
 * of them export two public methods: <code>dump()</code> which prints the
 * result on default output stream, and <code>dumpOf()</code> which simply
 * returns the generated dump string.
 *
 * <ul> <li> <b>Column</b> a dump with one line per field </li>
 *
 * <li> <b>Row</b> a dump with all information on one row </li>
 *
 * <li> <b>Html</b> an Html stream with fields arranged in tables </li>
 *
 * </ul>
 *
 * Here are some examples of use:
 * <pre>
 * // Using the predefined static helper methods
 * Dumper.dump(myinstance);
 * Dumper.dump(myinstance, "My Title");
 * Dumper.dump(myinstance, "My Title", 2);
 * System.out.println(Dumper.dumpOf(myinstance));
 * System.out.println(Dumper.htmlDumpOf(myinstance));
 *
 *  // Using directly the Dumper subclasses
 * new Dumper.Column(myinstance).print();
 * System.out.println(new Dumper.Row(myinstance).toString());
 * display(new Dumper.Html(myinstance).toString());
 * </pre>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Dumper
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The object to be dumped
     */
    protected final Object obj;

    /**
     * The string buffer used as output
     */
    protected final StringBuffer sb;

    /**
     * Class (beware, this variable is updated as we walk up the inheritance
     * tree)
     */
    protected Class cl;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Dumper.
     *
     * @param obj the object instance to be dumped.
     */
    private Dumper (Object obj)
    {
        // (re)Allocate the string buffer
        sb = new StringBuffer(1024);

        // Cache the object & the related class
        this.obj = obj;
        cl = obj.getClass();
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // dump //
    //------//
    /**
     * Helper function that prints the internal data of an object onto the
     * standard output.
     *
     * @param obj the instance to dump
     */
    public static void dump (Object obj)
    {
        dump(obj, null, 0);
    }

    //------//
    // dump //
    //------//
    /**
     * Helper function that prints the internal data of an object onto the
     * standard output, with a specified left indentation level.
     *
     * @param obj   the instance to dump
     * @param level the indentation level (0 means no indentation)
     */
    public static void dump (Object obj,
                             int    level)
    {
        dump(obj, null, level);
    }

    //------//
    // dump //
    //------//
    /**
     * Helper function that prints the internal data of an object onto the
     * standard output, with the ability to print a related title
     *
     * @param obj   the object to dump
     * @param title the title to print beforehand
     */
    public static void dump (Object obj,
                             String title)
    {
        dump(obj, title, 0);
    }

    //------//
    // dump //
    //------//
    /**
     * Helper function that prints the internal data of an object onto the
     * standard output, with room for a title and left indentation.
     *
     * @param obj   the object to dump
     * @param title the title to print beforehand
     * @param level the indentation level (0 for no indent)
     */
    public static void dump (Object obj,
                             String title,
                             int    level)
    {
        new Column(obj, title, level).print();
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Helper function that returns a line which contains the whole set of
     * internal data
     *
     * @param obj the object whose data is to be printed
     *
     * @return the string of data values
     */
    public static String dumpOf (Object obj)
    {
        return new Row(obj).toString();
    }

    //------------//
    // htmlDumpOf //
    //------------//
    /**
     * Helper function that prints a special kind of information string, using
     * HTML tags so that an html editor can easily render this.
     *
     * @param obj the object to dump
     *
     * @return the HTML string
     */
    public static String htmlDumpOf (Object obj)
    {
        return new Html(obj).toString();
    }

    //-------//
    // print //
    //-------//
    /**
     * Print the dump string onto the standard output
     */
    public void print ()
    {
        System.out.println(toString());
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return the string buffer content
     *
     * @return the dump of the object as a string
     */
    @Override
    public String toString ()
    {
        // Do the processing
        processObject();

        // Return the final content of string buffer
        return sb.toString();
    }

    //-----------------//
    // isClassRelevant //
    //-----------------//
    /**
     * Predicate to determine if a given class is worth being printed. This
     * method could be overridden to reflect customized policy. Note that when
     * walking up the inheritance tree, the browsing is stopped as soon as a
     * non-relevant class is encountered.
     *
     * @param cl the class at stake
     *
     * @return true if found relevant
     */
    protected boolean isClassRelevant (Class cl)
    {
        return (cl != null) && !cl.getName()
                                  .startsWith("java.");
    }

    //-----------------//
    // isFieldRelevant //
    //-----------------//
    /**
     * Predicate to determine if a given field is worth being printed. This
     * method could be overridden to reflect customized policy.
     *
     * @param field the field at stake
     *
     * @return true if found relevant
     */
    protected boolean isFieldRelevant (Field field)
    {
        // We don't print static field since the Dumper is meant for instances
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }

        // We don't print non-user visible entities
        if (field.getName()
                 .indexOf('$') != -1) {
            return false;
        }

        return true;
    }

    //------------------//
    // printClassEpilog //
    //------------------//
    /**
     * To be overridden so as to print the epilog of class data
     */
    protected void printClassEpilog ()
    {
    }

    //------------------//
    // printClassProlog //
    //------------------//
    /**
     * To be overridden so as to print the prolog of class data
     */
    protected void printClassProlog ()
    {
    }

    //------------//
    // printField //
    //------------//
    /**
     * Basic printing of field name and value. The method can of course be
     * overridden.
     *
     * @param name  the field name
     * @param value the field value, which may be null
     */
    protected void printField (String name,
                               Object value)
    {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.toString());
        }
    }

    //--------------//
    // processClass //
    //--------------//
    private void processClass ()
    {
        // Class Prolog
        printClassProlog();

        // Process the class Fields
        for (Field field : cl.getDeclaredFields()) {
            processField(field);
        }

        // Class Epilog
        printClassEpilog();
    }

    //--------------//
    // processField //
    //--------------//
    private void processField (Field field)
    {
        // Check that we are really interested in printing this field out
        if (isFieldRelevant(field)) {
            // Override any access limitation
            field.setAccessible(true);

            try {
                // Retrieve field value in the object instance
                Object value = field.get(obj);

                // Print the field value as requested
                printField(field.getName(), value);
            } catch (IllegalAccessException ex) {
                // Cannot occur in fact, thanks to setAccessible
            }
        }
    }

    //---------------//
    // processObject //
    //---------------//
    private void processObject ()
    {
        do {
            // Process the class at hand
            processClass();

            // Walk up the inheritance tree
            cl = cl.getSuperclass();
        } while (isClassRelevant(cl));
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Column //
    //--------//
    /**
     * Class <code>Column</code> implements a Dumper where all fields are
     * presented in one column, each field on a separate line. The column can be
     * left indented, according to the specified indentation level.
     */
    public static class Column
        extends Dumper
    {
        private static final String MEMBER_GAP = "   ";
        private static final String INDENT_GAP = ".  ";
        private final String        title;
        private final StringBuffer  prefix;

        public Column (Object obj,
                       String title,
                       int    level)
        {
            super(obj);

            // Cache the title
            if (title != null) {
                this.title = title;
            } else {
                this.title = "";
            }

            // Prepare indent prefix
            prefix = new StringBuffer(level * INDENT_GAP.length());

            for (int i = level; i > 0; i--) {
                prefix.append(INDENT_GAP);
            }
        }

        @Override
        protected void printClassProlog ()
        {
            // We print the class name only for the lowest class in
            // heritance hierarchy
            if (obj.getClass() == cl) {
                sb.append("\n");
                sb.append(prefix)
                  .append(cl.getName());
                sb.append(" ")
                  .append(title)
                  .append(":");
            }
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            sb.append("\n");
            sb.append(prefix)
              .append(MEMBER_GAP);
            sb.append(name)
              .append("=");
            super.printField(name, value);
        }
    }

    //------//
    // Html //
    //------//
    /**
     * Class <code>Html</code> implements a Dumper using HTML tags to present
     * fields in a table.
     */
    public static class Html
        extends Dumper
    {
        protected Html (Object obj)
        {
            super(obj);
        }

        @Override
        protected void printClassEpilog ()
        {
            // End table of fields
            sb.append("</table>");
        }

        @Override
        protected void printClassProlog ()
        {
            // Class name
            sb.append("<H3>Attributes of this " + cl.getName() + "</H3>");

            // Start table of fields
            sb.append("<table border=1>");
            sb.append("<tr><th>Name</th><th>Value</th></tr>");
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            // One table row per field
            sb.append("<tr>");

            // First the field name
            sb.append("<td>")
              .append(name)
              .append("</td>");

            // Then the field value
            sb.append("<td>");
            super.printField(name, value);

            sb.append("</td>");
            sb.append("</tr>");
        }
    }

    //-----//
    // Row //
    //-----//
    /**
     * Class <code>Row</code> implements a Dumper where all fields are presented
     * on the same line.
     */
    public static class Row
        extends Dumper
    {
        protected Row (Object obj)
        {
            super(obj);
        }

        protected void printClassEpilog ()
        {
            sb.append("}");
        }

        protected void printClassProlog ()
        {
            // Class name
            sb.append("{");

            // Special annotation for superclass
            if (obj.getClass() != cl) {
                sb.append("from ");
            }

            sb.append(cl.getName())
              .append(":");
        }

        protected void printField (String name,
                                   Object value)
        {
            sb.append(" ");
            sb.append(name)
              .append("=");
            super.printField(name, value);
        }
    }
}
