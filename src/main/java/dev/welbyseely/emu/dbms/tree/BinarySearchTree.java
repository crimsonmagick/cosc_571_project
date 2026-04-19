package dev.welbyseely.emu.dbms.tree;

import java.util.ArrayList;
import java.util.List;

class Node<T extends Comparable<? super T>, V> {

  T key;
  V value;
  Node<T, V> left, right;

  public Node(T key, V value) {
    this.key = key;
    this.value = value;
    this.left = right = null;
  }
}

/**
 * Based on <a
 * href="https://www.geeksforgeeks.org/java/java-program-to-construct-a-binary-search-tree/">...</a>
 */
public class BinarySearchTree<T extends Comparable<? super T>, V> {

  Node<T, V> root;

  public BinarySearchTree() {
    root = null;
  }

  public void insert(T key, V value) {
    root = insertRec(root, key, value);
  }

  Node<T, V> insertRec(Node<T, V> root, T key, V value) {
    if (root == null) {
      root = new Node<>(key, value);
      return root;
    }
    final int comparison = key.compareTo(root.key);
    if (comparison < 0) {
      root.left = insertRec(root.left, key, value);
    } else if (comparison > 0) {
      root.right = insertRec(root.right, key, value);
    } else {
      throw new DuplicateEntry(key);
    }

    return root;
  }

  public void delete(T key) {
    root = deleteRec(root, key);
  }

  Node<T, V> deleteRec(Node<T, V> root, T key) {
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

      ResultEntry<T, V> minResult = minKey(root.right);
      root.key = minResult.key();
      root.value = minResult.value();
      root.right = deleteRec(root.right, root.key);
    }

    return root;
  }

  ResultEntry<T, V> minKey(Node<T, V> root) {
    T mink = root.key;
    V minv = root.value;
    while (root.left != null) {
      mink = root.left.key;
      minv = root.left.value;
      root = root.left;
    }
    return new ResultEntry<>(mink, minv);
  }

  public ResultEntry<T, V> search(T key) {
    return searchRec(root, key);
  }

  ResultEntry<T, V> searchRec(Node<T, V> root, T key) {
    if (root == null) {
      return null;
    }
    final int comparison = key.compareTo(root.key);
    if (comparison == 0) {
      return new ResultEntry<>(root.key, root.value);
    }
    if (comparison < 0) {
      return searchRec(root.left, key);
    }
    return searchRec(root.right, key);
  }

  public List<ResultEntry<T, V>> inorder() {
    List<ResultEntry<T, V>> result = new ArrayList<>();
    inorderRec(root, result);
    return result;
  }

  private void inorderRec(Node<T, V> root, List<ResultEntry<T, V>> result) {
    if (root != null) {
      inorderRec(root.left, result);
      final ResultEntry<T, V> resultEntry = new ResultEntry<>(root.key, root.value);
      result.add(resultEntry);
      inorderRec(root.right, result);
    }
  }

  public List<ResultEntry<T, V>> preorder() {
    List<ResultEntry<T, V>> result = new ArrayList<>();
    preorderRec(root, result);
    return result;
  }

  void preorderRec(Node<T, V> root, List<ResultEntry<T, V>> result) {
    if (root != null) {
      ResultEntry<T, V> resultEntry = new ResultEntry<>(root.key, root.value);
      result.add(resultEntry);
      preorderRec(root.left, result);
      preorderRec(root.right, result);
    }
  }

  public List<ResultEntry<T, V>> postorder() {
    List<ResultEntry<T, V>> result = new ArrayList<>();
    postorderRec(root, result);
    return result;
  }

  void postorderRec(Node<T, V> root, List<ResultEntry<T, V>> result) {
    if (root != null) {
      postorderRec(root.left, result);
      postorderRec(root.right, result);
      final ResultEntry<T, V> resultEntry = new ResultEntry<>(root.key, root.value);
      result.add(resultEntry);
    }
  }

}