#include <config.h>
#include <string.h>
#include <time.h>

#include <zbar.h>

#ifdef DEBUG_CODE11
# define DEBUG_LEVEL (DEBUG_CODE11)
#endif

#include "debug.h"
#include "decoder.h"

// Define a compile switch for logging
#define TIME_PROFILE_ON

#include <android/log.h>
#define LOG_TAG "JNI_LOG"

#ifdef TIME_PROFILE_ON
#define LOG_PRINT(fmt, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##__VA_ARGS__)
#else
#define LOG_PRINT(fmt, ...) ((void)0)  // No-op when logging is disabled
#endif

#define NUM_CODE11_CHARS (0x0c)


// Define a global or static struct to store timestamps
static struct timespec start_time, end_time;

//static const unsigned char code11_characters[NUM_CODE11_CHARS] = "9187546320-*";
static const unsigned char code11_characters[11] = "0123456789-";
static const unsigned short code11_symbols[NUM_CODE11_CHARS] = {
        0x01,
        0x11,
        0x09,
        0x18,
        0x05,
        0x14,
        0x0c,
        0x03,
        0x12,
        0x10,
        0x04,
        0x06,
};
typedef struct char11_s {
    unsigned char chk,rev,fwd;
} char11_t;


static const char11_t code11_encodings[NUM_CODE11_CHARS] = {
        {0x01,0x10,0x00},
        {0x11,0x11,0x01},
        {0x09,0x12,0x02},
        {0x18,0x03,0x03},
        {0x05,0x14,0x04},
        {0x14,0x05,0x05},
        {0x0c,0x06,0x06},
        {0x03,0x18,0x07},
        {0x12,0x09,0x08},
        {0x10,0x01,0x09},
        {0x04,0x04,0x0a},
        {0x06,0x0d,0x0b},
};

static inline unsigned char code11_decode1(unsigned char enc, unsigned e, unsigned s)
{

    unsigned char E = decode_e(e, s,20);
    if(E > 9){
        return(0xff);}
    enc <<= 1;
    if(E > 1.9) {
        enc |= 1;
        dbprintf(2, "1");
    }
    else {
        dbprintf(2, "0");
    }
    return(enc);
}

static inline signed char code11_decode5(zbar_decoder_t *dcode)
{
    code11_decoder_t *dcode11 = &dcode->code11;
    if(dcode11->s5 < 5){
        return(-1);}

    /* threshold bar width ratios */
    unsigned char i,j,enc = 0;
    for(i = 0; i < 5; i++) {

        enc = code11_decode1(enc, get_width(dcode, i), dcode11->s5);
        if(enc == 0xff){
            return(-1);}
    }

    zassert(enc < 0x20, -1, " enc=%x s5=%x\n", enc, dcode11->s5);

    for (j = 0; j < 12; j++) {
        if(code11_symbols[j] == enc) {
            const char11_t *c = &code11_encodings[j];
            if(enc == c->chk) {
                dcode11->width = dcode11->s5;
                return c->fwd;
            }
        }
    }
    return(-1);
}

static inline signed char code11_decode_start(zbar_decoder_t *dcode){
    code11_decoder_t *dcode11 = &dcode->code11;
    dbprintf(2," s=%d",dcode11->s5);

    //detecting start code
    signed char c = code11_decode5(dcode);

    if(c != 0x0b){
        dbprintf(2,"\n");
        return(ZBAR_NONE);
    }

    //check leading quiet zone
    unsigned quiet = get_width(dcode,5);
    if(quiet && quiet < dcode11->s5 / 2){
        dbprintf(2,"Invalid quiet\n");
        return(ZBAR_NONE);
    }

    dcode11->element = 5;
    dcode11->character = 0;
    return(ZBAR_PARTIAL);
}

static inline int code11_postprocess (zbar_decoder_t *dcode){
    code11_decoder_t *dcode11 = &dcode->code11;
    int i;
    for (i = 0; i < dcode11->character / 2; i++) {
        unsigned j = dcode11->character - 1 - i;
        char code = dcode->buf[i];
        dcode->buf[i] = dcode->buf[j];
        dcode->buf[j] = code;
    }

    for(i = 0; i < dcode11->character; i++){
        dcode->buf[i] = ((dcode->buf[i] < 0x0b) ? code11_characters[(unsigned)dcode->buf[i]] : '?');
    }
    zassert(i < dcode->buf_alloc, -1, "i=%02x %s\n", i,
            _zbar_decoder_buf_dump(dcode->buf, dcode11->character));

    dcode->buflen = i;
    dcode->buf[i] = '\0';
    dcode->modifiers = 0;
    return(0);

}

static inline int check_width (unsigned ref,unsigned w)
{
    unsigned dref = ref;
    ref *= 4;
    w *= 4;
    return(ref - dref <= w && w <= ref + dref);
}

zbar_symbol_type_t _zbar_decode_code11(zbar_decoder_t *dcode){

    code11_decoder_t *dcode11 = &dcode->code11;
    signed char c;
    //update latest character width
    dcode11->s5 -= get_width(dcode,5);
    dcode11->s5 += get_width(dcode,0);
    // Start timestamp when detection begins
    clock_gettime(CLOCK_MONOTONIC, &start_time);
    //decode the start character
    if(dcode11->character < 0){
        if(get_color(dcode) != ZBAR_BAR){
            return(ZBAR_NONE);
        }
        dbprintf(2,"  code 11");
        return(code11_decode_start(dcode));
    }

    //check for correct decoding
    if (++dcode11->element < 5) {
        return (ZBAR_NONE);
    }

    dbprintf(2, "      code11[%02d+%x]",dcode11->character, dcode11->element);

    if(dcode11->element == 6) {
        unsigned space = get_width(dcode, 0);
        if(dcode11->character &&
           dcode->buf[dcode11->character - 1] == 0x0b) {  /* STOP */
            /* trim STOP character */
            zbar_symbol_type_t sym;
            dcode11->character--;
            sym = ZBAR_NONE;

            //trailing quiet zone check
            if (space && space < dcode11->width / 2)
                dbprintf(2, " [invalid qz]\n");


            else if (dcode11->character < CFG(*dcode11, ZBAR_CFG_MIN_LEN) ||
                       (CFG(*dcode11, ZBAR_CFG_MAX_LEN) > 0 &&
                        dcode11->character > CFG(*dcode11, ZBAR_CFG_MAX_LEN)))
                dbprintf(2, " [invalid len]\n");
            else if (!code11_postprocess(dcode)) {
                /* FIXME checksum */
                dbprintf(2, " [valid end]\n");
                sym = ZBAR_CODE11;
            }

            //preparing for next read
            dcode11->character = -1;
            if(!sym)
                release_lock(dcode, ZBAR_CODE11);
            // Stop timestamp when decoding is finished
            clock_gettime(CLOCK_MONOTONIC, &end_time);
            // Calculate elapsed time in milliseconds
            double elapsed_time = (end_time.tv_sec - start_time.tv_sec) * 1000.0 +
                                  (end_time.tv_nsec - start_time.tv_nsec) / 1000000.0;
            LOG_PRINT( "Chiru: Time taken for decoding code11 barcode = %.4f ms",elapsed_time);
            const char* str = doubleToString(elapsed_time);
            dcode->elapsed_time = str;
            return(sym);
        }
        if(space > dcode11->width / 2) {
            /* inter-character space check failure */
            dbprintf(2, " ics>%d [invalid ics]", dcode11->width);
            if(dcode11->character)
                release_lock(dcode, ZBAR_CODE11);
            dcode11->character = -1;
        }
        dcode11->element = 0;
        dbprintf(2, "\n");
        return(ZBAR_NONE);
    }

    dbprintf(2, " s=%d ", dcode11->s5);
    if(!check_width(dcode11->width, dcode11->s5)) {
        dbprintf(2, " [width]\n");
        if(dcode11->character)
            release_lock(dcode, ZBAR_CODE11);
        dcode11->character = -1;
        return(ZBAR_NONE);
    }

    c = code11_decode5(dcode);
    dbprintf(2, " c=%d", c);

    /* lock shared resources */
    if(!dcode11->character && acquire_lock(dcode, ZBAR_CODE11)) {
        dcode11->character = -1;
        return(ZBAR_PARTIAL);
    }

    if(c < 0 || size_buf(dcode, dcode11->character + 1)) {
        dbprintf(1, (c < 0) ? " [aborted]\n" : " [overflow]\n");
        release_lock(dcode, ZBAR_CODE11);
        dcode11->character = -1;
        return(ZBAR_NONE);
    }else {
        zassert(c < 0x0c, ZBAR_NONE, "c=%02x s5=%x\n", c, dcode11->s5);
        dbprintf(2, "\n");
    }
    dcode->buf[dcode11->character++] = c;
    return(ZBAR_NONE);
}












