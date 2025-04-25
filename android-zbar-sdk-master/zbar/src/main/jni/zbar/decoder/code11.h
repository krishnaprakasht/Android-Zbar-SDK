
#ifndef _CODE11_H_
#define _CODE11_H_

// Code 11 specific decoding state
typedef struct code11_decoder_s {
    unsigned direction : 1;     /* scan direction: 0=fwd, 1=rev */
    unsigned element   : 4;       /* element offset 0-4 */
    int character      : 12;         /* character position in symbol */
    unsigned s5;                /* current character width */
    unsigned width;             /* last character width */

    unsigned config;
    int configs[NUM_CFGS];      /* int valued configurations */
} code11_decoder_t;

// reset Code 11 specific state
static inline void code11_reset (code11_decoder_t *dcode11)
{
    dcode11->direction = 0;
    dcode11->element = 0;
    dcode11->character = -1;
    dcode11->s5 = 0;
}

// decode Code 11 symbols
zbar_symbol_type_t _zbar_decode_code11(zbar_decoder_t *dcode);

#endif