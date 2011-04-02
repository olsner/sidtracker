import sys
import os

def addStat(stat, key, inc = 1):
	if stat is not None:
		stat[key] = stat.get(key, 0) + inc

def zeroRunEncode(data, stat = None, maxValue = 256, assumePadded = False):
	zerorun = 0
	for x in data:
		if x:
			addStat(stat, 'non-zero')
			if not zerorun:
				addStat(stat, 'non-zero after non-zero')
			while zerorun >= maxValue:
				addStat(stat, 'zerorun overflow')
				yield maxValue-1, 0
				zerorun -= maxValue
			yield zerorun, x
			zerorun = 0
		else:
			addStat(stat, 'zero')
			zerorun += 1
	if not assumePadded:
		while zerorun >= maxValue:
			addStat(stat, 'zerorun overflow')
			yield maxValue-1, 0
			zerorun -= maxValue
		if zerorun:
			yield zerorun-1, 0

def runLengthEncode(data, stat = None, maxValue = 256):
	data = iter(data)
	runlength = 1
	lastX = data.next()
	for x in data:
		if x == lastX:
			runlength += 1
		elif runlength >= maxValue or x != lastX:
			yield runlength-1, lastX
			lastX = x
			runlength = 1
	assert runlength > 0
	yield runlength-1, lastX

def runLengthDecode(data):
	for count,x in data:
		for _ in xrange(1 + count):
			yield x

def unpairify(xs):
	for x,y in xs:
		yield x
		yield y

def pairify(xs):
	xs = iter(xs)
	while 1:
		try:
			x = xs.next()
		except StopIteration:
			return

		try:
			y = xs.next()
			yield x,y
		except StopIteration:
			yield x

def zeroRunDecode(data, len = None):
	written = 0
	for zeroes,x in pairify(data):
		for _ in xrange(zeroes):
			yield 0
		yield x
		written += 1 + zeroes
	if len is not None:
		for _ in xrange(len - written):
			yield 0

def main(argv):
	_,name,inf,out = argv
	
	data = open(inf).read()
	h = open(out, 'w')
	
	print >>h, "static unsigned short %s[4096];" % name
	
	print >>h, "static const unsigned char initdata_%s[] = {" % name
	stats = {}
	data = map(ord, data)
	encoded = list(zeroRunEncode(data, stats, assumePadded = True))
	
	rleEncoded = list(runLengthEncode(encoded, stats))
	stats['RLE-encoded size after zero-run coding'] = len(rleEncoded) * 3
	assert list(runLengthDecode(rleEncoded)) == encoded
	
	rleDirectly = list(runLengthEncode(data))
	stats['RLE-encoded directly'] = len(rleDirectly) * 2
	assert list(runLengthDecode(rleDirectly)) == data
	
	encoded = list(unpairify(encoded))
	assert len(encoded) % 2 == 0
	decoded = list(zeroRunDecode(encoded, len(data)))
	if decoded != data:
		print "Encoded"
		print repr(list(encoded))
		print "Decoded"
		print repr(decoded)
		print "Original"
		print repr(data) 
		assert decoded == data
	stats['Original size'] = len(data)
	stats['Printed'] = len(encoded)
	
	stats['Saved encoded'] = len(data) - len(encoded)
	stats['Saved encoded-RLE'] = len(data) - len(rleEncoded)
	stats['Saved direct-RLE'] = len(data) - len(rleDirectly)
	
	for x,y in pairify(encoded):
		assert x >= 0 and x < 256
		assert y >= 0 and y < 256
		print >>h, x, ',', y, ','

	print >>h, "};"
	
	print >>h, "static void init_%s() { init_wavedata_zerorun(%s, initdata_%s, %d); }" % (name, name, name, len(encoded) / 2)

	for k,v in stats.iteritems():
		print >>h, '// ', k + ':', v

if __name__=='__main__':
	sys.exit(main(sys.argv))