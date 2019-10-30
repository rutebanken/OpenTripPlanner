package org.opentripplanner.graph_builder.triptransformer.transform;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class TransitTransformFileInput {
    private static final Logger LOG = LoggerFactory.getLogger(TransitTransformFileInput.class);

    private final File inputFile;

    TransitTransformFileInput(File inputDir) {
        this.inputFile = inputDir == null ? null : new File(inputDir, "TransitTransform.txt");
    }

    List<List<String>> readFile() {
        try {
            if(inputFile == null || !inputFile.exists() || !inputFile.canRead()) {
                LOG.info("Trip Transit transformations no performed, no file available.");
                return Collections.emptyList();
            }

            List<String> lines = FileUtils.readLines(inputFile, StandardCharsets.UTF_8);
            List<List<String>> cmdLines = new ArrayList<>();
            for (String line : lines) {
                if(line.isBlank() || line.startsWith("#")) continue;
                List<String> tokens = Arrays.stream(line.split(";")).map(String::trim).collect(Collectors.toList());
                cmdLines.add(tokens);
            }

            return cmdLines;
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
