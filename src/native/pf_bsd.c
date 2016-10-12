#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/bpf.h>
#include <net/if.h>

#include <err.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

#include "pf.h"

/* Possible BPF device paths. */
static const char *bpf_dev_paths[] = {
    "/dev/bpf",
    "/dev/bpf0",
    "/dev/bpf1",
    "/dev/bpf2",
    "/dev/bpf3",
    "/dev/bpf4",
    "/dev/bpf5",
    "/dev/bpf6",
    "/dev/bpf7",
    "/dev/bpf8",
    "/dev/bpf9",
    NULL
};

int pf_open(const char *iface, int len, int dlt, int hdr_complete, int imm)
{
	int fd;
	struct ifreq ifr;

	for (const char **pathp = bpf_dev_paths; *pathp != NULL; pathp++) {
		fd = open(*pathp, O_RDWR);
		if (fd != -1)
			break;
	}

	if (fd == -1) {
		warn("open");
		goto err_open;
	}

	if (ioctl(fd, BIOCSBLEN, &len) == -1) {
		warn("BIOCSBLEN");
		goto err_ioctl;
	}

	strlcpy(ifr.ifr_name, iface, sizeof(ifr.ifr_name));

	if (ioctl(fd, BIOCSETIF, &ifr) == -1) {
		warn("BIOCSETIF");
		goto err_ioctl;
	}

	if (dlt != 0) {
		if (ioctl(fd, BIOCSDLT, &dlt) == -1) {
			warn("BIOCSDLT");
			goto err_ioctl;
		}
	}

	if (ioctl(fd, BIOCSHDRCMPLT, &hdr_complete) == -1) {
		warn("BIOCSHDRCMPLT");
		goto err_ioctl;
	}

	if (ioctl(fd, BIOCIMMEDIATE, &imm) == -1) {
		warn("BIOCIMMEDIATE");
		goto err_ioctl;
	}

	return fd;

err_ioctl:
	(void)close(fd);
err_open:
	return -1;
}

int pf_set_filter(int fd, const int *ins, const int ins_len)
{
	warnx("BIOCSETF");
	// TODO
	return -1;
}

int pf_read(int fd, void *buf, int len)
{
	ssize_t nr;

	do {
		nr = read(fd, buf, len);
		if (nr == -1 && errno == EINTR)
			continue;
	} while (nr == 0);

	if (nr == -1) {
		warn("read");
		return -1;
	}

	return (int)nr;
}

int pf_write(int fd, const void *buf, int len)
{
	ssize_t nw;

	do
		nw = write(fd, buf, len);
	while (nw == -1 && errno == EINTR);

	if (nw == -1) {
		warn("write");
		return -1;
	}

	return (int)nw;
}

void pf_close(int fd)
{
	if (close(fd) == -1)
		warn("close");
}
