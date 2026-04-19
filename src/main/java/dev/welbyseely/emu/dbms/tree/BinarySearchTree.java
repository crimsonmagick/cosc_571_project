package dev.welbyseely.emu.dbms.tree;

import java.util.ArrayList;
import java.util.List;

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
      return searchRec(root.left, key);
    }
    return searchRec(root.right, key);
  }

  public List<T> inorder() {
    List<T> result = new ArrayList<>();
    inorderRec(root, result);
    return result;
  }

  private void inorderRec(Node<T> root, List<T> result) {
    if (root != null) {
      inorderRec(root.left, result);
      result.add(root.key);
      inorderRec(root.right, result);
    }
  }

  public List<T> preorder() {
    List<T> result = new ArrayList<>();
    preorderRec(root, result);
    return result;
  }

  void preorderRec(Node<T> root, List<T> result) {
    if (root != null) {
      result.add(root.key);
      preorderRec(root.left, result);
      preorderRec(root.right, result);
    }
  }

  public List<T> postorder() {
    List<T> result = new ArrayList<>();
    postorderRec(root, result);
    return result;
  }

  void postorderRec(Node<T> root, List<T> result) {
    if (root != null) {
      postorderRec(root.left, result);
      postorderRec(root.right, result);
      result.add(root.key);
    }
  }

}