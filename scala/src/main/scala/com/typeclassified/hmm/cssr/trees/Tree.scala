package com.typeclassified.hmm.cssr.trees

import breeze.linalg.DenseVector
import breeze.numerics._
import com.typeclassified.hmm.cssr.shared.{Epsilon, Probablistic}

import scala.collection.mutable.ListBuffer

object Tree {
  implicit val ep:Epsilon = new Epsilon(0.01)

  def matches[L1 <: Leaf[L1], L2 <: Leaf[L2]](u:L1)(w:L2): Boolean = u ~= w

  def matches(u:DenseVector[Double])(w:DenseVector[Double]): Boolean = round(u) == round(w)

  def round(dist:DenseVector[Double]): DenseVector[Double] = {
    val rndPrecision:Double = 1 / ep.precision
    (rint(dist * rndPrecision):DenseVector[Double]) / rndPrecision
  }

  def round[L1 <: Leaf[L1]](u:L1): DenseVector[Double] = round(u.distribution)

  def firstExcisable[L <: Leaf[L]](w:L):Option[L] = {
    // ancestors must be ordered by depth with the root first. Hence, reverse
    getAncestors(w).reverse.find{matches(w)}
  }

  def getAncestors[L <: Leaf[L]](w:L):List[L] = {
    val ancestors = ListBuffer[L]()
    var active = w
    while (active.parent.nonEmpty) {
      ancestors += active.parent.get
      active = active.parent.get
    }
    ancestors.toList
  }

  def getAncestorsRecursive[L <: Leaf[L]](w:L, ancestors:List[L]=List()):List[L] = {
    val active = Option.apply(w)
    if (active.isEmpty) {
      ancestors
    } else {
      val next:List[L] = ancestors ++ List(active).flatten
      val parent:Option[L] = active.flatMap(_.parent)
      if (parent.isEmpty) next else getAncestorsRecursive(parent.get, next)
    }
  }
}

class Tree {
  // Just a marker class
}

class Leaf[B <: Leaf[_]] (val parent: Option[B] = None) extends Probablistic {
  implicit val ep:Epsilon = new Epsilon(0.01)

  def ~= [A <: Leaf[A]] (pLeaf:A)(implicit ep:Epsilon):Boolean = {
    (abs(distribution :- pLeaf.distribution) :< ep.precision).reduceRight(_&&_)
  }
}
