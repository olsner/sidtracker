{-# LANGUAGE NoMonomorphismRestriction #-}

import Control.Applicative

import qualified Data.ByteString.Lazy as B
import Data.Binary (Binary,get)
import Data.Binary.Get
import Data.List
import Data.Map (Map)
import qualified Data.Map as M
import Data.Maybe
import Data.Word

import System.Environment

data Cat = Global | Voice Int deriving (Eq,Ord,Show)
-- Word will limit the length of a song to about half an hour on 32-bit platforms...
data Write = Write { time :: Word, reg :: Word8, value :: Word8 } deriving (Eq,Ord,Show)

getInt = do
    t <- getWord8
    case t of
        0xff -> fromIntegral <$> getWord32be
        0xfe -> fromIntegral <$> getWord16be
        _    -> fromIntegral <$> pure t

instance Binary Write where
    get = Write <$> getInt <*> getWord8 <*> getWord8

getMany = isEmpty >>= \b -> if b then pure [] else (:) <$> get <*> getMany
decode = runGet getMany

cats = [Global, Voice 1, Voice 2, Voice 3]

collect xs = M.fromList [ (c,[ x | (c2,x) <- xs, c2 == c]) | c <- cats ]
-- collect :: Ord a => [(a, b)] -> Map a [b]
-- collect = foldl' (\m (c,x) -> M.insertWith' (flip (++)) c [x] m) M.empty

-- http://www.oxyron.de/html/registers_sid.html
reglist =
    -- $0..$14: 7 registers per channel:
    -- freq low+hi
    -- pulse width low+hi
    -- control (waveform and gate)
    -- attack/decay
    -- sustain/release
    replicate 7 (Voice 1) ++
    replicate 7 (Voice 2) ++
    replicate 7 (Voice 3) ++
    -- $15/$16: FC low+high
    -- $17: Filter control (Note: this has one bit per channel 1..3)
    -- $18: high/band/low filter, volume
    -- $19-1a: potx/y
    replicate 6 (Global) ++
    replicate 2 (Voice 3)
regmap = M.fromList (zip [0..] reglist)
cat (Write { reg = r }) = fromMaybe Global (M.lookup r regmap)

categorize :: [Write] -> [(Cat,Write)]
categorize = map (\x -> (cat x, x))

undelta t (w@Write { time = delta } : ws) = w { time = delta + t } : undelta (delta + t) ws
undelta _ [] = []

main = do
    input <- collect . categorize . undelta 0 . decode <$> B.readFile "temp"
    mapM_ print . take 200 . fromJust $ M.lookup (Voice 3) input
    --mapM_ print (take 200 input)
    print (M.map length input)
