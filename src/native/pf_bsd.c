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

int
pf_open(const char *iface, int len, int dlt, int hdr_complete, int imm)
{
	int fd;
	struct ifreq ifr;

	/* Open the first available BPF device. */
	for (const char **pathp = bpf_dev_paths; *pathp != NULL; pathp++) {
		fd = open(*pathp, O_RDWR);
		if (fd != -1)
			break;
	}

	if (fd == -1) {
		warn("open");
		goto err_open;
	}

	/* Set BPF device read buffer length. */
	if (ioctl(fd, BIOCSBLEN, &len) == -1) {
		warn("BIOCSBLEN");
		goto err_ioctl;
	}

	/* Set BPF device hardware interface. */
	strlcpy(ifr.ifr_name, iface, sizeof(ifr.ifr_name));

	if (ioctl(fd, BIOCSETIF, &ifr) == -1) {
		warn("BIOCSETIF");
		goto err_ioctl;
	}

	/* If a non-zero data link type is given, attempt to use it. */
	if (dlt != 0 && ioctl(fd, BIOCSDLT, &dlt) == -1) {
		warn("BIOCSDLT");
		goto err_ioctl;
	}

	/* Set header complete -flag. */
	if (ioctl(fd, BIOCSHDRCMPLT, &hdr_complete) == -1) {
		warn("BIOCSHDRCMPLT");
		goto err_ioctl;
	}

	/* Set immediate read -flag. */
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

int
pf_set_filter(int fd, const int *ins, const int ins_len)
{
	warnx("BIOCSETF");
	// TODO
	return -1;
}

int32_t
pf_read(int fd,
    uint8_t *buf, int32_t buf_size, int32_t max_packets,
    int32_t *header_indices, int32_t *header_sizes,
    int32_t *payload_indices, int32_t *payload_sizes,
    int32_t *nheaders, int32_t *npayloads)
{
	ssize_t nr;

	/* 
	 * Attempt to read from BPF device until data is received,
	 * while ignoring signals.
	 *
	 * TODO: Modify this to account for user-defined time-outs.
	 */
	do {
		nr = read(fd, buf, buf_size);
		if (nr == -1 && errno == EINTR)
			continue;
	} while (nr == 0);

	if (nr == -1) {
		warn("read");
		return -1;
	}

	/* Zero these just to be safe. */
	*nheaders = 0;
	*npayloads = 0;

	size_t i_hdr = 0; /* packet header index */
	while (i_hdr < (size_t)nr) {
		if ((max_packets <= *nheaders) || (max_packets <= *npayloads))
			break;

		/* This should never happen. Check anyway. */
		if ((size_t)nr - i_hdr < sizeof(struct bpf_hdr)) {
			warnx("read: truncated BPF header");
			return -1;
		}

		/* XXX: suspicious */
		struct bpf_hdr *hdr = (void *)(&buf[i_hdr]);

		*header_indices++ = i_hdr;
		*header_sizes++ = hdr->bh_hdrlen;

		*payload_indices++ = i_hdr + hdr->bh_hdrlen;
		*payload_sizes++ = hdr->bh_caplen;

		(*nheaders)++;
		(*npayloads)++;

		/* Increment header index to the start of the next packet. */
		i_hdr += BPF_WORDALIGN(hdr->bh_hdrlen + hdr->bh_caplen);
	}

	return nr;
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
