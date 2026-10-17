package org.fallout2ce;

import java.util.function.Function;

/**
 * Port of the interface-bar init slice of {@code src/interface.h} /
 * {@code src/interface.cc}: {@code interfaceInit} (0x45D880,
 * {@code intface_init}) up to its {@code return 0}, with
 * {@code customInterfaceBarInit}, {@code extendedApBarInitToWindow},
 * {@code sidePanelsInit}, {@code intface_init_items} (0x45FD2C) and the
 * teardown that {@code intface_fatal_error} (0x461124) needs.
 *
 * <p>Vectors: {@code [isorest]} in
 * {@code docker/refharness/golden/v97.txt}, produced by
 * {@code sectionIsoRest} in {@code docker/refharness/ref_main.cc}:
 * {@code iface=0 enter=0}. The two embedded
 * {@code INTRFACE:}/{@code Elevators init:} lines are {@code debugPrint}
 * noise: per the established rule the text is not asserted, but its cause is
 * reproduced faithfully - the default {@code settings.ui.iface_bar_width}
 * is 800 while the harness screen is 640x480, so
 * {@link #customInterfaceBarInit} takes the {@code debugPrint} branch and
 * falls back to the 640-wide default bar.
 *
 * <p>NOTES / mapping decisions (all documented at the use site too):
 * <ul>
 * <li>Fatal errors: C {@code intface_fatal_error(rc)} is
 * {@code interfaceFree(); return rc;}. Java keeps the returned {@code -1}
 * code (same convention as the v42 {@link WindowManager#winGetStr} and
 * {@link DBox} ports, which return the code instead of throwing) and
 * {@link #interfaceFatalError} performs the same free.</li>
 * <li>{@code indicatorBarInit} is NOT ported: the indicator bar (message
 * list box window, custom sfall tags) has no Java home yet. C returns 0 for
 * it here, so the ported {@code return 0} still matches; the call lands with
 * the interfaceBar/displayMonitor notes below.</li>
 * <li>{@code displayMonitorInit} is NOT ported
 * ({@code src/display_monitor.cc} unported); it is a pure window/FRM
 * creation step whose result is unused by the vectors.</li>
 * <li>{@code interfaceBarFree} (indicator-bar window) is NOT ported; see
 * {@link #interfaceFree}.</li>
 * <li>{@code quickToolbarSetEnabled}/{@code quickToolbarInit} land on the
 * existing {@link QuickToolbar} iOS hit-test seam, which has no init state
 * (no-op, exactly like the C inline fallbacks that seam ports).</li>
 * <li>Item state: {@code InterfaceItemState} ({@code gInterfaceItemStates})
 * is a two-field holder here - only {@code intface_init_items}' sentinel
 * item pointers exist in this slice; hit modes and the action-button
 * render path arrive with the item UI phase.</li>
 * <li>Sound seam: {@link #soundPlayFile} stands in for
 * {@code soundPlayFile} in {@code game_sound.cc} (not ported), default
 * no-op returning -1 - the same {@code DBox} convention.</li>
 * </ul>
 */
public final class Interface {

    private Interface() {
    }

    /** Port of {@code INTERFACE_BAR_WIDTH} (interface.h). */
    public static final int INTERFACE_BAR_WIDTH = 640;

    /** Port of {@code INTERFACE_BAR_HEIGHT} (interface.h). */
    public static final int INTERFACE_BAR_HEIGHT = 100;

    /** Port of {@code INTERFACE_ITEM_ACTION_BUTTON_WIDTH} (interface.cc). */
    public static final int INTERFACE_ITEM_ACTION_BUTTON_WIDTH = 188;

    /** Port of {@code INTERFACE_ITEM_ACTION_BUTTON_HEIGHT} (interface.cc). */
    public static final int INTERFACE_ITEM_ACTION_BUTTON_HEIGHT = 67;

    /** Width and height of AP bulbs (interface.cc {@code kApBarBulbSize}). */
    private static final int AP_BAR_BULB_SIZE = 5;

    /** Horizontal margin between AP bulbs ({@code kApBarBulbMargin}). */
    private static final int AP_BAR_BULB_MARGIN = 4;

    /** Port of {@code kApBarMaxBulbs}. */
    private static final int AP_BAR_MAX_BULBS = 16;

    /** Port of {@code kApBarMaxWidth}. */
    private static final int AP_BAR_MAX_WIDTH = (AP_BAR_BULB_SIZE + AP_BAR_BULB_MARGIN) * AP_BAR_MAX_BULBS;

    /** Port of {@code kApBarYOffset}. */
    private static final int AP_BAR_Y_OFFSET = 14;

    // 0x519024 interfaceWindow
    public static int interfaceBarWindow = -1;

    /** 0x519028 gInterfaceBarContentOffset. */
    public static int interfaceBarContentOffset = 0;

    /** {@code gInterfaceBarWidth} (BSS 800; falls back to 640 when narrow). */
    public static int interfaceBarWidth = 800;

    /** {@code gInterfaceBarIsCustom} (BSS false). */
    public static boolean interfaceBarIsCustom = false;

    // 0x518F08 insideInit
    private static boolean interfaceBarInitialized = false;

    // 0x518F10 intfaceEnabled
    private static boolean interfaceBarEnabled = false;

    // 0x518F14 intfaceHidden
    private static boolean interfaceBarHidden = false;

    // 0x518F7C itemButtonRect (combat mode curtains rect).
    private static final Geometry.Rect interfaceBarEndButtonsRect = new Geometry.Rect();

    // 0x518F7C itemButtonRect
    private static final Geometry.Rect interfaceBarMainActionRect = new Geometry.Rect();

    // 0x518F18-0x518F9C buttons.
    private static int inventoryButton = -1;
    private static int optionsButton = -1;
    private static int skilldexButton = -1;
    private static int mapButton = -1;
    private static int pipboyButton = -1;
    private static int characterButton = -1;
    private static int singleAttackButton = -1;
    private static int changeHandsButton = -1;

    // 0x518F7C Hand
    private static Inventory.Hand interfaceCurrentHand = Inventory.Hand.HAND_LEFT;

    // 0x59D3F4 interfaceBuffer (BSS null).
    private static byte[] interfaceWindowBuffer;

    // 0x518FD4 movePointRect
    private static final Geometry.Rect apBarRect = new Geometry.Rect();

    // 0x59D40C movePointBackground
    private static final byte[] apBarBackgroundData = new byte[AP_BAR_MAX_WIDTH * AP_BAR_BULB_SIZE];

    private static int apBarMaxAP;
    private static int apBarWidth;

    /** AP bar offset relative to the content area of the bar window. */
    private static int apBarXOffset;

    // 0x59A2B4 itemButtonUp
    private static final byte[] itemButtonUp =
            new byte[INTERFACE_ITEM_ACTION_BUTTON_WIDTH * INTERFACE_ITEM_ACTION_BUTTON_HEIGHT];

    // 0x5970D4 itemButtonDown
    private static final byte[] itemButtonDown =
            new byte[INTERFACE_ITEM_ACTION_BUTTON_WIDTH * INTERFACE_ITEM_ACTION_BUTTON_HEIGHT];

    // 0x5970F8 Hand: item state per hand (intface_init_items sentinel only).
    private static final Object[] interfaceItemItems = { null, null };

    private static final Art.FrmImage inventoryButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage inventoryButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage optionsButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage optionsButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage skilldexButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage skilldexButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage skilldexButtonMaskFrmImage = new Art.FrmImage();
    private static final Art.FrmImage mapButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage mapButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage mapButtonMaskFrmImage = new Art.FrmImage();
    private static final Art.FrmImage pipboyButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage pipboyButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage characterButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage characterButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage changeHandsButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage changeHandsButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage changeHandsButtonMaskFrmImage = new Art.FrmImage();
    private static final Art.FrmImage itemButtonNormalFrmImage = new Art.FrmImage();
    private static final Art.FrmImage itemButtonPressedFrmImage = new Art.FrmImage();
    private static final Art.FrmImage itemButtonDisabledFrmImage = new Art.FrmImage();
    private static final Art.FrmImage numbersFrmImage = new Art.FrmImage();
    private static final Art.FrmImage greenLightFrmImage = new Art.FrmImage();
    private static final Art.FrmImage yellowLightFrmImage = new Art.FrmImage();
    private static final Art.FrmImage redLightFrmImage = new Art.FrmImage();

    // gCustomInterfaceBarBackground (BSS null).
    private static Art.ArtData customInterfaceBarBackground;

    // sfall side panels (BSS -1).
    private static int sidePanelsLeadingWindow = -1;
    private static int sidePanelsTrailingWindow = -1;

    /**
     * Sound seam for {@code soundPlayFile} (game_sound.cc not ported; the
     * default no-op returning -1 is what C returns when sound is
     * uninitialized). Same convention as {@link DBox#soundPlayFile}.
     */
    public static Function<String, Integer> soundPlayFile = name -> -1;

    private static void gsoundMedButtPress(int btn, int keyCode) {
        soundPlayFile.apply("ib2p1xx1");
    }

    private static void gsoundMedButtRelease(int btn, int keyCode) {
        soundPlayFile.apply("ib2lu1x1");
    }

    private static void gsoundLrgButtPress(int btn, int keyCode) {
        soundPlayFile.apply("ib3p1xx1");
    }

    private static void gsoundLrgButtRelease(int btn, int keyCode) {
        soundPlayFile.apply("ib3lu1x1");
    }

    private static FrmId ifaceFrm(int frameId) {
        return FrmId.ofType(ObjectType.INTERFACE, frameId);
    }

    private static Draw.Buffer interfaceWindowBuf2D() {
        return new Draw.Buffer(interfaceWindowBuffer, interfaceBarWidth, INTERFACE_BAR_HEIGHT);
    }

    private static Draw.Buffer apBarBackgroundBuf2D() {
        return new Draw.Buffer(apBarBackgroundData, apBarWidth, AP_BAR_BULB_SIZE);
    }

    /** Port of {@code customInterfaceBarInit}. */
    static void customInterfaceBarInit() {
        interfaceBarWidth = Settings.settings.ui.iface_bar_width;
        interfaceBarContentOffset = interfaceBarWidth - INTERFACE_BAR_WIDTH;
        if (interfaceBarContentOffset > 0) {
            if (Svga.screenGetWidth() > INTERFACE_BAR_WIDTH
                    && interfaceBarWidth <= Svga.screenGetWidth()) {
                String path = String.format("art\\intrface\\HR_IFACE_%d.FRM", interfaceBarWidth);

                customInterfaceBarBackground = Art.artLoad(path);
            } else {
                Debug.print(
                        "\nINTRFACE: Custom interface bar width (%d) is greater than screen width (%d). Using default interface bar.\n",
                        interfaceBarWidth, Svga.screenGetWidth());
            }
        }

        if (customInterfaceBarBackground != null) {
            interfaceBarIsCustom = true;
        } else {
            interfaceBarContentOffset = 0;
            interfaceBarWidth = INTERFACE_BAR_WIDTH;
            interfaceBarIsCustom = false;
        }
    }

    /** Port of {@code customInterfaceBarExit}. */
    static void customInterfaceBarExit() {
        // C internal_free()s the art here; the Java art handle is a plain
        // decoded object with no allocator, so dropping the reference is the
        // whole teardown (same as any ported artLoad owner).
        customInterfaceBarBackground = null;
    }

    /** Port of {@code customInterfaceBarGetBackgroundImageData}. */
    static byte[] customInterfaceBarGetBackgroundImageData() {
        if (!interfaceBarIsCustom) {
            return null;
        }

        return Art.artGetFrameData(customInterfaceBarBackground, 0, Rotation.NE.value());
    }

    /**
     * Port of {@code extendedApBarInitToWindow}: inits the AP bar offsets
     * from the custom AP bar setting, blits the extended AP bar art over
     * the interface bar pixels, and copies the bulb row into the static
     * {@code apBarBackgroundData}.
     */
    private static void extendedApBarInitToWindow() {
        final int apBarXOffsetOriginal = 316;
        final int apBarWidthOriginal = 90;
        final int apBarMaxApOriginal = 10;

        if (Settings.settings.ui.extend_ap_bar) {
            apBarMaxAP = AP_BAR_MAX_BULBS;
            apBarWidth = AP_BAR_MAX_WIDTH;
            // Shift X offset left according to the extra AP bar width.
            apBarXOffset = apBarXOffsetOriginal - (AP_BAR_MAX_WIDTH - apBarWidthOriginal) / 2;
        } else {
            apBarMaxAP = apBarMaxApOriginal;
            apBarWidth = apBarWidthOriginal;
            apBarXOffset = apBarXOffsetOriginal;
        }

        apBarRect.left = interfaceBarContentOffset + apBarXOffset;
        apBarRect.top = AP_BAR_Y_OFFSET;
        apBarRect.right = interfaceBarContentOffset + apBarXOffset + apBarWidth - 1;
        apBarRect.bottom = AP_BAR_Y_OFFSET + AP_BAR_BULB_SIZE - 1;

        Draw.Buffer ifaceBarBuf = interfaceWindowBuf2D();

        // Blit extended AP bar art into static window buffer, overwriting pixels from the interface bar FRM.
        if (Settings.settings.ui.extend_ap_bar) {
            Art.ArtData apBarArt = Art.artLoad("art\\intrface\\iface_apbar_e.frm");
            if (apBarArt != null) {
                byte[] apBarFrmBuf = Art.artGetFrameData(apBarArt, 0, Rotation.NE.value());
                if (apBarFrmBuf != null) {
                    int apBarBgXOffset = apBarXOffset - 23;
                    final int apBarBgYOffset = AP_BAR_Y_OFFSET - 4;

                    Draw.blitBuffer2D(
                            new Draw.Buffer(apBarFrmBuf, Art.artGetWidth(apBarArt, 0, Rotation.NE.value()),
                                    Art.artGetHeight(apBarArt, 0, Rotation.NE.value())),
                            ifaceBarBuf, interfaceBarContentOffset + apBarBgXOffset, apBarBgYOffset);
                }
            }
        }

        // Blit the thin bar of pixels covering the bulbs (that change color) into static apBarBackgroundData buffer.
        Draw.Buffer abBarBgBuf = apBarBackgroundBuf2D();
        Draw.blitBuffer2D(ifaceBarBuf, interfaceBarContentOffset + apBarXOffset, AP_BAR_Y_OFFSET,
                abBarBgBuf.width(), abBarBgBuf.height(), abBarBgBuf, 0, 0);
    }

    /** Port of {@code sidePanelsInit}. */
    private static void sidePanelsInit() {
        if (Settings.settings.ui.iface_bar_mode) {
            return;
        }

        int sideArtId = Settings.settings.ui.iface_bar_side_art;
        if (sideArtId <= 0) {
            return;
        }

        if (interfaceBarWidth >= Svga.screenGetWidth()) {
            return;
        }

        Geometry.Rect windowRect = new Geometry.Rect();
        WindowManager.windowGetRect(interfaceBarWindow, windowRect);

        sidePanelsLeadingWindow = WindowManager.windowCreate(0, windowRect.top, windowRect.left,
                windowRect.bottom - windowRect.top + 1, Color.COLOR_FIRST,
                WindowManager.WindowFlags.WINDOW_HIDDEN.id()
                        | WindowManager.WindowFlags.WINDOW_DONT_MOVE_TOP.id());
        sidePanelsTrailingWindow = WindowManager.windowCreate(windowRect.right + 1, windowRect.top,
                Svga.screenGetWidth() - windowRect.right - 1, windowRect.bottom - windowRect.top + 1,
                Color.COLOR_FIRST,
                WindowManager.WindowFlags.WINDOW_HIDDEN.id()
                        | WindowManager.WindowFlags.WINDOW_DONT_MOVE_TOP.id());

        String path = String.format("art\\intrface\\HR_IFACELFT%d.frm", sideArtId);
        sidePanelsDraw(path, sidePanelsLeadingWindow, true);

        path = String.format("art\\intrface\\HR_IFACERHT%d.frm", sideArtId);
        sidePanelsDraw(path, sidePanelsTrailingWindow, false);
    }

    /** Port of {@code sidePanelsExit}. */
    static void sidePanelsExit() {
        if (sidePanelsTrailingWindow != -1) {
            WindowManager.windowDestroy(sidePanelsTrailingWindow);
            sidePanelsTrailingWindow = -1;
        }

        if (sidePanelsLeadingWindow != -1) {
            WindowManager.windowDestroy(sidePanelsLeadingWindow);
            sidePanelsLeadingWindow = -1;
        }
    }

    /** Port of {@code sidePanelsDraw}. */
    private static void sidePanelsDraw(String path, int win, boolean isLeading) {
        Art.ArtData image = Art.artLoad(path);
        if (image == null) {
            return;
        }

        byte[] imageData = Art.artGetFrameData(image, 0, Rotation.NE.value());

        int imageWidth = Art.artGetWidth(image, 0, Rotation.NE.value());
        int imageHeight = Art.artGetHeight(image, 0, Rotation.NE.value());

        int windowWidth = WindowManager.windowGetWidth(win);
        int windowHeight = WindowManager.windowGetHeight(win);

        int width = Math.min(imageWidth, windowWidth);

        int imageDataOfs = 0;
        if (!Settings.settings.ui.iface_bar_sides_ori && isLeading) {
            imageDataOfs = imageWidth - width;
        }

        if (Settings.settings.ui.iface_bar_sides_ori && !isLeading) {
            imageDataOfs = imageWidth - width;
        }

        // NOTE: C offsets the source pointer (imageData += imageWidth - width).
        // Draw.blitBufferToBufferStretch has no source-offset parameter, so
        // the sub-rect is materialized into a copy first (same pixels, same
        // pitch, same stretch).
        byte[] source = imageData;
        if (imageDataOfs != 0) {
            source = new byte[imageHeight * imageWidth];
            for (int row = 0; row < imageHeight; row++) {
                System.arraycopy(imageData, imageDataOfs + row * imageWidth, source, row * imageWidth,
                        width);
            }
        }

        Draw.blitBufferToBufferStretch(source, width, imageHeight, imageWidth,
                WindowManager.windowGetBuffer(win), windowWidth, windowHeight, windowWidth);
    }

    /**
     * Port of {@code interfaceInit} (0x45D880). Returns 0 on success and
     * {@code -1} when the bar window already exists or a required window /
     * FRM / button could not be created (each failure runs
     * {@link #interfaceFatalError}).
     */
    public static int interfaceInit() {
        if (interfaceBarWindow != -1) {
            return -1;
        }

        customInterfaceBarInit();

        interfaceBarEndButtonsRect.left = 580 + interfaceBarContentOffset;
        interfaceBarEndButtonsRect.top = 38;
        interfaceBarEndButtonsRect.right = 637 + interfaceBarContentOffset;
        interfaceBarEndButtonsRect.bottom = 96;

        interfaceBarMainActionRect.left = 267 + interfaceBarContentOffset;
        interfaceBarMainActionRect.top = 26;
        interfaceBarMainActionRect.right = 455 + interfaceBarContentOffset;
        interfaceBarMainActionRect.bottom = 93;

        interfaceBarInitialized = true;

        int interfaceBarWindowX = (Svga.screenGetWidth() - interfaceBarWidth) / 2;
        int interfaceBarWindowY = Svga.screenGetHeight() - INTERFACE_BAR_HEIGHT;

        interfaceBarWindow = WindowManager.windowCreate(interfaceBarWindowX, interfaceBarWindowY,
                interfaceBarWidth, INTERFACE_BAR_HEIGHT,
                Byte.toUnsignedInt(Color.colorTable[Color.rgb555(0x000000)]),
                WindowManager.WindowFlags.WINDOW_HIDDEN.id());
        if (interfaceBarWindow == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        interfaceWindowBuffer = WindowManager.windowGetBuffer(interfaceBarWindow);
        if (interfaceWindowBuffer == null) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        // Blit interface bar FRM into static window buffer.
        if (interfaceBarIsCustom) {
            Draw.blitBufferToBuffer(customInterfaceBarGetBackgroundImageData(), interfaceBarWidth,
                    INTERFACE_BAR_HEIGHT - 1, interfaceBarWidth, interfaceWindowBuffer,
                    interfaceBarWidth);
        } else {
            Art.FrmImage backgroundFrmImage = new Art.FrmImage();
            if (!backgroundFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.MainInterface.id()))) {
                backgroundFrmImage.close();
                return interfaceFatalError(-1);
            }

            Draw.blitBufferToBuffer(backgroundFrmImage.getData(), interfaceBarWidth,
                    INTERFACE_BAR_HEIGHT - 1, interfaceBarWidth, interfaceWindowBuffer,
                    interfaceBarWidth);
            backgroundFrmImage.unlock();
        }

        extendedApBarInitToWindow();

        if (!inventoryButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.InventoryButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!inventoryButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.InventoryButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        inventoryButton = WindowManager.buttonCreate(interfaceBarWindow, 211 + interfaceBarContentOffset, 40,
                32, 21, -1, -1, -1, Kb.KEY_LOWERCASE_I, inventoryButtonNormalFrmImage.getData(),
                inventoryButtonPressedFrmImage.getData(), null, 0);
        if (inventoryButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetCallbacks(inventoryButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!optionsButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.OptionsButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!optionsButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.OptionsButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        optionsButton = WindowManager.buttonCreate(interfaceBarWindow, 210 + interfaceBarContentOffset, 61,
                34, 34, -1, -1, -1, Kb.KEY_LOWERCASE_O, optionsButtonNormalFrmImage.getData(),
                optionsButtonPressedFrmImage.getData(), null, 0);
        if (optionsButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetCallbacks(optionsButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!skilldexButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!skilldexButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!skilldexButtonMaskFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        skilldexButton = WindowManager.buttonCreate(interfaceBarWindow, 523 + interfaceBarContentOffset, 6,
                22, 21, -1, -1, -1, Kb.KEY_LOWERCASE_S, skilldexButtonNormalFrmImage.getData(),
                skilldexButtonPressedFrmImage.getData(), null,
                WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());
        if (skilldexButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetMask(skilldexButton, skilldexButtonMaskFrmImage.getData());
        WindowManager.buttonSetCallbacks(skilldexButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!mapButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.AutomapButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!mapButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.AutomapButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!mapButtonMaskFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.AutomapButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        mapButton = WindowManager.buttonCreate(interfaceBarWindow, 526 + interfaceBarContentOffset, 39,
                41, 19, -1, -1, -1, Kb.KEY_TAB, mapButtonNormalFrmImage.getData(),
                mapButtonPressedFrmImage.getData(), null,
                WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());
        if (mapButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetMask(mapButton, mapButtonMaskFrmImage.getData());
        WindowManager.buttonSetCallbacks(mapButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!pipboyButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.PipBoyButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!pipboyButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.PipBoyButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        pipboyButton = WindowManager.buttonCreate(interfaceBarWindow, 526 + interfaceBarContentOffset, 77,
                41, 19, -1, -1, -1, Kb.KEY_LOWERCASE_P, pipboyButtonNormalFrmImage.getData(),
                pipboyButtonPressedFrmImage.getData(), null, 0);
        if (pipboyButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetMask(pipboyButton, mapButtonMaskFrmImage.getData());
        WindowManager.buttonSetCallbacks(pipboyButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!characterButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.CharacterButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!characterButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.CharacterButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        characterButton = WindowManager.buttonCreate(interfaceBarWindow, 526 + interfaceBarContentOffset, 58,
                41, 19, -1, -1, -1, Kb.KEY_LOWERCASE_C, characterButtonNormalFrmImage.getData(),
                characterButtonPressedFrmImage.getData(), null, 0);
        if (characterButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetMask(characterButton, mapButtonMaskFrmImage.getData());
        WindowManager.buttonSetCallbacks(characterButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!itemButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.SingleAttackBigUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!itemButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.SingleAttackBigDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!itemButtonDisabledFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.SingleAttackBigUpUnreadied.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        System.arraycopy(itemButtonNormalFrmImage.getData(), 0, itemButtonUp, 0, itemButtonUp.length);
        System.arraycopy(itemButtonPressedFrmImage.getData(), 0, itemButtonDown, 0, itemButtonDown.length);

        // NOTE: C passes the literal -20 as the mouse-up event code (no named
        // constant for it); the attack path reads button state, not the event.
        singleAttackButton = WindowManager.buttonCreate(interfaceBarWindow, 267 + interfaceBarContentOffset, 26,
                INTERFACE_ITEM_ACTION_BUTTON_WIDTH, INTERFACE_ITEM_ACTION_BUTTON_HEIGHT, -1, -1, -1, -20,
                itemButtonUp, itemButtonDown, null,
                WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());
        if (singleAttackButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetRightMouseCallbacks(singleAttackButton, -1, Kb.KEY_LOWERCASE_N, null, null);
        WindowManager.buttonSetCallbacks(singleAttackButton, Interface::gsoundLrgButtPress,
                Interface::gsoundLrgButtRelease);

        if (!changeHandsButtonNormalFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!changeHandsButtonPressedFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonDown.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!changeHandsButtonMaskFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.BigRedButtonUp.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        // Swap hands button
        changeHandsButton = WindowManager.buttonCreate(interfaceBarWindow, 218 + interfaceBarContentOffset, 6,
                22, 21, -1, -1, -1, Kb.KEY_LOWERCASE_B, changeHandsButtonNormalFrmImage.getData(),
                changeHandsButtonPressedFrmImage.getData(), null,
                WindowManager.ButtonFlags.BUTTON_FLAG_TRANSPARENT.id());
        if (changeHandsButton == -1) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        WindowManager.buttonSetMask(changeHandsButton, changeHandsButtonMaskFrmImage.getData());
        WindowManager.buttonSetCallbacks(changeHandsButton, Interface::gsoundMedButtPress,
                Interface::gsoundMedButtRelease);

        if (!numbersFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.HitPointsNumbers.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!greenLightFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.HealthGreenLight.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!yellowLightFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.HealthYellowLight.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        if (!redLightFrmImage.lock(ifaceFrm(ArtDefs.InterfaceFrameId.HealthRedLight.id()))) {
            // NOTE: Uninline.
            return interfaceFatalError(-1);
        }

        // NOTE: Unported: indicatorBarInit() (indicator bar window + sfall
        // custom tags); C returns 0 for it in this path.

        interfaceCurrentHand = Inventory.Hand.HAND_LEFT;

        // NOTE: Uninline.
        intfaceInitItems();

        // NOTE: Unported: displayMonitorInit() (src/display_monitor.cc).

        sidePanelsInit();

        interfaceBarEnabled = true;
        interfaceBarInitialized = false;
        interfaceBarHidden = true;

        // NOTE: quickToolbarSetEnabled/quickToolbarInit land on the QuickToolbar
        // hit-test seam, which carries no init state (no-op).

        return 0;
    }

    /** Port of {@code intface_init_items} (0x45FD2C). */
    private static int intfaceInitItems() {
        // NOTE: port of the upstream FIXME - these values are initialized with
        // -1 in C and never checked for -1; Java stores the null sentinel.
        interfaceItemItems[Inventory.Hand.HAND_LEFT.id()] = null;
        interfaceItemItems[Inventory.Hand.HAND_RIGHT.id()] = null;

        return 0;
    }

    /** Port of {@code intface_fatal_error} (0x461124). */
    private static int interfaceFatalError(int rc) {
        interfaceFree();

        return rc;
    }

    /** Port of {@code interfaceFree} (0x45E440), ported subset. */
    static void interfaceFree() {
        if (interfaceBarWindow != -1) {
            sidePanelsExit();

            redLightFrmImage.unlock();
            yellowLightFrmImage.unlock();
            greenLightFrmImage.unlock();

            numbersFrmImage.unlock();

            if (changeHandsButton != -1) {
                WindowManager.buttonDestroy(changeHandsButton);
                changeHandsButton = -1;
            }

        changeHandsButtonMaskFrmImage.unlock();
            changeHandsButtonPressedFrmImage.unlock();
            changeHandsButtonNormalFrmImage.unlock();

            if (singleAttackButton != -1) {
                WindowManager.buttonDestroy(singleAttackButton);
                singleAttackButton = -1;
            }

            itemButtonDisabledFrmImage.unlock();
            itemButtonPressedFrmImage.unlock();
            itemButtonNormalFrmImage.unlock();

            if (characterButton != -1) {
                WindowManager.buttonDestroy(characterButton);
                characterButton = -1;
            }

            characterButtonPressedFrmImage.unlock();
            characterButtonNormalFrmImage.unlock();

            if (pipboyButton != -1) {
                WindowManager.buttonDestroy(pipboyButton);
                pipboyButton = -1;
            }

            pipboyButtonPressedFrmImage.unlock();
            pipboyButtonNormalFrmImage.unlock();

            if (mapButton != -1) {
                WindowManager.buttonDestroy(mapButton);
                mapButton = -1;
            }

            mapButtonMaskFrmImage.unlock();
            mapButtonPressedFrmImage.unlock();
            mapButtonNormalFrmImage.unlock();

            if (skilldexButton != -1) {
                WindowManager.buttonDestroy(skilldexButton);
                skilldexButton = -1;
            }

            skilldexButtonMaskFrmImage.unlock();
            skilldexButtonPressedFrmImage.unlock();
            skilldexButtonNormalFrmImage.unlock();

            if (optionsButton != -1) {
                WindowManager.buttonDestroy(optionsButton);
                optionsButton = -1;
            }

            optionsButtonPressedFrmImage.unlock();
            optionsButtonNormalFrmImage.unlock();

            if (inventoryButton != -1) {
                WindowManager.buttonDestroy(inventoryButton);
                inventoryButton = -1;
            }

            inventoryButtonPressedFrmImage.unlock();
            inventoryButtonNormalFrmImage.unlock();

            WindowManager.windowDestroy(interfaceBarWindow);
            interfaceBarWindow = -1;
        }

        customInterfaceBarExit();

        // NOTE: Unported tail: interfaceBarFree() (indicator bar window).

        interfaceWindowBuffer = null;
    }

    /** Port of {@code interfaceBarEnabled}. */
    static boolean interfaceBarEnabled() {
        return interfaceBarEnabled;
    }

    /** Port of {@code interfaceBarHidden} accessor (C static read). */
    static boolean interfaceBarHidden() {
        return interfaceBarHidden;
    }

    /** Port of the {@code gInterfaceBarInitialized} static. */
    static boolean interfaceBarInitialized() {
        return interfaceBarInitialized;
    }

    /** Port of the {@code gInterfaceBarEndButtonsRect} static. */
    static Geometry.Rect interfaceBarEndButtonsRect() {
        return interfaceBarEndButtonsRect;
    }

    /** Port of the {@code gInterfaceBarMainActionRect} static. */
    static Geometry.Rect interfaceBarMainActionRect() {
        return interfaceBarMainActionRect;
    }

    /** Port of the {@code gInterfaceCurrentHand} static. */
    static Inventory.Hand interfaceGetCurrentHand() {
        return interfaceCurrentHand;
    }

    /** Port of the {@code apBarRect} static. */
    static Geometry.Rect apBarRect() {
        return apBarRect;
    }

    /** Port of the {@code apBarWidth} / {@code apBarMaxAP} statics. */
    static int apBarWidth() {
        return apBarWidth;
    }

    /** Port of the {@code apBarMaxAP} static. */
    static int apBarMaxAP() {
        return apBarMaxAP;
    }

    /** Port of the {@code gInterfaceItemStates[hand].item} sentinel (C: {@code (Object*)-1}). */
    static Object interfaceItem(Inventory.Hand hand) {
        return interfaceItemItems[hand.id()];
    }

    /** Port of the {@code gInterfaceBarWindow} read side. */
    static byte[] interfaceWindowBuffer() {
        return interfaceWindowBuffer;
    }

    /** Test-only full reset (mirrors a fresh process / C BSS state). */
    static void resetForTests() {
        interfaceFree();

        interfaceBarContentOffset = 0;
        interfaceBarWidth = 800;
        interfaceBarIsCustom = false;
        interfaceBarInitialized = false;
        interfaceBarEnabled = false;
        interfaceBarHidden = false;
        interfaceCurrentHand = Inventory.Hand.HAND_LEFT;
        apBarMaxAP = 0;
        apBarWidth = 0;
        apBarXOffset = 0;
        java.util.Arrays.fill(apBarBackgroundData, (byte) 0);
        java.util.Arrays.fill(itemButtonUp, (byte) 0);
        java.util.Arrays.fill(itemButtonDown, (byte) 0);
        interfaceItemItems[Inventory.Hand.HAND_LEFT.id()] = null;
        interfaceItemItems[Inventory.Hand.HAND_RIGHT.id()] = null;
        sidePanelsLeadingWindow = -1;
        sidePanelsTrailingWindow = -1;
        interfaceBarEndButtonsRect.left = 0;
        interfaceBarEndButtonsRect.top = 0;
        interfaceBarEndButtonsRect.right = 0;
        interfaceBarEndButtonsRect.bottom = 0;
        interfaceBarMainActionRect.left = 0;
        interfaceBarMainActionRect.top = 0;
        interfaceBarMainActionRect.right = 0;
        interfaceBarMainActionRect.bottom = 0;
        apBarRect.left = 0;
        apBarRect.top = 0;
        apBarRect.right = 0;
        apBarRect.bottom = 0;
        soundPlayFile = name -> -1;
    }
}