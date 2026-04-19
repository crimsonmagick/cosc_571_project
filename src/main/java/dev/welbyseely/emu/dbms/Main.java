package dev.welbyseely.emu.dbms;

import static java.lang.System.out;

import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import dev.welbyseely.emu.dbms.tree.DuplicateEntry;

public class Main {


  // Main Function
  public static void main(String[] args) {
    BinarySearchTree<Integer> tree = new BinarySearchTree<>();

    // Inserting elements
    tree.insert(50);
    tree.insert(30);
    tree.insert(20);
    tree.insert(40);
    tree.insert(70);
    tree.insert(60);
    tree.insert(80);

    out.println("Inorder traversal:");
    tree.inorder();

    try {
      tree.insert(80);
    } catch (DuplicateEntry e) {
      out.println("Caught e: " + e.getMessage());
    }

    // Deleting elements
    tree.delete(20);
    tree.delete(30);

    out.println("Inorder traversal after deletion:");
    tree.inorder();

    // Searching for an element
    int searchKey = 70;
    out.println("Is " + searchKey + " present in the tree? " + tree.search(searchKey));

    // Traversals
    out.println("Preorder traversal:");
    tree.preorder();

    out.println("Postorder traversal:");
    tree.postorder();
  }
}