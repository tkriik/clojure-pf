CC=		gcc

CFLAGS=		-std=c99 -pedantic -Wall -Wextra -Werror -c

SRC_BSD=	src/native/pf_bsd.c

OBJ_BSD=	pf_bsd.o

LIB=		libclojure_pf.so
LIBDIR=		resources

bsd: $(SRC_BSD)
	$(CC) $(CFLAGS) $(SRC_BSD)
	mkdir -p $(LIBDIR)
	$(CC) -shared -o $(LIBDIR)/$(LIB) $(OBJ_BSD)

.PHONY: clean

clean:
	rm -f $(OBJ_BSD)
	rm -f $(LIBDIR)/$(LIB)
