//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c h e m a A n n o t a t o r                                 //
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
package org.audiveris.schema;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.codehaus.mojo.jaxb2.schemageneration.AbstractXsdGeneratorMojo;
import org.codehaus.mojo.jaxb2.schemageneration.SchemaGenerationMojo;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocRenderer;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.NoAuthorJavaDocRenderer;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SearchableDocumentation;
import org.codehaus.mojo.jaxb2.shared.FileSystemUtilities;
import org.codehaus.mojo.jaxb2.shared.filters.Filter;
import org.codehaus.mojo.jaxb2.shared.filters.pattern.FileFilterAdapter;
import org.codehaus.mojo.jaxb2.shared.filters.pattern.PatternFileFilter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocData;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SortableLocation;

/**
 * Class {@code SchemaAnnotator} annotates a plain XSD schema with JavaDoc content.
 * <p>
 * Code is a very simplified version of {@link SchemaGenerationMojo} since the plain 'vanilla'
 * XSD files already exist, generated from Java sources.
 * Its role is thus limited to the injection of JavaDoc content directly retrieved from the same
 * Java sources.
 * <p>
 * It uses an enhanced JavaDocExtractor, since the original one did not process inner classes.
 * <p>
 * NOTA: Unfortunately, it can only process XSD files whose names match "schemaN.xsd".
 * This is due to final {@link AbstractXsdGeneratorMojo#SCHEMAGEN_EMITTED_FILENAME} pattern.
 *
 * @author Hervé Bitteur
 */
public class SchemaAnnotator
        extends AbstractXsdGeneratorMojo
{

    //~ Static fields/initializers -----------------------------------------------------------------
    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * The directory where the plain XSD files are read.
     * From this directory the XSDs will be copied to the outputDirectory for further processing.
     *
     * @see #outputDirectory
     */
    // "${project.build.directory}/schemagen-work/compile_scope"
    private final File workDirectory;

    /**
     * The directory where the annotated XSD file(s) will be
     * placed, after all transformations are done.
     */
    private final File outputDirectory;

    /**
     * List of paths to files and/or directories which should be recursively searched
     * for Java source files.
     */
    private final List<File> sources;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the {@code SchemaAnnotator} instance with its parameters.
     *
     * @param workDirectory   directory where plain XSD files are read
     * @param outputDirectory directory where annotated XSD files are written
     * @param sources         list of files/directories to be recursively searched for Java sources
     */
    public SchemaAnnotator (File workDirectory,
                            File outputDirectory,
                            List<File> sources)
    {
        this.workDirectory = workDirectory;
        this.outputDirectory = outputDirectory;
        this.sources = sources;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected File getWorkDirectory ()
    {
        return workDirectory;
    }

    @Override
    protected List<URL> getCompiledClassNames ()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<URL> getSources ()
    {
        return null;
    }

    @Override
    protected void addResource (Resource resource)
    {
    }

    @Override
    protected File getOutputDirectory ()
    {
        return outputDirectory;
    }

    @Override
    protected List<String> getClasspath ()
            throws MojoExecutionException
    {
        return Collections.emptyList();
    }

    @Override
    protected String getStaleFileName ()
    {
        return SchemaGenerationMojo.STALE_FILENAME;
    }

    //---------//
    // process //
    //---------//
    public final void process ()
            throws MojoExecutionException,
                   MojoFailureException
    {
        try {
            // Copy plain XSDs files from the WorkDirectory to the OutputDirectory.
            final List<Filter<File>> exclusionFilters = PatternFileFilter
                    .createIncludeFilterList(getLog(), "\\.class");

            final List<File> toCopy = FileSystemUtilities.resolveRecursively(
                    Arrays.asList(getWorkDirectory()),
                    exclusionFilters, getLog());

            for (File current : toCopy) {
                // Get the path to the current file
                final String currentPath = FileSystemUtilities.getCanonicalPath(current
                        .getAbsoluteFile());
                final File target = new File(getOutputDirectory(),
                                             FileSystemUtilities.relativize(currentPath,
                                                                            getWorkDirectory(),
                                                                            true));

                // Copy the file to the same relative structure within the output directory.
                FileSystemUtilities.createDirectory(target.getParentFile(), false);
                FileUtils.copyFile(current, target);
            }

            //
            // The XSD post-processing should be applied in the following order:
            //
            // 1. [XsdAnnotationProcessor]:            Inject JavaDoc annotations for Classes.
            // 2. [XsdEnumerationAnnotationProcessor]: Inject JavaDoc annotations for Enums.
            // 3. [ChangeNamespacePrefixProcessor]:    Change namespace prefixes within XSDs.
            // 4. [ChangeFilenameProcessor]:           Change the fileNames of XSDs.
            //
//                    // Map the XML Namespaces to their respective XML URIs (and reverse)
//                    // The keys are the generated 'vanilla' XSD file names.
//            Map<String, SimpleNamespaceResolver> resolverMap = null;
//                    final Map<String, SimpleNamespaceResolver> resolverMap = XsdGeneratorHelper
//                            .getFileNameToResolverMap(getOutputDirectory());
            getLog().info("XSD post-processing: Adding JavaDoc annotations in generated XSDs.");

            // Retrieve all .java files under 'sources' directories
            final Filter<File> acceptJavaFilter = new FileFilterAdapter(
                    f -> f.isDirectory() || f.getName().endsWith(".java"));
            acceptJavaFilter.initialize(getLog());
            final List<File> javaFiles = FileSystemUtilities.filterFiles(
                    sources, acceptJavaFilter, getLog());
            getLog().info("Java files: " + javaFiles.size());

            // Acquire JavaDocs, using the MyJavaDocExtractor
            final MyJavaDocExtractor extractor = new MyJavaDocExtractor(getLog())
                    .addSourceFiles(javaFiles);
            final SearchableDocumentation javaDocs = extractor.process();
            ///dumpDocs(javaDocs);

            // Modify the 'vanilla' generated XSDs by inserting the JavaDoc as annotations
            final JavaDocRenderer renderer = new NoAuthorJavaDocRenderer();
            final int numProcessedFiles = MyXsdGeneratorHelper.insertJavaDocAsAnnotations(
                    getLog(),
                    getEncoding(false),
                    getOutputDirectory(),
                    javaDocs,
                    renderer);

            ///if (getLog().isDebugEnabled()) {
            getLog().info("XSD post-processing: " + numProcessedFiles
                                  + " files processed.");
            ///}
        } catch (MojoExecutionException e) {
            throw e;
        } catch (IOException |
                 IllegalArgumentException e) {

            // Find the root exception, and print its stack trace to the Maven Log.
            // These invocation target exceptions tend to produce really deep stack traces,
            // hiding the actual root cause of the exception.
            Throwable current = e;
            while (current.getCause() != null) {
                current = current.getCause();
            }

            getLog().error("Execution failed.");

            //
            // Print a stack trace
            //
            StringBuilder rootCauseBuilder = new StringBuilder();
            rootCauseBuilder.append("\n");
            rootCauseBuilder.append("[Exception]: ").append(current.getClass().getName()).append(
                    "\n");
            rootCauseBuilder.append("[Message]: ").append(current.getMessage()).append("\n");
            for (StackTraceElement el : current.getStackTrace()) {
                rootCauseBuilder.append("         ").append(el.toString()).append("\n");
            }
            getLog().error(rootCauseBuilder.toString());
        }
    }

    // HB
    public void dumpDocs (SearchableDocumentation docs)
    {
        SortedMap<SortableLocation, JavaDocData> map = docs.getAll();

        for (Entry<SortableLocation, JavaDocData> entry : map.entrySet()) {
            getLog().info(entry.getKey() + " => " + entry.getValue());
        }
    }

    //------//
    // main //
    //------//
    /**
     * Main entry point for the annotator.
     *
     * @param args the command line arguments
     *             - arg1: work directory
     *             - arg2: output directory
     *             - following args: files/directories containing Java sources
     */
    public static void main (String[] args)
    {
        if ((args == null) || args.length < 3) {
            System.err.println("Error in SchemaAnnotator, expecting 3 arguments at least");
            throw new RuntimeException("Incorrect arguments to SchemaAnnotator");
        }

        try {
            final File work = new File(args[0]);
            final File output = new File(args[1]);
            final List<File> sources = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                sources.add(new File(args[i]));
            }

            final SchemaAnnotator sa = new SchemaAnnotator(work, output, sources);
            sa.process();
        } catch (Exception ex) {
            System.err.println("Error in schema annotator: " + ex);
        }
    }
}
