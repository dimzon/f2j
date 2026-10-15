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
#include <variant>
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
#include "content_config.h"
#include "dfile.h"
#include "datafile.h"
#include "dinput.h"
#include "draw.h"
#include "graph_lib.h"
#include "game_config.h"
#include "game_config_migration.h"
#include "game_movie.h"
#include "input.h"
#include "kb.h"
#include "options_schema.h"
#include "options_settings_model.h"
#include "pcx.h"
#include "pointer_registry.h"
#include "sfall_config.h"
#include "stat_defs.h"
#include "skill_defs.h"
#include "perk_defs.h"
#include "trait_defs.h"
#include "map_defs.h"
#include "animation_defs.h"
#include "art_defs.h"
#include "combat_ai_defs.h"
#include "combat_defs.h"
#include "obj_types.h"
#include "proto.h"
#include "proto_types.h"
#include "version.h"
#include "font_manager.h"
#include "game_config.h"
#include "input.h"
#include "message.h"
#include "region.h"
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
#include "audio_channels.h"
#include "geometry.h"
#include "platform_compat.h"
#include "text_font.h"
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

void sectionFont()
{
    std::puts("[font]");

    // The [wordWrap] section installs a fake font manager; restore pristine
    // static-init state so this section vectors real game behavior (the
    // real game never has the fake installed).
    fallout::gFontManagersCount = 0;
    fallout::gCurrentFont = -1;
    fallout::fontDrawTextPtr = nullptr;
    fallout::fontGetLineHeight = nullptr;
    fallout::fontGetStringWidth = nullptr;
    fallout::fontGetCharacterWidth = nullptr;
    fallout::fontGetMonospacedStringWidth = nullptr;
    fallout::fontGetLetterSpacing = nullptr;
    fallout::fontGetBufferSize = nullptr;
    fallout::fontGetMonospacedCharacterWidth = nullptr;

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.system.language = "english";
    fallout::xbaseOpen("/game-en/master.dat");

    int init = fallout::textFontsInit();
    std::printf("init=%d current=%d\n", init, fallout::fontGetCurrent());

    std::printf("font0 height=%d spacing=%d width-Hi=%d charW-H=%d monoW=%d bufsize-Hi=%d\n",
        fallout::fontGetLineHeight(),
        fallout::fontGetLetterSpacing(),
        fallout::fontGetStringWidth("Hi!"),
        fallout::fontGetCharacterWidth('H'),
        fallout::fontGetMonospacedCharacterWidth(),
        fallout::fontGetBufferSize("Hi!"));
    std::printf("mono-str=%d\n", fallout::fontGetMonospacedStringWidth("Hi!"));

    unsigned char text[64 * 32];
    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42));
    std::printf("draw=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_SHADOWED));
    std::printf("draw-shadowed=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_MONOSPACED));
    std::printf("draw-mono=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_UNDERLINED));
    std::printf("draw-underlined=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", 4, 64,
        static_cast<fallout::ColorWithFlags>(42));
    std::printf("draw-clip4=%08lX\n", crcBytes(text, sizeof(text)));

    unsigned char canvas[40 * 20];
    memset(canvas, 0, sizeof(canvas));
    fallout::Buffer2D dest(canvas, 40, 20);
    fallout::fontDrawText2D(dest, 3, 5, "Hi!", 36, static_cast<fallout::ColorWithFlags>(42));
    std::printf("draw2d=%08lX\n", crcBytes(canvas, sizeof(canvas)));

    fallout::fontSetCurrent(1);
    std::printf("current1=%d height1=%d width-Hi1=%d\n",
        fallout::fontGetCurrent(), fallout::fontGetLineHeight(),
        fallout::fontGetStringWidth("Hi!"));

    // Font 4 is missing: current changes, descriptor stays on font 1.
    fallout::fontSetCurrent(4);
    std::printf("current4=%d height4=%d\n",
        fallout::fontGetCurrent(), fallout::fontGetLineHeight());

    // Out of every manager range: no-op.
    fallout::fontSetCurrent(99);
    fallout::fontSetCurrent(-1);
    std::printf("current-still=%d\n", fallout::fontGetCurrent());

    fallout::FontManager dupe = fallout::gTextFontManager;
    std::printf("add-dupe=%d add-null=%d\n",
        fallout::fontManagerAdd(&dupe),
        fallout::fontManagerAdd(nullptr));

    // Overlap check is exclusive-upper: {9,9} slips through (but is shadowed).
    fallout::FontManager edge = fallout::gTextFontManager;
    edge.minFont = 9;
    edge.maxFont = 9;
    std::printf("add-edge=%d\n", fallout::fontManagerAdd(&edge));

    fallout::textFontsExit();
    std::printf("load0=%d load9=%d\n",
        fallout::textFontLoad(0), fallout::textFontLoad(9));

    fallout::settingsExit(false);
}

void sectionIfaceFont()
{
    std::puts("[ifacefont]");

    // Like [font]: the [wordWrap] fake is reset away (the real game never
    // has it installed).
    fallout::gFontManagersCount = 0;
    fallout::gCurrentFont = -1;
    fallout::fontDrawTextPtr = nullptr;
    fallout::fontGetLineHeight = nullptr;
    fallout::fontGetStringWidth = nullptr;
    fallout::fontGetCharacterWidth = nullptr;
    fallout::fontGetMonospacedStringWidth = nullptr;
    fallout::fontGetLetterSpacing = nullptr;
    fallout::fontGetBufferSize = nullptr;
    fallout::fontGetMonospacedCharacterWidth = nullptr;

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.system.language = "english";
    fallout::xbaseOpen("/game-en/master.dat");

    // Pristine statics: scaled metrics degrade to zero, draws are silent.
    fallout::ColorWithFlags plain42 = static_cast<fallout::ColorWithFlags>(42);
    std::printf("pre width=%d\n",
        fallout::interfaceFontGetStringWidthScaled("Hi!", plain42, 1.0f));
    unsigned char pre[32];
    memset(pre, 0, sizeof(pre));
    fallout::Buffer2D predest(pre, 8, 4);
    fallout::interfaceFontDrawTextScaled2D(predest, 0, 0, "Hi!", plain42, 1.0f);
    std::printf("pre-draw=%08lX\n", crcBytes(pre, sizeof(pre)));

    // Blend tables for the rasterizer come from color.pal.
    fallout::colorPaletteLoad("color.pal");

    int init = fallout::interfaceFontsInit();
    std::printf("init=%d\n", init);

    // Wire the modern manager like game.cc does, then draw through the
    // public proc pointers (the path the game actually uses).
    fallout::FontManager modern = fallout::gModernFontManager;
    std::printf("add-modern=%d\n", fallout::fontManagerAdd(&modern));
    fallout::fontSetCurrent(100);
    std::printf("current=%d\n", fallout::fontGetCurrent());

    std::printf("font0 height=%d spacing=%d width-Hi=%d charW-A=%d charW-space=%d monoW=%d bufsize=%d mono-str=%d\n",
        fallout::fontGetLineHeight(),
        fallout::fontGetLetterSpacing(),
        fallout::fontGetStringWidth("Hi!"),
        fallout::fontGetCharacterWidth('A'),
        fallout::fontGetCharacterWidth(' '),
        fallout::fontGetMonospacedCharacterWidth(),
        fallout::fontGetBufferSize("Hi!"),
        fallout::fontGetMonospacedStringWidth("Hi!"));

    unsigned char text[64 * 32];
    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42));
    std::printf("draw=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_SHADOWED));
    std::printf("draw-shadowed=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_MONOSPACED));
    std::printf("draw-mono=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", sizeof(text), 64,
        static_cast<fallout::ColorWithFlags>(42 | fallout::DRAW_TEXT_FLAG_UNDERLINED));
    std::printf("draw-underlined=%08lX\n", crcBytes(text, sizeof(text)));

    memset(text, 0, sizeof(text));
    fallout::fontDrawText(text, "Hi!", 4, 64,
        static_cast<fallout::ColorWithFlags>(42));
    std::printf("draw-clip4=%08lX\n", crcBytes(text, sizeof(text)));

    unsigned char canvas[40 * 20];
    memset(canvas, 0, sizeof(canvas));
    fallout::Buffer2D dest(canvas, 40, 20);
    fallout::interfaceFontDrawTextScaled2D(dest, 2, 2, "Hi!", plain42, 1.0f);
    std::printf("scaled1=%08lX width1=%d\n", crcBytes(canvas, sizeof(canvas)),
        fallout::interfaceFontGetStringWidthScaled("Hi!", plain42, 1.0f));

    memset(canvas, 0, sizeof(canvas));
    fallout::interfaceFontDrawTextScaled2D(dest, 2, 2, "Hi!", plain42, 2.0f);
    std::printf("scaled2=%08lX width2=%d\n", crcBytes(canvas, sizeof(canvas)),
        fallout::interfaceFontGetStringWidthScaled("Hi!", plain42, 2.0f));

    fallout::fontSetCurrent(101);
    std::printf("current101=%d height=%d\n",
        fallout::fontGetCurrent(), fallout::fontGetLineHeight());

    // Font 9 has no data: current changes, descriptor stays on font 1.
    fallout::fontSetCurrent(109);
    std::printf("current109=%d height=%d\n",
        fallout::fontGetCurrent(), fallout::fontGetLineHeight());

    fallout::interfaceFontsExit();
    std::printf("reinit=%d\n", fallout::interfaceFontsInit());

    fallout::settingsExit(false);
}

// regionContainsPoint lives in region.cc but is missing from region.h.
namespace fallout {
bool regionContainsPoint(Region* region, int x, int y);
}

void sectionRegion()
{
    std::puts("[region]");

    fallout::Region* square = fallout::regionCreate(4);
    std::printf("create name=[%s] flags=%d bounds=%d,%d,%d,%d centroid=%d,%d\n",
        fallout::regionGetName(square), square->flags,
        square->minX, square->minY, square->maxX, square->maxY,
        square->centroidX, square->centroidY);

    fallout::regionAddPoint(square, 0, 0);
    fallout::regionAddPoint(square, 10, 0);
    fallout::regionAddPoint(square, 10, 10);
    fallout::regionAddPoint(square, 0, 10);
    fallout::_regionSetBound(square);
    std::printf("square bounds=%d,%d,%d,%d centroid=%d,%d\n",
        square->minX, square->minY, square->maxX, square->maxY,
        square->centroidX, square->centroidY);

    std::printf("contains inside=%d vertex=%d edge=%d outside=%d neg=%d null=%d\n",
        fallout::regionContainsPoint(square, 5, 5) ? 1 : 0,
        fallout::regionContainsPoint(square, 0, 0) ? 1 : 0,
        fallout::regionContainsPoint(square, 5, 0) ? 1 : 0,
        fallout::regionContainsPoint(square, 15, 5) ? 1 : 0,
        fallout::regionContainsPoint(square, -1, -1) ? 1 : 0,
        fallout::regionContainsPoint(nullptr, 5, 5) ? 1 : 0);

    fallout::Region* tri = fallout::regionCreate(0);
    fallout::regionAddPoint(tri, 0, 0);
    fallout::regionAddPoint(tri, 8, 0);
    fallout::regionAddPoint(tri, 0, 6);
    fallout::_regionSetBound(tri);
    std::printf("tri bounds=%d,%d,%d,%d centroid=%d,%d in=%d out=%d\n",
        tri->minX, tri->minY, tri->maxX, tri->maxY,
        tri->centroidX, tri->centroidY,
        fallout::regionContainsPoint(tri, 1, 1) ? 1 : 0,
        fallout::regionContainsPoint(tri, 7, 5) ? 1 : 0);

    fallout::regionSetName(square, "hello");
    std::printf("name=[%s]\n", fallout::regionGetName(square));
    fallout::regionSetName(square, nullptr);
    std::printf("name-null=[%s]\n", fallout::regionGetName(square));
    fallout::regionSetName(square, "0123456789abcdef0123456789abcdefXYZ");
    std::printf("name-long=[%s]\n", fallout::regionGetName(square));
    std::printf("name-null-region=[%s]\n", fallout::regionGetName(nullptr));

    int tag = 12345;
    std::printf("userdata-null=%d\n", fallout::regionGetUserData(square) == nullptr ? 1 : 0);
    fallout::regionSetUserData(square, &tag);
    std::printf("userdata-roundtrip=%d\n",
        *(int*)fallout::regionGetUserData(square) == 12345 ? 1 : 0);

    fallout::regionAddFlag(square, 1);
    fallout::regionAddFlag(square, 4);
    std::printf("flags=%d\n", square->flags);

    fallout::regionAddPoint(nullptr, 1, 2);
    fallout::regionDelete(nullptr);
    fallout::regionDelete(square);
    fallout::regionDelete(tri);
    std::printf("deleted=1\n");
}

// datafile.cc extras missing from datafile.h upstream.
namespace fallout {
void datafileSetNameMangler(DatafileNameMangler* mangler);
void datafileSetLoader(DatafileLoader* loader);
void datafileRemapPixelsRgb8(uint8_t* data, uint8_t* palette, int width, int height);
void datafileRemapPixelsRgb6(uint8_t* data, uint8_t* palette, int width, int height);
uint8_t* datafileLoadPalette(char* path);
void datafilePackUntilZero(uint8_t* data, int* widthPtr, int* heightPtr);
}

namespace {
// Maps "alias.pcx" to the committed fixture (proves the mangler hook).
char gAliasTarget[] = "/src/java/engine/src/test/resources/fixtures/test.pcx";
char* testMangler(char* path)
{
    if (strcmp(path, "alias.pcx") == 0) {
        return gAliasTarget;
    }
    return path;
}

// Canned 2x2 image (proves the loader hook); marks the palette.
uint8_t* testLoader(char* path, uint8_t* palette, int* widthPtr, int* heightPtr)
{
    (void)path;
    palette[0] = 0x42;
    *widthPtr = 2;
    *heightPtr = 2;
    uint8_t* data = (uint8_t*)malloc(4);
    data[0] = 9;
    data[1] = 8;
    data[2] = 7;
    data[3] = 6;
    return data;
}
} // namespace

void sectionDatafile()
{
    std::puts("[datafile]");

    fallout::xbaseOpen("/game-en/master.dat");
    fallout::colorPaletteLoad("color.pal");

    char pcx[] = "/src/java/engine/src/test/resources/fixtures/test.pcx";
    char odd[] = "/src/java/engine/src/test/resources/fixtures/test-odd.pcx";

    unsigned char pal[768];
    memset(pal, 0xAA, sizeof(pal));
    int w = -1;
    int h = -1;
    unsigned char* img = fallout::pcxRead(pcx, &w, &h, pal);
    uLong imgCrc = img != nullptr ? crcBytes(img, w * h) : 0;
    std::printf("pcx=%dx%d img-crc=%08lX pal-crc=%08lX\n", w, h, imgCrc,
        crcBytes(pal, sizeof(pal)));

    int ow = -1;
    int oh = -1;
    unsigned char opal[768];
    unsigned char* oimg = fallout::pcxRead(odd, &ow, &oh, opal);
    uLong oddCrc = oimg != nullptr ? crcBytes(oimg, ow * oh) : 0;
    std::printf("odd=%dx%d img-crc=%08lX\n", ow, oh, oddCrc);

    int mw = -1;
    int mh = -1;
    char missing[] = "/tmp/nope.pcx";
    unsigned char* miss = fallout::pcxRead(missing, &mw, &mh, pal);
    std::printf("missing-null=%d dims=%d,%d\n", miss == nullptr ? 1 : 0, mw, mh);

    int rw = -1;
    int rh = -1;
    unsigned char* remapped = fallout::datafileRead(pcx, &rw, &rh);
    uLong remapCrc = remapped != nullptr ? crcBytes(remapped, rw * rh) : 0;
    std::printf("read=%dx%d crc=%08lX\n", rw, rh, remapCrc);

    unsigned char rgb6[4] = { 0, 1, 2, 3 };
    unsigned char pal6[768];
    for (int i = 0; i < 768; i++) {
        // NOTE: 6-bit VGA values (like real palettes); 8-bit input would
        // overflow the 15-bit table index (UB, out-of-bounds read in C).
        pal6[i] = (unsigned char)((i * 5 + 1) & 0x3F);
    }
    fallout::datafileRemapPixelsRgb6(rgb6, pal6, 2, 2);
    std::printf("remap6=%02X%02X%02X%02X\n", rgb6[0], rgb6[1], rgb6[2], rgb6[3]);

    unsigned char pack[12] = { 1, 2, 0, 4, 5, 6, 0, 0, 0, 0, 0, 0 };
    int pw = 4;
    int ph = 3;
    fallout::datafilePackUntilZero(pack, &pw, &ph);
    std::printf("pack=%dx%d crc=%08lX\n", pw, ph, crcBytes(pack, pw * ph));
    int zw = 5;
    int zh = 0;
    fallout::datafilePackUntilZero(pack, &zw, &zh);
    std::printf("pack-degenerate=%d,%d\n", zw, zh);

    int zsize = -1;
    char zipPath[] = "/tmp/f2test.zip";
    unsigned char* blob = fallout::datafileLoad(zipPath, &zsize);
    uLong blobCrc = blob != nullptr ? crcBytes(blob, zsize) : 0;
    std::printf("load size=%d crc=%08lX\n", zsize, blobCrc);
    int msize = -1;
    unsigned char* mblob = fallout::datafileLoad(missing, &msize);
    std::printf("load-missing=%d\n", mblob == nullptr ? 1 : 0);

    unsigned char* loadedPal = fallout::datafileLoadPalette(pcx);
    std::printf("loadpal=%d crc=%08lX\n", loadedPal != nullptr ? 1 : 0,
        loadedPal != nullptr ? crcBytes(loadedPal, 768) : 0);
    unsigned char* loadedMiss = fallout::datafileLoadPalette(missing);
    std::printf("loadpal-missing=%d\n", loadedMiss == nullptr ? 1 : 0);
    std::printf("getpal-crc=%08lX\n", crcBytes(fallout::datafileGetPalette(), 768));

    fallout::datafileSetNameMangler(testMangler);
    char alias[] = "alias.pcx";
    int aw = -1;
    int ah = -1;
    unsigned char* aimg = fallout::datafileReadRaw(alias, &aw, &ah);
    std::printf("mangled=%dx%d\n", aw, ah);
    fallout::datafileSetNameMangler(fallout::datafileDefaultNameManglerImpl);

    fallout::datafileSetLoader(testLoader);
    char custom[] = "custom.xxx";
    int cw = -1;
    int ch = -1;
    unsigned char* cimg = fallout::datafileRead(custom, &cw, &ch);
    uLong customCrc = cimg != nullptr ? crcBytes(cimg, cw * ch) : 0;
    std::printf("loaded=%dx%d crc=%08lX palmark=%02X\n", cw, ch, customCrc,
        fallout::datafileGetPalette()[0]);
    fallout::datafileSetLoader(nullptr);
}

static void printCfgValue(fallout::Config* cfg, const char* section, const char* key)
{
    char* value;
    if (fallout::configGetString(cfg, section, key, &value)) {
        std::printf("%s.%s=[%s]\n", section, key, value);
    } else {
        std::printf("%s.%s=<missing>\n", section, key);
    }
}

void sectionCfgMig()
{
    std::puts("[cfgmig]");

    system("mkdir -p /tmp/f2mig1 /tmp/f2mig2/config /tmp/f2mig3/config");
    remove("/tmp/ddraw.ini");
    remove("/tmp/f2mig2/config/game.cfg");
    remove("/tmp/f2mig3/config/game.cfg");

    char prog[] = "/tmp/prog";
    char* argv0[] = { prog };

    // Defaults with no ddraw.ini around.
    std::printf("sfall-init=%d\n", fallout::sfallConfigInit(1, argv0) ? 1 : 0);
    int overrideMode = -1;
    fallout::configGetInt(&fallout::gSfallConfig, "Misc", "OverrideCriticalTable", &overrideMode);
    std::printf("sfall-default=%d\n", overrideMode);
    std::printf("sfall-reinit=%d\n", fallout::sfallConfigInit(1, argv0) ? 1 : 0);
    fallout::sfallConfigExit();
    fallout::sfallConfigExit();

    {
        FILE* f = fopen("/tmp/ddraw.ini", "wb");
        fputs("[Misc]\nOverrideCriticalTable=5\nBooksFile=bks.ini\n"
              "StartingMap=arroyo\nStartYear=2250\nWorldMapFPSPatch=1\n"
              "WorldMapDelay2=100\nMovie1=custom.mve\nFallout1Behavior=1\n"
              "CheckWeaponAmmoCost=1\n",
            f);
        fclose(f);
    }
    std::printf("sfall-init-file=%d\n", fallout::sfallConfigInit(1, argv0) ? 1 : 0);
    printCfgValue(&fallout::gSfallConfig, "Misc", "OverrideCriticalTable");
    printCfgValue(&fallout::gSfallConfig, "Misc", "BooksFile");

    char sfallProg[] = "prog";
    char* sfallArgv[] = { sfallProg };

    // contentConfigInit with no master_patches dir: migrations bail early.
    fallout::settingsInit(false, 1, sfallArgv);
    fallout::settings.system.master_patches_path = "/tmp/f2nope";
    fallout::contentConfigInit();
    std::printf("content-init=%d\n", fallout::gContentConfig.isInitialized() ? 1 : 0);
    fallout::contentConfigExit();

    // F2Res game-config migration.
    {
        FILE* f = fopen("/tmp/f2mig1/fallout2.cfg", "wb");
        fputs("[screen]\nwindowed=1\n", f);
        fclose(f);
        f = fopen("/tmp/f2mig1/f2_res.ini", "wb");
        fputs("[MAIN]\nSCR_WIDTH=800\nSCR_HEIGHT=600\nWINDOWED=1\nSCALE_2X=1\n"
              "[MAINMENU]\nMAIN_MENU_SIZE=2\nSCALE_BUTTONS_AND_TEXT_MENU=1\n"
              "[IFACE]\nIFACE_BAR_MODE=1\n",
            f);
        fclose(f);
    }
    fallout::Config gameCfg;
    fallout::configInit(&gameCfg);
    char gameCfgPath[] = "/tmp/f2mig1/fallout2.cfg";
    fallout::configRead(&gameCfg, gameCfgPath, false);
    std::printf("f2res-migrate=%d\n",
        fallout::gameConfigMigrateFromF2Res(gameCfgPath, &gameCfg) ? 1 : 0);
    printCfgValue(&gameCfg, "screen", "resolution_x");
    printCfgValue(&gameCfg, "screen", "scale");
    printCfgValue(&gameCfg, "ui", "main_menu_scale_mode");
    printCfgValue(&gameCfg, "ui", "iface_bar_mode");
    std::printf("f2res-again=%d\n",
        fallout::gameConfigMigrateFromF2Res(gameCfgPath, &gameCfg) ? 1 : 0);
    fallout::configFree(&gameCfg);

    // Sfall content migration (writes only when the target is absent).
    fallout::settings.system.master_patches_path = "/tmp/f2mig2";
    fallout::contentConfigTryMigrateFromSfall("config\\game.cfg");
    fallout::Config migrated;
    fallout::configInit(&migrated);
    char migratedPath[] = "/tmp/f2mig2/config/game.cfg";
    std::printf("sfall-migrated-file=%d\n",
        fallout::configRead(&migrated, migratedPath, false) ? 1 : 0);
    printCfgValue(&migrated, "start", "map");
    printCfgValue(&migrated, "start", "year");
    printCfgValue(&migrated, "worldmap", "travel_delay");
    printCfgValue(&migrated, "movies", "movie1");
    printCfgValue(&migrated, "movies", "endgame_movie_male");
    printCfgValue(&migrated, "combat", "check_weapon_ammo_cost");
    fallout::configFree(&migrated);
    fallout::contentConfigTryMigrateFromSfall("config\\game.cfg");
    std::printf("sfall-migrate-again=1\n");

    // F2Res content migration.
    strcpy(fallout::gGameConfigFilePath, "/tmp/f2mig3/fallout2.cfg");
    {
        FILE* f = fopen("/tmp/f2mig3/fallout2.cfg", "wb");
        fputs("[screen]\nwindowed=1\n", f);
        fclose(f);
        f = fopen("/tmp/f2mig3/f2_res.ini", "wb");
        fputs("[MAINMENU]\nMENU_BG_OFFSET_X=20\nSCALE_BUTTONS_AND_TEXT_MENU=1\n", f);
        fclose(f);
    }
    fallout::settings.system.master_patches_path = "/tmp/f2mig3";
    fallout::contentConfigTryMigrateFromF2Res("config\\game.cfg");
    fallout::Config migrated2;
    fallout::configInit(&migrated2);
    char migrated2Path[] = "/tmp/f2mig3/config/game.cfg";
    std::printf("f2res-content-file=%d\n",
        fallout::configRead(&migrated2, migrated2Path, false) ? 1 : 0);
    printCfgValue(&migrated2, "main_menu", "main_menu_panel_offset_x");
    printCfgValue(&migrated2, "main_menu", "scale_buttons_and_text");
    fallout::configFree(&migrated2);

    // Movie default names (bounds included).
    const char* movie0 = fallout::gameMovieGetDefaultFileName(0);
    const char* movie16 = fallout::gameMovieGetDefaultFileName(16);
    std::printf("movie0=[%s] movie16=[%s] movie-neg=%d movie-17=%d movie-32=%d\n",
        movie0 != nullptr ? movie0 : "(null)",
        movie16 != nullptr ? movie16 : "(null)",
        fallout::gameMovieGetDefaultFileName(-1) == nullptr ? 1 : 0,
        fallout::gameMovieGetDefaultFileName(17) == nullptr ? 1 : 0,
        fallout::gameMovieGetDefaultFileName(32) == nullptr ? 1 : 0);

    fallout::sfallConfigExit();
    fallout::settingsExit(false);
}

namespace {
// Ticker probes (prepend order => reverse execution).
int gTickLog[16];
int gTickCount = 0;
void tickerA()
{
    gTickLog[gTickCount++] = 1;
}
void tickerB()
{
    gTickLog[gTickCount++] = 2;
}
} // namespace

void sectionInput()
{
    std::puts("[input]");

    std::printf("bk-init=%u\n", fallout::_get_bk_time());

    fallout::tickersAdd(tickerA);
    fallout::tickersAdd(tickerB);
    fallout::tickersExecute();
    std::printf("exec1=%d,%d\n", gTickLog[0], gTickLog[1]);

    fallout::tickersRemove(tickerA);
    gTickCount = 0;
    fallout::tickersExecute();
    std::printf("exec2-removed=%d count=%d\n", gTickLog[0], gTickCount);

    fallout::tickersAdd(tickerA);
    gTickCount = 0;
    fallout::tickersExecute();
    std::printf("exec3-readded=%d,%d\n", gTickLog[0], gTickLog[1]);

    fallout::tickersDisable();
    gTickCount = 0;
    fallout::tickersExecute();
    std::printf("disabled=%d\n", gTickCount);
    fallout::tickersEnable();
    fallout::tickersExecute();
    std::printf("re-enabled=%d\n", gTickCount);
    fallout::tickersRemove(tickerA);
    fallout::tickersRemove(tickerB);
    fallout::tickersExecute();

    unsigned int t0 = fallout::getTicks();
    fallout::inputBlockForTocks(50);
    unsigned int t1 = fallout::getTicks();
    std::printf("block50=%d\n", fallout::getTicksBetween(t1, t0) >= 50 ? 1 : 0);

    system("rm -f /tmp/scr*.bmp /tmp/scr*.png");
    unsigned char shot[12];
    for (int i = 0; i < 12; i++) {
        shot[i] = (unsigned char)(i * 17 + 3);
    }
    unsigned char shotPal[768];
    for (int i = 0; i < 768; i++) {
        shotPal[i] = (unsigned char)((i * 3 + 5) & 0xFF);
    }
    int bmpRc = fallout::screenshotHandlerDefaultImpl(4, 3, shot, shotPal);
    FILE* bmp = fopen("/tmp/scr00000.bmp", "rb");
    long bmpSize = -1;
    uLong bmpCrc = 0;
    if (bmp != nullptr) {
        fseek(bmp, 0, SEEK_END);
        bmpSize = ftell(bmp);
        rewind(bmp);
        unsigned char* bmpBytes = (unsigned char*)malloc(bmpSize);
        size_t got = fread(bmpBytes, 1, bmpSize, bmp);
        (void)got;
        bmpCrc = crc32(crc32(0L, Z_NULL, 0), bmpBytes, bmpSize);
        free(bmpBytes);
        fclose(bmp);
    }
    std::printf("bmp=%d size=%ld crc=%08lX\n", bmpRc, bmpSize, bmpCrc);

    int pngRc = fallout::screenshotHandlerPngImpl(4, 3, shot, shotPal);
    FILE* png = fopen("/tmp/scr00000.png", "rb");
    long pngSize = -1;
    uLong pngCrc = 0;
    if (png != nullptr) {
        fseek(png, 0, SEEK_END);
        pngSize = ftell(png);
        rewind(png);
        unsigned char* pngBytes = (unsigned char*)malloc(pngSize);
        size_t got = fread(pngBytes, 1, pngSize, png);
        (void)got;
        pngCrc = crc32(crc32(0L, Z_NULL, 0), pngBytes, pngSize);
        free(pngBytes);
        fclose(png);
    }
    std::printf("png=%d size=%ld crc=%08lX\n", pngRc, pngSize, pngCrc);
}

void sectionKb()
{
    std::puts("[kb]");
    std::printf("esc=%d tab=%d backspace=%d enter=%d space=%d del=%d\n",
        (int)fallout::KEY_ESCAPE, (int)fallout::KEY_TAB, (int)fallout::KEY_BACKSPACE,
        (int)fallout::KEY_RETURN, (int)fallout::KEY_SPACE, (int)fallout::KEY_DEL);
    std::printf("a=%d z=%d A=%d Z=%d 0=%d 9=%d\n",
        (int)fallout::KEY_LOWERCASE_A, (int)fallout::KEY_LOWERCASE_Z,
        (int)fallout::KEY_UPPERCASE_A, (int)fallout::KEY_UPPERCASE_Z,
        (int)fallout::KEY_0, (int)fallout::KEY_9);
    std::printf("f1=%d f10=%d f11=%d f12=%d\n",
        (int)fallout::KEY_F1, (int)fallout::KEY_F10,
        (int)fallout::KEY_F11, (int)fallout::KEY_F12);
    std::printf("shift-f1=%d ctrl-f1=%d alt-f1=%d\n",
        (int)fallout::KEY_SHIFT_F1, (int)fallout::KEY_CTRL_F1, (int)fallout::KEY_ALT_F1);
    std::printf("up=%d down=%d left=%d right=%d\n",
        (int)fallout::KEY_ARROW_UP, (int)fallout::KEY_ARROW_DOWN,
        (int)fallout::KEY_ARROW_LEFT, (int)fallout::KEY_ARROW_RIGHT);
    std::printf("ctrl-up=%d alt-up=%d\n",
        (int)fallout::KEY_CTRL_ARROW_UP, (int)fallout::KEY_ALT_ARROW_UP);
    std::printf("home=%d end=%d pgup=%d pgdn=%d ins=%d\n",
        (int)fallout::KEY_HOME, (int)fallout::KEY_END, (int)fallout::KEY_PAGE_UP,
        (int)fallout::KEY_PAGE_DOWN, (int)fallout::KEY_INSERT);
    std::printf("alt-c=%d ctrl-c=%d alt-x=%d\n",
        (int)fallout::KEY_ALT_C, (int)fallout::KEY_CTRL_C, (int)fallout::KEY_ALT_X);
    std::printf("ctrl-a=%d alt-m=%d numpad5=%d\n",
        (int)fallout::KEY_CTRL_A, (int)fallout::KEY_ALT_M, (int)fallout::KEY_NUMBERPAD_5);
    std::printf("first-char=%d last-char=%d queue=%d\n",
        (int)fallout::KEY_FIRST_INPUT_CHARACTER, (int)fallout::KEY_LAST_INPUT_CHARACTER,
        KEY_QUEUE_SIZE);
    std::printf("mod-shift=%d mod-ctrl=%d mod-alt=%d any-shift=%d\n",
        KEYBOARD_EVENT_MODIFIER_LEFT_SHIFT, KEYBOARD_EVENT_MODIFIER_LEFT_CONTROL,
        KEYBOARD_EVENT_MODIFIER_LEFT_ALT, KEYBOARD_EVENT_MODIFIER_ANY_SHIFT);
    std::printf("lock-num=%d lock-caps=%d state-up=%d state-down=%d\n",
        MODIFIER_KEY_STATE_NUM_LOCK, MODIFIER_KEY_STATE_CAPS_LOCK,
        KEY_STATE_UP, KEY_STATE_DOWN);
}

void sectionDInput()
{
    std::puts("[dinput]");

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.screen.mouse_lock = false;
    fallout::gProgramIsActive = true;

    std::printf("uses-relative=%d\n", fallout::mouseDeviceUsesRelativeMode() ? 1 : 0);
    std::printf("direct-init=%d\n", fallout::directInputInit() ? 1 : 0);
    std::printf("acquire-mouse=%d acquire-kbd=%d\n",
        fallout::mouseDeviceAcquire() ? 1 : 0,
        fallout::keyboardDeviceAcquire() ? 1 : 0);

    fallout::MouseData data;
    memset(&data, 0xAA, sizeof(data));
    int gotData = fallout::mouseDeviceGetData(&data) ? 1 : 0;
    std::printf("getdata=%d pos=%d,%d buttons=%d,%d wheel=%d,%d\n",
        gotData, data.x, data.y, (int)data.buttons[0], (int)data.buttons[1],
        data.wheelX, data.wheelY);

    SDL_Event wheel;
    memset(&wheel, 0, sizeof(wheel));
    wheel.type = SDL_MOUSEWHEEL;
    wheel.wheel.x = 3;
    wheel.wheel.y = -2;
    fallout::handleMouseEvent(&wheel);
    memset(&data, 0, sizeof(data));
    fallout::mouseDeviceGetData(&data);
    std::printf("wheel=%d,%d\n", data.wheelX, data.wheelY);
    fallout::mouseDeviceGetData(&data);
    std::printf("wheel-drained=%d,%d\n", data.wheelX, data.wheelY);

    // Forced relative mode fails on the dummy driver, but the flag sticks.
    // NOTE: separate statements (argument evaluation order is unspecified).
    fallout::settings.screen.mouse_lock = true;
    int relInit = fallout::mouseDeviceInitMode() ? 1 : 0;
    int relUses = fallout::mouseDeviceUsesRelativeMode() ? 1 : 0;
    std::printf("relmode-init=%d uses-relative=%d\n", relInit, relUses);
    fallout::settings.screen.mouse_lock = false;
    int absInit = fallout::mouseDeviceInitMode() ? 1 : 0;
    int absUses = fallout::mouseDeviceUsesRelativeMode() ? 1 : 0;
    std::printf("absmode-init=%d uses-relative=%d\n", absInit, absUses);

    fallout::KeyboardData kb;
    std::printf("kbd-reset=%d kbd-data=%d kbd-unacquire=%d\n",
        fallout::keyboardDeviceReset() ? 1 : 0,
        fallout::keyboardDeviceGetData(&kb) ? 1 : 0,
        fallout::keyboardDeviceUnacquire() ? 1 : 0);
    std::printf("mouse-unacquire=%d\n", fallout::mouseDeviceUnacquire() ? 1 : 0);
    fallout::mouseDeviceFree();
    fallout::keyboardDeviceFree();
    fallout::directInputFree();
    std::printf("freed=1\n");

    fallout::settingsExit(false);
}

void sectionGraph()
{
    std::puts("[graph]");

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.sound.music_channels = 1;
    fallout::settings.sound.speech_channels = 1;
    fallout::settings.sound.sfx_channels = 4;
    fallout::settings.sound.movie_channels = 1;
    fallout::settings.sound.script_channels = 4;
    fallout::settings.sound.float_channels = 4;
    fallout::settings.sound.pipboy_channels = 1;

    std::printf("channels music=%d speech=%d sfx=%d movie=%d script=%d float=%d pipboy=%d total=%d\n",
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_MUSIC),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_SPEECH),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_SFX),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_MOVIE),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_SCRIPT),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_FLOAT),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_PIPBOY),
        fallout::audioChannelsGetTotal());

    fallout::settings.sound.sfx_channels = 100;
    fallout::settings.sound.script_channels = 0;
    std::printf("clamped sfx=%d script=%d bad-type=%d total=%d\n",
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_SFX),
        fallout::audioChannelsGetCount(fallout::AUDIO_CHANNEL_SCRIPT),
        fallout::audioChannelsGetCount((fallout::AudioChannelType)99),
        fallout::audioChannelsGetTotal());

    fallout::xbaseOpen("/game-en/master.dat");
    fallout::colorPaletteLoad("color.pal");

    std::printf("high-white=%d high-black=%d\n",
        (int)fallout::HighRGB(fallout::_colorTable[fallout::rgb555(0xFFFFFF)]),
        (int)fallout::HighRGB(fallout::_colorTable[fallout::rgb555(0x000000)]));

    char lbm[] = "/src/java/engine/src/test/resources/fixtures/test.lbm";
    unsigned char pixels[64];
    memset(pixels, 0xAA, sizeof(pixels));
    int lbmWidth = fallout::load_lbm_to_buf(lbm, pixels, 0, 0, 5, 3);
    std::printf("lbm=%d crc=%08lX\n", lbmWidth, crcBytes(pixels, 6 * 4));

    unsigned char clip[16];
    memset(clip, 0xAA, sizeof(clip));
    int clipWidth = fallout::load_lbm_to_buf(lbm, clip, 1, 1, 4, 2);
    std::printf("lbm-clip=%d crc=%08lX\n", clipWidth, crcBytes(clip, sizeof(clip)));

    char missing[] = "/tmp/nope.lbm";
    unsigned char miss[8];
    std::printf("lbm-missing=%d\n", fallout::load_lbm_to_buf(missing, miss, 0, 0, 1, 1));

    unsigned char plain[64];
    for (int i = 0; i < 64; i++) {
        plain[i] = (unsigned char)(i / 8);
    }
    unsigned char packed[128];
    memset(packed, 0, sizeof(packed));
    int packedSize = fallout::graphCompress(plain, packed, 64);
    uLong packCrc = packedSize > 0 ? crcBytes(packed, packedSize) : 0;
    std::printf("compress=%d crc=%08lX\n", packedSize, packCrc);

    unsigned char unpacked[64];
    memset(unpacked, 0xAA, sizeof(unpacked));
    int unpackRc = fallout::graphDecompress(packed, packedSize, unpacked, 64);
    std::printf("decompress=%d match=%d\n", unpackRc,
        memcmp(plain, unpacked, 64) == 0 ? 1 : 0);

    unsigned char tiny[4] = { 1, 2, 3, 4 };
    unsigned char tinyOut[4];
    std::printf("decompress-trunc=%d decompress-null=%d\n",
        fallout::graphDecompress(tiny, 4, tinyOut, 64),
        fallout::graphDecompress(nullptr, 0, tinyOut, 4));

    unsigned char gray[16];
    for (int i = 0; i < 16; i++) {
        gray[i] = (unsigned char)(i * 16);
    }
    fallout::grayscalePaletteUpdate(static_cast<fallout::Color>(0),
        static_cast<fallout::Color>(255));
    fallout::grayscalePaletteApply(gray, 4, 4, 4);
    std::printf("grayscale=%08lX\n", crcBytes(gray, sizeof(gray)));

    unsigned char grayPitch[24];
    memset(grayPitch, 0, sizeof(grayPitch));
    for (int i = 0; i < 16; i++) {
        grayPitch[i] = (unsigned char)(i * 16);
    }
    fallout::grayscalePaletteApply(grayPitch, 4, 4, 6);
    std::printf("grayscale-pitch=%08lX\n", crcBytes(grayPitch, sizeof(grayPitch)));

    fallout::settingsExit(false);
}

static void printSchemaDescriptor(const fallout::SettingDescriptor& desc)
{
    std::printf("schema id=[%s] type=%d cat=%d section=[%s] key=[%s] source=[%s]",
        desc.id.c_str(), (int)desc.valueType, (int)desc.category,
        desc.section.c_str(), desc.key.c_str(), desc.source.c_str());
    std::printf(" default=");
    if (std::holds_alternative<bool>(desc.defaultValue)) {
        std::printf("%d", std::get<bool>(desc.defaultValue) ? 1 : 0);
    } else if (std::holds_alternative<int>(desc.defaultValue)) {
        std::printf("%d", std::get<int>(desc.defaultValue));
    } else if (std::holds_alternative<double>(desc.defaultValue)) {
        std::printf("%.2f", std::get<double>(desc.defaultValue));
    } else {
        std::printf("[%s]", std::get<std::string>(desc.defaultValue).c_str());
    }
    std::printf(" label=%d:[%s] apply=%d choices=%d\n",
        desc.labelMessageId, desc.fallbackLabel.c_str(),
        (int)desc.applyPolicy, (int)desc.choices.size());
}

void sectionOptions()
{
    std::puts("[options]");

    {
        FILE* f = fopen("/tmp/schema.cfg", "wb");
        fputs("[screen.resolution_x]\nfile = fallout2.cfg\ntype = int\ncategory = screen\n"
              "default = 640\nlabel_id = 10\nlabel = Resolution Width\n"
              "desc_id = 11\ndescription = Horizontal resolution.\napply = restart\n"
              "[ui.anim_speed]\nfile = fallout2.cfg\ntype = real\ncategory = interface\n"
              "default = 1.0\n"
              "[preferences.combat_speed]\ntype = choice\ncategory = gameplay\n"
              "choices = 0:Slow, 1:Normal, 2:Fast\ndefault = 1\nvanilla = 1\n"
              "[debug.show_fps]\ntype = bool\ncategory = debug\ndefault = 0\n"
              "[ui.motd]\ntype = text\ncategory = interface\ndefault = hello\n"
              "[ui.hotkey]\ntype = keybinding\ncategory = interface\ndefault = 13\n",
            f);
        fclose(f);
        f = fopen("/tmp/schema-bad.cfg", "wb");
        fputs("[screen.ok]\ntype = int\ncategory = screen\ndefault = 1\n"
              "[screen.bad]\ntype = frobnicate\ncategory = screen\ndefault = 1\n",
            f);
        fclose(f);
    }

    fallout::Config schema;
    fallout::configInit(&schema);
    fallout::configRead(&schema, "/tmp/schema.cfg", false);
    std::vector<fallout::SettingDescriptor> descriptors;
    // NOTE: separate statements (argument evaluation order is unspecified).
    bool parsed = fallout::optionsSchemaParse(&schema, &descriptors);
    std::printf("parse-all=%d count=%d\n", parsed ? 1 : 0, (int)descriptors.size());
    for (const auto& desc : descriptors) {
        printSchemaDescriptor(desc);
    }
    fallout::configFree(&schema);

    fallout::Config badSchema;
    fallout::configInit(&badSchema);
    fallout::configRead(&badSchema, "/tmp/schema-bad.cfg", false);
    std::vector<fallout::SettingDescriptor> badDescriptors;
    bool parsedBad = fallout::optionsSchemaParse(&badSchema, &badDescriptors);
    std::printf("parse-bad=%d\n", parsedBad ? 1 : 0);
    fallout::configFree(&badSchema);

    fallout::SettingDescriptor noDot;
    fallout::Config empty;
    fallout::configInit(&empty);
    bool parsedNoDot = fallout::optionsSchemaParseSection(&empty, "screennodot", &noDot);
    bool parsedNull = fallout::optionsSchemaParseSection(nullptr, "screen.x", &noDot);
    std::printf("parse-nodot=%d parse-null=%d\n",
        parsedNoDot ? 1 : 0, parsedNull ? 1 : 0);
    fallout::configFree(&empty);

    char prog[] = "prog";
    char* argv0[] = { prog };
    // Hermetic config: the [settings] section leaves its own file behind.
    {
        FILE* f = fopen("/tmp/fallout2.cfg", "wb");
        fputs("[screen]\nresolution_x=640\n", f);
        fclose(f);
    }
    fallout::settingsInit(false, 1, argv0);

    fallout::OptionsSettingsModel model;
    std::printf("model-edits=%d dirty=%d\n",
        (int)model.edits().size(), model.isDirty() ? 1 : 0);

    std::string error;
    bool setResx = model.setValue("screen.resolution_x", 800, &error);
    std::printf("set-resx=%d\n", setResx ? 1 : 0);
    bool dirtyAll = model.isDirty();
    bool dirtyResx = model.isDirty("screen.resolution_x");
    bool dirtyMusic = model.isDirty("sound.music");
    std::printf("dirty=%d dirty-resx=%d dirty-music=%d\n",
        dirtyAll ? 1 : 0, dirtyResx ? 1 : 0, dirtyMusic ? 1 : 0);
    error.clear();
    bool setUnknown = model.setValue("nope.nope", 1, &error);
    std::printf("set-unknown=%d err=[%s]\n", setUnknown ? 1 : 0, error.c_str());
    error.clear();
    bool setBool = model.setValue("screen.resolution_x", true, &error);
    std::printf("set-bool=%d err=[%s]\n", setBool ? 1 : 0, error.c_str());
    model.rollback();
    std::printf("rollback-dirty=%d\n", model.isDirty() ? 1 : 0);
    error.clear();
    bool resetDefault = model.resetToDefault("screen.resolution_x", &error);
    std::printf("reset-default=%d\n", resetDefault ? 1 : 0);
    model.rollback();

    model.setValue("screen.resolution_x", 800, nullptr);
    error.clear();
    bool committed = model.commit(&error);
    int active = std::get<int>(fallout::settingsGetValue(*findDescriptor("screen.resolution_x")));
    int configured =
        std::get<int>(fallout::settingsGetConfiguredValue(*findDescriptor("screen.resolution_x")));
    std::printf("commit=%d active=%d configured=%d\n", committed ? 1 : 0, active, configured);
    std::printf("commit-dirty=%d\n", model.isDirty() ? 1 : 0);

    char arg1[] = "[screen]resolution_x=1024";
    char* argv[] = { prog, arg1 };
    fallout::settingsExit(false);
    fallout::settingsInit(false, 2, argv);
    fallout::OptionsSettingsModel modelCli;
    error.clear();
    bool setOverridden = modelCli.setValue("screen.resolution_x", 800, &error);
    std::printf("set-overridden=%d err=[%s]\n", setOverridden ? 1 : 0, error.c_str());
    fallout::settingsExit(false);
}

void sectionPtrReg()
{
    std::puts("[ptrreg]");

    // Fresh process: the shared registry is empty, refs start at 1.
    int a = 0;
    int b = 0;
    int rNull = fallout::ptrToInt(nullptr);
    int rA = fallout::ptrToInt(&a);
    int rB = fallout::ptrToInt(&b);
    // NOTE: separate statements (argument evaluation order is unspecified).
    bool fetchA = fallout::intToPtr(rA) == &a;
    bool fetchB = fallout::intToPtr(rB, true) == &b;
    bool fetchBGone = fallout::intToPtr(rB) == nullptr;
    bool fetchZero = fallout::intToPtr(0) == nullptr;
    bool fetchMissing = fallout::intToPtr(999) == nullptr;
    int rC = fallout::ptrToInt(&a);
    std::printf("store-null=%d store-a=%d store-b=%d fetch-a=%d fetch-remove-b=%d fetch-b-gone=%d fetch-zero=%d fetch-missing=%d store-c=%d\n",
        rNull, rA, rB,
        fetchA ? 1 : 0, fetchB ? 1 : 0, fetchBGone ? 1 : 0,
        fetchZero ? 1 : 0, fetchMissing ? 1 : 0, rC);
}

void sectionGameDefs()
{
    std::puts("[gamedefs]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    fallout::Stat s = fallout::STAT_HEALING_RATE;
    fallout::Stat postOld = s++;
    int postNew = static_cast<int>(s);
    bool statValidNeg = fallout::statIsValid(-1);
    bool statValidFirst = fallout::statIsValid(fallout::STAT_FIRST);
    bool statValidLast = fallout::statIsValid(fallout::STAT_COUNT - 1);
    bool statValidCount = fallout::statIsValid(fallout::STAT_COUNT);
    fallout::PcStat pc = fallout::PC_STAT_KARMA;
    fallout::PcStat pcPostOld = pc++;
    int pcPostNew = static_cast<int>(pc);
    bool pcValidNeg = fallout::pcStatIsValid(-1);
    bool pcValidCount = fallout::pcStatIsValid(fallout::PC_STAT_COUNT);
    std::printf("stat-count=%d stat-first=%d special=%d saveable=%d primary-count=%d primary-min=%d primary-max=%d primary-range=%d pc-level-max=%d valid-neg=%d valid-first=%d valid-last=%d valid-count=%d post-old=%d post-new=%d pc-count=%d pc-first=%d pc-post-old=%d pc-post-new=%d pc-valid-neg=%d pc-valid-count=%d\n",
        (int)fallout::STAT_COUNT, (int)fallout::STAT_FIRST,
        (int)fallout::SPECIAL_STAT_COUNT, (int)fallout::SAVEABLE_STAT_COUNT,
        (int)fallout::PRIMARY_STAT_COUNT, PRIMARY_STAT_MIN, PRIMARY_STAT_MAX, PRIMARY_STAT_RANGE,
        PC_LEVEL_MAX,
        statValidNeg ? 1 : 0, statValidFirst ? 1 : 0, statValidLast ? 1 : 0, statValidCount ? 1 : 0,
        (int)postOld, postNew,
        (int)fallout::PC_STAT_COUNT, (int)fallout::PC_STAT_FIRST,
        (int)pcPostOld, pcPostNew,
        pcValidNeg ? 1 : 0, pcValidCount ? 1 : 0);

    fallout::Skill sk = fallout::SKILL_SNEAK;
    fallout::Skill skPostOld = sk++;
    int skPostNew = static_cast<int>(sk);
    bool skValidNeg = fallout::skillIsValid(-1);
    bool skValidCount = fallout::skillIsValid(fallout::SKILL_COUNT);
    std::printf("skill-count=%d skill-first=%d tagged=%d default-tagged=%d sneak=%d valid-neg=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::SKILL_COUNT, (int)fallout::SKILL_FIRST,
        NUM_TAGGED_SKILLS, DEFAULT_TAGGED_SKILLS, (int)fallout::SKILL_SNEAK,
        skValidNeg ? 1 : 0, skValidCount ? 1 : 0,
        (int)skPostOld, skPostNew);

    fallout::Perk pk = fallout::PERK_TAG;
    fallout::Perk pkPostOld = pk++;
    int pkPostNew = static_cast<int>(pk);
    bool pkValidNeg = fallout::perkIsValid(-1);
    bool pkValidCount = fallout::perkIsValid(fallout::PERK_COUNT);
    std::printf("perk-count=%d perk-first=%d tag=%d mutate=%d jinxed=%d valid-neg=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::PERK_COUNT, (int)fallout::PERK_FIRST,
        (int)fallout::PERK_TAG, (int)fallout::PERK_MUTATE, (int)fallout::PERK_JINXED,
        pkValidNeg ? 1 : 0, pkValidCount ? 1 : 0,
        (int)pkPostOld, pkPostNew);

    fallout::Trait tr = fallout::TRAIT_GIFTED;
    fallout::Trait trPostOld = tr++;
    int trPostNew = static_cast<int>(tr);
    bool trValidNeg = fallout::traitIsValid(-1);
    bool trValidCount = fallout::traitIsValid(fallout::TRAIT_COUNT);
    std::printf("trait-count=%d trait-first=%d max-selected=%d gifted=%d valid-neg=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::TRAIT_COUNT, (int)fallout::TRAIT_FIRST,
        TRAITS_MAX_SELECTED_COUNT, (int)fallout::TRAIT_GIFTED,
        trValidNeg ? 1 : 0, trValidCount ? 1 : 0,
        (int)trPostOld, trPostNew);

    fallout::MapHeaderFlags flags = fallout::MAP_HEADER_SAVED;
    flags |= fallout::MAP_HEADER_ELEVATION_0;
    flags |= fallout::MAP_HEADER_ELEVATION_1;
    int flagsOr = static_cast<int>(flags);
    flags &= fallout::MAP_HEADER_ELEVATION_1;
    int flagsAnd = static_cast<int>(flags);
    int notSaved = static_cast<int>(~fallout::MAP_HEADER_SAVED);
    bool elevValid2 = fallout::elevationIsValid(2);
    bool elevValid3 = fallout::elevationIsValid(3);
    bool sqValid9999 = fallout::squareGridTileIsValid(9999);
    bool sqValid10000 = fallout::squareGridTileIsValid(10000);
    bool hexValid39999 = fallout::hexGridTileIsValid(39999);
    bool hexValid40000 = fallout::hexGridTileIsValid(40000);
    std::printf("elev=%d square=%d hex=%d elev-valid2=%d elev-valid3=%d sq-valid9999=%d sq-valid10000=%d hex-valid39999=%d hex-valid40000=%d flags-or=%d flags-and=%d not-saved=%d\n",
        ELEVATION_COUNT, SQUARE_GRID_SIZE, HEX_GRID_SIZE,
        elevValid2 ? 1 : 0, elevValid3 ? 1 : 0,
        sqValid9999 ? 1 : 0, sqValid10000 ? 1 : 0,
        hexValid39999 ? 1 : 0, hexValid40000 ? 1 : 0,
        flagsOr, flagsAnd, notSaved);

    fallout::AnimationRequestOptions opts = fallout::ANIMATION_REQUEST_UNRESERVED;
    opts |= fallout::ANIMATION_REQUEST_RESERVED;
    opts |= fallout::ANIMATION_REQUEST_NO_STAND;
    int optsStd = static_cast<int>(opts);
    int optsPing = static_cast<int>(fallout::ANIMATION_REQUEST_PING | fallout::ANIMATION_REQUEST_INSIGNIFICANT);
    bool animValidNeg = fallout::animationTypeIsValid(-1);
    bool animValidLast = fallout::animationTypeIsValid(fallout::ANIM_COUNT - 1);
    bool animValidCount = fallout::animationTypeIsValid(fallout::ANIM_COUNT);
    std::printf("anim-count=%d anim-first=%d anim-stand=%d anim-fire-cont=%d anim-callshot=%d knock-first=%d knock-last=%d sf-first=%d sf-last=%d opts-std=%d opts-ping=%d valid-neg=%d valid-last=%d valid-count=%d\n",
        (int)fallout::ANIM_COUNT, (int)fallout::ANIM_FIRST,
        (int)fallout::ANIM_STAND, (int)fallout::ANIM_FIRE_CONTINUOUS, (int)fallout::ANIM_CALLED_SHOT_PIC,
        (int)fallout::FIRST_KNOCKDOWN_AND_DEATH_ANIM, (int)fallout::LAST_KNOCKDOWN_AND_DEATH_ANIM,
        (int)fallout::FIRST_SF_DEATH_ANIM, (int)fallout::LAST_SF_DEATH_ANIM,
        optsStd, optsPing,
        animValidNeg ? 1 : 0, animValidLast ? 1 : 0, animValidCount ? 1 : 0);

    // Pristine config: earlier sections never touch the shared content config,
    // but free+init makes the default vector independent of call order.
    fallout::configFree(&fallout::gContentConfig);
    fallout::configInit(&fallout::gContentConfig);
    char buf[VERSION_MAX];
    fallout::versionGetVersion(buf, sizeof(buf));
    std::printf("version-default=[%s]\n", buf);
    fallout::configSetString(&fallout::gContentConfig, CONTENT_CONFIG_MAIN_MENU_SECTION, "version_string",
        "Custom Build 123456789012345678901234567890EXTRA");
    fallout::versionGetVersion(buf, sizeof(buf));
    std::printf("version-custom=[%s] len=%d\n", buf, (int)strlen(buf));
    fallout::configSetString(&fallout::gContentConfig, CONTENT_CONFIG_MAIN_MENU_SECTION, "version_string", "");
    fallout::versionGetVersion(buf, sizeof(buf));
    std::printf("version-empty=[%s]\n", buf);
    fallout::configFree(&fallout::gContentConfig);
}

void sectionArtDefs()
{
    std::puts("[artdefs]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    bool wValidNeg = fallout::weaponAnimationIsValid(-1);
    bool wValidNone = fallout::weaponAnimationIsValid(0);
    bool wValidMax = fallout::weaponAnimationIsValid(15);
    bool wValidOver = fallout::weaponAnimationIsValid(16);
    std::printf("weapon-none=%d weapon-sfallt=%d valid-neg=%d valid-none=%d valid-max=%d valid-over=%d\n",
        (int)fallout::WeaponAnimation::None, (int)fallout::WeaponAnimation::SfallT,
        wValidNeg ? 1 : 0, wValidNone ? 1 : 0, wValidMax ? 1 : 0, wValidOver ? 1 : 0);

    int tileOr = static_cast<int>(fallout::TileFlags::AlwaysHidden | fallout::TileFlags::TemporarilyHidden);
    // The C++ operator| masks with 0xF: (~0 | 2) & 0xF == 15.
    int tileMask = static_cast<int>(~fallout::TileFlags::None | fallout::TileFlags::AlwaysHidden);
    int tileAnd = static_cast<int>(~fallout::TileFlags::TemporarilyHidden & fallout::TileFlags::AlwaysHidden);
    std::printf("tile-or=%d tile-mask=%d tile-and=%d tile-grid=%d tile-last=%d\n",
        tileOr, tileMask, tileAnd,
        (int)fallout::TileFrameId::Grid, (int)fallout::TileFrameId::Last);

    std::printf("dude-looks=%d bg-vault=%d headfidget-bad=%d misc-scroll=%d scenery-ff3=%d\n",
        (int)fallout::DUDE_NATIVE_LOOK_COUNT, (int)fallout::BackgroundFrameId::Vault,
        (int)fallout::HeadFidget::Bad, (int)fallout::MiscFrameId::ScrollBlocker,
        (int)fallout::SceneryFrameId::ForceField3);

    std::printf("iface-first=%d iface-last=%d iface-mainmenu=%d iface-death=%d iface-partyweapup=%d iface-pipnote=%d iface-mvenum=%d\n",
        (int)fallout::InterfaceFrameId::First, (int)fallout::InterfaceFrameId::Last,
        (int)fallout::InterfaceFrameId::MainMenuBackgroundImage,
        (int)fallout::InterfaceFrameId::DeathScene,
        (int)fallout::InterfaceFrameId::PartyWeaponUp,
        (int)fallout::InterfaceFrameId::PipBoyNoteNumbers,
        (int)fallout::InterfaceFrameId::MovementPointsNumbers);

    std::printf("sk-outdoorsman=%d sk-tag=%d sk-jinxed=%d sk-psycho=%d sk-wephandling=%d sk-rep48=%d sk-drug67=%d\n",
        (int)fallout::SkillDexFrameId::Outdoorsman, (int)fallout::SkillDexFrameId::Tag,
        (int)fallout::SkillDexFrameId::Jinxed, (int)fallout::SkillDexFrameId::PsychoAddiction,
        (int)fallout::SkillDexFrameId::WeaponHandling,
        (int)fallout::SkillDexFrameId::Reputation, (int)fallout::SkillDexFrameId::DrugResistant);
}

void kbSimulate(int scancode, int down)
{
    fallout::KeyboardData data;
    data.key = scancode;
    data.down = (char)(down != 0 ? 1 : 0);
    fallout::_kb_simulate_key(&data);
}

void sectionKbFull()
{
    std::puts("[kbfull]");

    // SDL scancode values used by kb.cc (transcribed to SdlScancode.java).
    std::printf("num-scancodes=%d\n", SDL_NUM_SCANCODES);
    std::printf("sc-a=%d sc-b=%d sc-c=%d sc-d=%d sc-e=%d sc-f=%d sc-g=%d sc-h=%d sc-i=%d sc-j=%d sc-k=%d sc-l=%d sc-m=%d\n",
        SDL_SCANCODE_A, SDL_SCANCODE_B, SDL_SCANCODE_C, SDL_SCANCODE_D, SDL_SCANCODE_E, SDL_SCANCODE_F,
        SDL_SCANCODE_G, SDL_SCANCODE_H, SDL_SCANCODE_I, SDL_SCANCODE_J, SDL_SCANCODE_K, SDL_SCANCODE_L,
        SDL_SCANCODE_M);
    std::printf("sc-n=%d sc-o=%d sc-p=%d sc-q=%d sc-r=%d sc-s=%d sc-t=%d sc-u=%d sc-v=%d sc-w=%d sc-x=%d sc-y=%d sc-z=%d\n",
        SDL_SCANCODE_N, SDL_SCANCODE_O, SDL_SCANCODE_P, SDL_SCANCODE_Q, SDL_SCANCODE_R, SDL_SCANCODE_S,
        SDL_SCANCODE_T, SDL_SCANCODE_U, SDL_SCANCODE_V, SDL_SCANCODE_W, SDL_SCANCODE_X, SDL_SCANCODE_Y,
        SDL_SCANCODE_Z);
    std::printf("sc-1=%d sc-2=%d sc-3=%d sc-4=%d sc-5=%d sc-6=%d sc-7=%d sc-8=%d sc-9=%d sc-0=%d\n",
        SDL_SCANCODE_1, SDL_SCANCODE_2, SDL_SCANCODE_3, SDL_SCANCODE_4, SDL_SCANCODE_5,
        SDL_SCANCODE_6, SDL_SCANCODE_7, SDL_SCANCODE_8, SDL_SCANCODE_9, SDL_SCANCODE_0);
    std::printf("sc-return=%d sc-escape=%d sc-backspace=%d sc-tab=%d sc-space=%d\n",
        SDL_SCANCODE_RETURN, SDL_SCANCODE_ESCAPE, SDL_SCANCODE_BACKSPACE, SDL_SCANCODE_TAB, SDL_SCANCODE_SPACE);
    std::printf("sc-minus=%d sc-equals=%d sc-leftbracket=%d sc-rightbracket=%d sc-backslash=%d\n",
        SDL_SCANCODE_MINUS, SDL_SCANCODE_EQUALS, SDL_SCANCODE_LEFTBRACKET, SDL_SCANCODE_RIGHTBRACKET,
        SDL_SCANCODE_BACKSLASH);
    std::printf("sc-semicolon=%d sc-apostrophe=%d sc-grave=%d sc-comma=%d sc-period=%d sc-slash=%d\n",
        SDL_SCANCODE_SEMICOLON, SDL_SCANCODE_APOSTROPHE, SDL_SCANCODE_GRAVE, SDL_SCANCODE_COMMA,
        SDL_SCANCODE_PERIOD, SDL_SCANCODE_SLASH);
    std::printf("sc-capslock=%d sc-f1=%d sc-f2=%d sc-f3=%d sc-f4=%d sc-f5=%d sc-f6=%d sc-f7=%d sc-f8=%d sc-f9=%d sc-f10=%d sc-f11=%d sc-f12=%d\n",
        SDL_SCANCODE_CAPSLOCK, SDL_SCANCODE_F1, SDL_SCANCODE_F2, SDL_SCANCODE_F3, SDL_SCANCODE_F4,
        SDL_SCANCODE_F5, SDL_SCANCODE_F6, SDL_SCANCODE_F7, SDL_SCANCODE_F8, SDL_SCANCODE_F9, SDL_SCANCODE_F10,
        SDL_SCANCODE_F11, SDL_SCANCODE_F12);
    std::printf("sc-insert=%d sc-home=%d sc-pageup=%d sc-delete=%d sc-end=%d sc-pagedown=%d\n",
        SDL_SCANCODE_INSERT, SDL_SCANCODE_HOME, SDL_SCANCODE_PAGEUP, SDL_SCANCODE_DELETE, SDL_SCANCODE_END,
        SDL_SCANCODE_PAGEDOWN);
    std::printf("sc-right=%d sc-left=%d sc-down=%d sc-up=%d sc-numlock=%d sc-scrolllock=%d\n",
        SDL_SCANCODE_RIGHT, SDL_SCANCODE_LEFT, SDL_SCANCODE_DOWN, SDL_SCANCODE_UP,
        SDL_SCANCODE_NUMLOCKCLEAR, SDL_SCANCODE_SCROLLLOCK);
    std::printf("sc-kp-divide=%d sc-kp-multiply=%d sc-kp-minus=%d sc-kp-plus=%d sc-kp-enter=%d\n",
        SDL_SCANCODE_KP_DIVIDE, SDL_SCANCODE_KP_MULTIPLY, SDL_SCANCODE_KP_MINUS, SDL_SCANCODE_KP_PLUS,
        SDL_SCANCODE_KP_ENTER);
    std::printf("sc-kp-1=%d sc-kp-2=%d sc-kp-3=%d sc-kp-4=%d sc-kp-5=%d sc-kp-6=%d sc-kp-7=%d sc-kp-8=%d sc-kp-9=%d sc-kp-0=%d\n",
        SDL_SCANCODE_KP_1, SDL_SCANCODE_KP_2, SDL_SCANCODE_KP_3, SDL_SCANCODE_KP_4, SDL_SCANCODE_KP_5,
        SDL_SCANCODE_KP_6, SDL_SCANCODE_KP_7, SDL_SCANCODE_KP_8, SDL_SCANCODE_KP_9, SDL_SCANCODE_KP_0);
    std::printf("sc-kp-decimal=%d sc-lctrl=%d sc-lshift=%d sc-lalt=%d sc-rctrl=%d sc-rshift=%d sc-ralt=%d\n",
        SDL_SCANCODE_KP_DECIMAL, SDL_SCANCODE_LCTRL, SDL_SCANCODE_LSHIFT, SDL_SCANCODE_LALT,
        SDL_SCANCODE_RCTRL, SDL_SCANCODE_RSHIFT, SDL_SCANCODE_RALT);
    std::printf("kmod-caps=%d kmod-num=%d kmod-scroll=%d\n", KMOD_CAPS, KMOD_NUM, KMOD_SCROLL);

    int initRc = fallout::keyboardInit();
    int initAgainRc = fallout::keyboardInit();
    std::printf("init=%d init-again=%d layout=%d\n",
        initRc, initAgainRc, fallout::keyboardGetLayout());

    fallout::keyboardSetLayout(99);
    int layoutBad = fallout::keyboardGetLayout();
    fallout::keyboardSetLayout(fallout::KEYBOARD_LAYOUT_QWERTY);
    int layoutQwerty = fallout::keyboardGetLayout();
    std::printf("layout-bad=%d layout-qwerty=%d\n", layoutBad, layoutQwerty);

    fallout::keyboardDisable();
    int disabled1 = fallout::keyboardIsDisabled();
    fallout::keyboardEnable();
    int disabled0 = fallout::keyboardIsDisabled();
    std::printf("disabled=%d enabled=%d\n", disabled1, disabled0);

    // Pressed-count wraps in unsigned char; skip lock keys (they would
    // latch modifier state for the vectors below).
    for (int sc = 0; sc < 300; sc++) {
        if (sc == SDL_SCANCODE_CAPSLOCK || sc == SDL_SCANCODE_NUMLOCKCLEAR || sc == SDL_SCANCODE_SCROLLLOCK) {
            continue;
        }
        kbSimulate(sc, 1);
    }
    int pressCount = (int)fallout::gPressedPhysicalKeysCount;
    for (int sc = 0; sc < 300; sc++) {
        if (sc == SDL_SCANCODE_CAPSLOCK || sc == SDL_SCANCODE_NUMLOCKCLEAR || sc == SDL_SCANCODE_SCROLLLOCK) {
            continue;
        }
        kbSimulate(sc, 0);
    }
    int releaseCount = (int)fallout::gPressedPhysicalKeysCount;
    std::printf("press-count=%d release-count=%d\n", pressCount, releaseCount);
    fallout::keyboardReset();
    int resetEmpty = fallout::_kb_getch();
    std::printf("reset-empty=%d\n", resetEmpty);

    kbSimulate(SDL_SCANCODE_A, 1);
    int aKey = fallout::_kb_getch();
    int afterA = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    std::printf("a=%d empty=%d\n", aKey, afterA);

    kbSimulate(SDL_SCANCODE_LSHIFT, 1);
    kbSimulate(SDL_SCANCODE_A, 1);
    int shiftEv = fallout::_kb_getch();
    int shiftA = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    kbSimulate(SDL_SCANCODE_LSHIFT, 0);
    std::printf("shift-ev=%d shift-a=%d\n", shiftEv, shiftA);

    kbSimulate(SDL_SCANCODE_F1, 1);
    int f1 = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_F1, 0);
    std::printf("f1=%d\n", f1);

    kbSimulate(SDL_SCANCODE_LALT, 1);
    kbSimulate(SDL_SCANCODE_X, 1);
    int altEv = fallout::_kb_getch();
    int altX = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_X, 0);
    kbSimulate(SDL_SCANCODE_LALT, 0);
    std::printf("alt-ev=%d alt-x=%d\n", altEv, altX);

    kbSimulate(SDL_SCANCODE_CAPSLOCK, 1);
    int capsEv = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_CAPSLOCK, 0);
    kbSimulate(SDL_SCANCODE_A, 1);
    int capsA = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    kbSimulate(SDL_SCANCODE_LSHIFT, 1);
    kbSimulate(SDL_SCANCODE_A, 1);
    int capsShiftEv = fallout::_kb_getch();
    int capsShiftA = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    kbSimulate(SDL_SCANCODE_LSHIFT, 0);
    kbSimulate(SDL_SCANCODE_CAPSLOCK, 1);
    int capsEv2 = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_CAPSLOCK, 0);
    kbSimulate(SDL_SCANCODE_A, 1);
    int capsOffA = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    std::printf("caps-ev=%d caps-a=%d caps-shift-ev=%d caps-shift-a=%d caps-ev2=%d caps-off-a=%d\n",
        capsEv, capsA, capsShiftEv, capsShiftA, capsEv2, capsOffA);

    for (int i = 0; i < 70; i++) {
        kbSimulate(SDL_SCANCODE_A, 1);
    }
    int drainMatches = 0;
    for (int i = 0; i < 63; i++) {
        if (fallout::_kb_getch() == 97) {
            drainMatches++;
        }
    }
    int drainEmpty = fallout::_kb_getch();
    kbSimulate(SDL_SCANCODE_A, 0);
    std::printf("drain-matches=%d drain-empty=%d\n", drainMatches, drainEmpty);

    kbSimulate(SDL_SCANCODE_A, 1);
    fallout::keyboardReset();
    int resetCount = (int)fallout::gPressedPhysicalKeysCount;
    int resetGetch = fallout::_kb_getch();
    std::printf("reset-count=%d reset-getch=%d\n", resetCount, resetGetch);

    fallout::keyboardFree();
    int freeGetch = fallout::_kb_getch();
    std::printf("free-getch=%d\n", freeGetch);
    // Reset while uninstalled still flushes device state (must not crash).
    fallout::keyboardReset();
}

void sectionCombatDefs()
{
    std::puts("[combatdefs]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    fallout::CriticalEffect ce = fallout::CRITICAL_EFFECT_3;
    fallout::CriticalEffect ceOld = ce++;
    int ceNew = static_cast<int>(ce);
    bool ceValidNeg = fallout::criticalEffectIsValid(-1);
    bool ceValidCount = fallout::criticalEffectIsValid(fallout::CRITICAL_EFFECT_COUNT);
    std::printf("crit-count=%d crit-first=%d valid-neg=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::CRITICAL_EFFECT_COUNT, (int)fallout::CRITICAL_EFFECT_FIRST,
        ceValidNeg ? 1 : 0, ceValidCount ? 1 : 0, (int)ceOld, ceNew);

    fallout::CombatState st = fallout::COMBAT_STATE_IN_COMBAT;
    st |= fallout::COMBAT_STATE_PLAYER_TURN;
    int stOr = static_cast<int>(st);
    st &= fallout::COMBAT_STATE_PLAYER_TURN;
    int stAnd = static_cast<int>(st);
    st &= 0x08u;
    int stAndU = static_cast<int>(st);
    int stNot = static_cast<int>(~fallout::COMBAT_STATE_OUT_COMBAT);
    std::printf("state-or=%d state-and=%d state-andu=%d state-not=%d\n", stOr, stAnd, stAndU, stNot);

    fallout::HitMode hm = fallout::HIT_MODE_PUNCH;
    fallout::HitMode hmOld = hm++;
    int hmNew = static_cast<int>(hm);
    bool hmValidNeg = fallout::hitModeIsValid(-1);
    bool hmValidCount = fallout::hitModeIsValid(fallout::HIT_MODE_COUNT);
    std::printf("hitmode-count=%d hitmode-first=%d punch-first=%d punch-last=%d kick-first=%d kick-last=%d valid-neg=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::HIT_MODE_COUNT, (int)fallout::HIT_MODE_FIRST,
        (int)fallout::FIRST_ADVANCED_PUNCH_HIT_MODE, (int)fallout::LAST_ADVANCED_PUNCH_HIT_MODE,
        (int)fallout::FIRST_ADVANCED_KICK_HIT_MODE, (int)fallout::LAST_ADVANCED_KICK_HIT_MODE,
        hmValidNeg ? 1 : 0, hmValidCount ? 1 : 0, (int)hmOld, hmNew);

    fallout::HitLocation hl = fallout::HIT_LOCATION_GROIN;
    fallout::HitLocation hlOld = hl++;
    int hlNew = static_cast<int>(hl);
    bool hlValidCount = fallout::hitLocationIsValid(fallout::HIT_LOCATION_COUNT);
    std::printf("hitloc-count=%d hitloc-specific=%d hitloc-first=%d uncalled=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::HIT_LOCATION_COUNT, (int)fallout::HIT_LOCATION_SPECIFIC_COUNT,
        (int)fallout::HIT_LOCATION_FIRST, (int)fallout::HIT_LOCATION_UNCALLED,
        hlValidCount ? 1 : 0, (int)hlOld, hlNew);

    fallout::CriticalHitDataMember cm = fallout::CRIT_DATA_MEMBER_FLAGS;
    fallout::CriticalHitDataMember cmOld = cm++;
    int cmNew = static_cast<int>(cm);
    std::printf("critmember-count=%d critmember-first=%d post-old=%d post-new=%d badshot-ok=%d badshot-arms=%d\n",
        (int)fallout::CRIT_DATA_MEMBER_COUNT, (int)fallout::CRIT_DATA_MEMBER_FIRST,
        (int)cmOld, cmNew,
        (int)fallout::COMBAT_BAD_SHOT_OK, (int)fallout::COMBAT_BAD_SHOT_BOTH_ARMS_CRIPPLED);

    fallout::CriticalHitDescription desc;
    desc.damageMultiplier = 3;
    desc.flags = (fallout::Dam)((int)fallout::DAM_CRITICAL | (int)fallout::DAM_DEAD);
    desc.massiveCriticalStat = fallout::STAT_STRENGTH;
    desc.massiveCriticalStatModifier = -2;
    desc.massiveCriticalFlags = fallout::DAM_HIT;
    desc.messageId = 100;
    desc.massiveCriticalMessageId = 101;
    std::printf("critdesc=%d,%d,%d,%d,%d,%d,%d\n",
        desc.values[0], desc.values[1], desc.values[2], desc.values[3],
        desc.values[4], desc.values[5], desc.values[6]);

    fallout::RunAwayMode ra = fallout::RUN_AWAY_MODE_NEVER;
    fallout::RunAwayMode raPostOld = ra++;
    int raPostNew = static_cast<int>(ra);
    fallout::RunAwayMode rd = fallout::RUN_AWAY_MODE_NONE;
    fallout::RunAwayMode rdPostOld = rd--;
    int rdPostNew = static_cast<int>(rd);
    std::printf("runaway-count=%d runaway-first=%d runaway-last=%d post-old=%d post-new=%d postdec-old=%d postdec-new=%d\n",
        (int)fallout::RUN_AWAY_MODE_COUNT, (int)fallout::RUN_AWAY_MODE_FIRST, (int)fallout::RUN_AWAY_MODE_LAST,
        (int)raPostOld, raPostNew, (int)rdPostOld, rdPostNew);

    fallout::Disposition dp = fallout::DISPOSITION_CUSTOM;
    fallout::Disposition dpPostOld = dp--;
    int dpPostNew = static_cast<int>(dp);
    std::printf("area-count=%d area-first=%d best-count=%d best-first=%d dist-count=%d dist-first=%d who-count=%d who-first=%d chem-count=%d chem-first=%d disp-count=%d disp-first=%d disp-postdec-old=%d disp-postdec-new=%d hurt-count=%d hurt-first=%d\n",
        (int)fallout::AREA_ATTACK_MODE_COUNT, (int)fallout::AREA_ATTACK_MODE_FIRST,
        (int)fallout::BEST_WEAPON_COUNT, (int)fallout::BEST_WEAPON_FIRST,
        (int)fallout::DISTANCE_COUNT, (int)fallout::DISTANCE_FIRST,
        (int)fallout::ATTACK_WHO_COUNT, (int)fallout::ATTACK_WHO_FIRST,
        (int)fallout::CHEM_USE_COUNT, (int)fallout::CHEM_USE_FIRST,
        (int)fallout::DISPOSITION_COUNT, (int)fallout::DISPOSITION_FIRST,
        (int)dpPostOld, dpPostNew,
        (int)fallout::HURT_COUNT, (int)fallout::HURT_FIRST);
}

void sectionObjTypes()
{
    std::puts("[objtypes]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    int rotAdd = static_cast<int>(fallout::ROTATION_NE + 2);
    int rotSub = static_cast<int>(fallout::ROTATION_W - 3);
    int rotMod = static_cast<int>(fallout::ROTATION_NW % fallout::ROTATION_COUNT);
    fallout::Rotation rp = fallout::ROTATION_NW;
    fallout::Rotation rpOld = rp++;
    int rpNew = static_cast<int>(rp);
    fallout::Rotation rm = fallout::ROTATION_NE;
    --rm;
    int rmNew = static_cast<int>(rm);
    bool rotValidNeg = fallout::rotationIsValid(-1);
    bool rotValidLast = fallout::rotationIsValid(fallout::ROTATION_LAST);
    bool rotValidCount = fallout::rotationIsValid(fallout::ROTATION_COUNT);
    std::printf("rot-first=%d rot-last=%d rot-count=%d add=%d sub=%d mod=%d post-old=%d post-new=%d pre-new=%d valid-neg=%d valid-last=%d valid-count=%d\n",
        (int)fallout::ROTATION_FIRST, (int)fallout::ROTATION_LAST, (int)fallout::ROTATION_COUNT,
        rotAdd, rotSub, rotMod, (int)rpOld, rpNew, rmNew,
        rotValidNeg ? 1 : 0, rotValidLast ? 1 : 0, rotValidCount ? 1 : 0);

    fallout::ObjectType ot = fallout::OBJ_TYPE_WALL;
    fallout::ObjectType otOld = ot++;
    int otNew = static_cast<int>(ot);
    bool otValidLast = fallout::objectTypeIsValid(fallout::OBJ_TYPE_COUNT - 1);
    bool otValidCount = fallout::objectTypeIsValid(fallout::OBJ_TYPE_COUNT);
    std::printf("objtype-count=%d objtype-first=%d proto-count=%d valid-last=%d valid-count=%d post-old=%d post-new=%d\n",
        (int)fallout::OBJ_TYPE_COUNT, (int)fallout::OBJ_TYPE_FIRST, (int)fallout::OBJ_TYPE_PROTO_COUNT,
        otValidLast ? 1 : 0, otValidCount ? 1 : 0, (int)otOld, otNew);

    int outlineOr = static_cast<int>(fallout::OUTLINE_TYPE_HOSTILE | fallout::OUTLINE_TYPE_ITEM);
    int outlineAnd = static_cast<int>(fallout::OUTLINE_TYPE_HOSTILE & (int)fallout::OUTLINE_TYPE_ITEM);
    fallout::OutlineType ox = fallout::OUTLINE_TYPE_BLOCKED;
    ox &= 0xFFu;
    int outlineAndU = static_cast<int>(ox);
    ox |= (unsigned int)fallout::OUTLINE_TYPE_ITEM;
    int outlineOrU = static_cast<int>(ox);
    std::printf("outline-or=%d outline-and=%d outline-andu=%d outline-oru=%d paletted=%d disabled=%d\n",
        outlineOr, outlineAnd, outlineAndU, outlineOrU, OUTLINE_PALETTED, OUTLINE_DISABLED);

    int inAnyHand = static_cast<int>(fallout::OBJECT_IN_ANY_HAND);
    int equipped = static_cast<int>(fallout::OBJECT_EQUIPPED);
    int flagFc000 = static_cast<int>(fallout::OBJECT_FLAG_0xFC000);
    int openDoor = static_cast<int>(fallout::OBJECT_OPEN_DOOR);
    int flagXor = static_cast<int>(fallout::OBJECT_HIDDEN ^ fallout::OBJECT_NO_SAVE);
    int flagNot = static_cast<int>(~fallout::OBJECT_NONE);
    fallout::ObjectFlags fx = fallout::OBJECT_NO_BLOCK;
    fx |= fallout::OBJECT_LIGHTING;
    int flagOr = static_cast<int>(fx);
    fx ^= fallout::OBJECT_LIGHTING;
    int flagXorEq = static_cast<int>(fx);
    fx &= fallout::OBJECT_NO_BLOCK;
    int flagAndEq = static_cast<int>(fx);
    std::printf("objflag-anyhand=%d objflag-equipped=%d objflag-fc000=%d objflag-opendoor=%d xor=%d not=%d or=%d xoreq=%d andeq=%d\n",
        inAnyHand, equipped, flagFc000, openDoor, flagXor, flagNot, flagOr, flagXorEq, flagAndEq);

    int dudeValid = fallout::dudeStateIsValid(fallout::DUDE_STATE_COUNT - 1) ? 1 : 0;
    int dudeInvalid = fallout::dudeStateIsValid(fallout::DUDE_STATE_COUNT) ? 1 : 0;
    int critOr = static_cast<int>(fallout::CRITTER_NO_STEAL | fallout::CRITTER_NO_DROP);
    int critNot = static_cast<int>(~fallout::CRITTER_NONE);
    fallout::CritterFlags cf = fallout::CRITTER_NO_HEAL;
    cf &= fallout::CRITTER_NO_HEAL;
    int critAndEq = static_cast<int>(cf);
    cf |= fallout::CRITTER_INVULNERABLE;
    int critOrEq = static_cast<int>(cf);
    std::printf("dude-count=%d dude-valid=%d dude-invalid=%d crit-or=%d crit-not=%d crit-andeq=%d crit-oreq=%d container-open=%d door-locked=%d obj-locked=%d obj-jammed=%d\n",
        (int)fallout::DUDE_STATE_COUNT, dudeValid, dudeInvalid, critOr, critNot, critAndEq, critOrEq,
        CONTAINER_FLAG_OPEN, DOOR_FLAG_LOCKED, OBJ_LOCKED, OBJ_JAMMED);

    int manOr = static_cast<int>(fallout::CRITTER_MANEUVER_ENGAGING | fallout::CRITTER_MANEUVER_DISENGAGING);
    int manNot = static_cast<int>(~fallout::CRITTER_MANEUVER_NONE);
    std::printf("maneuver-fleeing=%d man-or=%d man-not=%d\n",
        (int)fallout::CRITTER_MANUEVER_FLEEING, manOr, manNot);

    int damCripLeg = static_cast<int>(fallout::DAM_CRIP_LEG_ANY);
    int damCripArm = static_cast<int>(fallout::DAM_CRIP_ARM_ANY);
    int damCrip = static_cast<int>(fallout::DAM_CRIP);
    int damOr = static_cast<int>(fallout::DAM_DEAD | fallout::DAM_HIT);
    int damNot = static_cast<int>(~fallout::DAM_NONE);
    fallout::Dam dm = fallout::DAM_ON_FIRE;
    dm |= fallout::DAM_BYPASS;
    int damOrEq = static_cast<int>(dm);
    dm &= fallout::DAM_ON_FIRE;
    int damAndEq = static_cast<int>(dm);
    std::printf("dam-cripleg=%d dam-criparm=%d dam-crip=%d dam-or=%d dam-not=%d dam-oreq=%d dam-andeq=%d\n",
        damCripLeg, damCripArm, damCrip, damOr, damNot, damOrEq, damAndEq);

    int bt = fallout::builtTileCreate(0x1ABCDE, 2);
    int btTile = fallout::builtTileGetTile(bt);
    int btElev = fallout::builtTileGetElevation(bt);
    int btRot = static_cast<int>(fallout::builtTileGetRotation(bt));
    int btFull = fallout::builtTileCreate(0x3FFFFFF, 7) | (5 << 26);
    int btFullTile = fallout::builtTileGetTile(btFull);
    int btFullElev = fallout::builtTileGetElevation(btFull);
    int btFullRot = static_cast<int>(fallout::builtTileGetRotation(btFull));
    std::printf("bt=%d bt-tile=%d bt-elev=%d bt-rot=%d btfull=%d btfull-tile=%d btfull-elev=%d btfull-rot=%d tilemask=%d elevmask=%d elevshift=%d rotmask=%d rotshift=%d\n",
        bt, btTile, btElev, btRot, btFull, btFullTile, btFullElev, btFullRot,
        BUILT_TILE_TILE_MASK, BUILT_TILE_ELEVATION_MASK, BUILT_TILE_ELEVATION_SHIFT,
        BUILT_TILE_ROTATION_MASK, BUILT_TILE_ROTATION_SHIFT);

    fallout::Map mp = fallout::MAP_IN_GAME_MOVIE1;
    fallout::Map mpOld = mp++;
    int mpNew = static_cast<int>(mp);
    std::printf("map-transition=%d map-invalid=%d map-first=%d map-arroyo-temple=%d map-movie1=%d post-old=%d post-new=%d\n",
        (int)fallout::MAP_TRANSITION, (int)fallout::MAP_INVALID, (int)fallout::MAP_FIRST,
        (int)fallout::MAP_ARROYO_TEMPLE, (int)fallout::MAP_IN_GAME_MOVIE1, (int)mpOld, mpNew);
}

void sectionPrototypes()
{
    std::puts("[prototypes]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    fallout::ItemType it = fallout::ITEM_TYPE_KEY;
    fallout::ItemType itOld = it++;
    int itNew = static_cast<int>(it);
    fallout::DamageType dt = fallout::DAMAGE_TYPE_FIRE;
    int dtStat = static_cast<int>(fallout::STAT_DAMAGE_THRESHOLD + dt);
    int drStat = static_cast<int>(fallout::STAT_DAMAGE_RESISTANCE + fallout::DAMAGE_TYPE_PLASMA);
    bool dtValid = fallout::damageTypeIsValid(fallout::DAMAGE_TYPE_COUNT);
    bool killValid = fallout::killTypeIsValid(fallout::KILL_TYPE_DEFAULT_COUNT - 1);
    bool killInvalid = fallout::killTypeIsValid(fallout::KILL_TYPE_DEFAULT_COUNT);
    bool killOver = fallout::killTypeOverrideIsValid(fallout::KILL_TYPE_PLAYER);
    bool killOverOut = fallout::killTypeOverrideIsValid(fallout::KILL_TYPE_OVERRIDE_COUNT + 1);
    std::printf("gender-count=%d item-count=%d item-first=%d post-old=%d post-new=%d scenery-count=%d material-count=%d dmg-count=%d dt-fire=%d dr-plasma=%d dt-valid=%d caliber-count=%d race-count=%d body-count=%d kill-default=%d kill-override=%d kill-player=%d kill-valid=%d kill-invalid=%d kill-over=%d kill-over-out=%d\n",
        (int)fallout::GENDER_COUNT,
        (int)fallout::ITEM_TYPE_COUNT, (int)fallout::ITEM_TYPE_FIRST, (int)itOld, itNew,
        (int)fallout::SCENERY_TYPE_COUNT, (int)fallout::MATERIAL_TYPE_COUNT,
        (int)fallout::DAMAGE_TYPE_COUNT, dtStat, drStat, dtValid ? 1 : 0,
        (int)fallout::CALIBER_TYPE_COUNT, (int)fallout::RACE_TYPE_COUNT, (int)fallout::BODY_TYPE_COUNT,
        (int)fallout::KILL_TYPE_DEFAULT_COUNT, (int)fallout::KILL_TYPE_OVERRIDE_COUNT, (int)fallout::KILL_TYPE_PLAYER,
        killValid ? 1 : 0, killInvalid ? 1 : 0, killOver ? 1 : 0, killOverOut ? 1 : 0);

    std::printf("item-armor=%d item-stimpak=%d item-money=%d item-jet=%d item-scorcher=%d critter-dude=%d critter-dogmeat=%d scenery-car=%d scenery-goo1=%d misc-blood=%d misc-exit1=%d misc-exitlast=%d\n",
        (int)fallout::ItemProtoTypeId::PowerArmor, (int)fallout::ItemProtoTypeId::Stimpak,
        (int)fallout::ItemProtoTypeId::Money, (int)fallout::ItemProtoTypeId::Jet,
        (int)fallout::ItemProtoTypeId::SolarScorcher,
        (int)fallout::CritterProtoTypeId::Dude, (int)fallout::CritterProtoTypeId::Dogmeat,
        (int)fallout::SceneryProtoTypeId::Car, (int)fallout::SceneryProtoTypeId::FirstRadioactiveGoo,
        (int)fallout::MiscProtoTypeId::Blood, (int)fallout::MiscProtoTypeId::FirstExitGrid,
        (int)fallout::MiscProtoTypeId::LastExitGrid);

    int protoOr = static_cast<int>(fallout::PROTO_FLAG_FLAT | fallout::PROTO_FLAG_NO_BLOCK);
    int protoShoot = static_cast<int>(fallout::PROTO_FLAG_SHOOT_THRU);
    fallout::ProtoExtendedFlags ef = fallout::PROTO_EXT_FLAG_BIG_GUN;
    ef |= fallout::PROTO_EXT_FLAG_IS_TWO_HANDED;
    int extOr = static_cast<int>(ef);
    ef ^= fallout::PROTO_EXT_FLAG_BIG_GUN;
    int extXor = static_cast<int>(ef);
    std::printf("proto-or=%d proto-shoot=%d ext-or=%d ext-xor=%d ext-hidden=%d list-extent=%d list-max=%d\n",
        protoOr, protoShoot, extOr, extXor,
        (int)fallout::PROTO_EXT_FLAG_HIDDEN, PROTO_LIST_EXTENT_SIZE, PROTO_LIST_MAX_ENTRIES);

    fallout::ProtoId empty = fallout::ProtoId::Empty();
    fallout::ProtoId stim(fallout::ItemProtoTypeId::Stimpak);
    fallout::ProtoId dog(fallout::CritterProtoTypeId::Dogmeat);
    fallout::ProtoId fromPid(40);
    fallout::ProtoId fromPidDog((1 << 24) | 342);
    fallout::ProtoId wideType((7 << 24) | 5);
    bool stimEq = stim == fallout::ItemProtoTypeId::Stimpak;
    bool stimNeCrit = stim != fallout::CritterProtoTypeId::Dude;
    bool fromEq = fromPid == stim;
    fallout::ItemProtoTypeId backToItem = stim.protoId<fallout::ItemProtoTypeId>();
    std::printf("empty-pid=%d empty-haspid=%d empty-valid=%d stim-pid=%d stim-type=%d stim-protoid=%d dog-pid=%d from-pid-type=%d from-pid-protoid=%d from-pid-valid=%d dog-type=%d wide-type=%d wide-valid=%d stim-eq=%d stim-ne-crit=%d from-eq=%d back-to-item=%d\n",
        empty.pid(), empty.hasPid() ? 1 : 0, empty.valid() ? 1 : 0,
        stim.pid(), (int)stim.objectType(), stim.protoId(),
        dog.pid(), (int)fromPid.objectType(), fromPid.protoId(), fromPid.valid() ? 1 : 0,
        (int)fromPidDog.objectType(), (int)wideType.objectType(), wideType.valid() ? 1 : 0,
        stimEq ? 1 : 0, stimNeCrit ? 1 : 0, fromEq ? 1 : 0, (int)backToItem);
    std::printf("mapto-item=%d mapto-critter=%d mapto-scenery=%d mapto-wall=%d mapto-tile=%d mapto-misc=%d\n",
        (int)fallout::MapProtoTypeIdToObjectType<fallout::ItemProtoTypeId>::value,
        (int)fallout::MapProtoTypeIdToObjectType<fallout::CritterProtoTypeId>::value,
        (int)fallout::MapProtoTypeIdToObjectType<fallout::SceneryProtoTypeId>::value,
        (int)fallout::MapProtoTypeIdToObjectType<fallout::WallProtoTypeId>::value,
        (int)fallout::MapProtoTypeIdToObjectType<fallout::TileProtoTypeId>::value,
        (int)fallout::MapProtoTypeIdToObjectType<fallout::MiscProtoTypeId>::value);

    fallout::ProtoId exitGrid((5 << 24) | 16);
    fallout::ProtoId noExit((5 << 24) | 15);
    fallout::ProtoId exitLast((5 << 24) | 23);
    fallout::ProtoId exitOver((5 << 24) | 24);
    std::printf("exitgrid=%d noexit=%d exitlast=%d exitover=%d itemmember-range=%d itemmember-material=%d critmember-headfid=%d scenerymember-data=%d wallmember-material=%d miscmember-max=%d typemember-int=%d typemember-string=%d\n",
        fallout::isExitGridProtoId(exitGrid) ? 1 : 0, fallout::isExitGridProtoId(noExit) ? 1 : 0,
        fallout::isExitGridProtoId(exitLast) ? 1 : 0, fallout::isExitGridProtoId(exitOver) ? 1 : 0,
        (int)fallout::ITEM_DATA_MEMBER_WEAPON_RANGE, (int)fallout::ITEM_DATA_MEMBER_MATERIAL,
        (int)fallout::CRITTER_DATA_MEMBER_HEAD_FID, (int)fallout::SCENERY_DATA_MEMBER_DATA,
        (int)fallout::WALL_DATA_MEMBER_MATERIAL, (int)fallout::MISC_DATA_MEMBER_EXTENDED_FLAGS,
        (int)fallout::PROTO_DATA_MEMBER_TYPE_INT, (int)fallout::PROTO_DATA_MEMBER_TYPE_STRING);
}

int main()
{
    // Unbuffered stdout: engine debug output goes to stderr (SDL_Log),
    // so without this the merged capture would group all stderr first.
    // v1.txt predates this fix (its INFO line sits on top).
    setvbuf(stdout, nullptr, _IONBF, 0);

    // The harness depends on fixtures.sh outputs. Never run the binary
    // directly: degraded /tmp state silently corrupts vectors, and the CWD
    // fallback of relative VFS paths writes scratch files (wtest.bin) into
    // the mounted repo.
    if (access("/tmp/f2test.zip", F_OK) != 0 || access("/tmp/f2xbase_a", F_OK) != 0) {
        std::fprintf(stderr, "refharness: missing fixtures, run via build.sh\n");
        return 2;
    }

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
    sectionFont();
    sectionIfaceFont();
    sectionRegion();
    sectionDatafile();
    sectionCfgMig();
    sectionInput();
    sectionKb();
    sectionDInput();
    sectionGraph();
    sectionOptions();
    sectionPtrReg();
    sectionGameDefs();
    sectionArtDefs();
    sectionKbFull();
    sectionCombatDefs();
    sectionObjTypes();
    sectionPrototypes();
    return 0;
}
