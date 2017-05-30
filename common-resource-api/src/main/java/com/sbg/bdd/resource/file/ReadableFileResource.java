package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.ReadableResource;

import java.io.*;

public class ReadableFileResource extends FileSystemResource implements ReadableResource {

    public ReadableFileResource(DirectoryResource directoryResource, File file) {
        super(directoryResource, file);
    }

    public WritableFileResource asWritable() {
        if (canWrite()) {
            return new WritableFileResource(getParent(), getFile());
        } else {
            throw new IllegalStateException("File " + getPath() + " is read only");
        }
    }

    @Override
    public boolean canWrite() {
        return getFile().canWrite();
    }

    @Override
    public byte[] read()  {
        try {
            return toBytes(new FileInputStream(getFile()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] toBytes(InputStream is) throws IOException {
        byte[] chunk = new byte[50];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            int chunkSize = 0;
            while ((chunkSize = is.read(chunk)) > -1) {
                os.write(chunk, 0, chunkSize);
            }
            os.flush();
        } finally {
            os.close();
        }
        return os.toByteArray();
    }

}
