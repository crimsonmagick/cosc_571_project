package dev.welbyseely.emu.dbms.tree

import spock.lang.Specification
import spock.lang.Unroll

class BinarySearchTreeTest extends Specification {

    @Unroll
    def "insert builds correct tree and inorder traversal is sorted for #type"() {
        given:
        def tree = new BinarySearchTree()

        when:
        values.each { tree.insert(it) }

        then:
        tree.inorder() == expected

        where:
        type     | values                              || expected
        "int"    | [50, 30, 20, 40, 70, 60, 80]        || [20, 30, 40, 50, 60, 70, 80]
        "string" | ["d", "b", "a", "c", "g", "f", "h"] || ["a", "b", "c", "d", "f", "g", "h"]
        "float"  | [5.0, 3.0, 2.0, 4.0, 7.0, 6.0, 8.0] || [2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    }

    @Unroll
    def "duplicate insert throws exception for #type"() {
        given:
        def tree = new BinarySearchTree()
        tree.insert(value)

        when:
        tree.insert(value)

        then:
        thrown(DuplicateEntry)

        where:
        type     | value
        "int"    | 80
        "string" | "x"
        "float"  | 8.0
    }

    @Unroll
    def "delete removes elements correctly for #type"() {
        given:
        def tree = new BinarySearchTree()
        values.each { tree.insert(it) }

        when:
        deletes.each { tree.delete(it) }

        then:
        tree.inorder() == expected

        where:
        type     | values                              | deletes    || expected
        "int"    | [50, 30, 20, 40, 70, 60, 80]        | [20, 30]   || [40, 50, 60, 70, 80]
        "string" | ["d", "b", "a", "c", "g", "f", "h"] | ["a", "b"] || ["c", "d", "f", "g", "h"]
        "float"  | [5.0, 3.0, 2.0, 4.0, 7.0, 6.0, 8.0] | [2.0, 3.0] || [4.0, 5.0, 6.0, 7.0, 8.0]
    }

    @Unroll
    def "search finds existing and non-existing elements for #type"() {
        given:
        def tree = new BinarySearchTree()
        values.each { tree.insert(it) }

        expect:
        tree.search(present)
        !tree.search(absent)

        where:
        type     | values                              | present | absent
        "int"    | [50, 30, 20, 40, 70, 60, 80]        | 70      | 999
        "string" | ["d", "b", "a", "c", "g", "f", "h"] | "g"     | "zzz"
        "float"  | [5.0, 3.0, 2.0, 4.0, 7.0, 6.0, 8.0] | 7.0     | 999.0
    }

    @Unroll
    def "preorder and postorder traversals are correct for #type"() {
        given:
        def tree = new BinarySearchTree()
        values.each { tree.insert(it) }

        expect:
        tree.preorder() == preorderExpected
        tree.postorder() == postorderExpected

        where:
        type     | values                              || preorderExpected                    | postorderExpected
        "int"    | [50, 30, 20, 40, 70, 60, 80]        || [50, 30, 20, 40, 70, 60, 80]        | [20, 40, 30, 60, 80, 70, 50]
        "string" | ["d", "b", "a", "c", "g", "f", "h"] || ["d", "b", "a", "c", "g", "f", "h"] | ["a", "c", "b", "f", "h", "g", "d"]
        "float"  | [5.0, 3.0, 2.0, 4.0, 7.0, 6.0, 8.0] || [5.0, 3.0, 2.0, 4.0, 7.0, 6.0, 8.0] | [2.0, 4.0, 3.0, 6.0, 8.0, 7.0, 5.0]
    }
}