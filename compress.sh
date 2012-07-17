#!/bin/sh

# Compresses stdin to stdout in a format readable by LZMA2InputStream.
# Note! The decoder currently assumes a 1MB dictionary

xz --format=raw --lzma2=dict=64M,mf=bt4,mode=normal,nice=273,depth=1000 -z -c
