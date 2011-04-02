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
	
	print >>h, "static const unsigned char initdata_%s[] = {" % name
	for byte in data:
		byte = ord(byte)
		if byte:
			if zerorun:
				conseqNonZero += 1
			print >>h, zerorun, ',', byte, ','
			zerorun = 0
			nonZero += 1
			printed += 2
		else:
			zero += 1
			zerorun += 1
			if zerorun == 256:
				print >>h, "255, 0,"
				zerorun = 0
				printed += 2
	print >>h, "};"
	
	print >>h, "static void init_%s() { init_wavedata_zerorun(%s, initdata_%s, %d); }" % (name, name, name, printed / 2)

	
	print >>h, "// Zeroes:", zero, "Non-zero:", nonZero, "Non-zero after non-zero:", conseqNonZero,"Printed:", printed

if __name__=='__main__':
	sys.exit(main(sys.argv))