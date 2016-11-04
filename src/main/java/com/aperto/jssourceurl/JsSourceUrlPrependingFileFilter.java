package com.aperto.jssourceurl;

import org.apache.maven.shared.filtering.AbstractMavenFilteringRequest;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.FileUtils.FilterWrapper;
import org.apache.maven.shared.utils.io.IOUtil;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.plexus.build.incremental.BuildContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * MavenFileFilter that prepends .js files with "//@sourceURL=..." annotation
 * (only when filtering is turned on, and when first line does not contain
 * "sourceURL").
 * @author joerg.frantzius
 *
 */
@Component(role = MavenFileFilter.class, hint = "default")
public class JsSourceUrlPrependingFileFilter extends DefaultMavenFileFilter {

    // unfortunately had to copy lots of code from super class because of private methods.
    // changes are marked as // CHANGED .. // CHANGED END

    @Requirement
    private MavenReaderFilter readerFilter;

    @Requirement
    private BuildContext buildContext;

    AbstractMavenFilteringRequest req;

    private void filterFile( @Nonnull File from, @Nonnull File to, @Nullable String encoding,
        @Nullable List<FilterWrapper> wrappers )
            throws IOException, MavenFilteringException
    {
        if (wrappers != null && wrappers.size() > 0) {
            Reader fileReader = null;
            Writer fileWriter = null;
            try {
                fileReader = getFileReader(encoding, from);
                fileWriter = getFileWriter(encoding, to);
                // CHANGED
                prependJsSourceUrl(from, to, fileWriter);
                // CHANGED END
                Reader src = readerFilter.filter(fileReader, true, wrappers);

                IOUtil.copy(src, fileWriter);
            } finally {
                IOUtil.close(fileReader);
                IOUtil.close(fileWriter);
            }
        } else {
            if (to.lastModified() < from.lastModified()) {
                FileUtils.copyFile(from, to);
            }
        }
    }

    // CHANGED (new method)
    protected void prependJsSourceUrl(File from, File to, Writer fileWriter) throws IOException {
        if (from.getName().endsWith(".js")) {
            List<String> allLines = Files.readAllLines(Paths.get(from.getAbsolutePath()));
            if (allLines.isEmpty() || (!allLines.isEmpty() && !allLines.get(0).contains("sourceURL"))) {
                File baseDir = req.getMavenSession().getCurrentProject().getBasedir();
                String relativePath = from.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
                String annotation = "//@ sourceURL=" + relativePath + "\n";
                getLogger().debug( "prepending sourceURL in target file " + to.getPath() + ": " + annotation);
                fileWriter.write(annotation);
            }
            //fileWriter.write("// test\n");
        }
    }
    // CHANGED END

    @Override
    public List<FilterWrapper> getDefaultFilterWrappers(AbstractMavenFilteringRequest req) throws MavenFilteringException {
        this.req = req;
        return super.getDefaultFilterWrappers(req);
    }


    /** {@inheritDoc} */
    public void copyFile( File from, File to, boolean filtering, List<FileUtils.FilterWrapper> filterWrappers,
        String encoding, boolean overwrite )
            throws MavenFilteringException
    {
        try {
            if (filtering) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("filtering " + from.getPath() + " to " + to.getPath());
                }
                filterFile(from, to, encoding, filterWrappers);
            } else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("copy " + from.getPath() + " to " + to.getPath());
                }
                FileUtils.copyFile(from, to, encoding, new FileUtils.FilterWrapper[0], overwrite);
            }

            buildContext.refresh(to);
        } catch (IOException e) {
            throw new MavenFilteringException(e.getMessage(), e);
        }

    }

    private Writer getFileWriter( String encoding, File to )
        throws IOException
    {
        if ( StringUtils.isEmpty( encoding ) )
        {
            return new FileWriter( to );
        }
        else
        {
            FileOutputStream outstream = new FileOutputStream( to );

            return new OutputStreamWriter( outstream, encoding );
        }
    }

    private Reader getFileReader( String encoding, File from )
        throws FileNotFoundException, UnsupportedEncodingException
    {
        // buffer so it isn't reading a byte at a time!
        if ( StringUtils.isEmpty( encoding ) )
        {
            return new BufferedReader( new FileReader( from ) );
        }
        else
        {
            FileInputStream instream = new FileInputStream( from );
            return new BufferedReader( new InputStreamReader( instream, encoding ) );
        }
    }


}
