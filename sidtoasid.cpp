#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include <sidplay/sidplay2.h>

void usage(FILE* out)
{
	fprintf(out, "Usage: sidtoasid INPUT OUTPUT\n");
}

unsigned writes = 0;
struct asidemu: public sidemu
{
	uint8_t regs[256];
	c64env* env;

	asidemu(sidbuilder* builder, c64env* env, sid2_model_t model):
		sidemu(builder),
		env(env)
	{
		reset(0);
		printf("init: model := %d\n", model);
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
		writes++;
		fprintf(stderr, "%lu: %#x := %#x\n", (long unsigned)getTime(), addr, data);
		regs[addr] = data;
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
	asidbuilder(): sidbuilder("aSID converter") {}

	sidemu* lock(c64env* env, sid2_model_t model)
	{
		return new asidemu(this, env, model);
	}
	void unlock(sidemu* device)
	{
		delete device;
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
	asidbuilder builder;
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
		printf("Writes: %u, time %f, samples %lu\n", writes, (float)player.time() / player.timebase(), samples);
		if ((float)player.time() / player.timebase() > seconds)
		{
			break;
		}
	}
}
