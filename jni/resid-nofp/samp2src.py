import sys
import os

def main(argv):
	_,name,inf,out = argv
	
	data = open(inf).read()
	h = open(out, 'w')
	
	nonZero = 0
	zero = 0
	# Number of non-zero after another non-zero
	conseqNonZero = 0
	
	zerorun = 0
	printed = 0
	
	print >>h, "static unsigned short %s[4096];" % name
	
	print >>h, "static void init_%s() __attribute__((noinline));" % (name)
	print >>h, "static void init_%s() { " % (name)
	print >>h, "\tunsigned short* dest = %s;" % (name)
	#print >>h, "\tsize_t ix = 0;"
	saved_ix = 0;
	for ix in xrange(0,len(data)):
		byte = ord(data[ix])
		if byte:
			if ix != saved_ix:
				print >>h, "\tdest += %d;" % (ix - saved_ix)
			print >>h, "\t*dest++ = (%d) << 4;" % (byte)
			saved_ix = ix + 1
	print >>h, "}"
	
if __name__=='__main__':
	sys.exit(main(sys.argv))