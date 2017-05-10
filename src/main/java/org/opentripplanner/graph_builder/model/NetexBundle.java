package org.opentripplanner.graph_builder.model;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NetexBundle {

    private File file;
    public static final String NETEX_COMMON_FILE_NAME =  "_common.xml";

    public NetexBundle(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public InputStream getCommonFile(){
        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            ZipEntry entry = zipFile.getEntry(NETEX_COMMON_FILE_NAME);
            return zipFile.getInputStream(entry);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ZipEntry> getFileEntries(){
        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            return zipFile.stream().filter(entry -> !entry.getName().equals(NETEX_COMMON_FILE_NAME)).collect(Collectors.toList());
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
