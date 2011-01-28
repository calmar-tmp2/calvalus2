package com.bc.calvalus.processing.beam;

import com.bc.calvalus.processing.shellexec.ExecutablesTool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test for {@link BeamOperatorTool}.
 */
public class BeamOperatorToolTest {
    private static final String REQUEST = "calvalus-processing/src/test/resources/beam-l2-sample-request.xml";
    private static final String OUTPUT_DIR = "file:///tmp/meris-l2beam-99";

    @Test
    public void testCommandLine() throws Exception {
        
        // verify that output is empty
        Path outputFile = new Path(OUTPUT_DIR);
        assertTrue("Shall not exist: " + outputFile,
                   ! outputFile.getFileSystem(new Configuration()).exists(outputFile));

        // Run the job
        ToolRunner.run(new BeamOperatorTool(), new String[] { REQUEST });

        assertTrue("Shall now exist: " + outputFile,
                   outputFile.getFileSystem(new Configuration()).exists(outputFile));
    }

    @Before
    public void init() throws IOException {
        clearWorkingDirs();
    }

//    @After
//    public void finish() throws IOException {
//        clearWorkingDirs();
//    }

    private void clearWorkingDirs() throws IOException {
        Path outputDir = new Path(OUTPUT_DIR);
        outputDir.getFileSystem(new Configuration()).delete(outputDir, true);
    }

}
