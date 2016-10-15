#ifndef _CLOJURE_PF_PF_H_
#define _CLOJURE_PF_PF_H_

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
 * On BSD-derived systems, a read from a BPF device may return
 * multiple packets with BPF headers prepended to each one. 
 * To ease packet deserializing later on, we fill the header and
 * payload buffers with the indices and sizes of each header and
 * payload respectively. To indicate the number of header and
 * payloads received, the values pointed to by nheaders and npayloads
 * will be set accordingly.
 *
 * On Linux, raw datagram based sockets should always return one packet.
 * In this case, the value pointed to by nheaders is set to zero
 * and npayloads to one.
 */
int32_t pf_read(int fd,
		uint8_t *buf, int32_t buf_size, int32_t max_packets,
		int32_t *header_indices, int32_t *header_sizes,
		int32_t *payload_indices, int32_t *payload_sizes,
		int32_t *nheaders, int32_t *npayloads);


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
