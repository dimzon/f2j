package org.fallout2ce;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Port of {@code src/xfile.h} / {@code src/xfile.cc}: the VFS handle layer
 * ({@code XFile} over plain files, DAT entries and gzip streams), the xbase
 * mount registry and the {@code xlist} enumeration.
 */
public final class XFile {

    private XFile() {
    }

    public enum Type {
        FILE,
        DFILE,
        GZFILE
    }

    /** Gzip branch state (position tracked manually, like zlib). */
    public static final class GzipState {
        public CompatFiles.EndAwareGzipStream in;
        public GZIPOutputStream out;
        public String path = "";
        public String mode = "";
        public long position;
        public boolean eof;
        public long bytesWritten;
    }

    /** Mirror of {@code struct XFile}. */
    public static final class Handle {
        public Type type;
        public RandomAccessFile file;
        public DFile.Handle dfile;
        public GzipState gz;
    }

    /** Mirror of {@code struct XBase}. */
    public static final class XBase {        public String path = "";
        public DBase.Archive dbase;
        public boolean isDbase;
    }

    static final List<XBase> xbaseHead = new ArrayList<>();
    static boolean exitHandlerRegistered;

    /** Mirror of {@code struct XList}. */
    public static final class XList {
        public final List<String> fileNames = new ArrayList<>();
    }

    static final int ENUM_FILE = 0;
    static final int ENUM_DIRECTORY = 1;
    static final int ENUM_DFILE = 2;

    // 0x4DED6C xfclose. Returns the backend rc, like C.
    public static int xfileClose(Handle stream) {
        int rc;
        switch (stream.type) {
        case DFILE:
            rc = DFile.dfileClose(stream.dfile);
            break;
        case GZFILE:
            rc = closeGzip(stream.gz) ? 0 : -1;
            break;
        default:
            rc = closeFile(stream.file) ? 0 : -1;
            break;
        }
        stream.type = null;
        stream.file = null;
        stream.dfile = null;
        stream.gz = null;
        return rc;
    }

    private static boolean closeFile(RandomAccessFile raf) {
        if (raf == null) {
            return true;
        }
        try {
            raf.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean closeGzip(GzipState gz) {
        try {
            if (gz.in != null) {
                gz.in.close();
            }
            if (gz.out != null) {
                gz.out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // 0x4DEE2C xfopen. Returns null on any failure, like C.
    public static Handle xfileOpen(String filePath, String mode) {
        Handle stream = new Handle();

        PlatformPaths.Split split = PlatformPaths.splitPath(filePath);
        String resolvedPath;

        if (!split.drive().isEmpty() || startsLikeAbsolute(split.dir())) {
            // Absolute path: plain stream.
            RandomAccessFile raf = CompatFiles.fopen(filePath, mode);
            if (raf == null) {
                return null;
            }
            stream.type = Type.FILE;
            stream.file = raf;
            resolvedPath = filePath;
        } else {
            // Relative path: walk xbases, then cwd fallback.
            boolean opened = false;
            String foundPath = filePath;
            for (XBase curr : xbaseHead) {
                if (curr.isDbase) {
                    DFile.Handle dfile = DFile.dfileOpen(curr.dbase, filePath, mode);
                    if (dfile != null) {
                        stream.type = Type.DFILE;
                        stream.dfile = dfile;
                        opened = true;
                        break;
                    }
                } else {
                    String joined = curr.path + "\\" + filePath;
                    RandomAccessFile raf = CompatFiles.fopen(joined, mode);
                    if (raf != null) {
                        stream.type = Type.FILE;
                        stream.file = raf;
                        foundPath = joined;
                        opened = true;
                        break;
                    }
                }
            }

            if (!opened) {
                RandomAccessFile raf = CompatFiles.fopen(filePath, mode);
                if (raf == null) {
                    return null;
                }
                stream.type = Type.FILE;
                stream.file = raf;
            }
            resolvedPath = foundPath;
        }

        if (stream.type == Type.FILE) {
            // Gzip-magic sniff, exactly like C (two fgetc + rewind).
            int ch1 = -1;
            int ch2 = -1;
            try {
                ch1 = stream.file.read();
                ch2 = stream.file.read();
                stream.file.seek(0);
            } catch (IOException e) {
                // Unreadable (e.g. write mode): plain stream, like C.
                try {
                    stream.file.seek(0);
                } catch (IOException ignored) {
                    // Ignore.
                }
                return stream;
            }
            if (ch1 == 0x1F && ch2 == 0x8B) {
                closeFile(stream.file);
                stream.file = null;
                GzipState gz = openGzip(resolvedPath, mode);
                if (gz == null) {
                    return null;
                }
                stream.type = Type.GZFILE;
                stream.gz = gz;
            }
        }

        return stream;
    }

    private static boolean startsLikeAbsolute(String dir) {
        return !dir.isEmpty() && (dir.charAt(0) == '\\' || dir.charAt(0) == '/' || dir.charAt(0) == '.');
    }

    private static GzipState openGzip(String path, String mode) {
        boolean write = mode.indexOf('w') >= 0 || mode.indexOf('a') >= 0 || mode.indexOf('+') >= 0;
        GzipState gz = new GzipState();
        gz.path = path;
        gz.mode = mode;
        if (write && mode.indexOf('r') < 0) {
            boolean append = mode.indexOf('a') >= 0;
            gz.out = CompatFiles.gzopenWrite(path, append);
            if (gz.out == null) {
                return null;
            }
        } else {
            gz.in = CompatFiles.gzopenRead(path);
            if (gz.in == null) {
                return null;
            }
        }
        return gz;
    }

    // 0x4DF1AC xvfprintf (formatted write; dfile branch is -1 in C too).
    public static int xfilePrintFormattedArgs(Handle stream, String format, Object... args) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfilePrintFormattedArgs(stream.dfile, format, args);
        case GZFILE: {
            byte[] bytes = String.format(format, args).getBytes(StandardCharsets.ISO_8859_1);
            try {
                stream.gz.out.write(bytes);
                stream.gz.position += bytes.length;
                stream.gz.bytesWritten += bytes.length;
                return bytes.length;
            } catch (IOException e) {
                return -1;
            }
        }
        default: {
            String text = String.format(format, args);
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            try {
                stream.file.write(bytes);
                return bytes.length;
            } catch (IOException e) {
                return -1;
            }
        }
        }
    }

    public static int xfilePrintFormatted(Handle stream, String format, Object... args) {
        return xfilePrintFormattedArgs(stream, format, args);
    }

    // 0x4DF22C xfgetc.
    public static int xfileReadChar(Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileReadChar(stream.dfile);
        case GZFILE:
            try {
                int ch = stream.gz.in.read();
                if (ch >= 0) {
                    stream.gz.position++;
                } else {
                    stream.gz.eof = true;
                }
                return ch;
            } catch (IOException e) {
                return -1;
            }
        default:
            try {
                return stream.file.read();
            } catch (IOException e) {
                return -1;
            }
        }
    }

    // 0x4DF280 xfgets.
    public static String xfileReadString(Handle stream, int size) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileReadString(stream.dfile, size);
        case GZFILE:
            try {
                String line = CompatFiles.gzgets(stream.gz.in, size);
                if (line != null) {
                    stream.gz.position += line.length();
                }
                return line;
            } catch (IOException e) {
                return null;
            }
        default:
            try {
                return CompatFiles.fgets(stream.file, size);
            } catch (IOException e) {
                return null;
            }
        }
    }

    // 0x4DF320 xfputc.
    public static int xfileWriteChar(int ch, Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileWriteChar(ch, stream.dfile);
        case GZFILE:
            try {
                stream.gz.out.write(ch);
                stream.gz.position++;
                stream.gz.bytesWritten++;
                return ch & 0xFF;
            } catch (IOException e) {
                return -1;
            }
        default:
            try {
                stream.file.write(ch);
                return ch & 0xFF;
            } catch (IOException e) {
                return -1;
            }
        }
    }

    // 0x4DF380 xfputs.
    public static int xfileWriteString(String string, Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileWriteString(string, stream.dfile);
        case GZFILE: {
            byte[] bytes = string.getBytes(StandardCharsets.ISO_8859_1);
            try {
                stream.gz.out.write(bytes);
                stream.gz.position += bytes.length;
                stream.gz.bytesWritten += bytes.length;
                return bytes.length;
            } catch (IOException e) {
                return -1;
            }
        }
        default: {
            byte[] bytes = string.getBytes(StandardCharsets.ISO_8859_1);
            try {
                stream.file.write(bytes);
                return bytes.length;
            } catch (IOException e) {
                return -1;
            }
        }
        }
    }

    // 0x4DF44C xfread. Returns full items read.
    public static long xfileRead(byte[] out, int offset, int size, int count, Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileRead(out, offset, size, count, stream.dfile);
        case GZFILE: {
            // NOTE: keeps the upstream single gzread / size arithmetic.
            if (size == 0 || count == 0) {
                return 0;
            }
            long want = (long) size * count;
            int n;
            try {
                n = stream.gz.in.read(out, offset, (int) Math.min(want, Integer.MAX_VALUE));
            } catch (IOException e) {
                return 0;
            }
            if (n <= 0) {
                if (n < 0) {
                    stream.gz.eof = true;
                }
                return 0;
            }
            stream.gz.position += n;
            checkGzipEnded(stream);
            return n / size;
        }
        default: {
            long want = (long) size * count;
            long total = 0;
            try {
                while (total < want) {
                    int n = stream.file.read(out, (int) (offset + total),
                            (int) Math.min(want - total, Integer.MAX_VALUE));
                    if (n < 0) {
                        break;
                    }
                    total += n;
                }
            } catch (IOException e) {
                // Short read stands (fread semantics).
            }
            return size == 0 ? 0 : total / size;
        }
        }
    }

    // 0x4DF4E8 xfwrite. NOTE: the GZ branch returns bytes (like C),
    // not items.
    public static long xfileWrite(byte[] data, int offset, int size, int count, Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileWrite(data, offset, size, count, stream.dfile);
        case GZFILE: {
            long want = (long) size * count;
            if (want > Integer.MAX_VALUE) {
                want = Integer.MAX_VALUE;
            }
            try {
                stream.gz.out.write(data, offset, (int) want);
                stream.gz.position += want;
                stream.gz.bytesWritten += want;
                return want;
            } catch (IOException e) {
                return 0;
            }
        }
        default: {
            long want = (long) size * count;
            long total = 0;
            try {
                while (total < want) {
                    int chunk = (int) Math.min(want - total, Integer.MAX_VALUE);
                    stream.file.write(data, (int) (offset + total), chunk);
                    total += chunk;
                }
            } catch (IOException e) {
                // Short write stands.
            }
            return size == 0 ? 0 : total / size;
        }
        }
    }

    // 0x4DF5D8 xfseek. GZ branch returns the new position (like gzseek).
    public static long xfileSeek(Handle stream, long offset, int origin) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileSeek(stream.dfile, offset, origin);
        case GZFILE:
            return gzipSeek(stream.gz, offset, origin);
        default:
            try {
                long base;
                switch (origin) {
                case DFile.SEEK_SET:
                    base = 0;
                    break;
                case DFile.SEEK_CUR:
                    base = stream.file.getFilePointer();
                    break;
                case DFile.SEEK_END:
                    base = stream.file.length();
                    break;
                default:
                    return -1;
                }
                stream.file.seek(base + offset);
                return 0;
            } catch (IOException e) {
                return -1;
            }
        }
    }

    static long gzipSeek(GzipState gz, long offset, int origin) {
        if (gz.in == null) {
            // Write stream: pad forward with zeros (like gzseek).
            long target;
            switch (origin) {
            case DFile.SEEK_SET:
                target = offset;
                break;
            case DFile.SEEK_CUR:
                target = gz.position + offset;
                break;
            case DFile.SEEK_END:
                return -1;
            default:
                return -1;
            }
            if (target < gz.position) {
                return -1;
            }
            try {
                byte[] zeros = new byte[(int) Math.min(target - gz.position, 65536)];
                while (gz.position < target) {
                    int chunk = (int) Math.min(zeros.length, target - gz.position);
                    gz.out.write(zeros, 0, chunk);
                    gz.position += chunk;
                    gz.bytesWritten += chunk;
                }
                return gz.position;
            } catch (IOException e) {
                return -1;
            }
        }

        long target;
        switch (origin) {
        case DFile.SEEK_SET:
            target = offset;
            break;
        case DFile.SEEK_CUR:
            target = gz.position + offset;
            break;
        case DFile.SEEK_END:
            return -1;
        default:
            return -1;
        }
        if (target < 0) {
            return -1;
        }
        if (target < gz.position) {
            if (!gzipReopen(gz)) {
                return -1;
            }
        }
        try {
            byte[] skip = new byte[8192];
            while (gz.position < target) {
                int n = gz.in.read(skip, 0, (int) Math.min(skip.length, target - gz.position));
                if (n < 0) {
                    gz.eof = true;
                    break;
                }
                gz.position += n;
                checkGzipEnded(gz);
            }
            return gz.position;
        } catch (IOException e) {
            return -1;
        }
    }

    private static boolean gzipReopen(GzipState gz) {
        try {
            gz.in.close();
        } catch (IOException e) {
            // Ignore.
        }
        gz.in = CompatFiles.gzopenRead(gz.path);
        if (gz.in == null) {
            return false;
        }
        gz.position = 0;
        gz.eof = false;
        return true;
    }

    // 0x4DF690 xftell.
    public static long xfileTell(Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileTell(stream.dfile);
        case GZFILE:
            return stream.gz.position;
        default:
            try {
                return stream.file.getFilePointer();
            } catch (IOException e) {
                return -1;
            }
        }
    }

    // 0x4DF6E4 xrewind.
    public static void xfileRewind(Handle stream) {
        switch (stream.type) {
        case DFILE:
            DFile.dfileRewind(stream.dfile);
            break;
        case GZFILE:
            gzipReopen(stream.gz);
            break;
        default:
            try {
                stream.file.seek(0);
            } catch (IOException e) {
                // Ignore.
            }
            break;
        }
    }

    // 0x4DF780 xfeof.
    public static int xfileEof(Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileEof(stream.dfile);
        case GZFILE:
            return stream.gz.eof ? 1 : 0;
        default:
            try {
                return stream.file.getFilePointer() >= stream.file.length() ? 1 : 0;
            } catch (IOException e) {
                return 0;
            }
        }
    }

    // 0x4DF828 xfilelength. NOTE: GZFILE quirk returns 0, like C.
    public static long xfileGetSize(Handle stream) {
        switch (stream.type) {
        case DFILE:
            return DFile.dfileGetSize(stream.dfile);
        case GZFILE:
            return 0;
        default:
            try {
                return CompatFiles.getFileSize(stream.file);
            } catch (IOException e) {
                return -1;
            }
        }
    }

    // 0x4DF878 xsetpath (null closes everything).
    public static boolean xbaseReopenAll(String paths) {
        xbaseCloseAll();
        if (paths != null) {
            for (String tok : paths.split(";", -1)) {
                if (!xbaseOpen(tok)) {
                    return false;
                }
            }
        }
        return true;
    }

    // 0x4DF938 xaddpath.
    public static boolean xbaseOpen(String path) {
        if (!exitHandlerRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(XFile::xbaseCloseAll));
            exitHandlerRegistered = true;
        }

        for (int i = 0; i < xbaseHead.size(); i++) {
            XBase curr = xbaseHead.get(i);
            if (curr.path.equalsIgnoreCase(path)) {
                if (i != 0) {
                    xbaseHead.remove(i);
                    xbaseHead.add(0, curr);
                }
                return true;
            }
        }

        XBase xbase = new XBase();
        xbase.path = path;

        int[] dbaseErrorFlags = { 0 };
        DBase.Archive dbase = DBase.dbaseOpen(path, dbaseErrorFlags);
        if (dbase != null) {
            xbase.isDbase = true;
            xbase.dbase = dbase;
            xbaseHead.add(0, xbase);
            return true;
        }

        if (dbaseErrorFlags[0] != 0) {
            if ((dbaseErrorFlags[0] & DBase.ERROR_EMPTY) != 0) {
                Debug.print("[xfile] %s: empty archive\n", path);
            }
            if ((dbaseErrorFlags[0] & DBase.ERROR_ZIP64) != 0) {
                Debug.print("[xfile] %s: ZIP64 format (unsupported)\n", path);
            }
            if ((dbaseErrorFlags[0] & DBase.ERROR_MULTI_DISK) != 0) {
                Debug.print("[xfile] %s: multi-disk ZIP (unsupported)\n", path);
            }
            if ((dbaseErrorFlags[0] & DBase.ERROR_ENCRYPTED) != 0) {
                Debug.print("[xfile] %s: encrypted entries (unsupported)\n", path);
            }
            if ((dbaseErrorFlags[0] & DBase.ERROR_DESCRIPTORS) != 0) {
                Debug.print("[xfile] %s: data descriptors (unsupported)\n", path);
            }
            if ((dbaseErrorFlags[0] & DBase.ERROR_UNSUPPORTED_METHOD) != 0) {
                Debug.print("[xfile] %s: unsupported compression method(s)\n", path);
            }
        }

        if (CompatFiles.isDir(path)) {
            xbaseHead.add(0, xbase);
            return true;
        }

        return false;
    }

    // Case-insensitive, trailing separators trimmed.
    public static boolean xbaseIsValidDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String target = trimSeparators(path);
        for (XBase curr : xbaseHead) {
            if (curr.isDbase || curr.path == null) {
                continue;
            }
            String current = trimSeparators(curr.path);
            if (current.length() == target.length()
                    && current.regionMatches(true, 0, target, 0, target.length())) {
                return true;
            }
        }
        return false;
    }

    private static String trimSeparators(String path) {
        int len = path.length();
        while (len > 0 && (path.charAt(len - 1) == '\\' || path.charAt(len - 1) == '/')) {
            len--;
        }
        return path.substring(0, len);
    }

    // 0x4DFFAC xmkdir (outcome-equivalent; no chdir games in Java).
    public static int xbaseMakeDirectory(String filePath) {
        String path;
        PlatformPaths.Split split = PlatformPaths.splitPath(filePath);
        if (!split.drive().isEmpty() || startsLikeAbsolute(split.dir())) {
            path = filePath;
        } else {
            String base = null;
            for (XBase curr : xbaseHead) {
                if (!curr.isDbase) {
                    base = curr.path;
                    break;
                }
            }
            if (base == null) {
                base = System.getProperty("user.dir");
            }
            path = base + "\\" + filePath;
        }
        return CompatFiles.mkdirRecursive(PlatformPaths.windowsPathToNative(path));
    }

    // 0x4DFB3C xenumfiles.
    static boolean xlistEnumerate(String pattern, XListHandler handler, XList xlist) {
        FileFind.Search search = new FileFind.Search();

        String nativePattern = PlatformPaths.windowsPathToNative(pattern);
        PlatformPaths.Split split = PlatformPaths.splitPath(nativePattern);
        if (!split.drive().isEmpty() || startsLikeAbsolute(split.dir())) {
            if (FileFind.fileFindFirst(nativePattern, search)) {
                do {
                    int type = FileFind.fileFindIsDirectory(search)
                            ? ENUM_DIRECTORY
                            : ENUM_FILE;
                    if (type == ENUM_DIRECTORY
                            && (FileFind.fileFindGetName(search).equals("..")
                                || FileFind.fileFindGetName(search).equals("."))) {
                        continue;
                    }
                    String name = PlatformPaths.makePath(
                            split.drive(), split.dir(), FileFind.fileFindGetName(search), "");
                    if (!handler.handle(name, type, xlist)) {
                        break;
                    }
                } while (FileFind.fileFindNext(search));
            }
            return FileFind.findFindClose(search);
        }

        for (XBase xbase : xbaseHead) {
            if (xbase.isDbase) {
                DBase.FindData fd = new DBase.FindData();
                if (DBase.dbaseFindFirstEntry(xbase.dbase, fd, pattern)) {
                    do {
                        if (!handler.handle(fd.fileName, ENUM_DFILE, xlist)) {
                            return DBase.dbaseFindClose(xbase.dbase, fd);
                        }
                    } while (DBase.dbaseFindNextEntry(xbase.dbase, fd));
                    DBase.dbaseFindClose(xbase.dbase, fd);
                }
            } else {
                String joined = PlatformPaths.windowsPathToNative(xbase.path + "\\" + pattern);
                if (FileFind.fileFindFirst(joined, search)) {
                    do {
                        int type = FileFind.fileFindIsDirectory(search)
                                ? ENUM_DIRECTORY
                                : ENUM_FILE;
                        if (type == ENUM_DIRECTORY
                                && (FileFind.fileFindGetName(search).equals("..")
                                    || FileFind.fileFindGetName(search).equals("."))) {
                            continue;
                        }
                        String name = PlatformPaths.makePath(
                                split.drive(), split.dir(), FileFind.fileFindGetName(search), "");
                        if (!handler.handle(name, type, xlist)) {
                            break;
                        }
                    } while (FileFind.fileFindNext(search));
                }
                FileFind.findFindClose(search);
            }
        }

        PlatformPaths.Split fresh = PlatformPaths.splitPath(nativePattern);
        if (FileFind.fileFindFirst(nativePattern, search)) {
            do {
                int type = FileFind.fileFindIsDirectory(search) ? ENUM_DIRECTORY : ENUM_FILE;
                if (type == ENUM_DIRECTORY
                        && (FileFind.fileFindGetName(search).equals("..")
                            || FileFind.fileFindGetName(search).equals("."))) {
                    continue;
                }
                String name = PlatformPaths.makePath(
                        fresh.drive(), fresh.dir(), FileFind.fileFindGetName(search), "");
                if (!handler.handle(name, type, xlist)) {
                    break;
                }
            } while (FileFind.fileFindNext(search));
        }
        return FileFind.findFindClose(search);
    }

    interface XListHandler {
        boolean handle(String name, int type, XList xlist);
    }

    // 0x4DFF28 xbuild_filelist.
    public static boolean xlistInit(String pattern, XList xlist) {
        xlistEnumerate(pattern, (name, type, list) -> {
            if (type == ENUM_DIRECTORY) {
                return true;
            }
            list.fileNames.add(name);
            return true;
        }, xlist);
        return true;
    }

    public static boolean xlistInitDirectories(String pattern, XList xlist) {
        xlistEnumerate(pattern, (name, type, list) -> {
            if (type != ENUM_DIRECTORY) {
                return true;
            }
            list.fileNames.add(name);
            return true;
        }, xlist);
        return true;
    }

    // 0x4DFF48 xfree_filelist.
    public static void xlistFree(XList xlist) {
        xlist.fileNames.clear();
    }

    // 0x4E01F8 xclearpath.
    static void xbaseCloseAll() {
        List<XBase> xbases = new ArrayList<>(xbaseHead);
        xbaseHead.clear();
        for (XBase curr : xbases) {
            if (curr.isDbase) {
                DBase.dbaseClose(curr.dbase);
            }
            curr.path = null;
            curr.dbase = null;
        }
    }

    /** Test seam: mounted xbase count. */
    static int xbaseCount() {
        return xbaseHead.size();
    }

    /** Test seam: head xbase path. */
    static String xbaseHeadPath() {
        return xbaseHead.isEmpty() ? null : xbaseHead.get(0).path;
    }

    /** Test seam: reset registry (tests must not leak mounts). */
    static void resetForTests() {
        xbaseCloseAll();
    }

    // Subclass hook so tests can observe gzip stream-end timing.
    static void checkGzipEnded(Handle stream) {
        // Called after successful gzip reads: zlib flags EOF once the
        // trailer is consumed, even when bytes were produced.
        if (stream.gz.in != null && stream.gz.in.streamEnded()) {
            stream.gz.eof = true;
        }
    }

    static void checkGzipEnded(GzipState gz) {
        if (gz.in != null && gz.in.streamEnded()) {
            gz.eof = true;
        }
    }
}
