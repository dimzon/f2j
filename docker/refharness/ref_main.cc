// Reference differential-test harness for the f2j Java port.
//
// Links against ALL upstream engine objects (except main.cc.o) and prints
// deterministic vectors for pure-ish engine functions. The Java port
// reproduces these vectors byte-for-byte; see golden/v1.txt.
//
// Build & run inside the f2j-ref container:
//   docker/refharness/build-refharness.ps1   (from repo root on the host)
//
// NOTE: only functions that do not require game data / video init are used.
// Randomness is seeded explicitly, so output is fully deterministic.

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <zlib.h>

#include <SDL.h>

#include "color.h"
#include "cycle.h"
#include "delay.h"
#include "fps_limiter.h"
#include "dictionary.h"
#include "art.h"
#include "cache.h"
#include "color.h"
#include "config.h"
#include "content_config.h"
#include "dfile.h"
#include "draw.h"
#include "game_config.h"
#include "input.h"
#include "message.h"
#include "settings.h"
#include "svga.h"
#include "file_utils.h"
#include "fpattern_windows.h"
#include "xfile.h"

namespace {

// Progress probe for dbGetFileContents accounting.
int gProgressCalls = 0;
void progressHandler()
{
    gProgressCalls++;
}

} // namespace
#include "geometry.h"
#include "platform_compat.h"
#include "win32.h"
#include "random.h"
#include "string_parsers.h"
#include "text_font.h"
#include "word_wrap.h"

namespace fallout {
// win32.cc (excluded from the harness link: it defines main()) owns this
// global; our vectors never touch video/input, so a fixed `true` is fine.
bool gProgramIsActive = true;
} // namespace fallout

// Fixed-metric fake font for wordWrap vectors (no game data needed).
// Mirrors how fontSetCurrent wires FontManager procs.
namespace {

int fakeCharWidth(int ch)
{
    (void)ch;
    return 5;
}

int fakeSpacing()
{
    return 1;
}

int fakeMonoCharWidth()
{
    return 6;
}

int fakeStringWidth(const char* s)
{
    return (int)strlen(s) * 6;
}

void fakeSetCurrent(int font)
{
    (void)font;
}

void installFakeFont()
{
    static bool installed = false;
    if (installed) {
        return;
    }
    fallout::FontManager fake {};
    fake.minFont = 0;
    fake.maxFont = 9;
    fake.setCurrentProc = fakeSetCurrent;
    fake.drawTextProc = nullptr;
    fake.getLineHeightProc = nullptr;
    fake.getStringWidthProc = fakeStringWidth;
    fake.getCharacterWidthProc = fakeCharWidth;
    fake.getMonospacedStringWidthProc = nullptr;
    fake.getLetterSpacingProc = fakeSpacing;
    fake.getBufferSizeProc = nullptr;
    fake.getMonospacedCharacterWidthProc = fakeMonoCharWidth;
    fallout::fontManagerAdd(&fake);
    fallout::fontSetCurrent(1);
    installed = true;
}

void printRect(const char* label, const fallout::Rect& r)
{
    std::printf("%s left=%d top=%d right=%d bottom=%d\n", label, r.left, r.top, r.right, r.bottom);
}

void sectionRgb555()
{
    std::puts("[rgb555]");
    const int inputs[] = { 0x000000, 0xFFFFFF, 0xFF0000, 0x00FF00, 0x0000FF, 0xFF5252, 0xC5D6FF, 0x123456, 0x7B7B29 };
    for (int rgb : inputs) {
        std::printf("rgb555 %06X=%d\n", rgb, fallout::rgb555(rgb));
    }
}

void sectionRectUnion()
{
    std::puts("[rectUnion]");
    const fallout::Rect cases[][2] = {
        { { 0, 0, 10, 10 }, { 5, 5, 15, 15 } },
        { { -5, -5, 0, 0 }, { 10, 10, 20, 20 } },
        { { 3, 3, 3, 3 }, { 3, 3, 3, 3 } },
    };
    for (const auto& c : cases) {
        fallout::Rect r {};
        fallout::rectUnion(&c[0], &c[1], &r);
        std::printf("union {(%d,%d)-(%d,%d)} {(%d,%d)-(%d,%d)} -> (%d,%d)-(%d,%d)\n",
            c[0].left, c[0].top, c[0].right, c[0].bottom,
            c[1].left, c[1].top, c[1].right, c[1].bottom,
            r.left, r.top, r.right, r.bottom);
    }
}

void sectionRectIntersection()
{
    std::puts("[rectIntersection]");
    const fallout::Rect cases[][2] = {
        { { 0, 0, 10, 10 }, { 5, 5, 15, 15 } },
        { { 0, 0, 10, 10 }, { 20, 20, 30, 30 } },
        { { 0, 0, 10, 10 }, { 0, 0, 10, 10 } },
        { { 0, 0, 10, 10 }, { 10, 10, 20, 20 } }, // edge-touching
    };
    for (const auto& c : cases) {
        fallout::Rect r {};
        int rc = fallout::rectIntersection(&c[0], &c[1], &r);
        std::printf("isect rc=%d {(%d,%d)-(%d,%d)} {(%d,%d)-(%d,%d)} -> (%d,%d)-(%d,%d)\n",
            rc,
            c[0].left, c[0].top, c[0].right, c[0].bottom,
            c[1].left, c[1].top, c[1].right, c[1].bottom,
            r.left, r.top, r.right, r.bottom);
    }
}

void sectionRectClip()
{
    std::puts("[rectClip]");
    // Overlapping hole: b minus t yields 4 surround rects.
    {
        fallout::Rect b { 0, 0, 20, 20 };
        fallout::Rect t { 5, 5, 10, 10 };
        int n = 0;
        for (fallout::RectListNode* node = fallout::rect_clip(&b, &t); node != nullptr; node = node->next) {
            char label[32];
            std::snprintf(label, sizeof(label), "clip-hole[%d]", n++);
            printRect(label, node->rect);
        }
        std::printf("clip-hole count=%d\n", n);
    }
    // No overlap: single rect equal to b.
    {
        fallout::Rect b { 0, 0, 20, 20 };
        fallout::Rect t { 30, 30, 40, 40 };
        int n = 0;
        for (fallout::RectListNode* node = fallout::rect_clip(&b, &t); node != nullptr; node = node->next) {
            char label[32];
            std::snprintf(label, sizeof(label), "clip-miss[%d]", n++);
            printRect(label, node->rect);
        }
        std::printf("clip-miss count=%d\n", n);
    }
}

void sectionRandom()
{
    std::puts("[random]");
    fallout::randomSeedPrerandom(12345);
    for (int i = 0; i < 10; i++) {
        std::printf("rand100[%d]=%d\n", i, fallout::randomBetween(1, 100));
    }
    fallout::randomSeedPrerandom(1);
    for (int i = 0; i < 5; i++) {
        std::printf("rand25[%d]=%d\n", i, fallout::randomBetween(1, 25));
    }
    // Degenerate + reversed ranges (see randomBetween min/max handling).
    std::printf("rand-degenerate=%d\n", fallout::randomBetween(10, 10));
    fallout::randomSeedPrerandom(7);
    std::printf("rand-reversed=%d\n", fallout::randomBetween(5, 3));
}

void sectionStrParsers()
{
    std::puts("[strParsers]");

    {
        char buf[64] = "  12, 34,abc";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseInt(&cursor, &value);
        std::printf("parseInt rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        char buf[64] = ",";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseInt(&cursor, &value);
        std::printf("parseInt-empty rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        const char* list[] = { "one", "two", "three" };
        char buf[64] = "Two, x";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseStrFromList(&cursor, &value, list, 3);
        std::printf("parseList rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        const char* list[] = { "one", "two", "three" };
        char buf[64] = "seven";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseStrFromList(&cursor, &value, list, 3);
        std::printf("parseList-miss rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        char buf[64] = "count=3, x=4";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseIntWithKey(&cursor, "count", &value, "=");
        std::printf("parseWithKey rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        char buf[64] = "x=4";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseIntWithKey(&cursor, "count", &value, "=");
        std::printf("parseWithKey-miss rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        char buf[64] = "count=3, x=4";
        char* cursor = buf;
        char key[64] = {};
        int value = -99;
        int rc = fallout::strParseKeyValue(&cursor, key, &value, "=");
        std::printf("parseKeyValue rc=%d key=[%s] value=%d rest=[%s]\n", rc, key, value, cursor);
    }
}

int yesNoCallback(char* string, int* valuePtr)
{
    if (std::strcmp(string, "yes") == 0) {
        *valuePtr = 42;
        return 0;
    }
    return 1;
}

void sectionStrParseFunc()
{
    std::puts("[strParseFunc]");
    {
        char buf[64] = "YES, next";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseStrFromFunc(&cursor, &value, yesNoCallback);
        std::printf("parseFunc rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
    {
        char buf[64] = "nope";
        char* cursor = buf;
        int value = -99;
        int rc = fallout::strParseStrFromFunc(&cursor, &value, yesNoCallback);
        std::printf("parseFunc-miss rc=%d value=%d rest=[%s]\n", rc, value, cursor);
    }
}

void sectionWordWrap()
{
    std::puts("[wordWrap]");
    installFakeFont();

    const struct {
        const char* text;
        int width;
    } cases[] = {
        { "hi", 100 },
        { "hi", 3 },
        { "aa bb cc", 20 },
        { "abcdefgh", 20 },
        { "aa-bb cc", 20 },
        { "", 100 },
        { "hi", 6 },
    };
    for (const auto& c : cases) {
        short breakpoints[WORD_WRAP_MAX_COUNT];
        short len = 0;
        int rc = fallout::wordWrap(c.text, c.width, breakpoints, &len);
        std::printf("wrap rc=%d len=%d text=[%s] width=%d bps=", rc, len, c.text, c.width);
        for (int i = 0; i < len; i++) {
            std::printf("%s%d", i ? "," : "", breakpoints[i]);
        }
        std::puts("");
    }
}

} // namespace

void dumpDict(const char* label, fallout::Dictionary* d)
{
    std::printf("%s length=%d capacity=%d valuesize=%d order=",
        label, d->entriesLength, d->entriesCapacity, (int)d->valueSize);
    for (int i = 0; i < d->entriesLength; i++) {
        std::printf("%s%s", i ? "," : "", d->entries[i].key);
    }
    std::puts("");
    for (int i = 0; i < d->entriesLength; i++) {
        std::printf("%s bytes=", d->entries[i].key);
        unsigned char* bytes = (unsigned char*)d->entries[i].value;
        if (bytes != nullptr) {
            for (size_t k = 0; k < d->valueSize; k++) {
                std::printf("%02X", bytes[k]);
            }
        } else {
            std::printf("(null)");
        }
        std::puts("");
    }
}

void sectionDict()
{
    std::puts("[dict]");

    fallout::Dictionary d {};
    std::printf("uninit-get=%d\n", fallout::dictionaryGetIndexByKey(&d, "a"));
    std::printf("uninit-add=%d\n", fallout::dictionaryAddValue(&d, "a", nullptr));
    std::printf("uninit-free=%d\n", fallout::dictionaryFree(&d));

    fallout::DictionaryIO noIo { nullptr, nullptr };
    std::printf("init=%d\n", fallout::dictionaryInit(&d, 0, 4, &noIo));

    unsigned char vDelta[4] = { 4, 0, 0, 0 };
    unsigned char vAlpha[4] = { 1, 0, 0, 0 };
    unsigned char vCharlie[4] = { 3, 0, 0, 0 };
    unsigned char vBravo[4] = { 2, 0, 0, 0 };
    fallout::dictionaryAddValue(&d, "delta", vDelta);
    fallout::dictionaryAddValue(&d, "alpha", vAlpha);
    fallout::dictionaryAddValue(&d, "CHARLIE", vCharlie);
    fallout::dictionaryAddValue(&d, "bravo", vBravo);
    dumpDict("added", &d);

    std::printf("get-ALPHA=%d\n", fallout::dictionaryGetIndexByKey(&d, "ALPHA"));
    std::printf("get-missing=%d\n", fallout::dictionaryGetIndexByKey(&d, "zzz"));
    std::printf("add-dup=%d\n", fallout::dictionaryAddValue(&d, "Alpha", vAlpha));

    std::printf("remove-bravo=%d\n", fallout::dictionaryRemoveValue(&d, "BRAVO"));
    dumpDict("removed", &d);
    std::printf("remove-missing=%d\n", fallout::dictionaryRemoveValue(&d, "bravo"));

    fallout::Dictionary c {};
    std::printf("copy=%d\n", fallout::dictionaryCopy(&c, &d));
    dumpDict("copied", &c);

    std::printf("shrink=%d\n", fallout::dictionarySetCapacity(&d, 1));
    std::printf("grow=%d\n", fallout::dictionarySetCapacity(&d, 10));
    std::printf("cap=%d\n", d.entriesCapacity);

    std::printf("free=%d\n", fallout::dictionaryFree(&d));
    std::printf("free-copy=%d\n", fallout::dictionaryFree(&c));
    std::printf("use-after-free=%d\n", fallout::dictionaryGetIndexByKey(&d, "alpha"));
}

void sectionDictFile()
{
    std::puts("[dictFile]");

    fallout::Dictionary d {};
    fallout::dictionaryInit(&d, 0, 4, nullptr);
    unsigned char vAlpha[4] = { 1, 2, 3, 4 };
    unsigned char vBeta[4] = { 5, 6, 7, 8 };
    fallout::dictionaryAddValue(&d, "alpha", vAlpha);
    fallout::dictionaryAddValue(&d, "beta", vBeta);

    const char* tmpPath = "/tmp/dict.bin";
    // Fixture for the Java port (loaded by DictionaryTest from resources).
    const char* fixturePath = "/src/java/engine/src/test/resources/fixtures/dict-v1.bin";

    FILE* out = fopen(tmpPath, "wb");
    std::printf("write=%d\n", fallout::dictionaryWrite(out, &d, 0));
    fclose(out);

    out = fopen(fixturePath, "wb");
    if (out != nullptr) {
        fallout::dictionaryWrite(out, &d, 0);
        fclose(out);
        std::puts("fixture-written");
    } else {
        std::puts("fixture-SKIPPED");
    }

    fallout::Dictionary e {};
    fallout::dictionaryInit(&e, 0, 0, nullptr);
    FILE* in = fopen(tmpPath, "rb");
    std::printf("load=%d\n", fallout::dictionaryLoad(in, &e, 0));
    fclose(in);
    dumpDict("loaded", &e);

    fallout::dictionaryFree(&d);
    fallout::dictionaryFree(&e);
}

void sectionPaths()
{
    std::puts("[paths]");

    const char* splitCases[] = {
        "/a/b/c.txt",
        "a\\b\\c",
        "//server/share/f.txt",
        "noext",
        "a/.h",
        "a/b.",
    };
    for (const char* input : splitCases) {
        char drive[COMPAT_MAX_DRIVE] = {};
        char dir[COMPAT_MAX_DIR] = {};
        char fname[COMPAT_MAX_FNAME] = {};
        char ext[COMPAT_MAX_EXT] = {};
        fallout::compat_splitpath(input, drive, dir, fname, ext);
        std::printf("split [%s] -> drive=[%s] dir=[%s] fname=[%s] ext=[%s]\n",
            input, drive, dir, fname, ext);
    }

    struct MakeCase {
        const char* drive;
        const char* dir;
        const char* fname;
        const char* ext;
    };
    const MakeCase makeCases[] = {
        { "", "/a/b/", "c", ".txt" },
        { "", "a\\b\\", "c", "txt" },
        { "//server", "/share/", "f", ".txt" },
        { "", "", "c", "" },
        { "C:", "\\a\\", "", ".txt" },
    };
    for (const auto& c : makeCases) {
        char path[COMPAT_MAX_PATH] = {};
        fallout::compat_makepath(path, c.drive, c.dir, c.fname, c.ext);
        std::printf("make drive=[%s] dir=[%s] fname=[%s] ext=[%s] -> [%s]\n",
            c.drive, c.dir, c.fname, c.ext, path);
    }

    {
        char path[COMPAT_MAX_PATH] = "a\\b/c\\d.txt";
        fallout::compat_windows_path_to_native(path);
        std::printf("native [%s]\n", path);
    }

    // Case-insensitive resolution against a real fixture tree.
    // Built by the build-refharness container command (see below).
    const char* resolveCases[] = {
        "/tmp/f2resolve/subdir/mixedcase.txt",
        "/tmp/f2resolve/SUBDIR/MIXEDCASE.TXT",
        "/tmp/f2resolve/subdir/nope/x",
        "/tmp/f2resolve/nonexistent/y",
    };
    for (const char* input : resolveCases) {
        char path[COMPAT_MAX_PATH] = {};
        snprintf(path, sizeof(path), "%s", input);
        fallout::compat_resolve_path(path);
        std::printf("resolve [%s] -> [%s]\n", input, path);
    }
}

void sectionFileUtils()
{
    std::puts("[fileUtils]");

    const char* plain = "/tmp/fu-plain.txt";
    const char* gz = "/tmp/fu-arch.gz";
    const char* back = "/tmp/fu-back.txt";
    const char* missing = "/tmp/fu-nope.txt";

    {
        FILE* f = fopen(plain, "wb");
        fputs("hello world\n", f);
        fclose(f);
    }

    std::printf("decompress-plain=%d\n", fallout::fileCopyDecompressed(plain, back));
    {
        FILE* f = fopen(back, "rb");
        char buf[64] = {};
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        std::printf("decompress-plain-content n=%d [%s]\n", (int)n, buf);
    }

    std::printf("compress=%d\n", fallout::fileCopyCompressed(plain, gz));
    {
        FILE* f = fopen(gz, "rb");
        int b0 = fgetc(f);
        int b1 = fgetc(f);
        fclose(f);
        std::printf("compress-magic=%02X%02X\n", b0, b1);
    }

    std::printf("decompress-gz=%d\n", fallout::fileCopyDecompressed(gz, back));
    {
        FILE* f = fopen(back, "rb");
        char buf[64] = {};
        size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        std::printf("decompress-gz-content n=%d [%s]\n", (int)n, buf);
    }

    std::printf("missing=%d\n", fallout::fileCopyDecompressed(missing, back));
    std::printf("missing-gz=%d\n", fallout::_gzdecompress_file(missing, back));
}

void sectionPattern()
{
    std::puts("[pattern]");
    // fpattern_windows_match (DOS variant, DELIM=0): 1 = match.
    const struct {
        const char* pat;
        const char* name;
    } cases[] = {
        { "text\\english\\game\\*.msg", "text\\english\\game\\misc.msg" },
        { "text\\english\\game\\*.msg", "text\\english\\cuts\\elder.sve" },
        { "*.msg", "misc.msg" },
        { "*.msg", "a\\misc.msg" },
        { "?", "a" },
        { "?", "" },
        { "?", "ab" },
        { "f??.txt", "foo.txt" },
        { "a*c", "ax-yyy.c" },
        { "a[0-9]z", "a1z" },
        { "a[!0-9]z", "aoz" },
        { "a[!0-9]z", "a1z" },
        { "a`?b", "a?b" },
        { "a``b", "a`b" },
        { "!a", "b" },
        { "!a", "a" },
        { "", "" },
        { "*", "" },
        { "", "a" },
        { "a\x1A"
          "c",
            "axc" },
        { "a\x1A"
          "c",
            "ax-yyy.c" },
        { "a[x-]b", "a-b" },
        { "a[!-]b", "a-b" },
        { "abc", "abc" },
        { "abc", "abd" },
        { "MISC.MSG", "misc.msg" },
        { "text\\english\\game\\misc.msg", "TEXT\\ENGLISH\\GAME\\MISC.MSG" },
    };
    for (const auto& c : cases) {
        std::printf("match [%s] [%s] -> %d\n", c.pat, c.name, fpattern_windows_match(c.pat, c.name));
    }
    std::printf("isvalid [a[] -> %d\n", fpattern_windows_isvalid("a["));
    std::printf("isvalid [a] -> %d\n", fpattern_windows_isvalid("a"));
}

void dumpEntry(fallout::DBase* db, const char* entryPath)
{
    fallout::DFile* f = fallout::dfileOpen(db, entryPath, "rb");
    if (f == nullptr) {
        std::printf("entry [%s] open=NULL\n", entryPath);
        return;
    }
    long size = fallout::dfileGetSize(f);
    unsigned char* data = (unsigned char*)malloc(size > 0 ? size : 1);
    long got = 0;
    if (size > 0) {
        size_t remaining = (size_t)size;
        while (remaining > 0) {
            size_t n = fallout::dfileRead(data + got, 1, remaining, f);
            if (n == 0) {
                break;
            }
            got += n;
            remaining -= n;
        }
    }
    uLong crc = crc32(0L, Z_NULL, 0);
    if (got > 0) {
        crc = crc32(crc, data, (uInt)got);
    }
    std::printf("entry [%s] size=%ld got=%ld crc=%08lX eof=%d\n",
        entryPath, size, got, crc, fallout::dfileEof(f) != 0);

    // Seek/tell probe.
    fallout::dfileRewind(f);
    unsigned char head[16] = {};
    fallout::dfileRead(head, 1, sizeof(head) > (size_t)size ? (size_t)size : sizeof(head), f);
    std::printf("entry [%s] tell=%ld head=", entryPath, fallout::dfileTell(f));
    long shown = size < 16 ? size : 16;
    for (long i = 0; i < shown; i++) {
        std::printf("%02X", head[i]);
    }
    std::puts("");
    int seekEndRc = fallout::dfileSeek(f, -1, SEEK_END);
    int seekSizeRc = fallout::dfileSeek(f, size, SEEK_SET);
    std::printf("entry [%s] seek-end=%d seek-size=%d\n", entryPath, seekEndRc, seekSizeRc);

    // Stale-position probe: uncompressed non-zero seeks move the file
    // pointer but not the tracked position (mirrored quirk).
    fallout::DFile* s = fallout::dfileOpen(db, entryPath, "rb");
    if (s != nullptr) {
        unsigned char tmp[4] = {};
        fallout::dfileRead(tmp, 1, 4, s);
        std::printf("entry [%s] after-read4 tell=%ld\n", entryPath, fallout::dfileTell(s));
        int seekRc = fallout::dfileSeek(s, 100, SEEK_SET);
        long seekTell = fallout::dfileTell(s);
        std::printf("entry [%s] seek100=%d tell=%ld\n", entryPath, seekRc, seekTell);
        unsigned char four[4] = {};
        size_t n = fallout::dfileRead(four, 1, 4, s);
        std::printf("entry [%s] post-seek-read n=%d tell=%ld bytes=%02X%02X%02X%02X\n", entryPath,
            (int)n, fallout::dfileTell(s), four[0], four[1], four[2], four[3]);
        fallout::dfileClose(s);
    }

    fallout::dfileClose(f);
    free(data);
}

void sectionDbase()
{
    std::puts("[dbase]");

    int errorFlags = 0;
    fallout::DBase* master = fallout::dbaseOpen("/game-en/master.dat", &errorFlags);
    std::printf("open-master=%d format=%d entries=%d errflags=%d\n",
        master != nullptr, master != nullptr ? (int)master->format : -1,
        master != nullptr ? master->entriesLength : -1, errorFlags);

    fallout::DBase* patch = fallout::dbaseOpen("/game-en/patch000.dat", &errorFlags);
    std::printf("open-patch=%d format=%d entries=%d\n",
        patch != nullptr, patch != nullptr ? (int)patch->format : -1,
        patch != nullptr ? patch->entriesLength : -1);

    fallout::DBase* missing = fallout::dbaseOpen("/game-en/nope.dat", &errorFlags);
    std::printf("open-missing=%d\n", missing != nullptr);

    {
        FILE* tmp = fopen("/tmp/empty.dat", "wb");
        fclose(tmp);
        errorFlags = 0;
        fallout::DBase* empty = fallout::dbaseOpen("/tmp/empty.dat", &errorFlags);
        std::printf("open-empty=%d errflags=%d\n", empty != nullptr, errorFlags);
    }

    if (master != nullptr) {
        fallout::DFileFindData fd {};
        int msgCount = 0;
        char first[3][256] = {};
        if (fallout::dbaseFindFirstEntry(master, &fd, "text\\english\\game\\*.msg")) {
            do {
                if (msgCount < 3) {
                    snprintf(first[msgCount], sizeof(first[msgCount]), "%s", fd.fileName);
                }
                msgCount++;
            } while (msgCount <= master->entriesLength + 1 && fallout::dbaseFindNextEntry(master, &fd));
        }
        std::printf("find-msgs count=%d first=[%s][%s][%s]\n", msgCount, first[0], first[1], first[2]);

        fallout::DFileFindData fd2 {};
        std::printf("find-none=%d\n",
            fallout::dbaseFindFirstEntry(master, &fd2, "zzz_no_such\\*.qqq") ? 1 : 0);

        dumpEntry(master, "text\\english\\game\\misc.msg");
        dumpEntry(master, "art\\intrface\\mainmenu.frm");
        dumpEntry(master, "no\\such\\file.txt");

        // Text-mode read (exercises \r\n collapsing path).
        fallout::DFile* t = fallout::dfileOpen(master, "text\\english\\game\\misc.msg", "rt");
        if (t != nullptr) {
            char line[128] = {};
            char* r = fallout::dfileReadString(line, sizeof(line), t);
            std::printf("textline=[%s]\n", r != nullptr ? line : "(null)");
            fallout::dfileClose(t);
        }

        fallout::DFile* w = fallout::dfileOpen(master, "text\\english\\game\\misc.msg", "wb");
        std::printf("open-write-mode=%d\n", w != nullptr);
        if (w != nullptr) {
            fallout::dfileClose(w);
        }

        fallout::dbaseClose(master);
    }

    if (patch != nullptr) {
        fallout::DFileFindData fd {};
        int count = 0;
        char first[256] = {};
        if (fallout::dbaseFindFirstEntry(patch, &fd, "*")) {
            snprintf(first, sizeof(first), "%s", fd.fileName);
            count = 1;
            while (count <= patch->entriesLength + 1 && fallout::dbaseFindNextEntry(patch, &fd)) {
                count++;
            }
        }
        std::printf("patch-find-all count=%d first=[%s]\n", count, first);
        fallout::dbaseClose(patch);
    }
}

void sectionZip()
{
    std::puts("[zip]");

    int errorFlags = 0;
    fallout::DBase* zip = fallout::dbaseOpen("/tmp/f2test.zip", &errorFlags);
    std::printf("open-zip=%d format=%d entries=%d errflags=%d\n",
        zip != nullptr, zip != nullptr ? (int)zip->format : -1,
        zip != nullptr ? zip->entriesLength : -1, errorFlags);

    if (zip != nullptr) {
        dumpEntry(zip, "a.txt");
        dumpEntry(zip, "sub\\b.txt");
        fallout::dbaseClose(zip);
    }

    errorFlags = 0;
    fallout::DBase* empty = fallout::dbaseOpen("/tmp/f2empty.zip", &errorFlags);
    std::printf("open-empty-zip=%d errflags=%d\n", empty != nullptr, errorFlags);
}

void sectionFixtureDat()
{
    std::puts("[fixtureDat]");

    // Committed fixture (also loaded by the Java tests).
    fallout::DBase* db = fallout::dbaseOpen(
        "/src/java/engine/src/test/resources/fixtures/f2j-test.dat", nullptr);
    std::printf("open-fixture=%d format=%d entries=%d\n",
        db != nullptr, db != nullptr ? (int)db->format : -1,
        db != nullptr ? db->entriesLength : -1);
    if (db == nullptr) {
        return;
    }

    dumpEntry(db, "medium.txt");
    dumpEntry(db, "big.txt");
    dumpEntry(db, "empty.txt");

    fallout::DFileFindData fd {};
    std::printf("fixture-find-nested=%d [%s]\n",
        fallout::dbaseFindFirstEntry(db, &fd, "sub\\*") ? 1 : 0, fd.fileName);

    fallout::dbaseClose(db);
}

void dumpXfile(const char* path)
{
    fallout::File* f = fallout::fileOpen(path, "rb");
    if (f == nullptr) {
        std::printf("xfile [%s] open=NULL\n", path);
        return;
    }
    long size = fallout::fileGetSize(f);
    // NOTE: GZFILE reports size 0 (quirk); still probe bounded content.
    size_t cap = size > 0 ? (size_t)size : 256;
    unsigned char* data = (unsigned char*)malloc(cap > 0 ? cap : 1);
    long got = 0;
    {
        size_t remaining = cap;
        while (remaining > 0) {
            size_t n = fallout::fileRead(data + got, 1, remaining, f);
            if (n == 0) {
                break;
            }
            got += n;
            remaining -= n;
        }
    }
    uLong crc = crc32(0L, Z_NULL, 0);
    if (got > 0) {
        crc = crc32(crc, data, (uInt)got);
    }
    std::printf("xfile [%s] size=%ld got=%ld crc=%08lX\n", path, size, got, crc);
    fallout::fileClose(f);
    free(data);
}

void sectionXbase()
{
    std::puts("[xbase]");
    std::printf("open-master=%d\n", fallout::xbaseOpen("/game-en/master.dat"));
    std::printf("open-patch=%d\n", fallout::xbaseOpen("/game-en/patch000.dat"));
    std::printf("open-dira=%d\n", fallout::xbaseOpen("/tmp/f2xbase_a"));
    std::printf("open-dirb=%d\n", fallout::xbaseOpen("/tmp/f2xbase_b"));
    std::printf("open-missing=%d\n", fallout::xbaseOpen("/tmp/f2nope"));
    // Re-open moves to the top (observable in sectionXfile priority below).
    std::printf("open-dup-a=%d\n", fallout::xbaseOpen("/tmp/f2xbase_a"));
    std::printf("isdir-a=%d\n", fallout::xbaseIsValidDirectory("/tmp/f2xbase_a"));
    std::printf("isdir-a-slash=%d\n", fallout::xbaseIsValidDirectory("/tmp/f2xbase_a/"));
    std::printf("isdir-a-upper=%d\n", fallout::xbaseIsValidDirectory("/TMP/F2XBASE_A"));
    std::printf("isdir-dat=%d\n", fallout::xbaseIsValidDirectory("/game-en/master.dat"));
    std::printf("isdir-missing=%d\n", fallout::xbaseIsValidDirectory("/tmp/f2nope"));
    std::printf("isdir-empty=%d\n", fallout::xbaseIsValidDirectory(""));
}

void sectionXfile()
{
    std::puts("[xfile]");
    // Priority: dir-a was re-opened last, so its shared.txt ("A") wins.
    dumpXfile("shared.txt");
    // Absolute paths bypass xbases.
    dumpXfile("/tmp/f2xbase_b/shared.txt");
    // DAT entry through the VFS.
    dumpXfile("text\\english\\game\\misc.msg");
    // Plain file with gzip magic takes the GZFILE branch (size quirk: 0).
    dumpXfile("arch.gz");
    {
        // Explicit GZ content probe (size lies, content does not).
        fallout::File* g = fallout::fileOpen("arch.gz", "rb");
        if (g != nullptr) {
            char content[64] = {};
            size_t n = fallout::fileRead(content, 1, sizeof(content) - 1, g);
            long tell = fallout::fileTell(g);
            int eof = fallout::fileEof(g);
            std::printf("gzread n=%d tell=%ld eof=%d [%s]\n", (int)n, tell, eof, content);
            fallout::fileClose(g);
        }
    }
    std::printf("open-missing=%d\n", fallout::xfileOpen("no_such_file.xyz", "rb") != nullptr);

    // Seek/tell/rewind/eof through the VFS handle.
    fallout::File* f = fallout::fileOpen("text\\english\\game\\misc.msg", "rb");
    if (f != nullptr) {
        unsigned char buf[4] = {};
        fallout::fileRead(buf, 1, 4, f);
        long tell1 = fallout::fileTell(f);
        fallout::fileRewind(f);
        long tell2 = fallout::fileTell(f);
        std::printf("vfs-seek tell1=%ld tell2=%ld eof=%d\n", tell1, tell2, fallout::fileEof(f));
        fallout::fileClose(f);
    }

    // compat_fgets \r\n collapsing through the VFS.
    fallout::File* t = fallout::fileOpen("crlf.txt", "rb");
    if (t != nullptr) {
        char line[128] = {};
        char* r = fallout::fileReadString(line, sizeof(line), t);
        std::printf("vfs-textline=[%s]\n", r != nullptr ? line : "(null)");
        fallout::fileClose(t);
    }

    // Write round-trip through the VFS (lands in dir-a: first dir xbase).
    fallout::File* w = fallout::fileOpen("wtest.bin", "wb");
    if (w != nullptr) {
        int v = 0x01020304;
        // Big-endian write, byte by byte like the engine does.
        unsigned char be[4] = { 0x01, 0x02, 0x03, 0x04 };
        size_t n = fallout::fileWrite(be, 1, 4, w);
        (void)v;
        fallout::fileClose(w);
        std::printf("vfs-write n=%d\n", (int)n);
    }
    dumpXfile("/tmp/f2xbase_a/wtest.bin");
}

void sectionDb()
{
    std::puts("[db]");
    std::printf("dbopen=%d\n", fallout::dbOpen("/game-en/master.dat", "/game-en/patch000.dat"));
    std::printf("dbopen-missing=%d\n", fallout::dbOpen("/game-en/nope.dat", nullptr));
    std::printf("dbtotal=%d\n", fallout::db_total());

    int miscSize = -99;
    int miscRc = fallout::dbGetFileSize("text\\english\\game\\misc.msg", &miscSize);
    std::printf("dbsize-misc=%d size=%d\n", miscRc, miscSize);
    int missingRc = fallout::dbGetFileSize("no\\such\\file.txt", &miscSize);
    std::printf("dbsize-missing=%d\n", missingRc);

    // Big-endian typed reads (be.bin = 01 02 03 04, zeros.bin = 0).
    fallout::File* b = fallout::fileOpen("be.bin", "rb");
    if (b != nullptr) {
        int i32 = 0;
        fallout::fileReadInt32(b, &i32);
        fallout::fileClose(b);
        std::printf("be-i32=%d\n", i32);
    }
    fallout::File* z = fallout::fileOpen("zeros.bin", "rb");
    if (z != nullptr) {
        int i32 = -99;
        bool bval = true;
        fallout::fileReadInt32(z, &i32);
        fallout::fileRewind(z);
        fallout::fileReadBool(z, &bval);
        fallout::fileClose(z);
        std::printf("zero-i32=%d zero-bool=%d\n", i32, bval ? 1 : 0);
    }

    // Progress-handler accounting (size via db, like callers do).
    fallout::fileSetReadProgressHandler(progressHandler, 4);
    gProgressCalls = 0;
    int contentSize = 0;
    fallout::dbGetFileSize("text\\english\\game\\misc.msg", &contentSize);
    fallout::File* m = fallout::fileOpen("text\\english\\game\\misc.msg", "rb");
    if (m != nullptr && contentSize > 0) {
        unsigned char* data = (unsigned char*)malloc((size_t)contentSize);
        fallout::dbGetFileContents("text\\english\\game\\misc.msg", data);
        uLong crc = crc32(0L, Z_NULL, 0);
        crc = crc32(crc, data, (uInt)contentSize);
        std::printf("progress-calls=%d size=%d crc=%08lX\n", gProgressCalls, contentSize, crc);
        free(data);
        fallout::fileClose(m);
    }
    fallout::fileSetReadProgressHandler(nullptr, 0);

    // Sorted/deduped/basename file list across all xbases.
    char** names = nullptr;
    int count = fallout::fileNameListInit("Art\\*", &names);
    std::printf("namelist count=%d\n", count);
    for (int i = 0; i < count && i < 3; i++) {
        std::printf("namelist[%d]=[%s]\n", i, names[i]);
    }
    if (count > 3) {
        std::printf("namelist[last]=[%s]\n", names[count - 1]);
    }
    fallout::fileNameListFree(&names, 0);

    // Directories only.
    {
        fallout::XList xlist {};
        int dircount = 0;
        if (fallout::xlistInitDirectories("sub\\*", &xlist)) {
            dircount = xlist.fileNamesLength;
            for (int i = 0; i < dircount; i++) {
                std::printf("dirlist[%d]=[%s]\n", i, xlist.fileNames[i]);
            }
        }
        std::printf("dirlist count=%d\n", dircount);
        fallout::xlistFree(&xlist);
    }
}

void sectionConfig()
{
    std::puts("[config]");

    fallout::Config cfg {};
    std::printf("init=%d\n", fallout::configInit(&cfg));
    std::printf("read=%d\n", fallout::configRead(&cfg, "/game-en/fallout2.cfg", false));

    char* s = nullptr;
    bool gotMaster = fallout::configGetString(&cfg, "system", "master_dat", &s);
    std::printf("get-master=%d [%s]\n", gotMaster ? 1 : 0, s != nullptr ? s : "(null)");

    int i = -99;
    bool gotResx = fallout::configGetInt(&cfg, "screen", "resolution_x", &i);
    std::printf("get-resx=%d %d\n", gotResx ? 1 : 0, i);

    bool b = false;
    bool gotMusic = fallout::configGetBool(&cfg, "sound", "music", &b);
    std::printf("get-music=%d %d\n", gotMusic ? 1 : 0, b ? 1 : 0);

    char* missing = nullptr;
    std::printf("get-missing=%d\n", fallout::configGetString(&cfg, "nope", "nope", &missing));
    char* withDefault = nullptr;
    fallout::configGetString(&cfg, "nope", "nope", &withDefault, "DEF");
    std::printf("get-default=[%s]\n", withDefault);
    int idef = -99;
    fallout::configGetInt(&cfg, "nope", "nope", &idef, 42);
    std::printf("get-int-default=%d\n", idef);

    // strtol quirks: hex, octal, trailing garbage, empty, overflow.
    fallout::configSetString(&cfg, "probe", "hex", "0x10");
    fallout::configSetString(&cfg, "probe", "oct", "010");
    fallout::configSetString(&cfg, "probe", "pct", "38%");
    fallout::configSetString(&cfg, "probe", "trail", "12x");
    fallout::configSetString(&cfg, "probe", "empty", "");
    fallout::configSetString(&cfg, "probe", "huge", "9999999999999999999");
    int v = 0;
    fallout::configGetInt(&cfg, "probe", "hex", &v);
    std::printf("int-hex=%d\n", v);
    fallout::configGetInt(&cfg, "probe", "oct", &v);
    std::printf("int-oct=%d\n", v);
    fallout::configGetInt(&cfg, "probe", "pct", &v);
    std::printf("int-pct=%d\n", v);
    fallout::configGetInt(&cfg, "probe", "trail", &v);
    std::printf("int-trail=%d\n", v);
    bool okEmpty = fallout::configGetInt(&cfg, "probe", "empty", &v);
    std::printf("int-empty=%d %d\n", okEmpty ? 1 : 0, v);
    fallout::configGetInt(&cfg, "probe", "huge", &v);
    std::printf("int-huge=%d\n", v);

    // Base-16 reads (sfall colors) and doubles.
    int b16 = 0;
    fallout::configGetIntBase(&cfg, "probe", "hex", &b16, 0, 16);
    std::printf("int-base16=%d\n", b16);
    fallout::configSetDouble(&cfg, "probe", "pi", 3.14159);
    double d = 0;
    fallout::configGetDouble(&cfg, "probe", "pi", &d);
    std::printf("double-pi=%f\n", d);

    // Int list protocol (synthetic).
    fallout::configSetString(&cfg, "probe", "list", "1,2,3");
    int arr[3] = {};
    bool gotList = fallout::configGetIntList(&cfg, "probe", "list", arr, 3);
    std::printf("intlist=%d [%d,%d,%d]\n", gotList ? 1 : 0, arr[0], arr[1], arr[2]);
    fallout::configSetString(&cfg, "probe", "short", "7,8");
    int arr2[3] = { -1, -1, -1 };
    bool gotShort = fallout::configGetIntList(&cfg, "probe", "short", arr2, 3);
    std::printf("intlist-short=%d [%d,%d,%d]\n", gotShort ? 1 : 0, arr2[0], arr2[1], arr2[2]);

    // Command-line overrides ("[section]key=value"; section untrimmed).
    // NOTE: writable buffers — the parser temporarily writes NULs.
    char arg0[] = "prog";
    char arg1[] = "[screen]resolution_x=1024";
    char arg2[] = "junk";
    char arg3[] = "[bad";
    char arg4[] = "[s]k=v=w";
    char arg5[] = "[ sp ]k2 = v2 ";
    char* argv[] = { arg0, arg1, arg2, arg3, arg4, arg5 };
    fallout::configParseCommandLineArguments(&cfg, 6, argv, nullptr);
    fallout::configGetInt(&cfg, "screen", "resolution_x", &v);
    std::printf("cli-resx=%d\n", v);
    char* kv = nullptr;
    fallout::configGetString(&cfg, "s", "k", &kv);
    std::printf("cli-kv=[%s]\n", kv != nullptr ? kv : "(null)");
    char* k2 = nullptr;
    fallout::configGetString(&cfg, " sp ", "k2", &k2);
    std::printf("cli-sp=[%s]\n", k2 != nullptr ? k2 : "(null)");

    // Standard write + re-read round-trip.
    fallout::configSetString(&cfg, "news", "nk", "nv");
    std::printf("write-std=%d\n", fallout::configWrite(&cfg, "/tmp/cfg-std.ini", false));
    fallout::Config cfg2 {};
    fallout::configInit(&cfg2);
    fallout::configRead(&cfg2, "/tmp/cfg-std.ini", false);
    char* nv = nullptr;
    fallout::configGetString(&cfg2, "news", "nk", &nv);
    std::printf("reread=[%s]\n", nv != nullptr ? nv : "(null)");

    // Side-by-side: comments/order/unknown keys survive.
    {
        FILE* f = fopen("/tmp/cfg-orig.ini", "wb");
        fputs("; top comment\n[screen]\n; res comment\nresolution_x=800 ; inline\nunknown_key=zzz\n\n[new]\na=1\n", f);
        fclose(f);
    }
    fallout::Config cfg3 {};
    fallout::configInit(&cfg3);
    fallout::configRead(&cfg3, "/tmp/cfg-orig.ini", false);
    fallout::configSetString(&cfg3, "screen", "resolution_x", "1024");
    fallout::configSetString(&cfg3, "added", "k", "v");
    fallout::configRemoveKey(&cfg3, "new", "a");
    std::printf("write-sbs=%d\n", fallout::configWriteEx(&cfg3, "/tmp/cfg-orig.ini",
        fallout::CONFIG_RETAIN_ALL));
    {
        FILE* f = fopen("/tmp/cfg-orig.ini", "rb");
        char line[512];
        while (fgets(line, sizeof(line), f) != nullptr) {
            // Trim trailing newline for stable vectors.
            size_t n = strlen(line);
            while (n > 0 && (line[n - 1] == '\n' || line[n - 1] == '\r')) {
                line[--n] = '\0';
            }
            std::printf("sbs-line=[%s]\n", line);
        }
        fclose(f);
    }

    fallout::configFree(&cfg);
    fallout::configFree(&cfg2);
    fallout::configFree(&cfg3);
}

void sectionMessage()
{
    std::puts("[message]");

    fallout::xbaseOpen("/game-en/master.dat");

    fallout::MessageList misc {};
    fallout::messageListInit(&misc);
    std::printf("load-misc=%d\n", fallout::messageListLoad(&misc, "game\\misc.msg"));
    std::printf("misc-count=%d\n", misc.entries_num);

    fallout::MessageListItem item {};
    item.num = 9;
    bool got9 = fallout::messageListGetItem(&misc, &item);
    std::printf("misc9=%d [%s]\n", got9 ? 1 : 0, got9 ? item.text : "(null)");
    item.num = 10;
    bool got10 = fallout::messageListGetItem(&misc, &item);
    std::printf("misc10=%d [%s]\n", got10 ? 1 : 0, got10 ? item.text : "(null)");
    item.num = 999999;
    std::printf("misc-huge=%d\n", fallout::messageListGetItem(&misc, &item) ? 1 : 0);

    fallout::MessageListItem missing {};
    char* missingText = fallout::getmsg(&misc, &missing, 999999);
    std::printf("getmsg-missing=[%s]\n", missingText);

    int countBefore = misc.entries_num;
    fallout::messageListLoad(&misc, "game\\misc.msg");
    std::printf("reload-count=%d (was %d)\n", misc.entries_num, countBefore);

    std::printf("badwords=%d\n", fallout::badwordsInit());
    fallout::badwordsExit();

    // Gender fixture through a dir xbase.
    system("mkdir -p /tmp/f2msg/text/english");
    {
        FILE* f = fopen("/tmp/f2msg/text/english/gender.msg", "wb");
        fputs("{0}{}{<He^She> went}\n", f);
        fputs("{1}{}{plain}\n", f);
        fputs("{2}{}{A<man^woman> and <boy^girl> walked}\n", f);
        fputs("{3}{}{a^b}\n", f);
        fputs("{4}{}{<x^y}\n", f);
        fclose(f);
    }
    fallout::xbaseOpen("/tmp/f2msg");

    // The game enables this via content config; the bare harness must too.
    fallout::configInit(&fallout::gContentConfig);
    fallout::configSetString(&fallout::gContentConfig, "dialog", "gender_words", "1");

    for (int g = 0; g < 2; g++) {
        fallout::MessageList list {};
        fallout::messageListInit(&list);
        fallout::messageListLoad(&list, "gender.msg");
        fallout::messageListFilterGenderWords(&list, g == 0 ? fallout::GENDER_MALE : fallout::GENDER_FEMALE);
        std::printf("gender-%s count=%d\n", g == 0 ? "male" : "female", list.entries_num);
        for (int i = 0; i < list.entries_num; i++) {
            std::printf("gender-%s[%d]=[%s]\n", g == 0 ? "male" : "female",
                list.entries[i].num, list.entries[i].text);
        }
        fallout::messageListFree(&list);
    }

    // Repository routing over loaded lists.
    fallout::messageListRepositoryInit();
    fallout::messageListRepositorySetStandardMessageList(
        fallout::STANDARD_MESSAGE_LIST_MISC, &misc);
    char* repoText = fallout::messageListRepositoryGetMsg(fallout::STANDARD_MESSAGE_LIST_MISC, 10);
    std::printf("repo-misc10=[%s]\n", repoText);
    int extraId = fallout::messageListRepositoryAddExtra("game\\misc.msg");
    std::printf("repo-extra=%d\n", extraId);
    char* extraText = fallout::messageListRepositoryGetMsg(extraId, 9);
    std::printf("repo-extra9=[%s]\n", extraText);
    char* noList = fallout::messageListRepositoryGetMsg(fallout::STANDARD_MESSAGE_LIST_COMBAT, 1);
    std::printf("repo-unset=[%s]\n", noList);
    fallout::messageListRepositoryReset();
    fallout::messageListRepositoryExit();

    fallout::messageListFree(&misc);
}

const fallout::SettingDescriptor* findDescriptor(const char* id);

void sectionSettings()
{
    std::puts("[settings]");

    // Config/save files land in CWD: isolate to /tmp (last section).
    chdir("/tmp");
    remove("/tmp/fallout2.cfg");

    char prog[] = "prog";
    char* argv0[] = { prog };

    // Phase A: defaults + set/validate vectors (no CLI overrides active).
    std::printf("init=%d\n", fallout::settingsInit(false, 1, argv0));
    std::printf("res=%dx%d windowed=%d music=%d lang=[%s]\n",
        fallout::settings.screen.resolution_x, fallout::settings.screen.resolution_y,
        (int)fallout::settings.screen.windowed, fallout::settings.sound.music ? 1 : 0,
        fallout::settings.system.language.c_str());
    std::printf("init-again=%d\n", fallout::settingsInit(false, 1, argv0));

    const fallout::SettingDescriptor* resxDesc = findDescriptor("screen.resolution_x");
    std::printf("desc-found=%d\n", resxDesc != nullptr);
    if (resxDesc != nullptr) {
        fallout::SettingValue active = fallout::settingsGetValue(*resxDesc);
        std::printf("get-value=%d\n", std::get<int>(active));
        std::string error;
        std::printf("set-800=%d\n",
            fallout::settingsSetValue(*resxDesc, fallout::SettingValue(800), &error) ? 1 : 0);
        fallout::SettingValue active2 = fallout::settingsGetValue(*resxDesc);
        fallout::SettingValue configured2 = fallout::settingsGetConfiguredValue(*resxDesc);
        std::printf("after-set active=%d configured=%d\n",
            std::get<int>(active2), std::get<int>(configured2));
        bool set100 = fallout::settingsSetValue(*resxDesc, fallout::SettingValue(100), &error);
        std::printf("set-100=%d err=[%s]\n", set100 ? 1 : 0, error.c_str());
        error.clear();
        bool setBool = fallout::settingsSetValue(*resxDesc, fallout::SettingValue(true), &error);
        std::printf("set-bool=%d err=[%s]\n", setBool ? 1 : 0, error.c_str());
    }
    fallout::settingsExit(false);

    // Phase B: out-of-range file value clamps on read.
    {
        FILE* f = fopen("/tmp/fallout2.cfg", "wb");
        fputs("[screen]\nresolution_x=100\n", f);
        fclose(f);
    }
    fallout::settingsInit(false, 1, argv0);
    std::printf("clamped-resx=%d\n", fallout::settings.screen.resolution_x);
    fallout::settingsExit(false);

    // Phase C: CLI overrides win, are flagged, and are stripped on save.
    char arg1[] = "[screen]resolution_x=1024";
    char arg2[] = "[ui]skip_opening_movies=2";
    char* argv[] = { prog, arg1, arg2 };
    std::printf("init-cli=%d\n", fallout::settingsInit(false, 3, argv));
    std::printf("cli-resx=%d cli-skip=%d\n",
        fallout::settings.screen.resolution_x, fallout::settings.ui.skip_opening_movies);
    resxDesc = findDescriptor("screen.resolution_x");
    std::printf("override=%d\n",
        resxDesc != nullptr && resxDesc->commandLineOverride ? 1 : 0);
    std::printf("set-overridden=%d\n",
        resxDesc != nullptr
                && !fallout::settingsSetValue(*resxDesc, fallout::SettingValue(800), nullptr)
            ? 1
            : 0);

    std::printf("save=%d\n", fallout::settingsSave() ? 1 : 0);
    {
        FILE* f = fopen("/tmp/fallout2.cfg", "rb");
        int resxLines = 0;
        int skipLines = 0;
        char firstResx[512] = {};
        if (f != nullptr) {
            char line[512];
            while (fgets(line, sizeof(line), f) != nullptr) {
                if (strstr(line, "resolution_x") != nullptr) {
                    resxLines++;
                    if (firstResx[0] == '\0') {
                        size_t n = strlen(line);
                        while (n > 0 && (line[n - 1] == '\n' || line[n - 1] == '\r')) {
                            line[--n] = '\0';
                        }
                        snprintf(firstResx, sizeof(firstResx), "%s", line);
                    }
                }
                if (strstr(line, "skip_opening_movies") != nullptr) {
                    skipLines++;
                }
            }
            fclose(f);
        }
        std::printf("saved-resx-lines=%d [%s] saved-skip-lines=%d\n", resxLines, firstResx, skipLines);
    }

    // Phase D: fields are NOT reset by re-init (stale 1024, not default).
    fallout::settingsExit(false);
    std::printf("reinit=%d resx=%d\n", fallout::settingsInit(false, 1, argv0) ? 1 : 0,
        fallout::settings.screen.resolution_x);
    fallout::settingsExit(false);
}

const fallout::SettingDescriptor* findDescriptor(const char* id)
{
    for (const auto& d : fallout::settingsGetDescriptors()) {
        if (d.id == id) {
            return &d;
        }
    }
    return nullptr;
}

void dumpFrm(const char* path);

void sectionArt()
{
    std::puts("[art]");
    std::printf("sizeof-art=%d sizeof-frame=%d\n", (int)sizeof(fallout::Art), (int)sizeof(fallout::ArtFrame));

    // Bisect artReadList("art\items\items.lst") — artInit fails on it.
    {
        fallout::xbaseOpen("/game-en/master.dat");
        fallout::File* s = fallout::fileOpen("art\\items\\items.lst", "rt");
        char string[200] = {};
        int count = 0;
        while (s != nullptr && fallout::fileReadString(string, sizeof(string), s)) {
            if (count == 0) {
                size_t n = strlen(string);
                while (n > 0 && (string[n - 1] == '\n' || string[n - 1] == '\r')) {
                    string[--n] = '\0';
                }
                std::printf("lst-first=[%s]\n", string);
            }
            count++;
        }
        int seekRc = -99;
        if (s != nullptr) {
            seekRc = fallout::fileSeek(s, 0, SEEK_SET);
        }
        int count2 = 0;
        while (s != nullptr && fallout::fileReadString(string, sizeof(string), s)) {
            count2++;
        }
        std::printf("lst-count=%d seek=%d recount=%d\n", count, seekRc, count2);
        if (s != nullptr) {
            fallout::fileClose(s);
        }
    }

    // Pure Fid math (no init needed).
    {
        fallout::FrmId misc(fallout::OBJ_TYPE_MISC, 5);
        std::printf("fid-misc fid=%d valid=%d type=%d frame=%d rot=%d anim=%d weapon=%d\n",
            misc.fid(), misc.valid() ? 1 : 0, (int)misc.objectType(), misc.frameId(),
            (int)misc.rotation(), (int)misc.animationType(), (int)misc.weaponAnimation());
    }
    {
        fallout::FrmId empty;
        std::printf("fid-empty fid=%d valid=%d\n", empty.fid(), empty.valid() ? 1 : 0);
    }
    {
        // Walk forces rotation NE for non-knockdown critter anims.
        fallout::FrmId critter(fallout::OBJ_TYPE_CRITTER, 12, fallout::ANIM_WALK,
            fallout::WeaponAnimation::Knife, fallout::ROTATION_SE);
        std::printf("fid-critter fid=%d rot=%d weapon=%d\n",
            critter.fid(), (int)critter.rotation(), (int)critter.weaponAnimation());
    }
    {
        fallout::FrmId fire(fallout::OBJ_TYPE_CRITTER, 7, fallout::ANIM_FIRE_SINGLE,
            fallout::WeaponAnimation::Pistol, fallout::ROTATION_NE);
        std::printf("fid-fire fid=%d\n", fire.fid());
    }

    fallout::xbaseOpen("/game-en/master.dat");
    {
        // Layer probe: linear find vs bsearch vs VFS for the same path.
        int eflags = 0;
        fallout::DBase* db = fallout::dbaseOpen("/game-en/master.dat", &eflags);
        fallout::DFileFindData fd {};
        bool lin = db != nullptr && fallout::dbaseFindFirstEntry(db, &fd, "art\\items\\items.lst");
        fallout::DFile* df = db != nullptr ? fallout::dfileOpen(db, "art\\items\\items.lst", "rt") : nullptr;
        fallout::File* xf = fallout::fileOpen("art\\items\\items.lst", "rt");
        std::printf("probe lin=%d bin=%d vfs=%d found=[%s]\n", lin ? 1 : 0, df != nullptr ? 1 : 0,
            xf != nullptr ? 1 : 0, lin ? fd.fileName : "(null)");
        if (df != nullptr) {
            fallout::dfileClose(df);
        }
        if (xf != nullptr) {
            fallout::fileClose(xf);
        }
        if (db != nullptr) {
            fallout::dbaseClose(db);
        }
    }
    // artInit needs critter.dat mounted (critter lists/FRMs live there).
    fallout::xbaseOpen("/game-en/master.dat");
    fallout::xbaseOpen("/game-en/patch000.dat");
    fallout::xbaseOpen("/game-en/critter.dat");
    int initRc = fallout::artInit();
    std::printf("artinit=%d\n", initRc);
    if (initRc == 0) {
        int ifaceIdx = fallout::artListIndex(fallout::OBJ_TYPE_INTERFACE, "mainmenu");
        std::printf("listindex-mainmenu=%d\n", ifaceIdx);
        std::printf("listindex-missing=%d\n",
            fallout::artListIndex(fallout::OBJ_TYPE_INTERFACE, "zzz_nope"));
        std::printf("listindex-badtype=%d\n", fallout::artListIndex(fallout::OBJ_TYPE_INVALID, "mainmenu"));
        std::printf("typename=%s\n", fallout::artGetObjectTypeName(fallout::OBJ_TYPE_INTERFACE));
        std::printf("hidden=%d\n", fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_INTERFACE));
        fallout::artToggleObjectTypeHidden(fallout::OBJ_TYPE_INTERFACE);
        std::printf("hidden-toggled=%d\n", fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_INTERFACE));
        fallout::artToggleObjectTypeHidden(fallout::OBJ_TYPE_INTERFACE);

        if (ifaceIdx >= 0) {
            fallout::FrmId menu(fallout::OBJ_TYPE_INTERFACE, ifaceIdx);
            std::printf("buildpath=[%s]\n", menu.filePath());
            dumpFrm(menu.filePath());
        }

        int critIdx = fallout::artListIndex(fallout::OBJ_TYPE_CRITTER, "hmjmps");
        std::printf("listindex-hmjmps=%d\n", critIdx);
        if (critIdx >= 0) {
            fallout::FrmId dude(fallout::OBJ_TYPE_CRITTER, critIdx, fallout::ANIM_WALK,
                fallout::WeaponAnimation::None, fallout::ROTATION_NE);
            std::printf("critterpath=[%s]\n", dude.filePath());
            dumpFrm(dude.filePath());
        }
    }

    // Error paths (no init needed, VFS only).
    {
        unsigned char small[16] = {};
        std::printf("read-tiny-buf=%d\n", fallout::artRead("art\\intrface\\mainmenu.frm", small, sizeof(small)));
    }
    {
        unsigned char buf[1024] = {};
        std::printf("read-missing=%d\n", fallout::artRead("art\\no\\such.frm", buf, sizeof(buf)));
    }
    {
        FILE* f = fopen("/tmp/trunc.frm", "wb");
        unsigned char zeros[40] = {};
        fwrite(zeros, 1, sizeof(zeros), f);
        fclose(f);
        fallout::xbaseOpen("/tmp");
        unsigned char buf[4096] = {};
        std::printf("read-trunc=%d\n", fallout::artRead("trunc.frm", buf, sizeof(buf)));
    }
    {
        unsigned char buf[8192] = {};
        std::printf("read-notfrm=%d\n",
            fallout::artRead("text\\english\\game\\misc.msg", buf, sizeof(buf)));
    }
}

void dumpFrm(const char* path)
{
    static unsigned char* data = (unsigned char*)malloc(4 * 1024 * 1024);
    int rc = fallout::artRead(path, data, 4 * 1024 * 1024);
    fallout::Art* art = (fallout::Art*)data;
    if (rc != 0) {
        std::printf("frm [%s] rc=%d\n", path, rc);
        return;
    }
    int w = fallout::artGetWidth(art, 0, fallout::ROTATION_NE);
    int h = fallout::artGetHeight(art, 0, fallout::ROTATION_NE);
    int x = -9999;
    int y = -9999;
    fallout::artGetFrameOffsets(art, 0, fallout::ROTATION_NE, &x, &y);
    int rx = -9999;
    int ry = -9999;
    fallout::artGetRotationOffsets(art, fallout::ROTATION_NE, &rx, &ry);
    unsigned char* pixels = fallout::artGetFrameData(art, 0, fallout::ROTATION_NE);
    uLong crc = crc32(0L, Z_NULL, 0);
    if (pixels != nullptr && w > 0 && h > 0) {
        crc = crc32(crc, pixels, (uInt)((unsigned int)w * (unsigned int)h));
    }
    std::printf("frm [%s] rc=%d ver=%d fps=%d action=%d frames=%d w=%d h=%d x=%d y=%d rx=%d ry=%d crc=%08lX\n",
        path, rc, art->version, fallout::artGetFramesPerSecond(art), fallout::artGetActionFrame(art),
        fallout::artGetFrameCount(art), w, h, x, y, rx, ry, crc);
    for (int r = 0; r < fallout::ROTATION_COUNT; r++) {
        int rw = fallout::artGetWidth(art, 0, (fallout::Rotation)r);
        int rh = fallout::artGetHeight(art, 0, (fallout::Rotation)r);
        int rox = -9999;
        int roy = -9999;
        fallout::artGetFrameOffsets(art, 0, (fallout::Rotation)r, &rox, &roy);
        unsigned char* rpixels = fallout::artGetFrameData(art, 0, (fallout::Rotation)r);
        uLong rcrc = crc32(0L, Z_NULL, 0);
        if (rpixels != nullptr && rw > 0 && rh > 0) {
            rcrc = crc32(rcrc, rpixels, (uInt)((unsigned int)rw * (unsigned int)rh));
        }
        std::printf("frm [%s] rot=%d w=%d h=%d x=%d y=%d crc=%08lX\n", path, r, rw, rh, rox, roy, rcrc);
    }
    std::printf("frm [%s] bad-frame=%d bad-rot=%d null-art=%d\n", path,
        fallout::artGetWidth(art, 999, fallout::ROTATION_NE),
        fallout::artGetWidth(art, 0, (fallout::Rotation)99),
        fallout::artGetWidth(nullptr, 0, fallout::ROTATION_NE));
}

// Synthetic cache backend: keys 1..4 map to 100/200/300/500 bytes.
static int cacheTestSize(int key, int* sizePtr)
{
    static const int sizes[] = { 100, 200, 300, 500 };
    if (key < 1 || key > 4) {
        return -1;
    }
    *sizePtr = sizes[key - 1];
    return 0;
}

static int cacheTestRead(int key, int* sizePtr, unsigned char* buffer)
{
    if (key < 1 || key > 4) {
        return -1;
    }
    memset(buffer, 0x10 + key, (size_t)*sizePtr);
    return 0;
}

static void cacheTestFree(void* ptr)
{
    (void)ptr;
}

void sectionCache()
{
    std::puts("[cache]");

    fallout::Cache cache {};
    std::printf("init=%d\n", fallout::cacheInit(&cache, cacheTestSize, cacheTestRead, cacheTestFree, 1000));

    unsigned char* data = nullptr;
    fallout::CacheEntry* entry = nullptr;
    bool locked = fallout::cacheLock(&cache, 1, (void**)&data, &entry);
    std::printf("lock1=%d byte=%02X refs=%u hits=%u\n", locked ? 1 : 0,
        locked ? data[0] : 0, locked ? entry->referenceCount : 0, locked ? entry->hits : 0);

    unsigned char* data2 = nullptr;
    fallout::CacheEntry* entry2 = nullptr;
    bool relocked = fallout::cacheLock(&cache, 1, (void**)&data2, &entry2);
    std::printf("relock1=%d refs=%u hits=%u same=%d\n", relocked ? 1 : 0,
        relocked ? entry2->referenceCount : 0, relocked ? entry2->hits : 0,
        (relocked && data == data2) ? 1 : 0);

    std::printf("unlock=%d\n", fallout::cacheUnlock(&cache, entry2) ? 1 : 0);
    std::printf("unlock=%d\n", fallout::cacheUnlock(&cache, entry) ? 1 : 0);
    std::printf("unlock-empty=%d\n", fallout::cacheUnlock(&cache, entry) ? 1 : 0);
    std::printf("unlock-null=%d\n", fallout::cacheUnlock(&cache, nullptr) ? 1 : 0);
    std::printf("lock-missing=%d\n", fallout::cacheLock(&cache, 99, (void**)&data, &entry) ? 1 : 0);

    // Flush drops the unreferenced entry; relock re-reads it.
    fallout::cacheFlush(&cache);
    unsigned char* data3 = nullptr;
    fallout::CacheEntry* entry3 = nullptr;
    bool relocked2 = fallout::cacheLock(&cache, 1, (void**)&data3, &entry3);
    std::printf("relock-after-flush=%d byte=%02X\n", relocked2 ? 1 : 0, relocked2 ? data3[0] : 0);
    if (relocked2) {
        fallout::cacheUnlock(&cache, entry3);
    }
    fallout::cacheFree(&cache);
}

void sectionCacheEvict()
{
    std::puts("[cacheEvict]");

    // Single cold 500B entry is swept to make room in a 600B cache.
    fallout::Cache cache {};
    fallout::cacheInit(&cache, cacheTestSize, cacheTestRead, cacheTestFree, 600);

    // Content under eviction pressure is allocator-dependent (see the
    // Heap note in Cache.java); only success codes are pinned here.
    unsigned char* d1 = nullptr;
    fallout::CacheEntry* e1 = nullptr;
    std::printf("lock-big=%d\n", fallout::cacheLock(&cache, 4, (void**)&d1, &e1) ? 1 : 0);
    if (e1 != nullptr) {
        fallout::cacheUnlock(&cache, e1);
    }

    unsigned char* d2 = nullptr;
    fallout::CacheEntry* e2 = nullptr;
    std::printf("lock-after-evict=%d\n", fallout::cacheLock(&cache, 2, (void**)&d2, &e2) ? 1 : 0);
    if (e2 != nullptr) {
        fallout::cacheUnlock(&cache, e2);
    }
    fallout::cacheFree(&cache);

    // Entry bigger than the whole cache never loads.
    fallout::Cache small {};
    fallout::cacheInit(&small, cacheTestSize, cacheTestRead, cacheTestFree, 400);
    unsigned char* ds = nullptr;
    fallout::CacheEntry* es = nullptr;
    std::printf("lock-over-max=%d\n", fallout::cacheLock(&small, 4, (void**)&ds, &es) ? 1 : 0);
    fallout::cacheFree(&small);
}

void sectionArtLoad()
{
    std::puts("[artload]");

    fallout::Art* art = fallout::artLoad("art\\intrface\\mainmenu.frm");
    std::printf("load=%d frames=%d\n", art != nullptr ? 1 : 0,
        art != nullptr ? fallout::artGetFrameCount(art) : -99);
    std::printf("load-missing=%d\n", fallout::artLoad("art\\no\\such.frm") != nullptr ? 1 : 0);
    std::printf("load-notfrm=%d\n", fallout::artLoad("text\\english\\game\\misc.msg") != nullptr ? 1 : 0);

    fallout::FrmId menu(fallout::OBJ_TYPE_INTERFACE, 140);
    fallout::FrmImage image;
    bool locked = image.lock(menu);
    unsigned char* pixels = locked ? image.getData() : nullptr;
    uLong crc = crc32(0L, Z_NULL, 0);
    if (pixels != nullptr) {
        crc = crc32(crc, pixels, (uInt)((unsigned int)image.getWidth() * (unsigned int)image.getHeight()));
    }
    std::printf("img-lock=%d w=%d h=%d x=%d y=%d crc=%08lX\n", locked ? 1 : 0,
        image.getWidth(), image.getHeight(), image.getXOffset(), image.getYOffset(), crc);
    image.unlock();

    fallout::FrmImage named;
    bool namedLocked = named.lock("art\\intrface\\mainmenu.frm");
    std::printf("named-lock=%d w=%d\n", namedLocked ? 1 : 0, named.getWidth());
    named.unlock();

    fallout::FrmImage rel;
    bool relLocked = rel.lock(fallout::OBJ_TYPE_INTERFACE, "mainmenu.frm");
    std::printf("rel-lock=%d w=%d\n", relLocked ? 1 : 0, rel.getWidth());
    rel.unlock();

    fallout::FrmImage bad;
    std::printf("lock-badframe=%d\n", bad.lock(menu, 999) ? 1 : 0);
    fallout::FrmImage missing;
    fallout::FrmId noSuch(fallout::OBJ_TYPE_INTERFACE, 4095);
    std::printf("lock-missing=%d\n", missing.lock(noSuch) ? 1 : 0);

    fallout::CacheEntry* handle = nullptr;
    fallout::Art* lockedArt = fallout::artLock(menu, &handle);
    std::printf("artlock=%d unlock=%d flush=%d\n", lockedArt != nullptr ? 1 : 0,
        fallout::artUnlock(handle), fallout::artCacheFlush());
}

void sectionColor()
{
    std::puts("[color]");

    fallout::xbaseOpen("/game-en/master.dat");
    bool loaded = fallout::colorPaletteLoad("color.pal");
    char* err = fallout::_colorError();
    std::printf("load=%d err=[%s]\n", loaded ? 1 : 0, err);

    std::printf("crc-cmap=%08lX\n",
        crc32(crc32(0L, Z_NULL, 0), fallout::_cmap, COLOR_MAP_SIZE));
    std::printf("crc-colortable=%08lX\n",
        crc32(crc32(0L, Z_NULL, 0), (unsigned char*)fallout::_colorTable, COLOR_PALETTE_SIZE_15BIT));
    std::printf("crc-intensity=%08lX\n",
        crc32(crc32(0L, Z_NULL, 0), (unsigned char*)fallout::intensityColorTable, 65536));
    std::printf("crc-mixadd=%08lX\n",
        crc32(crc32(0L, Z_NULL, 0), (unsigned char*)fallout::colorMixAddTable, 65536));
    std::printf("crc-mixmul=%08lX\n",
        crc32(crc32(0L, Z_NULL, 0), (unsigned char*)fallout::colorMixMulTable, 65536));

    int mapped = 0;
    for (int i = 0; i < COLOR_COUNT; i++) {
        if (fallout::_mappedColor[i] != 0) {
            mapped++;
        }
    }
    std::printf("mapped=%d\n", mapped);

    int redIdx = fallout::rgb555(0xFF0000);
    int whiteIdx = fallout::rgb555(0xFFFFFF);
    int blackIdx = fallout::rgb555(0x000000);
    std::printf("red=%d white=%d black=%d\n",
        (int)fallout::_colorTable[redIdx] & 0xFF, (int)fallout::_colorTable[whiteIdx] & 0xFF,
        (int)fallout::_colorTable[blackIdx] & 0xFF);
    std::printf("rgb-white=%d\n", fallout::Color2RGB(fallout::_colorTable[whiteIdx]));
    std::printf("calc=%d\n", (int)fallout::_calculateColor(0, fallout::_colorTable[whiteIdx]) & 0xFF);

    fallout::Color* blend = fallout::_getColorBlendTable(fallout::_colorTable[redIdx]);
    // NOTE: _buildBlendTable writes 14 of the 16 rows; the 2 trailing rows
    // keep malloc garbage -> mask them out so the vector is deterministic.
    unsigned char blendBuf[4096];
    std::memcpy(blendBuf, blend, sizeof(blendBuf));
    std::memset(blendBuf + 3584, 0, 512);
    uLong blendCrc = crc32(crc32(0L, Z_NULL, 0), blendBuf, sizeof(blendBuf));
    std::printf("blend-crc=%08lX\n", blendCrc);
    fallout::Color* blend2 = fallout::_getColorBlendTable(fallout::_colorTable[redIdx]);
    std::printf("blend-same=%d\n", blend == blend2 ? 1 : 0);
    fallout::_freeColorBlendTable(fallout::_colorTable[redIdx]);
    fallout::_freeColorBlendTable(fallout::_colorTable[redIdx]);
    fallout::Color* blend3 = fallout::_getColorBlendTable(fallout::_colorTable[redIdx]);
    uLong blendCrc3 = 0;
    if (blend3 != nullptr) {
        std::memcpy(blendBuf, blend3, sizeof(blendBuf));
        std::memset(blendBuf + 3584, 0, 512);
        blendCrc3 = crc32(crc32(0L, Z_NULL, 0), blendBuf, sizeof(blendBuf));
    }
    std::printf("blend-rebuilt=%d crc=%08lX same=%d\n",
        blend3 != nullptr ? 1 : 0, blendCrc3, blendCrc3 == blendCrc ? 1 : 0);
    fallout::_freeColorBlendTable(fallout::_colorTable[redIdx]);

    bool missingLoaded = fallout::colorPaletteLoad("no_such.pal");
    char* missingErr = fallout::_colorError();
    std::printf("load-missing=%d err=[%s]\n", missingLoaded ? 1 : 0, missingErr);
}

void sectionColorCycle()
{
    std::puts("[colorcycle]");

    std::printf("sdl-init=%d\n", SDL_Init(SDL_INIT_TIMER) == 0 ? 1 : 0);

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.system.color_cycling = true;
    fallout::settings.system.cycle_speed_factor = 1;

    fallout::colorCycleInit();
    std::printf("init-enabled=%d\n", fallout::colorCycleEnabled() ? 1 : 0);
    fallout::colorCycleInit();
    std::printf("init-again=%d\n", fallout::colorCycleEnabled() ? 1 : 0);
    std::printf("speed=%d\n", fallout::settings.system.cycle_speed_factor);
    fallout::cycleSetSpeedFactor(3);
    std::printf("set-speed=%d\n", fallout::settings.system.cycle_speed_factor);
    fallout::cycleSetSpeedFactor(1);

    unsigned char* palette = fallout::_getSystemPalette();
    uLong rangeCrc0 = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("crc-range0=%08lX\n", rangeCrc0);

    usleep(250000);
    fallout::colorCycleTicker();
    uLong crc1 = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick1-crc=%08lX slime0=%d,%d,%d bobber=%d\n", crc1,
        (int)palette[229 * 3], (int)palette[229 * 3 + 1], (int)palette[229 * 3 + 2],
        (int)palette[254 * 3]);

    fallout::colorCycleTicker();
    uLong crc2 = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick2-same=%d\n", crc2 == crc1 ? 1 : 0);

    fallout::colorCycleReset();
    std::printf("reset-enabled=%d\n", fallout::colorCycleEnabled() ? 1 : 0);
    fallout::colorCycleTicker();
    uLong crc3 = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick3-crc=%08lX changed=%d slime0=%d,%d,%d bobber=%d\n", crc3,
        crc3 != crc1 ? 1 : 0,
        (int)palette[229 * 3], (int)palette[229 * 3 + 1], (int)palette[229 * 3 + 2],
        (int)palette[254 * 3]);

    fallout::colorCycleDisable();
    std::printf("disable=%d\n", fallout::colorCycleEnabled() ? 1 : 0);
    fallout::colorCycleTicker();
    uLong crcDisabled = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick-disabled-same=%d\n", crcDisabled == crc3 ? 1 : 0);

    fallout::colorCycleEnable();
    usleep(250000);
    fallout::colorCycleTicker();
    uLong crc4 = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick4-crc=%08lX changed=%d slime0=%d,%d,%d bobber=%d\n", crc4,
        crc4 != crc3 ? 1 : 0,
        (int)palette[229 * 3], (int)palette[229 * 3 + 1], (int)palette[229 * 3 + 2],
        (int)palette[254 * 3]);

    fallout::colorCycleFree();
    std::printf("free-enabled=%d\n", fallout::colorCycleEnabled() ? 1 : 0);
    fallout::colorCycleTicker();
    uLong crcAfterFree = crc32(crc32(0L, Z_NULL, 0), palette + 229 * 3, 81);
    std::printf("tick-after-free-same=%d\n", crcAfterFree == crc4 ? 1 : 0);
    fallout::colorCycleReset();
    std::printf("reset-after-free=%d\n", fallout::colorCycleEnabled() ? 1 : 0);

    fallout::settingsExit(false);
}

void sectionFps()
{
    std::puts("[fps]");

    // SDL timer is already up (see [colorcycle]); all vectors below are
    // lower-bound checks so scheduling jitter cannot break them.
    fallout::FpsLimiter limiter(60);

    unsigned int t0 = fallout::getTicks();
    limiter.mark();
    limiter.throttle();
    unsigned int t1 = fallout::getTicks();
    std::printf("throttle-delayed=%d\n", fallout::getTicksBetween(t1, t0) >= 15 ? 1 : 0);

    t0 = fallout::getTicks();
    limiter.mark();
    SDL_Delay(20);
    limiter.throttle();
    t1 = fallout::getTicks();
    std::printf("throttle-nodelay=%d\n", fallout::getTicksBetween(t1, t0) >= 20 ? 1 : 0);

    fallout::FpsLimiter fast(1000);
    t0 = fallout::getTicks();
    fast.mark();
    fast.throttle();
    t1 = fallout::getTicks();
    std::printf("fps1000-delayed=%d\n", fallout::getTicksBetween(t1, t0) >= 1 ? 1 : 0);

    delay_ms(0);
    delay_ms(-3);
    std::printf("delay-nonpositive-ok=1\n");

    t0 = fallout::getTicks();
    delay_ms(50);
    t1 = fallout::getTicks();
    std::printf("delay50=%d\n", fallout::getTicksBetween(t1, t0) >= 50 ? 1 : 0);
}

void sectionSvga()
{
    std::puts("[svga]");

    setenv("SDL_VIDEODRIVER", "dummy", 1);
    int videoInit = SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER);
    std::printf("video-init=%d\n", videoInit == 0 ? 1 : 0);

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.screen.resolution_x = 640;
    fallout::settings.screen.resolution_y = 480;
    fallout::settings.screen.scale = 1;
    fallout::settings.screen.windowed = fallout::WindowMode::Windowed;
    fallout::settings.ui.iface_bar_mode = false;
    fallout::gProgramIsActive = true;

    // _scr_blit is preset to _GNW95_ShowRect at startup; _zero_mem starts null.
    bool scrBlitPreset = fallout::_scr_blit != nullptr;
    std::printf("scrblit-preset=%d\n", scrBlitPreset ? 1 : 0);
    bool zeroMemNull = fallout::_zero_mem == nullptr;
    std::printf("zeromem-null=%d\n", zeroMemNull ? 1 : 0);
    fallout::_zero_vid_mem();
    std::printf("zero-noop=1\n");

    // Manual SDL context mirroring createRenderer (the GL window flag is
    // rejected by the dummy driver, so the window is created without it;
    // _GNW95_init_window then skips creation and every line below the
    // window check in _GNW95_init_mode_ex is real C code).
    fallout::gSdlWindow = SDL_CreateWindow("f2j",
        SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, 640, 480,
        SDL_WINDOW_ALLOW_HIGHDPI);
    std::printf("window=%d\n", fallout::gSdlWindow != nullptr ? 1 : 0);
    fallout::gSdlRenderer = SDL_CreateRenderer(fallout::gSdlWindow, -1, 0);
    SDL_RenderSetLogicalSize(fallout::gSdlRenderer, 640, 480);
    fallout::gSdlTexture = SDL_CreateTexture(fallout::gSdlRenderer,
        SDL_PIXELFORMAT_RGB888, SDL_TEXTUREACCESS_STREAMING, 640, 480);
    Uint32 format = 0;
    SDL_QueryTexture(fallout::gSdlTexture, &format, nullptr, nullptr, nullptr);
    fallout::gSdlTextureSurface = SDL_CreateRGBSurfaceWithFormat(0, 640, 480,
        SDL_BITSPERPIXEL(format), format);
    bool rendererOk = fallout::gSdlRenderer != nullptr
        && fallout::gSdlTexture != nullptr
        && fallout::gSdlTextureSurface != nullptr;
    std::printf("renderer=%d\n", rendererOk ? 1 : 0);

    int mode = fallout::_init_mode_640_480();
    std::printf("mode640480=%d scr=%d,%d,%d,%d\n", mode,
        fallout::_scr_size.left, fallout::_scr_size.top,
        fallout::_scr_size.right, fallout::_scr_size.bottom);
    bool hooksSet = fallout::_scr_blit != nullptr && fallout::_zero_mem != nullptr;
    std::printf("hooks-set=%d\n", hooksSet ? 1 : 0);
    std::printf("screen=%dx%d visible=%d exclusive=%d\n",
        fallout::screenGetWidth(), fallout::screenGetHeight(),
        fallout::screenGetVisibleHeight(),
        fallout::screenIsExclusiveFullscreen() ? 1 : 0);
    std::printf("surface=%dx%d pitch=%d\n",
        fallout::gSdlSurface->w, fallout::gSdlSurface->h, fallout::gSdlSurface->pitch);

    fallout::directDrawSetPalette(fallout::_cmap);
    unsigned char* pal = fallout::directDrawGetPalette();
    uLong palCrc = crc32(crc32(0L, Z_NULL, 0), pal, 768);
    std::printf("pal-crc=%08lX\n", palCrc);

    unsigned char src[100 * 50];
    for (int i = 0; i < 100 * 50; i++) {
        src[i] = (unsigned char)(i & 0xFF);
    }
    // Zero first: fresh SDL surfaces are not guaranteed cleared, and only
    // the blit window below is overwritten.
    fallout::_zero_vid_mem();
    fallout::_GNW95_ShowRect(src, 100, 0, 10, 5, 80, 40, 200, 100);
    uLong blitCrc = crc32(crc32(0L, Z_NULL, 0),
        (unsigned char*)fallout::gSdlSurface->pixels,
        fallout::gSdlSurface->pitch * fallout::gSdlSurface->h);
    std::printf("blit-crc=%08lX\n", blitCrc);

    fallout::_zero_vid_mem();
    uLong zeroCrc = crc32(crc32(0L, Z_NULL, 0),
        (unsigned char*)fallout::gSdlSurface->pixels,
        fallout::gSdlSurface->pitch * fallout::gSdlSurface->h);
    std::printf("zero-crc=%08lX\n", zeroCrc);

    fallout::settings.screen.resolution_x = 800;
    fallout::settings.screen.resolution_y = 600;
    int mode800 = fallout::_init_mode_800_600();
    unsigned char* pal2 = fallout::directDrawGetPalette();
    uLong palCrc2 = crc32(crc32(0L, Z_NULL, 0), pal2, 768);
    std::printf("mode800=%d scr=%d,%d,%d,%d surface=%dx%d pal-preserved=%d\n", mode800,
        fallout::_scr_size.left, fallout::_scr_size.top,
        fallout::_scr_size.right, fallout::_scr_size.bottom,
        fallout::gSdlSurface->w, fallout::gSdlSurface->h,
        palCrc2 == palCrc ? 1 : 0);

    fallout::settings.screen.resolution_x = 1280;
    fallout::settings.screen.resolution_y = 960;
    fallout::settings.screen.scale = 2;
    int modeScaled = fallout::_GNW95_init_mode_ex(320, 200, 8);
    std::printf("scaled=%d scr=%d,%d,%d,%d surface=%dx%d\n", modeScaled,
        fallout::_scr_size.left, fallout::_scr_size.top,
        fallout::_scr_size.right, fallout::_scr_size.bottom,
        fallout::gSdlSurface->w, fallout::gSdlSurface->h);

    fallout::settings.screen.resolution_x = 640;
    fallout::settings.screen.resolution_y = 480;
    fallout::settings.screen.scale = 2;
    int modeFallback = fallout::_GNW95_init_mode_ex(0, 0, 8);
    std::printf("fallback=%d scr=%d,%d,%d,%d surface=%dx%d\n", modeFallback,
        fallout::_scr_size.left, fallout::_scr_size.top,
        fallout::_scr_size.right, fallout::_scr_size.bottom,
        fallout::gSdlSurface->w, fallout::gSdlSurface->h);

    int mode16 = fallout::_init_mode_640_480_16();
    std::printf("mode16=%d\n", mode16);

    fallout::directDrawFree();
    unsigned char* pal3 = fallout::directDrawGetPalette();
    uLong palCrc3 = crc32(crc32(0L, Z_NULL, 0), pal3, 768);
    std::printf("free-pal-crc=%08lX\n", palCrc3);

    fallout::settingsExit(false);
}

static uLong crcBytes(unsigned char* buf, int size)
{
    return crc32(crc32(0L, Z_NULL, 0), buf, size);
}

void sectionDraw()
{
    std::puts("[draw]");

    // Lines on 64x64 heap buffers (malloc is uninitialized: preset first).
    unsigned char* line = (unsigned char*)malloc(64 * 64);
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 2, 3, 10, 3, static_cast<fallout::Color>(7));
    std::printf("line-h=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 10, 3, 2, 3, static_cast<fallout::Color>(7));
    std::printf("line-hrev=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 4, 1, 4, 8, static_cast<fallout::Color>(9));
    std::printf("line-v=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 4, 8, 4, 1, static_cast<fallout::Color>(9));
    std::printf("line-vrev=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 5, 5, 5, 5, static_cast<fallout::Color>(3));
    std::printf("line-point=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 1, 1, 3, 9, static_cast<fallout::Color>(5));
    std::printf("line-steep=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 1, 6, 12, 2, static_cast<fallout::Color>(6));
    std::printf("line-shallow=%08lX\n", crcBytes(line, 64 * 64));
    memset(line, 0, 64 * 64);
    fallout::bufferDrawLine(line, 64, 12, 2, 1, 6, static_cast<fallout::Color>(6));
    std::printf("line-shallow-rev=%08lX\n", crcBytes(line, 64 * 64));
    free(line);

    unsigned char rect[16 * 16];
    memset(rect, 0, sizeof(rect));
    fallout::bufferDrawRect(rect, 16, 2, 2, 9, 7, static_cast<fallout::Color>(7));
    std::printf("rect=%08lX\n", crcBytes(rect, sizeof(rect)));
    memset(rect, 0, sizeof(rect));
    fallout::bufferDrawRectShadowed(rect, 16, 2, 2, 9, 7, static_cast<fallout::Color>(1), static_cast<fallout::Color>(2));
    std::printf("rect-shadowed=%08lX\n", crcBytes(rect, sizeof(rect)));

    unsigned char bsrc[8 * 10];
    for (int i = 0; i < 8 * 10; i++) {
        bsrc[i] = (unsigned char)((i / 10 * 8 + i % 10 + 1) & 0xFF);
    }
    unsigned char bdst[12 * 12];
    memset(bdst, 0, sizeof(bdst));
    fallout::blitBufferToBuffer(bsrc, 8, 8, 10, bdst, 12);
    std::printf("blit=%08lX\n", crcBytes(bdst, sizeof(bdst)));
    unsigned char tsrc[8 * 10];
    for (int i = 0; i < 8 * 10; i++) {
        tsrc[i] = (unsigned char)(((i / 10 + i % 10) % 3 == 0) ? 0 : (i & 0xFF) + 1);
    }
    memset(bdst, 0, sizeof(bdst));
    fallout::blitBufferToBufferTrans(tsrc, 8, 8, 10, bdst, 12);
    std::printf("blit-trans=%08lX\n", crcBytes(bdst, sizeof(bdst)));

    unsigned char s26x6[36];
    for (int i = 0; i < 36; i++) {
        s26x6[i] = (unsigned char)(i + 1);
    }
    fallout::ConstBuffer2D src2d(s26x6, 6, 6);
    unsigned char d10x10[100];
    memset(d10x10, 0, sizeof(d10x10));
    fallout::Buffer2D dst2d(d10x10, 10, 10);
    fallout::blitBuffer2D(src2d, dst2d, -2, 3);
    std::printf("blit2d-clip=%08lX\n", crcBytes(d10x10, sizeof(d10x10)));
    memset(d10x10, 0, sizeof(d10x10));
    fallout::blitBuffer2D(src2d, 1, 1, 3, 3, dst2d, 4, 4);
    std::printf("blit2d-sub=%08lX\n", crcBytes(d10x10, sizeof(d10x10)));
    memset(d10x10, 7, sizeof(d10x10));
    fallout::blitBuffer2D(src2d, dst2d, 20, 20);
    std::printf("blit2d-outside=%08lX\n", crcBytes(d10x10, sizeof(d10x10)));

    unsigned char s44x4[16];
    for (int i = 0; i < 16; i++) {
        s44x4[i] = (unsigned char)(i + 1);
    }
    fallout::ConstBuffer2D src44(s44x4, 4, 4);
    unsigned char d8x8[64];
    memset(d8x8, 0, sizeof(d8x8));
    fallout::Buffer2D dst88(d8x8, 8, 8);
    fallout::blitBuffer2DScaled(src44, dst88, 0, 0, 8, 8);
    std::printf("scaled-up=%08lX\n", crcBytes(d8x8, sizeof(d8x8)));
    memset(d8x8, 0, sizeof(d8x8));
    fallout::blitBuffer2DScaled(src44, 1, 1, 2, 2, dst88, 1, 1, 4, 4);
    std::printf("scaled-sub=%08lX\n", crcBytes(d8x8, sizeof(d8x8)));
    unsigned char ts44x4[16];
    for (int i = 0; i < 16; i++) {
        ts44x4[i] = (unsigned char)((i % 5 == 0) ? 0 : i + 1);
    }
    fallout::ConstBuffer2D tsrc44(ts44x4, 4, 4);
    memset(d8x8, 9, sizeof(d8x8));
    fallout::blitBuffer2DScaledTrans(tsrc44, dst88, 0, 0, 8, 8);
    std::printf("scaled-trans=%08lX\n", crcBytes(d8x8, sizeof(d8x8)));

    unsigned char s42x2[8];
    for (int i = 0; i < 8; i++) {
        s42x2[i] = (unsigned char)(i + 1);
    }
    fallout::ConstBuffer2D src42(s42x2, 4, 2);
    memset(d8x8, 0, sizeof(d8x8));
    fallout::Rect fitUp = fallout::blitBuffer2DCenteredAspectFit(src42, dst88, static_cast<fallout::Color>(9), true);
    uLong fitUpCrc = crcBytes(d8x8, sizeof(d8x8));
    std::printf("fit-up=%08lX bounds=%d,%d,%d,%d\n", fitUpCrc,
        fitUp.left, fitUp.top, fitUp.right, fitUp.bottom);
    memset(d8x8, 0, sizeof(d8x8));
    fallout::Rect fitFlat = fallout::blitBuffer2DCenteredAspectFit(src42, dst88, static_cast<fallout::Color>(9), false);
    uLong fitFlatCrc = crcBytes(d8x8, sizeof(d8x8));
    std::printf("fit-flat=%08lX bounds=%d,%d,%d,%d\n", fitFlatCrc,
        fitFlat.left, fitFlat.top, fitFlat.right, fitFlat.bottom);
    unsigned char s88x8[64];
    for (int i = 0; i < 64; i++) {
        s88x8[i] = (unsigned char)(i + 1);
    }
    fallout::ConstBuffer2D src88(s88x8, 8, 8);
    unsigned char d4x4[16];
    memset(d4x4, 0, sizeof(d4x4));
    fallout::Buffer2D dst44(d4x4, 4, 4);
    fallout::Rect fitDown = fallout::blitBuffer2DCenteredAspectFit(src88, dst44, static_cast<fallout::Color>(9), false);
    uLong fitDownCrc = crcBytes(d4x4, sizeof(d4x4));
    std::printf("fit-down=%08lX bounds=%d,%d,%d,%d\n", fitDownCrc,
        fitDown.left, fitDown.top, fitDown.right, fitDown.bottom);

    unsigned char f57x[35];
    fallout::Buffer2D fill57(f57x, 5, 7);
    fallout::bufferFill2D(fill57, static_cast<fallout::Color>(3));
    std::printf("fill2d=%08lX\n", crcBytes(f57x, sizeof(f57x)));
    fallout::Buffer2D fillNull(nullptr, 5, 7);
    fallout::bufferFill2D(fillNull, static_cast<fallout::Color>(3));
    std::printf("fill2d-null=1\n");

    unsigned char f65x[48];
    memset(f65x, 0, sizeof(f65x));
    fallout::bufferFill(f65x, 6, 5, 8, static_cast<fallout::Color>(4));
    std::printf("fill=%08lX\n", crcBytes(f65x, sizeof(f65x)));

    unsigned char tex[32];
    for (int i = 0; i < 32; i++) {
        tex[i] = (unsigned char)(i + 1);
    }
    fallout::_buf_texture(tex, 8, 4, 8, nullptr, 0, 0);
    std::printf("texture-dormant=%08lX\n", crcBytes(tex, sizeof(tex)));

    // NOTE: intensity tables come from [color] (color.pal stays loaded).
    unsigned char li44x4[16];
    for (int i = 0; i < 16; i++) {
        li44x4[i] = (unsigned char)(i * 16);
    }
    fallout::_lighten_buf(li44x4, 4, 4, 4);
    std::printf("lighten=%08lX\n", crcBytes(li44x4, sizeof(li44x4)));

    unsigned char sw44x4[16];
    for (int i = 0; i < 16; i++) {
        sw44x4[i] = (unsigned char)((i % 4 == 0) ? 3 : ((i % 4 == 1) ? 7 : 5));
    }
    fallout::_swap_color_buf(sw44x4, 4, 4, 4, static_cast<fallout::Color>(3), static_cast<fallout::Color>(7));
    std::printf("swap=%08lX\n", crcBytes(sw44x4, sizeof(sw44x4)));

    unsigned char ol8x8[64];
    memset(ol8x8, 0, sizeof(ol8x8));
    for (int y = 2; y < 6; y++) {
        for (int x = 2; x < 6; x++) {
            ol8x8[y * 8 + x] = 7;
        }
    }
    fallout::bufferOutline(ol8x8, 8, 8, 8, static_cast<fallout::Color>(9));
    std::printf("outline=%08lX\n", crcBytes(ol8x8, sizeof(ol8x8)));

    unsigned char cpydst[20];
    memset(cpydst, 0, sizeof(cpydst));
    fallout::srcCopy(cpydst, 5, bsrc, 10, 4, 3);
    std::printf("srccopy=%08lX\n", crcBytes(cpydst, sizeof(cpydst)));
    memset(cpydst, 0, sizeof(cpydst));
    fallout::transSrcCopy(cpydst, 5, tsrc, 10, 4, 3);
    std::printf("transsrccopy=%08lX\n", crcBytes(cpydst, sizeof(cpydst)));
}

int main()
{
    // Unbuffered stdout: engine debug output goes to stderr (SDL_Log),
    // so without this the merged capture would group all stderr first.
    // v1.txt predates this fix (its INFO line sits on top).
    setvbuf(stdout, nullptr, _IONBF, 0);

    sectionRgb555();
    sectionRectUnion();
    sectionRectIntersection();
    sectionRectClip();
    sectionRandom();
    sectionStrParsers();
    sectionStrParseFunc();
    sectionWordWrap();
    sectionDict();
    sectionDictFile();
    sectionPaths();
    sectionFileUtils();
    sectionPattern();
    sectionDbase();
    sectionZip();
    sectionFixtureDat();
    sectionXbase();
    sectionXfile();
    sectionDb();
    sectionConfig();
    sectionMessage();
    sectionArt();
    sectionCache();
    sectionCacheEvict();
    sectionArtLoad();
    sectionColor();
    sectionSettings();
    sectionColorCycle();
    sectionFps();
    sectionSvga();
    sectionDraw();
    return 0;
}
