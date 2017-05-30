package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.WritableResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WritableFileResource extends FileSystemResource implements WritableResource {
    public WritableFileResource(DirectoryResource previous, File file) {
        super(previous, file);
    }

    @Override
    public boolean canRead() {
        return getFile().exists() && getFile().canRead();
    }

    @Override
    public void write(byte[] data)  {
        try {
            ensureDirPresent();
            writeData(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void ensureDirPresent() {
        if(!getFile().getParentFile().exists()){
            if(!getFile().getParentFile().mkdirs()){
                throw new IllegalStateException("Could not create dir: " + getParent().getFile().getAbsolutePath());
            }
        }
    }

    private void writeData(byte[] data) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(getFile());
        try {
            outputStream.write(data);
            outputStream.flush();
        }finally{
            outputStream.close();
        }
    }

    public ReadableFileResource asReadable(){
        if(canRead()){
            return (ReadableFileResource) getParent().getChild(getFile().getName());
        }else{
            throw new IllegalStateException("File " + getPath() +" does not exist yet or cannot be read");
        }
    }
}
