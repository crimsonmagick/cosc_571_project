package dev.welbyseely.emu.dbms.tree

import spock.lang.Specification
import spock.lang.Unroll

class BinarySearchTreeSpec extends Specification {

    @Unroll
    def "insert builds correct tree and inorder traversal is sorted for #type"() {
        given:
        def tree = new BinarySearchTree()

        when:
        entries.each { tree.insert(it.key, it.value) }

        then:
        tree.inorder() == expected

        where:
        type     | entries                                                                       || expected
        "int"    | [re(50, 0), re(30, 1), re(20, 2), re(40, 3), re(70, 4), re(60, 5), re(80, 6)] || [re(20, 2), re(30, 1), re(40, 3), re(50, 0), re(60, 5), re(70, 4), re(80, 6)]
        "string" | [re("d", 0), re("b", 1), re("a", 2), re("c", 3), re("g", 4), re("f", 5), re("h", 6)]
                                                                                                 || [re("a", 2), re("b", 1), re("c", 3), re("d", 0), re("f", 5), re("g", 4), re("h", 6)]
        "float"  | [re(5.0, 0), re(3.0, 1), re(2.0, 2), re(4.0, 3), re(7.0, 4), re(6.0, 5), re(8.0, 6)]
                                                                                                 || [re(2.0, 2), re(3.0, 1), re(4.0, 3), re(5.0, 0), re(6.0, 5), re(7.0, 4), re(8.0, 6)]
    }

    @Unroll
    def "duplicate insert throws exception for #type"() {
        given:
        def tree = new BinarySearchTree()
        tree.insert(key, value)

        when:
        tree.insert(key, value)

        then:
        thrown(DuplicateEntry)

        where:
        type     | key | value
        "int"    | 80  | 0
        "string" | "x" | 1
        "float"  | 8.0 | 2
    }

    @Unroll
    def "delete removes elements correctly for #type"() {
        given:
        def tree = new BinarySearchTree()
        entries.each { tree.insert(it.key, it.value) }

        when:
        deletes.each { tree.delete(it) }

        then:
        tree.inorder() == expected

        where:
        type     | entries                                                                       | deletes    || expected
        "int"    | [re(50, 0), re(30, 1), re(20, 2), re(40, 3), re(70, 4), re(60, 5), re(80, 6)] | [20, 30]   || [re(40, 3), re(50, 0), re(60, 5), re(70, 4), re(80, 6)]
        "string" | [re("d", 0), re("b", 1), re("a", 2), re("c", 3), re("g", 4), re("f", 5), re("h", 6)]
                                                                                                 | ["a", "b"] || [re("c", 3), re("d", 0), re("f", 5), re("g", 4), re("h", 6)]
        "float"  | [re(5.0, 0), re(3.0, 1), re(2.0, 2), re(4.0, 3), re(7.0, 4), re(6.0, 5), re(8.0, 6)]
                                                                                                 | [2.0, 3.0] || [re(4.0, 3), re(5.0, 0), re(6.0, 5), re(7.0, 4), re(8.0, 6)]
    }

    def "delete root with two children preserves structure"() {
        given:
        def tree = new BinarySearchTree()
        [re(50, 0), re(30, 1), re(70, 2), re(60, 3), re(80, 4)].each {
            tree.insert(it.key, it.value)
        }

        when:
        tree.delete(50)

        then:
        tree.inorder() == [re(30, 1), re(60, 3), re(70, 2), re(80, 4)]
    }

    @Unroll
    def "search finds existing and non-existing elements for #type"() {
        given:
        def tree = new BinarySearchTree()
        entries.each { tree.insert(it.key, it.value) }

        expect:
        tree.search(present.key) == present
        tree.search(absent.key) == null

        where:
        type     | entries                                                                       | present    | absent
        "int"    | [re(50, 0), re(30, 1), re(20, 2), re(40, 3), re(70, 4), re(60, 5), re(80, 6)] | re(70, 4)  | re(999, 12)
        "string" | [re("d", 0), re("b", 1), re("a", 2), re("c", 3), re("g", 4), re("f", 5), re("h", 6)]
                                                                                                 | re("g", 4) | re("zzz", 345)
        "float"  | [re(5.0, 0), re(3.0, 1), re(2.0, 2), re(4.0, 3), re(7.0, 4), re(6.0, 5), re(8.0, 6)]
                                                                                                 | re(7.0, 4) | re(999.0, 234)
    }

    @Unroll
    def "preorder and postorder traversals are correct for #type"() {
        given:
        def tree = new BinarySearchTree()
        entries.each { tree.insert(it.key, it.value) }

        expect:
        tree.preorder() == preorderExpected
        tree.postorder() == postorderExpected

        where:
        type     | entries                                                                       || preorderExpected | postorderExpected
        "int"    | [re(50, 0), re(30, 1), re(20, 2), re(40, 3), re(70, 4), re(60, 5), re(80, 6)] || [re(50, 0), re(30, 1), re(20, 2), re(40, 3), re(70, 4), re(60, 5), re(80, 6)]
                                                                                                                     | [re(20, 2), re(40, 3), re(30, 1), re(60, 5), re(80, 6), re(70, 4), re(50, 0)]
        "string" | [re("d", 0), re("b", 1), re("a", 2), re("c", 3), re("g", 4), re("f", 5), re("h", 6)]
                                                                                                 || [re("d", 0), re("b", 1), re("a", 2), re("c", 3), re("g", 4), re("f", 5), re("h", 6)]
                                                                                                                     | [re("a", 2), re("c", 3), re("b", 1), re("f", 5), re("h", 6), re("g", 4), re("d", 0)]
        "float"  | [re(5.0, 0), re(3.0, 1), re(2.0, 2), re(4.0, 3), re(7.0, 4), re(6.0, 5), re(8.0, 6)]
                                                                                                 || [re(5.0, 0), re(3.0, 1), re(2.0, 2), re(4.0, 3), re(7.0, 4), re(6.0, 5), re(8.0, 6)]
                                                                                                                     | [re(2.0, 2), re(4.0, 3), re(3.0, 1), re(6.0, 5), re(8.0, 6), re(7.0, 4), re(5.0, 0)]
    }

    private static <T, V> ResultEntry<T, V> re(T k, V v) {
        new ResultEntry<>(k, v)
    }
}