package dev.welbyseely.emu.dbms.tree;

class Node<T extends Comparable<T>> {

  T key;
  Node<T> left, right;

  public Node(T key) {
    this.key = key;
    this.left = right = null;
  }
}

/**
 * Based on <a
 * href="https://www.geeksforgeeks.org/java/java-program-to-construct-a-binary-search-tree/">...</a>
 */
public class BinarySearchTree<T extends Comparable<T>> {

  Node<T> root;

  public BinarySearchTree() {
    root = null;
  }

  public void insert(T key) {
    root = insertRec(root, key);
  }

  Node<T> insertRec(Node<T> root, T key) {
    if (root == null) {
      root = new Node<>(key);
      return root;
    }
    final int comparison = key.compareTo(root.key);
    if (comparison < 0) {
      root.left = insertRec(root.left, key);
    } else if (comparison > 0) {
      root.right = insertRec(root.right, key);
    } else {
      throw new DuplicateEntry(key);
    }

    return root;
  }

  public void delete(T key) {
    root = deleteRec(root, key);
  }

  Node<T> deleteRec(Node<T> root, T key) {
    if (root == null) {
      return root;
    }

    final int comparison = key.compareTo(root.key);
    if (comparison < 0) {
      root.left = deleteRec(root.left, key);
    } else if (comparison > 0) {
      root.right = deleteRec(root.right, key);
    } else {
      if (root.left == null) {
        return root.right;
      } else if (root.right == null) {
        return root.left;
      }

      root.key = minValue(root.right);
      root.right = deleteRec(root.right, root.key);
    }

    return root;
  }

  T minValue(Node<T> root) {
    T minv = root.key;
    while (root.left != null) {
      minv = root.left.key;
      root = root.left;
    }
    return minv;
  }

  public boolean search(T key) {
    return searchRec(root, key);
  }

  boolean searchRec(Node<T> root, T key) {
    if (root == null) {
      return false;
    }
    final int comparison = key.compareTo(root.key);
    if (comparison == 0) {
      return true;
    }
    if (comparison < 0) {
      return searchRec(root.right, key);
    }
    return searchRec(root.left, key);
  }

  public void inorder() {
    inorderRec(root);
    System.out.println("\n");
  }

  void inorderRec(Node<T> root) {
    if (root != null) {
      inorderRec(root.left);
      System.out.print(root.key + " ");
      inorderRec(root.right);
    }
  }

  public void preorder() {
    preorderRec(root);
    System.out.println("\n");

  }

  void preorderRec(Node<T> root) {
    if (root != null) {
      System.out.print(root.key + " ");
      preorderRec(root.left);
      preorderRec(root.right);
    }
  }

  // Postorder traversal
  public void postorder() {
    postorderRec(root);
    System.out.println("\n");
  }

  void postorderRec(Node<T> root) {
    if (root != null) {
      postorderRec(root.left);
      postorderRec(root.right);
      System.out.print(root.key + " ");
    }
  }

}