package org.opentripplanner.standalone.datastore.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.datastore.FileType.OTP_STATUS;
import static org.opentripplanner.standalone.datastore.FileType.REPORT;

public class DirectoryDataSourceTest {
    private static final String DIRNAME = "the_wall";
    private static final String UTF_8 = "UTF-8";
    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        tempDir.delete();
    }

    @Test
    public void testAccessorsForNoneExistingDirectory() throws IOException {
        File target = new File(tempDir, DIRNAME);
        File copyTarget = new File(tempDir, DIRNAME);
        CompositeDataSource subject = new DirectoryDataSource(target, REPORT);
        CompositeDataSource copySubject = new DirectoryDataSource(copyTarget, REPORT);
        String expectedPath = new File(tempDir, DIRNAME).getPath();

        assertTrue(subject.content().toString(), subject.content().isEmpty());
        assertEquals(DIRNAME, subject.name());
        assertEquals(expectedPath, subject.path());
        assertEquals(REPORT, subject.type());
        assertEquals(0L, subject.lastModified());
        assertEquals(0L, subject.size());
        assertFalse(subject.exists());
        assertTrue(subject.isWritable());

        assertEquals("REPORT " + expectedPath, subject.toString());
        assertEquals(copySubject, subject);
        assertEquals(copySubject.hashCode(), subject.hashCode());

        subject.close();

    }

    @Test
    public void testIO() throws IOException {
        // Given a file, yet not created
        File target = new File(tempDir, DIRNAME);
        File child = new File(target, "a.txt");

        // And then CREATE a file data source - our subject
        CompositeDataSource subject = new DirectoryDataSource(target, REPORT);

        // Verify content is empty
        assertEquals("[]", toString(subject.content()));

        // Write something to a file in the directory
        IOUtils.write("Go, go, go!", subject.entry(child.getName()).asOutputStream(), UTF_8);


        // Verify the subject directory is created and still is writable
        assertTrue(subject.exists());
        assertTrue(subject.isWritable());

        // Verify content is updated
        assertEquals("[a.txt]", toString(subject.content()));

        // Assert content can be read using the subject and file
        assertEquals("Go, go, go!", FileUtils.readFileToString(child, UTF_8));

        // Then delete subject with all content
        subject.delete();

        // Assert dir is still writable, none existing and content is gone
        assertTrue(subject.isWritable());
        assertFalse(subject.exists());
        assertFalse(target.exists());
        assertEquals("[]", toString(subject.content()));

        subject.close();
    }

    @Test
    public void testMoveEntry() throws IOException {
        // Given:
        String source = "a.inProgress";
        String target = "a.ok";

        CompositeDataSource subject = new DirectoryDataSource(tempDir, OTP_STATUS);
        IOUtils.write("OK", subject.entry(source).asOutputStream(), UTF_8);

        // Assert source exist and target does not (before it is moved)
        assertFalse(new File(tempDir, target).exists());
        assertTrue(new File(tempDir, source).exists());

        // When:
        subject.rename(source, target);

        // Then assert target exist and source does not (after it is moved)
        assertTrue(new File(tempDir, target).exists());
        assertFalse(new File(tempDir, source).exists());
    }

    @Test
    public void testDeleteEntry() throws IOException {
        // Given a file:
        String filename = "a.txt";
        File file = new File(tempDir, filename);
        FileUtils.write(file, "OK", UTF_8);

        CompositeDataSource subject = new DirectoryDataSource(tempDir, OTP_STATUS);

        // Then assert file exist
        assertTrue(file.exists());

        // When:
        subject.delete(filename);

        // Then assert target do NOT exist after delete
        assertFalse(file.exists());
    }

    private String toString(Collection<DataSource> sources) {
        return sources.stream().map(DataSource::name).collect(Collectors.toList()).toString();
    }
}