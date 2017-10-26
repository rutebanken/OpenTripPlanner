package org.opentripplanner.graph_builder.model;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NetexStopPlaceBundle {

    private File file;
    public static final String NETEX_STOP_PLACE_FILE =  "stopPlaces.xml";

    public NetexStopPlaceBundle(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public InputStream getStopPlaceFile() {
        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            ZipEntry entry = zipFile.stream().filter(files -> files.getName().equals(NETEX_STOP_PLACE_FILE)).findFirst().orElseThrow(FileNotFoundException::new);
            return zipFile.getInputStream(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getFileInputStream(ZipEntry entry){
        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            return zipFile.getInputStream(entry);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkInputs() {
        if (file != null) {
            if (!file.exists()) {
                throw new RuntimeException("NETEX Path " + file + " does not exist.");
            }
            if (!file.canRead()) {
                throw new RuntimeException("NETEX Path " + file + " cannot be read.");
            }
        }
    }


}