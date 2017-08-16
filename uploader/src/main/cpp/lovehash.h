//
// Created by AlexS on 23.01.17.
//

#ifndef LOVECOLLAGE_ANDROID_LOVEHASH_H
#define LOVECOLLAGE_ANDROID_LOVEHASH_H

#if defined(_MSC_VER) && (_MSC_VER < 1600)

typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
typedef unsigned __int64 uint64_t;

// Other compilers

#else	// defined(_MSC_VER)

#include <stdint.h>

#endif // !defined(_MSC_VER)

//-----------------------------------------------------------------------------

void
LoveHash3_x86_32(const void *key, int len, uint32_t seed, void *out); //use it

void LoveHash3_x86_128(const void *key, int len, uint32_t seed, void *out);

void LoveHash3_x64_128(const void *key, int len, uint32_t seed, void *out);

//-----------------------------------------------------------------------------


#endif //LOVECOLLAGE_ANDROID_LOVEHASH_H
