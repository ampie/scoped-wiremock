package com.sbg.bdd.resource.file

import com.sbg.bdd.resource.WritableResource
import org.junit.Assert
import spock.lang.Specification

class WhenWorkingWithPotentialFiles extends Specification {
    def 'should resolve an potential grandchild file'() {

        given:
        DirectoryResourceRoot root = getRoot()
        deletePotentialChildren(root)
        when:
        def writableResource = root.resolvePotential('newchilddir','newchildfile.txt')
        then:
        writableResource.canRead() == false
        writableResource.path == '/newchilddir/newchildfile.txt'
    }
    def 'should write a file and keep the parent resource in sync'() {

        given:
        DirectoryResourceRoot root = getRoot()
        WritableResource writableResource = deletePotentialChildren(root)
        def data = [1, 2, 3, 4, 5] as byte[]

        when:
        writableResource.write(data)

        then:
        writableResource.getParent().list().length ==1
        writableResource.canRead() == true
        writableResource.asReadable().read() == data
    }
    def 'should not be able to read from a non-existing a file'() {

        given:
        DirectoryResourceRoot root = getRoot()

        when:
        WritableResource writableResource = deletePotentialChildren(root)

        then:
        writableResource.canRead() == false
        try {
            writableResource.asReadable().read() == data
            Assert.fail()
        } catch (Exception e) {
        }
    }


    private WritableResource deletePotentialChildren(DirectoryResourceRoot root) {
        def writableResource = root.resolvePotential('newchilddir', 'newchildfile.txt')
        if (writableResource.getFile().exists()) {
            writableResource.getFile().delete();
        }
        if (writableResource.getFile().getParentFile().exists()) {
            writableResource.getFile().getParentFile().delete();
        }
        if (writableResource.getParent().list().length > 0) {
            throw new IllegalStateException("Could not successfully delete files")
        }
        writableResource
    }

    private DirectoryResourceRoot getRoot() {
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new DirectoryResourceRoot('root', rootDir);
        root
    }

}
