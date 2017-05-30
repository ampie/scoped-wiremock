package com.sbg.bdd.resource.file

import com.sbg.bdd.resource.Resource
import com.sbg.bdd.resource.ResourceContainer
import com.sbg.bdd.resource.ResourceFilter
import spock.lang.Specification

class WhenTraversingAnExistingDirectoryTree extends Specification {
    def 'should resolve an existing child directory'() {

        given:
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new RootDirectoryResource(rootDir);

        when:
        def childDir1 = root.resolveExisting('childdir1')
        then:
        childDir1 instanceof DirectoryResource
        childDir1.path == '/childdir1'
    }

    def 'should resolve an existing grandchild file'() {

        given:
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new RootDirectoryResource(rootDir);

        when:
        def grandChildFile = root.resolveExisting('childdir1', 'file1_1.txt')
        then:
        grandChildFile instanceof ReadableFileResource
        grandChildFile.path == '/childdir1/file1_1.txt'

    }
    def 'should list files matching a certain name'() {

        given:
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new RootDirectoryResource(rootDir);
        def childDir = root.resolveExisting('childdir1')

        when:
        def listing = childDir.list(new ResourceFilter() {
            @Override
            boolean accept(ResourceContainer container, String name) {
                return name.endsWith("json")
            }
        })
        then:
        listing.length ==1
        listing[0].path=='/childdir1/file1_2.json'

    }
    def 'should support forward slashes in the provided segments to resolve'() {

        given:
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new RootDirectoryResource(rootDir);

        when:
        def grandChildFile = root.resolveExisting('childdir1/childdir1_1', 'file1_1_1.txt')
        then:
        grandChildFile instanceof ReadableFileResource
        grandChildFile.path == '/childdir1/childdir1_1/file1_1_1.txt'

    }
    def 'should treat empty segments as references to the current dir'() {

        given:
        def markerResource = Thread.currentThread().contextClassLoader.getResource("common-resource-api-marker.txt")
        def rootDir = new File(markerResource.file).getParentFile()
        def root = new RootDirectoryResource(rootDir);

        when:
        def grandChildFile = root.resolveExisting('childdir1', null, 'childdir1_1', '', 'file1_1_1.txt')
        then:
        grandChildFile instanceof ReadableFileResource
        grandChildFile.path == '/childdir1/childdir1_1/file1_1_1.txt'

    }
}
