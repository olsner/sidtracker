#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include <sidplay/sidplay2.h>

/* Notes on data density: almost all timestamp deltas fit in one byte, some
 * fairly common offsets up to ~19200 (0xff*75 + 62), then some outliers.
 * Largest time-delta: 40110.
 *
 * Offsets 1 through 3 never appear in $randomsidtune.
 */
struct deltafile
{
	int fd;

	deltafile(const char* filename)
	{
		fd = creat(filename, 0666);
		assert(fd >= 0);
	}

	~deltafile()
	{
		close(fd);
	}

	void write(uint8_t byte)
	{
		int res = ::write(fd, &byte, 1);
		assert(res == 1);
	}

	void writeInt(unsigned long value)
	{
		if (value < 0xfe)
		{
			write(value);
		}
		else if (value <= 0xffff)
		{
			write(0xfe);
			writeInt16(value);
		}
		else
		{
			write(0xff);
			writeInt32(value);
		}
	}

	void writeInt16(unsigned long value)
	{
		write((value >> 8) & 0xff);
		write(value & 0xff);
	}

	void writeInt32(unsigned long value)
	{
		writeInt16((value >> 16) & 0xffff);
		writeInt16(value & 0xffff);
	}
};

void usage(FILE* out)
{
	fprintf(out, "Usage: sidtoasid INPUT OUTPUT\n");
}

struct asidemu: public sidemu
{
	uint8_t regs[256];
	c64env* env;
	deltafile outfile;
	unsigned long lastWrite;
	unsigned long writes;

	asidemu(const char* outfile,
			sidbuilder* builder, c64env* env, sid2_model_t model):
		sidemu(builder),
		env(env),
		outfile(outfile),
		lastWrite(0),
		writes(0)
	{
		reset(0);
		printf("init %p: model := %d\n", this, model);
	}

	~asidemu()
	{
		printf("done %p: %lu writes\n", this, writes);
	}

	void reset(uint8_t vol)
	{
		// FIXME What's the actual initial state of a SID?
		memset(regs, 0, sizeof(regs));
	}

	uint8_t read(uint8_t addr)
	{
		// We don't record reads...
		return regs[addr];
	}

	void write(uint8_t addr, uint8_t data)
	{
		unsigned long now = getTime();
		//fprintf(stderr, "%lu +%lu %#x := %#x\n", now, now - lastWrite, addr, data);
		if (now >= lastWrite)
		{
			uint8_t delta = regs[addr] ^ data;
			outfile.writeInt(now - lastWrite);
			outfile.write(addr);
			outfile.write(delta);

			lastWrite = now;
		}

		regs[addr] = data;
		writes++;
	}

	event_clock_t getTime() const
	{
		return env->context().getTime(env->context().phase());
	}

	const char* credits() { return "SID to SID* conversion, by Simon Brenner"; }
	const char* error() { return NULL; }

	int_least32_t output(uint8_t v)
	{
		return 1;
	}

	void voice(uint_least8_t, uint_least8_t, bool) {}
	void gain(int_least8_t) {}
};

struct asidbuilder: public sidbuilder
{
	const char* outfile;
	bool locked;
	sidemu* emu;

	asidbuilder(const char* outfile):
		sidbuilder("aSID converter"),
		outfile(outfile),
		locked(false),
		emu(NULL)
	{}

	~asidbuilder()
	{
		delete emu;
	}

	sidemu* lock(c64env* env, sid2_model_t model)
	{
		if (!locked && !emu)
		{
			emu = new asidemu(outfile, this, env, model);
			locked = emu != NULL;
			return emu;
		}
		return NULL;
	}
	void unlock(sidemu* device)
	{
		assert(locked && emu == device);
		delete device;
		locked = false;
		emu = NULL;
	}

	const char* credits() { return "SID to SID* conversion, by Simon Brenner"; }
	const char* error() const { return NULL; }
};

int main(int argc, const char* argv[])
{
	sidplay2 player;
	//player.debug(true, stderr);

	if (argc != 3)
	{
		usage(stderr);
		return 1;
	}

	const char* infile = argv[1];
	const char* outfile = argv[2];

	sid2_config_t cfg = player.config();
	cfg.clockDefault = SID2_CLOCK_PAL;
	cfg.clockForced = false;
	cfg.clockSpeed = SID2_CLOCK_PAL;
	cfg.frequency = 44100;
	cfg.precision = SID2_DEFAULT_PRECISION;
	cfg.emulateStereo = false;
	cfg.sidDefault = SID2_MOS6581;
	cfg.sidModel = SID2_MOS6581;
	cfg.optimisation = SID2_DEFAULT_OPTIMISATION;
	cfg.sidSamples = true;
	cfg.playback = sid2_mono;

	asidbuilder builder(outfile);
	cfg.sidEmulation = &builder;
	int i = player.config(cfg);
	if (i < 0)
	{
		printf("config status: %d\n", i);
		abort();
	}

	SidTune tune(infile);
	tune.selectSong(0);
	if (player.load(&tune) < 0)
	{
		printf("load failed\n");
		abort();
	}
	SidTuneInfo subInfo;
	tune.getInfo(subInfo);

	sid2_info_t info = player.info();
    SidTuneInfo tuneInfo = *player.info().tuneInfo;
	/*if (player.fastForward(0) < 0)
	{
		printf("fastForward to beginning failed\n");
		abort();
	}*/
	printf("Opened successfully! state is %d\n", player.state());

	char dummy[44100];
	int seconds = 244; // The length of Spellbound.sid
	unsigned long samples = 0;
	player.play(dummy, sizeof(dummy));
	while (player.state() != sid2_stopped)
	{
		int res = player.play(dummy, sizeof(dummy));
		assert(res == sizeof(dummy));
		samples += res;
		//printf("%lu: State now %d (play returned %d)\n", (unsigned long)player.time(), player.state(), x);
		//printf("Error: %s\n", player.error());
		if ((float)player.time() / player.timebase() > seconds)
		{
			break;
		}
	}
}
