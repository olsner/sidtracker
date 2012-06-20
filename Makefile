all: sidtoasid

sidtoasid: sidtoasid.cpp
	g++ -g -o $@ $< -lsidplay2
