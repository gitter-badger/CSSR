{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE ConstraintKinds #-}
module CSSR.Prelude
  ( module X
  , groupBy
  , identity
  , unsafeHead
  , minimumBy
  , unsafeMinimumBy
  , impossible
  , prettyDecimal
  , f'4
  , CSSR.Prelude.head
  , GVector
  , UVector
  , Locations
  , Idx
  , Event
  , Delim
  , DataFileContents
  ) where

import Control.Arrow       as X
import Control.Exception   as X
import Control.Monad       as X
import Control.DeepSeq     as X (NFData, deepseq)
import Data.Foldable       as X hiding (minimumBy)
import Data.Function       as X (on)
import Data.List           as X (intercalate, nub, (\\), sort, sortBy, delete)
import Data.Maybe          as X (catMaybes)
import Data.Monoid         as X
import Data.Hashable       as X
import Data.HashMap.Strict as X (HashMap)
import Data.HashSet        as X (HashSet)
import Data.Set            as X (Set)
import Data.Sequence       as X (Seq)
import Data.Text           as X (Text)
import Data.Vector         as X (Vector, (!))
import Data.Vector.Mutable as X (MVector)
import Debug.Trace         as X
import GHC.Exts            as X (IsList(fromList))
import GHC.Generics        as X (Generic)
import GHC.Natural         as X (Natural)
import Lens.Micro.Platform as X
import Lens.Micro.Internal as X
import Prelude             as X hiding (id, head)

import qualified Data.Vector.Generic as GV (Vector)
import qualified Data.Vector.Unboxed as UV (Vector)
import qualified Data.HashMap.Strict as HM
import qualified Data.Foldable       as F
import qualified Data.Vector         as V
import qualified Prelude             as P
import Numeric

identity :: a -> a
identity = P.id

unsafeMinimumBy :: Foldable t => (a -> a -> Ordering) -> t a -> a
unsafeMinimumBy = F.minimumBy

minimumBy :: Foldable t => (a -> a -> Ordering) -> t a -> Maybe a
minimumBy f as
  | null as   = Nothing
  | otherwise = Just $ unsafeMinimumBy f as

groupBy :: forall a b . (Hashable b, Eq b) => (a -> b) -> [a] -> [(b, [a])]
groupBy fn = HM.toList . foldr step mempty
  where
    step :: a -> X.HashMap b [a] -> X.HashMap b [a]
    step a memo = HM.alter go (fn a) memo
      where
        go :: Maybe [a] -> Maybe [a]
        go   Nothing = Just [a]
        go (Just as) = Just (a:as)

unsafeHead :: Foldable f => f a -> a
unsafeHead = P.head . toList

head :: Foldable f => f a -> Maybe a
head fs
  | null fs   = Nothing
  | otherwise = Just (unsafeHead fs)

type Locations = HashMap Idx Integer
type Idx = Integer
type Event = Text
type Delim = Text
type DataFileContents = Vector Event
type GVector = GV.Vector
type UVector = UV.Vector

instance Hashable x => Hashable (Vector x) where
  hashWithSalt salt = hashWithSalt salt . V.toList

impossible = error

prettyDecimal :: RealFloat a => Int -> a -> String
prettyDecimal p f = showFFloat (Just p) f ""

f'4 :: RealFloat a => a -> String
f'4 = prettyDecimal 4
