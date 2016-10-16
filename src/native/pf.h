#ifndef _CLOJURE_PF_H_
#define _CLOJURE_PF_H_

#include <stdint.h>

/*
 * Returns a file descriptor pointing to a socket or BPF device
 * that is associated with the given interface. Additionally,
 * the following options are provided:
 *  - read buffer size
 *  - data link type
 *  - header complete -flag
 *  - immediate read -flag
 */
int pf_open(const char *iface, int rsize, int dlt, int hdr_complete, int imm);

/*
 * Sets the filter program to be used by the socket/device.
 * The actual BPF program is extracted from the raw integer array.
 */
int pf_set_filter(int fd, const int *ins, int ins_len);

/*
 * Reads at most buf_size bytes from a socket/device into a designated buffer.
 *
 * To ease packet deserializing later on, we fill the payload
 * buffers with the indices and sizes of each payload.
 * To indicate the number of payloads received, the value pointed to by
 * npayloads will be set accordingly.
 *
 * On BSD-derived systems, a read from a BPF device may return
 * multiple packets with BPF headers prepended to each one,
 * which are then used to fill the seconds and microseconds -buffers.
 *
 * On Linux, raw datagram based sockets should always return one packet
 * without a BPF header. In this case, the function will generate a 
 * single timestamp with microsecond resolution, which will be written
 * to the seconds and microseconds -buffers.
 */
int32_t pf_read(int fd,
		uint8_t *buf,			int32_t buf_size,
		int32_t  max_packets,
		int64_t	*seconds,		int64_t *microseconds,
		int32_t *payload_indices,	int32_t *payload_sizes,
		int32_t *npayloads);


/*
 * Writes len bytes from a designated buffer to a socket/device.
 *
 * Returns the number of bytes written on success, -1 otherwise.
 */
int pf_write(int fd, const void *buf, int len);

/*
 * Closes a socket/device.
 */
void pf_close(int fd);

#endif
