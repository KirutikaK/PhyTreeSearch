package hu.sztaki.phytree.tree;

/*
 Copyright (c) 2002 Compaq Computer Corporation

 SOFTWARE RELEASE

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 - Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 - Neither the names of Compaq Research, Compaq Computer Corporation
 nor the names of its contributors may be used to endorse or promote
 products derived from this Software without specific prior written
 permission.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL COMPAQ COMPUTER CORPORATION BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import hu.sztaki.phytree.FastaItem;

import java.util.*;

/**
 * A class representing a node of a (phylognenetic) tree. The tree that this
 * node belongs to is of type Tree. Nodes have fields that store a pre- and
 * post-ordering.
 * 
 * A TreeNode has a list of children, a unique key, a leftmostleaf and a
 * rightmost leaf
 * 
 * @author Tamara Munzner, Li Zhang, Yunhong Zhou
 * @version 2.2
 * @see Tree
 * @see GridCell
 */
public class TreeNode {

  /**
   * Array of child nodes that are attached below this internal node. Null if
   * this is a leaf.
   */
  protected ArrayList<TreeNode> children; // eventually turn this into an array
                                          // (need

  // to change parser)

  protected final Tree tree;
  
  public List<TreeNode> getChildren() {
    return children;
  }

  /**
   * key is unique for nodes in one tree. Keys are pre-ordered (root = 0,
   * depth-traversal ordering).
   */
  public int key;

  /**
   * Score for a node in [0,1] that corresponds to the topological similarity
   * between two tree drawers.
   * 
   * @see TreePairs#getBestCorrNodeScore(Tree, TreeNode, Tree, int)
   */
  private Double bcnScore;

  // /**
  // * The offset of the point with respect to the cell. We only have
  // * this for the row offset as we assume that the vertical edges
  // * are all aligned. When computing the Y coordinate of a node, we
  // * add nodeOffsetR to the pointOffset[1], a fixed parameter set by
  // * AccordionDrawer.
  // */
  /**
   * The last frame that had a computed {@link #midYPosition}, for caching.
   */
  protected int computedFrame; // store frame midYPosition was last
  // calculated (needed to place parents)

  private FastaItem sequence;
  
  public FastaItem getSequence() {
    return sequence;
  }

  public void setSequence(FastaItem sequence) {
    this.sequence = sequence;
  }

  public String getSeqString() {
    if (sequence == null)
      return "";
    return sequence.getSequenceString();
  }

  // 1 for leaf nodes, 1< for inner nodes, max length of path to a leaf +1
  private int subTreeHeight = -1;

  public int getSubTreeHeight() {
    if (subTreeHeight == -1) {
      if (isLeaf()) {
        subTreeHeight = 1;
      } else {
        int maxHeight = 0;
        for (TreeNode node : children) {
          int subHeight = node.getSubTreeHeight();
          maxHeight = Math.max(subHeight, maxHeight);
        }
        subTreeHeight = maxHeight + 1;
      }
    }
    return subTreeHeight;
  }

  private int leafNum = -1;
  private List<String> patterns = new ArrayList<String>();
  // how many of the leaves of the subtree starting at this node contain the
  // pattern
  private List<Integer> patternNums = new ArrayList<Integer>();

  private void addPattern(String pattern) {
    patterns.add(pattern);
    patternNums.add(-1);
  }

  public int getLeafNum() {
    if (leafNum == -1) {
      if (isLeaf()) {
        leafNum = 1;
      } else {
        leafNum = 0;
        for (TreeNode c : children) {
          leafNum += c.getLeafNum();
        }
      }
    }
    return leafNum;
  }

  public int getLeafNumWithPattern(String pattern) {
    if (!patterns.contains(pattern)) {
      addPattern(pattern);
    }
    int idx = patterns.indexOf(pattern);
    int val = patternNums.get(idx);
    if (val == -1) {
      int computed = computeLeafNumWithPattern(pattern);
      patternNums.set(idx, computed);
      val = computed;
    }
    return val;
  }

  private int computeLeafNumWithPattern(String pattern) {
    if (isLeaf()) {
      if (getSeqString().contains(pattern)) {
        return 1;
      } else {
        return 0;
      }
    } else {
      int sum = 0;
      for (TreeNode tn : children) {
        sum += tn.getLeafNumWithPattern(pattern);
      }
      return sum;
    }
  }

  /**
   * Returns the minimum key value of nodes in the subtree rooted by this node.
   * 
   * @return The index of the smallest descendant node (which is the key for
   *         this node).
   */
  // this is the key for this node
  public int getMin() {
    return key;
  }

  /**
   * Returns the maximum key value of nodes in the subtree rooted but this node.
   * 
   * @return The index of the smallest descendant node (which is the key for the
   *         rightmost leaf node).
   */
  // this is the key of the rightmost leaf
  public int getMax() {
    return rightmostLeaf.key;
  }

  /**
   * Returns the key for this node.
   * 
   * @return The value of {@link #key} for this node.
   */
  public int getKey() {
    return key;
  }

  /**
   * Returns the label for this node, which is {@link #name}.
   * 
   * @return The value of {@link #name} for this node.
   */
  public String getName() {
    return name;
  }

  /**
   * Tests to see if this node has a vertical or horizontal edge component.
   * 
   * @param xy
   *          0/X for horizontal, 1/Y for vertical nodes.
   * @return True if this node has an edge in the chosen direction. Only root
   *         nodes don't have a horizontal edge, and only leaves don't have
   *         vertical edges.
   */
  protected boolean getEdge(int xy) {
    if (xy == 0)
      return !isRoot();
    else
      return !isLeaf();
  }

  /**
   * Implements Comparable interface - sorts on key field.
   * 
   * @param o
   *          The other object to compare this node to.
   * @return -1 if this is smaller than the object's key, +1 if greater, 0 if
   *         equal.
   */
  public int compareTo(Object o) {
    if (key == ((TreeNode) o).key)
      return 0;
    else if (key < ((TreeNode) o).key)
      return -1;
    else
      return 1;
  }

  /** The parent of this node. This is null for the root node. */
  public TreeNode parent;

  /**
   * Node name with default "". Most internal nodes have no name and all leaf
   * nodes have a name. This becomes the long version of the node name when
   * fully qualified names are used.
   */
  protected String name = ""; // the long form in fully qualified names

  /**
   * The text that appears when the node is highlighted or has a name displayed.
   */
  public String label = ""; // always short form

  /** Distance from this node to the root node. The root is at height 1. */
  public int depth;

  /**
   * Weight is the horizontal edge length for the edge immediately above the
   * node. Edge lengths are not determined by this number currently; all edges
   * are stretched to make leaves right aligned, with minimal integral lengths.
   */
  public float distFromParent = 0.0f;

  /**
   * Leftmost (minimum) leaf node under this internal node (or this node for
   * leaves).
   */
  public TreeNode leftmostLeaf;
  /**
   * Rightmost (maximum) leaf node under this internal node (or this node for
   * leaves).
   */
  public TreeNode rightmostLeaf;

  /** The number of leaves under this internal node (or 1 for leaves). */
  public int numberLeaves;

  /** The next preorder node. */
  public TreeNode preorderNext = null;

  /** The next postorder node. */
  public TreeNode posorderNext = null;

  /**
   * Default tree node constructor. Children list initially set to capacity 2 as
   * in most case binary. Used in 2 places: create the root when creating the
   * tree; the parser uses this to create nodes attached to the root.
   */
  public TreeNode(Tree t) {
    children = new ArrayList<TreeNode>(2);
    bcnScore = new Double(0.0);
    tree = t;
  }

  /**
   * Clean this node of children.
   */
  public void close() {
    children.clear();
  }

  /**
   * Destroy this node. Runs {@link #close()}.
   */
  protected void finalize() throws Throwable {

    try {
      close();
    } finally {
      super.finalize();
      // System.out.println("finally clean treeNodes");
    }
  }

  /**
   * Set the name for this node, the name is usually the label drawn with this
   * node.
   * 
   * @param s
   *          The new value of {@link #name}, the name for this node.
   */
  public void setName(String s) {
    name = s;
  }

  /**
   * Get the number of children under this node.
   * 
   * @return Number of nodes stored in the children array {@link #children}.
   */
  public int numberChildren() {
    return children.size();
  }

  /**
   * Get a given child for this node, with range checking and casting.
   * 
   * @param i
   *          The child index to get.
   * @return The i(th) child for this node.
   */
  public TreeNode getChild(int i) {
    if (i < children.size())
      return (TreeNode) children.get(i);
    else
      return null;
  }

  /**
   * Tests to determine if this node is a leaf. Does not work for nodes not in
   * the tree structure.
   * 
   * @return True if this node has no linked children, and therefore is a leaf
   *         node for the tree.
   */
  public boolean isLeaf() {
    return children.isEmpty();
  }

  /**
   * Tests to determine if this node is the root of its tree. Does not work for
   * nodes not in the tree structure.
   * 
   * @return True if this node has no linked parent, and therefore is the root
   *         of the tree.
   */
  public boolean isRoot() {
    return (null == parent);
  }

  /**
   * Tests nodes for equality, based on the name of the node.
   * 
   * @param n
   *          Second node to test vs. this node.
   * @return True if the names of both nodes are the same, false otherwise.
   */
  public boolean equals(TreeNode n) {
    return (name.equals(n.name));
  }

  /**
   * Add a child to the end of the list of children. Note there is no remove
   * child method, this is permanent. Additional processing for linking nodes
   * (setting up pointers and leaf properties, for example) is done later.
   * 
   * @param n
   *          New child node for this node.
   */
  public void addChild(TreeNode n) {
    children.add(n);
    n.parent = this;
  }

  /**
   * Get the parent for this node.
   * 
   * @return Value of {@link #parent}.
   */
  public TreeNode parent() {
    return parent;
  }

  /**
   * Set the weight of this treenode, which encodes the length of the horizontal
   * edge. Edge weights are not implemented currently for drawing.
   * 
   * @param w
   *          New edge weight for this node, {@link #distFromParent}.
   */
  public void setWeight(double w) {
    distFromParent = (float) w;
  }
  
  public double getDistFromParent() {
    return distFromParent;
  }

  /**
   * Get the weight of this treenode, which encodes the length of the horizontal
   * edge. Edge weights are not implemented currently for drawing.
   * 
   * @return Edge weight for this node, {@link #distFromParent}.
   */
  public float getWeight() {
    return distFromParent;
  }

  /**
   * Get the first child of this node. Doesn't work with leaf nodes.
   * 
   * @return First child of this internal node.
   */
  protected TreeNode firstChild() {
    return (TreeNode) children.get(0);
  }

  /**
   * Get the last child of this node. Doesn't work with leaf nodes.
   * 
   * @return Last child of this internal node.
   */
  public TreeNode lastChild() {
    return (TreeNode) children.get(children.size() - 1);
  }

  /**
   * Long form printing for a single node. Used in conjunction with
   * {@link #printSubtree()} to display a whole subtree.
   * 
   */
  public void print() {
    if (name != null)
      System.out.print("node name: " + name + "\t");
    else
      System.out.print("node name null,\t");
    System.out.println("key: " + key);
  }

  /**
   * Set the extreme leaves for this node. This is done in leaf->root direction,
   * so all linking can be done in O(n) time.
   * 
   */
  public void setExtremeLeaves() {
    if (isLeaf()) {
      leftmostLeaf = this;
      rightmostLeaf = this;
      return;
    }
    leftmostLeaf = firstChild().leftmostLeaf;
    rightmostLeaf = lastChild().rightmostLeaf;
  }

  /** root->leaf traversal, depth first in direction of leftmost leaf. */
  public void linkNodesInPreorder() {
    if (isLeaf())
      return;
    preorderNext = firstChild();
    for (int i = 0; i < numberChildren() - 1; i++)
      getChild(i).rightmostLeaf.preorderNext = getChild(i + 1);
    // rightmostLeaf.preorderNext = null; // redundant
  }

  /** Leaf->root traversal, starting at leftmost leaf of tree. */
  public void linkNodesInPostorder() {
    if (isLeaf())
      return;
    // n.posorderNext = null; // redundant
    for (int i = 0; i < numberChildren() - 1; i++)
      getChild(i).posorderNext = getChild(i + 1).leftmostLeaf;
    lastChild().posorderNext = this;
  }

  /**
   * Sets the number of leaves, must be run on leaves first (pre-order)
   * 
   * @return The number of leaves ({@link #numberLeaves}) including the current
   *         node (leaves = 1)
   */
  public int setNumberLeaves() {
    numberLeaves = 0;
    if (isLeaf())
      numberLeaves = 1;
    else
      for (int i = 0; i < children.size(); i++)
        numberLeaves += getChild(i).numberLeaves;
    return numberLeaves;
  }

  /**
   * String value of this node, name + key + tree height information.
   * 
   * @return String representation of this node.
   */
  public String toString() {
    // String edge[] = {edges[0]!=null?edges[0].toString():"X",
    // edges[1]!=null?edges[1].toString():"Y"};
    return name + "(" + key + " @ " + depth + ")";
  }

  /**
   * Set the {@link #bcnScore} for this node.
   * 
   * @param n
   *          New value of {@link #bcnScore}.
   */
  public void setBcnScore(float n) {
    bcnScore = new Double(n);
  }

  /**
   * Get the BCN score for this treenode.
   * 
   * @return Value of {@link #bcnScore} for this node.
   */
  public Double getBcnScore() {
    return bcnScore;
  }

  private boolean hasPattern = false;

  public void setHasPattern(boolean hasPattern) {
    this.hasPattern = hasPattern;
  }

  public int numOfDisorderedPatternLeaves(String pattern, double threshold) {
    if (isLeaf()) {
      if (sequence.hasDisorderProbs() 
          && sequence.getSequenceString().contains(pattern)) {
        if (sequence.hasDisorderedPattern(pattern, threshold)) {
          return 1;
        }
      }
      return 0;
    } else {
      int sumChildren = 0;
      for (TreeNode n : children) {
        sumChildren += n.numOfDisorderedPatternLeaves(pattern, threshold);
      }
      return sumChildren;
    }
  }
  
  public String getNewickSubtree(boolean withColors) {
    if (isLeaf()) {
      String ret;
      if (hasPattern && withColors) {
        ret = getName().trim() + "[&&NHX:COLOR=1]:" + distFromParent;
      } else {
        ret = getName().trim() + ":" + distFromParent;
      }
      return ret;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (TreeNode child : children) {
      sb.append(child.getNewickSubtree(withColors));
      sb.append(",");
    }
    // remove last comma
    sb.deleteCharAt(sb.length() - 1);
    sb.append(")");
    if (hasPattern && withColors) {
      sb.append(getName().trim() + "[&&NHX:COLOR=1]:" + distFromParent);
    } else {
      sb.append(getName().trim() + ":" + distFromParent);
    }

    return sb.toString();
  }
  
  public List<FastaItem> addSubtreeFastaItemsToSet(List<FastaItem> set) {
    List<FastaItem> res = set; 
    if (isLeaf()) {
      res.add(getSequence());
      return res;
    }
    for (TreeNode c: children) {
      res = c.addSubtreeFastaItemsToSet(res); 
    }
    return set;
  }

  public String getOrString(String pattern) {    
    if (isLeaf()) {
      if (null != pattern) {
        if (getSeqString().contains(pattern)) {
          return " OR " + sequence.getAcNum();
        }
      } else { // no pattern is set: every leaf has to be in result
        return " OR " + sequence.getAcNum();
      }
    } else { // not leaf: see children
      String ret = "";
      for (TreeNode c : children) {
        ret = ret + c.getOrString(pattern);
      }
      return ret;
    }
    return "";
  }
  
  public void renameFromLongToSimple() {
    String origname = new String(this.name);
    if (!isLeaf()) {
      for (TreeNode c: children) {
        c.renameFromLongToSimple();
      }
    }
    if (this.name.startsWith("sp") && this.name.contains("|")) {
      String[] fields = name.split("\\|");
      if (fields.length < 3) {
        System.err.println("Error, unexpected name format:" + this.name);
        System.err.println("Expecting headers like: \n >sp|<IDENTIFIER>|<IDX OF FRAGMENT>|<NOTES>");
        return;
      }
      this.name = fields[1] + "_" + fields[2];
      tree.renameNode(this, origname, this.name);
    }
  }

  // select only leaves of this subtree that have the pattern, and are far 
  // enough from each other
  // required min distance is in sum of branch lengths
  // returns a fasta formatted string
  public String getBlastFastaString(String pattern, double minDistInTree) {
    List<BlastFastaItem> bfiList = getBlastNodes(pattern, minDistInTree);
    StringBuilder sb = new StringBuilder();
    for (BlastFastaItem bfi: bfiList) {
      sb.append(bfi.seq.getHeaderRow() + "\n");
      for (String s : bfi.seq.getSequenceRows()) {
        sb.append(s + "\n");
      }
    }
    return sb.toString();
  }
  
  
  public String drawSubtreeString(TreeNode n, int level, boolean withSeq,
      boolean withDists) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < level; i++) {
      sb.append("        ");
    }
    if (withDists) {
      sb.append("| " + n.distFromParent + "\n");
    } else {
      sb.append("|\n");
    }

    for (int i = 0; i < level; i++) {
      sb.append("        ");
    }
    sb.append(" -- " + n.name);

    if (n.isLeaf()) {
      if (withSeq && n.getSequence() != null) {
        sb.append(" (").append(n.getSeqString()).append(")");
      }
      sb.append("\n");
      return sb.toString();
    }
    sb.append(" --\n");
    for (TreeNode child : n.children) {
      sb.append(drawSubtreeString(child, level + 1, withSeq, withDists));
    }
    return sb.toString();
  }
  
  private class BlastFastaItem {
    public FastaItem seq;
    public double distFromLeafNow = 0.0;
    public BlastFastaItem(FastaItem fi, double dist) {
      seq = fi;
      distFromLeafNow = dist;
    }
  }
  
  public List<BlastFastaItem> getBlastNodes(String pattern, 
      double minDistInTree) {
    if (isLeaf()) {
      ArrayList<BlastFastaItem> ret = new ArrayList<BlastFastaItem>();
      if (null != pattern) {
        if (getSeqString().contains(pattern)) {
          ret.add(new BlastFastaItem(sequence, getDistFromParent()));
          return ret;
        } else {
          // not adding this leaf, return empty list
          return ret;
        }
      } else {
        ret.add(new BlastFastaItem(sequence, getDistFromParent()));
        return ret;
      }
    } else {
      // greedy algo: start putting elements in a return list, 
      // but check before if the new one is too close
      // to the already added ones
      ArrayList<BlastFastaItem> ret = new ArrayList<BlastFastaItem>();
      for (TreeNode c : children) {
        ArrayList<BlastFastaItem> toAdd = new ArrayList<BlastFastaItem>();
        List<BlastFastaItem> bfiList = c.getBlastNodes(pattern, minDistInTree);
        
        if (ret.isEmpty()) { // these have been checked already, so add all of them
          for (BlastFastaItem bfi : bfiList) {
            ret.add(bfi);
          }
        } else {
          // check if the new elements are far enough from all the already added ones
          // technically need to add them to a separate list to avoid
          // concurrent modification exception
          for (BlastFastaItem bfiNew : bfiList) {
            for (BlastFastaItem bfiAdded : ret) {
              double distOfItems = bfiNew.distFromLeafNow + bfiAdded.distFromLeafNow;
              if  (distOfItems > minDistInTree) {
                // update new
                
                // then add, if not added
                if (!toAdd.contains(bfiNew)) {
                  toAdd.add(bfiNew);
                }
              }
            }
          }
          ret = unifyResultsAndUpdateDists(ret, toAdd);
          return ret;
        }
      }
      return ret;
    }
  }

  
  private ArrayList<BlastFastaItem> unifyResultsAndUpdateDists(
      ArrayList<BlastFastaItem> ret, ArrayList<BlastFastaItem> toAdd) {
    // update distances
    for (BlastFastaItem bfi: ret) {
      bfi.distFromLeafNow = bfi.distFromLeafNow + this.getDistFromParent();
    }
    // update and add to final result
    for (BlastFastaItem bfi: toAdd) {
      bfi.distFromLeafNow = bfi.distFromLeafNow + this.getDistFromParent();
      ret.add(bfi);
    }
    return ret;
  }
}