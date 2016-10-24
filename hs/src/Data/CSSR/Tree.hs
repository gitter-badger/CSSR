module Data.CSSR.Tree where


class Tree t => LoopingTree t where
  -- implicit val ep:Epsilon = new Epsilon(0.01)
  -- implicit val sig:Double = 0.001d

  -------------------------------------------------------------------------------
  -- | Two words match if and only if they lead to the same prediction for the
  -- next step.
  -------------------------------------------------------------------------------
  matches :: Probablistic -- ^A leaf node from a parse tree, representing a word
          -> Probablistic -- ^A leaf node from a parse tree, representing a word
          -> Bool         -- ^whether or not the two leaves have the same
                          --  prediction for the next step
  matches u w = u.testNull(w)

   ------------------------------------------------------------------------------
   -- | Given some word {@code w}, and word {@code eq}, where w = eq: we call
   -- prefix e excisable from w iff, for all prefixes p, match(peq, pq). That is,
   -- inserting the extra history e before q makes no difference to predictions.
   --
   ------------------------------------------------------------------------------
  firstExcisable :: Leaf -- ^A leaf node from a parse tree, representing a word
                 -> Maybe Leaf -- ^the first possible ancestor leaf which has the
                               --  same next-step prediction as w

  firstExcisable w = getAncestors(w).reverse.find{matches(w)}
  -- ancestors must be ordered by depth with the root first. Hence, reverse

  getAncestors :: Foldable f
               => Leaf
               -> f Leaf
  getAncestors w = go w []
    where
      go :: Foldable f => Leaf -> f Leaf -> f Leaf
      go w ancestors = undefined
  -- def getAncestorsRecursive[L <: Leaf[L]](w:L, ancestors:List[L]=List()):List[L] = {
  --   val active = Option.apply(w)
  --   if (active.isEmpty) {
  --     ancestors
  --   } else {
  --     val next:List[L] = ancestors ++ List(active).flatten
  --     val parent:Option[L] = active.flatMap(_.parent)
  --     if (parent.isEmpty) next else getAncestorsRecursive(parent.get, next)
  --   }
  -- }

data Tree = Tree { root :: Leaf } deriving (Show)

getDepth :: Foldable f => Tree -> Integer -> f Leaf
getDepth tree depth = go depth [root tree]
  where
    go :: Foldable f => Integer -> f Leaf -> f Leaf
    go depth nodes
      | depth <= 0 = nodes
      | otherwise = getDepth (depth-1) (nodes.flatMap{ _.getChildren() })

collectLeaves :: Foldable f => Tree -> f Leaf
collectLeaves tree = go [root tree] []
  where
    go :: Foldable f => f Leaf -> f Leaf -> f Leaf
    go    [] collected = collected
    go layer collected = filter (not null . children) layer
      -- val nextLayer:Iterable[L] = layer.partition(_.getChildren().isEmpty)._2
      -- collectLeaves(nextLayer.flatMap(n => n.getChildren()), collected ++ layer)

data Leaf = Leaf -- extends Probabalistic
  { observation :: Char
  , parent :: Maybe Leaf
  } deriving (Eq)

path :: Foldable f => Tree -> f Char
path = undefined -- Tree.getAncestorsRecursive(this.asInstanceOf[B]).map(_.observation).filterNot(_ == '\0')
-- def getChildren():Iterable[B]
-- def next(c:Char):Option[B]

