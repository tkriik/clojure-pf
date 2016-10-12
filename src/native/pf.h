#ifndef _CLOJURE_PF_PF_H_
#define _CLOJURE_PF_PF_H_

#include <stdint.h>

/*
 * Returns a file descriptor pointing to a socket or BPF device
 * that is associated with the given interface. Additionally,
 * the following options are provided:
 *  - data link type
 *  - header complete -flag
 *  - immediate read -flag
 */
int pf_open(const char *iface, int len, int dlt, int hdr_complete, int imm);

/*
 * Sets the filter program to be used by the socket/device.
 * The actual BPF program is extracted from the raw integer array.
 */
int pf_set_filter(int fd, const int *ins, int ins_len);

/*
 * Reads len bytes from a socket/device into the designated buffer.
 *
 * Returns the number of bytes read on success, -1 otherwise.
 */
int pf_read(int fd, void *buf, int len);

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

/*
 * Retrieves the BPF header length.
 */
int pf_header_length(void);

#endif
