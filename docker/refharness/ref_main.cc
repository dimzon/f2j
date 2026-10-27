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
#include <cstdint>
#include <unistd.h>
#include <variant>
#include <zlib.h>

#include <SDL.h>

#include "color.h"
#include "cycle.h"
#include "dbox.h"
#include "delay.h"
#include "fps_limiter.h"
#include "dictionary.h"
#include "art.h"
#include "animation.h"
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
#include "queue.h"
#include "light.h"
#include "reaction.h"
#include "select_file_list.h"
#include "nevs.h"
#include "sfall_global_vars.h"
#include "sfall_ini.h"
#include "autorun.h"
#include "game_mouse.h"
#include "stat.h"
#include "skill.h"
#include "combat.h"
#include "critter.h"
#include "scripts.h"
#include "sfall_global_scripts.h"
#include "trait.h"
#include "object.h"
#include "interface.h"
#include "elevator.h"
#include "map.h"
#include "map_edge.h"
#include "item.h"
#include "sfall_script_hooks.h"
#include "inventory.h"
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
#include "critter.h"
#include "inventory.h"
#include "item.h"
#include "mouse.h"
#include "mouse_manager.h"
#include "obj_types.h"
#include "perk.h"
#include "proto.h"
#include "proto_instance.h"
#include "proto_types.h"
#include "skill.h"
#include "stat.h"
#include "touch.h"
#include "version.h"
#include "widget.h"
#include "window.h"
#include "window_manager.h"
#include "window_manager_private.h"
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
#include "worldmap.h"
#include "automap.h"

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
#include "tile.h"
#include "party_member.h"
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
    // Hermeticity for later sections: windowManagerInit runs inputInit,
    // which fails when the keyboard is still installed (no output change).
    fallout::keyboardFree();
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

void sectionGameTypes()
{
    std::puts("[gametypes]");

    // NOTE: separate statements (argument evaluation order is unspecified).
    fallout::RadiationLevel rl = fallout::RADIATION_LEVEL_FATAL;
    fallout::RadiationLevel rlOld = rl++;
    int rlNew = static_cast<int>(rl);
    std::printf("radiation-count=%d radiation-first=%d post-old=%d post-new=%d goris-combat=%d goris-robe=%d\n",
        (int)fallout::RADIATION_LEVEL_COUNT, (int)fallout::RADIATION_LEVEL_NONE,
        (int)rlOld, rlNew,
        (int)fallout::kGorisCombatBaseFrameId, (int)fallout::kGorisRobeBaseFrameId);

    fallout::HealingItem hi = fallout::HEALING_ITEM_HEALING_POWDER;
    fallout::HealingItem hiOld = hi++;
    int hiNew = static_cast<int>(hi);
    std::printf("attack-invalid=%d attack-first=%d attack-count=%d healing-first=%d healing-count=%d post-old=%d post-new=%d\n",
        (int)fallout::ATTACK_TYPE_INVALID, (int)fallout::ATTACK_TYPE_FIRST, (int)fallout::ATTACK_TYPE_COUNT,
        (int)fallout::HEALING_ITEM_FIRST, (int)fallout::HEALING_ITEM_COUNT, (int)hiOld, hiNew);

    std::printf("hand-count=%d slot-w=%d slot-h=%d barter-extra=%d useitem-error=%d useitem-ok=%d useitem-remove=%d useitem-drop=%d\n",
        (int)fallout::HAND_COUNT, INVENTORY_SLOT_WIDTH, INVENTORY_SLOT_HEIGHT, fallout::kExpandedBarterExtraSlots,
        (int)fallout::USE_ITEM_RESULT_ERROR, (int)fallout::USE_ITEM_RESULT_OK,
        (int)fallout::USE_ITEM_RESULT_REMOVE, (int)fallout::USE_ITEM_RESULT_DROP);
}

void sectionAbility()
{
    std::puts("[ability]");

    std::printf("stat-err=%d steal-caught=%d steal-success=%d steal-fail=%d perkprop-first=%d perkprop-last=%d perkprop-count=%d\n",
        STAT_ERR_INVALID_STAT,
        (int)fallout::SkillStealResult::Caught, (int)fallout::SkillStealResult::Success,
        (int)fallout::SkillStealResult::Fail,
        (int)fallout::PerkProperty::FrmId, (int)fallout::PerkProperty::Luck,
        (int)fallout::PerkProperty::Luck + 1);
}

void sectionUiTypes()
{
    std::puts("[uitypes]");

    std::printf("wmerr-ok=%d wmerr-running=%d wmerr-title=%d wmerr-input=%d wminit-none=%d wminit-buffered=%d group-cap=%d\n",
        (int)fallout::WINDOW_MANAGER_OK, (int)fallout::WINDOW_MANAGER_ERR_ALREADY_RUNNING,
        (int)fallout::WINDOW_MANAGER_ERR_TITLE_NOT_SET, (int)fallout::WINDOW_MANAGER_ERR_INITIALIZING_INPUT,
        (int)fallout::WINDOW_MANAGER_INIT_FLAG_NONE, (int)fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED,
        BUTTON_GROUP_BUTTON_LIST_CAPACITY);
    std::printf("win-defaults=%d win-dontmove=%d win-movetop=%d win-hidden=%d win-modal=%d win-transparent=%d win-draggable=%d win-managed=%d\n",
        (int)fallout::WINDOW_USE_DEFAULTS, (int)fallout::WINDOW_DONT_MOVE_TOP, (int)fallout::WINDOW_MOVE_ON_TOP,
        (int)fallout::WINDOW_HIDDEN, (int)fallout::WINDOW_MODAL, (int)fallout::WINDOW_TRANSPARENT,
        (int)fallout::WINDOW_DRAGGABLE_BY_BACKGROUND, (int)fallout::WINDOW_MANAGED);
    std::printf("btn-checkable=%d btn-checkondown=%d btn-notoggleoff=%d btn-disabled=%d btn-drag=%d btn-transparent=%d btn-graphic=%d btn-checked=%d btn-radio=%d btn-rightcfg=%d\n",
        (int)fallout::BUTTON_FLAG_CHECKABLE, (int)fallout::BUTTON_FLAG_CHECK_ON_DOWN,
        (int)fallout::BUTTON_FLAG_NO_TOGGLE_OFF, (int)fallout::BUTTON_FLAG_DISABLED,
        (int)fallout::BUTTON_DRAG_HANDLE, (int)fallout::BUTTON_FLAG_TRANSPARENT,
        (int)fallout::BUTTON_FLAG_GRAPHIC, (int)fallout::BUTTON_FLAG_CHECKED, (int)fallout::BUTTON_FLAG_RADIO,
        (int)fallout::BUTTON_FLAG_RIGHT_MOUSE_BUTTON_CONFIGURED);
    std::printf("align-left=%d align-right=%d align-center=%d mbev-count=%d mrbev-count=%d\n",
        (int)fallout::TEXT_ALIGNMENT_LEFT, (int)fallout::TEXT_ALIGNMENT_RIGHT, (int)fallout::TEXT_ALIGNMENT_CENTER,
        (int)fallout::MANAGED_BUTTON_MOUSE_EVENT_COUNT, (int)fallout::MANAGED_BUTTON_RIGHT_MOUSE_EVENT_COUNT);

    std::printf("cursor-w=%d cursor-h=%d cursor-size=%d state-left=%d state-right=%d ev-down=%d ev-repeat=%d ev-up=%d ev-anydown=%d ev-wheel=%d repeat-time=%d sens-min=%.2f sens-max=%.2f\n",
        MOUSE_DEFAULT_CURSOR_WIDTH, MOUSE_DEFAULT_CURSOR_HEIGHT, MOUSE_DEFAULT_CURSOR_SIZE,
        MOUSE_STATE_LEFT_BUTTON_DOWN, MOUSE_STATE_RIGHT_BUTTON_DOWN,
        MOUSE_EVENT_ANY_BUTTON_DOWN, MOUSE_EVENT_ANY_BUTTON_REPEAT, MOUSE_EVENT_ANY_BUTTON_UP,
        MOUSE_EVENT_LEFT_BUTTON_DOWN | MOUSE_EVENT_RIGHT_BUTTON_DOWN, MOUSE_EVENT_WHEEL,
        BUTTON_REPEAT_TIME, MOUSE_SENSITIVITY_MIN, MOUSE_SENSITIVITY_MAX);

    std::printf("gesture-tap=%d gesture-pan=%d gstate-possible=%d gstate-ended=%d mgr-cache=%d mgr-none=%d mgr-static=%d mgr-animated=%d\n",
        (int)fallout::kTap, (int)fallout::kPan, (int)fallout::kPossible, (int)fallout::kEnded,
        MOUSE_MGR_CACHE_CAPACITY, (int)fallout::MOUSE_MANAGER_MOUSE_TYPE_NONE,
        (int)fallout::MOUSE_MANAGER_MOUSE_TYPE_STATIC, (int)fallout::MOUSE_MANAGER_MOUSE_TYPE_ANIMATED);
}

static int testVideoInit()
{
    return 0;
}

static void testVideoExit()
{
}

// The dummy video driver has no SDL surface; presenting is a no-op here.
// All refresh results stay observable in window buffers and _screen_buffer.
static void testScrBlit(unsigned char* src, int src_pitch, int unused, int src_x, int src_y,
    int src_width, int src_height, int dest_x, int dest_y)
{
}

static unsigned int testMouseTime = 0;

static int testMouseTimeProvider()
{
    return (int)testMouseTime;
}

static uLong winBufCrc(int win)
{
    fallout::Window* window = fallout::windowGetWindow(win);
    uLong crc = crc32(0L, Z_NULL, 0);
    if (window != nullptr && window->buffer != nullptr) {
        crc = crc32(crc, window->buffer,
            (uInt)((unsigned int)window->width * (unsigned int)window->height));
    }
    return crc;
}

static void kbSim(int scancode, int down)
{
    fallout::KeyboardData data;
    data.key = scancode;
    data.down = (char)(down != 0 ? 1 : 0);
    fallout::_kb_simulate_key(&data);
}

static void fingerStart(long long fingerId, unsigned int timestamp, float x, float y)
{
    SDL_TouchFingerEvent event;
    memset(&event, 0, sizeof(event));
    event.fingerId = (SDL_FingerID)fingerId;
    event.timestamp = timestamp;
    event.x = x;
    event.y = y;
    fallout::touch_handle_start(&event);
}

static void fingerMove(long long fingerId, unsigned int timestamp, float x, float y)
{
    SDL_TouchFingerEvent event;
    memset(&event, 0, sizeof(event));
    event.fingerId = (SDL_FingerID)fingerId;
    event.timestamp = timestamp;
    event.x = x;
    event.y = y;
    fallout::touch_handle_move(&event);
}

static void fingerEnd(long long fingerId, unsigned int timestamp, float x, float y)
{
    SDL_TouchFingerEvent event;
    memset(&event, 0, sizeof(event));
    event.fingerId = (SDL_FingerID)fingerId;
    event.timestamp = timestamp;
    event.x = x;
    event.y = y;
    fallout::touch_handle_end(&event);
}

void sectionMouseWin()
{
    std::puts("[mousewin]");

    fallout::xbaseOpen("/tmp/f2cursors");

    fallout::_scr_blit = testScrBlit;
    fallout::_mouse_blit = testScrBlit;

    int wmInit = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit=%d\n", wmInit);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);

    int win = fallout::windowCreate(10, 20, 100, 50, (fallout::ColorWithFlags)5, 0);
    int winWidth = fallout::windowGetWidth(win);
    int winHeight = fallout::windowGetHeight(win);
    std::printf("win=%d width=%d height=%d\n", win, winWidth, winHeight);
    fallout::Rect rect;
    fallout::windowGetRect(win, &rect);
    std::printf("rect=%d,%d,%d,%d\n", rect.left, rect.top, rect.right, rect.bottom);
    std::printf("buf-crc=%08lX\n", winBufCrc(win));
    fallout::windowDrawBorder(win);
    std::printf("border-crc=%08lX\n", winBufCrc(win));
    fallout::windowDrawText(win, (char*)"Hi", 90, 2, 2, (fallout::ColorWithFlags)3);
    std::printf("text-crc=%08lX\n", winBufCrc(win));
    fallout::windowDrawLine(win, 0, 0, 99, 49, (fallout::ColorWithFlags)7);
    fallout::windowDrawRect(win, 5, 5, 20, 20, (fallout::ColorWithFlags)9);
    fallout::windowFill(win, 10, 10, 20, 20, (fallout::ColorWithFlags)11);
    std::printf("draw-crc=%08lX\n", winBufCrc(win));
    fallout::windowShow(win);
    int atShown = fallout::windowGetAtPoint(15, 25);
    fallout::windowHide(win);
    int atHidden = fallout::windowGetAtPoint(15, 25);
    std::printf("at-shown=%d at-hidden=%d\n", atShown, atHidden);
    fallout::windowDestroy(win);
    int gone = fallout::windowGetWindow(win) == nullptr ? 1 : 0;
    std::printf("destroyed=%d\n", gone);

    // Mouse engine (cursor starts hidden; show it first).
    fallout::mouseManagerShowMouse();
    int shownHidden = fallout::cursorIsHidden() ? 1 : 0;
    fallout::_mouse_set_position(100, 80);
    int posX = -1;
    int posY = -1;
    fallout::mouseGetPosition(&posX, &posY);
    fallout::mouseGetRect(&rect);
    bool inAll = fallout::_mouse_in(0, 0, 640, 480);
    bool inSmall = fallout::_mouse_in(0, 0, 10, 10);
    bool clickIn = fallout::_mouse_click_in(90, 70, 110, 90);
    bool clickOut = fallout::_mouse_click_in(0, 0, 10, 10);
    std::printf("shown-hidden=%d pos=%d,%d rect=%d,%d,%d,%d in-all=%d in-small=%d click-in=%d click-out=%d\n",
        shownHidden, posX, posY, rect.left, rect.top, rect.right, rect.bottom,
        inAll ? 1 : 0, inSmall ? 1 : 0, clickIn ? 1 : 0, clickOut ? 1 : 0);
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int evDown = fallout::mouseGetEvent();
    int lastButtons = fallout::mouse_get_last_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int evRepeat = fallout::mouseGetEvent();
    fallout::_mouse_simulate_input(0, 0, 0);
    int evUp = fallout::mouseGetEvent();
    std::printf("ev-down=%d last-buttons=%d ev-repeat=%d ev-up=%d\n", evDown, lastButtons, evRepeat, evUp);
    int wheelX = -1;
    int wheelY = -1;
    fallout::mouseGetWheel(&wheelX, &wheelY);
    int noWheelKey = -1;
    fallout::convertMouseWheelToArrowKey(&noWheelKey);
    SDL_Event wheelEvent;
    memset(&wheelEvent, 0, sizeof(wheelEvent));
    wheelEvent.type = SDL_MOUSEWHEEL;
    wheelEvent.wheel.y = 1;
    fallout::handleMouseEvent(&wheelEvent);
    fallout::_mouse_info();
    int wheelEv = fallout::mouseGetEvent();
    int wheelKey = -1;
    fallout::convertMouseWheelToArrowKey(&wheelKey);
    std::printf("wheel=%d,%d nowheel-key=%d wheel-ev=%d wheel-key=%d\n", wheelX, wheelY, noWheelKey, wheelEv, wheelKey);
    fallout::mouseManagerHideMouse();
    int hideHidden = fallout::cursorIsHidden() ? 1 : 0;
    fallout::mouseManagerShowMouse();
    int showHidden = fallout::cursorIsHidden() ? 1 : 0;
    std::printf("hide-hidden=%d show-hidden=%d\n", hideHidden, showHidden);

    // Touch gestures (screen is 640x480 here).
    fallout::touch_set_touchscreen_mode(true);
    int tsMode = fallout::touch_get_touchscreen_mode() ? 1 : 0;
    fallout::touch_set_pan_mode(true);
    int panMode = fallout::touch_get_pan_mode() ? 1 : 0;
    fallout::touch_set_touchscreen_mode(false);
    fallout::touch_set_pan_mode(false);
    fingerStart(1, 1000, 0.1f, 0.2f);
    fingerEnd(1, 1050, 0.1f, 0.2f);
    fallout::touch_process_gesture();
    fallout::Gesture gesture;
    memset(&gesture, 0, sizeof(gesture));
    int gotTap = fallout::touch_get_gesture(&gesture) ? 1 : 0;
    int gotTapAgain = fallout::touch_get_gesture(&gesture) ? 1 : 0;
    std::printf("ts-mode=%d pan-mode=%d tap=%d tap-type=%d tap-state=%d tap-n=%d tap-x=%d tap-y=%d tap-again=%d\n",
        tsMode, panMode, gotTap, (int)gesture.type, (int)gesture.state, gesture.numberOfTouches,
        gesture.x, gesture.y, gotTapAgain);
    fingerStart(2, 2000, 0.5f, 0.5f);
    fingerMove(2, 2100, 0.52f, 0.5f);
    fallout::touch_process_gesture();
    memset(&gesture, 0, sizeof(gesture));
    int gotPan = fallout::touch_get_gesture(&gesture) ? 1 : 0;
    fingerEnd(2, 2200, 0.52f, 0.5f);
    fallout::touch_process_gesture();
    fallout::Gesture gestureEnd;
    memset(&gestureEnd, 0, sizeof(gestureEnd));
    int gotPanEnd = fallout::touch_get_gesture(&gestureEnd) ? 1 : 0;
    std::printf("pan=%d pan-type=%d pan-state=%d pan-n=%d pan-x=%d pan-y=%d panend=%d panend-state=%d\n",
        gotPan, (int)gesture.type, (int)gesture.state, gesture.numberOfTouches, gesture.x, gesture.y,
        gotPanEnd, (int)gestureEnd.state);
    fingerStart(3, 3000, 0.3f, 0.3f);
    SDL_Delay(600);
    fallout::touch_process_gesture();
    memset(&gesture, 0, sizeof(gesture));
    int gotPress = fallout::touch_get_gesture(&gesture) ? 1 : 0;
    fingerEnd(3, 3700, 0.3f, 0.3f);
    fallout::touch_process_gesture();
    memset(&gestureEnd, 0, sizeof(gestureEnd));
    int gotPressEnd = fallout::touch_get_gesture(&gestureEnd) ? 1 : 0;
    std::printf("press=%d press-type=%d press-state=%d press-n=%d pressend=%d pressend-state=%d\n",
        gotPress, (int)gesture.type, (int)gesture.state, gesture.numberOfTouches,
        gotPressEnd, (int)gestureEnd.state);

    // Cursor cache (mounts /tmp/f2cursors above).
    int setAnim = fallout::mouseManagerSetFrame((char*)"cur.anim", 0) ? 1 : 0;
    unsigned char* palette = nullptr;
    int hotX = -1;
    int hotY = -1;
    int foundW = -1;
    int foundH = -1;
    int foundType = -1;
    fallout::MouseManagerCacheEntry* found = fallout::mouseManagerFindCacheEntry((char*)"cur.anim",
        &palette, &hotX, &hotY, &foundW, &foundH, &foundType);
    int setAnimAgain = fallout::mouseManagerSetFrame((char*)"cur.anim", 0) ? 1 : 0;
    int setPtr = fallout::mouseManagerSetMousePointer((char*)"ptr.txt") ? 1 : 0;
    int setShape = fallout::mouseManagerSetMouseShape((char*)"cur.pcx", 5, 6) ? 1 : 0;
    int setMissing = fallout::mouseManagerSetMousePointer((char*)"nope.txt") ? 1 : 0;
    int setBad = fallout::mouseManagerSetFrame((char*)"bad.anim", 0) ? 1 : 0;
    int findHotX = -1;
    int findHotY = -1;
    int foundW2 = -1;
    int foundH2 = -1;
    int foundType2 = -1;
    fallout::mouseManagerFindCacheEntry((char*)"cur.pcx", &palette, &findHotX, &findHotY,
        &foundW2, &foundH2, &foundType2);
    std::printf("set-anim=%d found=%d found-name=[%s] found-w=%d found-h=%d found-type=%d found-hot=%d,%d set-anim-again=%d set-ptr=%d set-shape=%d set-missing=%d set-bad=%d cur-hot=%d,%d cur-type=%d\n",
        setAnim, found != nullptr ? 1 : 0, found != nullptr ? found->fileName : "(null)",
        foundW, foundH, foundType, hotX, hotY,
        setAnimAgain, setPtr, setShape, setMissing, setBad, findHotX, findHotY, foundType2);
    testMouseTime = fallout::gMouseManagerTimeProvider() + 1000;
    fallout::gMouseManagerTimeProvider = testMouseTimeProvider;
    // Re-arm the animated cursor on frame 1 (!= current 0), then advance
    // past its deadline.
    fallout::mouseManagerSetFrame((char*)"cur.anim", 1);
    fallout::mouseManagerUpdate();
    int updHotX = -1;
    int updHotY = -1;
    fallout::mouseManagerFindCacheEntry((char*)"cur.anim", &palette, &updHotX, &updHotY,
        &foundW, &foundH, &foundType);
    fallout::gMouseManagerTimeProvider = fallout::mouseManagerTimeProviderDefaultImpl;
    std::printf("upd-hot=%d,%d\n", updHotX, updHotY);
    fallout::mouseManagerResetMouse();
    fallout::mouseManagerHideMouse();
    int mgrHide = fallout::cursorIsHidden() ? 1 : 0;
    fallout::mouseManagerShowMouse();
    int mgrShow = fallout::cursorIsHidden() ? 1 : 0;
    std::printf("mgr-hide=%d mgr-show=%d\n", mgrHide, mgrShow);

    // Keyboard still works through the shared init.
    kbSim(SDL_SCANCODE_B, 1);
    int bKey = fallout::_kb_getch();
    kbSim(SDL_SCANCODE_B, 0);
    std::printf("kb-b=%d\n", bKey);

    // Leave the system live for [winbtn]; clear the mouse event latch
    // (two zero-sims provably reach the idle path on a quiet device).
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_mouse_simulate_input(0, 0, 0);
}

static int testEnterProcCalls = 0;
static int testEnterProcCode = -99;

static void testEnterProc(int btn, int keyCode)
{
    (void)btn;
    testEnterProcCalls++;
    testEnterProcCode = keyCode;
}

void sectionWinBtn()
{
    std::puts("[winbtn]");

    // Reuses [mousewin]'s live system: re-init would fail at fonts (managers
    // are process-lifetime) and report already-initialized. Reset the cursor
    // to the default frame so the click math below uses hot (1,1).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();

    int win = fallout::windowCreate(10, 10, 120, 80, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(win);

    int btn1 = fallout::buttonCreate(win, 5, 5, 40, 20, 11, 12, 13, 14, nullptr, nullptr, nullptr, 0);
    int btn2 = fallout::buttonCreate(win, 50, 5, 40, 20, 21, 22, 23, 24, nullptr, nullptr, nullptr,
        fallout::BUTTON_FLAG_0x40);
    std::printf("btn1=%d btn2=%d btnwin=%d lastwin=%d\n",
        btn1, btn2, fallout::buttonGetWindowId(btn1), fallout::_win_last_button_winID());

    // Hover btn1 (center at window(10,10) + btn(5,5) + half size).
    fallout::_mouse_set_position(35, 25);
    fallout::_mouse_simulate_input(0, 0, 0);
    int hoverCode = fallout::_win_check_all_buttons();
    // Move away from every button.
    fallout::_mouse_set_position(115, 75);
    fallout::_mouse_simulate_input(0, 0, 0);
    int exitCode = fallout::_win_check_all_buttons();
    std::printf("hover=%d exit=%d\n", hoverCode, exitCode);

    // Click btn1: hover, down, up.
    fallout::_mouse_set_position(35, 25);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int downCode = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    int upCode = fallout::_win_check_all_buttons();
    std::printf("down=%d up=%d\n", downCode, upCode);

    // Right button on unconfigured btn1, then configure and repeat.
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_RIGHT_BUTTON_DOWN);
    int rightUnconf = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    int rightUpUnconf = fallout::_win_check_all_buttons();
    fallout::buttonSetRightMouseCallbacks(btn1, 31, 32, nullptr, nullptr);
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_RIGHT_BUTTON_DOWN);
    int rightDownConf = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    int rightUpConf = fallout::_win_check_all_buttons();
    std::printf("right-unconf=%d right-up-unconf=%d right-down-conf=%d right-up-conf=%d\n",
        rightUnconf, rightUpUnconf, rightDownConf, rightUpConf);

    // Enter proc fires with the code, then the code is cleared (no 0x40).
    fallout::buttonSetMouseCallbacks(btn1, testEnterProc, nullptr, nullptr, nullptr);
    fallout::_mouse_set_position(115, 75);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_set_position(35, 25);
    fallout::_mouse_simulate_input(0, 0, 0);
    int procRet = fallout::_win_check_all_buttons();
    std::printf("proc-calls=%d proc-code=%d proc-ret=%d\n", testEnterProcCalls, testEnterProcCode,
        procRet);
    // btn2 keeps its code (0x40 flag).
    fallout::buttonSetMouseCallbacks(btn2, testEnterProc, nullptr, nullptr, nullptr);
    fallout::_mouse_set_position(115, 75);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_set_position(80, 25);
    fallout::_mouse_simulate_input(0, 0, 0);
    int procRet40 = fallout::_win_check_all_buttons();
    std::printf("proc-ret40=%d\n", procRet40);

    // Checkable button toggle (double pump: first drains btn2's exit).
    int btn3 = fallout::buttonCreate(win, 5, 30, 40, 20, 41, 42, 33, 34, nullptr, nullptr, nullptr,
        fallout::BUTTON_FLAG_CHECKABLE);
    fallout::_mouse_set_position(35, 55);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_win_check_all_buttons();
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int checkDown = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    int checkUp = fallout::_win_check_all_buttons();
    int checkedAfter = fallout::_win_button_down(btn3) ? 1 : 0;
    std::printf("check-down=%d check-up=%d checked=%d\n", checkDown, checkUp, checkedAfter);

    // Disabled button ignores clicks; disabling a hovered button enqueues
    // its exit code (drained here to keep later vectors hermetic).
    fallout::buttonDisable(btn3);
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int disabledDown = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::buttonEnable(btn3);
    int disableExit = fallout::inputGetInput();
    std::printf("disabled-down=%d disable-exit=%d\n", disabledDown, disableExit);

    // Radio group: checking one unchecks the other.
    int btn4 = fallout::buttonCreate(win, 5, 55, 30, 15, 51, 52, 53, 54, nullptr, nullptr, nullptr,
        fallout::BUTTON_FLAG_CHECKABLE);
    int btn5 = fallout::buttonCreate(win, 40, 55, 30, 15, 61, 62, 63, 64, nullptr, nullptr, nullptr,
        fallout::BUTTON_FLAG_CHECKABLE);
    int radioRc = 0;
    {
        int btns[2] = { btn4, btn5 };
        radioRc = fallout::_win_group_radio_buttons(2, btns);
    }
    fallout::_win_set_button_rest_state(btn4, true, 0);
    int restChecked = fallout::_win_button_down(btn4) ? 1 : 0;
    fallout::_mouse_set_position(20, 72);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_win_check_all_buttons();
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    int radio4 = fallout::_win_button_down(btn4) ? 1 : 0;
    int radio5 = fallout::_win_button_down(btn5) ? 1 : 0;
    std::printf("radio=%d rest-checked=%d radio4=%d radio5=%d\n", radioRc, restChecked, radio4,
        radio5);

    // Rest state with enqueue flag feeds inputGetInput.
    fallout::_win_set_button_rest_state(btn5, true, 1);
    int restCode = fallout::inputGetInput();
    std::printf("rest-code=%d\n", restCode);

    // Press-and-release enqueues down/up codes.
    int pressRc = fallout::_win_button_press_and_release(btn1);
    int pressCode1 = fallout::inputGetInput();
    int pressCode2 = fallout::inputGetInput();
    std::printf("press-rc=%d press-code1=%d press-code2=%d\n", pressRc, pressCode1, pressCode2);

    // Text button paints into the window buffer.
    int textBtn = fallout::_win_register_text_button(win, 75, 50, 71, 72, 73, 74, (char*)"OK", 0);
    std::printf("textbtn=%d textbtn-crc=%08lX\n", textBtn, winBufCrc(win));

    // Graphic button with pattern images paints on refresh.
    unsigned char normal[40 * 20];
    for (int i = 0; i < 40 * 20; i++) {
        normal[i] = (unsigned char)(i & 0x3F);
    }
    int gfxBtn = fallout::buttonCreate(win, 75, 5, 40, 20, 81, 82, 83, 84, normal, nullptr, nullptr,
        0);
    fallout::windowRefresh(win);
    std::printf("gfxbtn=%d gfxbtn-crc=%08lX\n", gfxBtn, winBufCrc(win));

    // Masked hit test: opaque pixel hits, transparent misses.
    unsigned char mask[40 * 20];
    for (int i = 0; i < 40 * 20; i++) {
        mask[i] = 0;
    }
    mask[10 * 40 + 20] = 1;
    fallout::buttonSetMask(gfxBtn, mask);
    // Opaque mask pixel: first pump drains the previous hover's exit,
    // second hovers the button.
    fallout::_mouse_set_position(10 + 75 + 20, 10 + 5 + 10);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    int maskHit = fallout::_win_check_all_buttons();
    // Transparent mask pixel inside the rect: exit fires, hover never lands.
    fallout::_mouse_set_position(10 + 75 + 21, 10 + 5 + 10);
    fallout::_mouse_simulate_input(0, 0, 0);
    int maskExit = fallout::_win_check_all_buttons();
    int maskMiss = fallout::_win_check_all_buttons();
    std::printf("mask-hit=%d mask-exit=%d mask-miss=%d\n", maskHit, maskExit, maskMiss);

    // Menu bars pass input through when no pulldown matches.
    int menuPassthrough = fallout::_GNW_check_menu_bars(42);
    std::printf("menu-passthrough=%d\n", menuPassthrough);

    // inputGetInput: queued event, empty queue, mouse-button event.
    fallout::enqueueInputEvent(55);
    int inputCode = fallout::inputGetInput();
    int inputEmpty = fallout::inputGetInput();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int inputMouse = fallout::inputGetInput();
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    std::printf("input-code=%d input-empty=%d input-mouse=%d lastwin=%d\n", inputCode, inputEmpty,
        inputMouse, fallout::_win_last_button_winID());

    fallout::buttonDestroy(btn2);
    int destroyedLookup = fallout::buttonGetWindowId(btn2);
    std::printf("destroyed-lookup=%d\n", destroyedLookup);
}

void sectionWinMenu()
{
    std::puts("[winmenu]");

    // Reuses [winbtn]'s live system: re-init fails (font managers are
    // process-lifetime, like [winbtn] documented) and exit would tear down
    // the shared state. Reset the cursor so click math uses hot (1,1).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();

    int win = fallout::windowCreate(10, 10, 200, 60, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(win);

    // Pulldown without a bar is rejected.
    char* noBarItems[] = { (char*)"Nope" };
    int noBarPulldown = fallout::_win_register_menu_pulldown(win, 2, (char*)"Nope", 99, 1, noBarItems,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    int barRc = fallout::_win_register_menu_bar(win, 0, 0, 200, 12,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    int barAgain = fallout::_win_register_menu_bar(win, 0, 0, 200, 12,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);

    // Bar wider than its window is rejected (separate small window).
    int smallWin = fallout::windowCreate(300, 10, 40, 30, (fallout::ColorWithFlags)5, 0);
    int barTooWide = fallout::_win_register_menu_bar(smallWin, 0, 0, 100, 12,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    fallout::windowDestroy(smallWin);
    std::printf("nobar-pulldown=%d bar=%d bar-again=%d bar-too-wide=%d\n",
        noBarPulldown, barRc, barAgain, barTooWide);

    char* gameItems[] = { (char*)"New", (char*)"Load", (char*)"Exit" };
    int pulldown1 = fallout::_win_register_menu_pulldown(win, 2, (char*)"Game", 101, 3, gameItems,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    char* optItems[] = { (char*)"On", (char*)"Off" };
    int pulldown2 = fallout::_win_register_menu_pulldown(win, 60, (char*)"Opt", 102, 2, optItems,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    fallout::Window* menuWin = fallout::windowGetWindow(win);
    int pulldowns = menuWin != nullptr && menuWin->menuBar != nullptr
        ? menuWin->menuBar->pulldownsLength
        : -1;
    std::printf("pulldown1=%d pulldown2=%d pulldowns=%d menu-crc=%08lX\n",
        pulldown1, pulldown2, pulldowns, winBufCrc(win));

    // Pure helpers: first-letter search is case-insensitive, width is max.
    const char* names[] = { "New", "Load", "Exit" };
    int findL = fallout::_find_first_letter('l', names, 3);
    int findE = fallout::_find_first_letter('E', names, 3);
    int findMiss = fallout::_find_first_letter('z', names, 3);
    int widthNeeded = fallout::_win_width_needed(names, 3);
    int menuPass = fallout::_GNW_check_menu_bars(42);
    std::printf("find-l=%d find-E=%d find-miss=%d width-needed=%d menu-pass=%d\n",
        findL, findE, findMiss, widthNeeded, menuPass);

    // Deleting the bar clears it; re-register works and passthrough holds.
    fallout::_win_delete_menu_bar(win);
    int menuPassAfter = fallout::_GNW_check_menu_bars(42);
    int barReRc = fallout::_win_register_menu_bar(win, 0, 0, 200, 12,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    fallout::_win_delete_menu_bar(win);
    std::printf("menu-pass-after=%d bar-re=%d\n", menuPassAfter, barReRc);

    // Timed messages: two slots, click-dismiss each via its full-window button.
    // Timed windows carry WINDOW_MOVE_ON_TOP; locate them by flag scan so
    // the vectors don't depend on id recycling.
    int tm1 = fallout::win_timed_msg((char*)"Hi", (fallout::ColorWithFlags)3);
    int tm2 = fallout::win_timed_msg((char*)"Bye", (fallout::ColorWithFlags)3);
    int tmWins[2] = { -1, -1 };
    int tmFound = 0;
    for (int id = 1; id < 20 && tmFound < 2; id++) {
        fallout::Window* w = fallout::windowGetWindow(id);
        if (w != nullptr && (w->flags & fallout::WINDOW_MOVE_ON_TOP) != 0) {
            tmWins[tmFound++] = id;
        }
    }
    int tmWin1 = tmWins[0];
    int tmWin2 = tmWins[1];
    fallout::Rect r1 {};
    fallout::windowGetRect(tmWin1, &r1);
    fallout::Rect r2 {};
    fallout::windowGetRect(tmWin2, &r2);
    std::printf("tm1=%d tm2=%d tmwins=%d,%d tmrect1=%d,%d,%d,%d tmrect2=%d,%d,%d,%d\n", tm1, tm2,
        tmWin1, tmWin2,
        r1.left, r1.top, r1.right, r1.bottom, r2.left, r2.top, r2.right, r2.bottom);

    fallout::_mouse_set_position((r1.left + r1.right) / 2, (r1.top + r1.bottom) / 2);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    int tmWin1Gone = fallout::windowGetWindow(tmWin1) == nullptr ? 1 : 0;
    int tmWin2Alive = fallout::windowGetWindow(tmWin2) != nullptr ? 1 : 0;
    std::printf("click1-gone=%d click1-other-alive=%d\n", tmWin1Gone, tmWin2Alive);

    fallout::_mouse_set_position((r2.left + r2.right) / 2, (r2.top + r2.bottom) / 2);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    int tmWin2Gone = fallout::windowGetWindow(tmWin2) == nullptr ? 1 : 0;
    std::printf("click2-gone=%d\n", tmWin2Gone);
}

void sectionWinFrm()
{
    std::puts("[winfrm]");

    // Reuses the live system (re-init is impossible, see [winbtn]/[winmenu]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();

    int win = fallout::windowCreate(10, 100, 200, 100, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(win);

    // Empty FRM id locks nothing.
    int badFrm = fallout::buttonCreateWithFrm(win, 5, 5, 111, 112, 113, 114,
        fallout::InterfaceFrmId {}, fallout::InterfaceFrmId {}, fallout::InterfaceFrmId {}, 0);
    std::printf("badfrm=%d\n", badFrm);

    fallout::InterfaceFrmId upId(fallout::InterfaceFrameId::LittleRedButtonUp);
    fallout::InterfaceFrmId downId(fallout::InterfaceFrameId::LittleRedButtonDown);
    int frmBtn = fallout::buttonCreateWithFrm(win, 5, 5, 111, 112, 113, 114,
        upId, downId, fallout::InterfaceFrmId {}, 0);
    // Freshly created buttons sit at the list head.
    fallout::Window* frmWinPre = fallout::windowGetWindow(win);
    fallout::Button* frmButton = frmWinPre != nullptr ? frmWinPre->buttonListHead : nullptr;
    int frmW = frmButton != nullptr ? frmButton->rect.right - frmButton->rect.left + 1 : -1;
    int frmH = frmButton != nullptr ? frmButton->rect.bottom - frmButton->rect.top + 1 : -1;
    std::printf("frmbtn=%d frm-w=%d frm-h=%d frm-crc=%08lX\n", frmBtn, frmW, frmH, winBufCrc(win));

    // Click it: hover / exit / down / up codes come from the FRM button.
    fallout::Window* frmWin = fallout::windowGetWindow(win);
    int cx = frmWin->rect.left + 5 + frmW / 2;
    int cy = frmWin->rect.top + 5 + frmH / 2;
    fallout::_mouse_set_position(cx, cy);
    fallout::_mouse_simulate_input(0, 0, 0);
    int frmHover = fallout::_win_check_all_buttons();
    fallout::_mouse_set_position(150, 180);
    fallout::_mouse_simulate_input(0, 0, 0);
    int frmExit = fallout::_win_check_all_buttons();
    fallout::_mouse_set_position(cx, cy);
    fallout::_mouse_simulate_input(0, 0, 0);
    fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, MOUSE_STATE_LEFT_BUTTON_DOWN);
    int frmDown = fallout::_win_check_all_buttons();
    fallout::_mouse_simulate_input(0, 0, 0);
    int frmUp = fallout::_win_check_all_buttons();
    std::printf("frm-hover=%d frm-exit=%d frm-down=%d frm-up=%d\n", frmHover, frmExit, frmDown, frmUp);

    // Disabled FRM images: unknown button is rejected, real one repaints.
    int disBad = fallout::buttonSetDisabledFrm(-1, upId, downId, fallout::InterfaceFrmId {});
    int disRc = fallout::buttonSetDisabledFrm(frmBtn, downId, upId, fallout::InterfaceFrmId {});
    fallout::buttonDisable(frmBtn);
    uLong disCrc = winBufCrc(win);
    fallout::buttonEnable(frmBtn);
    uLong enCrc = winBufCrc(win);
    std::printf("dis-bad=%d dis-rc=%d dis-crc=%08lX en-crc=%08lX\n", disBad, disRc, disCrc, enCrc);

    // Raw image re-register: graphic button accepts, text button is rejected.
    int textBtn = fallout::_win_register_text_button(win, 5, 60, 121, 122, 123, 124, (char*)"OK", 0);
    fallout::FrmImage imgLock;
    int imgLockRc = imgLock.lock(upId) ? 1 : 0;
    int imgRc = fallout::_win_register_button_image(frmBtn, imgLock.getData(), nullptr, nullptr, true);
    uLong imgCrc = winBufCrc(win);
    int imgBad = fallout::_win_register_button_image(textBtn, imgLock.getData(), nullptr, nullptr, true);
    std::printf("img-lock=%d img-rc=%d img-crc=%08lX img-bad=%d\n", imgLockRc, imgRc, imgCrc, imgBad);

    fallout::buttonDestroy(frmBtn);
    int destroyedLookup = fallout::buttonGetWindowId(frmBtn);
    std::printf("destroyed-lookup=%d\n", destroyedLookup);
}

void sectionWinDlg()
{
    std::puts("[windlg]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();
    fallout::keyboardSetLayout(fallout::KEYBOARD_LAYOUT_QWERTY);

    // Park the mouse where no window button can fire: modal pumps call
    // _win_check_all_buttons first, so every key must come from _kb_getch.
    fallout::_mouse_set_position(639, 479);
    fallout::_mouse_simulate_input(0, 0, 0);

    // Pure field-width helper: digits of min/max plus the cursor.
    size_t calc1 = fallout::_calc_max_field_chars_wcursor(0, 99);
    size_t calc2 = fallout::_calc_max_field_chars_wcursor(-5, 100);
    size_t calc3 = fallout::_calc_max_field_chars_wcursor(7, 7);
    std::printf("calc=%d,%d,%d\n", (int)calc1, (int)calc2, (int)calc3);

    // Non-modal pulldown window: geometry, paint, then destroy.
    char* items[] = { (char*)"New", (char*)"Load", (char*)"Exit" };
    fallout::Rect pdRect {};
    int pdWin = fallout::_create_pull_down(items, 3, 300, 200,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5, &pdRect);
    uLong pdCrc = winBufCrc(pdWin);
    fallout::windowDestroy(pdWin);
    int pdGone = fallout::windowGetWindow(pdWin) == nullptr ? 1 : 0;
    std::printf("pdwin=%d pdrect=%d,%d,%d,%d pdcrc=%08lX pdgone=%d\n",
        pdWin, pdRect.left, pdRect.top, pdRect.right, pdRect.bottom, pdCrc, pdGone);

    // Modal yes/no: pre-seeded keys answer without any clicks.
    kbSim(SDL_SCANCODE_Y, 1);
    int yesRc = fallout::win_yes_no((char*)"Quit?", 200, 150, (fallout::ColorWithFlags)3);
    kbSim(SDL_SCANCODE_N, 1);
    int noRc = fallout::win_yes_no((char*)"Quit?", 200, 150, (fallout::ColorWithFlags)3);
    std::printf("yes=%d no=%d\n", yesRc, noRc);

    // Modal message: ESC dismisses.
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int msgRc = fallout::_win_msg((char*)"Hello", 200, 150, (fallout::ColorWithFlags)3);
    std::printf("msg=%d\n", msgRc);

    // Modal string entry: two chars + RETURN commits.
    char name[16] = "";
    kbSim(SDL_SCANCODE_A, 1);
    kbSim(SDL_SCANCODE_B, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int getStrRc = fallout::_win_get_str(name, 8, (char*)"Name", 200, 150);
    std::printf("getstr-rc=%d getstr-val=[%s]\n", getStrRc, name);

    // Direct input_str: ESC cancels (value kept), typing + RETURN commits.
    int inWin = fallout::windowCreate(10, 250, 200, 60, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(inWin);
    char ibuf[16] = "Q";
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int inEscRc = fallout::_win_input_str(inWin, ibuf, 10, 16, 16,
        (fallout::ColorWithFlags)3, (fallout::Color)5);
    char tbuf[16] = "Hi";
    kbSim(SDL_SCANCODE_X, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int inTypeRc = fallout::_win_input_str(inWin, tbuf, 10, 16, 16,
        (fallout::ColorWithFlags)3, (fallout::Color)5);
    fallout::windowDestroy(inWin);
    std::printf("inesc-rc=%d inesc-val=[%s] intype-rc=%d intype-val=[%s]\n",
        inEscRc, ibuf, inTypeRc, tbuf);

    // Modal number entry: "42" commits, ESC restores the original.
    int numVal = 5;
    kbSim(SDL_SCANCODE_4, 1);
    kbSim(SDL_SCANCODE_2, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int numRc = fallout::win_get_num_i(&numVal, 0, 99, true, (char*)"Age", 200, 150);
    int numVal2 = 5;
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int numEscRc = fallout::win_get_num_i(&numVal2, 0, 99, true, (char*)"Age", 200, 150);
    std::printf("num-rc=%d num-val=%d numesc-rc=%d numesc-val=%d\n", numRc, numVal, numEscRc, numVal2);
}

static const char* const* gListCbItems = nullptr;
static int gListCbIndex = -99;

static void testListCb(const char* const* items, int index)
{
    gListCbItems = items;
    gListCbIndex = index;
}

void sectionWinMenu2()
{
    std::puts("[winmenu2]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();
    fallout::keyboardSetLayout(fallout::KEYBOARD_LAYOUT_QWERTY);

    // Park the mouse away from every button: the modal pumps below read
    // keys from _kb_getch, mouse input would corrupt the sequences.
    fallout::_mouse_set_position(639, 479);
    fallout::_mouse_simulate_input(0, 0, 0);

    // Standalone pulldown (no menu bar): arrows skip the "" separator.
    char* navItems[] = { (char*)"A", (char*)"", (char*)"C" };
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int navRc = fallout::_win_pull_down(navItems, 3, 300, 200, (fallout::ColorWithFlags)3);
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int navEscRc = fallout::_win_pull_down(navItems, 3, 300, 200, (fallout::ColorWithFlags)3);
    std::printf("nav=%d navesc=%d\n", navRc, navEscRc);

    // Menu-bar processing on a registered bar: DOWN+RETURN picks item 0
    // of pulldown 0 (returns its keyCode, enqueues the item index).
    int mwin = fallout::windowCreate(10, 300, 200, 60, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(mwin);
    fallout::_win_register_menu_bar(mwin, 0, 0, 200, 12,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    char* gameItems[] = { (char*)"New", (char*)"Load", (char*)"Exit" };
    char* optItems[] = { (char*)"On", (char*)"Off" };
    fallout::_win_register_menu_pulldown(mwin, 2, (char*)"Game", 101, 3, gameItems,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    fallout::_win_register_menu_pulldown(mwin, 60, (char*)"Opt", 102, 2, optItems,
        (fallout::ColorWithFlags)3, (fallout::ColorWithFlags)5);
    fallout::MenuBar* menuBar = fallout::windowGetWindow(mwin)->menuBar;
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int menuRet = fallout::_GNW_process_menu(menuBar, 0);
    int menuQueued = fallout::inputGetInput();
    // RIGHT switches to pulldown 1, ESC aborts the whole menu.
    kbSim(SDL_SCANCODE_RIGHT, 1);
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int menuSwitch = fallout::_GNW_process_menu(menuBar, 0);
    std::printf("menuret=%d menuqueued=%d menuswitch=%d\n", menuRet, menuQueued, menuSwitch);
    fallout::_win_delete_menu_bar(mwin);
    fallout::windowDestroy(mwin);

    // List selection: immediate RETURN, arrows, ESC, callback, start offset.
    const char* files[] = { "alpha", "bravo", "CHARLIE", "delta" };
    kbSim(SDL_SCANCODE_RETURN, 1);
    int listNow = fallout::_win_list_select((char*)"Pick", files, 4, nullptr, 200, 100,
        (fallout::ColorWithFlags)3);
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int listNav = fallout::_win_list_select((char*)"Pick", files, 4, nullptr, 200, 100,
        (fallout::ColorWithFlags)3);
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int listEsc = fallout::_win_list_select((char*)"Pick", files, 4, nullptr, 200, 100,
        (fallout::ColorWithFlags)3);
    std::printf("list-now=%d list-nav=%d list-esc=%d\n", listNow, listNav, listEsc);

    gListCbIndex = -99;
    kbSim(SDL_SCANCODE_DOWN, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int listCbRet = fallout::_win_list_select((char*)"Pick", files, 4, testListCb, 200, 100,
        (fallout::ColorWithFlags)3);
    // NOTE: start>0 with a short list overreads the item array upstream
    // (_win_text(items+start, itemsLength)), so the offset/scroll paths use
    // a 12-item list (capacity is 10): start=2 selects index 2, 'g' jumps
    // to index 6 through the first-letter branch.
    const char* files12[] = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l" };
    kbSim(SDL_SCANCODE_RETURN, 1);
    int listAt = fallout::_win_list_select_at((char*)"Pick", files12, 12, nullptr, 200, 100,
        (fallout::ColorWithFlags)3, 2);
    kbSim(SDL_SCANCODE_G, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    int listLetter = fallout::_win_list_select_at((char*)"Pick", files12, 12, nullptr, 200, 100,
        (fallout::ColorWithFlags)3, 0);
    std::printf("listcb-idx=%d listcb-ret=%d listat=%d listletter=%d\n",
        gListCbIndex, listCbRet, listAt, listLetter);
}

static uLong rawBufCrc(unsigned char* buf, size_t size)
{
    uLong crc = crc32(0L, Z_NULL, 0);
    if (buf != nullptr && size != 0) {
        crc = crc32(crc, buf, (uInt)size);
    }
    return crc;
}

void sectionWinText()
{
    std::puts("[wintext]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);

    // Widget font/text attributes (script window state, no window needed).
    int fontBefore = fallout::windowGetFont();
    int setFontRc = fallout::windowSetFont(1);
    int fontAfter = fallout::windowGetFont();
    int setFlagsRc = fallout::scriptWindowSetTextFlags(
        (fallout::DrawTextFlags)(fallout::DRAW_TEXT_FLAG_NO_BG | fallout::DRAW_TEXT_FLAG_UNDERLINED));
    int flagsAfter = (int)fallout::scriptWindowGetTextFlags();
    int setColorRc = fallout::scriptWindowSetTextColor(1.0, 0.0, 0.0);
    int textColor = (int)fallout::scriptWindowGetTextColor();
    int setHiRc = fallout::scriptWindowSetHighlightColor(0.0, 1.0, 0.0);
    int hiColor = (int)fallout::scriptWindowGetHighlightColor();
    fallout::scriptWindowResetTextAttributes();
    int resetFlags = (int)fallout::scriptWindowGetTextFlags();
    int resetColor = (int)fallout::scriptWindowGetTextColor();
    fallout::windowSetFont(fontBefore);
    std::printf("font=%d setfont=%d font-after=%d setflags=%d flags=%d setcolor=%d color=%d sethi=%d hi=%d resetflags=%d resetcolor=%d\n",
        fontBefore, setFontRc, fontAfter, setFlagsRc, flagsAfter, setColorRc, textColor,
        setHiRc, hiColor, resetFlags, resetColor);

    int win = fallout::windowCreate(10, 370, 200, 100, (fallout::ColorWithFlags)5, 0);
    fallout::windowShow(win);

    // Alignment variants paint at different x offsets; maxY guard is a no-op.
    fallout::windowPrintBuf(win, (char*)"Hi", 2, 100, 1000, 2, 2, (fallout::ColorWithFlags)3,
        fallout::TEXT_ALIGNMENT_LEFT);
    uLong leftCrc = winBufCrc(win);
    fallout::windowPrintBuf(win, (char*)"Hi", 2, 100, 1000, 2, 20, (fallout::ColorWithFlags)3,
        fallout::TEXT_ALIGNMENT_CENTER);
    uLong centerCrc = winBufCrc(win);
    fallout::windowPrintBuf(win, (char*)"Hi", 2, 100, 1000, 2, 40, (fallout::ColorWithFlags)3,
        fallout::TEXT_ALIGNMENT_RIGHT);
    uLong rightCrc = winBufCrc(win);
    fallout::windowPrintBuf(win, (char*)"Hi", 2, 100, 10, 2, 60, (fallout::ColorWithFlags)3,
        fallout::TEXT_ALIGNMENT_LEFT);
    uLong guardCrc = winBufCrc(win);
    std::printf("left=%08lX center=%08lX right=%08lX guard=%08lX\n",
        leftCrc, centerCrc, rightCrc, guardCrc);

    // Word wrap splits on width; null yields an empty list.
    int wrapCount = -1;
    char** wrapped = fallout::windowWordWrap((char*)"aa bb cc dd ee", 40, 0, &wrapCount);
    std::printf("wrap-n=%d wrap=[", wrapCount);
    for (int i = 0; i < wrapCount; i++) {
        std::printf("%s%s", i != 0 ? "|" : "", wrapped[i]);
    }
    std::printf("]\n");
    fallout::windowFreeWordList(wrapped, wrapCount);
    int nullCount = -1;
    char** nullWrapped = fallout::windowWordWrap(nullptr, 40, 0, &nullCount);
    std::printf("wrapnull-n=%d wrapnull=%d\n", nullCount, nullWrapped == nullptr ? 1 : 0);

    // Multiline render, plain and spaced.
    fallout::windowWrapLine(win, (char*)"aa bb cc dd ee", 60, 60, 100, 2,
        (fallout::ColorWithFlags)3, fallout::TEXT_ALIGNMENT_LEFT);
    uLong wrapCrc = winBufCrc(win);
    fallout::windowWrapLineWithSpacing(win, (char*)"aa bb cc dd ee", 60, 60, 100, 2,
        (fallout::ColorWithFlags)3, fallout::TEXT_ALIGNMENT_LEFT, 2);
    uLong spacingCrc = winBufCrc(win);
    fallout::windowDestroy(win);
    std::printf("wrapline=%08lX spacing=%08lX\n", wrapCrc, spacingCrc);

    // Pure buffer scalers on fixed patterns.
    unsigned char src4[4 * 4];
    for (int i = 0; i < 16; i++) {
        src4[i] = (unsigned char)(i * 17);
    }
    unsigned char dstUp[8 * 8] = {};
    fallout::_drawScaled(dstUp, 8, 8, 8, src4, 4, 4, 4);
    unsigned char dstSame[4 * 4] = {};
    fallout::_drawScaledBuf(dstSame, 4, 4, src4, 4, 4);
    unsigned char dstDown[2 * 2] = {};
    fallout::_drawScaled(dstDown, 2, 2, 2, src4, 4, 4, 4);
    std::printf("scaled-up=%08lX scaled-same=%08lX scaled-down=%08lX\n",
        rawBufCrc(dstUp, sizeof(dstUp)), rawBufCrc(dstSame, sizeof(dstSame)),
        rawBufCrc(dstDown, sizeof(dstDown)));

    unsigned char tile3[9 * 9];
    for (int i = 0; i < 81; i++) {
        tile3[i] = (unsigned char)(i + 1);
    }
    unsigned char tileDst[12 * 12] = {};
    fallout::_fillBuf3x3(tile3, 9, 9, tileDst, 12, 12);
    std::printf("fill3x3=%08lX\n", rawBufCrc(tileDst, sizeof(tileDst)));
}

void sectionWinDbg()
{
    std::puts("[windbg]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);

    int dbg1 = fallout::_win_debug((char*)"Hi");
    // Debug window sits at 80,80; its center hits nothing else alive.
    int dbgWin = fallout::windowGetAtPoint(230, 176);
    uLong dbgCrc = winBufCrc(dbgWin);
    int dbg2 = fallout::_win_debug((char*)"Hello world, this is a long debug line that wraps!\nsecond");
    uLong dbgCrc2 = winBufCrc(dbgWin);
    std::printf("dbg1=%d dbgwin=%d dbg-crc=%08lX dbg2=%d dbg-crc2=%08lX\n",
        dbg1, dbgWin, dbgCrc, dbg2, dbgCrc2);

    // The Close-button callback destroys the window; the next message
    // recreates it from scratch.
    fallout::_win_debug_delete(-1, -1);
    int dbgGone = fallout::windowGetWindow(dbgWin) == nullptr ? 1 : 0;
    int dbgAtAfter = fallout::windowGetAtPoint(230, 176);
    int dbg3 = fallout::_win_debug((char*)"Again");
    int dbgWin3 = fallout::windowGetAtPoint(230, 176);
    std::printf("dbg-gone=%d dbg-at-after=%d dbg3=%d dbgwin3=%d\n",
        dbgGone, dbgAtAfter, dbg3, dbgWin3);
    fallout::_win_debug_delete(-1, -1);
}

void sectionWinDbox()
{
    std::puts("[windbox]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();
    fallout::keyboardSetLayout(fallout::KEYBOARD_LAYOUT_QWERTY);

    // Park the mouse away from every button: the modal pumps below read
    // keys from _kb_getch, mouse input would corrupt the sequences.
    fallout::_mouse_set_position(639, 479);
    fallout::_mouse_simulate_input(0, 0, 0);

    const char* body[] = { "line one", "line two" };
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int dlgEsc = fallout::showDialogBox((char*)"Hi", body, 2, 200, 150,
        (fallout::ColorWithFlags)3, nullptr, (fallout::ColorWithFlags)5, 0);
    kbSim(SDL_SCANCODE_Y, 1);
    int dlgYes = fallout::showDialogBox((char*)"Hi", body, 2, 200, 150,
        (fallout::ColorWithFlags)3, nullptr, (fallout::ColorWithFlags)5, fallout::DIALOG_BOX_YES_NO);
    kbSim(SDL_SCANCODE_N, 1);
    int dlgNo = fallout::showDialogBox((char*)"Hi", body, 2, 200, 150,
        (fallout::ColorWithFlags)3, nullptr, (fallout::ColorWithFlags)5, fallout::DIALOG_BOX_YES_NO);
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    int dlgClean = fallout::showDialogBox((char*)"Hi", body, 2, 200, 150,
        (fallout::ColorWithFlags)3, nullptr, (fallout::ColorWithFlags)5, fallout::DIALOG_BOX_NO_BUTTONS);
    std::printf("dlg-esc=%d dlg-yes=%d dlg-no=%d dlg-clean=%d\n", dlgEsc, dlgYes, dlgNo, dlgClean);

    kbSim(SDL_SCANCODE_RETURN, 1);
    const char* inpAb = fallout::showInputDialog((char*)"ab", 200, 150, (char*)"Done");
    char inpAbBuf[32] = "(null)";
    if (inpAb != nullptr) {
        snprintf(inpAbBuf, sizeof(inpAbBuf), "%s", inpAb);
    }
    kbSim(SDL_SCANCODE_BACKSPACE, 1);
    kbSim(SDL_SCANCODE_C, 1);
    kbSim(SDL_SCANCODE_RETURN, 1);
    const char* inpAc = fallout::showInputDialog((char*)"ab", 200, 150, (char*)"Done");
    char inpAcBuf[32] = "(null)";
    if (inpAc != nullptr) {
        snprintf(inpAcBuf, sizeof(inpAcBuf), "%s", inpAc);
    }
    kbSim(SDL_SCANCODE_ESCAPE, 1);
    const char* inpEsc = fallout::showInputDialog((char*)"ab", 200, 150, (char*)"Done");
    std::printf("inp-ab=[%s] inp-ac=[%s] inp-escnull=%d\n",
        inpAbBuf, inpAcBuf, inpEsc == nullptr ? 1 : 0);
}

void sectionWinFile()
{
    std::puts("[winfile]");

    // Reuses the live system (re-init is impossible, see [winbtn]).
    int wmInitAgain = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    std::printf("wminit-again=%d\n", wmInitAgain);
    int inputAgain = fallout::inputInit();
    std::printf("input-again=%d\n", inputAgain);
    fallout::mouseSetFrame(nullptr, 0, 0, 0, 0, 0, 0);
    fallout::mouseManagerShowMouse();
    fallout::keyboardSetLayout(fallout::KEYBOARD_LAYOUT_QWERTY);

    // Park the mouse away from every button: the modal pumps below read
    // keys from _kb_getch, mouse input would corrupt the sequences.
    fallout::_mouse_set_position(639, 479);
    fallout::_mouse_simulate_input(0, 0, 0);

    // Load dialog: arrows move, Done (500) copies the selected entry.
    // Dialog codes have no SDL scancodes, and _process_bk appends kb keys
    // after queued events, so everything is enqueued in consumption order.
    char* slots[] = { (char*)"slot1", (char*)"slot2", (char*)"slot3" };
    char loadEscDest[32] = "?";
    fallout::enqueueInputEvent(fallout::KEY_ESCAPE);
    int loadEscRc = fallout::showLoadFileDialog((char*)"Load", slots, loadEscDest, 3, 200, 100, 0);
    char loadDest[32] = "?";
    fallout::enqueueInputEvent(fallout::KEY_ARROW_DOWN);
    fallout::enqueueInputEvent(fallout::KEY_ARROW_DOWN);
    fallout::enqueueInputEvent(500);
    int loadRc = fallout::showLoadFileDialog((char*)"Load", slots, loadDest, 3, 200, 100, 0);
    std::printf("load-rc=%d load-dest=[%s] loadesc-rc=%d loadesc-dest=[%s]\n",
        loadRc, loadDest, loadEscRc, loadEscDest);

    // Empty list: Done gives rc=1 without touching dest.
    char emptyDest[32] = "?";
    fallout::enqueueInputEvent(500);
    int loadEmptyRc = fallout::showLoadFileDialog((char*)"Load", nullptr, emptyDest, 0, 200, 100, 0);
    std::printf("loadempty-rc=%d loadempty-dest=[%s]\n", loadEmptyRc, emptyDest);

    // Save dialog: typing + Done commits the name; empty + Done is rc=1.
    char saveDest[32] = "";
    fallout::enqueueInputEvent('x');
    fallout::enqueueInputEvent('y');
    fallout::enqueueInputEvent(500);
    int saveRc = fallout::showSaveFileDialog((char*)"Save", slots, saveDest, 3, 200, 100, 0);
    char saveEmptyDest[32] = "";
    fallout::enqueueInputEvent(500);
    int saveEmptyRc = fallout::showSaveFileDialog((char*)"Save", slots, saveEmptyDest, 3, 200, 100, 0);
    char saveEscDest[32] = "?";
    fallout::enqueueInputEvent(fallout::KEY_ESCAPE);
    int saveEscRc = fallout::showSaveFileDialog((char*)"Save", slots, saveEscDest, 3, 200, 100, 0);
    std::printf("save-rc=%d save-dest=[%s] saveempty-rc=%d saveesc-rc=%d saveesc-dest=[%s]\n",
        saveRc, saveDest, saveEmptyRc, saveEscRc, saveEscDest);
    // NOTE: no windowManagerExit here on purpose: later sections reuse the
    // live window system (re-init is impossible, see [winbtn]).
}

void sectionProtoDb()
{
    std::puts("[protodb]");

    // No window system needed: proto reads go straight through the VFS.
    // (Deliberately no protoInit: it mkdirs patch dirs and pulls the
    // whole message/stat/perk stack, so names stay unavailable here.)

    char path[256] = {};
    fallout::proto_make_path(path, fallout::ProtoId(40));
    std::printf("path40=[%s]\n", path);

    // Stimpak lazily loads its .pro on first access, then hits the cache.
    fallout::Proto* stim1 = nullptr;
    fallout::Proto* stim2 = nullptr;
    int stimRc1 = fallout::protoGetProto(fallout::ProtoId(40), &stim1);
    int stimRc2 = fallout::protoGetProto(fallout::ProtoId(40), &stim2);
    std::printf("stim-rc=%d,%d same=%d pid=%d msgid=%d fid=%d type=%d material=%d size=%d weight=%d cost=%d light=%d,%d flags=%d\n",
        stimRc1, stimRc2, (stim1 != nullptr && stim1 == stim2) ? 1 : 0,
        stim1 != nullptr ? stim1->item.pid : -999,
        stim1 != nullptr ? stim1->item.messageId : -999,
        stim1 != nullptr ? stim1->item.fid : -999,
        stim1 != nullptr ? (int)stim1->item.type : -999,
        stim1 != nullptr ? (int)stim1->item.material : -999,
        stim1 != nullptr ? stim1->item.size : -999,
        stim1 != nullptr ? stim1->item.weight : -999,
        stim1 != nullptr ? stim1->item.cost : -999,
        stim1 != nullptr ? stim1->item.lightDistance : -999,
        stim1 != nullptr ? stim1->item.lightIntensity : -999,
        stim1 != nullptr ? (int)stim1->item.flags : -999);

    // Integer data members mirror the struct fields above.
    fallout::ProtoDataMemberValue memberValue {};
    int pidType = fallout::protoGetDataMember(fallout::ProtoId(40), fallout::ITEM_DATA_MEMBER_PID, &memberValue);
    int pidVal = memberValue.integerValue;
    int weightType = fallout::protoGetDataMember(fallout::ProtoId(40), fallout::ITEM_DATA_MEMBER_WEIGHT, &memberValue);
    int weightVal = memberValue.integerValue;
    int costType = fallout::protoGetDataMember(fallout::ProtoId(40), fallout::ITEM_DATA_MEMBER_COST, &memberValue);
    int costVal = memberValue.integerValue;
    int badType = fallout::protoGetDataMember(fallout::ProtoId(40), 9999, &memberValue);
    std::printf("member-pid=%d:%d member-weight=%d:%d member-cost=%d:%d member-bad=%d\n",
        pidType, pidVal, weightType, weightVal, costType, costVal, badType);

    // Invalid ids never touch the disk.
    fallout::Proto* badProto = nullptr;
    int emptyRc = fallout::protoGetProto(fallout::ProtoId(), &badProto);
    int hugeRc = fallout::protoGetProto(fallout::ProtoId(16777215), &badProto);
    std::printf("empty-rc=%d huge-rc=%d badptr=%d\n", emptyRc, hugeRc, badProto == nullptr ? 1 : 0);
}

static void noopTileRefresh(fallout::Rect* rect)
{
    (void)rect;
}

static unsigned char tileBuf[640 * 480];

void sectionTileMath()
{
    std::puts("[tilemath]");

    // Headless tile init: grids 200x200 hex / 100x100 square, no map.
    int initRc = fallout::tileInit(nullptr, 100, 100, 200, 200, tileBuf, 640, 480, 640, noopTileRefresh);
    int centerAfterInit = fallout::gCenterTile;
    int setRc = fallout::tileSetCenter(17488, TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);
    int centerAfterSet = fallout::gCenterTile;
    std::printf("init=%d init-center=%d setcenter=%d center=%d\n",
        initRc, centerAfterInit, setRc, centerAfterSet);

    // Rotation offset tables (static data).
    std::printf("off=%d,%d,%d,%d,%d,%d d5=%d,%d,%d,%d,%d,%d\n",
        fallout::_off_tile[0], fallout::_off_tile[1], fallout::_off_tile[2],
        fallout::_off_tile[3], fallout::_off_tile[4], fallout::_off_tile[5],
        fallout::dword_51D984[0], fallout::dword_51D984[1], fallout::dword_51D984[2],
        fallout::dword_51D984[3], fallout::dword_51D984[4], fallout::dword_51D984[5]);

    // The pair from the to-right-of comment (0x4450 vs 0x3BB8).
    int dist = fallout::tileDistanceBetween(17488, 15288);
    int distSame = fallout::tileDistanceBetween(17488, 17488);
    int distBad = fallout::tileDistanceBetween(-1, 17488);
    int inFront = fallout::tileIsInFrontOf(17488, 15288) ? 1 : 0;
    int toRight = fallout::tileIsToRightOf(17488, 15288) ? 1 : 0;
    int rot = (int)fallout::tileGetRotationTo(17488, 15288);
    std::printf("dist=%d dist-same=%d dist-bad=%d infront=%d toright=%d rot=%d\n",
        dist, distSame, distBad, inFront, toRight, rot);

    int step1 = fallout::tileGetTileInDirection(17488, fallout::ROTATION_NE, 1);
    int step3 = fallout::tileGetTileInDirection(17488, fallout::ROTATION_NE, 3);
    int beyond = fallout::_tile_num_beyond(17488, 15288, 2);
    int beyond0 = fallout::_tile_num_beyond(17488, 15288, 0);
    std::printf("step1=%d step3=%d beyond=%d beyond0=%d\n", step1, step3, beyond, beyond0);

    int edge0 = fallout::tileIsEdge(0) ? 1 : 0;
    int edge199 = fallout::tileIsEdge(199) ? 1 : 0;
    int edgeMid = fallout::tileIsEdge(20100) ? 1 : 0;
    int edgeBad = fallout::tileIsEdge(-1) ? 1 : 0;
    int sq = fallout::squareTileFromTile(20100);
    int sqBad = fallout::squareTileFromTile(-1);
    std::printf("edge0=%d edge199=%d edgemid=%d edgebad=%d sq=%d sqbad=%d\n",
        edge0, edge199, edgeMid, edgeBad, sq, sqBad);

    // Screen roundtrip through the current center.
    int sx = -1;
    int sy = -1;
    int toScrRc = fallout::tileToScreenXY(17488, &sx, &sy);
    int backTile = fallout::tileFromScreenXY(sx, sy);
    int badScr = fallout::tileToScreenXY(-1, &sx, &sy);
    int outsideTile = fallout::tileFromScreenXY(-1000, -1000);
    std::printf("toscr=%d back=%d badscr=%d outside=%d\n", toScrRc, backTile, badScr, outsideTile);

    fallout::tileExit();
}

void sectionSquareMath()
{
    std::puts("[squaremath]");

    // Hermetic re-init (same geometry as [tilemath]); tileExit is a no-op.
    int initRc = fallout::tileInit(nullptr, 100, 100, 200, 200, tileBuf, 640, 480, 640, noopTileRefresh);
    fallout::tileSetCenter(17488, TILE_SET_CENTER_FLAG_IGNORE_SCROLL_RESTRICTIONS);

    // Square 5050 is hex 20100 (see [tilemath]); elevation is ignored.
    int sx = -1;
    int sy = -1;
    int toScrRc = fallout::squareTileToScreenXY(5050, &sx, &sy, 0);
    int px = sx;
    int py = sy;
    int backSq = fallout::squareTileFromScreenXY(sx, sy, 0);
    int badScr = fallout::squareTileToScreenXY(-1, &sx, &sy, 0);
    int outsideSq = fallout::squareTileFromScreenXY(-1000, -1000, 0);
    std::printf("init=%d toscr=%d back=%d badscr=%d outside=%d\n",
        initRc, toScrRc, backSq, badScr, outsideSq);

    int rx = -1;
    int ry = -1;
    int roofRc = fallout::squareTileToRoofScreenXY(5050, &rx, &ry, 0);
    int cx = -999;
    int cy = -999;
    fallout::squareTileScreenToCoord(px, py, 0, &cx, &cy);
    int rcx = -999;
    int rcy = -999;
    fallout::squareTileScreenToCoordRoof(rx, ry, 0, &rcx, &rcy);
    std::printf("roof=%d roofxy=%d,%d coord=%d,%d roofcoord=%d,%d\n",
        roofRc, rx, ry, cx, cy, rcx, rcy);

    fallout::tileExit();
}

void sectionTileInRect()
{
    std::puts("[tileinrect]");

    // opTileInTileRect (src/interpreter_extra.cc) pops five tiles: the
    // tested tile first (top of stack, the last script arg), then the
    // fourth, third, second and first script args. The second and third
    // are dropped; minX/maxY come from the fourth arg, maxX/minY from
    // the first. tile = y * 200 + x.
    struct TileInRectCase {
        int tile1;
        int tile2;
        int tile3;
        int tile4;
        int tile;
    };
    static const TileInRectCase cases[] = {
        { 2020, 0, 0, 4010, 3015 }, // (15,15) inside [10..20]x[10..20]
        { 2020, 0, 0, 4010, 2010 }, // (10,10) lower corner, inclusive
        { 2020, 0, 0, 4010, 4020 }, // (20,20) upper corner, inclusive
        { 2020, 0, 0, 4010, 3021 }, // (21,15) x outside
        { 2020, 0, 0, 4010, 4215 }, // (15,21) y outside
        { 2020, 0, 0, 4010, 1809 }, // (9,9) both outside
        { 2020, 0, 0, 4010, -1 }, // no tile
        { 2020, 0, 0, 4010, 40000 }, // (0,200) far outside
        { 2020, 1999, 3999, 4010, 3015 }, // dropped corners differ, same rect
        { 4010, 0, 0, 2020, 3015 }, // swapped corners: empty range
    };

    for (const TileInRectCase& c : cases) {
        // Pop order of the original: tile, tile4, tile3, tile2, tile1.
        int points[5] = { c.tile, c.tile4, c.tile3, c.tile2, c.tile1 };

        int x = points[0] % 200;
        int y = points[0] / 200;

        int minX = points[1] % 200;
        int maxX = points[4] % 200;

        int minY = points[4] / 200;
        int maxY = points[1] / 200;

        int result = 0;
        if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
            result = 1;
        }

        std::printf("t1=%d t4=%d tile=%d in=%d\n", c.tile1, c.tile4, c.tile, result);
    }
}

static int testQueueClearCalls = 0;

static int testQueueClearFn(fallout::Object* owner, void* data)
{
    (void)owner;
    (void)data;
    testQueueClearCalls++;
    return 1;
}

void sectionEventQueue()
{
    std::puts("[eventqueue]");

    // No window system needed: game time is 302400 (static init).
    fallout::queueInit();
    int emptyInit = fallout::queueIsEmpty() ? 1 : 0;
    unsigned int nextEmpty = fallout::queueGetNextEventTime();
    // Opaque data cookies: never dereferenced (these types have no
    // free/read/write procs), only null-tested through find.
    int add1 = fallout::queueAddEvent(100, nullptr, (void*)(uintptr_t)0x1001, fallout::EVENT_TYPE_KNOCKOUT);
    int add2 = fallout::queueAddEvent(50, nullptr, (void*)(uintptr_t)0x1002, fallout::EVENT_TYPE_SNEAK);
    int add3 = fallout::queueAddEvent(150, nullptr, (void*)(uintptr_t)0x1003, fallout::EVENT_TYPE_KNOCKOUT);
    unsigned int nextTime = fallout::queueGetNextEventTime();
    int emptyAfter = fallout::queueIsEmpty() ? 1 : 0;
    int hasKnock = fallout::queueHasEvent(nullptr, fallout::EVENT_TYPE_KNOCKOUT) ? 1 : 0;
    int hasDrug = fallout::queueHasEvent(nullptr, fallout::EVENT_TYPE_DRUG) ? 1 : 0;
    std::printf("empty-init=%d next-empty=%u add=%d,%d,%d next=%u empty=%d has-knock=%d has-drug=%d\n",
        emptyInit, nextEmpty, add1, add2, add3, nextTime, emptyAfter, hasKnock, hasDrug);

    // Find chain walks the two KNOCKOUT nodes, then exhausts.
    void* found1 = fallout::queueFindFirstEvent(nullptr, fallout::EVENT_TYPE_KNOCKOUT);
    void* found2 = fallout::queueFindNextEvent(nullptr, fallout::EVENT_TYPE_KNOCKOUT);
    void* found3 = fallout::queueFindNextEvent(nullptr, fallout::EVENT_TYPE_KNOCKOUT);
    std::printf("find=%d,%d,%d\n",
        found1 != nullptr ? 1 : 0, found2 != nullptr ? 1 : 0, found3 != nullptr ? 1 : 0);

    // Processing with no due events touches no handlers (game time is 0).
    int procIdle = fallout::queueProcessEvents();
    unsigned int nextIdle = fallout::queueGetNextEventTime();
    int remType = fallout::queueRemoveEventsByType(nullptr, fallout::EVENT_TYPE_KNOCKOUT);
    int hasKnockAfter = fallout::queueHasEvent(nullptr, fallout::EVENT_TYPE_KNOCKOUT) ? 1 : 0;
    std::printf("proc-idle=%d next-idle=%u remtype=%d has-knock-after=%d\n",
        procIdle, nextIdle, remType, hasKnockAfter);

    // Save/clear/load roundtrip through a scratch file (no serde procs).
    fallout::File* saveStream = fallout::fileOpen((char*)"/tmp/qev.bin", (char*)"wb");
    int saveRc = saveStream != nullptr ? fallout::queueSave(saveStream) : -99;
    if (saveStream != nullptr) {
        fallout::fileClose(saveStream);
    }
    fallout::queueClear();
    int emptyCleared = fallout::queueIsEmpty() ? 1 : 0;
    fallout::File* loadStream = fallout::fileOpen((char*)"/tmp/qev.bin", (char*)"rb");
    int loadRc = loadStream != nullptr ? fallout::queueLoad(loadStream) : -99;
    if (loadStream != nullptr) {
        fallout::fileClose(loadStream);
    }
    unsigned int nextLoaded = fallout::queueGetNextEventTime();
    int hasSneakLoaded = fallout::queueHasEvent(nullptr, fallout::EVENT_TYPE_SNEAK) ? 1 : 0;
    std::printf("save=%d cleared=%d load=%d next-loaded=%u has-sneak=%d\n",
        saveRc, emptyCleared, loadRc, nextLoaded, hasSneakLoaded);

    // Conditional clear counts + drops the survivors; exit drains all.
    testQueueClearCalls = 0;
    fallout::queueClearByEventType(fallout::EVENT_TYPE_SNEAK, testQueueClearFn);
    int emptyAfterClear = fallout::queueIsEmpty() ? 1 : 0;
    int exitRc = fallout::queueExit();
    std::printf("clear-calls=%d empty-after-clear=%d exit=%d\n",
        testQueueClearCalls, emptyAfterClear, exitRc);
}

void sectionLightReact()
{
    std::puts("[lightreact]");

    // No window system needed; tile arrays are compile-time sized.
    // (Ambient set needs gDude perks and screen refresh — skipped.)
    int initRc = fallout::lightInit();
    int ambient = fallout::lightGetAmbientIntensity();
    int baseTile = fallout::lightGetTileIntensity(0, 20100);
    fallout::lightSetTileIntensity(0, 20100, 70000);
    int clampedTile = fallout::lightGetTileIntensity(0, 20100);
    int trueTile = fallout::lightGetTrueTileIntensity(0, 20100);
    fallout::lightIncreaseTileIntensity(0, 20100, 100);
    int afterInc = fallout::lightGetTrueTileIntensity(0, 20100);
    fallout::lightDecreaseTileIntensity(0, 20100, 200);
    int afterDec = fallout::lightGetTrueTileIntensity(0, 20100);
    int badElev = fallout::lightGetTileIntensity(9, 20100);
    int badTile = fallout::lightGetTileIntensity(0, -1);
    fallout::lightSetTileIntensity(9, 20100, 5);
    fallout::lightResetTileIntensity();
    int afterReset = fallout::lightGetTileIntensity(0, 20100);
    std::printf("init=%d ambient=%d base=%d clamped=%d true=%d inc=%d dec=%d badelev=%d badtile=%d reset=%d\n",
        initRc, ambient, baseTile, clampedTile, trueTile, afterInc, afterDec, badElev, badTile, afterReset);

    // Reaction levels off thresholds; custom thresholds re-map them.
    int bad = (int)fallout::reactionTranslateValue(-50);
    int edge = (int)fallout::reactionTranslateValue(-10);
    int neutral = (int)fallout::reactionTranslateValue(0);
    int good = (int)fallout::reactionTranslateValue(11);
    fallout::reactionSetThresholds(0, 100);
    int customLow = (int)fallout::reactionTranslateValue(0);
    int customMid = (int)fallout::reactionTranslateValue(50);
    int customHigh = (int)fallout::reactionTranslateValue(101);
    fallout::reactionResetThresholds();
    int resetNeutral = (int)fallout::reactionTranslateValue(0);
    std::printf("bad=%d edge=%d neutral=%d good=%d custom=%d,%d,%d reset=%d\n",
        bad, edge, neutral, good, customLow, customMid, customHigh, resetNeutral);
}

void sectionGameMouseUtil()
{
    std::puts("[gmouseutil]");

    // Pure type facts (engine needs a map; these enumerations don't).
    std::printf("modes=%d first-skill=%d skills=%d items=%d cursors=%d first-anim=%d scroll-invalid-off=%d\n",
        (int)fallout::GAME_MOUSE_MODE_COUNT,
        (int)fallout::FIRST_GAME_MOUSE_MODE_SKILL,
        (int)fallout::GAME_MOUSE_MODE_SKILL_COUNT,
        (int)fallout::GAME_MOUSE_ACTION_MENU_ITEM_COUNT,
        (int)fallout::MOUSE_CURSOR_TYPE_COUNT,
        (int)fallout::FIRST_GAME_MOUSE_ANIMATED_CURSOR,
        (int)fallout::MOUSE_CURSOR_SCROLL_OFFSET_INVALID);

    // Sorted VFS file list across the mounted xbases.
    int msgCount = -1;
    char** msgList = fallout::_getFileList((char*)"text\\english\\game\\*.msg", &msgCount);
    std::printf("msgs=%d first=[%s] last=[%s]\n",
        msgCount,
        msgCount > 0 ? msgList[0] : "(none)",
        msgCount > 0 ? msgList[msgCount - 1] : "(none)");
    fallout::_freeFileList(msgList);
    int noneCount = -1;
    char** noneList = fallout::_getFileList((char*)"no\\such\\*.zzz", &noneCount);
    std::printf("none=%d nonenull=%d\n", noneCount, noneList == nullptr ? 1 : 0);

    // Autorun mutex is a Windows-only no-op elsewhere.
    bool mutexRc = fallout::autorunMutexCreate();
    fallout::autorunMutexClose();
    std::printf("autorun=%d\n", mutexRc ? 1 : 0);
}

void sectionStatSkill()
{
    std::puts("[statskill]");

    // No window system needed. Pure tables first (need no init).
    int cost0 = fallout::skillsGetCost(0);
    int cost100 = fallout::skillsGetCost(100);
    int cost101 = fallout::skillsGetCost(101);
    int cost126 = fallout::skillsGetCost(126);
    int cost151 = fallout::skillsGetCost(151);
    int cost176 = fallout::skillsGetCost(176);
    int cost201 = fallout::skillsGetCost(201);
    std::printf("cost=%d,%d,%d,%d,%d,%d,%d\n",
        cost0, cost100, cost101, cost126, cost151, cost176, cost201);
    int defSneak = fallout::skillGetDefaultValue(fallout::SKILL_SNEAK);
    int defBad = fallout::skillGetDefaultValue((fallout::Skill)-1);
    int tagged = fallout::skillIsTagged(fallout::SKILL_SNEAK) ? 1 : 0;
    int diffNorm = fallout::skillGetGameDifficultyModifier(fallout::SKILL_SNEAK);
    int diffOther = fallout::skillGetGameDifficultyModifier(fallout::SKILL_SMALL_GUNS);

    // NOTE: statsInit itself is out of reach headless (it segfaults here;
    // it pulls pc/message stacks that need the full game), so the bounds
    // vectors exercise the setter/getter tables from their zero state.
    int pcMax0 = fallout::statGetConfiguredMaximum(fallout::STAT_STRENGTH, false);
    int npcMax0 = fallout::statGetConfiguredMaximum(fallout::STAT_STRENGTH, true);
    int badStat = fallout::statGetConfiguredMaximum((fallout::Stat)-1, false);
    fallout::statSetPcMaximum(fallout::STAT_STRENGTH, 12);
    fallout::statSetPcMinimum(fallout::STAT_STRENGTH, 1);
    int pcMaxSet = fallout::statGetConfiguredMaximum(fallout::STAT_STRENGTH, false);
    int pcMinSet = fallout::statGetConfiguredMinimum(fallout::STAT_STRENGTH, false);
    std::printf("pcmax0=%d npcmax0=%d bad=%d setmax=%d setmin=%d\n",
        pcMax0, npcMax0, badStat, pcMaxSet, pcMinSet);

    fallout::GameDifficulty savedDiff = fallout::settings.preferences.game_difficulty;
    fallout::settings.preferences.game_difficulty = fallout::GAME_DIFFICULTY_HARD;
    int diffHard = fallout::skillGetGameDifficultyModifier(fallout::SKILL_SNEAK);
    fallout::settings.preferences.game_difficulty = fallout::GAME_DIFFICULTY_EASY;
    int diffEasy = fallout::skillGetGameDifficultyModifier(fallout::SKILL_SNEAK);
    fallout::settings.preferences.game_difficulty = savedDiff;
    std::printf("defsneak=%d defbad=%d tagged=%d diff=%d,%d hard=%d easy=%d\n",
        defSneak, defBad, tagged, diffNorm, diffOther, diffHard, diffEasy);
}

void sectionCombatSkill()
{
    std::puts("[combatskill]");

    // No window system needed. Penalty tables start zeroed (no combatInit:
    // it touches gDude), reset restores the compiled defaults.
    int pen0 = fallout::combat_get_hit_location_penalty(fallout::HIT_LOCATION_HEAD);
    fallout::combat_set_hit_location_penalty(fallout::HIT_LOCATION_HEAD, -15);
    int penSet = fallout::combat_get_hit_location_penalty(fallout::HIT_LOCATION_HEAD);
    fallout::combat_set_hit_location_penalty((fallout::HitLocation)99, -15);
    int penBad = fallout::combat_get_hit_location_penalty((fallout::HitLocation)99);
    fallout::combat_reset_hit_location_penalty();
    int penHead = fallout::combat_get_hit_location_penalty(fallout::HIT_LOCATION_HEAD);
    int penTorso = fallout::combat_get_hit_location_penalty((fallout::HitLocation)3);
    int highlight = fallout::combatGetTargetHighlight();
    std::printf("pen0=%d penset=%d penbad=%d penhead=%d pentorso=%d highlight=%d\n",
        pen0, penSet, penBad, penHead, penTorso, highlight);

    // Skill-use timestamps (game time is the 302400 static) with save/load.
    int upd1 = fallout::skillUpdateLastUse(fallout::SKILL_SNEAK);
    int upd2 = fallout::skillUpdateLastUse(fallout::SKILL_SNEAK);
    fallout::File* save1 = fallout::fileOpen((char*)"/tmp/sku1.bin", (char*)"wb");
    int saveRc1 = save1 != nullptr ? fallout::skillsUsageSave(save1) : -99;
    if (save1 != nullptr) {
        fallout::fileClose(save1);
    }
    unsigned char buf1[216] = {};
    fallout::File* read1 = fallout::fileOpen((char*)"/tmp/sku1.bin", (char*)"rb");
    size_t got1 = read1 != nullptr ? fallout::fileRead(buf1, 1, sizeof(buf1), read1) : 0;
    if (read1 != nullptr) {
        fallout::fileClose(read1);
    }
    int upd3 = fallout::skillUpdateLastUse(fallout::SKILL_SNEAK);
    fallout::File* save2 = fallout::fileOpen((char*)"/tmp/sku2.bin", (char*)"wb");
    int saveRc2 = save2 != nullptr ? fallout::skillsUsageSave(save2) : -99;
    if (save2 != nullptr) {
        fallout::fileClose(save2);
    }
    unsigned char buf2[216] = {};
    fallout::File* read2 = fallout::fileOpen((char*)"/tmp/sku2.bin", (char*)"rb");
    size_t got2 = read2 != nullptr ? fallout::fileRead(buf2, 1, sizeof(buf2), read2) : 0;
    if (read2 != nullptr) {
        fallout::fileClose(read2);
    }
    fallout::File* loadBack = fallout::fileOpen((char*)"/tmp/sku1.bin", (char*)"rb");
    int loadRc = loadBack != nullptr ? fallout::skillsUsageLoad(loadBack) : -99;
    if (loadBack != nullptr) {
        fallout::fileClose(loadBack);
    }
    unsigned char buf3[216] = {};
    fallout::File* save3 = fallout::fileOpen((char*)"/tmp/sku3.bin", (char*)"wb");
    int saveRc3 = save3 != nullptr ? fallout::skillsUsageSave(save3) : -99;
    if (save3 != nullptr) {
        fallout::fileClose(save3);
    }
    fallout::File* read3 = fallout::fileOpen((char*)"/tmp/sku3.bin", (char*)"rb");
    size_t got3 = read3 != nullptr ? fallout::fileRead(buf3, 1, sizeof(buf3), read3) : 0;
    if (read3 != nullptr) {
        fallout::fileClose(read3);
    }
    std::printf("upd=%d,%d,%d save=%d,%d,%d load=%d got=%d,%d,%d crc=%08lX,%08lX,%08lX\n",
        upd1, upd2, upd3, saveRc1, saveRc2, saveRc3, loadRc, (int)got1, (int)got2, (int)got3,
        rawBufCrc(buf1, sizeof(buf1)), rawBufCrc(buf2, sizeof(buf2)), rawBufCrc(buf3, sizeof(buf3)));
}

void sectionGameTime()
{
    std::puts("[gametime]");

    // No window system needed. Clock starts at the 302400 static init.
    unsigned int t0 = fallout::gameTimeGetTime();
    int month = -1;
    int day = -1;
    int year = -1;
    fallout::gameTimeGetDate(&month, &day, &year);
    int hour = fallout::gameTimeGetHour();
    char timeStr[16] = {};
    snprintf(timeStr, sizeof(timeStr), "%s", fallout::gameTimeGetTimeString());
    std::printf("t0=%u date=%d/%d/%d hour=%d timestr=[%s]\n", t0, month, day, year, hour, timeStr);

    // Set/add arithmetic (0 maps to 1; big jumps would end the game).
    fallout::gameTimeSetTime(1000);
    unsigned int tSet = fallout::gameTimeGetTime();
    fallout::gameTimeAddTicks(600);
    fallout::gameTimeAddSeconds(6);
    unsigned int tAdv = fallout::gameTimeGetTime();
    fallout::gameTimeSetTime(0);
    unsigned int tZero = fallout::gameTimeGetTime();
    fallout::gameTimeSetTime(302400);
    std::printf("set=%u adv=%u zero=%u\n", tSet, tAdv, tZero);

    // Midnight update event lands on the queue (no map header: single add).
    fallout::queueInit();
    int schedRc = fallout::gameTimeScheduleUpdateEvent();
    unsigned int nextEv = fallout::queueGetNextEventTime();
    int hasGameTime = fallout::queueHasEvent(nullptr, fallout::EVENT_TYPE_GAME_TIME) ? 1 : 0;
    fallout::queueExit();
    std::printf("sched=%d next=%u has-gametime=%d\n", schedRc, nextEv, hasGameTime);

    // Unique object id counter: fresh, bump, restore, guard rails.
    int idCounter0 = fallout::scriptsGetUniqueObjectIdCounter();
    int idNew1 = fallout::scriptsNewUniqueObjectId();
    int idNew2 = fallout::scriptsNewUniqueObjectId();
    fallout::scriptsRestoreUniqueObjectIdCounter(idNew1);
    int idRestored = fallout::scriptsGetUniqueObjectIdCounter();
    fallout::scriptsRestoreUniqueObjectIdCounter(-5);
    int idBadRestore = fallout::scriptsGetUniqueObjectIdCounter();
    fallout::scriptsResetUniqueObjectIdCounter();
    int idReset = fallout::scriptsGetUniqueObjectIdCounter();
    std::printf("ids=%d,%d,%d restored=%d badrestore=%d reset=%d\n",
        idCounter0, idNew1, idNew2, idRestored, idBadRestore, idReset);
}

void sectionTraitSelect()
{
    std::puts("[traitselect]");

    // No window system needed, no traitsInit (it segfaults headless like
    // statsInit/perksInit): selection state + static bonus tables work
    // from zero state.
    fallout::traitsReset();
    fallout::Trait sel1 = fallout::TRAIT_INVALID;
    fallout::Trait sel2 = fallout::TRAIT_INVALID;
    fallout::traitsGetSelected(&sel1, &sel2);
    fallout::traitsSetSelected((fallout::Trait)15, (fallout::Trait)7);
    fallout::Trait got1 = fallout::TRAIT_INVALID;
    fallout::Trait got2 = fallout::TRAIT_INVALID;
    fallout::traitsGetSelected(&got1, &got2);
    int isGifted = fallout::traitIsSelected((fallout::Trait)15) ? 1 : 0;
    int isFastMeta = fallout::traitIsSelected((fallout::Trait)0) ? 1 : 0;
    int statMod = fallout::traitGetStatModifier(fallout::STAT_STRENGTH);
    int skillMod = fallout::traitGetSkillModifier(fallout::SKILL_SNEAK);
    std::printf("sel0=%d,%d sel=%d,%d is=%d,%d statmod=%d skillmod=%d\n",
        (int)sel1, (int)sel2, (int)got1, (int)got2, isGifted, isFastMeta, statMod, skillMod);

    // Save/load roundtrip of the two selection slots.
    fallout::File* saveStream = fallout::fileOpen((char*)"/tmp/trt.bin", (char*)"wb");
    int saveRc = saveStream != nullptr ? fallout::traitsSave(saveStream) : -99;
    if (saveStream != nullptr) {
        fallout::fileClose(saveStream);
    }
    fallout::traitsReset();
    fallout::File* loadStream = fallout::fileOpen((char*)"/tmp/trt.bin", (char*)"rb");
    int loadRc = loadStream != nullptr ? fallout::traitsLoad(loadStream) : -99;
    if (loadStream != nullptr) {
        fallout::fileClose(loadStream);
    }
    fallout::Trait back1 = fallout::TRAIT_INVALID;
    fallout::Trait back2 = fallout::TRAIT_INVALID;
    fallout::traitsGetSelected(&back1, &back2);
    std::printf("save=%d load=%d back=%d,%d\n", saveRc, loadRc, (int)back1, (int)back2);
}

void sectionObjCreate()
{
    std::puts("[objcreate]");

    // No window system needed. Objects live unplaced (no map required).
    fallout::Object* stim = nullptr;
    int stimRc = fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    std::printf("stim-rc=%d stim-null=%d pid=%d fid=%d flags=%d sid=%d id=%d\n",
        stimRc, stim == nullptr ? 1 : 0,
        stim != nullptr ? stim->pid : -999,
        stim != nullptr ? stim->fid : -999,
        stim != nullptr ? stim->flags : -999,
        stim != nullptr ? stim->sid : -999,
        stim != nullptr ? stim->id : -999);

    fallout::Object* bad = nullptr;
    int badRc = fallout::objectCreateWithProtoId(&bad, fallout::ProtoId());
    std::printf("bad-rc=%d bad-null=%d\n", badRc, bad == nullptr ? 1 : 0);

    fallout::Object* cash = nullptr;
    int cashRc = fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    fallout::Object* dog = nullptr;
    int dogRc = fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    std::printf("cash-rc=%d cash-id=%d dog-rc=%d dog-null=%d\n",
        cashRc, cash != nullptr ? cash->id : -999,
        dogRc, dog == nullptr ? 1 : 0);
    if (dog != nullptr) {
        std::printf("dog-pid=%d dog-fid=%d dog-flags=%d\n", dog->pid, dog->fid, dog->flags);
    }
}

void sectionCritterStats()
{
    std::puts("[critterstats]");

    // No window system needed. Reads run off the created objects' protos
    // (gDude is null here, so all dude-only branches are skipped).
    fallout::Object* dog = nullptr;
    int dogRc = fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    // NOTE: critterGetStat clamps to the bounds tables, which are zero
    // headless (statsInit is out of reach), so it reads 0 throughout;
    // the unclamped base/bonus getters below carry the real proto data.
    int dogStr = fallout::critterGetBaseStat(dog, fallout::STAT_STRENGTH);
    int dogAgi = fallout::critterGetBaseStat(dog, fallout::STAT_AGILITY);
    int dogLuck = fallout::critterGetBaseStat(dog, fallout::STAT_LUCK);
    int dogBonus = fallout::critterGetBonusStat(dog, fallout::STAT_STRENGTH);
    int dogClamped = fallout::critterGetStat(dog, fallout::STAT_STRENGTH);
    std::printf("dog-rc=%d str=%d agi=%d luck=%d bonus=%d clamped=%d\n",
        dogRc, dogStr, dogAgi, dogLuck, dogBonus, dogClamped);

    // Base-stat writes clamp against the (zero) bounds tables here.
    int setRc = fallout::critterSetBaseStat(dog, fallout::STAT_STRENGTH, 6);
    int dogStrAfter = fallout::critterGetStat(dog, fallout::STAT_STRENGTH);
    std::printf("set-rc=%d str-after=%d\n", setRc, dogStrAfter);

    // Skill values off proto skills + primary stats (no dude tags here).
    int sneakVal = fallout::skillGetValue(dog, fallout::SKILL_SNEAK);
    int gunsVal = fallout::skillGetValue(dog, fallout::SKILL_SMALL_GUNS);
    int stimSkill = fallout::skillGetValue(stim, fallout::SKILL_SNEAK);
    int badSkill = fallout::skillGetValue(dog, (fallout::Skill)-1);
    std::printf("sneak=%d guns=%d stim-skill=%d bad-skill=%d\n",
        sneakVal, gunsVal, stimSkill, badSkill);
}

void sectionInventory()
{
    std::puts("[inventory]");

    // No window system needed. Items live in the critter's inventory list
    // (names need message lists, so only numeric accessors are probed).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    int stimW = fallout::itemGetWeight(stim);
    int stimCost = fallout::itemGetCost(stim);
    int stimType = (int)fallout::itemGetType(stim);
    int cashType = (int)fallout::itemGetType(cash);
    int invW0 = fallout::objectGetInventoryWeight(dog);
    int addStim = fallout::itemAdd(dog, stim, 1);
    int addCash = fallout::itemAdd(dog, cash, 3);
    int invW1 = fallout::objectGetInventoryWeight(dog);
    int qtyCash = fallout::itemGetQuantity(dog, cash);
    fallout::Object* slot0 = fallout::inventoryItemByIndex(dog, 0);
    fallout::Object* slot9 = fallout::inventoryItemByIndex(dog, 9);
    int findIdx = -1;
    fallout::Object* found = fallout::inventoryFindByType(dog, (fallout::ItemType)cashType, &findIdx);
    std::printf("w=%d cost=%d types=%d,%d invw0=%d add=%d,%d invw1=%d qty=%d slot0=%d slot9null=%d find=%d@%d\n",
        stimW, stimCost, stimType, cashType, invW0, addStim, addCash, invW1, qtyCash,
        slot0 != nullptr ? slot0->pid : -999, slot9 == nullptr ? 1 : 0,
        found != nullptr ? found->pid : -999, findIdx);

    int remStim = fallout::itemRemove(dog, stim, 1);
    fallout::Object* slot0After = fallout::inventoryItemByIndex(dog, 0);
    int qtyAfter = fallout::itemGetQuantity(dog, stim);
    std::printf("rem=%d slot0after=%d qtyafter=%d\n",
        remStim, slot0After != nullptr ? slot0After->pid : -999, qtyAfter);
}

void sectionEquip()
{
    std::puts("[equip]");

    // No window system needed. Dog wields with animate=false (no iso anim).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* jacket = nullptr;
    int jacketRc = fallout::objectCreateWithProtoId(&jacket, fallout::ProtoId(3));
    int jacketType = jacket != nullptr ? (int)fallout::itemGetType(jacket) : -999;
    int equipArmor = -99;
    int wornAfter = -99;
    if (jacketRc == 0 && jacketType == (int)fallout::ITEM_TYPE_ARMOR) {
        equipArmor = fallout::inventoryEquipFunc(dog, jacket, fallout::HAND_RIGHT, false);
        wornAfter = (jacket->flags & fallout::OBJECT_WORN) != 0 ? 1 : 0;
    }
    std::printf("jacket-rc=%d jacket-type=%d equip-armor=%d worn=%d\n",
        jacketRc, jacketType, equipArmor, wornAfter);

    // Weapon skill/attack mapping is pure proto reads (scorcher=390).
    fallout::Object* gun = nullptr;
    int gunRc = fallout::objectCreateWithProtoId(&gun, fallout::ProtoId(390));
    int gunType = gun != nullptr ? (int)fallout::itemGetType(gun) : -999;
    int gunSkill = gun != nullptr
        ? (int)fallout::weaponGetSkillForHitMode(gun, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY)
        : -999;
    int gunAttack = gun != nullptr
        ? (int)fallout::weaponGetAttackTypeForHitMode(gun, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY)
        : -999;
    std::printf("gun-rc=%d gun-type=%d gun-skill=%d gun-attack=%d\n",
        gunRc, gunType, gunSkill, gunAttack);

    // Dog has no gun frames: art gate rejects the wield, no crash.
    int equipGun = -99;
    if (gunRc == 0) {
        equipGun = fallout::inventoryEquipFunc(dog, gun, fallout::HAND_RIGHT, false);
    }
    fallout::Object* handItem = fallout::critterGetItem2(dog);
    std::printf("equip-gun=%d hand2=%d\n",
        equipGun, handItem != nullptr ? handItem->pid : -1);
}

void sectionBallistics()
{
    std::puts("[ballistics]");

    // No window system needed. Scorcher proto reads + dog's worn jacket.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* gun = nullptr;
    fallout::objectCreateWithProtoId(&gun, fallout::ProtoId(390));
    fallout::Object* jacket = nullptr;
    fallout::objectCreateWithProtoId(&jacket, fallout::ProtoId(3));
    fallout::itemAdd(dog, jacket, 1);
    fallout::inventoryEquipFunc(dog, jacket, fallout::HAND_RIGHT, false);

    int minDmg = -999;
    int maxDmg = -999;
    int dmgRc = fallout::weaponGetDamageMinMax(gun, &minDmg, &maxDmg);
    int dmgNull = fallout::weaponGetDamageMinMax(nullptr, nullptr, nullptr);
    int radius = fallout::weaponGetDamageRadius(gun, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    int rangeBare = fallout::weaponGetRange(dog, fallout::HIT_MODE_PUNCH);
    int apBare = fallout::weaponGetActionPointCost(dog, fallout::HIT_MODE_PUNCH, false);
    std::printf("dmg-rc=%d min=%d max=%d dmg-null=%d radius=%d range=%d ap=%d\n",
        dmgRc, minDmg, maxDmg, dmgNull, radius, rangeBare, apBare);

    int armorAc = fallout::armorGetArmorClass(jacket);
    int armorDt = fallout::armorGetDamageThreshold(jacket, (fallout::DamageType)0);
    int armorDr = fallout::armorGetDamageResistance(jacket, (fallout::DamageType)0);
    fallout::Object* worn = fallout::critterGetArmor(dog);
    fallout::Object* bare = fallout::critterGetArmor(gun);
    std::printf("armor-ac=%d armor-dt=%d armor-dr=%d worn=%d bare-null=%d\n",
        armorAc, armorDt, armorDr,
        worn != nullptr ? worn->pid : -999, bare == nullptr ? 1 : 0);
}

void sectionItemMove()
{
    std::puts("[itemmove]");

    // No window system needed. Two critters pass stacks back and forth.
    fallout::Object* dog1 = nullptr;
    fallout::objectCreateWithProtoId(&dog1, fallout::ProtoId(16777558));
    fallout::Object* dog2 = nullptr;
    fallout::objectCreateWithProtoId(&dog2, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    fallout::itemAdd(dog1, stim, 1);
    fallout::itemAdd(dog1, cash, 2);

    int moveStim = fallout::itemMove(dog1, dog2, stim, 1);
    int qBlocked1 = fallout::itemGetQuantity(dog1, stim);
    // Non-force checks biped/barter carrying rules (dogs fail); force skips.
    int moveForce = fallout::itemMoveForce(dog1, dog2, stim, 1);
    int q1 = fallout::itemGetQuantity(dog1, stim);
    int q2 = fallout::itemGetQuantity(dog2, stim);
    int owned2 = stim->owner == dog2 ? 1 : 0;
    fallout::itemMoveAll(dog1, dog2);
    int qCash1 = fallout::itemGetQuantity(dog1, cash);
    int qCash2 = fallout::itemGetQuantity(dog2, cash);
    std::printf("move=%d blocked-q=%d force=%d q=%d,%d owned=%d moveall-cash=%d,%d\n",
        moveStim, qBlocked1, moveForce, q1, q2, owned2, qCash1, qCash2);

    int remCash = fallout::itemRemove(dog2, cash, 2);
    int destroyCash = fallout::objectDestroy(cash, nullptr);
    int destroyNull = fallout::objectDestroy(nullptr, nullptr);
    fallout::Object* slot0 = fallout::inventoryItemByIndex(dog2, 0);
    std::printf("rem=%d destroy=%d destroynull=%d slot0=%d\n",
        remCash, destroyCash, destroyNull, slot0 != nullptr ? slot0->pid : -999);
}

void sectionItemDrop()
{
    std::puts("[itemdrop]");

    // No window system needed. Plain (unequipped) stacks drop to a tile;
    // equipped drops would repaint through the tile refresher (no map).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    fallout::itemAdd(dog, stim, 1);
    fallout::itemAdd(dog, cash, 2);
    int dropRc = fallout::itemDropAll(dog, 20100);
    fallout::Object* slot0 = fallout::inventoryItemByIndex(dog, 0);
    int qtyStim = fallout::itemGetQuantity(dog, stim);
    int qtyCash = fallout::itemGetQuantity(dog, cash);
    std::printf("drop=%d slot0null=%d qty=%d,%d\n",
        dropRc, slot0 == nullptr ? 1 : 0, qtyStim, qtyCash);

    // Empty inventory drops trivially; a bad tile restores the stack.
    fallout::Object* dog2 = nullptr;
    fallout::objectCreateWithProtoId(&dog2, fallout::ProtoId(16777558));
    int dropEmpty = fallout::itemDropAll(dog2, 20100);
    fallout::Object* stim2 = nullptr;
    fallout::objectCreateWithProtoId(&stim2, fallout::ProtoId(40));
    fallout::itemAdd(dog2, stim2, 1);
    int dropBadTile = fallout::itemDropAll(dog2, -1);
    int qtyKept = fallout::itemGetQuantity(dog2, stim2);
    std::printf("dropempty=%d dropbadtile=%d qtykept=%d\n", dropEmpty, dropBadTile, qtyKept);
}

void sectionAmmoCont()
{
    std::puts("[ammocont]");

    // No window system needed. Ammo piles clamp to proto capacity.
    fallout::Object* ammo = nullptr;
    fallout::objectCreateWithProtoId(&ammo, fallout::ProtoId(14));
    int ammoCap = fallout::ammoGetCapacity(ammo);
    int ammoQty0 = fallout::ammoGetQuantity(ammo);
    fallout::ammoSetQuantity(ammo, 7);
    int ammoQty7 = fallout::ammoGetQuantity(ammo);
    fallout::ammoSetQuantity(ammo, 1000000);
    int ammoQtyClamped = fallout::ammoGetQuantity(ammo);
    fallout::ammoSetQuantity(ammo, -5);
    int ammoQtyZero = fallout::ammoGetQuantity(ammo);
    fallout::Object* gun = nullptr;
    fallout::objectCreateWithProtoId(&gun, fallout::ProtoId(390));
    int gunCap = fallout::ammoGetCapacity(gun);
    int gunQty = fallout::ammoGetQuantity(gun);
    int nullCap = fallout::ammoGetCapacity(nullptr);
    std::printf("cap=%d qty=%d,%d,%d,%d guncap=%d gunqty=%d nullcap=%d\n",
        ammoCap, ammoQty0, ammoQty7, ammoQtyClamped, ammoQtyZero, gunCap, gunQty, nullCap);

    // Containers total the sizes of their contents.
    fallout::Object* box = nullptr;
    fallout::objectCreateWithProtoId(&box, fallout::ProtoId(42));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    int maxSize = fallout::containerGetMaxSize(box);
    int total0 = fallout::containerGetTotalSize(box);
    fallout::itemAdd(box, stim, 1);
    int total1 = fallout::containerGetTotalSize(box);
    fallout::itemAdd(box, cash, 2);
    int total2 = fallout::containerGetTotalSize(box);
    fallout::Object* gun2 = nullptr;
    fallout::objectCreateWithProtoId(&gun2, fallout::ProtoId(390));
    int gunSize = fallout::itemGetSize(gun2);
    fallout::itemAdd(box, gun2, 1);
    int total3 = fallout::containerGetTotalSize(box);
    int nullMax = fallout::containerGetMaxSize(nullptr);
    int nullTotal = fallout::containerGetTotalSize(nullptr);
    std::printf("maxsize=%d total=%d,%d,%d gunsize=%d null=%d,%d\n",
        maxSize, total1, total2, total3, gunSize, nullMax, nullTotal);
}

void sectionExplosives()
{
    std::puts("[explosives]");

    // No window system needed. Damage globals start zeroed (explosions
    // init is static); custom entries live in a small registry.
    int dynMin = -999;
    int dynMax = -999;
    bool dynGot = fallout::explosiveGetDamage(fallout::ItemProtoTypeId::Dynamite, &dynMin, &dynMax);
    bool dynIs = fallout::explosiveIsExplosive(fallout::ItemProtoTypeId::Dynamite);
    bool armedIs = fallout::explosiveIsActiveExplosive(fallout::ItemProtoTypeId::ArmedDynamite);
    bool stimIs = fallout::explosiveIsExplosive(fallout::ProtoId(40));
    fallout::explosiveAdd(fallout::ProtoId(40), fallout::ProtoId(41), 5, 9);
    bool customIs = fallout::explosiveIsExplosive(fallout::ProtoId(40));
    int custMin = -999;
    int custMax = -999;
    bool custGot = fallout::explosiveGetDamage(fallout::ProtoId(40), &custMin, &custMax);
    fallout::ProtoId activated(40);
    bool actRc = fallout::explosiveActivate(activated);
    bool setDyn = fallout::explosiveSetDamage(fallout::ItemProtoTypeId::Dynamite, 10, 20);
    int dynMin2 = -999;
    int dynMax2 = -999;
    fallout::explosiveGetDamage(fallout::ItemProtoTypeId::Dynamite, &dynMin2, &dynMax2);
    std::printf("dyn=%d:%d,%d dynis=%d armedis=%d stimis=%d customis=%d custom=%d:%d,%d activated=%d:%d setdyn=%d dyn2=%d,%d\n",
        dynGot ? 1 : 0, dynMin, dynMax, dynIs ? 1 : 0, armedIs ? 1 : 0, stimIs ? 1 : 0,
        customIs ? 1 : 0, custGot ? 1 : 0, custMin, custMax,
        actRc ? 1 : 0, activated.pid(), setDyn ? 1 : 0, dynMin2, dynMax2);

    // Book lookup on the empty registry + critter body/kill reads.
    int bookMsg = -999;
    fallout::Skill bookSkill = (fallout::Skill)-99;
    bool bookFound = fallout::booksGetInfo(fallout::ProtoId(40), &bookMsg, &bookSkill);
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    int dogBody = (int)fallout::critterGetBodyType(dog);
    int stimBody = (int)fallout::critterGetBodyType(stim);
    int dogKill = (int)fallout::critterGetKillType(dog);
    int stimKill = (int)fallout::critterGetKillType(stim);
    std::printf("book=%d body=%d,%d kill=%d,%d\n",
        bookFound ? 1 : 0, dogBody, stimBody, dogKill, stimKill);
}

void sectionVitals()
{
    std::puts("[vitals]");

    // No window system needed. Fresh objects read zero vitals; HP adjusts
    // clamp against max HP, which is zero headless (see [critterstats]).
    // Poison/radiation adjusts are dude-only.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    int hp0 = fallout::critterGetHitPoints(dog);
    int poison0 = fallout::critterGetPoison(dog);
    int rad0 = fallout::critterGetRadiation(dog);
    int stimHp = fallout::critterGetHitPoints(stim);
    int adjRc = fallout::critterAdjustHitPoints(dog, 5);
    int hp1 = fallout::critterGetHitPoints(dog);
    int poisonRc = fallout::critterAdjustPoison(dog, 5);
    int radRc = fallout::critterAdjustRadiation(dog, 5);
    std::printf("hp=%d poison=%d rad=%d stimhp=%d adj=%d hp1=%d poisonrc=%d radrc=%d\n",
        hp0, poison0, rad0, stimHp, adjRc, hp1, poisonRc, radRc);
}

void sectionObjFind()
{
    std::puts("[objfind]");

    // No window system needed. Only tile-placed objects are visible to
    // the find chain; floating objects are skipped.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    fallout::Object* pre = fallout::objectFindFirst();
    fallout::itemAdd(dog, stim, 1);
    fallout::itemAdd(dog, cash, 2);
    fallout::itemDropAll(dog, 20100);

    // Walk the tile chain, collecting pids in order.
    int pids[8] = { -1, -1, -1, -1, -1, -1, -1, -1 };
    int count = 0;
    for (fallout::Object* obj = fallout::objectFindFirst();
            obj != nullptr && count < 8;
            obj = fallout::objectFindNext()) {
        pids[count++] = obj->pid;
    }
    std::printf("pre-null=%d count=%d pids=%d,%d,%d\n",
        pre == nullptr ? 1 : 0, count, pids[0], pids[1], pids[2]);

    // Destroying the (floating, empty) dog changes nothing visible;
    // destroying the tile-bound stim shrinks the chain.
    int destroyDog = fallout::objectDestroy(dog, nullptr);
    int countAfterDog = 0;
    for (fallout::Object* obj = fallout::objectFindFirst();
            obj != nullptr;
            obj = fallout::objectFindNext()) {
        countAfterDog++;
    }
    int destroyStim = fallout::objectDestroy(stim, nullptr);
    int firstAfter = -999;
    fallout::Object* firstObj = fallout::objectFindFirst();
    if (firstObj != nullptr) {
        firstAfter = firstObj->pid;
    }
    std::printf("destroydog=%d count-after-dog=%d destroystim=%d first-after=%d\n",
        destroyDog, countAfterDog, destroyStim, firstAfter);
}

void sectionObjAttrs()
{
    std::puts("[objattrs]");

    // No window system needed. Null rects skip all screen refresh math.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int rot0 = dog->rotation;
    int cwRc1 = fallout::objectRotateClockwise(dog, nullptr);
    int cwRc2 = fallout::objectRotateClockwise(dog, nullptr);
    int rot2 = dog->rotation;
    int ccwRc = fallout::objectRotateCounterClockwise(dog, nullptr);
    int rot1 = dog->rotation;
    int setRc = fallout::objectSetRotation(dog, fallout::ROTATION_NW, nullptr);
    int rotNw = dog->rotation;
    int setBad = fallout::objectSetRotation(dog, (fallout::Rotation)6, nullptr);
    int rotAfterBad = dog->rotation;
    int setNull = fallout::objectSetRotation(nullptr, fallout::ROTATION_NE, nullptr);
    std::printf("rot0=%d cw=%d,%d rot2=%d ccw=%d rot1=%d set=%d nw=%d bad=%d still=%d null=%d\n",
        rot0, cwRc1, cwRc2, rot2, ccwRc, rot1, setRc, rotNw, setBad, rotAfterBad, setNull);

    // Ownership follows inventory moves (add alone does not adopt).
    fallout::Object* dog2 = nullptr;
    fallout::objectCreateWithProtoId(&dog2, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* ownerFree = fallout::objectGetOwner(stim);
    fallout::itemAdd(dog, stim, 1);
    fallout::Object* ownerAfterAdd = fallout::objectGetOwner(stim);
    fallout::itemMoveForce(dog, dog2, stim, 1);
    fallout::Object* ownerAfterMove = fallout::objectGetOwner(stim);
    std::printf("owner-free=%d owner-add=%d owner-move=%d\n",
        ownerFree == nullptr ? 1 : 0,
        ownerAfterAdd == nullptr ? 1 : 0,
        ownerAfterMove == dog2 ? 1 : 0);
}

void sectionObjVis()
{
    std::puts("[objvis]");

    // No window system needed. Null rects skip all screen refresh math.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int hideRc = fallout::objectHide(dog, nullptr);
    int hiddenFlag = (dog->flags & fallout::OBJECT_HIDDEN) != 0 ? 1 : 0;
    int hideAgain = fallout::objectHide(dog, nullptr);
    int showRc = fallout::objectShow(dog, nullptr);
    int showAgain = fallout::objectShow(dog, nullptr);
    int hideNull = fallout::objectHide(nullptr, nullptr);
    int showNull = fallout::objectShow(nullptr, nullptr);
    std::printf("hide=%d flag=%d hideagain=%d show=%d showagain=%d null=%d,%d\n",
        hideRc, hiddenFlag, hideAgain, showRc, showAgain, hideNull, showNull);

    // Disconnect unplaces: tile head loses the node, tile resets to -1.
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::itemAdd(dog, stim, 1);
    fallout::itemDropAll(dog, 20100);
    int chainBefore = 0;
    for (fallout::Object* obj = fallout::objectFindFirst();
            obj != nullptr;
            obj = fallout::objectFindNext()) {
        chainBefore++;
    }
    int discRc = fallout::_obj_disconnect(stim, nullptr);
    int stimTile = stim->tile;
    int chainAfter = 0;
    for (fallout::Object* obj = fallout::objectFindFirst();
            obj != nullptr;
            obj = fallout::objectFindNext()) {
        chainAfter++;
    }
    int discAgain = fallout::_obj_disconnect(stim, nullptr);
    std::printf("chain=%d disc=%d tile=%d chain-after=%d again=%d\n",
        chainBefore, discRc, stimTile, chainAfter, discAgain);
}

void sectionObjOutline()
{
    std::puts("[objoutline]");

    // No window system needed. Null rects skip all screen refresh math.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int setRc = fallout::objectSetOutline(dog, fallout::OUTLINE_TYPE_ITEM, nullptr);
    int outlineAfter = dog->outline;
    int setAgain = fallout::objectSetOutline(dog, fallout::OUTLINE_TYPE_ITEM, nullptr);
    int hasOutline = fallout::objectHasOutline(dog) ? 1 : 0;
    int clearRc = fallout::objectClearOutline(dog, nullptr);
    int outlineCleared = dog->outline;
    int clearNull = fallout::objectClearOutline(nullptr, nullptr);
    std::printf("set=%d outline=%d again=%d has=%d clear=%d cleared=%d clearnull=%d\n",
        setRc, outlineAfter, setAgain, hasOutline, clearRc, outlineCleared, clearNull);

    int x0 = dog->x;
    int y0 = dog->y;
    int offRc = fallout::_obj_offset(dog, 10, -5, nullptr);
    int offNull = fallout::_obj_offset(nullptr, 10, -5, nullptr);
    std::printf("xy0=%d,%d off=%d xy1=%d,%d offnull=%d\n",
        x0, y0, offRc, dog->x, dog->y, offNull);

    // Misc charges ride on the item object (money default zero here).
    fallout::Object* cash = nullptr;
    fallout::objectCreateWithProtoId(&cash, fallout::ProtoId(41));
    int charges0 = fallout::miscItemGetCharges(cash);
    int maxCharges = fallout::miscItemGetMaxCharges(cash);
    int usesCharges = fallout::miscItemUsesCharges(cash) ? 1 : 0;
    fallout::miscItemSetCharges(cash, 7);
    int charges7 = fallout::miscItemGetCharges(cash);
    std::printf("charges=%d max=%d uses=%d set7=%d\n", charges0, maxCharges, usesCharges, charges7);
}

void sectionGameMouse()
{
    std::puts("[gamemouse]");

    int initRc = fallout::gameMouseInit();
    int initAgain = fallout::gameMouseInit();
    int cursorAfterInit = (int)fallout::gameMouseGetCursor();
    int setCross = fallout::gameMouseSetCursor(fallout::MOUSE_CURSOR_CROSSHAIR);
    int cursorCross = (int)fallout::gameMouseGetCursor();
    int setCrossAgain = fallout::gameMouseSetCursor(fallout::MOUSE_CURSOR_CROSSHAIR);
    int setArrow = fallout::gameMouseSetCursor(fallout::MOUSE_CURSOR_ARROW);
    int cursorArrow = (int)fallout::gameMouseGetCursor();
    std::printf("init=%d again=%d cursor=%d setcross=%d cross=%d crossagain=%d setarrow=%d arrow=%d\n",
        initRc, initAgain, cursorAfterInit, setCross, cursorCross,
        setCrossAgain, setArrow, cursorArrow);

    // NOTE: gameMouseSetMode/CycleMode need the 3d/map machinery
    // (_gmouse_3d_move_to over live objects) and segfault headless,
    // so only the (zero) mode state is observed here.
    int modeInit = (int)fallout::gameMouseGetMode();
    std::printf("mode=%d\n", modeInit);

    int scrollInit = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_enable_scrolling();
    int scrollOn = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_disable_scrolling();
    int scrollOff = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    int clickInit = fallout::_gmouse_get_click_to_scroll() ? 1 : 0;
    fallout::_gmouse_set_click_to_scroll(true);
    int clickOn = fallout::_gmouse_get_click_to_scroll() ? 1 : 0;
    std::printf("scroll=%d,%d,%d click=%d,%d\n", scrollInit, scrollOn, scrollOff, clickInit, clickOn);

    // NOTE: ObjectsHide/Show repaint through the tile refresher, which
    // is null headless (see [itemdrop] note) — only visibility is read.
    int visInit = fallout::gameMouseObjectsIsVisible() ? 1 : 0;
    std::printf("vis=%d\n", visInit);

    fallout::gameMouseExit();
}

void sectionGmouseEn()
{
    std::puts("[gmouseen]");

    // Fresh init (v71 exited). Enable/disable flips cursor + scrolling;
    // second calls are guarded no-ops.
    int initRc = fallout::gameMouseInit();
    fallout::_gmouse_disable_scrolling();
    int scrollOff = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_disable(0);
    int disCursor = (int)fallout::gameMouseGetCursor();
    int disScroll = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_disable(0);
    int disAgainCursor = (int)fallout::gameMouseGetCursor();
    int disAgainScroll = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_enable();
    int enCursor = (int)fallout::gameMouseGetCursor();
    int enScroll = fallout::gmouse_scrolling_is_enabled() ? 1 : 0;
    fallout::_gmouse_enable();
    int enAgainCursor = (int)fallout::gameMouseGetCursor();
    std::printf("init=%d scrolloff=%d dis=%d,%d disagain=%d,%d en=%d,%d enagain=%d\n",
        initRc, scrollOff, disCursor, disScroll, disAgainCursor, disAgainScroll,
        enCursor, enScroll, enAgainCursor);

    fallout::gameMouseExit();
}

void sectionArtHide()
{
    std::puts("[arthide]");

    // No window system needed. Hiding the item art type blinds the tile
    // find chain (all tile-placed leftovers are items); toggle back after.
    int hide0 = fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_ITEM);
    int hideBad = fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_INVALID);
    fallout::artToggleObjectTypeHidden(fallout::OBJ_TYPE_ITEM);
    int hide1 = fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_ITEM);
    fallout::Object* blindFind = fallout::objectFindFirst();
    int blindPid = blindFind != nullptr ? blindFind->pid : -999;
    fallout::artToggleObjectTypeHidden(fallout::OBJ_TYPE_ITEM);
    int hideRestored = fallout::artIsObjectTypeHidden(fallout::OBJ_TYPE_ITEM);
    fallout::Object* sightedFind = fallout::objectFindFirst();
    std::printf("hide=%d bad=%d on=%d blindnull=%d blindpid=%d restored=%d sighted=%d\n",
        hide0, hideBad, hide1,
        blindFind == nullptr ? 1 : 0, blindPid, hideRestored,
        sightedFind != nullptr ? 1 : 0);
}

void sectionProtoNew()
{
    std::puts("[protonew]");

    // No window system needed. Fresh ids come off the list counters;
    // defaults are compiled in (no protoInit here).
    fallout::ProtoId newId;
    int newRc = fallout::proto_new(newId, fallout::OBJ_TYPE_ITEM);
    fallout::Proto* newProto = nullptr;
    int getRc = newRc == 0 ? fallout::protoGetProto(newId, &newProto) : -99;
    std::printf("new-rc=%d newpid=%d get=%d weight=%d cost=%d type=%d material=%d\n",
        newRc, newId.pid(), getRc,
        newProto != nullptr ? newProto->item.weight : -999,
        newProto != nullptr ? newProto->item.cost : -999,
        newProto != nullptr ? (int)newProto->item.type : -999,
        newProto != nullptr ? (int)newProto->item.material : -999);

    fallout::ProtoId badId;
    int badRc = fallout::proto_new(badId, fallout::OBJ_TYPE_INVALID);
    std::printf("bad-rc=%d\n", badRc);
}

void sectionProtoCopy()
{
    std::puts("[protocopy]");

    // No window system needed. The v74 leftover bumped the item counter,
    // so the fresh pid here follows it deterministically.
    fallout::ProtoId freshId;
    int freshRc = fallout::proto_new(freshId, fallout::OBJ_TYPE_ITEM);
    int copyRc = -99;
    int copyWeight = -999;
    int copyPid = -999;
    if (freshRc == 0) {
        copyRc = fallout::proto_copy_proto(fallout::ProtoId(40), freshId);
        fallout::Proto* copied = nullptr;
        if (fallout::protoGetProto(freshId, &copied) == 0 && copied != nullptr) {
            copyWeight = copied->item.weight;
            copyPid = copied->pid;
        }
    }
    int crossRc = fallout::proto_copy_proto(fallout::ProtoId(40), fallout::ProtoId(16777558));
    int missRc = fallout::proto_copy_proto(fallout::ProtoId(999), freshId);
    std::printf("fresh=%d:%d copy=%d weight=%d pid=%d cross=%d miss=%d\n",
        freshRc, freshId.pid(), copyRc, copyWeight, copyPid, crossRc, missRc);

    // Subtype is a direct type-tag comparison (-1 matches everything).
    fallout::Proto* stimProto = nullptr;
    fallout::protoGetProto(fallout::ProtoId(40), &stimProto);
    int sub2 = stimProto != nullptr && fallout::proto_is_subtype(stimProto, 2) ? 1 : 0;
    int sub3 = stimProto != nullptr && fallout::proto_is_subtype(stimProto, 3) ? 1 : 0;
    int subAny = stimProto != nullptr && fallout::proto_is_subtype(stimProto, -1) ? 1 : 0;
    std::printf("sub=%d,%d,%d\n", sub2, sub3, subAny);

    // Object duplication keeps the data, mints a fresh id.
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* twin = nullptr;
    int copyObjRc = fallout::_obj_copy(&twin, stim);
    fallout::Object* carrier = nullptr;
    fallout::objectCreateWithProtoId(&carrier, fallout::ProtoId(16777558));
    fallout::itemAdd(carrier, stim, 1);
    int objCost = fallout::objectGetCost(carrier);
    int objCostBare = fallout::objectGetCost(stim);
    std::printf("objcopy=%d twinpid=%d twinid=%d sametwin=%d objcost=%d barecost=%d\n",
        copyObjRc, twin != nullptr ? twin->pid : -999, twin != nullptr ? twin->id : -999,
        (twin != nullptr && twin != stim) ? 1 : 0, objCost, objCostBare);
}

void sectionMapVars()
{
    std::puts("[mapvars]");

    // No window system needed. Global length stays zero (map-owned);
    // locals grow on demand. Rebuild resets tile light then re-applies.
    fallout::ProgramValue out;
    out.opcode = VALUE_TYPE_INT;
    out.integerValue = -999;
    int globRc = fallout::mapGetGlobalVar(0, out);
    int allocRc = fallout::mapAllocLocalVars(3);
    fallout::ProgramValue setVal(42);
    int setRc = fallout::mapSetLocalVar(1, setVal);
    fallout::ProgramValue gotVal;
    gotVal.opcode = VALUE_TYPE_INT;
    gotVal.integerValue = -999;
    int getRc = fallout::mapGetLocalVar(1, gotVal);
    int badRc = fallout::mapGetLocalVar(99, gotVal);
    std::printf("glob=%d alloc=%d set=%d get=%d:%d bad=%d\n",
        globRc, allocRc, setRc, getRc, gotVal.integerValue, badRc);

    fallout::lightSetTileIntensity(0, 20100, 70000);
    int preRebuild = fallout::lightGetTileIntensity(0, 20100);
    fallout::_obj_rebuild_all_light();
    int postRebuild = fallout::lightGetTileIntensity(0, 20100);
    std::printf("pre=%d post=%d\n", preRebuild, postRebuild);
}

void sectionSkillTag()
{
    std::puts("[skilltag]");

    // No window system needed. Tag slots start zeroed (which already
    // matches SMALL_GUNS id 0); dude-only bonuses stay dormant (no dude).
    int taggedSneak0 = fallout::skillIsTagged(fallout::SKILL_SNEAK) ? 1 : 0;
    int taggedGuns0 = fallout::skillIsTagged(fallout::SKILL_SMALL_GUNS) ? 1 : 0;
    fallout::Skill tags[2] = { fallout::SKILL_SNEAK, fallout::SKILL_SMALL_GUNS };
    fallout::skillsSetTagged(tags, 2);
    fallout::Skill back[4] = { (fallout::Skill)-99, (fallout::Skill)-99, (fallout::Skill)-99, (fallout::Skill)-99 };
    fallout::skillsGetTagged(back, 4);
    int taggedSneak1 = fallout::skillIsTagged(fallout::SKILL_SNEAK) ? 1 : 0;
    std::printf("tagged0=%d,%d back=%d,%d,%d,%d tagged1=%d\n",
        taggedSneak0, taggedGuns0,
        (int)back[0], (int)back[1], (int)back[2], (int)back[3], taggedSneak1);

    // Tagging changes nothing for non-dude critters (dude branch gated).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int sneakVal = fallout::skillGetValue(dog, fallout::SKILL_SNEAK);
    std::printf("sneak=%d\n", sneakVal);
}

void sectionPcStat()
{
    std::puts("[pcstat]");

    // No window system needed. Tables are compile-time; values start zero
    // (pcStatsReset never ran headless). Experience paths are skipped:
    // they pull the level/xp machinery.
    int sp0 = fallout::pcGetStat(fallout::PC_STAT_UNSPENT_SKILL_POINTS);
    int badGet = fallout::pcGetStat((fallout::PcStat)99);
    int setLevel = fallout::pcSetStat(fallout::PC_STAT_LEVEL, 5);
    int levelVal = fallout::pcGetStat(fallout::PC_STAT_LEVEL);
    int setLevelHigh = fallout::pcSetStat(fallout::PC_STAT_LEVEL, 1000);
    int setRepLow = fallout::pcSetStat(fallout::PC_STAT_REPUTATION, -21);
    int setRep = fallout::pcSetStat(fallout::PC_STAT_REPUTATION, 7);
    int repVal = fallout::pcGetStat(fallout::PC_STAT_REPUTATION);
    int setBad = fallout::pcSetStat((fallout::PcStat)99, 1);
    std::printf("sp0=%d badget=%d setlevel=%d level=%d toohigh=%d toolow=%d setrep=%d rep=%d badset=%d\n",
        sp0, badGet, setLevel, levelVal, setLevelHigh, setRepLow, setRep, repVal, setBad);
}

void sectionXpRoll()
{
    std::puts("[xproll]");

    // No window system needed. Fallback xp formula (no xp table headless).
    int xp1 = fallout::pcGetExperienceForLevel(1);
    int xp2 = fallout::pcGetExperienceForLevel(2);
    int xp5 = fallout::pcGetExperienceForLevel(5);
    int xp21 = fallout::pcGetExperienceForLevel(21);
    int xp99 = fallout::pcGetExperienceForLevel(99);
    int xp0 = fallout::pcGetExperienceForLevel(0);
    int xp100 = fallout::pcGetExperienceForLevel(100);
    std::printf("xp=%d,%d,%d,%d,%d,%d,%d\n", xp1, xp2, xp5, xp21, xp99, xp0, xp100);

    // Stat rolls off clamped stats + game random (seeded in [random]).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int howMuch1 = -999;
    int roll1 = fallout::statRoll(dog, fallout::STAT_STRENGTH, 0, &howMuch1);
    int howMuch2 = -999;
    int roll2 = fallout::statRoll(dog, fallout::STAT_STRENGTH, 10, &howMuch2);
    int rollNull = fallout::statRoll(dog, fallout::STAT_STRENGTH, 0, nullptr);
    std::printf("roll=%d:%d rollmod=%d:%d rollnull=%d\n", roll1, howMuch1, roll2, howMuch2, rollNull);
}

void sectionSkillRoll()
{
    std::puts("[skillroll]");

    // No window system needed. Dog is not the dude, game day is 0, so no
    // party/crit paths fire; chances come off the seeded stream ([random]).
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    int how1 = -999;
    int roll1 = fallout::skillRoll(dog, fallout::SKILL_SNEAK, 0, &how1);
    int how2 = -999;
    int roll2 = fallout::skillRoll(dog, fallout::SKILL_SNEAK, 95, &how2);
    int rollBad = fallout::skillRoll(dog, (fallout::Skill)-1, 0, nullptr);
    int howStim = -999;
    int rollStim = fallout::skillRoll(stim, fallout::SKILL_SNEAK, 0, &howStim);
    std::printf("roll=%d:%d rollmod=%d:%d rollbad=%d rollstim=%d:%d\n",
        roll1, how1, roll2, how2, rollBad, rollStim, howStim);
}

void sectionMiscGetters()
{
    std::puts("[miscgetters]");

    // No window system needed. Edge/roof subsystems are unloaded (zeros);
    // movement math is pure flag reads; the dog carries nothing.
    int edgeOn = fallout::mapEdgeIsEnabled() ? 1 : 0;
    int zoneSel = fallout::mapEdgeZoneIsSelected() ? 1 : 0;
    int alignX = fallout::mapEdgeGetTileXAlignment();
    int alignY = fallout::mapEdgeGetTileYAlignment();
    // NOTE: _square_roof_intersect needs loaded roof grids (segfaults
    // headless), so only the unloaded edge state is pinned here.
    std::printf("edge=%d zone=%d align=%d,%d\n", edgeOn, zoneSel, alignX, alignY);

    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    int moveCost = fallout::critterGetMovementPointCostAdjustedForCrippledLegs(dog, 5);
    int moveCostBad = fallout::critterGetMovementPointCostAdjustedForCrippledLegs(stim, 5);
    int encumbered = fallout::critterIsEncumbered(dog) ? 1 : 0;
    std::printf("movecost=%d bad=%d encumbered=%d\n", moveCost, moveCostBad, encumbered);
}

void sectionFindCid()
{
    std::puts("[findcid]");

    // No window system needed. Fresh objects carry cid 0 until combat assigns.
    fallout::Object* dog1 = nullptr;
    fallout::objectCreateWithProtoId(&dog1, fallout::ProtoId(16777558));
    fallout::Object* dog2 = nullptr;
    fallout::objectCreateWithProtoId(&dog2, fallout::ProtoId(16777558));
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    fallout::Object* list[3] = { dog2, dog1, stim };
    int foundDog = fallout::_find_cid(0, dog1->cid, list, 3);
    int foundFrom1 = fallout::_find_cid(1, dog1->cid, list, 3);
    int notFound = fallout::_find_cid(0, 9999, list, 3);
    std::printf("cid=%d found=%d from1=%d missing=%d\n",
        dog1->cid, foundDog, foundFrom1, notFound);
}

void sectionNevsEvents()
{
    std::puts("[nevsevents]");

    // No window system needed. Null programs only exercise the registry
    // bookkeeping (firing needs the interpreter).
    fallout::_nevs_initonce();
    int add1 = fallout::_nevs_addevent((char*)"ev1", nullptr, 3, fallout::NEVS_TYPE_EVENT);
    int addAgain = fallout::_nevs_addevent((char*)"ev1", nullptr, 5, fallout::NEVS_TYPE_HANDLER);
    // NOTE: signalling a FOUND event logs its raw pointer (ASLR noise),
    // so only missing names are signalled here (same rc either way).
    int sigMiss = fallout::_nevs_signal((char*)"nope");
    int clearMiss = fallout::_nevs_clearevent((char*)"nope");
    int clear1 = fallout::_nevs_clearevent((char*)"ev1");
    std::printf("add=%d,%d signal=%d clearmiss=%d clear=%d\n",
        add1, addAgain, sigMiss, clearMiss, clear1);

    // Fill the 40-slot table, overflow, free one, re-add.
    char name[8];
    int fillFail = 0;
    for (int i = 0; i < 40; i++) {
        snprintf(name, sizeof(name), "ev%02d", i);
        if (fallout::_nevs_addevent(name, nullptr, 1, fallout::NEVS_TYPE_EVENT) != 0) {
            fillFail++;
        }
    }
    int overAdd = fallout::_nevs_addevent((char*)"over", nullptr, 1, fallout::NEVS_TYPE_EVENT);
    int clearSlot = fallout::_nevs_clearevent((char*)"ev07");
    int reAdd = fallout::_nevs_addevent((char*)"over", nullptr, 1, fallout::NEVS_TYPE_EVENT);
    std::printf("fillfail=%d over=%d clearslot=%d readd=%d\n",
        fillFail, overAdd, clearSlot, reAdd);
    fallout::_nevs_close();
}

void sectionSfallVars()
{
    std::puts("[sfallvars]");

    // No window system needed. Pure key/value store + file serde.
    bool initOk = fallout::sfall_gl_vars_init();
    bool storeInt = fallout::sfall_gl_vars_store(7, 42);
    int intVal = -999;
    bool fetchInt = fallout::sfall_gl_vars_fetch(7, intVal);
    int missVal = -999;
    bool fetchMiss = fallout::sfall_gl_vars_fetch(8, missVal);
    bool storeStr = fallout::sfall_gl_vars_store("mykey123", 99);
    int strVal = -999;
    bool fetchStr = fallout::sfall_gl_vars_fetch("mykey123", strVal);
    std::printf("init=%d store=%d fetch=%d:%d miss=%d str=%d:%d\n",
        initOk ? 1 : 0, storeInt ? 1 : 0, fetchInt ? 1 : 0, intVal,
        fetchMiss ? 1 : 0, storeStr ? 1 : 0, strVal);

    fallout::File* saveStream = fallout::fileOpen((char*)"/tmp/sgv.bin", (char*)"wb");
    bool saveOk = saveStream != nullptr ? fallout::sfall_gl_vars_save(saveStream) : false;
    if (saveStream != nullptr) {
        fallout::fileClose(saveStream);
    }
    fallout::sfall_gl_vars_reset();
    int clearedVal = -999;
    bool fetchCleared = fallout::sfall_gl_vars_fetch(7, clearedVal);
    fallout::File* loadStream = fallout::fileOpen((char*)"/tmp/sgv.bin", (char*)"rb");
    bool loadOk = loadStream != nullptr ? fallout::sfall_gl_vars_load(loadStream) : false;
    if (loadStream != nullptr) {
        fallout::fileClose(loadStream);
    }
    int backVal = -999;
    bool fetchBack = fallout::sfall_gl_vars_fetch(7, backVal);
    int backStr = -999;
    fallout::sfall_gl_vars_fetch("mykey123", backStr);
    std::printf("save=%d cleared=%d load=%d back=%d:%d backstr=%d\n",
        saveOk ? 1 : 0, fetchCleared ? 1 : 0, loadOk ? 1 : 0,
        fetchBack ? 1 : 0, backVal, backStr);
    fallout::sfall_gl_vars_exit();
}

void sectionSfallIni()
{
    std::puts("[sfallini]");

    // No window system needed. ddraw.ini comes from the [cfgmig] fixture.
    int critTable = -999;
    bool gotCrit = fallout::sfall_ini_get_int("ddraw.ini|Misc|OverrideCriticalTable", &critTable);
    char books[64] = {};
    bool gotBooks = fallout::sfall_ini_get_string("ddraw.ini|Misc|BooksFile", books, sizeof(books));
    int missVal = -999;
    bool gotMiss = fallout::sfall_ini_get_int("ddraw.ini|Misc|Nope", &missVal);
    int badVal = -999;
    bool gotBad = fallout::sfall_ini_get_int("nope", &badVal);
    std::printf("crit=%d:%d books=%d:[%s] miss=%d:%d bad=%d\n",
        gotCrit ? 1 : 0, critTable, gotBooks ? 1 : 0, books,
        gotMiss ? 1 : 0, missVal, gotBad ? 1 : 0);

    bool setOk = fallout::sfall_ini_set_int("ddraw.ini|Misc|OverrideCriticalTable", 7);
    int critAfter = -999;
    fallout::sfall_ini_get_int("ddraw.ini|Misc|OverrideCriticalTable", &critAfter);
    fallout::sfall_ini_cache_clear();
    int critCleared = -999;
    fallout::sfall_ini_get_int("ddraw.ini|Misc|OverrideCriticalTable", &critCleared);
    std::printf("set=%d after=%d cleared=%d\n",
        setOk ? 1 : 0, critAfter, critCleared);
}

void sectionObjPlace()
{
    std::puts("[objplace]");

    // No window system needed. Null rects skip all screen refresh math.
    fallout::Object* dog = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    int locBadTile = fallout::objectSetLocation(dog, -1, 0, nullptr);
    int locBadElev = fallout::objectSetLocation(dog, 20100, 9, nullptr);
    int locNull = fallout::objectSetLocation(nullptr, 20100, 0, nullptr);
    int locRc = fallout::objectSetLocation(dog, 20100, 0, nullptr);
    int findPlaced = -999;
    fallout::Object* placedObj = fallout::objectFindById(dog->id);
    if (placedObj != nullptr) {
        findPlaced = placedObj->pid;
    }
    fallout::Object* findMiss = fallout::objectFindById(999999);
    std::printf("locbad=%d,%d null=%d loc=%d tile=%d elev=%d find=%d missnull=%d\n",
        locBadTile, locBadElev, locNull, locRc, dog->tile, dog->elevation,
        findPlaced, findMiss == nullptr ? 1 : 0);

    fallout::lightSetTileIntensity(0, 20100, 70000);
    int brightLight = fallout::objectGetLightIntensity(dog);
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    int floatLight = fallout::objectGetLightIntensity(stim);
    std::printf("bright=%d float=%d\n", brightLight, floatLight);

    int frameRc = fallout::objectSetFrame(dog, 0, nullptr);
    int frameBad = fallout::objectSetFrame(dog, 999, nullptr);
    int frameNull = fallout::objectSetFrame(nullptr, 0, nullptr);
    int enOutline = fallout::objectEnableOutline(dog, nullptr);
    int disOutline = fallout::objectDisableOutline(dog, nullptr);
    std::printf("frame=%d bad=%d null=%d en=%d dis=%d\n",
        frameRc, frameBad, frameNull, enOutline, disOutline);
}

void sectionItemNames()
{
    std::puts("[itemnames]");

    // No window system needed. Proto message file read straight from VFS.
    fallout::MessageList msgList;
    int listInit = fallout::messageListInit(&msgList) ? 1 : 0;
    char path[64] = {};
    snprintf(path, sizeof(path), "%spro_item.msg", fallout::asc_5186C8);
    int listLoad = fallout::messageListLoad(&msgList, path) ? 1 : 0;

    char stimName[64] = {};
    fallout::MessageListItem nameItem;
    nameItem.num = 4000;
    bool gotStim = false;
    if (fallout::messageListGetItem(&msgList, &nameItem)) {
        snprintf(stimName, sizeof(stimName), "%s", nameItem.text);
        gotStim = true;
    }
    fallout::Proto* moneyProto = nullptr;
    fallout::protoGetProto(fallout::ProtoId(41), &moneyProto);
    char cashName[64] = {};
    bool gotCash = false;
    if (moneyProto != nullptr) {
        fallout::MessageListItem cashItem;
        cashItem.num = moneyProto->item.messageId;
        if (fallout::messageListGetItem(&msgList, &cashItem)) {
            snprintf(cashName, sizeof(cashName), "%s", cashItem.text);
            gotCash = true;
        }
    }
    fallout::MessageListItem missItem;
    missItem.num = 999999;
    bool gotMiss = fallout::messageListGetItem(&msgList, &missItem);
    std::printf("init=%d load=%d stim=%d:[%s] cash=%d:[%s] miss=%d\n",
        listInit, listLoad, gotStim ? 1 : 0, stimName, gotCash ? 1 : 0, cashName,
        gotMiss ? 1 : 0);
    fallout::messageListFree(&msgList);

    bool repoInit = fallout::messageListRepositoryInit();
    int extraId = fallout::messageListRepositoryAddExtra(path);
    std::printf("repoinit=%d extra=%d\n", repoInit ? 1 : 0, extraId);
    fallout::messageListRepositoryExit();
}

static bool getMsgText(const char* file, int num, char* dest, size_t destSize)
{
    fallout::MessageList msgList;
    if (!fallout::messageListInit(&msgList)) {
        return false;
    }
    char path[64] = {};
    snprintf(path, sizeof(path), "%s%s", fallout::asc_5186C8, file);
    if (!fallout::messageListLoad(&msgList, path)) {
        return false;
    }
    fallout::MessageListItem item;
    item.num = num;
    bool found = fallout::messageListGetItem(&msgList, &item);
    if (found) {
        snprintf(dest, destSize, "%s", item.text);
    }
    fallout::messageListFree(&msgList);
    return found;
}

void sectionNames()
{
    std::puts("[names]");

    // No window system needed. Message ids mirror the init loops
    // (trait 100+n, perk 101+n, stat 100+n, skill 100+n).
    char traitName[64] = {};
    bool gotTrait = getMsgText("trait.msg", 100 + 15, traitName, sizeof(traitName));
    char perkName[64] = {};
    bool gotPerk = getMsgText("perk.msg", 101 + 51, perkName, sizeof(perkName));
    char statName[64] = {};
    bool gotStat = getMsgText("stat.msg", 100 + 0, statName, sizeof(statName));
    char skillName[64] = {};
    bool gotSkill = getMsgText("skill.msg", 100 + 8, skillName, sizeof(skillName));
    std::printf("trait=%d:[%s] perk=%d:[%s] stat=%d:[%s] skill=%d:[%s]\n",
        gotTrait ? 1 : 0, traitName, gotPerk ? 1 : 0, perkName,
        gotStat ? 1 : 0, statName, gotSkill ? 1 : 0, skillName);
}

void sectionMoreNames()
{
    std::puts("[morenames]");

    // Message ids mirror the init loops; dog id comes from its proto.
    fallout::Proto* dogProto = nullptr;
    fallout::protoGetProto(fallout::ProtoId(16777558), &dogProto);
    int dogMsgId = dogProto != nullptr ? dogProto->messageId : -999;
    char dogName[64] = {};
    bool gotDog = getMsgText("pro_crit.msg", dogMsgId, dogName, sizeof(dogName));
    char killName[64] = {};
    bool gotKill = getMsgText("proto.msg", 1450 + 11, killName, sizeof(killName));
    char stimDesc[128] = {};
    bool gotStimDesc = getMsgText("pro_item.msg", 4001, stimDesc, sizeof(stimDesc));
    char giftedDesc[128] = {};
    bool gotGiftedDesc = getMsgText("trait.msg", 200 + 15, giftedDesc, sizeof(giftedDesc));
    std::printf("dogid=%d dog=%d:[%s] kill=%d:[%s]\n",
        dogMsgId, gotDog ? 1 : 0, dogName, gotKill ? 1 : 0, killName);
    std::printf("stimdesc=%d:[%s] gifteddesc=%d:[%s]\n",
        gotStimDesc ? 1 : 0, stimDesc, gotGiftedDesc ? 1 : 0, giftedDesc);
}

void sectionMoreDesc()
{
    std::puts("[moredesc]");

    // Car pids come from the type tables; description ids mirror inits.
    // (Critter descriptions have no entry for the dog; skipped.)
    char taggedDesc[128] = {};
    bool gotTaggedDesc = getMsgText("perk.msg", 1101 + 51, taggedDesc, sizeof(taggedDesc));
    char sneakDesc[128] = {};
    bool gotSneakDesc = getMsgText("skill.msg", 200 + 8, sneakDesc, sizeof(sneakDesc));
    char strDesc[128] = {};
    bool gotStrDesc = getMsgText("stat.msg", 200 + 0, strDesc, sizeof(strDesc));
    std::printf("taggeddesc=%d:[%s]\nsneakdesc=%d:[%s]\nstrdesc=%d:[%s]\n",
        gotTaggedDesc ? 1 : 0, taggedDesc,
        gotSneakDesc ? 1 : 0, sneakDesc, gotStrDesc ? 1 : 0, strDesc);
}

// Real game scripts, loaded straight out of master.dat through the VFS and
// executed.
//
// The intlib registers the game's own script API as extra opcodes - the
// range 0x804C to 0x8281, about 566 of them, all of which need the running
// game. The core interpreter opcodes are exactly 0x8000 to 0x804B, so the
// harness clears every handler slot above 0x04B to take the game out of the
// picture. Both engines then stop at the same place: the first opcode that
// needs the game.
//
// What that compares is the interpreter itself - the DAT read, the file
// layout, the procedure and identifier tables, and the real opcode mix the
// game's compiler emitted - up to the boundary where the game takes over.
static void scriptRunOne(const char* name, int budget)
{
    fallout::Program* program = fallout::programCreateByPath((char*)name);
    if (program == nullptr) {
        std::printf("%s load-failed\n", name);
        return;
    }

    // programCreateByPath leaves the instruction pointer at 0, which is where
    // a script's top-level code starts.
    program->flags = 0;
    program->instructionPointer = 0;

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X stack=%d", name, program->instructionPointer,
        (unsigned int)program->flags, (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// Opcode census over every .int in the game data.
//
// The walk mirrors src/scan_unimplemented_opcodes.h: a script's top-level code
// is the first 42 bytes, the procedure bodies come after the static string
// table, and the code is walked linearly - the game's compiler emits it
// linearly, so no control flow needs following. Each opcode is two bytes,
// and PUSH carries a four-byte operand.
//
// The census is what sizes the remaining work: the intlib registers 448
// opcodes, but the scripts only use a subset of them, and the subset that
// actually shows up is the list that has to be ported.
//
// The histogram is printed without names so that Java can reproduce it
// exactly - the names live in the Java disassembler, which is a separate
// tool.
static void scriptScanAll()
{
    // Read the archive directly instead of through the VFS. The VFS resolves
    // names case-insensitively across every mounted archive, and master.dat
    // and patch000.dat both carry scripts whose names differ only in case -
    // "scripts\\arbridge.int" and "Scripts\\ARBRIDGE.int". Which archive
    // wins then depends on the mount order, which is a property of the game's
    // startup rather than of the script format. Scanning master.dat's own
    // entries keeps the census well defined.
    int errorFlags = 0;
    fallout::DBase* db = fallout::dbaseOpen("/game-en/master.dat", &errorFlags);
    if (db == nullptr) {
        std::printf("dbaseOpen failed\n");
        return;
    }

    int histogram[768];
    memset(histogram, 0, sizeof(histogram));

    int scriptCount = 0;
    long long instructionCount = 0;
    int unknownCount = 0;

    for (int index = 0; index < db->entriesLength; index++) {
        const char* path = db->entries[index].path;
        const int nameLength = (int)strlen(path);
        if (nameLength < 4 || fallout::compat_stricmp(path + nameLength - 4, ".int") != 0) {
            continue;
        }

        fallout::DFile* dfile = fallout::dfileOpen(db, path, "rb");
        if (dfile == nullptr) {
            continue;
        }

        const int size = (int)fallout::dfileGetSize(dfile);
        unsigned char* data = (unsigned char*)malloc(size > 0 ? size : 1);
        if (data == nullptr) {
            fallout::dfileClose(dfile);
            continue;
        }
        memset(data, 0, size > 0 ? size : 1);
        fallout::dfileRead(data, 1, size, dfile);
        fallout::dfileClose(dfile);

        // Layout: [top-level code][procedure count][table][identifiers][strings][bodies]
        const int procedureCount = fallout::stackReadInt32(data, 42);
        const int identifiersPos = 24 * procedureCount + 42 + 4;
        const int staticStringsPos = identifiersPos + fallout::stackReadInt32(data, identifiersPos) + 4 + 4;
        int staticStringLen = fallout::stackReadInt32(data, staticStringsPos);
        if (staticStringLen == -1) {
            staticStringLen = -4;
        }
        const int codePos = staticStringsPos + 4 + staticStringLen + 4;

        auto walk = [&](int from, int to) {
            int pos = from;
            while (pos + 2 <= to) {
                const int opcode = fallout::stackReadInt16(data, pos);
                if (!((opcode >> 8) & 0x80)) {
                    break;
                }
                const int opcodeIndex = opcode & 0x3FF;
                histogram[opcodeIndex]++;
                instructionCount++;
                if (fallout::gInterpreterOpcodeHandlers[opcodeIndex] == nullptr) {
                    unknownCount++;
                }
                pos += 2;
                if (opcodeIndex == (fallout::OPCODE_PUSH & 0x3FF)) {
                    pos += 4;
                }
            }
        };

        walk(0, 0x2A);
        walk(codePos, size);

        scriptCount++;
        free(data);
    }

    fallout::dbaseClose(db);

    std::printf("scripts=%d instructions=%lld unknown=%d\n", scriptCount, instructionCount, unknownCount);
    for (int index = 0; index < 768; index++) {
        if (histogram[index] != 0) {
            std::printf("op=0x%03X count=%d\n", index, histogram[index]);
        }
    }
}

void sectionScriptScan()
{
    std::puts("[scriptscan]");

    // The opcode handlers have to be registered for the unknown-count to mean
    // "not implemented" rather than "not registered yet".
    fallout::interpreterRegisterOpcodeHandlers();

    scriptScanAll();
}

void sectionScriptRun()
{
    std::puts("[scriptrun]");

    // The opcode handlers are registered by sectionVm, which runs after this
    // section, so register them here too - otherwise the first opcode of a
    // real script is "Undefined opcode 0x8002".
    fallout::interpreterRegisterOpcodeHandlers();

    // Take the game out: the intlib's opcodes need a running game, and the
    // core interpreter opcodes are exactly 0x8000 to 0x804B.
    for (int index = 0x04C; index < 768; index++) {
        fallout::gInterpreterOpcodeHandlers[index] = nullptr;
    }

    // Smallest first: these are the scripts whose top-level code gets
    // furthest before the game is needed.
    scriptRunOne("scripts\\miJerky.int", 200);
    scriptRunOne("scripts\\CATH2MV.int", 200);
    scriptRunOne("scripts\\acdog.int", 200);
    scriptRunOne("scripts\\ABEL.int", 200);
}

void sectionScriptAttach()
{
    std::puts("[scriptattach]");

    // objectCreateWithProtoId runs objectSetScriptFromProto (src/object.cc,
    // the 0x489CCC create path): the object receives the proto's sid and a
    // Script record is added whose owner is the object. The section pins
    // the create-time attachment and the reads the context builtins amount
    // to (scriptGetSid -> scriptGetScript -> the script's fields).
    fallout::Object* dog = nullptr;
    int rc = fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    if (dog == nullptr || rc != 0) {
        std::printf("dog create-failed\n");
        return;
    }
    std::printf("dog sid=%d\n", dog->sid);

    fallout::Script* script = nullptr;
    if (dog->sid != -1 && fallout::scriptGetScript(dog->sid, &script) == 0 && script != nullptr) {
        std::printf("owner-match=%d index=%d critter-idx=%d\n",
            script->owner == dog ? 1 : 0, script->index, dog->scriptIndex);
    } else {
        std::printf("no-script\n");
    }

    // A program bound to a script consults the script's fields the way the
    // context builtins read them: self via scriptGetSelf (owner), target,
    // source, fixedParam and actionBeingUsed straight off the record.
    fallout::Program* prog = fallout::programCreateByPath((char*)"scripts\\acdog.int");
    if (prog == nullptr) {
        std::printf("prog load-failed\n");
        return;
    }

    int sid;
    if (fallout::scriptAdd(&sid, fallout::SCRIPT_TYPE_CRITTER) == -1) {
        std::printf("scriptadd failed\n");
        fallout::programFree(prog);
        return;
    }

    fallout::Script* scr = nullptr;
    if (fallout::scriptGetScript(sid, &scr) == -1) {
        std::printf("scriptget failed\n");
        fallout::programFree(prog);
        return;
    }

    scr->program = prog;
    scr->owner = dog;
    scr->source = nullptr;
    scr->target = dog;
    scr->actionBeingUsed = 7;
    fallout::scriptSetFixedParam(sid, 42);

    int sidFromProg = fallout::scriptGetSid(prog);
    std::printf("sid-match=%d self=%d target=%d source=%d fixed=%d action=%d\n",
        sidFromProg == sid ? 1 : 0,
        fallout::scriptGetSelf(prog) == dog ? 1 : 0,
        scr->target == dog ? 1 : 0,
        scr->source == nullptr ? 1 : 0,
        scr->fixedParam,
        scr->actionBeingUsed);

    fallout::programFree(prog);
}

void sectionScriptRunGame()
{
    std::puts("[scriptrungame]");

    // The global variables, parsed the way the game parses them at startup.
    // The game's own tables cannot be filled from here - gGameGlobalPointers
    // is private to game.cc - so the parse goes into a local array, and the
    // count and a checksum of the values pin it. A difference in how the file
    // is read shows up here even when the scripts never look at the values.
    //
    // The harness itself has no global variables, so the game's builtins see
    // an empty table and answer -1, which is the state the Java side has to
    // reproduce.
    int gamLength = 0;
    int* gamVars = nullptr;
    if (fallout::globalVarsRead("data\\vault13.gam", "GAME_GLOBAL_VARS:", &gamLength, &gamVars) == 0) {
        int checksum = 0;
        for (int index = 0; index < gamLength; index++) {
            checksum = checksum * 31 + gamVars[index];
        }
        std::printf("gamvars length=%d checksum=%d\n", gamLength, checksum);
    }
    std::printf("globals length=%d\n", fallout::gGameGlobalVarsLength);

    // A few of the game's message strings, read through the original's own
    // message-file loader, so the Java port's parser can be compared against
    // it. The lookup the scripts use needs the script list, which the
    // harness has not loaded, so the file is read directly instead.
    {
        fallout::MessageList list;
        fallout::messageListInit(&list);
        if (fallout::messageListLoad(&list, "dialog\\ACBrahmn.msg")) {
            for (int num : { 100, 101, 102 }) {
                fallout::MessageListItem item;
                memset(&item, 0, sizeof(item));
                item.num = num;
                if (fallout::messageListGetItem(&list, &item)) {
                    std::printf("msg num=%d [%s]\n", num, item.text != nullptr ? item.text : "(null)");
                } else {
                    std::printf("msg num=%d (missing)\n", num);
                }
            }
        } else {
            std::printf("msg (file not loaded)\n");
        }
        fallout::messageListFree(&list);
    }

    // The same scripts with the game's own builtins available. The intlib
    // reaches into the running game, so this is the state the game's scripts
    // see - and the state the Java side has to reproduce once the game
    // subsystems are ported.
    fallout::interpreterRegisterOpcodeHandlers();

    scriptRunOne("scripts\\miJerky.int", 200);
    scriptRunOne("scripts\\CATH2MV.int", 200);
    scriptRunOne("scripts\\acdog.int", 200);
    scriptRunOne("scripts\\ABEL.int", 200);
    // These use the game's global variables, so they exercise the globals read
    // out of vault13.gam.
    scriptRunOne("scripts\\doorcode.int", 200);
    scriptRunOne("scripts\\CODEDISK.int", 200);
    scriptRunOne("scripts\\ARTIFACT.int", 200);
    scriptRunOne("scripts\\MutFruit.int", 200);
    // These use the inventory: what an object carries, and a critter's
    // equipped items.
    scriptRunOne("scripts\\DOG2.int", 200);
    scriptRunOne("scripts\\VSGTEAM9.int", 200);
    scriptRunOne("scripts\\JONATHAN.int", 200);
    // This one writes to the message monitor and the debug log.
    scriptRunOne("scripts\\FREEZER.int", 200);
    // These read the object's kind, tile and proto id.
    scriptRunOne("scripts\\GRAFFITI.int", 200);
    scriptRunOne("scripts\\BVAIR.int", 200);
    // These ask the game a question through a metarule.
    scriptRunOne("scripts\\ziclosed.int", 200);
    scriptRunOne("scripts\\cowbomb.int", 200);
    scriptRunOne("scripts\\CSAREA2.int", 200);
}

void sectionScriptProg()
{
    std::puts("[scriptprog]");

    // No window system needed. Bytecode loads straight from VFS;
    // execution needs the game and stays out of reach.
    fallout::Program* prog = fallout::programCreateByPath((char*)"scripts\\ABEL.int");
    int startIdx = prog != nullptr ? fallout::programFindProcedure(prog, (char*)"start") : -999;
    int missIdx = prog != nullptr ? fallout::programFindProcedure(prog, (char*)"nope") : -999;
    int pop1 = -999;
    int pop2 = -999;
    if (prog != nullptr) {
        fallout::programStackPushInteger(prog, 42);
        fallout::programStackPushInteger(prog, 7);
        pop1 = fallout::programStackPopInteger(prog);
        pop2 = fallout::programStackPopInteger(prog);
        fallout::programFree(prog);
    }
    // NOTE: missing files are fatal by design (noreturn error with no
    // interpreter context), so only the successful load is pinned here.
    std::printf("loaded=%d start=%d miss=%d pop=%d,%d\n",
        prog != nullptr ? 1 : 0, startIdx, missIdx, pop1, pop2);
}

void sectionProgramStack()
{
    std::puts("[programstack]");

    // No window system needed. LIFO discipline + type-tagged values;
    // underflow/mismatch paths are fatal by design (unpinned here).
    fallout::Program* prog = fallout::programCreateByPath((char*)"scripts\\ABEL.int");
    fallout::programStackPushFloat(prog, 2.5f);
    fallout::ProgramValue floatVal = fallout::programStackPopValue(prog);
    fallout::programStackPushString(prog, (char*)"hi");
    char strBuf[16] = {};
    snprintf(strBuf, sizeof(strBuf), "%s", fallout::programStackPopString(prog));
    fallout::programStackPushPointer(prog, nullptr);
    void* ptrVal = fallout::programStackPopPointer(prog);
    fallout::ProgramValue intVal;
    intVal.opcode = VALUE_TYPE_INT;
    intVal.integerValue = 9;
    fallout::programStackPushValue(prog, intVal);
    int poppedInt = fallout::programStackPopInteger(prog);
    std::printf("float=%d:%f str=[%s] ptrnull=%d int=%d\n",
        (int)floatVal.opcode, (double)floatVal.floatValue, strBuf,
        ptrVal == nullptr ? 1 : 0, poppedInt);
    fallout::programFree(prog);
}

void sectionProgRetStack()
{
    std::puts("[progretstack]");

    // No window system needed. Return stack pops are type-blind (unlike
    // the value stack); the program list itself has no observable state.
    fallout::Program* prog = fallout::programCreateByPath((char*)"scripts\\ABEL.int");
    int procCount = prog != nullptr ? prog->procedureCount() : -999;
    int retPop1 = -999;
    int retPop2 = -999;
    int retPtrNull = -999;
    if (prog != nullptr) {
        fallout::programReturnStackPushInteger(prog, 11);
        fallout::programReturnStackPushPointer(prog, nullptr);
        retPtrNull = fallout::programReturnStackPopPointer(prog) == nullptr ? 1 : 0;
        retPop1 = fallout::programReturnStackPopInteger(prog);
        fallout::programReturnStackPushInteger(prog, 22);
        retPop2 = fallout::programReturnStackPopInteger(prog);
        fallout::programFree(prog);
    }
    std::printf("procs=%d retptrnull=%d retpop=%d,%d\n",
        procCount, retPtrNull, retPop1, retPop2);
}

void sectionStatsSave()
{
    std::puts("[statssave]");

    // No window system needed. PC values roundtrip through a scratch file;
    // AP bonuses start at their compiled defaults.
    int apBonus0 = fallout::statGetUnspentApBonus();
    int apPerk0 = fallout::statGetUnspentApPerkBonus();
    fallout::statSetUnspentApBonus(6);
    fallout::statSetUnspentApPerkBonus(7);
    int apBonus = fallout::statGetUnspentApBonus();
    int apPerk = fallout::statGetUnspentApPerkBonus();
    std::printf("ap=%d,%d set=%d,%d\n", apBonus0, apPerk0, apBonus, apPerk);

    fallout::pcSetStat(fallout::PC_STAT_LEVEL, 5);
    fallout::pcSetStat(fallout::PC_STAT_REPUTATION, 7);
    fallout::File* saveStream = fallout::fileOpen((char*)"/tmp/stt.bin", (char*)"wb");
    int saveRc = saveStream != nullptr ? fallout::statsSave(saveStream) : -99;
    if (saveStream != nullptr) {
        fallout::fileClose(saveStream);
    }
    fallout::pcSetStat(fallout::PC_STAT_LEVEL, 1);
    fallout::pcSetStat(fallout::PC_STAT_REPUTATION, 0);
    fallout::File* loadStream = fallout::fileOpen((char*)"/tmp/stt.bin", (char*)"rb");
    int loadRc = loadStream != nullptr ? fallout::statsLoad(loadStream) : -99;
    if (loadStream != nullptr) {
        fallout::fileClose(loadStream);
    }
    int levelBack = fallout::pcGetStat(fallout::PC_STAT_LEVEL);
    int repBack = fallout::pcGetStat(fallout::PC_STAT_REPUTATION);
    std::printf("save=%d load=%d back=%d,%d\n", saveRc, loadRc, levelBack, repBack);
}

void sectionFrmIds()
{
    std::puts("[frmids]");

    // No window system needed. Skill/stat/trait frame ids are compiled
    // into the description tables (Gifted rides its static initializer).
    int sneakFrm = fallout::skillGetFrmId(fallout::SKILL_SNEAK).frameId();
    int skillBad = fallout::skillGetFrmId((fallout::Skill)-1).frameId();
    int strFrm = fallout::statGetFrmId(fallout::STAT_STRENGTH).frameId();
    int statBad = fallout::statGetFrmId((fallout::Stat)-1).frameId();
    int giftedFrm = fallout::traitGetFrmId((fallout::Trait)15).frameId();
    std::printf("sneak=%d skillbad=%d str=%d statbad=%d gifted=%d\n",
        sneakFrm, skillBad, strFrm, statBad, giftedFrm);
}

void sectionPartyObjects()
{
    std::puts("[partyobjects]");

    // Party descriptions come from data\party.txt; objectsInit then builds
    // the dude/egg on top (isoInit would redo this, so it stays split).
    int partyRc = fallout::partyMembersInit();
    static unsigned char objBuf[640 * 480];
    int objRc = fallout::objectsInit(objBuf, 640, 480, 640);
    fallout::Object* dude = fallout::gDude;
    fallout::Object* egg = fallout::gEgg;
    std::printf("party=%d objects=%d dude=%d:%d egg=%d:%d\n",
        partyRc, objRc,
        dude != nullptr ? dude->pid : -999,
        dude != nullptr ? dude->id : -999,
        egg != nullptr ? egg->pid : -999,
        egg != nullptr ? egg->id : -999);

    // The stats of the player come from the proto of the player.
    std::printf("dude-str=%d dude-agi=%d dude-luck=%d\n",
        dude != nullptr ? fallout::critterGetBaseStat(dude, fallout::STAT_STRENGTH) : -999,
        dude != nullptr ? fallout::critterGetBaseStat(dude, fallout::STAT_AGILITY) : -999,
        dude != nullptr ? fallout::critterGetBaseStat(dude, fallout::STAT_LUCK) : -999);

    // Re-adding the dude is a no-op (pid match), not a duplicate.
    int dupRc = dude != nullptr ? fallout::partyMemberAdd(dude) : -99;
    std::printf("dup-add=%d\n", dupRc);
}

void sectionCombatDamage()
{
    std::puts("[combatdamage]");

    // [partyobjects] ran first, so the dude exists; a fresh dog (no
    // script sid) is the target. The section pins the death flags without
    // applying them, the non-lethal application through _apply_damage,
    // and the lethal arm of actionDamage (whose internal formula is a
    // static helper, so it is observed through the outcome).
    fallout::Object* dog = nullptr;
    if (fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558)) == -1 || dog == nullptr) {
        std::printf("dog create-failed\n");
        return;
    }
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::printf("dude=null\n");
        return;
    }

    int dt = fallout::critterGetStat(dog,
        (fallout::Stat)((int)fallout::STAT_DAMAGE_THRESHOLD + (int)fallout::DAMAGE_TYPE_NORMAL));
    int dr = fallout::critterGetStat(dog,
        (fallout::Stat)((int)fallout::STAT_DAMAGE_RESISTANCE + (int)fallout::DAMAGE_TYPE_NORMAL));
    int exp = fallout::critterGetExp(dog);
    std::printf("dt=%d dr=%d exp=%d\n", dt, dr, exp);

    dog->data.critter.hp = 20;
    dog->data.critter.combat.results = fallout::DAM_NONE;
    fallout::Attack attack;
    fallout::attackInit(&attack, dude, dog,
        (fallout::HitMode)fallout::HIT_MODE_PUNCH,
        (fallout::HitLocation)fallout::HIT_LOCATION_TORSO);
    attack.attackerFlags = fallout::DAM_HIT;
    attack.intendedTarget = dog;
    attack.defenderDamage = 15;
    fallout::attackComputeDeathFlags(&attack);
    std::printf("nonlethal-dead=%d\n",
        (attack.defenderFlags & fallout::DAM_DEAD) != fallout::DAM_NONE ? 1 : 0);

    fallout::_apply_damage(&attack, false);
    std::printf("hp=%d dead=%d last=%d\n",
        dog->data.critter.hp,
        (dog->data.critter.combat.results & fallout::DAM_DEAD) != fallout::DAM_NONE ? 1 : 0,
        dog->data.critter.combat.damageLastTurn);

    dog->data.critter.hp = 5;
    dog->data.critter.combat.results = fallout::DAM_NONE;
    attack.defenderDamage = 10;
    attack.defenderFlags = fallout::DAM_NONE;
    attack.attackerFlags = fallout::DAM_HIT;
    fallout::attackComputeDeathFlags(&attack);
    std::printf("lethal-dead=%d\n",
        (attack.defenderFlags & fallout::DAM_DEAD) != fallout::DAM_NONE ? 1 : 0);
    fallout::_apply_damage(&attack, false);
    std::printf("kill hp=%d dead=%d kills=%d\n",
        dog->data.critter.hp,
        (dog->data.critter.combat.results & fallout::DAM_DEAD) != fallout::DAM_NONE ? 1 : 0,
        fallout::killsGetByType(fallout::KILL_TYPE_DOG));
}

void sectionCombatRequest()
{
    std::puts("[combatrequest]");

    // The script combat request (src/scripts.cc): scriptsRequestCombat
    // raises the flag and copies the CombatStartData; a locked request is
    // refused, and _scripts_clear_combat_requests clears the lock when the
    // requesting script's owner is the attacker. The actual _combat run is
    // game phase, so the section pins the request/refuse/clear cycle only.
    fallout::Object* dog = nullptr;
    if (fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558)) == -1 || dog == nullptr) {
        std::printf("dog create-failed\n");
        return;
    }
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::printf("dude=null\n");
        return;
    }

    int sid;
    if (fallout::scriptAdd(&sid, fallout::SCRIPT_TYPE_CRITTER) == -1) {
        std::printf("scriptadd failed\n");
        return;
    }
    fallout::Script* scr = nullptr;
    if (fallout::scriptGetScript(sid, &scr) == -1) {
        std::printf("scriptget failed\n");
        return;
    }
    scr->owner = dude;

    fallout::CombatStartData csd;
    csd.attacker = dude;
    csd.defender = dog;
    csd.actionPointsBonus = 0;
    csd.accuracyBonus = 15;
    csd.damageBonus = 0;
    csd.minDamage = 3;
    csd.maxDamage = 12;
    csd.overrideAttackResults = 1;
    csd.attackerResults = fallout::DAM_HIT;
    csd.targetResults = fallout::DAM_DEAD;

    int rc = fallout::scriptsRequestCombat(&csd);
    fallout::_scripts_request_combat_locked(&csd);
    int lockedRc = fallout::scriptsRequestCombat(&csd);
    fallout::_scripts_clear_combat_requests(scr);
    int againRc = fallout::scriptsRequestCombat(&csd);

    std::printf("rc=%d locked-rc=%d again-rc=%d\n", rc, lockedRc, againRc);
}

void sectionPerkRank()
{
    std::puts("[perkrank]");

    // [partyobjects] ran first, so the party descriptions exist and the
    // dude is there. perksInit allocates the rank table and zeroes it;
    // perkGetRank reads the critter's slot (dude is index 0) or falls
    // back to it.
    // The message-list repository the perk names register into is owned by
    // the game phase (game.cc messageListRepositoryInit), so bring it up
    // like the game does before the perk engine touches it.
    if (!fallout::messageListRepositoryInit()) {
        std::printf("msglistrepo failed\n");
        return;
    }

    int initRc = fallout::perksInit();
    if (initRc == -1) {
        std::printf("perksinit failed\n");
        return;
    }

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::printf("dude=null\n");
        return;
    }

    std::printf("descs=%d\n", fallout::gPartyMemberDescriptionsLength);
    int r0 = fallout::perkGetRank(dude, (fallout::Perk)0); // awareness
    int r1 = fallout::perkGetRank(dude, (fallout::Perk)1); // bonus hth attacks
    int r3 = fallout::perkGetRank(dude, (fallout::Perk)3); // bonus move
    int rBad = fallout::perkGetRank(dude, (fallout::Perk)-1);
    int h0 = fallout::perkHasRank(dude, (fallout::Perk)0) ? 1 : 0;

    fallout::Object* dog = nullptr;
    if (fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558)) == -1 || dog == nullptr) {
        std::printf("dog create-failed\n");
        fallout::perksExit();
        return;
    }
    int rDog = fallout::perkGetRank(dog, (fallout::Perk)3);

    std::printf("r=%d,%d,%d,%d h=%d dog=%d\n", r0, r1, r3, rBad, h0, rDog);

    fallout::perksExit();
}

void sectionWeaponData()
{
    std::puts("[weapondata]");

    // The weapon-read helpers the attack computation leans on
    // (src/item.cc / src/combat.cc): the animation per hit mode (the
    // extended-flags nibble into _attack_anim, neutral punches/throws for
    // bare hands), the critical-failure table index, the ammo-cost fixup
    // (Super Cattle Prod / Mega Power Fist double their cost) and the
    // unarmed bonus-critical-chance table.
    fallout::Object* gun = nullptr;
    if (fallout::objectCreateWithProtoId(&gun, fallout::ProtoId(390)) == -1 || gun == nullptr) {
        std::printf("gun create-failed\n");
        return;
    }
    fallout::Object* dog = nullptr;
    if (fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558)) == -1 || dog == nullptr) {
        std::printf("dog create-failed\n");
        return;
    }

    int animPrimary = (int)fallout::weaponGetAnimationForHitMode(gun, (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    int animSecondary = (int)fallout::weaponGetAnimationForHitMode(gun, (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY);
    int animPunch = (int)fallout::weaponGetAnimationForHitMode(nullptr, (fallout::HitMode)fallout::HIT_MODE_PUNCH);
    int animKick = (int)fallout::weaponGetAnimationForHitMode(nullptr, (fallout::HitMode)fallout::HIT_MODE_KICK);
    int animDog = (int)fallout::critterGetAnimationForHitMode(dog, (fallout::HitMode)fallout::HIT_MODE_PUNCH);
    std::printf("anim=%d,%d,%d,%d,%d\n", animPrimary, animSecondary, animPunch, animKick, animDog);

    int critNull = fallout::weaponGetCriticalFailureType(nullptr);
    int critGun = fallout::weaponGetCriticalFailureType(gun);
    std::printf("critfail=%d,%d\n", critNull, critGun);

    int qty = 2;
    int ammoRc = fallout::weaponComputeAmmoCost(gun, &qty);
    std::printf("ammocost rc=%d qty=%d\n", ammoRc, qty);

    int critHammer = fallout::unarmedGetBonusCriticalChance((fallout::HitMode)fallout::HIT_MODE_HAMMER_PUNCH);
    int critKick = fallout::unarmedGetBonusCriticalChance((fallout::HitMode)fallout::HIT_MODE_PIERCING_KICK);
    int critPunch = fallout::unarmedGetBonusCriticalChance((fallout::HitMode)fallout::HIT_MODE_PUNCH);
    std::printf("bonuscrit=%d,%d,%d\n", critHammer, critKick, critPunch);
}

namespace fallout {
struct AiPacket;
int f2jReferenceCombatAttack(Object*, Object*, HitMode, HitLocation,
    CombatStartData*, int, bool, Attack*);
int f2jReferenceCheckBadShot(Object*, Object*, HitMode, bool, int, int, int);
int f2jReferenceCriticalHit(Object*, Object*, HitMode, HitLocation, int, Attack*);
int f2jReferenceSpray(Object*, Object*, Object*, HitMode, int, int, int*, Attack*);
void f2jReferenceExplosionExtras(Object*, Object*, Object*, HitMode, int, Attack*);
void f2jReferenceRangedMiss(Object*, Object*, Object*, HitMode, int, bool, Attack*);
int f2jReferencePlayerKnockedOutBy(int);
void f2jReferenceSetEndingGuy(Object*);
int f2jReferenceAiInit();
int f2jReferenceAiCount();
const char* f2jReferenceAiName(int);
void f2jReferenceAiPacketDump(int, int*);
void f2jReferenceAiClearHurtTooMuch(int);
int f2jReferenceRating(Object*);
AreaAttackMode f2jReferenceAreaAttackMode(Object*);
RunAwayMode f2jReferenceRunAwayMode(Object*);
AiPacket* f2jReferenceAiPacketRef(int);
AiPacket* f2jReferenceAiPacketByNum(int);
int f2jReferenceAiRangeForHitMode(Object*, Object*, HitMode);
bool f2jReferenceAiWeaponAttackInRange(Object*, Object*, Object*);
int f2jReferenceCaiGetMinHp(AiPacket*);
bool f2jReferenceCaiHasWeapPrefType(AiPacket*, AttackType);
bool f2jReferenceAiCanUseWeapon(Object*, Object*, HitMode);
void f2jReferenceCheckRetaliation(Object*, Object*);
void f2jReferenceAiSetBestWeapon(AiPacket*, BestWeapon);
void f2jReferenceAiSetRunAwayMode(AiPacket*, RunAwayMode);
void f2jReferenceAiSetMinHp(AiPacket*, int);
void f2jReferenceAiSetMinToHit(AiPacket*, int);
int f2jReferenceAiMinToHit(AiPacket*);
int f2jReferenceAiBestWeapon(AiPacket*);
int f2jReferenceAiRunAwayMode(AiPacket*);
int f2jReferenceAiMinHp(AiPacket*);
bool f2jReferenceCritterCanUseWeapon(Object*, Object*, HitMode);
void f2jReferenceAiAssignPacket(Object*, AiPacket*);
bool f2jReferenceWeaponCanBeReloadedWith(Object*, Object*);
bool f2jReferenceAiHaveAmmo(Object*, Object*, Object**);
Object* f2jReferenceAiBestWeapon(Object*, Object*, Object*, Object*);
Object* f2jReferenceAiSearchInvenWeap(Object*, bool, Object*);
Object* f2jReferenceAiSearchInvenArmor(Object*);
bool f2jReferenceAiCanUseItem(Object*, Object*);
HitMode f2jReferenceAiPickHitMode(Object*, Object*, Object*);
int f2jReferenceAiTryAttackSwitchFix(Object*, Object*, HitMode*, Object**);
bool f2jReferenceCombatSafetyInvalidateWeapon(Object*, Object*, HitMode, Object*);
void f2jReferenceAiSetAreaAttackMode(AiPacket*, AreaAttackMode);
int f2jReferenceAiAreaAttackMode(AiPacket*);
int f2jReferenceAiSecondaryFreq(AiPacket*);
void f2jReferenceAiSetSecondaryFreq(AiPacket*, int);
int f2jReferenceAiGetChemUse(AiPacket*);
void f2jReferenceAiSetChemUse(AiPacket*, ChemUse);
int f2jReferenceAiChemPrimaryDesire(AiPacket*, int);
void f2jReferenceAiSetChemPrimaryDesire(AiPacket*, int, int);
void f2jReferenceAiSetBurstDisabled(Object*, bool);
bool f2jReferenceAiIsBurstDisabled(Object*);
void f2jReferenceAiBeginCritters(int, Object**);
void f2jReferenceAiOverCritters();
int f2jReferenceAiCurrCritNum();
void f2jReferenceAiSortListDistance(Object**, int, Object*);
void f2jReferenceAiSortListStrength(Object**, int);
void f2jReferenceAiSortListWeakness(Object**, int);
Object* f2jReferenceAiFindNearestTeam(Object*, Object*, int);
Object* f2jReferenceAiFindNearestTeamInCombat(Object*, Object*, int);
void f2jReferenceAiFindAttackers(Object*, Object**);
Object* f2jReferenceAiDangerSource(Object*);
int f2jReferenceAiIsWithinPerception(Object*, Object*, int);
bool f2jReferenceCritterIsFleeing(Object*);
int f2jReferenceAiRating(Object*);
void f2jReferenceAiSetPacketAttackWho(AiPacket*, int);
void f2jReferenceAiSetPacketDisposition(AiPacket*, int);
void f2jReferenceAiSetPacketDistance(AiPacket*, int);
int f2jReferenceAiAttackWho(AiPacket*);
int f2jReferenceAiDisposition(AiPacket*);
int f2jReferenceAiDistance(AiPacket*);
int f2jReferenceAiGetDisposition(Object*);
int f2jReferenceAiGetDistance(Object*);
int f2jReferenceAiGetAttackWho(Object*);
int f2jReferenceAiGetBestWeapon(Object*);
void f2jReferenceAiInfoInit(int);
Object* f2jReferenceAiInfoGetLastTarget(Object*);
int f2jReferenceAiInfoSetLastTarget(Object*, Object*);
Object* f2jReferenceAiInfoGetFriendlyDead(Object*);
int f2jReferenceAiInfoSetFriendlyDead(Object*, Object*);
void f2jReferenceAiSetPacketMaxDist(AiPacket*, int);
int f2jReferenceAiMoveStepsCloser(Object*, Object*, int, bool);
int f2jReferenceAiMoveCloser(Object*, Object*, bool);
void f2jReferenceAiRunAway(Object*, Object*);
int f2jReferenceAiMoveAway(Object*, Object*, int);
bool f2jReferenceAiFindFriend(Object*, int, int);
int f2jReferenceAiPerformDistancePrefs(Object*, Object*);
bool f2jReferenceAiFindFriend(Object*, int, int);
int f2jReferenceAiCalledShot(Object*, Object*, HitMode);
int f2jReferenceAiCheckDrugs(Object*);
void f2jReferenceCombatAi(Object*, Object*);
bool f2jReferenceWantToJoin(Object*);
bool f2jReferenceWantToStop(Object*);
int f2jReferenceAiMagicHands(Object*, Object*, int);
int f2jReferenceAiSearchEnviron(Object*, int);
int f2jReferenceAiRetrieveObject(Object*, Object*);
int f2jReferenceAiAttack(Object*, Object*, HitMode);
int f2jReferenceAiCalledFreq(AiPacket*);
void f2jReferenceAiSetCalledFreq(AiPacket*, int);
void f2jReferenceSetTurnRunning(int);
void f2jReferenceSetCombatRoster(Object**, int, int, int);
int f2jReferenceCombatAttemptEnd();
void f2jReferenceSetProbeScheduling(bool, int);
void f2jReferenceAiRetargetFromFriendlyFire(Object*, Object*, int*);
bool f2jReferenceAiAttackWouldIntersect(Object*, Object*, Object*, int, int*);
int f2jReferenceAnimLastAction(int*);
}

void sectionCombatBadShot()
{
    std::puts("[combatbadshot]");
    fallout::Object* attacker = nullptr;
    fallout::Object* defender = nullptr;
    fallout::Object* weapon = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8)); // 10mm pistol
    if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(attacker, 20100, 0, nullptr);
    fallout::objectSetLocation(defender, 20101, 0, nullptr);
    fallout::itemAdd(attacker, weapon, 1);
    weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    int mode = fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY;
    std::printf("ok=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 10, -1));
    defender->data.critter.combat.results = fallout::DAM_DEAD;
    std::printf("dead=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 10, -1));
    defender->data.critter.combat.results = fallout::DAM_NONE;
    std::printf("low-ap=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 2, 10, -1));
    std::printf("noammo=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 0, -1));
    fallout::objectSetLocation(defender, 23000, 0, nullptr);
    std::printf("range=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 10, -1));
    fallout::objectSetLocation(defender, 20101, 0, nullptr);
    std::printf("crippled=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 10,
            fallout::DAM_CRIP_ARM_LEFT | fallout::DAM_CRIP_ARM_RIGHT));
    std::printf("bothcrippled=%d\n",
        fallout::f2jReferenceCheckBadShot(attacker, defender, (fallout::HitMode)mode, false, 10, 10,
            fallout::DAM_CRIP_ARM_LEFT));
}

void sectionSpray()
{
    std::puts("[spray]");
    fallout::Object* attacker = nullptr;
    fallout::Object* defender = nullptr;
    fallout::Object* weapon = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8)); // 10mm pistol
    if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(attacker, 20100, 0, nullptr);
    fallout::objectSetLocation(defender, 20101, 0, nullptr);
    fallout::itemAdd(attacker, weapon, 1);
    weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    fallout::Proto* wproto = nullptr;
    fallout::protoGetProto(weapon, &wproto);
    int savedRounds = wproto->item.data.weapon.rounds;
    int savedFlags = wproto->item.extendedFlags;
    wproto->item.data.weapon.rounds = 4;
    wproto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(
        (savedFlags & ~0xF) | 7); // FIRE_BURST anim
    fallout::ammoSetQuantity(weapon, 10);

    for (int seed : { 1, 46, 90, 200 }) {
        fallout::Attack attack;
        int fired = 0;
        int roll = fallout::f2jReferenceSpray(attacker, defender, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY,
            95, seed, &fired, &attack);
        std::printf("seed=%d roll=%d fired=%d extras=%d hit=%d dmg=%d flags=%d\n",
            seed, roll, fired, attack.extrasLength,
            (attack.attackerFlags & fallout::DAM_HIT) != fallout::DAM_NONE ? 1 : 0,
            attack.defenderDamage, (int)attack.defenderFlags);
    }

    wproto->item.data.weapon.rounds = savedRounds;
    wproto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(savedFlags);
}

void sectionExplosionExtras()
{
    std::puts("[explosionextras]");
    fallout::Object* attacker = nullptr;
    fallout::Object* defender = nullptr;
    fallout::Object* weapon = nullptr;
    fallout::Object* extraA = nullptr;
    fallout::Object* extraB = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8));
    fallout::objectCreateWithProtoId(&extraA, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&extraB, fallout::ProtoId(16777558));
    if (attacker == nullptr || defender == nullptr || weapon == nullptr
        || extraA == nullptr || extraB == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(attacker, 1100, 0, nullptr);
    fallout::objectSetLocation(defender, 1101, 0, nullptr);
    int tileNE = fallout::tileGetTileInDirection(1101, fallout::ROTATION_NE, 1);
    int tileSW = fallout::tileGetTileInDirection(1101, fallout::ROTATION_SW, 1);
    fallout::objectSetLocation(extraA, tileNE, 0, nullptr);
    fallout::objectSetLocation(extraB, tileSW, 0, nullptr);
    fallout::itemAdd(attacker, weapon, 1);
    weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    for (int seed : { 1, 7 }) {
        fallout::Attack snapshot;
        fallout::f2jReferenceExplosionExtras(attacker, defender, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, seed, &snapshot);
        std::printf("seed=%d aFlags=%x extras=%d", seed,
            (int)snapshot.attackerFlags, snapshot.extrasLength);
        for (int i = 0; i < snapshot.extrasLength; i += 1) {
            std::printf(" tile=%d loc=%d pid=%d dmg=%d flags=%d kb=%d",
                snapshot.extras[i]->tile, (int)snapshot.extrasHitLocation[i],
                snapshot.extras[i]->pid, snapshot.extrasDamage[i],
                (int)snapshot.extrasFlags[i], snapshot.extrasKnockback[i]);
        }
        std::putchar('\n');
    }

    fallout::Object* attacker2 = nullptr;
    fallout::Object* defender2 = nullptr;
    fallout::Object* weapon2 = nullptr;
    fallout::objectCreateWithProtoId(&attacker2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon2, fallout::ProtoId(8));
    if (attacker2 != nullptr && defender2 != nullptr && weapon2 != nullptr) {
        int neighborTile = fallout::tileGetTileInDirection(1201, fallout::ROTATION_NE, 1);
        fallout::objectSetLocation(defender2, 1201, 0, nullptr);
        fallout::objectSetLocation(attacker2, neighborTile, 0, nullptr);
        fallout::itemAdd(attacker2, weapon2, 1);
        weapon2->flags |= fallout::OBJECT_IN_RIGHT_HAND;
        for (int seed : { 3 }) {
            fallout::Attack snapshot;
            fallout::f2jReferenceExplosionExtras(attacker2, defender2, weapon2,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, seed, &snapshot);
            std::printf("backwash seed=%d aFlags=%x dmg=%d extras=%d\n",
                seed, (int)snapshot.attackerFlags, snapshot.defenderDamage,
                snapshot.extrasLength);
        }
    }
}

void sectionRangedMiss()
{
    std::puts("[rangedmiss]");
    {
        fallout::Object* attacker = nullptr;
        fallout::Object* defender = nullptr;
        fallout::Object* weapon = nullptr;
        fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
        fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
        fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8));
        if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
            std::puts("create-failed");
            return;
        }
        fallout::objectSetLocation(attacker, 2100, 0, nullptr);
        fallout::objectSetLocation(defender, 2101, 0, nullptr);
        fallout::itemAdd(attacker, weapon, 1);
        weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

        fallout::Proto* wproto = nullptr;
        fallout::protoGetProto(weapon, &wproto);
        int savedDamageType = wproto->item.data.weapon.damageType;
        int savedFlags = wproto->item.extendedFlags;
        wproto->item.data.weapon.damageType = fallout::DAMAGE_TYPE_EXPLOSION;
        wproto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(
            (savedFlags & ~0xF) | 5); // ANIM_THROW_ANIM

        for (int seed : { 3, 9, 17 }) {
            fallout::Attack snapshot;
            fallout::f2jReferenceRangedMiss(attacker, defender, weapon,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY,
                seed, true, &snapshot);
            std::printf("gr seed=%d tile=%d flags=%x dmg=%d targetpid=%d\n",
                seed, snapshot.tile, (int)snapshot.attackerFlags,
                snapshot.defenderDamage, snapshot.defender->pid);
        }

        wproto->item.data.weapon.damageType = static_cast<fallout::DamageType>(savedDamageType);
        wproto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(savedFlags);
    }

    {
        fallout::Object* attacker = nullptr;
        fallout::Object* defender = nullptr;
        fallout::Object* weapon = nullptr;
        fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
        fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
        fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8));
        if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
            std::puts("create-failed");
            return;
        }
        fallout::objectSetLocation(attacker, 2200, 0, nullptr);
        fallout::objectSetLocation(defender, 2205, 0, nullptr);
        fallout::itemAdd(attacker, weapon, 1);
        weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

        for (int seed : { 5, 11, 23 }) {
            fallout::Attack snapshot;
            fallout::f2jReferenceRangedMiss(attacker, defender, weapon,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY,
                seed, false, &snapshot);
            std::printf("rg seed=%d tile=%d flags=%x dmg=%d targetpid=%d\n",
                seed, snapshot.tile, (int)snapshot.attackerFlags,
                snapshot.defenderDamage, snapshot.defender->pid);
        }
    }
}

void sectionCombatEnd()
{
    std::puts("[combatend]");
    if (fallout::gDude == nullptr) {
        std::puts("dude=null");
        return;
    }
    std::printf("dead1=%d\n", fallout::f2jReferencePlayerKnockedOutBy(1));
    std::printf("dead0=%d\n", fallout::f2jReferencePlayerKnockedOutBy(0));

    fallout::Object* attacker = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    if (attacker == nullptr) {
        std::puts("create-failed");
        return;
    }
    attacker->data.critter.combat.team = 3;
    fallout::f2jReferenceSetEndingGuy(attacker);
    std::printf("team=%d\n", fallout::f2jReferencePlayerKnockedOutBy(0));
    fallout::f2jReferenceSetEndingGuy(nullptr);
    std::printf("team-null=%d\n", fallout::f2jReferencePlayerKnockedOutBy(0));
    std::printf("dead-team=%d\n", fallout::f2jReferencePlayerKnockedOutBy(1));
}

void sectionAi()
{
    std::puts("[ai]");
    // stat bounds + stat/ai messages (gameInit wires statsInit).
    std::printf("statsinit=%d\n", fallout::statsInit());
    int rc = fallout::f2jReferenceAiInit();
    std::printf("init=%d\n", rc);
    int count = fallout::f2jReferenceAiCount();
    std::printf("count=%d\n", count);

    for (int index : { 0, 1, 2 }) {
        const char* name = fallout::f2jReferenceAiName(index);
        if (name == nullptr) {
            continue;
        }
        // Pin the field aiPacketInit leaves uninitialized for packets without
        // the ai.txt key, otherwise the dump carries heap garbage.
        fallout::f2jReferenceAiClearHurtTooMuch(index);
        int fields[36];
        fallout::f2jReferenceAiPacketDump(index, fields);
        std::printf("p%d name=%s num=%d maxd=%d minhit=%d minhp=%d aggr=%d hurt=%d sec=%d called=%d font=%d color=%d outline=%d chance=%d\n",
            index, name, fields[0], fields[1], fields[2], fields[3],
            fields[4], fields[5], fields[6], fields[7], fields[8],
            fields[9], fields[10], fields[11]);
        std::printf("p%d run=%d,%d move=%d,%d attack=%d,%d miss=%d,%d head=%d,%d torso=%d,%d groin=%d,%d area=%d runaway=%d best=%d dist=%d who=%d chem=%d desire=%d,%d,%d disp=%d\n",
            index, fields[12], fields[13], fields[14], fields[15],
            fields[16], fields[17], fields[18], fields[19],
            fields[20], fields[21], fields[22], fields[23],
            fields[24], fields[25], fields[26], fields[27], fields[28],
            fields[29], fields[30], fields[31], fields[32], fields[33],
            fields[34], fields[35]);
    }

    fallout::Object* critter = nullptr;
    fallout::objectCreateWithProtoId(&critter, fallout::ProtoId(16777558));
    if (critter == nullptr) {
        std::puts("create-failed");
        return;
    }
    // Normalize the AC bonus (the proto's own bonus differs per loader), so
    // the vector pins only the deterministic arrangement.
    fallout::critterSetBonusStat(critter, fallout::STAT_ARMOR_CLASS, 50);
    std::printf("rate-bare=%d\n", fallout::f2jReferenceRating(critter));

    fallout::Object* wpn = nullptr;
    fallout::objectCreateWithProtoId(&wpn, fallout::ProtoId(8));
    if (wpn != nullptr) {
        fallout::itemAdd(critter, wpn, 1);
        wpn->flags |= fallout::OBJECT_IN_LEFT_HAND;
        critter->data.critter.combat.aiPacket = 179;
        std::printf("rate-wpn=%d\n", fallout::f2jReferenceRating(critter));
        std::printf("area=%d runaway=%d\n",
            (int)fallout::f2jReferenceAreaAttackMode(critter),
            (int)fallout::f2jReferenceRunAwayMode(critter));
    }

    // A packet without an explicit run-away mode picks one from min_hp vs
    // max hit points. Arrange primary stats and derive max HP.
    fallout::Object* critter2 = nullptr;
    fallout::objectCreateWithProtoId(&critter2, fallout::ProtoId(16777558));
    if (critter2 != nullptr) {
        fallout::critterSetBaseStat(critter2, fallout::STAT_STRENGTH, 5);
        fallout::critterSetBaseStat(critter2, fallout::STAT_ENDURANCE, 5);
        fallout::critterUpdateDerivedStats(critter2);
        critter2->data.critter.combat.aiPacket = 130;
        std::printf("runaway2=%d\n",
            (int)fallout::f2jReferenceRunAwayMode(critter2));
    }
}

void sectionAiDecisions()
{
    std::puts("[aidec]");
    std::printf("statsinit=%d\n", fallout::statsInit());
    std::printf("init=%d\n", fallout::f2jReferenceAiInit());

    fallout::Object* critter = nullptr;
    fallout::objectCreateWithProtoId(&critter, fallout::ProtoId(16777558));
    fallout::Object* weapon = nullptr;
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8));
    if (critter == nullptr || weapon == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::Proto* cproto = nullptr;
    fallout::protoGetProto(critter, &cproto);

    // aiGetWeaponRangeForHitMode: weapon ranges, unarmed (long limbs), and
    // the thrown-weapon strength clamp.
    std::printf("range-p=%d range-s=%d\n",
        fallout::f2jReferenceAiRangeForHitMode(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY),
        fallout::f2jReferenceAiRangeForHitMode(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY));
    std::printf("range-null=%d\n",
        fallout::f2jReferenceAiRangeForHitMode(critter, nullptr,
            (fallout::HitMode)fallout::HIT_MODE_PUNCH));
    cproto->critter.data.flags |= fallout::CRITTER_LONG_LIMBS;
    std::printf("range-null-long=%d\n",
        fallout::f2jReferenceAiRangeForHitMode(critter, nullptr,
            (fallout::HitMode)fallout::HIT_MODE_PUNCH));
    cproto->critter.data.flags &= ~fallout::CRITTER_LONG_LIMBS;

    fallout::Object* spear = nullptr;
    fallout::objectCreateWithProtoId(&spear, fallout::ProtoId(7));
    if (spear != nullptr) {
        fallout::Proto* sproto = nullptr;
        fallout::protoGetProto(spear, &sproto);
        int maxRange2 = sproto->item.data.weapon.maxRange2;
        std::printf("range-throw=%d cap=%d\n",
            fallout::f2jReferenceAiRangeForHitMode(critter, spear,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY),
            fallout::f2jReferenceAiRangeForHitMode(critter, spear,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY));
        sproto->item.data.weapon.maxRange2 = 40;
        int str = fallout::critterGetStat(critter, fallout::STAT_STRENGTH);
        std::printf("range-throw-40=%d str=%d\n",
            fallout::f2jReferenceAiRangeForHitMode(critter, spear,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY),
            str);
        sproto->item.data.weapon.maxRange2 = maxRange2;
    }

    // aiWeaponAttackInRange: pistol (primary only) at 2/26 tiles; spear
    // (short melee primary, longer thrown secondary) at 1/3/9 tiles.
    fallout::Object* target = nullptr;
    fallout::objectCreateWithProtoId(&target, fallout::ProtoId(16777558));
    if (target != nullptr) {
        fallout::objectSetLocation(critter, 20100, 0, nullptr);
        fallout::objectSetLocation(target, 20102, 0, nullptr);
        std::printf("inrange-pistol-2=%d\n",
            fallout::f2jReferenceAiWeaponAttackInRange(critter, weapon, target) ? 1 : 0);
        fallout::objectSetLocation(target, 20126, 0, nullptr);
        std::printf("inrange-pistol-26=%d\n",
            fallout::f2jReferenceAiWeaponAttackInRange(critter, weapon, target) ? 1 : 0);
        if (spear != nullptr) {
            fallout::objectSetLocation(target, 20101, 0, nullptr);
            std::printf("inrange-spear-1=%d\n",
                fallout::f2jReferenceAiWeaponAttackInRange(critter, spear, target) ? 1 : 0);
            fallout::objectSetLocation(target, 20103, 0, nullptr);
            std::printf("inrange-spear-3=%d\n",
                fallout::f2jReferenceAiWeaponAttackInRange(critter, spear, target) ? 1 : 0);
            fallout::objectSetLocation(target, 20109, 0, nullptr);
            std::printf("inrange-spear-9=%d\n",
                fallout::f2jReferenceAiWeaponAttackInRange(critter, spear, target) ? 1 : 0);
        }
    }

    // _cai_get_min_hp: null, explicit modes (incl. LAST), derived min_hp
    // (INVALID), and an out-of-range mode.
    fallout::AiPacket* a0 = fallout::f2jReferenceAiPacketRef(0);
    fallout::AiPacket* a2 = fallout::f2jReferenceAiPacketRef(2);
    int savedA0Best = fallout::f2jReferenceAiBestWeapon(a0);
    int savedA0MinToHit = fallout::f2jReferenceAiMinToHit(a0);
    int savedA2Mode = fallout::f2jReferenceAiRunAwayMode(a2);
    int savedA2MinHp = fallout::f2jReferenceAiMinHp(a2);
    std::printf("minhp-null=%d\n", fallout::f2jReferenceCaiGetMinHp(nullptr));
    fallout::f2jReferenceAiSetRunAwayMode(a2, fallout::RUN_AWAY_MODE_NONE);
    std::printf("minhp-none=%d\n", fallout::f2jReferenceCaiGetMinHp(a2));
    fallout::f2jReferenceAiSetRunAwayMode(a2, fallout::RUN_AWAY_MODE_COWARD);
    std::printf("minhp-coward=%d\n", fallout::f2jReferenceCaiGetMinHp(a2));
    fallout::f2jReferenceAiSetRunAwayMode(a2, fallout::RUN_AWAY_MODE_NEVER);
    std::printf("minhp-never=%d\n", fallout::f2jReferenceCaiGetMinHp(a2));
    fallout::f2jReferenceAiSetRunAwayMode(a2, fallout::RUN_AWAY_MODE_INVALID);
    fallout::f2jReferenceAiSetMinHp(a2, 37);
    std::printf("minhp-invalid=%d\n", fallout::f2jReferenceCaiGetMinHp(a2));
    fallout::f2jReferenceAiSetRunAwayMode(a2, (fallout::RunAwayMode)-2);
    std::printf("minhp-bad=%d\n", fallout::f2jReferenceCaiGetMinHp(a2));

    // _caiHasWeapPrefType: one bit mask per best_weapon row, ordered
    // NONE, UNARMED, MELEE, THROW, RANGED.
    {
        int types[5] = {
            fallout::ATTACK_TYPE_NONE,
            fallout::ATTACK_TYPE_UNARMED,
            fallout::ATTACK_TYPE_MELEE,
            fallout::ATTACK_TYPE_THROW,
            fallout::ATTACK_TYPE_RANGED,
        };
        for (int best = -1; best < fallout::BEST_WEAPON_COUNT; best++) {
            fallout::f2jReferenceAiSetBestWeapon(a0, (fallout::BestWeapon)best);
            int mask = 0;
            for (int i = 0; i < 5; i++) {
                if (fallout::f2jReferenceCaiHasWeapPrefType(
                        a0, (fallout::AttackType)types[i])) {
                    mask |= 1 << i;
                }
            }
            std::printf("pref-%d=%d\n", best, mask);
        }
    }

    // _ai_can_use_weapon: art gate, skill vs min_to_hit, weapon preference.
    // Frame 11 is hmjmps, whose pistol fire-single art exists; the maddog
    // object's own frame 16 has no such art.
    int maddogFid = critter->fid;
    critter->fid = fallout::FrmId(fallout::OBJ_TYPE_CRITTER, 11,
        fallout::ANIM_STAND, fallout::WeaponAnimation::None,
        fallout::ROTATION_NE).fid();
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_RANGED);
    fallout::f2jReferenceAiSetMinToHit(a0, 90);
    fallout::f2jReferenceAiAssignPacket(critter, a0);
    int savedSkill = cproto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
    std::printf("canuse-skill=%d\n",
        fallout::f2jReferenceAiCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    cproto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 120;
    std::printf("canuse-true=%d\n",
        fallout::f2jReferenceAiCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_MELEE);
    std::printf("canuse-pref=%d\n",
        fallout::f2jReferenceAiCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_RANGED);
    critter->fid = maddogFid;
    std::printf("canuse-art=%d\n",
        fallout::f2jReferenceAiCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    critter->fid = fallout::FrmId(fallout::OBJ_TYPE_CRITTER, 11,
        fallout::ANIM_STAND, fallout::WeaponAnimation::None,
        fallout::ROTATION_NE).fid();
    cproto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;

    // critterCanUseWeapon: type gates, crippled arms, two-handed weapons.
    fallout::Object* rifle = nullptr;
    fallout::objectCreateWithProtoId(&rifle, fallout::ProtoId(10));
    fallout::Object* nonWeapon = nullptr;
    fallout::objectCreateWithProtoId(&nonWeapon, fallout::ProtoId(1));
    auto savedResults = critter->data.critter.combat.results;
    std::printf("cuw-null-critter=%d\n",
        fallout::f2jReferenceCritterCanUseWeapon(nullptr, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    std::printf("cuw-null-weapon=%d\n",
        fallout::f2jReferenceCritterCanUseWeapon(critter, nullptr,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    if (nonWeapon != nullptr) {
        std::printf("cuw-nonweapon=%d\n",
            fallout::f2jReferenceCritterCanUseWeapon(critter, nonWeapon,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    }
    std::printf("cuw-art=%d\n",
        fallout::f2jReferenceCritterCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    if (rifle != nullptr) {
        std::printf("cuw-twohand=%d\n",
            fallout::f2jReferenceCritterCanUseWeapon(critter, rifle,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
        critter->data.critter.combat.results |= fallout::DAM_CRIP_ARM_RIGHT;
        std::printf("cuw-twohand-crip=%d\n",
            fallout::f2jReferenceCritterCanUseWeapon(critter, rifle,
                (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    }
    std::printf("cuw-onehand-crip=%d\n",
        fallout::f2jReferenceCritterCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    critter->data.critter.combat.results |= fallout::DAM_CRIP_ARM_ANY;
    std::printf("cuw-bothcrip=%d\n",
        fallout::f2jReferenceCritterCanUseWeapon(critter, weapon,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0);
    critter->data.critter.combat.results = savedResults;
    critter->fid = maddogFid;

    // _combatai_check_retaliation: no target, weaker/stronger/equal ratings.
    // The rating adds the best held weapon's max damage, so the armed
    // critters outrank the bare one without touching the shared proto.
    fallout::Object* attacker = nullptr;
    fallout::Object* weak = nullptr;
    fallout::Object* dead = nullptr;
    fallout::Object* strong = nullptr;
    fallout::Object* equal = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weak, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&dead, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&strong, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&equal, fallout::ProtoId(16777558));
    fallout::Object* armed1 = nullptr;
    fallout::Object* armed2 = nullptr;
    fallout::objectCreateWithProtoId(&armed1, fallout::ProtoId(10));
    fallout::objectCreateWithProtoId(&armed2, fallout::ProtoId(10));
    if (attacker != nullptr && weak != nullptr && dead != nullptr
        && strong != nullptr && equal != nullptr
        && armed1 != nullptr && armed2 != nullptr) {
        attacker->data.critter.combat.team = 1;
        weak->data.critter.combat.team = 2;
        dead->data.critter.combat.team = 2;
        strong->data.critter.combat.team = 3;
        equal->data.critter.combat.team = 3;
        fallout::itemAdd(strong, armed1, 1);
        armed1->flags |= fallout::OBJECT_IN_RIGHT_HAND;
        fallout::itemAdd(equal, armed2, 1);
        armed2->flags |= fallout::OBJECT_IN_RIGHT_HAND;
        dead->data.critter.combat.results |= fallout::DAM_DEAD;

        attacker->data.critter.combat.whoHitMe = nullptr;
        fallout::f2jReferenceCheckRetaliation(attacker, weak);
        std::printf("retaliate-null=%d\n",
            attacker->data.critter.combat.whoHitMe == weak ? 1 : 0);

        attacker->data.critter.combat.whoHitMe = strong;
        fallout::f2jReferenceCheckRetaliation(attacker, dead);
        std::printf("retaliate-dead=%d\n",
            attacker->data.critter.combat.whoHitMe == strong ? 1 : 0);

        attacker->data.critter.combat.whoHitMe = weak;
        fallout::f2jReferenceCheckRetaliation(attacker, strong);
        std::printf("retaliate-strong=%d\n",
            attacker->data.critter.combat.whoHitMe == strong ? 1 : 0);

        attacker->data.critter.combat.whoHitMe = strong;
        fallout::f2jReferenceCheckRetaliation(attacker, equal);
        std::printf("retaliate-equal=%d\n",
            attacker->data.critter.combat.whoHitMe == strong ? 1 : 0);
    }

    // Leave the shared packet table as the later sections expect it.
    fallout::f2jReferenceAiSetBestWeapon(a0, (fallout::BestWeapon)savedA0Best);
    fallout::f2jReferenceAiSetMinToHit(a0, savedA0MinToHit);
    fallout::f2jReferenceAiSetRunAwayMode(a2, (fallout::RunAwayMode)savedA2Mode);
    fallout::f2jReferenceAiSetMinHp(a2, savedA2MinHp);
}

void sectionAiInventory()
{
    std::puts("[aiinv]");
    std::printf("statsinit=%d\n", fallout::statsInit());
    std::printf("init=%d\n", fallout::f2jReferenceAiInit());

    // lightSetAmbientIntensity (solar scorcher gate) dereferences the dude's
    // perk ranks, so the party/perk tables gameInit normally builds are needed.
    fallout::messageListRepositoryInit();
    fallout::partyMembersInit();
    fallout::perksInit();
    // itemIsHealing (aiCanUseItem) reads the content-config healing pid table.
    {
        char prog[] = "prog";
        char* sfallArgv[] = { prog };
        fallout::sfallConfigInit(1, sfallArgv);
        fallout::itemsInit();
    }

    int savedLight = fallout::lightGetAmbientIntensity();

    fallout::Object* dog = nullptr;
    fallout::Object* target = nullptr;
    fallout::Object* human = nullptr;
    fallout::Object* pistol = nullptr;
    fallout::Object* pistol2 = nullptr;
    fallout::Object* smg = nullptr;
    fallout::Object* spear = nullptr;
    fallout::Object* flare = nullptr;
    fallout::Object* molotov = nullptr;
    fallout::Object* ammo = nullptr;
    fallout::Object* cell = nullptr;
    fallout::Object* nonWeapon = nullptr;
    fallout::Object* scorcher = nullptr;
    fallout::Object* stimpak = nullptr;
    fallout::objectCreateWithProtoId(&dog, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&target, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&human, fallout::ProtoId(16777376)); // MAN, biped, INT 8
    fallout::objectCreateWithProtoId(&pistol, fallout::ProtoId(8)); // 10mm pistol, ammo pid 29
    fallout::objectCreateWithProtoId(&pistol2, fallout::ProtoId(8));
    fallout::objectCreateWithProtoId(&smg, fallout::ProtoId(9)); // ranged primary + burst secondary
    fallout::objectCreateWithProtoId(&spear, fallout::ProtoId(7)); // melee primary, throw secondary
    fallout::objectCreateWithProtoId(&flare, fallout::ProtoId(79)); // throw primary
    fallout::objectCreateWithProtoId(&molotov, fallout::ProtoId(159)); // throw, explosion damage
    fallout::objectCreateWithProtoId(&ammo, fallout::ProtoId(29)); // 10mm ammo
    fallout::objectCreateWithProtoId(&cell, fallout::ProtoId(38)); // small energy cell
    fallout::objectCreateWithProtoId(&nonWeapon, fallout::ProtoId(1));
    fallout::objectCreateWithProtoId(&scorcher, fallout::ProtoId(390));
    fallout::objectCreateWithProtoId(&stimpak, fallout::ProtoId(40));
    if (dog == nullptr || target == nullptr || human == nullptr || pistol == nullptr
        || pistol2 == nullptr || smg == nullptr || spear == nullptr || flare == nullptr
        || molotov == nullptr || ammo == nullptr || cell == nullptr || nonWeapon == nullptr
        || scorcher == nullptr || stimpak == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(dog, 20100, 0, nullptr);
    fallout::objectSetLocation(human, 20100, 0, nullptr);
    fallout::objectSetLocation(target, 20101, 0, nullptr);
    fallout::Proto* humanProto = nullptr;
    fallout::protoGetProto(human, &humanProto);
    fallout::Proto* pistolProto = nullptr;
    fallout::protoGetProto(pistol, &pistolProto);
    fallout::AiPacket* a0 = fallout::f2jReferenceAiPacketRef(0);
    int savedBest = fallout::f2jReferenceAiBestWeapon(a0);
    int savedMinToHit = fallout::f2jReferenceAiMinToHit(a0);
    int savedArea = fallout::f2jReferenceAiAreaAttackMode(a0);
    int savedFreq = fallout::f2jReferenceAiSecondaryFreq(a0);
    int savedChemUse = fallout::f2jReferenceAiGetChemUse(a0);
    constexpr int kChemDesireCount = 3;
    int savedDesire[kChemDesireCount];
    for (int i = 0; i < kChemDesireCount; i++) {
        savedDesire[i] = fallout::f2jReferenceAiChemPrimaryDesire(a0, i);
    }

    // weaponCanBeReloadedWith: type/caliber/ammo-type gates.
    std::printf("reload-match=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, ammo) ? 1 : 0);
    std::printf("reload-cell=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, cell) ? 1 : 0);
    std::printf("reload-null-weapon=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(nullptr, ammo) ? 1 : 0);
    std::printf("reload-null-ammo=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, nullptr) ? 1 : 0);
    std::printf("reload-nonammo=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, nonWeapon) ? 1 : 0);
    std::printf("reload-weapon-ammo=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, pistol2) ? 1 : 0);
    int savedWeaponAmmoType = pistol->data.item.weapon.ammoTypePid;
    int savedWeaponAmmoQuantity = pistol->data.item.weapon.ammoQuantity;
    pistol->data.item.weapon.ammoQuantity = 5;
    pistol->data.item.weapon.ammoTypePid = cell->pid;
    std::printf("reload-occupied=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, ammo) ? 1 : 0);
    pistol->data.item.weapon.ammoQuantity = 0;
    std::printf("reload-empty=%d\n", fallout::f2jReferenceWeaponCanBeReloadedWith(pistol, ammo) ? 1 : 0);
    pistol->data.item.weapon.ammoTypePid = savedWeaponAmmoType;
    pistol->data.item.weapon.ammoQuantity = savedWeaponAmmoQuantity;

    // aiHaveAmmo: ammo match, solar scorcher light gate, right-hand unequip.
    {
        fallout::Object* h1 = nullptr;
        fallout::Object* h2 = nullptr;
        fallout::Object* h3 = nullptr;
        fallout::Object* h4 = nullptr;
        fallout::Object* h5 = nullptr;
        fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h2, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h3, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h4, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h5, fallout::ProtoId(16777376));
        fallout::itemAdd(h1, ammo, 1);
        fallout::Object* out = nullptr;
        bool haveAmmo = fallout::f2jReferenceAiHaveAmmo(h1, pistol, &out);
        std::printf("haveammo=%d out=%d\n", haveAmmo ? 1 : 0, out == ammo ? 1 : 0);
        out = nullptr;
        haveAmmo = fallout::f2jReferenceAiHaveAmmo(h2, pistol, &out);
        std::printf("haveammo-none=%d out=%d\n", haveAmmo ? 1 : 0, out == nullptr ? 1 : 0);
        fallout::itemAdd(h3, cell, 1);
        std::printf("haveammo-cell=%d\n",
            fallout::f2jReferenceAiHaveAmmo(h3, pistol, nullptr) ? 1 : 0);
        fallout::lightSetAmbientIntensity(LIGHT_INTENSITY_MAX, false);
        std::printf("scorcher-bright=%d\n",
            fallout::f2jReferenceAiHaveAmmo(h2, scorcher, nullptr) ? 1 : 0);
        fallout::lightSetAmbientIntensity(0, false);
        std::printf("scorcher-dark=%d\n",
            fallout::f2jReferenceAiHaveAmmo(h2, scorcher, nullptr) ? 1 : 0);
        fallout::lightSetAmbientIntensity(savedLight, false);
        fallout::itemAdd(h4, spear, 1);
        // Flags only: headless inven_wield/_dude_stand needs critter art.
        spear->flags |= fallout::OBJECT_IN_RIGHT_HAND;
        fallout::itemAdd(h4, cell, 1);
        fallout::f2jReferenceAiHaveAmmo(h4, spear, nullptr);
        std::printf("unequip-spear=%d\n", (spear->flags & fallout::OBJECT_IN_RIGHT_HAND) == 0 ? 1 : 0);
        fallout::itemAdd(h5, pistol, 1);
        pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;
        fallout::itemAdd(h5, cell, 1);
        fallout::f2jReferenceAiHaveAmmo(h5, pistol, nullptr);
        std::printf("unequip-pistol=%d\n", (pistol->flags & fallout::OBJECT_IN_RIGHT_HAND) != 0 ? 1 : 0);
        pistol->flags &= ~fallout::OBJECT_IN_ANY_HAND;
    }

    // _ai_best_weapon: preference order, damage tie-break, hidden/flare
    // overrides and the random mode.
    fallout::f2jReferenceAiAssignPacket(dog, a0);
    fallout::f2jReferenceAiSetMinToHit(a0, 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_RANGED);
    std::printf("best-ranged=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, pistol, spear, target) == pistol ? 1 : 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_MELEE);
    std::printf("best-melee=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, pistol, spear, target) == spear ? 1 : 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_NO_PREF);
    std::printf("best-nopref=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, pistol, spear, target) == pistol ? 1 : 0);
    std::printf("best-tie=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, pistol, pistol2, target) == pistol ? 1 : 0);
    std::printf("best-null=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, nullptr, nullptr, target) == nullptr ? 1 : 0);
    std::printf("best-null-attacker=%d\n",
        fallout::f2jReferenceAiBestWeapon(nullptr, pistol, spear, target) == nullptr ? 1 : 0);
    fallout::objectSetLocation(target, 20126, 0, nullptr);
    std::printf("best-far-null=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, nullptr, nullptr, target) == nullptr ? 1 : 0);
    fallout::objectSetLocation(target, 20101, 0, nullptr);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_RANDOM);
    fallout::randomSeedPrerandom(12345);
    fallout::Object* randomPick = fallout::f2jReferenceAiBestWeapon(dog, pistol, spear, target);
    std::printf("best-random=%d\n", randomPick == pistol ? 1 : randomPick == spear ? 2 : 0);
    fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_RANGED);
    int savedExtendedFlags = pistolProto->item.extendedFlags;
    pistolProto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(savedExtendedFlags | fallout::PROTO_EXT_FLAG_HIDDEN);
    std::printf("best-hidden=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, pistol, spear, target) == pistol ? 1 : 0);
    pistolProto->item.extendedFlags = static_cast<fallout::ProtoExtendedFlags>(savedExtendedFlags);
    std::printf("best-flare=%d\n",
        fallout::f2jReferenceAiBestWeapon(dog, flare, pistol, target) == pistol ? 1 : 0);

    // _ai_search_inven_weap: body/AP/skill/ammo gates, right-hand skip.
    {
        int savedFid = human->fid;
        human->fid = fallout::FrmId(fallout::OBJ_TYPE_CRITTER, 11, fallout::ANIM_STAND,
            fallout::WeaponAnimation::None, fallout::ROTATION_NE).fid();
        int savedSkill = humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
        humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 120;
        fallout::f2jReferenceAiAssignPacket(human, a0);
        fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_NO_PREF);
        fallout::f2jReferenceAiSetMinToHit(a0, 0);
        human->data.critter.combat.ap = 10;

        fallout::Object* h6 = nullptr;
        fallout::Object* h7 = nullptr;
        fallout::Object* h8 = nullptr;
        fallout::Object* h9 = nullptr;
        fallout::objectCreateWithProtoId(&h6, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h7, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h8, fallout::ProtoId(16777376));
        fallout::objectCreateWithProtoId(&h9, fallout::ProtoId(16777376));
        fallout::Object* humans[] = { h6, h7, h8, h9 };
        for (fallout::Object* h : humans) {
            if (h != nullptr) {
                h->fid = human->fid;
                fallout::f2jReferenceAiAssignPacket(h, a0);
                h->data.critter.combat.ap = 10;
            }
        }
        if (h6 != nullptr && h7 != nullptr && h8 != nullptr && h9 != nullptr) {
            std::printf("sweap-empty=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h6, true, target) == nullptr ? 1 : 0);
            fallout::itemAdd(h7, pistol, 1);
            std::printf("sweap-noammo=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, true, target) == nullptr ? 1 : 0);
            fallout::itemAdd(h7, ammo, 1);
            std::printf("sweap-ammo=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, true, target) == pistol ? 1 : 0);
            h7->data.critter.combat.ap = 3;
            std::printf("sweap-ap-low=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, true, target) == nullptr ? 1 : 0);
            std::printf("sweap-ap-low-ignore=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, false, target) == pistol ? 1 : 0);
            h7->data.critter.combat.ap = 10;
            fallout::f2jReferenceAiSetMinToHit(a0, 200);
            std::printf("sweap-skill=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, true, target) == nullptr ? 1 : 0);
            fallout::f2jReferenceAiSetMinToHit(a0, 0);
            fallout::itemAdd(h8, spear, 1);
            std::printf("sweap-spear=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h8, true, target) == spear ? 1 : 0);
            fallout::itemAdd(h9, pistol, 1);
            pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;
            fallout::itemAdd(h9, spear, 1);
            std::printf("sweap-righthand=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h9, true, target) == spear ? 1 : 0);
            std::printf("sweap-null-def=%d\n",
                fallout::f2jReferenceAiSearchInvenWeap(h7, true, nullptr) == pistol ? 1 : 0);
            pistol->flags &= ~fallout::OBJECT_IN_ANY_HAND;
        }
        fallout::itemAdd(dog, pistol, 1);
        fallout::itemAdd(dog, ammo, 1);
        std::printf("sweap-dog=%d\n",
            fallout::f2jReferenceAiSearchInvenWeap(dog, true, target) == nullptr ? 1 : 0);

        std::printf("armor-dog=%d\n",
            fallout::f2jReferenceAiSearchInvenArmor(dog) == nullptr ? 1 : 0);
        std::printf("armor-human=%d\n",
            fallout::f2jReferenceAiSearchInvenArmor(human) == nullptr ? 1 : 0);

        humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;
        human->fid = savedFid;
    }

    // aiCanUseItem: chem desire, body/kill/intelligence gates and the
    // per-chem-use hp thresholds.
    {
        fallout::f2jReferenceAiAssignPacket(human, a0);
        fallout::f2jReferenceAiSetChemPrimaryDesire(a0, 0, 40);
        fallout::f2jReferenceAiSetChemPrimaryDesire(a0, 1, -1);
        fallout::f2jReferenceAiSetChemPrimaryDesire(a0, 2, -1);
        human->data.critter.hp = 1000;
        std::printf("item-desire=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        for (int i = 0; i < kChemDesireCount; i++) {
            fallout::f2jReferenceAiSetChemPrimaryDesire(a0, i, savedDesire[i]);
        }
        human->data.critter.hp = 0;
        std::printf("item-stimpak=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        std::printf("item-notheal=%d\n", fallout::f2jReferenceAiCanUseItem(human, nonWeapon) ? 1 : 0);
        human->data.critter.hp = 1000;
        std::printf("item-hp=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        human->data.critter.hp = 0;
        std::printf("item-null=%d,%d\n",
            fallout::f2jReferenceAiCanUseItem(nullptr, stimpak) ? 1 : 0,
            fallout::f2jReferenceAiCanUseItem(human, nullptr) ? 1 : 0);

        int savedInt = humanProto->critter.data.baseStats[fallout::STAT_INTELLIGENCE];
        humanProto->critter.data.baseStats[fallout::STAT_INTELLIGENCE] = 2;
        std::printf("item-int=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        humanProto->critter.data.baseStats[fallout::STAT_INTELLIGENCE] = savedInt;

        fallout::KillType savedKill = humanProto->critter.data.killType;
        humanProto->critter.data.killType = fallout::KILL_TYPE_DOG;
        std::printf("item-kill=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        humanProto->critter.data.killType = savedKill;

        fallout::BodyType savedBody = humanProto->critter.data.bodyType;
        humanProto->critter.data.bodyType = fallout::BODY_TYPE_QUADRUPED;
        std::printf("item-body=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        humanProto->critter.data.bodyType = savedBody;

        int savedMaxHp = humanProto->critter.data.baseStats[fallout::STAT_MAXIMUM_HIT_POINTS];
        int savedBonusMaxHp = humanProto->critter.data.bonusStats[fallout::STAT_MAXIMUM_HIT_POINTS];
        humanProto->critter.data.baseStats[fallout::STAT_MAXIMUM_HIT_POINTS] = 100;
        humanProto->critter.data.bonusStats[fallout::STAT_MAXIMUM_HIT_POINTS] = 0;
        human->data.critter.hp = 40;
        fallout::f2jReferenceAiSetChemUse(a0, fallout::CHEM_USE_CLEAN);
        std::printf("item-chem-clean=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        fallout::f2jReferenceAiSetChemUse(a0, fallout::CHEM_USE_STIMS_WHEN_HURT_LITTLE);
        std::printf("item-chem-little=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        fallout::f2jReferenceAiSetChemUse(a0, fallout::CHEM_USE_STIMS_WHEN_HURT_LOTS);
        std::printf("item-chem-lots=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        fallout::f2jReferenceAiSetChemUse(a0, fallout::CHEM_USE_ALWAYS);
        std::printf("item-chem-always=%d\n", fallout::f2jReferenceAiCanUseItem(human, stimpak) ? 1 : 0);
        humanProto->critter.data.baseStats[fallout::STAT_MAXIMUM_HIT_POINTS] = savedMaxHp;
        humanProto->critter.data.bonusStats[fallout::STAT_MAXIMUM_HIT_POINTS] = savedBonusMaxHp;
        human->data.critter.hp = 0;
    }

    // _combat_safety_invalidate_weapon: non-explosive weapons and the
    // explosion radius vs attacker intelligence/distance.
    std::printf("safety-pistol=%d\n",
        fallout::f2jReferenceCombatSafetyInvalidateWeapon(human, pistol,
            fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, target) ? 1 : 0);
    std::printf("safety-smg=%d\n",
        fallout::f2jReferenceCombatSafetyInvalidateWeapon(human, smg,
            fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY, target) ? 1 : 0);
    fallout::explosionSetRadius(3);
    std::printf("safety-grenade-int=%d\n",
        fallout::f2jReferenceCombatSafetyInvalidateWeapon(human, molotov,
            fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, target) ? 1 : 0);
    std::printf("safety-grenade-dog=%d\n",
        fallout::f2jReferenceCombatSafetyInvalidateWeapon(dog, molotov,
            fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, target) ? 1 : 0);
    fallout::objectSetLocation(target, 20126, 0, nullptr);
    std::printf("safety-grenade-far=%d\n",
        fallout::f2jReferenceCombatSafetyInvalidateWeapon(human, molotov,
            fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, target) ? 1 : 0);
    fallout::objectSetLocation(target, 20101, 0, nullptr);
    fallout::explosionSetRadius(-1);

    // _ai_pick_hit_mode: secondary mode by area-attack policy, preference,
    // burst disable, distance and the random frequency path.
    {
        int savedFid = human->fid;
        human->fid = fallout::FrmId(fallout::OBJ_TYPE_CRITTER, 11, fallout::ANIM_STAND,
            fallout::WeaponAnimation::None, fallout::ROTATION_NE).fid();
        int savedSkill = humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
        humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 120;
        fallout::f2jReferenceAiAssignPacket(human, a0);
        fallout::f2jReferenceAiSetMinToHit(a0, 0);
        fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_NO_PREF);
        human->data.critter.combat.ap = 10;

        std::printf("pick-null=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, nullptr, target) == fallout::HIT_MODE_PUNCH ? 1 : 0);
        std::printf("pick-nonweapon=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, nonWeapon, target) == fallout::HIT_MODE_PUNCH ? 1 : 0);
        std::printf("pick-pistol=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, pistol, target) == fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY ? 1 : 0);
        fallout::f2jReferenceAiSetAreaAttackMode(a0, fallout::AREA_ATTACK_MODE_ALWAYS);
        fallout::f2jReferenceAiSetBurstDisabled(human, true);
        std::printf("pick-burst-disabled=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY ? 1 : 0);
        fallout::f2jReferenceAiSetBurstDisabled(human, false);
        std::printf("pick-always=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY ? 1 : 0);
        fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_MELEE);
        std::printf("pick-melee-pref=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY ? 1 : 0);
        fallout::f2jReferenceAiSetBestWeapon(a0, fallout::BEST_WEAPON_NO_PREF);
        fallout::f2jReferenceAiSetAreaAttackMode(a0, fallout::AREA_ATTACK_MODE_SOMETIMES);
        fallout::f2jReferenceAiSetSecondaryFreq(a0, 1);
        fallout::randomSeedPrerandom(1);
        std::printf("pick-sometimes=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY ? 1 : 0);
        fallout::f2jReferenceAiSetAreaAttackMode(a0, fallout::AREA_ATTACK_MODE_INVALID);
        fallout::randomSeedPrerandom(1);
        std::printf("pick-default=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY ? 1 : 0);
        fallout::objectSetLocation(target, 20126, 0, nullptr);
        fallout::randomSeedPrerandom(1);
        std::printf("pick-far=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY ? 1 : 0);
        fallout::objectSetLocation(target, 20101, 0, nullptr);
        fallout::f2jReferenceAiSetAreaAttackMode(a0, fallout::AREA_ATTACK_MODE_BE_SURE);
        std::printf("pick-tohit=%d\n",
            fallout::_determine_to_hit(human, target, fallout::HIT_LOCATION_TORSO,
                fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY));
        fallout::randomSeedPrerandom(1);
        std::printf("pick-besure=%d\n",
            fallout::f2jReferenceAiPickHitMode(human, smg, target) == fallout::HIT_MODE_RIGHT_WEAPON_SECONDARY ? 1 : 0);

        humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;
        human->fid = savedFid;
    }

    // Restore the shared packet table and light level.
    fallout::f2jReferenceAiSetBestWeapon(a0, (fallout::BestWeapon)savedBest);
    fallout::f2jReferenceAiSetMinToHit(a0, savedMinToHit);
    fallout::f2jReferenceAiSetAreaAttackMode(a0, (fallout::AreaAttackMode)savedArea);
    fallout::f2jReferenceAiSetSecondaryFreq(a0, savedFreq);
    fallout::f2jReferenceAiSetChemUse(a0, (fallout::ChemUse)savedChemUse);
    for (int i = 0; i < kChemDesireCount; i++) {
        fallout::f2jReferenceAiSetChemPrimaryDesire(a0, i, savedDesire[i]);
    }
    fallout::lightSetAmbientIntensity(savedLight, false);
}

void sectionCriticalHit()
{
    std::puts("[criticalhit]");
    fallout::Object* attacker = nullptr;
    fallout::Object* defender = nullptr;
    fallout::Object* weapon = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8)); // 10mm pistol
    if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(attacker, 20100, 0, nullptr);
    fallout::objectSetLocation(defender, 20101, 0, nullptr);
    fallout::itemAdd(attacker, weapon, 1);
    weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    for (int seed : { 1, 21, 46, 71, 91, 101 }) {
        fallout::Attack attack;
        int multiplier = fallout::f2jReferenceCriticalHit(attacker, defender,
            (fallout::HitMode)fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY,
            (fallout::HitLocation)fallout::HIT_LOCATION_TORSO, seed, &attack);
        std::printf("seed=%d mult=%d flags=%d msg=%d\n",
            seed, multiplier, (int)attack.defenderFlags, attack.criticalMessageId);
    }
}

void sectionCombatAttack()
{
    std::puts("[combatattack]");
    fallout::Object* attacker = nullptr;
    fallout::Object* defender = nullptr;
    fallout::Object* weapon = nullptr;
    fallout::objectCreateWithProtoId(&attacker, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&defender, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&weapon, fallout::ProtoId(8)); // 10mm pistol
    if (attacker == nullptr || defender == nullptr || weapon == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(attacker, 20100, 0, nullptr);
    fallout::objectSetLocation(defender, 20101, 0, nullptr);
    fallout::itemAdd(attacker, weapon, 1);
    weapon->flags |= fallout::OBJECT_IN_RIGHT_HAND;
    fallout::Proto* proto = nullptr;
    fallout::protoGetProto(attacker, &proto);
    int savedSkill = proto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
    proto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 200;
    unsigned int savedTime = fallout::gameTimeGetTime();
    fallout::gameTimeSetTime(0); // no critical-roll promotion

    std::printf("weapon-skill=%d value=%d null=%d\n",
        (int)fallout::weaponGetSkillForHitMode(weapon, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY),
        fallout::weaponGetSkillValue(attacker, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY),
        fallout::weaponGetSkillValue(nullptr, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY));

    auto run = [&](const char* name, fallout::CombatStartData* start, int ap,
                   int schedule, bool finish, fallout::HitLocation location) {
        attacker->data.critter.hp = 50;
        attacker->data.critter.combat.ap = ap;
        attacker->data.critter.combat.results = fallout::DAM_NONE;
        defender->data.critter.hp = 50;
        defender->data.critter.combat.results = fallout::DAM_NONE;
        defender->data.critter.combat.damageLastTurn = 0;
        fallout::ammoSetQuantity(weapon, 10);
        fallout::randomSeedPrerandom(1);
        fallout::Attack attack;
        int rc = fallout::f2jReferenceCombatAttack(attacker, defender,
            fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, location, start, schedule, finish, &attack);
        std::printf("%s rc=%d ap=%d hit=%d damage=%d flags=%d used=%d ammo=%d hp=%d last=%d\n",
            name, rc, attacker->data.critter.combat.ap,
            (attack.attackerFlags & fallout::DAM_HIT) != fallout::DAM_NONE ? 1 : 0,
            attack.defenderDamage, (int)attack.defenderFlags, attack.ammoQuantity,
            fallout::ammoGetQuantity(weapon), defender->data.critter.hp,
            defender->data.critter.combat.damageLastTurn);
    };

    run("normal", nullptr, 10, 0, false, fallout::HIT_LOCATION_TORSO);
    run("aimed", nullptr, 10, 0, false, fallout::HIT_LOCATION_HEAD);
    run("low-ap", nullptr, 2, 0, false, fallout::HIT_LOCATION_TORSO);
    run("schedule-fail", nullptr, 10, -1, false, fallout::HIT_LOCATION_TORSO);
    fallout::CombatStartData start{};
    start.damageBonus = 100;
    start.minDamage = 3;
    start.maxDamage = 9;
    start.overrideAttackResults = 1;
    start.attackerResults = fallout::DAM_DEAD;
    start.targetResults = fallout::DAM_KNOCKED_DOWN;
    run("override", &start, 10, 0, false, fallout::HIT_LOCATION_TORSO);
    start.damageBonus = -100;
    run("minimum", &start, 10, 0, false, fallout::HIT_LOCATION_TORSO);
    proto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 20;
    start.damageBonus = 0;
    start.minDamage = 0;
    start.maxDamage = INT_MAX;
    start.overrideAttackResults = 0;
    start.accuracyBonus = 100;
    run("accuracy-bonus", &start, 10, 0, false, fallout::HIT_LOCATION_TORSO);
    proto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 200;
    run("finished", nullptr, 10, 0, true, fallout::HIT_LOCATION_TORSO);
    fallout::objectSetLocation(defender, 23000, 0, nullptr);
    run("out-of-range", nullptr, 10, 0, false, fallout::HIT_LOCATION_TORSO);

    proto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;
    fallout::gameTimeSetTime(savedTime);
}

void sectionPath()
{
    std::puts("[path]");

    // pathfinderFindPath (src/animation.cc): A* over the object grid with
    // _obj_blocking_at, including the SFALL multihex retarget quirk, the
    // canUseDoor gate and the radioactive-goo cost surcharge. The section
    // lays dogs/door/goo on an empty stretch of the elevation-0 grid; no
    // parent-process section runs after this one.
    fallout::Object* dude = fallout::gDude;

    fallout::Object* mover = nullptr;
    fallout::Object* human = nullptr;
    fallout::Object* door = nullptr;
    fallout::Object* goo = nullptr;
    fallout::Object* dogs[6] = { nullptr, nullptr, nullptr, nullptr, nullptr, nullptr };
    fallout::objectCreateWithProtoId(&mover, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&human, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&door, fallout::ProtoId(33554585)); // scenery 153
    fallout::objectCreateWithProtoId(&goo, fallout::ProtoId(33555417)); // scenery 985
    for (int index = 0; index < 6; index++) {
        fallout::objectCreateWithProtoId(&dogs[index], fallout::ProtoId(16777558));
    }
    if (mover == nullptr || human == nullptr || door == nullptr || goo == nullptr) {
        std::printf("create-failed mover=%d human=%d door=%d goo=%d\n",
            mover != nullptr ? 1 : 0,
            human != nullptr ? 1 : 0,
            door != nullptr ? 1 : 0,
            goo != nullptr ? 1 : 0);
        return;
    }
    for (int index = 0; index < 6; index++) {
        if (dogs[index] == nullptr) {
            std::puts("create-failed");
            return;
        }
    }

    auto park = [](fallout::Object* obj) {
        fallout::objectSetLocation(obj, 199, 0, nullptr);
    };
    auto runPath = [](fallout::Object* obj, int to, int requireEmpty, const char* tag) {
        unsigned char rotations[800];
        memset(rotations, 0xAA, sizeof(rotations));
        int rc = fallout::pathfinderFindPath(obj, obj->tile, to, rotations,
            requireEmpty, fallout::_obj_blocking_at);
        std::printf("%s=%d", tag, rc);
        if (rc > 0) {
            std::printf(" rot=");
            for (int index = 0; index < rc; index++) {
                std::printf("%s%d", index != 0 ? "," : "", (int)rotations[index]);
            }
        }
        std::printf("\n");
    };

    const int a = 24101;
    const int b = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 4);
    const int l1 = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 1);
    const int l2 = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 2);
    const int l3 = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 3);
    std::printf("tiles A=%d L1=%d L2=%d L3=%d B=%d\n", a, l1, l2, l3, b);

    for (int index = 0; index < 6; index++) {
        park(dogs[index]);
    }
    park(human);
    park(door);
    park(goo);
    fallout::objectSetLocation(mover, a, 0, nullptr);

    // Trivial and straight-line cases, with and without the empty-destination
    // guard.
    runPath(mover, a, 0, "same");
    runPath(mover, b, 0, "straight");
    runPath(mover, b, 1, "straight-empty");

    // Destination occupied by a plain dog: requireEmptyDest gates upfront,
    // otherwise the path ends on the occupied tile itself.
    park(dogs[0]);
    fallout::objectSetLocation(dogs[0], b, 0, nullptr);
    runPath(mover, b, 0, "dest-blocked");
    runPath(mover, b, 1, "dest-blocked-empty");

    // Multihex blocker on the destination retargets the search to the first
    // reachable neighbour (SFALL center-tile fix).
    dogs[0]->flags |= fallout::OBJECT_MULTIHEX;
    runPath(mover, b, 0, "dest-multihex");
    dogs[0]->flags &= ~fallout::OBJECT_MULTIHEX;

    // A wall of dogs across the straight line: detour around the flank.
    fallout::objectSetLocation(dogs[0], l1, 0, nullptr);
    fallout::objectSetLocation(dogs[1], l2, 0, nullptr);
    fallout::objectSetLocation(dogs[2], l3, 0, nullptr);
    runPath(mover, b, 0, "wall");
    for (int index = 0; index < 3; index++) {
        park(dogs[index]);
    }

    // Enclosed destination: five dogs seal every approach except l3.
    int neighbours[6];
    for (int index = 0; index < 6; index++) {
        neighbours[index] = fallout::tileGetTileInDirection(b, (fallout::Rotation)index, 1);
    }
    int l3Slot = 0;
    for (int index = 0; index < 6; index++) {
        if (neighbours[index] == l3) {
            l3Slot = index;
            continue;
        }
        fallout::objectSetLocation(dogs[index], neighbours[index], 0, nullptr);
    }
    runPath(mover, b, 0, "open-approach");
    fallout::objectSetLocation(dogs[l3Slot], l3, 0, nullptr);
    runPath(mover, b, 0, "unreachable");
    park(dogs[l3Slot]);

    // Door scenarios: five dogs seal the destination except l3, where the
    // door object stands. canUseDoor consults the proto door type, the
    // object lock flag, the biped body type, the gecko kill type and (for
    // the dude only) the proto walk-through flag. The door object must
    // block (no NO_BLOCK) for the gate to be consulted at all.
    fallout::Proto* doorProto = nullptr;
    fallout::protoGetProto(door, &doorProto);
    fallout::SceneryType savedDoorType = doorProto->scenery.type;
    int savedDoorGenericFlags = doorProto->scenery.data.generic.genericFlags;
    doorProto->scenery.type = fallout::SCENERY_TYPE_DOOR;
    doorProto->scenery.data.generic.genericFlags &= ~0x04;
    door->flags &= ~fallout::OBJECT_NO_BLOCK;

    int savedDoorOpenFlags = door->data.scenery.door.openFlags;
    door->data.scenery.door.openFlags &= ~DOOR_FLAG_LOCKED;
    fallout::objectSetLocation(door, l3, 0, nullptr);
    for (int index = 0; index < 6; index++) {
        if (index != l3Slot) {
            fallout::objectSetLocation(dogs[index], neighbours[index], 0, nullptr);
        }
    }

    fallout::objectSetLocation(human, a, 0, nullptr);
    park(mover);
    runPath(human, b, 0, "door-human");
    door->data.scenery.door.openFlags |= DOOR_FLAG_LOCKED;
    runPath(human, b, 0, "door-human-locked");
    door->data.scenery.door.openFlags &= ~DOOR_FLAG_LOCKED;

    fallout::Proto* humanProto = nullptr;
    fallout::protoGetProto(human, &humanProto);
    fallout::KillType savedHumanKillType = humanProto->critter.data.killType;
    humanProto->critter.data.killType = fallout::KILL_TYPE_GECKO;
    runPath(human, b, 0, "door-gecko");
    humanProto->critter.data.killType = savedHumanKillType;
    park(human);

    if (dude != nullptr) {
        // objectSetLocation(gDude) segfaults headless this late in the run,
        // so the dude stays in place (tile -1 in this process) and only its
        // tile field is pointed at the scenario, which is all the pathfinder
        // reads; the dude is absent from the tile lists either way.
        int savedDudeTile = dude->tile;
        dude->tile = a;
        runPath(dude, b, 0, "door-dude");
        doorProto->scenery.data.generic.genericFlags |= 0x04;
        runPath(dude, b, 0, "door-dude-walkthru");
        doorProto->scenery.data.generic.genericFlags &= ~0x04;
        dude->tile = savedDudeTile;
        std::printf("dude-restore=%d\n", dude->tile);
    }

    doorProto->scenery.type = savedDoorType;
    doorProto->scenery.data.generic.genericFlags = savedDoorGenericFlags;
    park(door);
    for (int index = 0; index < 6; index++) {
        park(dogs[index]);
    }

    // Goo scenarios: radioactive goo scenery. Without NO_BLOCK the tile is
    // skipped like any blocker; with it the tile is walkable and the A*
    // node cost surcharge applies (400, or 100 for a gecko).
    fallout::objectSetLocation(mover, a, 0, nullptr);
    fallout::objectSetLocation(goo, l2, 0, nullptr);
    int savedGooFlags = goo->flags;
    goo->flags &= ~fallout::OBJECT_NO_BLOCK;
    runPath(mover, b, 0, "goo-block");
    goo->flags |= fallout::OBJECT_NO_BLOCK;
    runPath(mover, b, 0, "goo-detour");
    fallout::Proto* moverProto = nullptr;
    fallout::protoGetProto(mover, &moverProto);
    fallout::KillType savedMoverKillType = moverProto->critter.data.killType;
    moverProto->critter.data.killType = fallout::KILL_TYPE_GECKO;
    runPath(mover, b, 0, "goo-gecko");
    moverProto->critter.data.killType = savedMoverKillType;

    // Forced crossing: the goo tile is the only approach left to the
    // destination, so the surcharge is paid either way.
    for (int index = 0; index < 6; index++) {
        if (index != l3Slot) {
            fallout::objectSetLocation(dogs[index], neighbours[index], 0, nullptr);
        }
    }
    fallout::objectSetLocation(goo, l3, 0, nullptr);
    runPath(mover, b, 0, "goo-forced");
    moverProto->critter.data.killType = fallout::KILL_TYPE_GECKO;
    runPath(mover, b, 0, "goo-forced-gecko");
    moverProto->critter.data.killType = savedMoverKillType;
    goo->flags = static_cast<fallout::ObjectFlags>(savedGooFlags);
}

void sectionAiTarget()
{
    std::puts("[aitarget]");

    // Target selection: the _curr_crit_list roster helpers (sorts, nearest
    // team, find attackers, danger source) plus perception. Boot
    // inheritance: [ai] ran aiInit, [aiinv] booted party/perks/items, tile
    // grid and object lists come from earlier sections. The section builds
    // its own roster, feeds it through _combat_ai_begin and drives
    // isInCombat through gCombatState.
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1
    fallout::Object* d1 = nullptr; // dog, team 1, the non-party actor
    fallout::Object* d2 = nullptr; // dog, team 1 (team 2 in danger scenarios)
    fallout::Object* d3 = nullptr; // species 16777249, team 2
    fallout::Object* d4 = nullptr; // species 16777252, team 2
    fallout::Object* d5 = nullptr; // dog, team 2, far away
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d3, fallout::ProtoId(16777249));
    fallout::objectCreateWithProtoId(&d4, fallout::ProtoId(16777252));
    fallout::objectCreateWithProtoId(&d5, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || d2 == nullptr || d3 == nullptr
        || d4 == nullptr || d5 == nullptr) {
        std::puts("create-failed");
        return;
    }

    // One axis south-east of a, distinct distances for the sorts. d5 looks
    // away so the far perception cases fall to hearing.
    const int a = 24101;
    const int rosterDistances[7] = { 10, 2, 0, 1, 3, 4, 30 };
    fallout::Object* roster[7] = { dude, h1, d1, d2, d3, d4, d5 };
    // The dude cannot go through objectSetLocation (known headless
    // segfault), so its tile is pointed at the axis directly; the dude is
    // absent from the tile lists either way.
    for (int index = 0; index < 7; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index == 0 ? 0 : index <= 3 ? 1 : 2;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        // critterIsDead reads current hit points: fresh objects sit at 0
        // and would read as dead for the AI info guards.
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        if (index == 2) {
            // The shooter stands behind the source so the shot line
            // crosses it.
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_NW, 1), 0, nullptr);
        } else if (index != 0) {
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
                0, nullptr);
        }
    }
    // Distances from d1: d2=1, h1=2, d3=3, d4=4, dude=10, d5=30.
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[0]);
    dude->elevation = 0;
    fallout::objectSetRotation(d5, fallout::ROTATION_NW, nullptr);

    // Distinct species give distinct ratings (melee damage + armor class)
    // for the strength/weakness sorts. Proto mutations earlier sections
    // left behind (dog AC bonus 50 from [ai], stray melee/armor bonuses)
    // apply to all critters of the species.
    fallout::f2jReferenceAiInfoInit(7);
    fallout::f2jReferenceAiBeginCritters(7, roster);

    fallout::CombatState savedCombatState = fallout::gCombatState;
    fallout::gCombatState = static_cast<fallout::CombatState>(
        fallout::COMBAT_STATE_IN_COMBAT | fallout::COMBAT_STATE_PLAYER_TURN);

    std::printf("rating=%d,%d,%d,%d,%d,%d,%d\n",
        fallout::f2jReferenceAiRating(dude),
        fallout::f2jReferenceAiRating(h1),
        fallout::f2jReferenceAiRating(d1),
        fallout::f2jReferenceAiRating(d2),
        fallout::f2jReferenceAiRating(d3),
        fallout::f2jReferenceAiRating(d4),
        fallout::f2jReferenceAiRating(d5));

    auto pids = [](fallout::Object** list, int length) {
        std::printf("order=");
        for (int index = 0; index < length; index++) {
            fallout::Object* obj = list[index];
            if (index != 0) {
                std::printf(",");
            }
            std::printf("%d", obj != nullptr ? (int)obj->pid : -1);
        }
        std::printf("\n");
    };

    fallout::Object* list[4];
    list[0] = d3;
    list[1] = nullptr;
    list[2] = d2;
    list[3] = d4;
    fallout::f2jReferenceAiSortListDistance(list, 4, d1);
    pids(list, 4);

    list[0] = d2;
    list[1] = d3;
    list[2] = d4;
    list[3] = nullptr;
    fallout::f2jReferenceAiSortListStrength(list, 3);
    pids(list, 4);
    fallout::f2jReferenceAiSortListWeakness(list, 3);
    pids(list, 4);

    // Nearest-team scans: same team (0x01) and other team (0x02), skipping
    // dead critters and the actor itself.
    std::printf("nt-same=%d\n", fallout::f2jReferenceAiFindNearestTeam(d1, d1, 1) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeam(d1, d1, 1)->pid : -1);
    d2->data.critter.combat.results = fallout::DAM_DEAD;
    std::printf("nt-same-dead=%d\n", fallout::f2jReferenceAiFindNearestTeam(d1, d1, 1) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeam(d1, d1, 1)->pid : -1);
    d2->data.critter.combat.results = fallout::DAM_NONE;
    std::printf("nt-other=%d\n", fallout::f2jReferenceAiFindNearestTeam(d1, d1, 2) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeam(d1, d1, 2)->pid : -1);
    d3->data.critter.combat.results = fallout::DAM_DEAD;
    std::printf("nt-other-dead=%d\n", fallout::f2jReferenceAiFindNearestTeam(d1, d1, 2) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeam(d1, d1, 2)->pid : -1);
    d3->data.critter.combat.results = fallout::DAM_NONE;

    // In-combat variant additionally requires whoHitMe.
    std::printf("ntc-same=%d\n", fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 1) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 1)->pid : -1);
    d2->data.critter.combat.whoHitMe = d1;
    std::printf("ntc-hit=%d\n", fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 1) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 1)->pid : -1);
    d2->data.critter.combat.whoHitMe = nullptr;
    d3->data.critter.combat.whoHitMe = d1;
    std::printf("ntc-other-hit=%d\n", fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 2) != nullptr
        ? (int)fallout::f2jReferenceAiFindNearestTeamInCombat(d1, d1, 2)->pid : -1);
    d3->data.critter.combat.whoHitMe = nullptr;

    // find-attackers: slot 0 is preserved, slots 1-3 fill from whoHitMe
    // relations.
    fallout::Object* targets[4] = { dude, nullptr, nullptr, nullptr };
    d2->data.critter.combat.whoHitMe = d1;
    h1->data.critter.combat.whoHitMe = d3;
    d4->data.critter.combat.whoHitMe = h1;
    fallout::f2jReferenceAiFindAttackers(d1, targets);
    pids(targets, 4);
    d2->data.critter.combat.whoHitMe = nullptr;
    h1->data.critter.combat.whoHitMe = nullptr;
    d4->data.critter.combat.whoHitMe = nullptr;
    fallout::f2jReferenceAiFindAttackers(d1, targets);
    pids(targets, 4);

    // Perception: visible branch (rotation within one step), glass halving,
    // hearing in combat and the dude sneak adjustments. _sneak_working is
    // true at this point of the run (earlier sections), so the sneaking
    // case below takes the quarter branch.
    std::printf("per-see=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, d2, 3));
    d5->flags |= fallout::OBJECT_TRANS_GLASS;
    std::printf("per-glass=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, d5, 3));
    d5->flags &= ~fallout::OBJECT_TRANS_GLASS;
    std::printf("per-far=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, d5, 3));
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 13);
    std::printf("per-hear=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, dude, 3));
    fallout::dudeEnableState(fallout::DUDE_STATE_SNEAKING);
    std::printf("per-sneak=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, dude, 3));
    fallout::dudeDisableState(fallout::DUDE_STATE_SNEAKING);
    fallout::gCombatState = fallout::COMBAT_STATE_PLAYER_TURN;
    std::printf("per-quiet=%d\n", fallout::f2jReferenceAiIsWithinPerception(h1, dude, 3));
    fallout::gCombatState = static_cast<fallout::CombatState>(
        fallout::COMBAT_STATE_IN_COMBAT | fallout::COMBAT_STATE_PLAYER_TURN);
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 10);
    dude->data.critter.combat.results = fallout::DAM_NONE;

    // Danger source. The packet the dude resolves to (by packet_num) drives
    // the party branch.
    fallout::AiPacket* packet0 = fallout::f2jReferenceAiPacketByNum(dude->data.critter.combat.aiPacket);
    int savedAttackWho = fallout::f2jReferenceAiAttackWho(packet0);
    int savedDisposition = fallout::f2jReferenceAiDisposition(packet0);
    int savedDistance = fallout::f2jReferenceAiDistance(packet0);
    auto ds = [&](fallout::Object* actor) {
        fallout::Object* found = fallout::f2jReferenceAiDangerSource(actor);
        std::printf("ds=%d\n", found != nullptr ? (int)found->pid : -1);
    };
    auto clearRelations = [&]() {
        for (int index = 0; index < 7; index++) {
            roster[index]->data.critter.combat.whoHitMe = nullptr;
            roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
            roster[index]->data.critter.combat.results = fallout::DAM_NONE;
            fallout::f2jReferenceAiInfoSetLastTarget(roster[index], nullptr);
        }
    };

    // Non-party actor: alive whoHitMe wins outright.
    d1->data.critter.combat.whoHitMe = d2;
    ds(d1);
    // Dead whoHitMe from the other team: nearest member of that team.
    d2->data.critter.combat.team = 2;
    d2->data.critter.combat.results = fallout::DAM_DEAD;
    ds(d1);
    // No relations at all.
    clearRelations();
    d2->data.critter.combat.team = 1;
    ds(d1);

    // Party branch, whomever-attacking-me with a valid last target.
    fallout::f2jReferenceAiSetPacketAttackWho(packet0, 0); // WHOMEVER_ATTACKING_ME
    fallout::f2jReferenceAiSetPacketDisposition(packet0, 3); // DEFENSIVE: ignore fleeing
    fallout::f2jReferenceAiSetPacketDistance(packet0, 0); // STAY_CLOSE
    d2->data.critter.combat.team = 2;
    fallout::f2jReferenceAiInfoSetLastTarget(dude, d2);
    fallout::f2jReferenceAiInfoSetLastTarget(d2, dude);
    ds(dude);
    // Fleeing target is skipped under DEFENSIVE; the scan picks a critter
    // attacking the dude.
    d2->data.critter.combat.maneuver = fallout::CRITTER_MANUEVER_FLEEING;
    d3->data.critter.combat.whoHitMe = nullptr;
    fallout::f2jReferenceAiInfoSetLastTarget(d3, dude);
    ds(dude);
    // Also fleeing: next attacker is knocked out, nobody left.
    d3->data.critter.combat.maneuver = fallout::CRITTER_MANUEVER_FLEEING;
    d4->data.critter.combat.whoHitMe = nullptr;
    fallout::f2jReferenceAiInfoSetLastTarget(d4, dude);
    d4->data.critter.combat.results = fallout::DAM_KNOCKED_OUT;
    ds(dude);
    // AGGRESSIVE: fleeing critters are not ignored, last target returns.
    d2->data.critter.combat.maneuver = fallout::CRITTER_MANUEVER_FLEEING;
    fallout::f2jReferenceAiSetPacketDisposition(packet0, 4); // AGGRESSIVE
    ds(dude);
    // STRONGEST/WEAKEST/CLOSEST with a preserved whoHitMe and an attacker;
    // the case clears dude->whoHitMe, which the WHOMEVER tail then reads.
    d3->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    d4->data.critter.combat.results = fallout::DAM_NONE;
    dude->data.critter.combat.whoHitMe = d3;
    d2->data.critter.combat.whoHitMe = dude;
    fallout::f2jReferenceAiSetPacketAttackWho(packet0, 1); // STRONGEST
    ds(dude);
    fallout::f2jReferenceAiSetPacketAttackWho(packet0, 2); // WEAKEST
    ds(dude);
    fallout::f2jReferenceAiSetPacketAttackWho(packet0, 4); // CLOSEST
    ds(dude);
    // WHOMEVER over the cleared whoHitMe: the attacker slot fills and the
    // final loop returns the first passable candidate.
    fallout::f2jReferenceAiSetPacketAttackWho(packet0, 3); // WHOMEVER
    ds(dude);

    fallout::f2jReferenceAiSetPacketAttackWho(packet0, savedAttackWho);
    fallout::f2jReferenceAiSetPacketDisposition(packet0, savedDisposition);
    fallout::f2jReferenceAiSetPacketDistance(packet0, savedDistance);
    fallout::gCombatState = savedCombatState;
    fallout::f2jReferenceAiOverCritters();
}


void sectionAiMove()
{
    std::puts("[aimove]");

    // AI movement decisions: _obj_ai_blocking_at with the _moveBlockObj BSS
    // quirk, the friendly-fire retarget (_cai_attackWouldIntersect and
    // _cai_retargetTileFromFriendlyFire) and the run/away/move-closer
    // helpers. The move helpers run OUT of combat (reg_anim_end then skips
    // _combat_anim_begin, so _combat_turn_run cannot spin); the registered
    // animation is the observable effect, dumped through the animation
    // probe TU.
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1, the weaker source
    fallout::Object* d1 = nullptr; // dog, team 1, the stronger ally/actor
    fallout::Object* d2 = nullptr; // dog, team 1, flank blocker
    fallout::Object* tgt = nullptr; // dog, team 2, the target
    fallout::Object* far = nullptr; // dog, team 2, far enemy
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&tgt, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&far, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || d2 == nullptr || tgt == nullptr
        || far == nullptr) {
        std::puts("create-failed");
        return;
    }

    const int a = 38101;
    // Roster: h1 at a (source, rating 10), d1 at +1 (ally, rating 70),
    // tgt at +4 (target), d2 parked, far at +40.
    fallout::Object* roster[5] = { dude, h1, d1, tgt, far };
    const int rosterDistances[5] = { 20, 0, -1, 4, 40 };
    for (int index = 0; index < 5; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index == 3 || index == 4 ? 2 : 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 5;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        if (index == 2) {
            // The shooter stands behind the source so the shot line
            // crosses it.
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_NW, 1), 0, nullptr);
        } else if (index != 0) {
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
                0, nullptr);
        }
    }
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[0]);
    dude->elevation = 0;

    fallout::f2jReferenceAiInfoInit(5);
    fallout::f2jReferenceAiBeginCritters(5, roster);

    auto dumpAnim = [](const char* tag) {
        int dump[12];
        if (fallout::f2jReferenceAnimLastAction(dump) == 0) {
            std::printf("%s kind=%d anim=%d dst=%d ap=%d | kind=%d anim=%d dst=%d ap=%d | kind=%d anim=%d dst=%d ap=%d\n",
                tag, dump[0], dump[1], dump[2], dump[3], dump[4], dump[5], dump[6], dump[7],
                dump[8], dump[9], dump[10], dump[11]);
        } else {
            std::printf("%s none\n", tag);
        }
    };

    // _obj_ai_blocking_at: the first critter on the tile is remembered in
    // _moveBlockObj and skipped; the call returns a non-critter blocker or
    // null, and the NEXT call returns the remembered critter.
    fallout::_moveBlockObj = nullptr;
    fallout::Object* first = fallout::_obj_ai_blocking_at(nullptr, a, 0);
    std::printf("ab1=%d block=%d\n", first != nullptr ? (int)first->pid : -1,
        fallout::_moveBlockObj != nullptr ? 1 : 0);
    fallout::Object* second = fallout::_obj_ai_blocking_at(nullptr, a, 0);
    std::printf("ab2=%d\n", second != nullptr ? (int)second->pid : -1);
    fallout::_moveBlockObj = nullptr;

    // Friendly-fire intersection, combat on (the AI info list gates the
    // retarget; the intersect check itself is stateless).
    fallout::CombatState savedCombatState = fallout::gCombatState;
    fallout::gCombatState = static_cast<fallout::CombatState>(
        fallout::COMBAT_STATE_IN_COMBAT | fallout::COMBAT_STATE_PLAYER_TURN);

    fallout::Proto* h1Proto = nullptr;
    fallout::protoGetProto(h1, &h1Proto);
    int savedHumanAcBonus = h1Proto->critter.data.bonusStats[fallout::STAT_ARMOR_CLASS];
    h1Proto->critter.data.bonusStats[fallout::STAT_ARMOR_CLASS] = 5;

    fallout::Object* pistol = nullptr;
    fallout::objectCreateWithProtoId(&pistol, fallout::ProtoId(8));
    if (pistol == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::itemAdd(d1, pistol, 1);
    pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    const int tgtTile = tgt->tile;
    int distance = 1;
    // d1 at +1 shoots at tgt at +4: the line crosses h1 at a.
    std::printf("iw-line=%d\n",
        fallout::f2jReferenceAiAttackWouldIntersect(d1, tgt, h1, tgtTile, &distance) ? 1 : 0);
    // The dude at +20 is off the line; the friend check falls to the
    // incidental-hit test.
    std::printf("iw-off=%d\n",
        fallout::f2jReferenceAiAttackWouldIntersect(d1, tgt, dude, tgtTile, &distance) ? 1 : 0);
    // Attacker without a weapon.
    std::printf("iw-unarmed=%d\n",
        fallout::f2jReferenceAiAttackWouldIntersect(h1, tgt, h1, tgtTile, &distance) ? 1 : 0);
    // Null friend behaves like a clear line.
    std::printf("iw-null-friend=%d\n",
        fallout::f2jReferenceAiAttackWouldIntersect(d1, tgt, nullptr, tgtTile, &distance) ? 1 : 0);

    // Friendly-fire retarget. The ally d1 (rating 70) is in the shot line,
    // so the target tile is abandoned for the nearest free flank. The
    // flanks fan out from the ally's facing.
    fallout::f2jReferenceAiInfoSetLastTarget(d1, tgt);
    int tile = tgtTile;
    fallout::f2jReferenceAiRetargetFromFriendlyFire(h1, tgt, &tile);
    std::printf("rt-line tile=%d\n", tile);
    // Same, but the near flank is blocked by d2.
    tile = tgtTile;
    fallout::objectSetLocation(d2, fallout::tileGetTileInDirection(tgtTile,
        (fallout::Rotation)((d1->rotation + 1) % 6), 1), 0, nullptr);
    fallout::f2jReferenceAiRetargetFromFriendlyFire(h1, tgt, &tile);
    std::printf("rt-blocked tile=%d\n", tile);
    fallout::objectSetLocation(d2, 199, 0, nullptr);
    // Weaker allies are ignored: the source is the dog (70), the only
    // ally in line is the human (10).
    tile = tgtTile;
    fallout::f2jReferenceAiRetargetFromFriendlyFire(d1, tgt, &tile);
    std::printf("rt-weak tile=%d\n", tile);
    // No allies at all.
    fallout::f2jReferenceAiInfoSetLastTarget(d1, nullptr);
    tile = tgtTile;
    fallout::f2jReferenceAiRetargetFromFriendlyFire(h1, tgt, &tile);
    std::printf("rt-none tile=%d\n", tile);
    // Source with zero intelligence skips the scan.
    int savedHumanInt = h1Proto->critter.data.baseStats[fallout::STAT_INTELLIGENCE];
    h1Proto->critter.data.baseStats[fallout::STAT_INTELLIGENCE] = 0;
    fallout::f2jReferenceAiInfoSetLastTarget(d1, tgt);
    tile = tgtTile;
    fallout::f2jReferenceAiRetargetFromFriendlyFire(h1, tgt, &tile);
    std::printf("rt-int0 tile=%d\n", tile);
    h1Proto->critter.data.baseStats[fallout::STAT_INTELLIGENCE] = savedHumanInt;

    // Movement helpers, combat off (the animation tail cannot spin).
    // The harness never ran animationInit, so the sequence store holds
    // zeroed (non-complete) slots and every registration would fail.
    fallout::animationReset();
    fallout::gCombatState = fallout::COMBAT_STATE_PLAYER_TURN;

    fallout::AiPacket* dogPacket = fallout::f2jReferenceAiPacketByNum(d1->data.critter.combat.aiPacket);
    int savedDogDistance = fallout::f2jReferenceAiDistance(dogPacket);

    // _ai_move_away with DISTANCE_STAY refuses.
    fallout::f2jReferenceAiSetPacketDistance(dogPacket, (int)fallout::DISTANCE_STAY);
    std::printf("ma-stay=%d\n", fallout::f2jReferenceAiMoveAway(d1, tgt, 3));
    // With STAY_CLOSE the dog walks away from the adjacent enemy.
    fallout::f2jReferenceAiSetPacketDistance(dogPacket, (int)fallout::DISTANCE_STAY_CLOSE);
    std::printf("ma=%d\n", fallout::f2jReferenceAiMoveAway(d1, tgt, 12));
    dumpAnim("ma-anim");

    // _ai_run_away: the enemy is within max_dist, so the dog flees. The
    // pending move-away sequence keeps the dog busy for the registry check,
    // so reset the store first.
    fallout::f2jReferenceAiSetPacketMaxDist(dogPacket, 10);
    fallout::animationReset();
    fallout::f2jReferenceAiRunAway(d1, tgt);
    std::printf("ra-fleeing=%d\n", fallout::f2jReferenceCritterIsFleeing(d1) ? 1 : 0);
    dumpAnim("ra-anim");
    // Out of range: disengaging, no animation. The maneuver accumulates,
    // so clear it first.
    d1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    fallout::f2jReferenceAiSetPacketMaxDist(dogPacket, 0);
    fallout::f2jReferenceAiRunAway(d1, far);
    std::printf("ra-far-fleeing=%d\n", fallout::f2jReferenceCritterIsFleeing(d1) ? 1 : 0);

    // _ai_move_steps_closer with plenty of action points: run branch.
    // The previous pending sequence keeps the owner busy for the registry
    // check, so reset the store before each move.
    d1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    fallout::animationReset();
    std::printf("mc=%d\n", fallout::f2jReferenceAiMoveStepsCloser(d1, tgt, 10, false));
    dumpAnim("mc-run");
    // Few action points: walk branch.
    fallout::animationReset();
    std::printf("mc-walk=%d\n", fallout::f2jReferenceAiMoveStepsCloser(d1, tgt, 1, false));
    dumpAnim("mc-walk-anim");
    // Multihex target: move to object branch (taunt prints the message).
    fallout::animationReset();
    tgt->flags |= fallout::OBJECT_MULTIHEX;
    std::printf("mc-multi=%d\n", fallout::f2jReferenceAiMoveStepsCloser(d1, tgt, 10, true));
    dumpAnim("mc-multi-anim");
    tgt->flags &= ~fallout::OBJECT_MULTIHEX;

    // _ai_find_friend: the nearest same-team friend is the human at
    // distance 1; a3 above it fires the approach move.
    fallout::animationReset();
    std::printf("ff=%d\n", fallout::f2jReferenceAiFindFriend(d1, 30, 5) ? 1 : 0);
    dumpAnim("ff-anim");

    fallout::f2jReferenceAiSetPacketDistance(dogPacket, savedDogDistance);
    h1Proto->critter.data.bonusStats[fallout::STAT_ARMOR_CLASS] = savedHumanAcBonus;
    fallout::gCombatState = savedCombatState;
    fallout::f2jReferenceAiOverCritters();
}

void sectionAiDist()
{
    std::puts("[aidist]");

    // Distance preferences (_cai_perform_distance_prefs) over the already
    // ported movement/retarget helpers. The combat tail of the retarget
    // branch cannot run headless (_combat_turn_run spins on the anim
    // counter), so the section runs out of combat where aiInfo reads are
    // null and the retarget stays inert; the retarget itself and the anim
    // registrars are pinned by [aimove].
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1, the weak sniper
    fallout::Object* d1 = nullptr; // dog, team 1, the main actor
    fallout::Object* d2 = nullptr; // dog, team 1, parked
    fallout::Object* tgt = nullptr; // dog, team 2, the target
    fallout::Object* far = nullptr; // dog, team 2, far enemy
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&tgt, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&far, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || d2 == nullptr || tgt == nullptr
        || far == nullptr) {
        std::puts("create-failed");
        return;
    }

    const int a = 38101;
    const int rosterDistances[5] = { 10, 0, -1, 4, 40 };
    fallout::Object* roster[5] = { dude, h1, d1, tgt, far };
    for (int index = 0; index < 5; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index == 3 || index == 4 ? 2 : 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 5;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        if (index == 2) {
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_NW, 1), 0, nullptr);
        } else if (index != 0) {
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
                0, nullptr);
        }
    }
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[0]);
    dude->elevation = 0;

    fallout::f2jReferenceAiInfoInit(5);
    fallout::f2jReferenceAiBeginCritters(5, roster);
    fallout::animationReset();

    auto dumpAnim = [](const char* tag) {
        int dump[14];
        if (fallout::f2jReferenceAnimLastAction(dump) != -1) {
            std::printf("%s slot=%d kind=%d anim=%d dst=%d ap=%d | kind=%d anim=%d dst=%d ap=%d | kind=%d anim=%d dst=%d ap=%d\n",
                tag, dump[12], dump[0], dump[1], dump[2], dump[3], dump[4], dump[5], dump[6],
                dump[7], dump[8], dump[9], dump[10], dump[11]);
        } else {
            std::printf("%s none\n", tag);
        }
    };

    // Unarmed AP cost for the human sniper (no weapon yet).
    std::printf("unarmed-apcost=%d\n",
        fallout::weaponGetActionPointCost(h1, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, false));

    fallout::Object* pistol = nullptr;
    fallout::objectCreateWithProtoId(&pistol, fallout::ProtoId(8));
    if (pistol == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::itemAdd(d1, pistol, 1);
    pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    fallout::AiPacket* dogPacket = fallout::f2jReferenceAiPacketByNum(d1->data.critter.combat.aiPacket);
    int savedDogDistance = fallout::f2jReferenceAiDistance(dogPacket);
    fallout::AiPacket* humanPacket = fallout::f2jReferenceAiPacketByNum(h1->data.critter.combat.aiPacket);
    int savedHumanDistance = fallout::f2jReferenceAiDistance(humanPacket);

    // STAY_CLOSE: the dog's whoHitMe is not the dude and the dude is more
    // than 5 tiles away, so the dog steps closer.
    fallout::f2jReferenceAiSetPacketDistance(dogPacket, (int)fallout::DISTANCE_STAY_CLOSE);
    std::printf("sc=%d\n", fallout::f2jReferenceAiPerformDistancePrefs(d1, tgt));
    dumpAnim("sc-anim");

    // CHARGE: the dog charges the target; the taunt prints the packet line.
    fallout::animationReset();
    fallout::f2jReferenceAiSetPacketDistance(dogPacket, (int)fallout::DISTANCE_CHARGE);
    std::printf("ch=%d\n", fallout::f2jReferenceAiPerformDistancePrefs(d1, tgt));
    dumpAnim("ch-anim");

    // SNIPE weak: the human (rating 10) snipes the dog (rating 70); with
    // spare AP after the shot cost the weaker actor retreats.
    fallout::animationReset();
    fallout::f2jReferenceAiSetPacketDistance(humanPacket, (int)fallout::DISTANCE_SNIPE);
    std::printf("sn-weak=%d\n", fallout::f2jReferenceAiPerformDistancePrefs(h1, d1));
    dumpAnim("sn-weak-anim");

    // SNIPE no AP: the dog has AP 3, the pistol costs 4, so the movement
    // points are spent - retreat.
    d1->data.critter.combat.ap = 3;
    fallout::animationReset();
    fallout::f2jReferenceAiSetPacketDistance(dogPacket, (int)fallout::DISTANCE_SNIPE);
    std::printf("sn-noap=%d\n", fallout::f2jReferenceAiPerformDistancePrefs(d1, tgt));
    dumpAnim("sn-noap-anim");

    // SNIPE no spare AP: the pistol costs the dog's whole AP (5), so the
    // movement points are exhausted and the else-branch retreat fires even
    // though the ratings are equal. The animation matches a direct
    // move-away.
    d1->data.critter.combat.ap = 5;
    fallout::animationReset();
    std::printf("sn-strong=%d\n", fallout::f2jReferenceAiPerformDistancePrefs(d1, tgt));
    dumpAnim("sn-strong-anim");

    fallout::f2jReferenceAiSetPacketDistance(dogPacket, savedDogDistance);
    fallout::f2jReferenceAiSetPacketDistance(humanPacket, savedHumanDistance);
    fallout::f2jReferenceAiOverCritters();
}

void sectionAiAttack()
{
    std::puts("[aiattack]");

    // AI attack head: _ai_called_shot (aim cost gate, critterCanAim,
    // called-shot roll, difficulty-scaled intelligence gate, to-hit
    // re-check) and _ai_attack (fleeing guard, rotate registration, called
    // shot, then _combat_attack). The attack runs outside combat; the
    // turn-run counter is parked so the _combat_turn_run calls inside the
    // flow return immediately.
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1, the attacker
    fallout::Object* d1 = nullptr; // dog, team 1
    fallout::Object* tgt = nullptr; // dog, team 2, the defender
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&tgt, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || tgt == nullptr) {
        std::puts("create-failed");
        return;
    }

    const int a = 38101;
    fallout::Object* roster[3] = { dude, h1, tgt };
    const int rosterDistances[3] = { 10, 0, 4 };
    for (int index = 0; index < 3; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index == 2 ? 2 : 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 10;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        if (index != 0) {
            fallout::objectSetLocation(roster[index],
                fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
                0, nullptr);
        }
    }
    dude->tile = fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[0]);
    dude->elevation = 0;

    fallout::f2jReferenceAiInfoInit(3);
    fallout::f2jReferenceAiBeginCritters(3, roster);
    fallout::animationReset();
    fallout::gCombatState = static_cast<fallout::CombatState>(
        fallout::COMBAT_STATE_IN_COMBAT | fallout::COMBAT_STATE_PLAYER_TURN);
    fallout::f2jReferenceSetTurnRunning(-1);

    // The attacker's weapon: another pistol, and the packet drives the
    // called-shot roll.
    fallout::Object* pistol = nullptr;
    fallout::objectCreateWithProtoId(&pistol, fallout::ProtoId(8));
    if (pistol == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::itemAdd(h1, pistol, 1);
    pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;

    fallout::AiPacket* humanPacket = fallout::f2jReferenceAiPacketByNum(h1->data.critter.combat.aiPacket);
    int savedCalledFreq = fallout::f2jReferenceAiCalledFreq(humanPacket);
    int savedMinToHit = fallout::f2jReferenceAiMinToHit(humanPacket);
    fallout::f2jReferenceAiSetCalledFreq(humanPacket, 1);
    fallout::f2jReferenceAiSetMinToHit(humanPacket, 40);

    // Called shot at the defender, with enough AP for aiming: the roll is
    // seeded to land on 1, so a specific location is picked. The small-guns
    // skill is raised so the aimed to-hit clears the packet minimum.
    fallout::Proto* humanProto = nullptr;
    fallout::protoGetProto(h1, &humanProto);
    int savedSkill = humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
    humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 200;
    std::printf("called-probe canaim=%d aimedcost=%d int=%d minhit=%d\n",
        fallout::critterCanAim(h1, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY) ? 1 : 0,
        fallout::weaponGetActionPointCost(h1, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY, true),
        fallout::critterGetStat(h1, fallout::STAT_INTELLIGENCE),
        fallout::f2jReferenceAiMinToHit(humanPacket));
    fallout::randomSeedPrerandom(1);
    int loc = (int)fallout::f2jReferenceAiCalledShot(h1, tgt, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    std::printf("called loc=%d\n", loc);
    // Too little AP for aiming: torso always.
    h1->data.critter.combat.ap = 1;
    fallout::randomSeedPrerandom(1);
    loc = (int)fallout::f2jReferenceAiCalledShot(h1, tgt, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    std::printf("called-noap loc=%d\n", loc);
    h1->data.critter.combat.ap = 10;
    // Weaponless attacker: no aim, torso.
    fallout::itemRemoveWithReason(h1, pistol, 1, fallout::RemoveInventoryObjectHookReason::ItemRemoved);
    fallout::randomSeedPrerandom(1);
    loc = (int)fallout::f2jReferenceAiCalledShot(h1, tgt, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    std::printf("called-unarmed loc=%d\n", loc);
    fallout::itemAdd(h1, pistol, 1);

    // _ai_attack's full path registers a rotate animation whose first-step
    // handler runs _dude_stand and turns the object; headless this stalls
    // in the art/object wiring (the same rotate is exercised by the ported
    // registrars' vectors). The vector pins the guard branch that returns
    // before any registration: a fleeing attacker refuses.
    d1->data.critter.combat.maneuver = fallout::CRITTER_MANUEVER_FLEEING;
    int atkRc = fallout::f2jReferenceAiAttack(d1, tgt, fallout::HIT_MODE_RIGHT_WEAPON_PRIMARY);
    std::printf("attack-fleeing rc=%d\n", atkRc);
    d1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;

    humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;
    fallout::f2jReferenceAiSetCalledFreq(humanPacket, savedCalledFreq);
    fallout::f2jReferenceAiSetMinToHit(humanPacket, savedMinToHit);
    fallout::gCombatState = fallout::COMBAT_STATE_PLAYER_TURN;
    fallout::f2jReferenceAiOverCritters();
}

void sectionAiDrug()
{
    std::puts("[aimdrug]");

    // AI chemistry: the drug-taking engine (drugItemTakeDrug and the
    // effect/withdrawal events) and the AI drug decision (_ai_check_drugs)
    // plus the environ search (_ai_search_environ). The section runs out of
    // combat (the retrieve path's turn-run and the rotate handlers need game
    // phase, so retrieve is deferred). The event queue is initialized so the
    // effect events register.
    fallout::queueInit();

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human (drug user)
    fallout::Object* d1 = nullptr; // dog
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr) {
        std::puts("create-failed");
        return;
    }

    // A clean corner of the map.
    const int a = 44101;
    fallout::Object* roster[2] = { h1, d1 };
    int distances[2] = { 0, 3 };
    for (int index = 0; index < 2; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 10;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        fallout::objectSetLocation(roster[index],
            fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, distances[index]), 0, nullptr);
    }

    // Items on the ground for the environ scan.
    fallout::Object* pistol = nullptr;
    fallout::Object* ammo = nullptr;
    fallout::Object* jet = nullptr;
    fallout::Object* buffout = nullptr;
    fallout::Object* mentats = nullptr;
    fallout::Object* stimpak = nullptr;
    fallout::objectCreateWithProtoId(&pistol, fallout::ProtoId(8));
    fallout::objectCreateWithProtoId(&ammo, fallout::ProtoId(29));
    fallout::objectCreateWithProtoId(&jet, fallout::ProtoId(259));
    fallout::objectCreateWithProtoId(&buffout, fallout::ProtoId(87));
    fallout::objectCreateWithProtoId(&mentats, fallout::ProtoId(53));
    fallout::objectCreateWithProtoId(&stimpak, fallout::ProtoId(40));
    if (pistol == nullptr || ammo == nullptr || jet == nullptr || buffout == nullptr
        || mentats == nullptr || stimpak == nullptr) {
        std::puts("create-failed");
        return;
    }
    fallout::objectSetLocation(pistol, 199, 0, nullptr);
    fallout::objectSetLocation(ammo, 199, 0, nullptr);
    fallout::objectSetLocation(jet, 199, 0, nullptr);
    fallout::objectSetLocation(buffout, 199, 0, nullptr);
    fallout::objectSetLocation(mentats, 199, 0, nullptr);
    fallout::objectSetLocation(stimpak, 199, 0, nullptr);

    // Drug take: examine the drug proto, then take each drug and report the
    // outcome plus the bonus stats and the queue event counts.
    auto countEvents = [](fallout::Object* owner, int type) {
        int count = 0;
        void* event = fallout::queueFindFirstEvent(owner, (fallout::EventType)type);
        while (event != nullptr) {
            count++;
            event = fallout::queueFindNextEvent(owner, (fallout::EventType)type);
        }
        return count;
    };

    fallout::randomSeedPrerandom(1);
    fallout::Proto* jetProto = nullptr;
    fallout::protoGetProto(jet, &jetProto);
    std::printf("jet-proto stat=%d,%d,%d amount=%d,%d,%d dur=%d,%d chance=%d\n",
        (int)jetProto->item.data.drug.stat[0],
        (int)jetProto->item.data.drug.stat[1],
        (int)jetProto->item.data.drug.stat[2],
        jetProto->item.data.drug.amount[0],
        jetProto->item.data.drug.amount[1],
        jetProto->item.data.drug.amount[2],
        jetProto->item.data.drug.duration1,
        jetProto->item.data.drug.duration2,
        jetProto->item.data.drug.addictionChance);

    std::printf("drug-before drg=%d wd=%d\n", countEvents(h1, 0 /* EVENT_TYPE_DRUG */), countEvents(h1, 2 /* EVENT_TYPE_WITHDRAWAL */));
    int takeRc = (int)fallout::drugItemTakeDrug(h1, jet);
    std::printf("jet rc=%d\n", takeRc);
    std::printf("drug-after drg=%d wd=%d\n", countEvents(h1, 0), countEvents(h1, 2));
    // Bonus stats for the jet proto (PERCEPTION/AGILITY/etc as per proto).
    std::printf("jet-bonus=%d,%d,%d\n",
        fallout::critterGetBonusStat(h1, jetProto->item.data.drug.stat[0]),
        fallout::critterGetBonusStat(h1, jetProto->item.data.drug.stat[1]),
        fallout::critterGetBonusStat(h1, jetProto->item.data.drug.stat[2]));

    // Taken again: the same drug is re-inserted (allowed maxActiveEffects 4).
    fallout::Proto* buffoutProto = nullptr;
    fallout::protoGetProto(buffout, &buffoutProto);
    std::printf("buffout-proto stat=%d,%d,%d amount=%d,%d,%d dur=%d,%d chance=%d\n",
        (int)buffoutProto->item.data.drug.stat[0],
        (int)buffoutProto->item.data.drug.stat[1],
        (int)buffoutProto->item.data.drug.stat[2],
        buffoutProto->item.data.drug.amount[0],
        buffoutProto->item.data.drug.amount[1],
        buffoutProto->item.data.drug.amount[2],
        buffoutProto->item.data.drug.duration1,
        buffoutProto->item.data.drug.duration2,
        buffoutProto->item.data.drug.addictionChance);
    int takeRc2 = (int)fallout::drugItemTakeDrug(h1, buffout);
    std::printf("buffout rc=%d\n", takeRc2);
    std::printf("drug-after2 drg=%d\n", countEvents(h1, 0));

    // _ai_search_environ over the ground items. The human first equips a
    // pistol (so the ammo scan can pair it) and the small-guns skill is
    // raised (so the weapon scan passes its can-use gate).
    fallout::itemAdd(h1, pistol, 1);
    pistol->flags |= fallout::OBJECT_IN_RIGHT_HAND;
    fallout::Proto* humanProto = nullptr;
    fallout::protoGetProto(h1, &humanProto);
    int savedSkill = humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS];
    humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = 200;
    std::printf("env-weapon=%d\n", fallout::f2jReferenceAiSearchEnviron(h1, (int)fallout::ITEM_TYPE_WEAPON));
    std::printf("env-ammo=%d\n", fallout::f2jReferenceAiSearchEnviron(h1, (int)fallout::ITEM_TYPE_AMMO));
    std::printf("env-drug=%d\n", fallout::f2jReferenceAiSearchEnviron(h1, (int)fallout::ITEM_TYPE_DRUG));
    humanProto->critter.data.skills[fallout::SKILL_SMALL_GUNS] = savedSkill;
}

void sectionCombatAiFlow()
{
    std::puts("[combatai]");

    // The main AI-turn decisions (_combat_ai) with the chem loop and the
    // attack flow as documented seams: the vectors hit only the branches
    // that do not enter them (no drugs, high HP, target null). The
    // danger-source and movement helpers underneath are pinned by their
    // own slices; here the decisions themselves are observed.
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1, the actor
    fallout::Object* d1 = nullptr; // dog, team 1, nearest teammate
    fallout::Object* d2 = nullptr; // dog, team 2, the enemy
    fallout::Object* far = nullptr; // dog, team 2, far enemy
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d2, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&far, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || d2 == nullptr || far == nullptr) {
        std::puts("create-failed");
        return;
    }

    const int a = 38101;
    // Roster: h1 at a, d1 at +1, d2 at +4, far at +40. Centering on h1.
    fallout::Object* roster[4] = { h1, d1, d2, far };
    const int rosterDistances[4] = { 0, 1, 4, 40 };
    for (int index = 0; index < 4; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index >= 2 ? 2 : 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 5;
        roster[index]->data.critter.combat.damageLastTurn = 0;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        fallout::objectSetLocation(roster[index],
            fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
            0, nullptr);
    }

    fallout::f2jReferenceAiInfoInit(4);
    fallout::f2jReferenceAiBeginCritters(4, roster);
    fallout::animationReset();
    // The section runs out of combat: the run registrations then skip the
    // combat-anim begin (whose first-step handler spins headless), and the
    // AI-info accessors are not required by the visited branches.

    // Packet tuning: STAY_CLOSE, max_dist 7, min_hp 40 (flee when HP drops
    // below). The human's packet field is pinned to 0 so the by-num
    // resolution is unambiguous.
    h1->data.critter.combat.aiPacket = 0;
    fallout::AiPacket* humanPacket = fallout::f2jReferenceAiPacketByNum(h1->data.critter.combat.aiPacket);
    fallout::f2jReferenceAiSetPacketDistance(humanPacket, (int)fallout::DISTANCE_STAY_CLOSE);
    fallout::f2jReferenceAiSetPacketMaxDist(humanPacket, 7);
    fallout::f2jReferenceAiSetMinHp(humanPacket, 40);

    // Flee branch: current HP (30) drops below min_hp (40), the run fires
    // and the maneuver is set. No whoHitMe edges: danger source stays null
    // so the attack flow is not entered. The run animation registers; the
    // turn tail is parked.
    h1->data.critter.hp = 30;
    d2->data.critter.combat.whoHitMe = nullptr;
    h1->data.critter.combat.whoHitMe = nullptr;
    h1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    fallout::f2jReferenceCombatAi(h1, d2);
    std::printf("flee-maneuver=%d\n",
        (h1->data.critter.combat.maneuver & fallout::CRITTER_MANUEVER_FLEEING) != 0 ? 1 : 0);
    std::printf("flee-ap=%d\n", h1->data.critter.combat.ap);
    h1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    h1->data.critter.hp = 50;

    // No enemy in the vicinity (target null, no whoHitMe, no attacker):
    // the distance prefs run, then the friendly-dead and teammate checks.
    // d1 is within the teammate threshold, so no move.

    // Friendly dead: a dead teammate is placed far; the AI walks away from
    // it (move-away registers).
    fallout::f2jReferenceAiInfoSetFriendlyDead(h1, d1);
    d1->data.critter.combat.results = fallout::DAM_DEAD;
    h1->data.critter.combat.ap = 10;
    fallout::f2jReferenceCombatAi(h1, nullptr);
    std::printf("friendlydead-cleared=%d\n",
        fallout::f2jReferenceAiInfoGetFriendlyDead(h1) == nullptr ? 1 : 0);
    h1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    d1->data.critter.combat.results = fallout::DAM_NONE;
    fallout::f2jReferenceAiInfoSetFriendlyDead(h1, nullptr);

    // Extra AP: with everything calm, the distance prefs run again (no
    // movement; the prefs only act when the triggers hit).
    h1->data.critter.combat.ap = 10;
    fallout::f2jReferenceCombatAi(h1, nullptr);
    std::printf("calm-ap=%d\n", h1->data.critter.combat.ap);

    // Join/stop decisions.
    std::printf("join-fleeing=%d\n", fallout::f2jReferenceWantToJoin(h1) ? 1 : 0);
    std::printf("stop=calm=%d\n", fallout::f2jReferenceWantToStop(h1) ? 1 : 0);
    h1->data.critter.combat.maneuver = fallout::CRITTER_MANUEVER_FLEEING;
    std::printf("stop-fleeing=%d\n", fallout::f2jReferenceWantToStop(h1) ? 1 : 0);
    h1->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
    d2->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;

    fallout::f2jReferenceAiOverCritters();
}

void sectionCombatEndAttempt()
{
    std::puts("[combatendattempt]");

    // combatAttemptEnd: the end-of-combat gate that consults the AI
    // want-to-stop/want-to-join decisions of every non-dude critter and
    // sets EXIT_REQUESTED when nobody objects. The roster and the fight
    // counters are arranged directly (the combat list is a static of
    // combat.cc, served by the probe setters).
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    fallout::Object* h1 = nullptr; // human, team 1, idle ally
    fallout::Object* d1 = nullptr; // dog, team 2, enemy
    fallout::Object* d2 = nullptr; // dog, team 2, second enemy
    fallout::objectCreateWithProtoId(&h1, fallout::ProtoId(16777376));
    fallout::objectCreateWithProtoId(&d1, fallout::ProtoId(16777558));
    fallout::objectCreateWithProtoId(&d2, fallout::ProtoId(16777558));
    if (h1 == nullptr || d1 == nullptr || d2 == nullptr) {
        std::puts("create-failed");
        return;
    }

    const int a = 38101;
    fallout::Object* roster[4] = { dude, h1, d1, d2 };
    const int rosterDistances[4] = { 10, 0, 4, 8 };
    for (int index = 0; index < 4; index++) {
        roster[index]->cid = index;
        roster[index]->data.critter.combat.team = index >= 2 ? 2 : 1;
        roster[index]->data.critter.combat.results = fallout::DAM_NONE;
        roster[index]->data.critter.combat.whoHitMe = nullptr;
        roster[index]->data.critter.combat.maneuver = fallout::CRITTER_MANEUVER_NONE;
        roster[index]->data.critter.combat.ap = 5;
        roster[index]->data.critter.combat.damageLastTurn = 0;
        roster[index]->data.critter.hp = 50;
        fallout::objectSetRotation(roster[index], fallout::ROTATION_SE, nullptr);
        fallout::objectSetLocation(roster[index],
            fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, rosterDistances[index]),
            0, nullptr);
    }
    dude->elevation = 0;
    fallout::objectSetLocation(dude, fallout::tileGetTileInDirection(a, fallout::ROTATION_SE, 10), 0, nullptr);

    fallout::f2jReferenceAiInfoInit(4);
    fallout::f2jReferenceAiBeginCritters(4, roster);
    fallout::animationReset();

    // Combat roster: 2 combatants (dude, h1) + 2 non-com (d1, d2).
    fallout::Object* combatList[2] = { dude, h1 };
    fallout::Object* noncomList[2] = { d1, d2 };
    fallout::Object* fullList[4] = { dude, h1, d1, d2 };
    fallout::f2jReferenceSetCombatRoster(fullList, 2, 2, 0);

    // Enemy in sight and not fleeing: want-to-stop is false -> no exit.
    // Give the enemies a living target (h1) so danger source finds them.
    d1->data.critter.combat.whoHitMe = h1;
    d2->data.critter.combat.whoHitMe = h1;
    int rc = fallout::f2jReferenceCombatAttemptEnd();
    std::printf("enemy-active exit=%d\n", rc);

    // Enemy dead: want-to-stop is true -> exit requested.
    d1->data.critter.combat.results = fallout::DAM_DEAD;
    d2->data.critter.combat.results = fallout::DAM_DEAD;
    rc = fallout::f2jReferenceCombatAttemptEnd();
    std::printf("enemy-dead exit=%d\n", rc);

    // Clean slate: no enemies at all -> exit requested (want-to-join loop
    // also passes).
    d1->data.critter.combat.results = fallout::DAM_NONE;
    d2->data.critter.combat.results = fallout::DAM_NONE;
    d1->data.critter.combat.whoHitMe = nullptr;
    d2->data.critter.combat.whoHitMe = nullptr;
    fallout::f2jReferenceSetCombatRoster(fullList, 1, 3, 0);
    rc = fallout::f2jReferenceCombatAttemptEnd();
    std::printf("no-enemies exit=%d\n", rc);

    fallout::gCombatState &= ~fallout::COMBAT_STATE_EXIT_REQUESTED;
    fallout::f2jReferenceAiOverCritters();
}

void sectionIsoRest()
{
    std::puts("[isorest]");

    // The isoInit tail after objectsInit: palette cycle state, interface
    // bar window, sfall elevators (no-op without config), entering spot.
    // (artInit/tileInit/objectsInit ran in [color]/[tilemath]/[partyobjects].)
    fallout::colorCycleInit();
    int ifaceRc = fallout::interfaceInit();
    fallout::elevatorsInit();
    int enterRc = fallout::mapSetEnteringLocation(-1, -1, fallout::ROTATION_INVALID);
    std::printf("iface=%d enter=%d\n", ifaceRc, enterRc);
}

void sectionMapLoad();
void sectionMapSave();
void sectionMapRegistry();
void sectionMapAreas();
void sectionWorldmapTiles();
void sectionMapNames();
void sectionStats();
void sectionCritterState();
void sectionCritterStateOp();
void sectionAnimMoveTile();
void sectionCritterPlace();
void sectionPartyPrefs();
void sectionItemsInit();
void sectionAutomap();
void sectionVm();
void sectionVmStack();
void sectionVmArith();
void sectionVmCompare();
void sectionVmControl();
void sectionVmLogic();
void sectionVmCall();
void sectionVmProc();
void sectionVmGlobals();
void sectionVmDelayed();
void sectionVmLifecycle();
void sectionVmExternal();
void sectionVmTimed();

// A fresh process for the map sections. The main run's object churn leaves
// freed objects linked (itemAdd never unlinks, _obj_inven_free frees), so
// _obj_remove_all inside mapNewMap would walk freed memory; the child boots
// its own engine, which is what the game does anyway.
void sectionMapTransition()
{
    std::puts("[maptransition]");

    // mapSetTransition: copy fields, MAP_FIRST (== MAP_RND_DESERT_1 == 0)
    // normalizes to MAP_TRANSITION (-2); combat-quit seam is false headless.
    // Null guard -> -1. MAP_INVALID (-1) is not normalized.
    fallout::MapTransition t1;
    t1.map = fallout::MAP_RND_DESERT_1;
    t1.tile = 5;
    t1.elevation = 1;
    t1.rotation = fallout::ROTATION_NE;
    int rc1 = fallout::mapSetTransition(&t1);
    std::printf("rc=%d map=%d tile=%d elev=%d rot=%d\n", rc1,
        (int)t1.map, t1.tile, t1.elevation, (int)t1.rotation);

    fallout::MapTransition t2;
    t2.map = fallout::MAP_INVALID;
    t2.tile = -1;
    t2.elevation = 0;
    t2.rotation = fallout::ROTATION_INVALID;
    int rc2 = fallout::mapSetTransition(&t2);
    std::printf("rc=%d map=%d tile=%d elev=%d rot=%d\n", rc2,
        (int)t2.map, t2.tile, t2.elevation, (int)t2.rotation);

    fallout::MapTransition t3;
    t3.map = fallout::MAP_TRANSITION;
    t3.tile = 0;
    t3.elevation = 0;
    t3.rotation = fallout::ROTATION_NE;
    int rc3 = fallout::mapSetTransition(&t3);
    std::printf("rc=%d map=%d tile=%d elev=%d rot=%d\n", rc3,
        (int)t3.map, t3.tile, t3.elevation, (int)t3.rotation);

    int rcNull = fallout::mapSetTransition(nullptr);
    std::printf("rc-null=%d\n", rcNull);
}
void sectionMapChild()
{
    std::puts("[mapnewmap]");

    setenv("SDL_VIDEODRIVER", "dummy", 1);
    SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER);

    char prog[] = "prog";
    char* argv0[] = { prog };
    fallout::settingsInit(false, 1, argv0);
    fallout::settings.screen.resolution_x = 640;
    fallout::settings.screen.resolution_y = 480;
    fallout::settings.screen.scale = 1;
    fallout::settings.screen.windowed = fallout::WindowMode::Windowed;
    fallout::settings.ui.iface_bar_mode = false;
    fallout::gProgramIsActive = true;

    fallout::gSdlWindow = SDL_CreateWindow("f2j",
        SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, 640, 480,
        SDL_WINDOW_ALLOW_HIGHDPI);
    fallout::gSdlRenderer = SDL_CreateRenderer(fallout::gSdlWindow, -1, 0);
    SDL_RenderSetLogicalSize(fallout::gSdlRenderer, 640, 480);
    fallout::gSdlTexture = SDL_CreateTexture(fallout::gSdlRenderer,
        SDL_PIXELFORMAT_RGB888, SDL_TEXTUREACCESS_STREAMING, 640, 480);
    Uint32 format = 0;
    SDL_QueryTexture(fallout::gSdlTexture, &format, nullptr, nullptr, nullptr);
    fallout::gSdlTextureSurface = SDL_CreateRGBSurfaceWithFormat(0, 640, 480,
        SDL_BITSPERPIXEL(format), format);
    int mode = fallout::_init_mode_640_480();
    fallout::directDrawSetPalette(fallout::_cmap);

    // The art lists and data\party.txt come out of the game database. Mirror
    // the main run: [db] opens master+patch, [art] mounts all three archives
    // as xbase (critter lists/FRMs live in critter.dat).
    int dbRc = fallout::dbOpen("/game-en/master.dat", "/game-en/patch000.dat");
    int xbMasterRc = fallout::xbaseOpen("/game-en/master.dat");
    int xbPatchRc = fallout::xbaseOpen("/game-en/patch000.dat");
    int xbCritterRc = fallout::xbaseOpen("/game-en/critter.dat");

    fallout::_scr_blit = testScrBlit;
    fallout::_mouse_blit = testScrBlit;
    int wmInit = fallout::windowManagerInit(testVideoInit, testVideoExit,
        fallout::WINDOW_MANAGER_INIT_FLAG_BUFFERED);
    int inputInitRc = fallout::inputInit();

    // isoInit's head, in a 64x64 window so the dude refresh inside
    // mapSetElevation finds an empty intersection and renders nothing.
    static fallout::TileData squareData[ELEVATION_COUNT];
    for (int index = 0; index < ELEVATION_COUNT; index++) {
        fallout::_square[index] = &squareData[index];
    }

    int isoWin = fallout::windowCreate(0, 0, 64, 64, static_cast<fallout::ColorWithFlags>(256), 10);
    fallout::gIsoWindow = isoWin;
    unsigned char* isoBuf = fallout::windowGetBuffer(isoWin);
    int artRc = fallout::artInit();
    int tileRc = fallout::tileInit(fallout::_square, SQUARE_GRID_WIDTH, SQUARE_GRID_HEIGHT,
        HEX_GRID_WIDTH, HEX_GRID_HEIGHT, isoBuf, 64, 64, 64, noopTileRefresh);
    int partyRc = fallout::partyMembersInit();
    unsigned char objBuf[640 * 480];
    int objectsRc = fallout::objectsInit(objBuf, 640, 480, 640);
    int gameMouseRc = fallout::gameMouseInit();
    std::printf("boot mode=%d wminit=%d input=%d win=%d buf=%d art=%d tile=%d party=%d objects=%d db=%d xb=%d,%d,%d mouse=%d\n",
        mode, wmInit, inputInitRc, isoWin, isoBuf != nullptr ? 1 : 0, artRc, tileRc,
        partyRc, objectsRc, dbRc, xbMasterRc, xbPatchRc, xbCritterRc, gameMouseRc);

    // mapNewMap: mapEdgeFree, elevation 0, center 20100, transition/header
    // reset, _obj_remove_all (NO_REMOVE objects survive: dude, egg, both
    // mouse cursors), map var frees, _square_reset (every grid frame becomes
    // the grid frame), _map_place_dude_and_mouse (dude + egg land on the
    // center tile via the objectSetLocation dude-tail), tileWindowRefresh.
    if (getenv("REFHARNESS_PROBE_MAP") != nullptr) {
        const char* probeName = getenv("REFHARNESS_PROBE_MAP");
        char probePath[64];
        snprintf(probePath, sizeof(probePath), "MAPS\\%s", probeName);
        fallout::File* stream = fallout::fileOpen(probePath, "rb");
        if (stream != nullptr) {
            int version = -1;
            char name[16] = {};
            int enteringTile = -1;
            int enteringElevation = -1;
            int enteringRotation = -1;
            int localCount = -1;
            int scriptIndex = -1;
            int flags = -1;
            int darkness = -1;
            int globalCount = -1;
            fallout::fileReadInt32(stream, &version);
            fallout::fileRead(name, 1, 16, stream);
            fallout::fileReadInt32(stream, &enteringTile);
            fallout::fileReadInt32(stream, &enteringElevation);
            fallout::fileReadInt32(stream, &enteringRotation);
            fallout::fileReadInt32(stream, &localCount);
            fallout::fileReadInt32(stream, &scriptIndex);
            fallout::fileReadInt32(stream, &flags);
            fallout::fileReadInt32(stream, &darkness);
            fallout::fileReadInt32(stream, &globalCount);
            std::printf("probe ver=%d name=%.16s tile=%d elev=%d rot=%d local=%d script=%d flags=%d dark=%d global=%d\n",
                version, name, enteringTile, enteringElevation, enteringRotation,
                localCount, scriptIndex, flags, darkness, globalCount);
            fallout::fileClose(stream);
        } else {
            std::puts("probe open-failed");
        }
        return;
    }
    fallout::mapNewMap();
    std::printf("newmap elev=%d center=%d hdr=%d,%d,%d,%d local=%d global=%d gvarlen=%d,%d\n",
        fallout::gElevation,
        fallout::gCenterTile,
        fallout::gMapHeader.version,
        fallout::gMapHeader.enteringTile,
        fallout::gMapHeader.enteringElevation,
        (int)fallout::gMapHeader.enteringRotation,
        fallout::gMapHeader.localVariablesCount,
        fallout::gMapHeader.globalVariablesCount,
        fallout::gMapGlobalVarsLength,
        fallout::gMapLocalVarsLength);

    // _square_reset rewrote every grid fid to the grid frame with old flags.
    const int* fids = fallout::_square[0]->tileFid;
    std::printf("square=%08X,%08X,%08X,%08X\n", fids[0], fids[1], fids[50], fids[99]);

    int objectCount = 0;
    for (fallout::Object* object = fallout::objectFindFirst(); object != nullptr;
         object = fallout::objectFindNext()) {
        objectCount++;
        std::printf("obj pid=%d tile=%d elev=%d rot=%d flags=%08X\n",
            object->pid,
            object->tile,
            object->elevation,
            object->rotation,
            (unsigned int)object->flags);
    }
    fallout::Object* dude = fallout::gDude;
    std::printf("objects=%d dude=%d,%d,%d light=%d flags=%08X\n",
        objectCount,
        dude != nullptr ? dude->tile : -999,
        dude != nullptr ? dude->elevation : -999,
        dude != nullptr ? dude->rotation : -999,
        dude != nullptr ? fallout::objectGetLightIntensity(dude) : -999,
        dude != nullptr ? (unsigned int)dude->flags : 0u);
    sectionMapLoad();
    sectionMapSave();
    sectionMapRegistry();
    sectionMapAreas();
    sectionMapNames();
    sectionStats();
    sectionCritterState();
    sectionCritterStateOp();
    sectionAnimMoveTile();
    sectionCritterPlace();
    sectionPartyPrefs();
    sectionItemsInit();
    sectionAutomap();
    sectionVm();
    sectionVmStack();
    sectionVmArith();
    sectionVmCompare();
    sectionVmControl();
    sectionVmLogic();
    sectionVmCall();
    sectionVmProc();
    sectionVmGlobals();
    sectionVmDelayed();
    sectionVmLifecycle();
    sectionVmExternal();
    sectionVmTimed();
    sectionWorldmapTiles();
}


// Some sections feed the engine deliberately over-long runs (the v26 graph
// stress, the v40 mouse blit), which trip ASan in an instrumented build.
// REFHARNESS_SKIP=WinMenu,Graph skips those so a trace can reach the later
// sections.
static bool sectionSkipped(const char* name)
{
    static const char* list = getenv("REFHARNESS_SKIP");

    if (list == nullptr) {
        return false;
    }

    size_t nameLength = strlen(name);

    for (const char* entry = list; *entry != '\0';) {
        const char* comma = strchr(entry, ',');
        size_t entryLength = comma != nullptr ? (size_t)(comma - entry) : strlen(entry);
        if (entryLength == nameLength && strncmp(entry, name, nameLength) == 0) {
            return true;
        }
        entry = comma != nullptr ? comma + 1 : entry + entryLength;
    }

    return false;
}

#define RUN_SECTION(name)             \
    do {                              \
        if (!sectionSkipped(#name)) { \
            section##name();          \
        }                             \
    } while (0)

void sectionObjPlaceOnTile()
{
std::puts("[objplaceontile]");

// Reuse the boot done by sectionMapChild? No - this runs in the main
// process after all previous sections. objectsInit already ran in
// [partyobjects]; calling it again would reset state. Instead verify
// objects are live, then create + connect + find + move.
if (fallout::gDude == nullptr) {
    std::puts("objects-not-initialized");
    return;
}

fallout::Object* obj = nullptr;
int rc = fallout::objectCreateWithFrmIdProtoId(&obj,
    fallout::FrmId(fallout::MiscFrameId::ScrollBlocker),
    fallout::ProtoId::Empty());
std::printf("create=%d pid=%d\n", rc, obj != nullptr ? obj->pid : -999);

    if (rc == 0 && obj != nullptr) {
        int setRc = fallout::objectSetLocation(obj, 20100, 0, nullptr);
        std::printf("set=%d tile=%d elev=%d\n", setRc,
            obj->tile, obj->elevation);

    int found = 0;
    for (fallout::Object* o = fallout::objectFindFirst(); o != nullptr;
         o = fallout::objectFindNext()) {
        if (o == obj) { found = 1; break; }
    }
    std::printf("found=%d\n", found);

    int moveRc = fallout::objectSetLocation(obj, 20517, 0, nullptr);
    std::printf("move=%d tile=%d elev=%d\n", moveRc,
        obj->tile, obj->elevation);

    found = 0;
    for (fallout::Object* o = fallout::objectFindFirst(); o != nullptr;
         o = fallout::objectFindNext()) {
        if (o == obj) { found = 1; break; }
    }
    std::printf("found-after-move=%d\n", found);

    fallout::objectDestroy(obj);
    }
}


void sectionMapStart()
{
    std::puts("[mapstart]");

    // mapSetStart: sets entering tile/elevation/rotation in the header.
    // mapGetCurrentMap: returns header index (MAP_INVALID in a fresh child).
    fallout::mapSetStart(20100, 0, fallout::ROTATION_NE);
    fallout::Map cur = fallout::mapGetCurrentMap();
    std::printf("start=%d,%d,%d current=%d\n",
        fallout::gMapHeader.enteringTile,
        fallout::gMapHeader.enteringElevation,
        (int)fallout::gMapHeader.enteringRotation,
        (int)cur);
}
void sectionMapLoad()
{
    std::puts("[mapload]");

    // isoInit's tail sets the entering spot to the -1 sentinel; the child
    // boots only the head, so mirror it before the load (otherwise
    // gEnteringTile stays -1 and tileSetCenter rejects it).
    fallout::mapSetEnteringLocation(-1, -1, fallout::ROTATION_INVALID);

    // perksInit allocates gPartyMemberPerkRanks, which lightSetAmbientIntensity
    // dereferences for the dude (perkGetRank -> perkGetRankData). It needs the
    // message-list repository (gameInit wires both).
    fallout::messageListRepositoryInit();
    fallout::perksInit();
    // wmSetupRandomEncounter reads wmMsgFile / encounter tables (worldmap).
    fallout::wmWorldMap_init();
    // scriptsExecMapEnterProc runs sfall global scripts (gameInit wires this).
    fallout::sfall_gl_scr_init();

    // Suppress the C++ map-load stderr noise: the party-script errors and
    // the non-deterministic "MAP LOAD: ... total=Nms" timer line. The printf
    // vectors go to stdout and stay deterministic.
    std::freopen("/dev/null", "w", stderr);

    char mapName[32] = "ARTEMPLE.MAP";
    int rc = fallout::mapLoadByName(mapName);
    std::printf("loadrc=%d ver=%d tile=%d elev=%d rot=%d local=%d global=%d gvarlen=%d,%d\n",
        rc,
        fallout::gMapHeader.version,
        fallout::gMapHeader.enteringTile,
        fallout::gMapHeader.enteringElevation,
        (int)fallout::gMapHeader.enteringRotation,
        fallout::gMapHeader.localVariablesCount,
        fallout::gMapHeader.globalVariablesCount,
        fallout::gMapGlobalVarsLength,
        fallout::gMapLocalVarsLength);

    const int* fids = fallout::_square[0]->tileFid;
    std::printf("square=%08X,%08X,%08X,%08X\n", fids[0], fids[1], fids[50], fids[99]);

    int objectCount = 0;
    for (fallout::Object* object = fallout::objectFindFirst(); object != nullptr;
         object = fallout::objectFindNext()) {
        objectCount++;
    }
    fallout::Object* dude = fallout::gDude;
    std::printf("objects=%d dude=%d,%d,%d\n",
        objectCount,
        dude != nullptr ? dude->tile : -999,
        dude != nullptr ? dude->elevation : -999,
        dude != nullptr ? dude->rotation : -999);
}

void sectionMapSave()
{
    std::puts("[mapsave]");

    // After mapLoadByName succeeded the header name is the bare file name.
    // mapBuildSavePath joins it under the save root (master_patches_path).
    char path[260];
    snprintf(path, sizeof(path), "%s", fallout::mapBuildSavePath(fallout::gMapHeader.name));
    std::printf("path=%s\n", path);

    // Render-boundary normalization. Three object fields reach the save file
    // as side effects of the render pass rather than of _map_save_file, so a
    // headless port cannot reproduce them byte-for-byte without the renderer:
    //
    //   obj.sx/sy  - rewritten by _obj_render_object (object.cc:5029) to the
    //                screen rect of the object, but only for objects whose
    //                art is resident in the cache (artLock) and only for the
    //                tiles it actually visits. Value depends on render order.
    //   OBJECT_SEEN- stamped by _obj_process_seen (object.cc:3136) from
    //                _obj_seen, which the render sweep fills via
    //                obj_set_seen (object.cc:1494).
    //
    // All three are cleared here, and mirrored in MapLoadTest#mapSaveVectors,
    // so the vector pins the save path itself rather than render bookkeeping.
    for (fallout::Object* object = fallout::objectFindFirst(); object != nullptr;
         object = fallout::objectFindNext()) {
        object->sx = 0;
        object->sy = 0;
        object->flags &= ~fallout::OBJECT_SEEN;
    }
    fallout::_obj_clear_seen();

    int rc = fallout::_map_save(false);
    std::printf("rc=%d\n", rc);

    fallout::File* f = fallout::fileOpen(path, "rb");
    if (f == nullptr) {
        std::printf("size=-1 crc=00000000\n");
        return;
    }
    long size = fallout::fileGetSize(f);
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
    std::printf("size=%ld crc=%08lX\n", size, crc);
    fallout::fileClose(f);
    free(data);
}

void sectionWorldmapTiles()
{
    std::puts("[worldmaptiles]");

    // wmWorldMap_init reads the game config (start position, paths) the way
    // the registry section has it, so bring it up the same way.
    fallout::configInit(&fallout::gGameConfig);
    char gameCfgPath[] = "/game-en/fallout2.cfg";
    fallout::configRead(&fallout::gGameConfig, gameCfgPath, false);

    // The temp save at the end of the init writes worldmap.dat through the
    // VFS, and with no writable base the working directory takes it - the
    // mounted repo. Step out to /tmp for the call and come back.
    char cwd[COMPAT_MAX_PATH];
    getcwd(cwd, sizeof(cwd));
    chdir("/tmp");
    int initRc = fallout::wmWorldMap_init();
    chdir(cwd);
    std::printf("init=%d\n", initRc);

    // The engine's tile parse is static, so the tile facts come out of the
    // same file through the exported config API, walked the way wmConfigInit
    // walks it: sections "Tile 0".. until art_idx misses.
    fallout::Config config;
    fallout::configInit(&config);
    std::printf("read=%d\n", fallout::configRead(&config, "data\\worldmap.txt", true) ? 1 : 0);

    int numHorizontal = 0;
    if (!fallout::configGetInt(&config, "Tile Data", "num_horizontal_tiles", &numHorizontal)) {
        std::puts("num-horizontal=missing");
        fallout::configFree(&config);
        fallout::configFree(&fallout::gGameConfig);
        return;
    }
    std::printf("num-horizontal=%d\n", numHorizontal);

    int tiles = 0;
    int lastArtIndex = -1;
    for (int tileIndex = 0; tileIndex < 9999; tileIndex++) {
        char section[40];
        snprintf(section, sizeof(section), "Tile %d", tileIndex);

        int artIndex;
        if (!fallout::configGetInt(&config, section, "art_idx", &artIndex)) {
            break;
        }
        tiles++;
        lastArtIndex = artIndex;

        if (tileIndex < 3) {
            int difficulty;
            bool hasDifficulty = fallout::configGetInt(&config, section, "encounter_difficulty", &difficulty);
            char* walkMaskName = nullptr;
            bool hasMask = fallout::configGetString(&config, section, "walk_mask_name", &walkMaskName);
            std::printf("tile %d art=%d diff=%d/%d mask=%d/%s\n",
                tileIndex, artIndex,
                hasDifficulty ? 1 : 0, hasDifficulty ? difficulty : 0,
                hasMask ? 1 : 0, hasMask ? walkMaskName : "-");
        }
    }
    std::printf("tiles=%d last-art=%d\n", tiles, lastArtIndex);

    // The bounds the area setters check: the world is num_horizontal_tiles
    // wide and tiles/num_horizontal_tiles tall, in world units.
    std::printf("bounds=%d,%d\n",
        350 * numHorizontal, 300 * (tiles / numHorizontal));

    // Tile 0 carries one subtile string per grid cell: row_column, 7 by 6.
    int subtileKeys = 0;
    for (int row = 0; row < 7; row++) {
        for (int column = 0; column < 6; column++) {
            char key[40];
            snprintf(key, sizeof(key), "%d_%d", row, column);
            char* value;
            if (fallout::configGetString(&config, "Tile 0", key, &value)) {
                subtileKeys++;
            }
        }
    }
    std::printf("tile0-subtile-keys=%d\n", subtileKeys);

    // The subtile strings of tile 0 (the first cell of the grid and the
    // last), as the engine's own parse reads them.
    char* st0 = nullptr;
    if (fallout::configGetString(&config, "Tile 0", "0_0", &st0)) {
        std::printf("tile0-st-0=%s\n", st0);
    }
    char* stLast = nullptr;
    if (fallout::configGetString(&config, "Tile 0", "6_5", &stLast)) {
        std::printf("tile0-st-last=%s\n", stLast);
    }

    // The terrain table of the world: the raw list, the first entries
    // parsed the way wmParseTerrainTypes parses them (name:difficulty,
    // comma separated), and the first random-map names the terrain's
    // config section maps.
    char* terrainTypes = nullptr;
    if (!fallout::configGetString(&config, "data", "terrain_types", &terrainTypes)) {
        std::puts("terrain-raw=<missing>");
    } else {
        std::printf("terrain-raw=%s\n", terrainTypes);
        char* pch = terrainTypes;
        for (int index = 0; index < 3; index++) {
            pch += std::strspn(pch, " ");
            size_t endPos = std::strcspn(pch, ",");
            size_t delimeterPos = std::strcspn(pch, ":");
            std::printf("terrain %d=%s/%d\n", index,
                std::string(pch, delimeterPos).c_str(), std::atoi(pch + delimeterPos + 1));
            if (pch[endPos] != ',') {
                break;
            }
            pch += endPos + 1;
        }
    }
    char* firstTerrain = nullptr;
    if (fallout::configGetString(&config, "data", "terrain_types", &firstTerrain)) {
        // The first name (before ':' or ',').
        char* colon = std::strchr(firstTerrain, ':');
        size_t nameLen = colon != nullptr ? (size_t)(colon - firstTerrain) : std::strlen(firstTerrain);
        char section[40];
        snprintf(section, sizeof(section), "Random Maps: %.*s", (int)nameLen, firstTerrain);
        for (int m = 0; m < 2; m++) {
            char key[40];
            snprintf(key, sizeof(key), "map_%02d", m);
            char* mapName = nullptr;
            if (fallout::configGetString(&config, section, key, &mapName)) {
                std::printf("terrain-map %d=%s\n", m, mapName);
            }
        }
    }

    // The encounter tables of the world: how many the config names, and
    // the header fields of the first ones (lookup name, maps, and how
    // many entries each holds - the entries themselves come with the
    // full table parse).
    int encounterTables = 0;
    for (int index = 0;; index++) {
        char section[40];
        snprintf(section, sizeof(section), "Encounter Table %d", index);
        char* lookupName = nullptr;
        if (!fallout::configGetString(&config, section, "lookup_name", &lookupName)) {
            break;
        }
        encounterTables++;
        if (index < 2) {
            std::printf("encounter %d name=%s\n", index, lookupName);
            int maps = 0;
            char* mapsStr = nullptr;
            if (fallout::configGetString(&config, section, "maps", &mapsStr)) {
                char* pch = mapsStr;
                while (*pch != '\0' && maps < 6) {
                    pch += std::strspn(pch, " ");
                    size_t endPos = std::strcspn(pch, ",");
                    std::printf("encounter %d map %d=%s\n", index, maps,
                        std::string(pch, endPos).c_str());
                    maps++;
                    if (pch[endPos] != ',') {
                        break;
                    }
                    pch += endPos + 1;
                }
            }
            std::printf("encounter %d maps=%d\n", index, maps);
            int entries = 0;
            for (;;) {
                char key[40];
                snprintf(key, sizeof(key), "enc_%02d", entries);
                char* entryStr = nullptr;
                if (!fallout::configGetString(&config, section, key, &entryStr)) {
                    break;
                }
                entries++;
            }
            std::printf("encounter %d entries=%d\n", index, entries);
            for (int e = 0; e < entries && e < 2; e++) {
                char key[40];
                snprintf(key, sizeof(key), "enc_%02d", e);
                char* entryStr = nullptr;
                if (fallout::configGetString(&config, section, key, &entryStr)) {
                    std::printf("encounter %d entry %d=%s\n", index, e, entryStr);
                }
            }
        }
    }
    std::printf("encounter-tables=%d\n", encounterTables);

    // The base encounter of the first sub-entry of the first table
    // (Raiders): the type lines, the team, and the formation block.
    {
        char section[40];
        snprintf(section, sizeof(section), "Encounter: Raiders");
        int types = 0;
        for (;;) {
            char key[40];
            snprintf(key, sizeof(key), "type_%02d", types);
            char* typeStr = nullptr;
            if (!fallout::configGetString(&config, section, key, &typeStr)) {
                break;
            }
            if (types < 2) {
                std::printf("base %d=%s\n", types, typeStr);
            }
            types++;
        }
        std::printf("base-types=%d\n", types);
        int team = 0;
        if (fallout::configGetInt(&config, section, "team_num", &team)) {
            std::printf("base-team=%d\n", team);
        }
        char* pos = nullptr;
        if (fallout::configGetString(&config, section, "position", &pos)) {
            std::printf("base-position=%s\n", pos);
        }
    }

    // With the engine's own tiles loaded, its subtile grid answers through
    // the exported lookups - the machinery mark_area_known rides on.
    if (initRc == 0 && tiles > 0) {
        int state = -1;
        int stateRc = fallout::wmSubTileGetVisitedState(100, 100, &state);
        std::printf("state 100,100 rc=%d state=%d\n", stateRc, state);

        // The marking probe sits well inside the world (the grid is
        // 1400x1500): the subtile block around 750,750, one subtile per
        // step of 50.
        std::printf("radius rc=%d\n", fallout::wmSubTileMarkRadiusVisited(750, 750, 1));
        for (int y = 700; y <= 800; y += 50) {
            for (int x = 700; x <= 800; x += 50) {
                state = -1;
                fallout::wmSubTileGetVisitedState(x, y, &state);
                std::printf("mark %d,%d=%d\n", x, y, state);
            }
        }

        // The reads the metarule rules answer, before the marking below:
        // how much the player knows of an area, and whether a map is
        // known - an entrance reads known only after the game walks into
        // the map through it. Map 3 is the Arroyo caves, map 0 a random
        // desert map no area's entrance reaches with a known state.
        std::printf("area-known-before 0=%d 1=%d invalid=%d\n",
            (int)fallout::wmAreaVisitedState((fallout::City)0),
            (int)fallout::wmAreaVisitedState((fallout::City)1),
            (int)fallout::wmAreaVisitedState(fallout::CITY_INVALID));
        std::printf("map-known-before 3=%d 0=%d -1=%d\n",
            fallout::wmMapIsKnown((fallout::Map)3) ? 1 : 0,
            fallout::wmMapIsKnown((fallout::Map)0) ? 1 : 0,
            fallout::wmMapIsKnown((fallout::Map)-1) ? 1 : 0);

        // wmMapMarkVisited goes through the areas (loaded by the init's own
        // area parse); map 3 is the Arroyo caves.
        std::printf("map-visited rc=%d\n", fallout::wmMapMarkVisited((fallout::Map)3));

        // After the marking the area reads known (a visit of an unknown
        // area lands as known, the quirk of the original), and marking
        // the map's entrance state is what makes the map itself read
        // known.
        std::printf("area-known-after 0=%d\n",
            (int)fallout::wmAreaVisitedState((fallout::City)0));
        std::printf("entrance rc=%d\n",
            fallout::wmMapMarkMapEntranceState((fallout::Map)3, -1, 1));
        std::printf("map-known-after=%d\n",
            fallout::wmMapIsKnown((fallout::Map)3) ? 1 : 0);

        // The gen-data reads the metarule rules answer: where the party
        // stands on the world, and which area that is. The init leaves
        // the area unset (the party's own movement sets it), and the
        // position is the defualt start - the config has no worldmap_x/y.
        int partyX = -1;
        int partyY = -1;
        fallout::wmGetPartyWorldPos(&partyX, &partyY);
        std::printf("party-pos=%d,%d\n", partyX, partyY);
        fallout::City curArea = fallout::CITY_INVALID;
        fallout::wmGetPartyCurArea(&curArea);
        std::printf("party-area=%d\n", (int)curArea);
    }

    fallout::configFree(&config);
    fallout::configFree(&fallout::gGameConfig);
}

void sectionMapRegistry()
{
    std::puts("[mapregistry]");

    // wmConfigInit reads gGameConfig, so bring the game config up first.
    fallout::configInit(&fallout::gGameConfig);
    char gameCfgPath[] = "/game-en/fallout2.cfg";
    fallout::configRead(&fallout::gGameConfig, gameCfgPath, false);

    int maxCount = fallout::wmMapMaxCount();
    std::printf("maxmap=%d\n", maxCount);

    // mapIsValid boundaries: MAP_FIRST is 0, one past the end is the first miss.
    std::printf("valid-min=%d valid-max=%d valid-past=%d\n",
        fallout::mapIsValid(0) ? 1 : 0,
        fallout::mapIsValid(maxCount - 1) ? 1 : 0,
        fallout::mapIsValid(maxCount) ? 1 : 0);

    // wmMapIdxToName across the first entries plus both out-of-range edges.
    char name[64];
    for (int idx = 0; idx < maxCount && idx < 3; idx++) {
        name[0] = '\0';
        int rc = fallout::wmMapIdxToName((fallout::Map)idx, name, sizeof(name));
        std::printf("idx-to-name %d rc=%d name=%s\n", idx, rc, name);
    }
    name[0] = '\0';
    int negRc = fallout::wmMapIdxToName((fallout::Map)-1, name, sizeof(name));
    std::printf("idx-to-name -1 rc=%d name-len=%d\n", negRc, (int)strlen(name));
    name[0] = '\0';
    int pastRc = fallout::wmMapIdxToName((fallout::Map)maxCount, name, sizeof(name));
    std::printf("idx-to-name %d rc=%d name-len=%d\n", maxCount, pastRc, (int)strlen(name));

    // wmMapMatchNameToIdx: extension-insensitive, lowercased, miss = -1.
    char probe[] = "ARROYO.MAP";
    std::printf("match ARROYO.MAP -> %d\n", (int)fallout::wmMapMatchNameToIdx(probe));
    char probe2[] = "ARROYO";
    std::printf("match ARROYO -> %d\n", (int)fallout::wmMapMatchNameToIdx(probe2));
    char probe3[] = "NOSUCHMAP.MAP";
    std::printf("match NOSUCHMAP.MAP -> %d\n", (int)fallout::wmMapMatchNameToIdx(probe3));
    char probe4[] = "arroyo.map";
    std::printf("match arroyo.map -> %d\n", (int)fallout::wmMapMatchNameToIdx(probe4));

    // wmMapIdxIsSaveable over the first entries.
    for (int idx = 0; idx < maxCount && idx < 3; idx++) {
        std::printf("saveable %d -> %d\n", idx, fallout::wmMapIdxIsSaveable((fallout::Map)idx) ? 1 : 0);
    }

    // wmMapCanRestHere probes the MAP_CAN_REST_ELEVATION_* bits of the
    // current map's flag word, which pins the bit layout (MAP_SAVED 0x01 ..
    // MAP_CAN_REST_ELEVATION_2 0x20) through a public entry point. A false
    // here means the slot was never registered with wmMapInit, so the probe
    // is only meaningful right after a map load; print the enum values too.
    std::printf("flag-bits saved=%d dead=%d pip=%d rest0=%d rest1=%d rest2=%d\n",
        (int)fallout::MAP_SAVED, (int)fallout::MAP_DEAD_BODIES_AGE,
        (int)fallout::MAP_PIPBOY_ACTIVE, (int)fallout::MAP_CAN_REST_ELEVATION_0,
        (int)fallout::MAP_CAN_REST_ELEVATION_1, (int)fallout::MAP_CAN_REST_ELEVATION_2);

    // Round-trip a real name so the match probe covers a non-zero index too.
    for (int idx = 0; idx < maxCount; idx++) {
        char probeName[64];
        probeName[0] = '\0';
        if (fallout::wmMapIdxToName((fallout::Map)idx, probeName, sizeof(probeName)) != 0) {
            continue;
        }
        char lower[64];
        int c = 0;
        for (; probeName[c] != '\0'; c++) {
            lower[c] = (char)((probeName[c] >= 'A' && probeName[c] <= 'Z') ? probeName[c] + 32 : probeName[c]);
        }
        lower[c] = '\0';
        std::printf("roundtrip %d %s -> %d\n", idx, lower, (int)fallout::wmMapMatchNameToIdx(lower));
        if (idx >= 2) {
            break;
        }
    }

    fallout::configFree(&fallout::gGameConfig);
}

void sectionMapAreas()
{
    std::puts("[mapareas]");

    fallout::configInit(&fallout::gGameConfig);
    char gameCfgPath[] = "/game-en/fallout2.cfg";
    fallout::configRead(&fallout::gGameConfig, gameCfgPath, false);

    int areaRc = fallout::wmAreaInit();
    int maxAreaNum = fallout::wmMaxAreaNum;
    std::printf("init=%d maxarea=%d city-first=%d city-invalid=%d\n",
        areaRc, maxAreaNum, (int)fallout::CITY_FIRST, (int)fallout::CITY_INVALID);

    // cityIsValid mirrors mapIsValid for the area table, so its boundary is
    // the same edge case worth pinning.
    std::printf("city-valid min=%d max=%d past=%d\n",
        fallout::cityIsValid(fallout::CITY_FIRST) ? 1 : 0,
        fallout::cityIsValid(maxAreaNum - 1) ? 1 : 0,
        fallout::cityIsValid(maxAreaNum) ? 1 : 0);

    // wmMatchAreaContainingMapIdx resolves a map through its area's entrance
    // table, so a successful hit also proves the entrances parsed.
    for (int mapIdx : { 0, 3, 150 }) {
        fallout::City area = fallout::CITY_INVALID;
        int rc = fallout::wmMatchAreaContainingMapIdx((fallout::Map)mapIdx, &area);
        std::printf("match %d rc=%d area=%d\n", mapIdx, rc, (int)area);
    }

    // mapAreSameArea additionally requires both maps to be saveable.
    std::printf("same 0-0=%d\n", fallout::mapAreSameArea((fallout::Map)0, (fallout::Map)0) ? 1 : 0);
    std::printf("same 0-150=%d\n", fallout::mapAreSameArea((fallout::Map)0, (fallout::Map)150) ? 1 : 0);
    std::printf("same -1-0=%d\n", fallout::mapAreSameArea((fallout::Map)-1, (fallout::Map)0) ? 1 : 0);

    // _get_map_idx_same returns the shared area id, or -1.
    std::printf("idx-same 0-0=%d\n", fallout::_get_map_idx_same((fallout::Map)0, (fallout::Map)0));
    std::printf("idx-same 0-150=%d\n", fallout::_get_map_idx_same((fallout::Map)0, (fallout::Map)150));

    // mapGetLoadedAreaId reads gMapHeader.index. Index 111 rather than the
    // table's last entry (150): the table has 151 rows but Map only names up
    // to 149, so 150 has no enum name and cannot be carried across the port.
    // 111 resolves to area 47, the same area the raw lookup above reports.
    fallout::gMapHeader.index = (fallout::Map)0;
    std::printf("loaded-area-0=%d\n", fallout::mapGetLoadedAreaId());
    fallout::gMapHeader.index = (fallout::Map)111;
    std::printf("loaded-area-111=%d\n", fallout::mapGetLoadedAreaId());

    fallout::configFree(&fallout::gGameConfig);
}

void sectionMapNames()
{
    std::puts("[mapnames]");

    // mapInit loads game\map.msg through the sfall alias; only the message
    // list matters here, so load it directly instead of running the rest of
    // mapInit (window show, tickers, mapNewMap).
    if (!fallout::messageListInit(&fallout::gMapMessageList)) {
        std::puts("msglist-init=0");
        return;
    }
    std::puts("msglist-init=1");
    std::printf("msglist-load=%d\n",
        fallout::messageListLoad(&fallout::gMapMessageList, "game\\map.msg") ? 1 : 0);

    // mapGetName indexes 200 + map * 3 + elevation.
    for (int mapIdx : { 0, 3, 26 }) {
        for (int elev = 0; elev < 3; elev++) {
            char* name = fallout::mapGetName((fallout::Map)mapIdx, elev);
            std::printf("name %d/%d = %s\n", mapIdx, elev, name != nullptr ? name : "<null>");
        }
    }
    // Out-of-range map and elevation must be null, not a lookup.
    std::printf("name -1/0 = %s\n",
        fallout::mapGetName((fallout::Map)-1, 0) != nullptr ? "nonnull" : "<null>");
    std::printf("name 0/3 = %s\n",
        fallout::mapGetName((fallout::Map)0, 3) != nullptr ? "nonnull" : "<null>");

    // mapGetCityName resolves through the area, then reads 1500 + city.
    for (int mapIdx : { 0, 3, 26 }) {
        char* city = fallout::mapGetCityName((fallout::Map)mapIdx);
        std::printf("city %d = %s\n", mapIdx, city != nullptr ? city : "<null>");
    }

    // mapDescriptionById takes the wmGetAreaIdxName path (message 1500+city).
    for (int mapIdx : { 0, 26 }) {
        char* desc = fallout::mapDescriptionById((fallout::Map)mapIdx);
        std::printf("desc %d = %s\n", mapIdx, desc != nullptr ? desc : "<null>");
    }

    fallout::messageListFree(&fallout::gMapMessageList);
}

void sectionStats()
{
    std::puts("[stats]");

    // statsInit pulls game\stat.msg through the sfall alias and fills the
    // name/description tables plus the XP thresholds from content config.
    int rc = fallout::statsInit();
    std::printf("init=%d\n", rc);

    // statGetName / statGetDescription read gStatDescriptions, which
    // statsInit filled from messages 100+stat and 200+stat.
    for (int stat : { 0, 1, 5, 36, 37 }) {
        char* name = fallout::statGetName((fallout::Stat)stat);
        char* desc = fallout::statGetDescription((fallout::Stat)stat);
        std::printf("stat %d name=%s desc=%s\n", stat,
            name != nullptr ? name : "<null>",
            desc != nullptr ? desc : "<null>");
    }
    // Out of range must be null on both sides.
    std::printf("stat -1 name=%s desc=%s\n",
        fallout::statGetName((fallout::Stat)-1) != nullptr ? "nonnull" : "<null>",
        fallout::statGetDescription((fallout::Stat)-1) != nullptr ? "nonnull" : "<null>");

    for (int pcStat : { 0, 1, 4 }) {
        char* name = fallout::pcStatGetName((fallout::PcStat)pcStat);
        std::printf("pcstat %d name=%s\n", pcStat, name != nullptr ? name : "<null>");
    }

    // XP thresholds: the level-1..N progression the content config drives.
    for (int level : { 1, 2, 3, 10, 50, 98 }) {
        std::printf("xp level %d = %d\n", level, fallout::pcGetExperienceForLevel(level));
    }
    std::printf("xp next = %d\n", fallout::pcGetExperienceForNextLevel());
}

void sectionCritterState()
{
    std::puts("[critterstate]");

    // critterInit gives the dude a critter proto so the state predicates,
    // which all require OBJ_TYPE_CRITTER, are reachable.
    fallout::critterInit();

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }
    std::printf("proto-type=%d\n", (int)fallout::ProtoId(dude).objectType());
    std::printf("hp=%d results=%d anim=%d\n",
        fallout::critterGetHitPoints(dude),
        (int)dude->data.critter.combat.results,
        (int)fallout::FrmId(dude).animationType());

    // critterIsDead has three branches: HP <= 0, the DAM_DEAD bit, and
    // neither. critterAdjustHitPoints clamps to STAT_MAXIMUM_HIT_POINTS,
    // which is 0 for a headless dude, so drive the fields directly to
    // isolate each branch.
    dude->data.critter.hp = 10;
    std::printf("dead hp=10 -> %d\n", fallout::critterIsDead(dude) ? 1 : 0);
    dude->data.critter.hp = 0;
    std::printf("dead hp=0 -> %d\n", fallout::critterIsDead(dude) ? 1 : 0);
    dude->data.critter.hp = 10;
    dude->data.critter.combat.results = (fallout::Dam)fallout::DAM_DEAD;
    std::printf("dead flag -> %d\n", fallout::critterIsDead(dude) ? 1 : 0);
    dude->data.critter.combat.results = (fallout::Dam)fallout::DAM_NONE;

    // critterIsCrippled is the DAM_CRIP bit alone.
    std::printf("crippled 0 -> %d\n", fallout::critterIsCrippled(dude) ? 1 : 0);
    dude->data.critter.combat.results |= fallout::DAM_CRIP;
    std::printf("crippled 1 -> %d\n", fallout::critterIsCrippled(dude) ? 1 : 0);
    dude->data.critter.combat.results = (fallout::Dam)(
        ((int)dude->data.critter.combat.results & ~(int)fallout::DAM_CRIP));

    // critterIsProne keys off the knockdown bits and two anim ranges
    // (20-35, 48-63); print the anim so a nonzero result is explicable.
    std::printf("prone 0 -> %d anim=%d results=%d\n",
        fallout::critterIsProne(dude) ? 1 : 0,
        (int)fallout::FrmId(dude).animationType(),
        (int)dude->data.critter.combat.results);
    dude->data.critter.combat.results |= fallout::DAM_KNOCKED_DOWN;
    std::printf("prone downed -> %d\n", fallout::critterIsProne(dude) ? 1 : 0);
    dude->data.critter.combat.results = (fallout::Dam)(
        ((int)dude->data.critter.combat.results & ~(int)fallout::DAM_KNOCKED_DOWN));

    // dudeHasState: POISONED/RADIATED are value thresholds, the rest are bits.
    for (int state : { 0, 1, 2, 3 }) {
        std::printf("dude-state %d = %d\n", state, fallout::dudeHasState((fallout::DudeState)state) ? 1 : 0);
    }
    std::printf("dude-state 9 = %d\n", fallout::dudeHasState((fallout::DudeState)9) ? 1 : 0);

    // POISONED (state 1) is a value threshold over critterGetPoison;
    // sneaking/level-up/addicted are critter flag bits that need critterInit
    // to have set them, so only the poisoned half is observable here.
    dude->data.critter.poison = 1;
    std::printf("poison 1 state1 = %d\n",
        fallout::dudeHasState((fallout::DudeState)1) ? 1 : 0);
    dude->data.critter.poison = 0;
    std::printf("poison 0 state1 = %d\n",
        fallout::dudeHasState((fallout::DudeState)1) ? 1 : 0);
}

void sectionCritterStateOp()
{
    std::puts("[critterstateop]");

    // opGetCritterState (src/interpreter_extra.cc): CRITTER_STATE_* is a
    // local enum there (NORMAL = 0, DEAD = 1, PRONE = 2). The lambda below
    // is the op body with the stack pops taken off.
    fallout::critterInit();

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    int savedFid = dude->fid;
    int savedHp = dude->data.critter.hp;
    fallout::Dam savedResults = dude->data.critter.combat.results;

    auto stateOf = [](fallout::Object* obj) {
        int state = 1; // CRITTER_STATE_DEAD
        if (fallout::ProtoId(obj).objectType() == fallout::OBJ_TYPE_CRITTER) {
            if (fallout::critterIsActive(obj)) {
                state = 0; // CRITTER_STATE_NORMAL
                fallout::AnimationType anim = fallout::FrmId(obj).animationType();
                if (anim >= fallout::ANIM_FALL_BACK_SF && anim <= fallout::ANIM_FALL_FRONT_SF) {
                    state = 2; // CRITTER_STATE_PRONE
                }
                state |= (obj->data.critter.combat.results & fallout::DAM_CRIP);
            } else {
                if (!fallout::critterIsDead(obj)) {
                    state = 2; // CRITTER_STATE_PRONE
                }
            }
        }
        return state;
    };

    auto setFidAnim = [&](int anim) {
        dude->fid = (dude->fid & ~0x00FF0000) | ((anim << 16) & 0x00FF0000);
    };

    dude->data.critter.hp = 10;
    dude->data.critter.combat.results = fallout::DAM_NONE;
    std::printf("anim=%d normal=%d\n",
        (int)fallout::FrmId(dude).animationType(), stateOf(dude));

    dude->data.critter.combat.results = fallout::DAM_CRIP;
    std::printf("crip=%d\n", stateOf(dude));
    dude->data.critter.combat.results = fallout::DAM_NONE;

    // The op's prone range is the narrow SF pair (48-49), not the whole
    // death range critterIsProne reads.
    for (int anim : { 47, 48, 49, 50, 20 }) {
        setFidAnim(anim);
        std::printf("anim=%d state=%d\n", anim, stateOf(dude));
    }

    setFidAnim(48);
    dude->data.critter.combat.results = fallout::DAM_CRIP;
    std::printf("prone-crip=%d\n", stateOf(dude));
    dude->data.critter.combat.results = fallout::DAM_NONE;
    dude->fid = savedFid;

    // Inactive without death reads prone; hp alone never inactivates.
    dude->data.critter.combat.results = fallout::DAM_KNOCKED_OUT;
    std::printf("knocked-out=%d\n", stateOf(dude));
    dude->data.critter.combat.results = fallout::DAM_LOSE_TURN;
    std::printf("lose-turn=%d\n", stateOf(dude));
    dude->data.critter.hp = 0;
    dude->data.critter.combat.results = fallout::DAM_NONE;
    std::printf("hp0-clear=%d\n", stateOf(dude));
    dude->data.critter.hp = 10;
    dude->data.critter.combat.results = fallout::DAM_DEAD;
    std::printf("dead-bit=%d\n", stateOf(dude));

    dude->data.critter.hp = savedHp;
    dude->data.critter.combat.results = savedResults;

    // A thing that is not a critter reads dead (the op logs a script
    // error on top, which is a log side channel, not a vector).
    fallout::Object* stim = nullptr;
    fallout::objectCreateWithProtoId(&stim, fallout::ProtoId(40));
    if (stim == nullptr) {
        std::puts("stim=null");
    } else {
        std::printf("stim=%d\n", stateOf(stim));
        fallout::objectDestroy(stim, nullptr);
    }
    std::printf("null=%d\n", stateOf(nullptr));
}

void sectionAnimMoveTile()
{
    std::puts("[animovetile]");

    // opAnimateMoveObjectToTile (src/interpreter_extra.cc) walks an object
    // to a tile through the animation queue. The queue is game phase, so
    // the section pins the part the port keeps: the guards, the walk/run
    // choice and the teleport target objectSetLocation computes.
    fallout::critterInit();

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    int savedTile = dude->tile;
    int savedElevation = dude->elevation;
    int savedHp = dude->data.critter.hp;
    fallout::Dam savedResults = dude->data.critter.combat.results;

    auto wouldMove = [](fallout::Object* obj, int tile) {
        if (obj == nullptr) {
            return 0;
        }
        if (tile <= -1) {
            return 0;
        }
        // scriptContextResolve needs a script registry (game phase).
        if (!fallout::critterIsActive(obj)) {
            return 0;
        }
        if (fallout::isInCombat()) {
            return 0;
        }
        return 1;
    };

    std::printf("combat=%d\n", fallout::isInCombat() ? 1 : 0);

    dude->data.critter.hp = 10;
    dude->data.critter.combat.results = fallout::DAM_NONE;

    for (int flags : { 0, 1, 0x10, 0x11 }) {
        int move = wouldMove(dude, 3015);
        int run = 0;
        if (move != 0) {
            int rest = flags;
            if ((rest & 0x10) != 0) {
                rest &= ~0x10;
            }
            run = (rest == 0) ? 0 : 1;
        }
        int rc = -1;
        int landed = dude->tile;
        if (move != 0) {
            rc = fallout::objectSetLocation(dude, 3015, savedElevation, nullptr);
            landed = dude->tile;
            fallout::objectSetLocation(dude, savedTile, savedElevation, nullptr);
        }
        std::printf("flags=%d move=%d run=%d rc=%d landed=%d\n", flags, move, run, rc, landed);
    }

    dude->data.critter.combat.results = fallout::DAM_KNOCKED_OUT;
    std::printf("inactive move=%d\n", wouldMove(dude, 3015));
    std::printf("null move=%d\n", wouldMove(nullptr, 3015));
    for (int tile : { -1, -2 }) {
        std::printf("tile=%d move=%d\n", tile, wouldMove(dude, tile));
    }

    dude->data.critter.hp = savedHp;
    dude->data.critter.combat.results = savedResults;
    std::printf("restored=%d,%d\n", dude->tile, dude->elevation);
}

void sectionCritterPlace()
{
    std::puts("[critterplace]");

    // opCritterAttemptPlacement (src/interpreter_extra.cc) settles a
    // critter near a tile through objectAttemptPlacement
    // (src/proto_instance.cc). The combat-delete tail needs the combat
    // engine (game phase), so the section runs the rest of the op body.
    fallout::critterInit();

    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    int savedTile = dude->tile;
    int savedElevation = dude->elevation;
    int savedHp = dude->data.critter.hp;
    fallout::Dam savedResults = dude->data.critter.combat.results;

    auto attempt = [](fallout::Object* critter, int tile, int elevation) {
        fallout::objectSetLocation(critter, 0, elevation, nullptr);
        return fallout::objectAttemptPlacement(critter, tile, elevation, 1);
    };

    dude->data.critter.hp = 10;
    dude->data.critter.combat.results = fallout::DAM_NONE;

    // Free tile: settles on it.
    int rc = attempt(dude, 3015, savedElevation);
    std::printf("free rc=%d tile=%d\n", rc, dude->tile);
    fallout::objectSetLocation(dude, savedTile, savedElevation, nullptr);

    // Blocked tile: a second dog stands on it.
    fallout::Object* blocker = nullptr;
    fallout::objectCreateWithProtoId(&blocker, fallout::ProtoId(16777558));
    if (blocker == nullptr) {
        std::puts("blocker=null");
    } else {
        fallout::objectSetLocation(blocker, 3015, savedElevation, nullptr);
        rc = attempt(dude, 3015, savedElevation);
        int predicted = fallout::tileGetTileInDirection(3015, (fallout::Rotation)5, 6);
        std::printf("blocked rc=%d tile=%d predicted=%d\n", rc, dude->tile, predicted);
        fallout::objectSetLocation(dude, savedTile, savedElevation, nullptr);
        fallout::objectDestroy(blocker, nullptr);
    }

    // Nowhere to settle: parks at tile 0 like the op body does, then the
    // placement itself refuses.
    rc = attempt(dude, -1, savedElevation);
    std::printf("nontile rc=%d tile=%d\n", rc, dude->tile);
    fallout::objectSetLocation(dude, savedTile, savedElevation, nullptr);

    dude->data.critter.hp = savedHp;
    dude->data.critter.combat.results = savedResults;
    std::printf("restored=%d,%d\n", dude->tile, dude->elevation);
}

void sectionPartyPrefs()
{
    std::puts("[partyprefs]");

    // partyMembersInit parses the party member descriptions out of the game
    // config; every lookup below resolves through those tables.
    fallout::partyMembersInit();
    std::printf("descs=%d\n", fallout::gPartyMemberDescriptionsLength);

    // The dude carries the first (non-player) description at index 1.
    fallout::Object* dude = fallout::gDude;
    if (dude == nullptr) {
        std::puts("dude=null");
        return;
    }

    // partyMemberGetCurrentLevel walks the proto-id table and reads the
    // parsed level_up_every; 0 means the dude is not a party member.
    std::printf("level dude=%d\n", fallout::partyMemberGetCurrentLevel(dude));

    // The supports-* predicates read one flag each out of the description.
    // The dude is not a party member, so spawn one of the first parsed
    // party member protos to reach the flag lookups.
    fallout::Object* member = nullptr;
    if (fallout::gPartyMemberProtoIds.size() > 1) {
        fallout::objectCreateWithProtoId(&member, fallout::gPartyMemberProtoIds[1]);
    }
    if (member == nullptr) {
        std::puts("member=null");
        return;
    }
    std::printf("member-pid=%d\n", (int)member->pid);
    std::printf("level member=%d\n", fallout::partyMemberGetCurrentLevel(member));

    for (int bw = 0; bw < 4; bw++) {
        std::printf("bestweapon %d = %d\n", bw,
            fallout::partyMemberSupportsBestWeapon(member, (fallout::BestWeapon)bw) ? 1 : 0);
    }
    for (int dm = 0; dm < 3; dm++) {
        std::printf("distance %d = %d\n", dm,
            fallout::partyMemberSupportsDistance(member, (fallout::DistanceMode)dm) ? 1 : 0);
    }
    for (int cu = 0; cu < 3; cu++) {
        std::printf("chemuse %d = %d\n", cu,
            fallout::partyMemberSupportsChemUse(member, (fallout::ChemUse)cu) ? 1 : 0);
    }
    for (int aw = 0; aw < 3; aw++) {
        std::printf("areaattack %d = %d\n", aw,
            fallout::partyMemberSupportsAreaAttackMode(member, (fallout::AreaAttackMode)aw) ? 1 : 0);
    }

    // An invalid enum must be rejected before the description lookup.
    std::printf("bestweapon 99 = %d\n",
        fallout::partyMemberSupportsBestWeapon(member, (fallout::BestWeapon)99) ? 1 : 0);
    std::printf("distance 99 = %d\n",
        fallout::partyMemberSupportsDistance(member, (fallout::DistanceMode)99) ? 1 : 0);
}

void sectionItemsInit()
{
    std::puts("[itemsinit]");

    // booksInitCustom reads gSfallConfig (BooksFile), which the child has not
    // brought up, so initialise it the way the game does before itemsInit.
    char prog[] = "prog";
    char* sfallArgv[] = { prog };
    fallout::sfallConfigInit(1, sfallArgv);

    // itemsInit loads game\item.msg and seeds the three content-config
    // tables (healing pids, explosives, books) that item, combat and party
    // all read back.
    int rc = fallout::itemsInit();
    std::printf("init=%d\n", rc);

    // healingItemsInit: default pids unless the content config overrides.
    std::printf("healing stimpak=%d\n",
        fallout::itemIsHealing((fallout::ProtoId)fallout::ItemProtoTypeId::Stimpak) ? 1 : 0);
    std::printf("healing super=%d\n",
        fallout::itemIsHealing((fallout::ProtoId)fallout::ItemProtoTypeId::SuperStimpak) ? 1 : 0);
    std::printf("healing powder=%d\n",
        fallout::itemIsHealing((fallout::ProtoId)fallout::ItemProtoTypeId::HealingPowder) ? 1 : 0);

    // explosionsReset: damage bounds come from the content config, then clamp.
    int lo = 0;
    int hi = 0;
    std::printf("dmg dynamite=%d",
        fallout::explosiveGetDamage((fallout::ProtoId)fallout::ItemProtoTypeId::Dynamite, &lo, &hi)
            ? 1 : 0);
    std::printf(" [%d,%d]\n", lo, hi);
    lo = 0;
    hi = 0;
    std::printf("dmg plastic=%d",
        fallout::explosiveGetDamage((fallout::ProtoId)fallout::ItemProtoTypeId::PlasticExplosives, &lo, &hi)
            ? 1 : 0);
    std::printf(" [%d,%d]\n", lo, hi);
    lo = 0;
    hi = 0;
    std::printf("dmg stimpak=%d",
        fallout::explosiveGetDamage((fallout::ProtoId)fallout::ItemProtoTypeId::Stimpak, &lo, &hi)
            ? 1 : 0);
    std::printf(" [%d,%d]\n", lo, hi);

    // booksInitVanilla seeds gBooks, which is file-static with no accessor, so
    // the book table is not observable from here; the healing and explosive
    // tables both are, through their public getters.
    std::printf("emits-light=%d\n", fallout::explosionEmitsLight() ? 1 : 0);

    // booksGetInfo is public and reads gBooks, so the table IS observable
    // through it - one book per vanilla entry, plus a non-book miss.
    int messageId = 0;
    fallout::Skill skill = fallout::SKILL_INVALID;
    for (fallout::ItemProtoTypeId book : { fallout::ItemProtoTypeId::BigBookOfScience,
             fallout::ItemProtoTypeId::DeansElectronics,
             fallout::ItemProtoTypeId::FirstAidBook,
             fallout::ItemProtoTypeId::GunsAndBullets,
             fallout::ItemProtoTypeId::ScoutHandBook }) {
        messageId = 0;
        skill = fallout::SKILL_INVALID;
        int found = fallout::booksGetInfo((fallout::ProtoId)book, &messageId, &skill) ? 1 : 0;
        std::printf("book %d msg=%d skill=%d found=%d\n", (int)book, messageId, (int)skill, found);
    }
    messageId = 0;
    skill = fallout::SKILL_INVALID;
    std::printf("book stimpak found=%d\n",
        fallout::booksGetInfo((fallout::ProtoId)fallout::ItemProtoTypeId::Stimpak, &messageId, &skill)
            ? 1 : 0);
}

void sectionAutomap()
{
    std::puts("[automap]");

    // AUTOMAP.DB ships as a loose file under data\, not inside a .dat, so the
    // relative "MAPS\AUTOMAP.DB" only resolves once data\ is mounted as a
    // directory-based xbase. gameInit does this via the master patches path.
    fallout::xbaseOpen("/game-en/data");

    // automapGetHeader opens MAPS\AUTOMAP.DB and parses version + dataSize +
    // the [160][3] offset table. Reading it is pure file work, so it works
    // headless; only the render/show half of automap.cc needs a display.
    fallout::AutomapHeader* header = nullptr;
    int rc = fallout::automapGetHeader(&header);
    std::printf("header-rc=%d\n", rc);
    if (rc != 0 || header == nullptr) {
        return;
    }

    std::printf("version=%d dataSize=%d\n", (int)header->version, header->dataSize);

    // The offset table is sparse: a 0 means "no automap for this map/elev".
    int nonZero = 0;
    for (int map = 0; map < AUTOMAP_MAP_COUNT; map++) {
        for (int elevation = 0; elevation < 3; elevation++) {
            if (header->offsets[map][elevation] != 0) {
                nonZero++;
            }
        }
    }
    std::printf("offsets=%dx%d nonZero=%d\n", AUTOMAP_MAP_COUNT, 3, nonZero);

    // First few populated entries, to pin the layout rather than just totals.
    int shown = 0;
    for (int map = 0; map < AUTOMAP_MAP_COUNT && shown < 4; map++) {
        for (int elevation = 0; elevation < 3 && shown < 4; elevation++) {
            if (header->offsets[map][elevation] != 0) {
                std::printf("offset %d/%d = %d\n", map, elevation, header->offsets[map][elevation]);
                shown++;
            }
        }
    }

    // automapEntryIsValid is file-static; the table boundary is
    // already pinned by the offsets walk above.
    // Display-map selection is pure state, no rendering needed.
    fallout::automapSetDisplayMap((fallout::Map)3, true);
    fallout::automapSetDisplayMap((fallout::Map)42, false);
    std::printf("window=%d\n", fallout::automapGetWindow());
}

// Builds a synthetic program from a raw bytecode blob and runs it, printing
// the resulting stack / frame / base / return-stack state. Same header shape
// as sectionVm: procedure count 0 at 42, opcodes from byte 54.
static void vmStackRun(const char* name, const unsigned char* code, int codeLength,
    int framePointer, int basePointer, int budget)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));
    memcpy(data + 54, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmstack-%s.int", name);
    FILE* f = fopen(path, "wb");
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }

    program->flags = 0;
    program->framePointer = framePointer;
    program->basePointer = basePointer;
    program->instructionPointer = 54;
    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d exited=%d flags=%08X frame=%d base=%d stack=%d",
        name,
        program->instructionPointer,
        program->exited ? 1 : 0,
        (unsigned int)program->flags,
        program->framePointer,
        program->basePointer,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf(" ret=%d", (int)program->returnStackValues->size());
    for (auto it = program->returnStackValues->rbegin(); it != program->returnStackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// Loads a prepared 256-byte blob, runs it from codeOffset and prints the
// resulting stack: floats as %.5f, dynamic strings by content, the rest as
// opcode:integerValue.
static void vmRunProgram(const char* name, const unsigned char* data, int codeOffset, int budget,
    bool showFrame = false)
{
    char path[64];
    snprintf(path, sizeof(path), "/tmp/vm-%s.int", name);
    FILE* f = fopen(path, "wb");
    fwrite(data, 1, 256, f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }

    program->flags = 0;
    program->framePointer = -1;
    program->basePointer = -1;
    program->instructionPointer = codeOffset;
    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X", name,
        program->instructionPointer, (unsigned int)program->flags);
    if (showFrame) {
        std::printf(" frame=%d base=%d", program->framePointer, program->basePointer);
    }
    std::printf(" stack=%d", (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        if (it->opcode == VALUE_TYPE_FLOAT) {
            std::printf(" A001:%.5f", it->floatValue);
        } else if (it->opcode == VALUE_TYPE_DYNAMIC_STRING) {
            std::printf(" 9801:%s", fallout::programGetString(program, it->opcode, it->integerValue));
        } else {
            std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
        }
    }
    std::printf("\n");

    fallout::programFree(program);
}

// One optional static string at 54, then the code.
static void vmArithRun(const char* name, const char* staticString,
    const unsigned char* code, int codeLength, int budget)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));

    int codeOffset = 54;
    if (staticString != nullptr) {
        size_t length = strlen(staticString) + 1;
        memcpy(data + 54, staticString, length);
        codeOffset = 54 + (int)length;
    }
    memcpy(data + codeOffset, code, codeLength);
    vmRunProgram(name, data, codeOffset, budget);
}

// Control-flow programs: also print the frame/base pointers.
static void vmControlRun(const char* name, const unsigned char* code, int codeLength, int budget)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));
    memcpy(data + 54, code, codeLength);
    vmRunProgram(name, data, 54, budget, true);
}

// Builds a program with one procedure and runs programExecuteProcedure.
// The top-level code (bytes 0-41) is NOOPs except POP, POP_FLAGS_EXIT at
// offset 24 - the fixed return address the compiler emits (verified against
// scripts\ABEL.int). The procedure table follows at 42, the body at 78.
static void vmCallRun(const char* name, const unsigned char* body, int bodyLength)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    for (int i = 0; i < 42; i += 2) {
        data[i] = 0x80;
        data[i + 1] = 0x00;
    }
    data[24] = 0x80;
    data[25] = 0x1A; // POP
    data[26] = 0x80;
    data[27] = 0x21; // POP_FLAGS_EXIT

    // Header fields are big-endian (stackReadInt32 reads BE).
    data[42] = 0;
    data[43] = 0;
    data[44] = 0;
    data[45] = 1; // procedure count
    data[62] = 0;
    data[63] = 0;
    data[64] = 0;
    data[65] = 78; // body offset
    // identifiers count (70) and strings count (74) stay zero.
    memcpy(data + 78, body, bodyLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vm-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, 256, f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    fallout::programExecuteProcedure(program, 0);

    std::printf("%s ip=%d flags=%08X stack=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf(" ret=%d", (int)program->returnStackValues->size());
    for (auto it = program->returnStackValues->rbegin(); it != program->returnStackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// One procedure-table row, as the compiler emits it. nameOffset indexes the
// identifiers blob, bodyOffset indexes the code region.
struct VmProc {
    int nameOffset;
    int flags;
    int argCount;
};

// Builds a full program image - procedure table, identifiers blob, static
// string table, then the top-level code - and runs programInterpret over it.
// procedureCount includes the always-present index 0 ("main"), which is the
// row opLookupStringProc skips. attachParent wires a second program as the
// child's parent so DETACH has something to detach from.
//
// Layout mirrors programCreateByPath: procedures = 42,
// identifiers = 24 * count + procedures + 4,
// staticStrings = identifiers + int32(identifiers) + 4. Every int32 header is
// big-endian (stackReadInt32 reads BE).
static void vmProcRun(const char* name, const VmProc* procs, int procedureCount,
    const char* identifiers, int identifiersLength,
    const char* stringTable, int stringTableLength,
    const unsigned char* code, int codeLength, int budget, bool attachParent)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    const int identifiersOffset = 46 + 24 * procedureCount;
    const int stringTableOffset = identifiersOffset + 4 + identifiersLength;
    const int codeOffset = stringTableOffset + 4 + stringTableLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    // Procedure bodies all point at the same NOOP pad just past the code, so
    // FETCH_PROCEDURE_ADDRESS reads a valid offset without running anything.
    const int bodyOffset = codeOffset + codeLength;
    for (int i = 0; i < procedureCount; i++) {
        writeBE32(42, procedureCount);
        const int row = 46 + 24 * i;
        writeBE32(row + 0, procs[i].nameOffset);
        writeBE32(row + 4, procs[i].flags);
        writeBE32(row + 8, 0); // time
        writeBE32(row + 12, -1); // conditionOffset
        writeBE32(row + 16, bodyOffset);
        writeBE32(row + 20, procs[i].argCount);
    }

    writeBE32(identifiersOffset, identifiersLength);
    memcpy(data + identifiersOffset + 4, identifiers, identifiersLength);
    writeBE32(stringTableOffset, stringTableLength);
    memcpy(data + stringTableOffset + 4, stringTable, stringTableLength);
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmproc-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;

    fallout::Program* parent = nullptr;
    if (attachParent) {
        parent = fallout::programCreateByPath(path);
        if (parent == nullptr) {
            std::printf("%s parent-null\n", name);
            fallout::programFree(program);
            return;
        }
        parent->flags = fallout::PROGRAM_FLAG_CHILD_CALL | fallout::PROGRAM_FLAG_CHILD_SPAWN;
        parent->child = program;
        program->parent = parent;
    }

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X frame=%d base=%d stack=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        program->framePointer, program->basePointer,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf(" ret=%d", (int)program->returnStackValues->size());
    for (auto it = program->returnStackValues->rbegin(); it != program->returnStackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    if (parent != nullptr) {
        std::printf(" parentFlags=%08X parentChild=%s",
            (unsigned int)parent->flags, parent->child == program ? "self" : "none");
    }
    std::printf("\n");

    // Break both links first: programFree walks and frees the child chain.
    if (parent != nullptr) {
        parent->child = nullptr;
        program->parent = nullptr;
        fallout::programFree(parent);
    }
    fallout::programFree(program);
}

// Runs code on a program pre-seeded with the frame
// programSetupCallWithReturnVal builds: return stack holding one resume
// address, value stack holding flags / checkWaitFunc / windowId bottom-up.
// POP_FLAGS_* needs exactly this shape - no trailing zero - so this is the
// pairing that [vmcall]'s push-return shows programSetupCall breaking.
static void vmReturnValRun(const char* name, const unsigned char* code, int codeLength, int resumeAddress)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));
    memcpy(data + 54, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmretval-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = 54;

    fallout::programReturnStackPushInteger(program, resumeAddress);
    fallout::programStackPushInteger(program, program->flags & 0xFFFF);
    fallout::programStackPushPointer(program, (void*)program->checkWaitFunc);
    fallout::programStackPushInteger(program, program->windowId);

    fallout::programInterpret(program, -1);

    std::printf("%s ip=%d flags=%08X stack=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf(" ret=%d", (int)program->returnStackValues->size());
    for (auto it = program->returnStackValues->rbegin(); it != program->returnStackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// A raw static-string table at 54 (offsets index into it), then the code.
static void vmStringCompareRun(const char* name, const char* stringTable, int tableLength,
    const unsigned char* code, int codeLength, int budget)
{
    unsigned char data[256];
    memset(data, 0, sizeof(data));
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));
    memcpy(data + 54, stringTable, tableLength);
    int codeOffset = 54 + tableLength;
    memcpy(data + codeOffset, code, codeLength);
    vmRunProgram(name, data, codeOffset, budget);
}

void sectionVmStack()
{
    std::puts("[vmstack]");

    // The stack/frame opcodes, exercised on hand-built bytecode so the
    // vectors pin the exact cell order without needing a real .int.

    // PUSH int / float, then POP / DUP / SWAP / POP.
    static const unsigned char stackMove[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0B, // PUSH int 11
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x16, // PUSH int 22
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0x80, 0x1A, // POP
        0x80, 0x1B, // DUP
        0x80, 0x18, // SWAP
        0x80, 0x1A, // POP
        0x80, 0x11, // STOP_PROGRAM
    };
    vmStackRun("move", stackMove, sizeof(stackMove), -1, -1, 32);

    // PUSH float: the opcode carries VALUE_TYPE_FLOAT (0xA001) and the
    // immediate is the IEEE-754 bits, so integerValue prints as 0x3FC00000.
    static const unsigned char pushFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0x80, 0x11, // STOP_PROGRAM
    };
    vmStackRun("pushfloat", pushFloat, sizeof(pushFloat), -1, -1, 32);

    // A value below the frame, two args, the nargs count, PUSH_BASE, two
    // locals, then POP_TO_BASE (which must stop at the frame) and POP_BASE.
    static const unsigned char frame[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x64, // PUSH int 100 (below frame)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5 (arg0)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06, // PUSH int 6 (arg1)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (nargs)
        0x80, 0x2B, // PUSH_BASE (frame = 3 - 2 = 1)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x07, // PUSH int 7 (local)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x08, // PUSH int 8 (local)
        0x80, 0x2A, // POP_TO_BASE (back to [100])
        0x80, 0x29, // POP_BASE (frame = -1)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmStackRun("frame", frame, sizeof(frame), -1, -1, 32);

    // D_TO_A / A_TO_D round-trip, then two D_TO_A, SWAPA, SET_GLOBAL.
    static const unsigned char global[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0B, // PUSH int 11
        0x80, 0x0D, // D_TO_A
        0x80, 0x0C, // A_TO_D
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x16, // PUSH int 22
        0x80, 0x0D, // D_TO_A
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x21, // PUSH int 33
        0x80, 0x0D, // D_TO_A
        0x80, 0x19, // SWAPA
        0x80, 0x2C, // SET_GLOBAL
        0x80, 0x11, // STOP_PROGRAM
    };
    vmStackRun("global", global, sizeof(global), -1, -1, 32);

    // EXIT: EXITED plus the intLib reference removal.
    static const unsigned char exit[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x0E, // EXIT
    };
    vmStackRun("exit", exit, sizeof(exit), -1, -1, 32);
}

void sectionVmArith()
{
    std::puts("[vmarith]");

    // The arithmetic / bitwise / logical operators, each leaving one result.
    // PUSH int is 0xC001 + 4 big-endian bytes; PUSH float is 0xA001 + the
    // IEEE-754 bits.

    // int + int that overflows int promotes to float (4.0e9).
    static const unsigned char addOverflow[] = {
        0xC0, 0x01, 0x77, 0x35, 0x94, 0x00, // PUSH int 2000000000
        0xC0, 0x01, 0x77, 0x35, 0x94, 0x00, // PUSH int 2000000000
        0x80, 0x39, // ADD
        0x80, 0x11, // STOP_PROGRAM
    };
    vmArithRun("add-overflow", nullptr, addOverflow, sizeof(addOverflow), 32);

    static const unsigned char addInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x03,
        0x80, 0x39,
        0x80, 0x11,
    };
    vmArithRun("add-int", nullptr, addInt, sizeof(addInt), 32);

    static const unsigned char addFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0xA0, 0x01, 0x40, 0x10, 0x00, 0x00, // PUSH float 2.25
        0x80, 0x39,
        0x80, 0x11,
    };
    vmArithRun("add-float", nullptr, addFloat, sizeof(addFloat), 32);

    // String + int concatenates: "ab" + "5".
    static const unsigned char addStrInt[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string offset 0
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
        0x80, 0x39,
        0x80, 0x11,
    };
    vmArithRun("add-str-int", "ab", addStrInt, sizeof(addStrInt), 32);

    static const unsigned char sub[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0A, // PUSH int 10
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x03, // PUSH int 3
        0x80, 0x3A, // SUB
        0x80, 0x11,
    };
    vmArithRun("sub", nullptr, sub, sizeof(sub), 32);

    static const unsigned char mul[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x07,
        0x80, 0x3B, // MUL
        0x80, 0x11,
    };
    vmArithRun("mul", nullptr, mul, sizeof(mul), 32);

    static const unsigned char div[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x14, // PUSH int 20
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x04, // PUSH int 4
        0x80, 0x3C, // DIV
        0x80, 0x11,
    };
    vmArithRun("div", nullptr, div, sizeof(div), 32);

    static const unsigned char divFloat[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0A, // PUSH int 10
        0xA0, 0x01, 0x40, 0x80, 0x00, 0x00, // PUSH float 4.0
        0x80, 0x3C, // DIV
        0x80, 0x11,
    };
    vmArithRun("div-float", nullptr, divFloat, sizeof(divFloat), 32);

    static const unsigned char mod[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x11, // PUSH int 17
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
        0x80, 0x3D, // MOD
        0x80, 0x11,
    };
    vmArithRun("mod", nullptr, mod, sizeof(mod), 32);

    static const unsigned char negateInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x07,
        0x80, 0x46, // NEGATE
        0x80, 0x11,
    };
    vmArithRun("negate-int", nullptr, negateInt, sizeof(negateInt), 32);

    static const unsigned char negateFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0x80, 0x46,
        0x80, 0x11,
    };
    vmArithRun("negate-float", nullptr, negateFloat, sizeof(negateFloat), 32);

    static const unsigned char floor[] = {
        0xA0, 0x01, 0x40, 0x70, 0x00, 0x00, // PUSH float 3.75
        0x80, 0x44, // FLOOR
        0x80, 0x11,
    };
    vmArithRun("floor", nullptr, floor, sizeof(floor), 32);

    static const unsigned char bitAnd[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0F,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x3C,
        0x80, 0x40, // BITWISE_AND
        0x80, 0x11,
    };
    vmArithRun("bitand", nullptr, bitAnd, sizeof(bitAnd), 32);

    static const unsigned char bitOr[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0F,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x30,
        0x80, 0x41, // BITWISE_OR
        0x80, 0x11,
    };
    vmArithRun("bitor", nullptr, bitOr, sizeof(bitOr), 32);

    static const unsigned char bitXor[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0F,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x3C,
        0x80, 0x42, // BITWISE_XOR
        0x80, 0x11,
    };
    vmArithRun("bitxor", nullptr, bitXor, sizeof(bitXor), 32);

    static const unsigned char bitNot[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x80, 0x43, // BITWISE_NOT
        0x80, 0x11,
    };
    vmArithRun("bitnot", nullptr, bitNot, sizeof(bitNot), 32);

    static const unsigned char logicalNot[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x45, // NOT
        0x80, 0x11,
    };
    vmArithRun("not", nullptr, logicalNot, sizeof(logicalNot), 32);
}

void sectionVmCompare()
{
    std::puts("[vmcompare]");

    // The six comparison operators: ints, floats, mixed int/float and
    // static strings. Each leaves one 0/1 result.

    static const unsigned char eqInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x33, // EQUAL
        0x80, 0x11,
    };
    vmArithRun("eq-int", nullptr, eqInt, sizeof(eqInt), 32);

    static const unsigned char eqIntNe[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06,
        0x80, 0x33, // EQUAL
        0x80, 0x11,
    };
    vmArithRun("eq-int-ne", nullptr, eqIntNe, sizeof(eqIntNe), 32);

    static const unsigned char neInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06,
        0x80, 0x34, // NOT_EQUAL
        0x80, 0x11,
    };
    vmArithRun("ne-int", nullptr, neInt, sizeof(neInt), 32);

    static const unsigned char ltInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x03,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x37, // LESS_THAN
        0x80, 0x11,
    };
    vmArithRun("lt-int", nullptr, ltInt, sizeof(ltInt), 32);

    static const unsigned char leInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x35, // LESS_THAN_EQUAL
        0x80, 0x11,
    };
    vmArithRun("le-int", nullptr, leInt, sizeof(leInt), 32);

    static const unsigned char geInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x36, // GREATER_THAN_EQUAL
        0x80, 0x11,
    };
    vmArithRun("ge-int", nullptr, geInt, sizeof(geInt), 32);

    static const unsigned char gtInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05,
        0x80, 0x38, // GREATER_THAN
        0x80, 0x11,
    };
    vmArithRun("gt-int", nullptr, gtInt, sizeof(gtInt), 32);

    static const unsigned char eqFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0x80, 0x33, // EQUAL
        0x80, 0x11,
    };
    vmArithRun("eq-float", nullptr, eqFloat, sizeof(eqFloat), 32);

    static const unsigned char ltFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0xA0, 0x01, 0x40, 0x20, 0x00, 0x00, // PUSH float 2.5
        0x80, 0x37, // LESS_THAN
        0x80, 0x11,
    };
    vmArithRun("lt-float", nullptr, ltFloat, sizeof(ltFloat), 32);

    static const unsigned char eqIntFloat[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2
        0xA0, 0x01, 0x40, 0x00, 0x00, 0x00, // PUSH float 2.0
        0x80, 0x33, // EQUAL
        0x80, 0x11,
    };
    vmArithRun("eq-int-float", nullptr, eqIntFloat, sizeof(eqIntFloat), 32);

    // String table "ab\0cd\0": offset 0 is "ab", offset 3 is "cd".
    static const char stringTable[] = { 'a', 'b', 0, 'c', 'd', 0 };

    static const unsigned char eqStr[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string "ab"
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string "ab"
        0x80, 0x33, // EQUAL
        0x80, 0x11,
    };
    vmStringCompareRun("eq-str", stringTable, sizeof(stringTable), eqStr, sizeof(eqStr), 32);

    static const unsigned char neStr[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string "ab"
        0x90, 0x01, 0x00, 0x00, 0x00, 0x03, // PUSH static string "cd"
        0x80, 0x34, // NOT_EQUAL
        0x80, 0x11,
    };
    vmStringCompareRun("ne-str", stringTable, sizeof(stringTable), neStr, sizeof(neStr), 32);

    static const unsigned char gtStr[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string "ab"
        0x90, 0x01, 0x00, 0x00, 0x00, 0x03, // PUSH static string "cd"
        0x80, 0x38, // GREATER_THAN
        0x80, 0x11,
    };
    vmStringCompareRun("gt-str", stringTable, sizeof(stringTable), gtStr, sizeof(gtStr), 32);
}

void sectionVmControl()
{
    std::puts("[vmcontrol]");

    // JUMP / IF / WHILE / STORE / FETCH. Jumps are absolute byte offsets,
    // so the blobs are laid out so the targets land on the marked opcodes.

    // JUMP to 70, skipping the PUSH int 1 at 62.
    static const unsigned char jump[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70
        0x80, 0x04, // JUMP
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (skipped)
        0x80, 0x11, // STOP_PROGRAM
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("jump", jump, sizeof(jump), 32);

    // IF pops the condition from the top, so the target goes on the stack
    // first. A truthy condition discards the target.
    static const unsigned char ifTrue[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x4C, // PUSH int 76 (target)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5 (condition)
        0x80, 0x2F, // IF
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1
        0x80, 0x11, // STOP_PROGRAM
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("if-true", ifTrue, sizeof(ifTrue), 32);

    // IF with a falsy condition jumps to the target.
    static const unsigned char ifFalse[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x4C, // PUSH int 76 (target)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (condition)
        0x80, 0x2F, // IF
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (skipped)
        0x80, 0x11, // STOP_PROGRAM
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("if-false", ifFalse, sizeof(ifFalse), 32);

    // WHILE truthy: the target is left on the stack (C's behavior).
    static const unsigned char whileTrue[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70 (target)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (condition)
        0x80, 0x30, // WHILE
        0x80, 0x11, // STOP_PROGRAM
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("while-true", whileTrue, sizeof(whileTrue), 32);

    // WHILE falsy: jumps to the target.
    static const unsigned char whileFalse[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x46, // PUSH int 70 (target)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (condition)
        0x80, 0x30, // WHILE
        0x80, 0x11, // STOP_PROGRAM
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (target)
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("while-false", whileFalse, sizeof(whileFalse), 32);

    // STORE then FETCH at frame+0. A value below the frame makes frame = 1,
    // so the 5 at index 1 is overwritten by 9 and read back.
    static const unsigned char storeFetch[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x64, // PUSH int 100 (below frame)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5 (arg0)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x06, // PUSH int 6 (arg1)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (nargs)
        0x80, 0x2B, // PUSH_BASE (frame = 3 - 2 = 1)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x09, // PUSH int 9
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (addr)
        0x80, 0x31, // STORE
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0 (addr)
        0x80, 0x32, // FETCH
        0x80, 0x11, // STOP_PROGRAM
    };
    vmControlRun("store-fetch", storeFetch, sizeof(storeFetch), 32);
}

void sectionVmLogic()
{
    std::puts("[vmlogic]");

    // AND / OR. The float cases read the raw bits (integerValue) masked with
    // 0x7FFFFFFF, which the float vectors exercise.

    static const unsigned char andInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x80, 0x3E, // AND
        0x80, 0x11,
    };
    vmArithRun("and-int", nullptr, andInt, sizeof(andInt), 32);

    static const unsigned char andIntFalse[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x80, 0x3E, // AND
        0x80, 0x11,
    };
    vmArithRun("and-int-false", nullptr, andIntFalse, sizeof(andIntFalse), 32);

    static const unsigned char orInt[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x80, 0x3F, // OR
        0x80, 0x11,
    };
    vmArithRun("or-int", nullptr, orInt, sizeof(orInt), 32);

    static const unsigned char orIntFalse[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00,
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x80, 0x3F, // OR
        0x80, 0x11,
    };
    vmArithRun("or-int-false", nullptr, orIntFalse, sizeof(orIntFalse), 32);

    // float 1.5 has non-zero bits, so AND with int 1 is 1.
    static const unsigned char andFloat[] = {
        0xA0, 0x01, 0x3F, 0xC0, 0x00, 0x00, // PUSH float 1.5
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1
        0x80, 0x3E, // AND
        0x80, 0x11,
    };
    vmArithRun("and-float", nullptr, andFloat, sizeof(andFloat), 32);

    // float 0.0 has zero bits, so OR with int 0 is 0.
    static const unsigned char orFloat[] = {
        0xA0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH float 0.0
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH int 0
        0x80, 0x3F, // OR
        0x80, 0x11,
    };
    vmArithRun("or-float", nullptr, orFloat, sizeof(orFloat), 32);
}

void sectionVmCall()
{
    std::puts("[vmcall]");

    // programExecuteProcedure on a one-procedure program. POP_EXIT returns to
    // the top-level code at 24 and stamps PROGRAM_FLAG_FINISHED, so the VM
    // stops there with the call frame still on the value stack.
    //
    // POP_RETURN does not stamp FINISHED, so the VM keeps running into the
    // top-level POP_FLAGS_EXIT at 26 - and that fatals. programSetupCall
    // pushes flags/checkWaitFunc/windowId and then one extra zero for the
    // callee's return-value slot, but opPopFlags pops only three, so the
    // windowId read steals the extra zero and the flags read lands on the
    // checkWaitFunc PTR cell: programStackPopInteger faults, the longjmp stamps
    // EXITED|FATAL_ERROR, and the IP stays at 28 (the opcode was already
    // fetched). POP_FLAGS_* pairs with programSetupCallWithReturnVal, which has
    // no trailing zero.

    static const unsigned char pushExit[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
        0x80, 0x1D, // POP_EXIT
    };
    vmCallRun("push-exit", pushExit, sizeof(pushExit));

    static const unsigned char pushReturn[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH int 5
        0x80, 0x1C, // POP_RETURN
    };
    vmCallRun("push-return", pushReturn, sizeof(pushReturn));
}

// Global variables (FETCH_GLOBAL / STORE_GLOBAL) and DUMP. These index the
// value stack from basePointer, which SET_GLOBAL snapshots as the stack size,
// so a self-contained program needs no call frame: push the globals, snapshot,
// write through them and read them back.
static void vmGlobalsRun(const char* name, const unsigned char* code, int codeLength, int budget)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    // One procedure row so the blob parses; bodyOffset points at the NOOP pad.
    const int identifiersOffset = 46 + 24;
    const int stringTableOffset = identifiersOffset + 4 + 4; // "proc" + NUL
    const int codeOffset = stringTableOffset + 4 + 4; // "str" + NUL
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    static const char identifiers[] = "proc";
    static const char strings[] = "str";

    writeBE32(42, 1);
    writeBE32(46 + 0, 4);  // nameOffset
    writeBE32(46 + 4, 0);  // flags
    writeBE32(46 + 8, 0);  // time
    writeBE32(46 + 12, -1); // conditionOffset
    writeBE32(46 + 16, bodyOffset);
    writeBE32(46 + 20, 0); // argCount

    writeBE32(identifiersOffset, (int)sizeof(identifiers));
    memcpy(data + identifiersOffset + 4, identifiers, sizeof(identifiers));
    writeBE32(stringTableOffset, (int)sizeof(strings));
    memcpy(data + stringTableOffset + 4, strings, sizeof(strings));
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmglobals-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X frame=%d base=%d stack=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        program->framePointer, program->basePointer,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// Delayed/conditional-call opcodes that do not read the clock: CALL_WHEN,
// CANCEL and CANCEL_ALL all just rewrite procedure-table fields, so the
// procedure rows are printed afterwards to prove the writes landed where C
// put them. CALL_AT and WAIT are held back - they fold getInterpreterTime()
// into the stored value and there is no way to pin the timer from outside.
static void vmDelayedRun(const char* name, const unsigned char* code, int codeLength, int budget,
    int procedureCount, const int (*initialFlags)[3])
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    const int identifiersOffset = 46 + 24 * procedureCount;
    static const char identifiers[] = "main\0one\0two\0three";
    const int stringTableOffset = identifiersOffset + 4 + sizeof(identifiers);
    static const char strings[] = "s";
    const int codeOffset = stringTableOffset + 4 + sizeof(strings);
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    writeBE32(42, procedureCount);
    for (int i = 0; i < procedureCount; i++) {
        const int row = 46 + 24 * i;
        writeBE32(row + 0, 4 + i * 4); // nameOffset into the identifier blob
        writeBE32(row + 4, initialFlags[i][0]);
        writeBE32(row + 8, initialFlags[i][1]);
        writeBE32(row + 12, initialFlags[i][2]);
        writeBE32(row + 16, bodyOffset);
        writeBE32(row + 20, 0);
    }

    writeBE32(identifiersOffset, (int)sizeof(identifiers));
    memcpy(data + identifiersOffset + 4, identifiers, sizeof(identifiers));
    writeBE32(stringTableOffset, (int)sizeof(strings));
    memcpy(data + stringTableOffset + 4, strings, sizeof(strings));
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmdelayed-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X stack=%d procs=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        (int)program->stackValues->size(), (int)procedureCount);
    for (int i = 0; i < procedureCount; i++) {
        const int row = 46 + 24 * i;
        std::printf(" [%d]f=%X t=%d c=%d", i,
            (unsigned int)fallout::stackReadInt32(program->data, row + 4),
            fallout::stackReadInt32(program->data, row + 8),
            fallout::stackReadInt32(program->data, row + 12));
    }
    std::printf("\n");

    fallout::programFree(program);
}

// Program lifecycle: CALLSTART / SPAWN / FORK / EXEC. All four reach
// runScript, whose name mangler is the identity function, so the pushed
// string is a path - the harness points it at a child script it writes.
//
// The script names come out of the static string table, which is why the
// PUSH here is 0x9001 and not the 0xC001 the other sections use. programGetNextOpcode
// reads a 16-bit opcode, the VM requires bit 0x80 of the high byte, and
// dispatches on `opcode & 0x3FF` - so 0x9001 passes validation and still
// lands on opPush. opPush copies the opcode into the pushed value's type,
// which is why VALUE_TYPE_INT is 0xC001 and VALUE_TYPE_STRING is 0x9001:
// the opcode's high byte IS the value type. programGetString then resolves
// 0x9001 to `staticStrings + 4 + operand`.
static void vmLifeRun(const char* name, const char* childPath, bool withChild,
    const unsigned char* code, int codeLength, int budget, bool attachParent)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    const int identifiersOffset = 46 + 24;
    static const char identifiers[] = "main";
    // Static strings: the child path at offset 0, a missing path after it.
    static const char missingPath[] = "/tmp/vmlife-missing.int";
    unsigned char strings[128];
    memset(strings, 0, sizeof(strings));
    const int childStringOffset = 0;
    const int missingStringOffset = (int)strlen(childPath) + 1;
    memcpy(strings + childStringOffset, childPath, strlen(childPath));
    memcpy(strings + missingStringOffset, missingPath, sizeof(missingPath));
    const int stringsLength = missingStringOffset + (int)sizeof(missingPath);

    const int stringTableOffset = identifiersOffset + 4 + sizeof(identifiers);
    const int codeOffset = stringTableOffset + 4 + stringsLength;
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    if (withChild) {
        // The child only needs a parseable table; its bodyOffset points just
        // past the identifiers blob, which is all zero bytes (NOOP), so
        // runScript's 24-instruction head start does nothing and returns.
        unsigned char child[1024];
        memset(child, 0, sizeof(child));
        const int childProc = 46;
        const int childBody = 46 + 24 + 4 + (int)sizeof(identifiers);
        auto writeBE = [](unsigned char* buf, int offset, int value) {
            buf[offset] = (unsigned char)(value >> 24);
            buf[offset + 1] = (unsigned char)(value >> 16);
            buf[offset + 2] = (unsigned char)(value >> 8);
            buf[offset + 3] = (unsigned char)value;
        };
        writeBE(child, 42, 1);
        writeBE(child, childProc + 0, 4);
        writeBE(child, childProc + 16, childBody);
        FILE* cf = fopen(childPath, "wb");
        if (cf != nullptr) {
            fwrite(child, 1, sizeof(child), cf);
            fclose(cf);
        }
    }

    writeBE32(42, 1);
    writeBE32(46 + 0, 4);
    writeBE32(46 + 16, bodyOffset);
    writeBE32(46 + 20, 0);

    writeBE32(identifiersOffset, (int)sizeof(identifiers));
    memcpy(data + identifiersOffset + 4, identifiers, sizeof(identifiers));
    writeBE32(stringTableOffset, stringsLength);
    memcpy(data + stringTableOffset + 4, strings, stringsLength);
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmlife-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;
    program->windowId = 77;

    fallout::Program* parent = nullptr;
    if (attachParent) {
        parent = fallout::programCreateByPath(path);
        parent->flags = fallout::PROGRAM_FLAG_CHILD_CALL | fallout::PROGRAM_FLAG_CHILD_SPAWN;
        parent->child = program;
        program->parent = parent;
    }

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X window=%d child=%s childParent=%s childWindow=%d exited=%d",
        name, program->instructionPointer, (unsigned int)program->flags, program->windowId,
        program->child != nullptr ? "yes" : "none",
        (program->child != nullptr && program->child->parent == program) ? "self" : "other",
        program->child != nullptr ? program->child->windowId : -1,
        program->exited ? 1 : 0);

    if (parent != nullptr) {
        std::printf(" parentFlags=%08X parentChild=%s", (unsigned int)parent->flags,
            parent->child != nullptr ? "yes" : "none");
    }
    std::printf("\n");

    // Free the child first so the parent keeps a consistent link while doing it.
    if (program->child != nullptr) {
        fallout::Program* child = program->child;
        program->child = nullptr;
        child->parent = nullptr;
        fallout::programFree(child);
    }
    if (parent != nullptr) {
        program->parent = nullptr;
        fallout::programFree(parent);
    }
    fallout::programFree(program);
}

// Emits PUSH_STATIC_STRING (opcode 0x9001) of a static-string offset.
static int pushStaticString(unsigned char* out, int offset)
{
    out[0] = 0x90; out[1] = 0x01;
    out[2] = (unsigned char)(offset >> 24);
    out[3] = (unsigned char)(offset >> 16);
    out[4] = (unsigned char)(offset >> 8);
    out[5] = (unsigned char)offset;
    return 6;
}

// External variable / procedure registry (src/export.cc). EXPORT_VARIABLE and
// EXPORT_PROCEDURE publish names into the two shared 1013-slot tables that
// FETCH_EXTERNAL and STORE_EXTERNAL read back. The popped operand is an
// identifier-table OFFSET, not the name.
//
// Two properties of the registry shape the harness. The tables are global and
// never reset, so entries leak from one case into the next - the cases are
// therefore ordered with the genuinely-missing ones first, before anything has
// been exported. And externalVariableCreate compares program->name, not the
// Program pointer, so every case must load from the SAME path or a second
// EXPORT_VARIABLE of a live name would collide with a different owner.
static void vmExternRun(const char* caseName, const char* path, const char* identifiers,
    int identifiersLength, const unsigned char* code, int codeLength, int budget)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    const int identifiersOffset = 46 + 24;
    static const char strings[] = "hello";
    const int stringTableOffset = identifiersOffset + 4 + identifiersLength;
    const int codeOffset = stringTableOffset + 4 + sizeof(strings);
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    writeBE32(42, 1);
    writeBE32(46 + 0, 4);
    writeBE32(46 + 16, bodyOffset);
    writeBE32(46 + 20, 0);

    writeBE32(identifiersOffset, identifiersLength);
    memcpy(data + identifiersOffset + 4, identifiers, identifiersLength);
    writeBE32(stringTableOffset, (int)sizeof(strings));
    memcpy(data + stringTableOffset + 4, strings, sizeof(strings));
    memcpy(data + codeOffset, code, codeLength);

    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", caseName);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", caseName);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;

    fallout::programInterpret(program, budget);

    std::printf("%s ip=%d flags=%08X stack=%d", caseName,
        program->instructionPointer, (unsigned int)program->flags,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    fallout::programFree(program);
}

// The "return from an external call" opcodes (POP_FLAGS_*_EXTERN). They
// restore the caller's state from the callee's return stack.
//
// The return stack is built here by hand, in the order the CONSUMERS pop it:
// caller, checkWaitFunc, flags, returnAddress. That is NOT the order
// setupExternalCallWithReturnVal pushes it - C pushes returnAddress last, so
// the real producer leaves the consumers reading the wrong slots. Since
// PROCEDURE_FLAG_IMPORTED is never set anywhere, the real path is dead, and
// this is the only way to exercise the opcodes' own logic.
//
// The callee's VALUE stack carries the frame opPopFlags restores: windowId on
// top, then checkWaitFunc, then flags.
static void vmExternCallRun(const char* name, const unsigned char* code, int codeLength,
    int budget, bool withValue, int value)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    static const char identifiers[] = "main";
    const int identifiersOffset = 46 + 24;
    static const char strings[] = "s";
    const int stringTableOffset = identifiersOffset + 4 + sizeof(identifiers);
    const int codeOffset = stringTableOffset + 4 + sizeof(strings);
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    writeBE32(42, 1);
    writeBE32(46 + 0, 4);
    writeBE32(46 + 16, bodyOffset);
    writeBE32(46 + 20, 0);

    writeBE32(identifiersOffset, (int)sizeof(identifiers));
    memcpy(data + identifiersOffset + 4, identifiers, sizeof(identifiers));
    writeBE32(stringTableOffset, (int)sizeof(strings));
    memcpy(data + stringTableOffset + 4, strings, sizeof(strings));
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmexterncall-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* callee = fallout::programCreateByPath(path);
    fallout::Program* caller = fallout::programCreateByPath(path);
    if (callee == nullptr || caller == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    callee->flags = 0;
    callee->instructionPointer = codeOffset;
    callee->windowId = 5;
    caller->flags = 0;
    caller->instructionPointer = 1234;
    caller->windowId = 9;

    // Callee value stack: [flags, checkWaitFunc, windowId] with windowId on top.
    // The flags must carry no bit the VM loop breaks on (EXITED, FATAL_ERROR,
    // STOPPED, CHILD_CALL, FINISHED, CHILD_SPAWN), otherwise the loop stops
    // before the opcode's effect on the instruction pointer can be seen.
    // PROGRAM_FLAG_RUNNING is the one flag that is inert here.
    fallout::programStackPushInteger(callee, 0x0002);
    fallout::programStackPushPointer(callee, nullptr);
    fallout::programStackPushInteger(callee, 77); // windowId
    if (withValue) {
        // The return value is popped BEFORE opPopFlags, so it goes on top.
        fallout::programStackPushInteger(callee, value);
    }

    // Callee return stack, in the order the consumers pop it. The return
    // address points at the STOP_PROGRAM that follows the opcode in the code,
    // so an opcode that does NOT set FINISHED resumes there and runs it -
    // which is how the two variants are told apart.
    fallout::programReturnStackPushInteger(callee, codeOffset + 2); // returnAddress
    fallout::programReturnStackPushInteger(callee, 0x0123); // caller's flags
    fallout::programReturnStackPushPointer(callee, nullptr); // caller's checkWaitFunc
    fallout::programReturnStackPushPointer(callee, caller);

    // The caller's own return stack holds the address the caller resumes at.
    fallout::programReturnStackPushInteger(caller, 2222);

    fallout::programInterpret(callee, budget);

    std::printf("%s ip=%d flags=%08X window=%d stack=%d ret=%d callerIp=%d callerFlags=%08X callerStack=%d",
        name, callee->instructionPointer, (unsigned int)callee->flags, callee->windowId,
        (int)callee->stackValues->size(), (int)callee->returnStackValues->size(),
        caller->instructionPointer,
        (unsigned int)caller->flags, (int)caller->stackValues->size());
    for (auto it = caller->stackValues->rbegin(); it != caller->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }
    std::printf("\n");

    caller->child = nullptr;
    callee->parent = nullptr;
    fallout::programFree(caller);
    fallout::programFree(callee);
}

// CALL_AT and WAIT, the two opcodes that fold the interpreter clock into a
// stored value.
//
// getInterpreterTime() is 1000 * getTicks() / 1000, and getTicks is the SDL
// millisecond clock, so the absolute value is wall-clock and cannot go into a
// golden file. What CAN is pinned:
//
//   CALL_AT stores 1000 * delay + now in the procedure's time field, where now
//   is the clock reading taken while the opcode ran. The harness brackets the
//   call with its own clock readings and checks that now falls inside the
//   bracket - which pins the delay term exactly and still proves the clock
//   was consulted once.
//
//   WAIT stores waitEnd = waitStart + duration, so the SPAN is exact even
//   though both endpoints are wall-clock.
static void vmTimedRun(const char* name, const unsigned char* code, int codeLength, int budget,
    int procedureCount, int waitProc, bool hasWait)
{
    unsigned char data[1024];
    memset(data, 0, sizeof(data));

    static const char identifiers[] = "main\0one";
    const int identifiersOffset = 46 + 24 * procedureCount;
    static const char strings[] = "s";
    const int stringTableOffset = identifiersOffset + 4 + sizeof(identifiers);
    const int codeOffset = stringTableOffset + 4 + sizeof(strings);
    const int bodyOffset = codeOffset + codeLength;

    auto writeBE32 = [&](int offset, int value) {
        data[offset] = (unsigned char)(value >> 24);
        data[offset + 1] = (unsigned char)(value >> 16);
        data[offset + 2] = (unsigned char)(value >> 8);
        data[offset + 3] = (unsigned char)value;
    };

    writeBE32(42, procedureCount);
    for (int i = 0; i < procedureCount; i++) {
        const int row = 46 + 24 * i;
        writeBE32(row + 0, 4 + i * 4);
        writeBE32(row + 4, 0);
        writeBE32(row + 8, 0);
        writeBE32(row + 12, -1);
        writeBE32(row + 16, bodyOffset);
        writeBE32(row + 20, 0);
    }

    writeBE32(identifiersOffset, (int)sizeof(identifiers));
    memcpy(data + identifiersOffset + 4, identifiers, sizeof(identifiers));
    writeBE32(stringTableOffset, (int)sizeof(strings));
    memcpy(data + stringTableOffset + 4, strings, sizeof(strings));
    memcpy(data + codeOffset, code, codeLength);

    char path[64];
    snprintf(path, sizeof(path), "/tmp/vmtimed-%s.int", name);
    FILE* f = fopen(path, "wb");
    if (f == nullptr) {
        std::printf("%s fopen-failed\n", name);
        return;
    }
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::printf("%s create=null\n", name);
        return;
    }
    program->flags = 0;
    program->instructionPointer = codeOffset;

    const unsigned int t0 = fallout::getTicks();
    fallout::programInterpret(program, budget);
    const unsigned int t1 = fallout::getTicks();

    // 1000 * getTicks() / 1000 in unsigned 32-bit arithmetic, same as
    // getInterpreterTime.
    auto interpreterTime = [](unsigned int ticks) {
        return (1000u * ticks / 1000u);
    };

    std::printf("%s ip=%d flags=%08X stack=%d", name,
        program->instructionPointer, (unsigned int)program->flags,
        (int)program->stackValues->size());
    for (auto it = program->stackValues->rbegin(); it != program->stackValues->rend(); ++it) {
        std::printf(" %04X:%d", (unsigned int)it->opcode, it->integerValue);
    }

    if (waitProc >= 0) {
        // The scheduled procedure's time field holds 1000 * delay + now. The
        // absolute value is wall-clock and cannot go into a golden file, so
        // the vector records the delay term and checks that the embedded
        // clock reading fell inside the bracket around the call. That still
        // has teeth: dropping the delay term, or adding it twice, pushes the
        // embedded reading outside the bracket.
        const int row = 46 + 24 * waitProc;
        const int stored = fallout::stackReadInt32(program->data, row + 8);
        const int delayTerm = 1000 * 2; // the 1000 * delay term CALL_AT adds
        const unsigned int now = (unsigned int)(stored - delayTerm);
        std::printf(" procFlags=%X delayTerm=%d nowInWindow=%s",
            (unsigned int)fallout::stackReadInt32(program->data, row + 4),
            delayTerm,
            (now >= interpreterTime(t0) && now <= interpreterTime(t1)) ? "yes" : "no");
    }

    if (hasWait) {
        // waitStart / waitEnd persist even after the VM clears checkWaitFunc,
        // so the span is observable in both wait cases.
        std::printf(" waitSpan=%d waiting=%s",
            (int)(program->waitEnd - program->waitStart),
            (program->flags & fallout::PROGRAM_IS_WAITING) != 0 ? "yes" : "no");
    }

    std::printf("\n");

    fallout::programFree(program);
}

void sectionVmTimed()
{
    std::puts("[vmtimed]");

    unsigned char code[64];
    int n;

    // CALL_AT: push the delay, then the procedure index (it is popped first).
    n = 0;
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0; code[n++] = 0; code[n++] = 0; code[n++] = 2;
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0; code[n++] = 0; code[n++] = 0; code[n++] = 1;
    code[n++] = 0x80; code[n++] = 0x06; // CALL_AT
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmTimedRun("call-at", code, n, 32, 2, 1, false);

    // WAIT with a positive duration. checkWait returns TRUE while the wait is
    // still in the future, and the VM loop keeps waiting only when it returns
    // FALSE - so a positive wait is skipped entirely and the program carries
    // on to the next instruction.
    n = 0;
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0; code[n++] = 0; code[n++] = 0x13; code[n++] = 0x88;
    code[n++] = 0x80; code[n++] = 0x47; // WAIT 5000
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0; code[n++] = 0; code[n++] = 0; code[n++] = 0x2A;
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmTimedRun("wait-positive", code, n, 32, 2, -1, true);

    // WAIT with a negative duration puts waitEnd in the past, so checkWait
    // returns FALSE and the VM loop spins on the waiting state until the
    // instruction budget runs out. The code after the WAIT never runs.
    n = 0;
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0xFF; code[n++] = 0xFF; code[n++] = 0xFF; code[n++] = 0xFF;
    code[n++] = 0x80; code[n++] = 0x47; // WAIT -1
    code[n++] = 0xC0; code[n++] = 0x01; code[n++] = 0; code[n++] = 0; code[n++] = 0; code[n++] = 0x2A;
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmTimedRun("wait-negative", code, n, 32, 2, -1, true);
}

void sectionVmExternal()
{
    std::puts("[vmexternal]");

    // Identifier offsets. programGetIdentifier is `identifiers + offset` with
    // NO +4, and `identifiers` points AT the block's own length field - so the
    // blob bytes start at offset 4 and "main\0shared\0thing" puts "main" at 4,
    // "shared" at 9 and "thing" at 16.
    static const char identifiers[] = "main\0shared\0thing";
    const int sharedOffset = 9;
    const int thingOffset = 16;

    // One path for every case, so program->name matches across them.
    static const char path[] = "/tmp/vmextern-shared.int";

    auto pushInt = [](unsigned char* out, int& n, int value) {
        out[n++] = 0xC0; out[n++] = 0x01;
        out[n++] = (unsigned char)(value >> 24);
        out[n++] = (unsigned char)(value >> 16);
        out[n++] = (unsigned char)(value >> 8);
        out[n++] = (unsigned char)value;
    };

    unsigned char code[96];
    int n;

    // Must run first: the table is empty, so fetching an unexported name is
    // fatal. Everything below depends on this ordering.
    n = 0;
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("fetch-missing", path, identifiers, (int)sizeof(identifiers), code, n, 32);

    n = 0;
    pushInt(code, n, 42);
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x15; // STORE_EXTERNAL
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("store-missing", path, identifiers, (int)sizeof(identifiers), code, n, 32);

    // export, store 42, fetch it back.
    n = 0;
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x16; // EXPORT_VARIABLE
    pushInt(code, n, 42);
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x15; // STORE_EXTERNAL
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("export-store-fetch", path, identifiers, (int)sizeof(identifiers), code, n, 32);

    // A second name lives beside it in the same table, untouched by the first.
    n = 0;
    pushInt(code, n, thingOffset);
    code[n++] = 0x80; code[n++] = 0x16; // EXPORT_VARIABLE thing
    pushInt(code, n, 7);
    pushInt(code, n, thingOffset);
    code[n++] = 0x80; code[n++] = 0x15; // STORE_EXTERNAL thing = 7
    pushInt(code, n, thingOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL thing
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL shared (still 42)
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("two-names", path, identifiers, (int)sizeof(identifiers), code, n, 32);

    // Re-exporting from the same programName is allowed and resets to 0.
    n = 0;
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x16; // EXPORT_VARIABLE shared (again)
    pushInt(code, n, sharedOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("re-export-resets", path, identifiers, (int)sizeof(identifiers), code, n, 32);

    // strncpy(name, id, 31) into char[32] truncates the STORED name, but the
    // lookup compares the stored name against the FULL identifier. A name
    // longer than 31 characters is therefore exported successfully and can
    // never be found again - exporting and then fetching it is fatal.
    static const char longIdentifiers[] = "main\0abcdefghijklmnopqrstuvwxyz0123456789";
    const int longOffset = 9;
    n = 0;
    pushInt(code, n, longOffset);
    code[n++] = 0x80; code[n++] = 0x16; // EXPORT_VARIABLE
    pushInt(code, n, longOffset);
    code[n++] = 0x80; code[n++] = 0x14; // FETCH_EXTERNAL
    code[n++] = 0x80; code[n++] = 0x11;
    vmExternRun("long-name-unfindable", path, longIdentifiers, (int)sizeof(longIdentifiers), code, n, 32);

    // The four external-call return opcodes.
    unsigned char xcode[32];
    int xn;

    xn = 0;
    xcode[xn++] = 0x80; xcode[xn++] = 0x22; // POP_FLAGS_RETURN_EXTERN
    xcode[xn++] = 0x80; xcode[xn++] = 0x11; // STOP_PROGRAM
    vmExternCallRun("return-extern", xcode, xn, 32, false, 0);

    xn = 0;
    xcode[xn++] = 0x80; xcode[xn++] = 0x23; // POP_FLAGS_EXIT_EXTERN
    xcode[xn++] = 0x80; xcode[xn++] = 0x11;
    vmExternCallRun("exit-extern", xcode, xn, 32, false, 0);

    // The return value is popped before opPopFlags, so it sits on top of the
    // frame. It ends up on the CALLER's stack.
    xn = 0;
    xcode[xn++] = 0x80; xcode[xn++] = 0x24; // POP_FLAGS_RETURN_VAL_EXTERN
    xcode[xn++] = 0x80; xcode[xn++] = 0x11;
    vmExternCallRun("return-val-extern", xcode, xn, 32, true, 55);

    // This one puts the value back on the CALLEE's stack instead.
    xn = 0;
    xcode[xn++] = 0x80; xcode[xn++] = 0x26; // POP_FLAGS_RETURN_VAL_EXIT_EXTERN
    xcode[xn++] = 0x80; xcode[xn++] = 0x11;
    vmExternCallRun("return-val-exit-extern", xcode, xn, 32, true, 66);
}

void sectionVmLifecycle()
{
    std::puts("[vmlifecycle]");

    static const char childPath[] = "/tmp/vmlife-child.int";
    const int childString = 0;
    const int missingString = (int)strlen(childPath) + 1;

    unsigned char code[64];
    int n = pushStaticString(code, childString);
    code[n++] = 0x80; code[n++] = 0x08; // CALLSTART
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("callstart", childPath, true, code, n, 32, false);

    n = pushStaticString(code, childString);
    code[n++] = 0x80; code[n++] = 0x0A; // SPAWN
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("spawn", childPath, true, code, n, 32, false);

    // FORK discards its fork, so the parent is left exactly as it was.
    n = pushStaticString(code, childString);
    code[n++] = 0x80; code[n++] = 0x0B; // FORK
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("fork", childPath, true, code, n, 32, false);

    // EXEC hands the (attached) parent the fork in place of this program.
    n = pushStaticString(code, childString);
    code[n++] = 0x80; code[n++] = 0x09; // EXEC
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("exec", childPath, true, code, n, 32, true);

    // A second CALLSTART with a child already attached hits the guard. This
    // vector only runs STOP_PROGRAM; the guard belongs to the parent chain,
    // which attachParent sets up before the program ever executes.
    n = 0;
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("attached-parent", childPath, false, code, n, 32, true);

    // A missing script makes runScript return null, which forkProgram turns
    // into a fatal error before any flag is set.
    n = pushStaticString(code, missingString);
    code[n++] = 0x80; code[n++] = 0x0B; // FORK
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("fork-missing", childPath, false, code, n, 32, false);

    // CALLSTART with a missing script: the CHILD_CALL flag is set before the
    // name is popped, so the flag survives the fatal.
    n = pushStaticString(code, missingString);
    code[n++] = 0x80; code[n++] = 0x08; // CALLSTART
    code[n++] = 0x80; code[n++] = 0x11; // STOP_PROGRAM
    vmLifeRun("callstart-missing", childPath, false, code, n, 32, false);
}

void sectionVmDelayed()
{
    std::puts("[vmdelayed]");

    // All rows start clear, so each vector's deltas are unambiguous.
    static const int clear[4][3] = {
        { 0x00, 0, 0 }, { 0x00, 0, 0 }, { 0x00, 0, 0 }, { 0x00, 0, 0 },
    };

    // CALL_WHEN arms procedure 2 with condition offset 0x1234. Note the push
    // order: the condition offset goes on first and the procedure index second,
    // because opConditionalCall pops data[0] as the index.
    static const unsigned char when[] = {
        0xC0, 0x01, 0x00, 0x00, 0x12, 0x34, // PUSH int 0x1234 (condition)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (procedure index)
        0x80, 0x07, // CALL_WHEN
        0x80, 0x11, // STOP_PROGRAM
    };
    vmDelayedRun("call-when", when, sizeof(when), 32, 4, clear);

    // A second CALL_WHEN on the same row ORs the flag in rather than replacing
    // it, so an already-CRITICAL row keeps that bit.
    static const int critical[4][3] = {
        { 0x00, 0, 0 }, { 0x00, 0, 0 }, { 0x10, 0, 0 }, { 0x00, 0, 0 },
    };
    vmDelayedRun("call-when-orr", when, sizeof(when), 32, 4, critical);

    // CANCEL zeroes flags, time and condition for one row only.
    static const int armed[4][3] = {
        { 0x00, 0, 0 },
        { 0x01, 1000, 0 },
        { 0x02, 0, 0x1234 },
        { 0x03, 2000, 0x5678 },
    };
    static const unsigned char cancel[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x03, // PUSH int 3 (procedure index)
        0x80, 0x48, // CANCEL
        0x80, 0x11, // STOP_PROGRAM
    };
    vmDelayedRun("cancel", cancel, sizeof(cancel), 32, 4, armed);

    // CANCEL_ALL wipes every row, main (index 0) included.
    static const unsigned char cancelAll[] = {
        0x80, 0x49, // CANCEL_ALL
        0x80, 0x11, // STOP_PROGRAM
    };
    vmDelayedRun("cancel-all", cancelAll, sizeof(cancelAll), 32, 4, armed);

    // CANCEL rejects an out-of-range index through programFatalError, which
    // the VM loop turns into EXITED|FATAL_ERROR (0x05 in the low flags).
    static const unsigned char cancelBad[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x04, // PUSH int 4 (== procedureCount)
        0x80, 0x48, // CANCEL
        0x80, 0x11, // STOP_PROGRAM
    };
    vmDelayedRun("cancel-range", cancelBad, sizeof(cancelBad), 32, 4, armed);
}

void sectionVmGlobals()
{
    std::puts("[vmglobals]");

    // SET_GLOBAL snapshots the stack size, so the three pushed values below
    // *are* the global variables. The addresses are NEGATIVE: STORE_GLOBAL and
    // FETCH_GLOBAL index basePointer + addr, and with base == size the only
    // in-range slots are addr in -(size)..-1, i.e. the pushed values themselves.
    // Locals go above base as positive offsets, which is why the base snapshot
    // never moves.
    static const unsigned char globals[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0A, // PUSH int 10  (global -3)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x14, // PUSH int 20  (global -2)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x1E, // PUSH int 30  (global -1)
        0x80, 0x2C, // SET_GLOBAL  base = 3
        // STORE_GLOBAL -1 = 99
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x63, // PUSH int 99
        0xC0, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, // PUSH int -1 (addr)
        0x80, 0x13, // STORE_GLOBAL
        // FETCH_GLOBAL -1 -> pushes 99
        0xC0, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, // PUSH int -1 (addr)
        0x80, 0x12, // FETCH_GLOBAL
        0x80, 0x11, // STOP_PROGRAM
    };
    vmGlobalsRun("store-fetch", globals, sizeof(globals), 32);

    // DUMP 2 discards the top two, leaving the three globals.
    static const unsigned char dump[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0A, // PUSH int 10
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x14, // PUSH int 20
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x1E, // PUSH int 30
        0x80, 0x2C, // SET_GLOBAL
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x63, // PUSH int 99
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x64, // PUSH int 100
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (count)
        0x80, 0x2E, // DUMP
        0x80, 0x11, // STOP_PROGRAM
    };
    vmGlobalsRun("dump", dump, sizeof(dump), 32);

    // Locals pushed after SET_GLOBAL sit above the base and must not disturb
    // the globals: after writing global -1 through two pushed operands, the
    // first global is still reachable at -3.
    static const unsigned char rebasify[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x0A, // PUSH int 10  (global -3)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x14, // PUSH int 20  (global -2)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x1E, // PUSH int 30  (global -1)
        0x80, 0x2C, // SET_GLOBAL  base = 3
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x63, // PUSH int 99  (operand)
        0xC0, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, // PUSH int -1 (addr)
        0x80, 0x13, // STORE_GLOBAL  (global -1 = 99)
        0xC0, 0x01, 0xFF, 0xFF, 0xFF, 0xFD, // PUSH int -3 (addr)
        0x80, 0x12, // FETCH_GLOBAL  -> pushes the first global, 10
        0x80, 0x11, // STOP_PROGRAM
    };
    vmGlobalsRun("locals-above-base", rebasify, sizeof(rebasify), 32);
}

void sectionVmProc()
{
    std::puts("[vmproc]");

    // Procedure metadata and critical-section opcodes, on a program with a
    // real procedure table, identifier blob and static-string table.
    //
    // Identifiers blob. Note the asymmetry: programGetIdentifier returns
    // identifiers + offset with NO +4, so nameOffset is measured from the
    // block's own length field, whereas programGetString returns
    // staticStrings + 4 + offset. Verified against a real script
    // (scripts\gl_highlighting.int: 15 procedures, identifiers length 538 at
    // 406, proc0 nameOffset 6 -> "........").
    //
    // The blob starts at identifiersOffset + 4, so with "main\0alpha\0beta"
    // laid out there the nameOffsets are 4, 9 and 15.
    static const char identifiers[] = "main\0alpha\0beta";
    // Static strings (offsets are relative to the first byte after the
    // length field): "beta" at 0, "gamma" at 5, "BETA" at 11.
    static const char strings[] = "beta\0gamma\0BETA";

    static const VmProc procs[] = {
        { 4, 0x00, 0 }, // 0: main
        { 9, 0x00, 2 }, // 1: alpha, 2 args
        { 15, 0x00, 1 }, // 2: beta, 1 arg
        { 15, 0x00, 3 }, // 3: beta again, 3 args
    };

    auto run = [&](const char* caseName, const unsigned char* code, int codeLength,
                       int budget, bool attachParent = false) {
        vmProcRun(caseName, procs, 4, identifiers, (int)sizeof(identifiers),
            strings, (int)sizeof(strings), code, codeLength, budget, attachParent);
    };

    // Critical sections: the 0x80 flag bit survives the per-step
    // `flags &= 0xFFFF; flags |= opcode << 16` rewrite in programInterpret.
    static const unsigned char enter[] = {
        0x80, 0x02, // ENTER_CRITICAL_SECTION
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x11, // STOP_PROGRAM
    };
    run("critical-enter", enter, sizeof(enter), 32);

    static const unsigned char enterLeave[] = {
        0x80, 0x02, // ENTER_CRITICAL_SECTION
        0x80, 0x03, // LEAVE_CRITICAL_SECTION
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x11, // STOP_PROGRAM
    };
    run("critical-leave", enterLeave, sizeof(enterLeave), 32);

    // START_CRITICAL / END_CRITICAL are the extended-set aliases of the same
    // two handlers.
    static const unsigned char startEnd[] = {
        0x80, 0x4A, // START_CRITICAL
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x4B, // END_CRITICAL
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2B, // PUSH int 43
        0x80, 0x11, // STOP_PROGRAM
    };
    run("critical-startend", startEnd, sizeof(startEnd), 32);

    // FETCH_PROCEDURE_ADDRESS pushes procedure 2's bodyOffset.
    static const unsigned char fetchAddr[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2
        0x80, 0x2D, // FETCH_PROCEDURE_ADDRESS
        0x80, 0x11, // STOP_PROGRAM
    };
    run("fetch-proc-addr", fetchAddr, sizeof(fetchAddr), 32);

    // CHECK_PROCEDURE_ARGUMENT_COUNT pops (index, expected) and compares
    // against the table's argCount. Proc 1 has 2 args.
    static const unsigned char checkOk[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (index)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x02, // PUSH int 2 (expected)
        0x80, 0x27, // CHECK_PROCEDURE_ARGUMENT_COUNT
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x07, // PUSH int 7
        0x80, 0x11, // STOP_PROGRAM
    };
    run("check-args-ok", checkOk, sizeof(checkOk), 32);

    // Mismatch: the name read is part of the fatal message, so the IP stays
    // past the already-fetched opcode and EXITED|FATAL_ERROR lands on top of
    // it.
    static const unsigned char checkBad[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x01, // PUSH int 1 (index)
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x09, // PUSH int 9 (expected)
        0x80, 0x27, // CHECK_PROCEDURE_ARGUMENT_COUNT
        0x80, 0x11, // STOP_PROGRAM
    };
    run("check-args-bad", checkBad, sizeof(checkBad), 32);

    // LOOKUP_PROCEDURE_BY_NAME skips index 0 and returns the first match, so
    // "beta" resolves to 2 even though index 3 shares the name. The lookup is
    // case-insensitive (SDL_strcasecmp).
    static const unsigned char lookupHit[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x00, // PUSH static string "beta"
        0x80, 0x28, // LOOKUP_PROCEDURE_BY_NAME
        0x80, 0x11, // STOP_PROGRAM
    };
    run("lookup-hit", lookupHit, sizeof(lookupHit), 32);

    static const unsigned char lookupCase[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x0B, // PUSH static string "BETA"
        0x80, 0x28, // LOOKUP_PROCEDURE_BY_NAME
        0x80, 0x11, // STOP_PROGRAM
    };
    run("lookup-case", lookupCase, sizeof(lookupCase), 32);

    // "gamma" matches no identifier -> fatal.
    static const unsigned char lookupMiss[] = {
        0x90, 0x01, 0x00, 0x00, 0x00, 0x05, // PUSH static string "gamma"
        0x80, 0x28, // LOOKUP_PROCEDURE_BY_NAME
        0x80, 0x11, // STOP_PROGRAM
    };
    run("lookup-miss", lookupMiss, sizeof(lookupMiss), 32);

    // DETACH with no parent is a no-op.
    static const unsigned char detachNull[] = {
        0x80, 0x0F, // DETACH
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x11, // STOP_PROGRAM
    };
    run("detach-null", detachNull, sizeof(detachNull), 32, false);

    // DETACH clears both child flags on the parent and unlinks it.
    static const unsigned char detachChild[] = {
        0x80, 0x0F, // DETACH
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x2A, // PUSH int 42
        0x80, 0x11, // STOP_PROGRAM
    };
    run("detach-child", detachChild, sizeof(detachChild), 32, true);

    // POP_FLAGS_RETURN_VAL_EXIT pops its value before restoring the frame, so
    // the value is already gone by the time opPopFlags reads the three frame
    // cells and the return value lands back on the caller's stack.
    static const unsigned char returnVal[] = {
        0xC0, 0x01, 0x00, 0x00, 0x00, 0x4D, // PUSH int 77
        0x80, 0x25, // POP_FLAGS_RETURN_VAL_EXIT
    };
    vmReturnValRun("return-val", returnVal, sizeof(returnVal), 200);

    // POP_FLAGS_EXIT reads the three frame cells directly, so the code must
    // not leave a value on top of them - the plain [vmcall] push-return case,
    // which fatals because programSetupCall's trailing zero is still there.
    static const unsigned char plainExit[] = {
        0x80, 0x21, // POP_FLAGS_EXIT
    };
    vmReturnValRun("flags-exit", plainExit, sizeof(plainExit), 200);
}

void sectionVm()
{
    std::puts("[vm]");

    // Build a minimal program in memory: the bytecode region C's programCreate
    // hands the VM is just data[instructionPointer..], so the header only needs
    // a procedure count of 0 to parse, and the opcodes go after it.
    //
    // Three NOOPs then EXIT_PROGRAM (0x8010): enough to prove the
    // fetch/decode/dispatch loop runs and terminates, without pulling in the
    // arithmetic opcodes.
    unsigned char data[64];
    memset(data, 0, sizeof(data));

    // procedures (int32 count) at offset 42, identifiers count, strings count.
    int32_t zero = 0;
    memcpy(data + 42, &zero, sizeof(zero));
    memcpy(data + 46, &zero, sizeof(zero));
    memcpy(data + 50, &zero, sizeof(zero));

    // Opcodes are int16 big-endian, starting at byte 54. Write the bytes
    // explicitly: memcpy of an int16 would store them little-endian and the
    // VM's stackReadInt16 would decode 0x8000 as 0x0080.
    static const unsigned char ops[] = {
        0x80, 0x00, // NOOP
        0x80, 0x00, // NOOP
        0x80, 0x00, // NOOP
        0x80, 0x10, // EXIT_PROGRAM
    };
    memcpy(data + 54, ops, sizeof(ops));

    // programCreateByPath parses from a File*, so round-trip through a temp
    // file the way a real .int arrives.
    char path[] = "/tmp/vm-probe.int";
    FILE* f = fopen(path, "wb");
    fwrite(data, 1, sizeof(data), f);
    fclose(f);

    fallout::interpreterRegisterOpcodeHandlers();
    fallout::Program* program = fallout::programCreateByPath(path);
    if (program == nullptr) {
        std::puts("create=null");
        return;
    }

    std::printf("procedures=%d\n", program->procedureCount());

    // programCreateByPath memsets the struct, so flags starts at 0 and the
    // loop's own exit flags are all that show up in the result.
    program->flags = 0;
    program->instructionPointer = 54;
    fallout::programInterpret(program, 4);
    std::printf("after-run ip=%d exited=%d flags=%02X\n",
        program->instructionPointer, program->exited ? 1 : 0, (unsigned int)program->flags);

    // A program already flagged EXITED must not run at all: the IP stays put.
    fallout::Program* second = fallout::programCreateByPath(path);
    if (second != nullptr) {
        second->flags = fallout::PROGRAM_FLAG_EXITED;
        second->instructionPointer = 54;
        fallout::programInterpret(second, 4);
        std::printf("pre-exited ip=%d flags=%02X\n",
            second->instructionPointer, (unsigned int)second->flags);
        fallout::programFree(second);
    }

    // 0x0080 has bit 7 of the (opcode >> 8) byte clear, which C treats as a
    // bad opcode. programFatalError is a longjmp out of the VM, so the
    // observable effect is the flags, not a propagated error.
    fallout::Program* third = fallout::programCreateByPath(path);
    if (third != nullptr) {
        third->data[54] = 0x00;
        third->data[55] = 0x80;
        third->flags = 0;
        third->instructionPointer = 54;
        fallout::programInterpret(third, 4);
        std::printf("bad-opcode flags=%02X\n", (unsigned int)third->flags);
        fallout::programFree(third);
    }

    fallout::programFree(program);
}

int main()
{
    // Unbuffered stdout: engine debug output goes to stderr (SDL_Log),
    // so without this the merged capture would group all stderr first.
    // v1.txt predates this fix (its INFO line sits on top).
    setvbuf(stdout, nullptr, _IONBF, 0);

    // The map sections boot their own engine in a child process (exec keeps
    // stdout on the same stream, so the vectors still append to this run).
    if (getenv("REFHARNESS_MAP_ONLY") != nullptr) {
        sectionMapChild();
        return 0;
    }

    // The harness depends on fixtures.sh outputs. Never run the binary
    // directly: degraded /tmp state silently corrupts vectors, and the CWD
    // fallback of relative VFS paths writes scratch files (wtest.bin) into
    // the mounted repo.
    if (access("/tmp/f2test.zip", F_OK) != 0 || access("/tmp/f2xbase_a", F_OK) != 0) {
        std::fprintf(stderr, "refharness: missing fixtures, run via build.sh\n");
        return 2;
    }

    RUN_SECTION(Rgb555);
    RUN_SECTION(RectUnion);
    RUN_SECTION(RectIntersection);
    RUN_SECTION(RectClip);
    RUN_SECTION(Random);
    RUN_SECTION(StrParsers);
    RUN_SECTION(StrParseFunc);
    RUN_SECTION(WordWrap);
    RUN_SECTION(Dict);
    RUN_SECTION(DictFile);
    RUN_SECTION(Paths);
    RUN_SECTION(FileUtils);
    RUN_SECTION(Pattern);
    RUN_SECTION(Dbase);
    RUN_SECTION(Zip);
    RUN_SECTION(FixtureDat);
    RUN_SECTION(Xbase);
    RUN_SECTION(Xfile);
    RUN_SECTION(Db);
    RUN_SECTION(Config);
    RUN_SECTION(Message);
    RUN_SECTION(Art);
    RUN_SECTION(Cache);
    RUN_SECTION(CacheEvict);
    RUN_SECTION(ArtLoad);
    RUN_SECTION(Color);
    RUN_SECTION(Settings);
    RUN_SECTION(ColorCycle);
    RUN_SECTION(Fps);
    RUN_SECTION(Svga);
    RUN_SECTION(Draw);
    RUN_SECTION(Font);
    RUN_SECTION(IfaceFont);
    RUN_SECTION(Region);
    RUN_SECTION(Datafile);
    RUN_SECTION(CfgMig);
    RUN_SECTION(Input);
    RUN_SECTION(Kb);
    RUN_SECTION(DInput);
    RUN_SECTION(Graph);
    RUN_SECTION(Options);
    RUN_SECTION(PtrReg);
    RUN_SECTION(GameDefs);
    RUN_SECTION(ArtDefs);
    RUN_SECTION(KbFull);
    RUN_SECTION(CombatDefs);
    RUN_SECTION(ObjTypes);
    RUN_SECTION(Prototypes);
    RUN_SECTION(GameTypes);
    RUN_SECTION(Ability);
    RUN_SECTION(UiTypes);
    RUN_SECTION(MouseWin);
    RUN_SECTION(WinBtn);
    RUN_SECTION(WinMenu);
    RUN_SECTION(WinFrm);
    RUN_SECTION(WinDlg);
    RUN_SECTION(WinMenu2);
    RUN_SECTION(WinText);
    RUN_SECTION(WinDbg);
    RUN_SECTION(WinDbox);
    RUN_SECTION(WinFile);
    RUN_SECTION(ProtoDb);
    RUN_SECTION(TileMath);
    RUN_SECTION(SquareMath);
    RUN_SECTION(TileInRect);
    RUN_SECTION(EventQueue);
    RUN_SECTION(LightReact);
    RUN_SECTION(GameMouseUtil);
    RUN_SECTION(StatSkill);
    RUN_SECTION(CombatSkill);
    RUN_SECTION(GameTime);
    RUN_SECTION(TraitSelect);
    RUN_SECTION(ObjCreate);
    RUN_SECTION(CritterStats);
    RUN_SECTION(Inventory);
    RUN_SECTION(Equip);
    RUN_SECTION(Ballistics);
    RUN_SECTION(ItemMove);
    RUN_SECTION(ItemDrop);
    RUN_SECTION(AmmoCont);
    RUN_SECTION(Explosives);
    RUN_SECTION(Vitals);
    RUN_SECTION(ObjFind);
    RUN_SECTION(ObjAttrs);
    RUN_SECTION(ObjVis);
    RUN_SECTION(ObjOutline);
    RUN_SECTION(GameMouse);
    RUN_SECTION(GmouseEn);
    RUN_SECTION(ArtHide);
    RUN_SECTION(ProtoNew);
    RUN_SECTION(ProtoCopy);
    RUN_SECTION(MapVars);
    RUN_SECTION(SkillTag);
    RUN_SECTION(PcStat);
    RUN_SECTION(XpRoll);
    RUN_SECTION(SkillRoll);
    RUN_SECTION(MiscGetters);
    RUN_SECTION(FindCid);
    RUN_SECTION(NevsEvents);
    RUN_SECTION(SfallVars);
    RUN_SECTION(SfallIni);
    RUN_SECTION(ObjPlace);
    RUN_SECTION(ItemNames);
    RUN_SECTION(Names);
    RUN_SECTION(MoreNames);
    RUN_SECTION(MoreDesc);
    RUN_SECTION(ScriptProg);
    RUN_SECTION(ScriptRun);
    RUN_SECTION(ScriptRunGame);
    RUN_SECTION(ScriptScan);
    RUN_SECTION(ProgramStack);
    RUN_SECTION(ProgRetStack);
    RUN_SECTION(StatsSave);
    RUN_SECTION(FrmIds);
    RUN_SECTION(PartyObjects);
    RUN_SECTION(CombatDamage);
    RUN_SECTION(CombatRequest);
    RUN_SECTION(PerkRank);
    RUN_SECTION(WeaponData);
    RUN_SECTION(IsoRest);
    RUN_SECTION(MapTransition);
    RUN_SECTION(ObjPlaceOnTile);
    RUN_SECTION(MapStart);
    RUN_SECTION(ScriptAttach);
    RUN_SECTION(CombatBadShot);
    RUN_SECTION(CriticalHit);
    RUN_SECTION(Ai);
    RUN_SECTION(AiDecisions);
    RUN_SECTION(AiInventory);
    RUN_SECTION(CombatEnd);
    RUN_SECTION(RangedMiss);
    RUN_SECTION(ExplosionExtras);
    RUN_SECTION(Spray);
    RUN_SECTION(CombatAttack);
    RUN_SECTION(Path);
    RUN_SECTION(AiTarget);
    RUN_SECTION(AiMove);
    RUN_SECTION(AiDist);
    RUN_SECTION(AiAttack);
    RUN_SECTION(AiDrug);
    RUN_SECTION(CombatAiFlow);
    RUN_SECTION(CombatEndAttempt);

// Re-exec for the map sections: a fresh engine avoids the freed-object
    // links the object sections leave behind.
    setenv("REFHARNESS_MAP_ONLY", "1", 1);
    execl("/proc/self/exe", "refharness", (char*)nullptr);
    std::fprintf(stderr, "refharness: exec failed\n");
    return 3;
}
