package com.typeclassified.hmm.cssr.trees

import breeze.linalg.DenseVector
import com.typeclassified.hmm.cssr.CSSR.Terminal
import com.typeclassified.hmm.cssr.parse.{AlphabetHolder, Alphabet}
import com.typeclassified.hmm.cssr.shared.EmpiricalDistribution

import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object LoopingTree {
  def allHomogeneous(tree: ParseTree, w:ParseLeaf): Boolean = homogeneous(allPrefixes(tree, w), w)
  def nextHomogeneous(tree: ParseTree)(w:ParseLeaf): Boolean = homogeneous(nextPrefixes(tree, w), w)

  def homogeneous(allHistories:ListBuffer[ParseLeaf], w:ParseLeaf): Boolean = allHistories.forall { pw => Tree.matches(w)(pw) }

  def allExcisable(tree:ParseTree)(w:ParseLeaf, e:String):Boolean = excisable(tree, allPrefixes(tree, w))(w, e)
  def nextExcisable(tree:ParseTree)(w:ParseLeaf, e:String):Boolean = excisable(tree, nextPrefixes(tree, w))(w, e)

  def excisable(tree:ParseTree, uews: ListBuffer[ParseLeaf])(w:ParseLeaf, e:String):Boolean = {
    excise(tree, uews)(w, e)
      .forall {
        case (uew, Some(uw)) => Tree.matches(uew)(uw)
        case (uew, None)     => true
        case _               => true
      }
  }

  def excise(tree:ParseTree, uews: ListBuffer[ParseLeaf])(w:ParseLeaf, e:String):ListBuffer[(ParseLeaf, Option[ParseLeaf])] = {
    val exciseStr:(String)=>String = excise(w.observed, e)
    uews
      .map { uew => (uew, exciseStr(uew.observed)) }
      .map { case (uew, uwStr) => (uew, tree.navigateHistoryRev(uwStr)) }
  }

  def excise(w:String, e:String)(uew: String):String = uew.take(uew.length - e.length - w.length) + w

  def prefixes(histories: ListBuffer[ParseLeaf], w:String):ListBuffer[ParseLeaf] = histories
    .filter { _.observed.takeRight(w.length) == w }

  def prefixes(histories: ListBuffer[ParseLeaf], w:ParseLeaf):ListBuffer[ParseLeaf] = prefixes(histories, w.observed)

  def prefixes(tree: ParseTree, w:String, prefixDepth:Int):ListBuffer[ParseLeaf] = {
    val histories = (w.length to prefixDepth)
      .flatMap { n => tree.getDepth(n) }
      .to[ListBuffer]

    prefixes(histories, w)
  }

  def nextPrefixes(tree: ParseTree, w:ParseLeaf):ListBuffer[ParseLeaf] = nextPrefixes(tree, w.observed)
  def nextPrefixes(tree: ParseTree, w:String):ListBuffer[ParseLeaf] = prefixes(tree, w, w.length+1)

  def allPrefixes(tree: ParseTree, w:ParseLeaf):ListBuffer[ParseLeaf] = allPrefixes(tree, w.observed)
  def allPrefixes(tree: ParseTree, w:String):ListBuffer[ParseLeaf] = prefixes(tree, w, tree.maxLength)

  def leafChildren(lleaves:Iterable[(Char, Node)]):Iterable[LLeaf] = lleaves.toMap.values.foldLeft(List[LLeaf]()){
    case (list, Left(a)) => list :+ a
    case (list, _) => list
  }

  def lleafChildren(lleaves:Iterable[(Char, Node)]):Iterable[LLeaf] = lleaves.toMap.values.foldLeft(List[LLeaf]()){
    case (list, Left(a)) => list :+ a
    case (list, Right(Left(a))) => list :+ a
    case (list, Right(Right(a))) => list
  }

  type AltNode = Either[Loop, EdgeSet]
  type Node = Either[LLeaf, AltNode]

  def findLoop(lleaf:LLeaf):Option[Loop] = Tree.firstExcisable(lleaf).flatMap( l => Some(new Loop(l, lleaf)) )

  def findEdgeSet(ltree: LoopingTree, lleaf: LLeaf):Option[EdgeSet] = {
    val terminals = ltree.terminals -- Tree.getAncestorsRecursive(lleaf).toSet[LLeaf]
    val terminalMatches:Set[LLeaf] = terminals.filter(Tree.matches(lleaf))
    if (terminalMatches.isEmpty) {
      None
    } else {
      val found:Option[EdgeSet] = terminalMatches
        .find{ _.edgeSet.isDefined }
        .flatMap{ _.edgeSet }

      if (found.nonEmpty) {
        val foundEdges = terminalMatches.filter(_.edgeSet.isDefined).map(_.edgeSet.get)
        assert(foundEdges.forall{ _ == found.get }, "All found edges must be the same.")
      }

      Option.apply(found.getOrElse( new EdgeSet(lleaf, terminalMatches) ))
    }
  }

  def findAlt(loopingTree: LoopingTree)(lleaf: LLeaf):Option[AltNode] = {
    lazy val edgeSet = findEdgeSet(loopingTree, lleaf)

    LoopingTree.findLoop(lleaf) match {
      case Some(l) => Some(Left(l))
      case None => if (edgeSet.isEmpty) None else Some(Right(edgeSet.get))
    }
  }

  def getLeaf(node:LoopingTree.Node):LLeaf = node match {
    case Left(lnode) => lnode
    case Right(Right(edgeset)) => edgeset.value
    case Right(Left(loop)) => loop.value
  }
}

class LoopingTree(val alphabet:Alphabet, root:LLeaf) extends Tree[LLeaf](root) {
  /** the set of non-looping terminal nodes */
  var terminals:Set[LLeaf] = HashSet()

  def this(ptree: ParseTree) = {
    this(ptree.alphabet, new LLeaf(ptree.root.observed, Set(ptree.root)))
    terminals = terminals + root
  }

  def navigateLoopingPath(history: String):Option[LoopingTree.Node] = {
    print("navigating: " + history)
    navigateLoopingPath(history, Some(Left(root)), _.last, _.init)
  }

  def navigateLoopingPath(history: String, active:Option[LoopingTree.Node], current:(String)=>Char, prior:(String)=>String): Option[LoopingTree.Node] = {
    if (history.isEmpty) {
      println(": " + active.toString)
      active
    } else {
      val next = active.flatMap { node => LoopingTree.getLeaf(node).nextLeaf(current(history)) }
      navigateLoopingPath(prior(history), next, current, prior)
    }
  }

  def navigateToLLeaf(history: String):Option[LLeaf] = navigateToLLeaf(history, Some(Left(root)), _.last, _.init)

  def navigateToLLeaf(history: String, active:Option[LoopingTree.Node], current:(String)=>Char, prior:(String)=>String): Option[LLeaf] = {
    val nextLeaf = active.flatMap { node => Option(LoopingTree.getLeaf(node)) }
    val nextNode = nextLeaf.flatMap { leaf => if (history.isEmpty) None else leaf.nextLeaf(current(history)) }
    if (nextNode.isEmpty) {
      println(": " + nextLeaf.toString)
      nextLeaf
    } else {
      navigateToLLeaf(prior(history), nextNode, current, prior)
    }
  }


  def navigateToLLeafButStopAtLoops(history: String):Option[LLeaf] = {
    print("navigating to LLeaf but stopping at loops: " + history)
    navigateToLLeafButStopAtLoops(history, Some(Left(root)), _.last, _.init)
  }

  def navigateToLLeafButStopAtLoops(history: String, active:Option[LoopingTree.Node], current:(String)=>Char, prior:(String)=>String): Option[LLeaf] = {
    val nextLeaf = active.flatMap { node => Option(LoopingTree.getLeaf(node)) }
    val nextNode = nextLeaf.flatMap { leaf => if (history.isEmpty) None else leaf.nextLeaf(current(history)) }
    if (nextNode.isEmpty) {
      val possibleloopOrLeaf = active.flatMap {
        case Right(Left(node)) => Option(node)
        case Right(Right(node)) => None // ignore edgesets for now
        case node => Option(LoopingTree.getLeaf(node))
      }

      println(": " + possibleloopOrLeaf.toString)
      possibleloopOrLeaf
    } else {
      navigateToLLeafButStopAtLoops(prior(history), nextNode, current, prior)
    }
  }

  def navigateToCollectedTerminal(history: String, terminals:Set[Terminal]): Option[LLeaf] = {
    print("navigating TERMINAL: " + history)
    navigateToCollectedTerminal(history, Some(Left(root)), _.last, _.init, terminals)
  }

  def navigateToCollectedTerminal(history: String, active:Option[LoopingTree.Node], current:(String)=>Char, prior:(String)=>String, terminals:Set[Terminal]): Option[LLeaf] = {

    val maybeLLeaf = navigateLoopingPath(history, active, current, prior)

    maybeLLeaf.flatMap {
      case Left(lleaf) => if (terminals.contains(lleaf)) Option(lleaf) else lleaf.terminalReference
      case Right(Right(edgeSet)) => edgeSet.edges.headOption
      case Right(Left(loop)) => {
        if (terminals.contains(loop)) Option(loop) else {
          (if (loop.terminalReference.nonEmpty) loop else loop.value).terminalReference
        }
      }
    }
  }

  def navigateToTerminal(history: String, terminals:Set[Terminal]): Option[LLeaf] = {
    print("navigating TERMINAL: " + history)
    navigateToTerminal(history, Some(Left(root)), _.last, _.init, terminals)
  }

  def navigateToTerminal(history: String, active:Option[LoopingTree.Node], current:(String)=>Char, prior:(String)=>String, terminals:Set[Terminal]): Option[LLeaf] = {

    val maybeLLeaf = navigateToLLeafButStopAtLoops(history, active, current, prior)

    maybeLLeaf.flatMap {
      lastLeaf => {
        if (terminals.contains(lastLeaf)) {
          Option(lastLeaf)
        } else {
          Option(lastLeaf.terminalReference.getOrElse(lastLeaf /* perhaps this should be None */))
        }
      }
    }
  }
}

// a simple abstract marker class for a looping tree's Loops and Edges
abstract class LoopWrapper(val value:LLeaf) {
  override def toString: String = getClass.getSimpleName + "(" + value.toString() + ")"
}

class Loop(val value: LLeaf, observed:String, seededHistories:Set[ParseLeaf] = Set(), parent:Option[LLeaf] = None) extends LLeaf(observed, seededHistories, parent) {
  def this(value:LLeaf, origin:LLeaf) = this(value, origin.observed, origin.histories, origin.parent)

  var asTerminal:Option[LLeaf] = None

  override def toString: String = getClass.getSimpleName + "(" + value.toString() + ")"
}

class EdgeSet (edge: LLeaf, val edges:Set[LLeaf]) extends LoopWrapper(edge) {

}

class LLeaf(val observed:String, seededHistories:Set[ParseLeaf] = Set(), parent:Option[LLeaf] = None) extends Leaf[LLeaf] (if ("".equals(observed)) 0.toChar else observed.head, parent) with EmpiricalDistribution {
  histories = seededHistories
  recalculateHists(histories)

  var children:mutable.Map[Char, LoopingTree.Node] = mutable.Map()

  var edgeSet:Option[EdgeSet] = None

  def this(p: ParseLeaf) = this(p.observed, Set(p), None)

  override def getChildren():Iterable[LLeaf] = LoopingTree.lleafChildren(children)

  def nextLeaf(c: Char): Option[LoopingTree.Node] = children.get(c)

  // this pretty much duplicates LoopingTree.getLeaf
  override def next(c: Char): Option[LLeaf] = children.get(c).flatMap { node => Some(LoopingTree.getLeaf(node)) }

  override def toString():String = {
    val rDist = rounded.toArray.mkString("[", ",", "]")
    val nChildren = children.keys.size
    s"{$observed, rounded:$rDist, size: ${histories.size}, children: $nChildren}"
  }

  // temp debugging purposes
  var originalDistribution:DenseVector[Double] = DenseVector.zeros(AlphabetHolder.alphabet.length)
  var terminalReference:Option[LLeaf] = None

  def refineWith(lLeaf: LLeaf):Unit = {
    originalDistribution = distribution
    distribution = lLeaf.distribution
    terminalReference = Option(lLeaf)
  }
}

