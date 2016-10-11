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

int pf_open(const char *iface, int dlt, char *errbuf, int errbuf_len)
{
	int fd;
	struct ifreq ifr;


	for (const char **pathp = bpf_dev_paths; *pathp != NULL; pathp++) {
		fd = open(*pathp, O_RDWR);
		if (fd != -1)
			break;
	}

	if (fd == -1)
		goto err_open;

	strlcpy(ifr.ifr_name, iface, sizeof(ifr.ifr_name));

	if (ioctl(fd, BIOCSETIF, &ifr) == -1)
		goto err_ioctl;

	if (ioctl(fd, BIOCSDLT, &dlt) == -1)
		goto err_ioctl;

	return fd;

err_ioctl:
	(void)close(fd);
err_open:
	(void)strerror_r(errno, errbuf, errbuf_len);

	return -1;
}


int pf_set_interface(int fd, const char *iface)
{
	struct ifreq ifr;

	strlcpy(ifr.ifr_name, iface, sizeof(ifr.ifr_name));

	return ioctl(fd, BIOCSETIF, &ifr);
}

int pf_set_rbuf_len(int fd, int len)
{
	return ioctl(fd, BIOCSBLEN, &len);
}

int pf_set_filter(int fd, const int *ins, const int ins_len)
{
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

	if (nr == -1)
		return -1;

	return (int)nr;
}

int pf_write(int fd, const void *buf, int len)
{
	ssize_t nw;

	do
		nw = write(fd, buf, len);
	while (nw == -1 && errno == EINTR);

	if (nw == -1)
		return -1;

	return (int)nw;
}

void pf_close(int fd)
{
	(void)close(fd);
}
