package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * A script API that records what the scripts ask for and answers with
 * fixed values.
 *
 * <p>This is what makes a script testable without the game: run the script
 * against this mock and assert on the recorded calls.
 *
 * <p>The values are rendered the way the trace is, so the trace is stable
 * across runs.
 */
public class MockScriptApi implements GameApi {

    /** One recorded call. */
    public static final class Call {
        public final String name;
        public final List<String> args;

        Call(String name, List<String> args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public String toString() {
            return name + "(" + String.join(",", args) + ")";
        }
    }

    private final List<Call> calls = new ArrayList<>();

    /** The calls made so far, in order. */
    public List<Call> calls() {
        return calls;
    }

    /** The calls made so far, one per line. */
    public String trace() {
        StringBuilder sb = new StringBuilder();
        for (Call call : calls) {
            sb.append(call).append('\n');
        }
        return sb.toString();
    }

    public void reset() {
        calls.clear();
    }

    private void record(String name, Object... args) {
        List<String> rendered = new ArrayList<>();
        for (Object arg : args) {
            rendered.add(render(arg));
        }
        calls.add(new Call(name, rendered));
    }

    private static String render(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof Program.ProgramValue) {
            Program.ProgramValue value = (Program.ProgramValue) arg;
            return String.format("%04X:%d", value.opcode & 0xFFFF, value.integerValue);
        }
        return arg.toString();
    }

    @Override
    public void opSayEnd() {
        record("opSayEnd");
        return;
    }

    @Override
    public void opSayStart() {
        record("opSayStart");
        return;
    }

    @Override
    public void opSayOption(Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayOption", a0, a1);
        return;
    }

    @Override
    public void opSayReply(Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayReply", a0, a1);
        return;
    }

    @Override
    public void opSayMessage(Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayMessage", a0, a1);
        return;
    }

    @Override
    public void opDisplay(String a0) {
        record("opDisplay", a0);
        return;
    }

    @Override
    public void giveExpPoints(int a0) {
        record("giveExpPoints", a0);
        return;
    }

    @Override
    public void scrReturn(int a0) {
        record("scrReturn", a0);
        return;
    }

    @Override
    public void playSfx(String a0) {
        record("playSfx", a0);
        return;
    }

    @Override
    public String objName(Object a0) {
        record("objName", a0);
        return "";
    }

    @Override
    public int getPcStat(int a0) {
        record("getPcStat", a0);
        return 0;
    }

    @Override
    public Object tileContainsPidObj(int a0, int a1, int a2) {
        record("tileContainsPidObj", a0, a1, a2);
        return null;
    }

    @Override
    public void setMapStart(int a0, int a1, int a2, int a3) {
        record("setMapStart", a0, a1, a2, a3);
        return;
    }

    @Override
    public void overrideMapStart(int a0, int a1, int a2, int a3) {
        record("overrideMapStart", a0, a1, a2, a3);
        return;
    }

    @Override
    public int hasSkill(Object a0, int a1) {
        record("hasSkill", a0, a1);
        return 0;
    }

    @Override
    public int usingSkill(Object a0, int a1) {
        record("usingSkill", a0, a1);
        return 0;
    }

    @Override
    public int rollVsSkill(Object a0, int a1, int a2) {
        record("rollVsSkill", a0, a1, a2);
        return 0;
    }

    @Override
    public int doCheck(Object a0, int a1, int a2) {
        record("doCheck", a0, a1, a2);
        return 0;
    }

    @Override
    public int success(int a0) {
        record("success", a0);
        return 0;
    }

    @Override
    public int critical(int a0) {
        record("critical", a0);
        return 0;
    }

    @Override
    public void markAreaKnown(int a0, int a1, int a2) {
        record("markAreaKnown", a0, a1, a2);
        return;
    }

    @Override
    public int random(int a0, int a1) {
        record("random", a0, a1);
        return 0;
    }

    @Override
    public int moveTo(Object a0, int a1, int a2) {
        record("moveTo", a0, a1, a2);
        return 0;
    }

    @Override
    public Object createObject(int a0, int a1, int a2, int a3) {
        record("createObject", a0, a1, a2, a3);
        return null;
    }

    @Override
    public void displayMsg(String a0) {
        record("displayMsg", a0);
        return;
    }

    @Override
    public void scriptOverrides() {
        record("scriptOverrides");
        return;
    }

    @Override
    public int objIsCarryingObj(Object a0, int a1) {
        record("objIsCarryingObj", a0, a1);
        return 0;
    }

    @Override
    public int tileContainsObjPid(int a0, int a1, int a2) {
        record("tileContainsObjPid", a0, a1, a2);
        return 0;
    }

    @Override
    public Object selfObj() {
        record("selfObj");
        return null;
    }

    @Override
    public Object sourceObj() {
        record("sourceObj");
        return null;
    }

    @Override
    public Object targetObj() {
        record("targetObj");
        return null;
    }

    @Override
    public Object opGetDude() {
        record("opGetDude");
        return null;
    }

    @Override
    public Object objBeingUsedWith() {
        record("objBeingUsedWith");
        return null;
    }

    @Override
    public Program.ProgramValue getLocalVar(int a0) {
        record("getLocalVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setLocalVar(int a0, Program.ProgramValue a1) {
        record("setLocalVar", a0, a1);
        return;
    }

    @Override
    public Program.ProgramValue getMapVar(int a0) {
        record("getMapVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setMapVar(int a0, Program.ProgramValue a1) {
        record("setMapVar", a0, a1);
        return;
    }

    @Override
    public Program.ProgramValue getGlobalVar(int a0) {
        record("getGlobalVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setGlobalVar(int a0, Program.ProgramValue a1) {
        record("setGlobalVar", a0, a1);
        return;
    }

    @Override
    public int scriptAction() {
        record("scriptAction");
        return 0;
    }

    @Override
    public int objType(Object a0) {
        record("objType", a0);
        return 0;
    }

    @Override
    public int itemSubtype(Object a0) {
        record("itemSubtype", a0);
        return 0;
    }

    @Override
    public int getCritterStat(Object a0, int a1) {
        record("getCritterStat", a0, a1);
        return 0;
    }

    @Override
    public int setCritterStat(Object a0, int a1, int a2) {
        record("setCritterStat", a0, a1, a2);
        return 0;
    }

    @Override
    public void animateStandObj(Object a0) {
        record("animateStandObj", a0);
        return;
    }

    @Override
    public void animateStandReverseObj(Object a0) {
        record("animateStandReverseObj", a0);
        return;
    }

    @Override
    public void animateMoveObjToTile(Object a0, int a1, int a2) {
        record("animateMoveObjToTile", a0, a1, a2);
        return;
    }

    @Override
    public int tileInTileRect(int a0, int a1, int a2, int a3, int a4) {
        record("tileInTileRect", a0, a1, a2, a3, a4);
        return 0;
    }

    @Override
    public void attack(Object a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7) {
        record("attack", a0, a1, a2, a3, a4, a5, a6, a7);
        return;
    }

    @Override
    public int tileDistance(int a0, int a1) {
        record("tileDistance", a0, a1);
        return 0;
    }

    @Override
    public int tileDistanceObjs(Object a0, Object a1) {
        record("tileDistanceObjs", a0, a1);
        return 0;
    }

    @Override
    public int tileNum(Object a0) {
        record("tileNum", a0);
        return 0;
    }

    @Override
    public int tileNumInDirection(int a0, int a1, int a2) {
        record("tileNumInDirection", a0, a1, a2);
        return 0;
    }

    @Override
    public void dropObj(Object a0) {
        record("dropObj", a0);
        return;
    }

    @Override
    public void addObjToInven(Object a0, Object a1) {
        record("addObjToInven", a0, a1);
        return;
    }

    @Override
    public void rmObjFromInven(Object a0, Object a1) {
        record("rmObjFromInven", a0, a1);
        return;
    }

    @Override
    public void wieldObjCritter(Object a0, Object a1) {
        record("wieldObjCritter", a0, a1);
        return;
    }

    @Override
    public void useObj(Object a0) {
        record("useObj", a0);
        return;
    }

    @Override
    public int objCanSeeObj(Object a0, Object a1) {
        record("objCanSeeObj", a0, a1);
        return 0;
    }

    @Override
    public void startGdialog(int a0, Object a1, int a2, int a3, int a4) {
        record("startGdialog", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public void endGdialog() {
        record("endGdialog");
        return;
    }

    @Override
    public Program.ProgramValue metarule3(int a0, Program.ProgramValue a1, Program.ProgramValue a2, Program.ProgramValue a3) {
        record("metarule3", a0, a1, a2, a3);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setObjVisibility(Object a0, int a1) {
        record("setObjVisibility", a0, a1);
        return;
    }

    @Override
    public void loadMap(Program.ProgramValue a0, int a1) {
        record("loadMap", a0, a1);
        return;
    }

    @Override
    public void wmAreaSetPos(int a0, int a1, int a2) {
        record("wmAreaSetPos", a0, a1, a2);
        return;
    }

    @Override
    public void setExitGrids(int a0, int a1, int a2, int a3, int a4) {
        record("setExitGrids", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public int animBusy(Object a0) {
        record("animBusy", a0);
        return 0;
    }

    @Override
    public int critterHeal(Object a0, int a1) {
        record("critterHeal", a0, a1);
        return 0;
    }

    @Override
    public void setLightLevel(int a0) {
        record("setLightLevel", a0);
        return;
    }

    @Override
    public int gameTime() {
        record("gameTime");
        return 0;
    }

    @Override
    public int elevation(Object a0) {
        record("elevation", a0);
        return 0;
    }

    @Override
    public void killCritter(Object a0, int a1) {
        record("killCritter", a0, a1);
        return;
    }

    @Override
    public void killCritterType(int a0, int a1) {
        record("killCritterType", a0, a1);
        return;
    }

    @Override
    public void critterDamage(Object a0, int a1, int a2) {
        record("critterDamage", a0, a1, a2);
        return;
    }

    @Override
    public void addTimerEvent(Object a0, int a1, int a2) {
        record("addTimerEvent", a0, a1, a2);
        return;
    }

    @Override
    public void rmTimerEvent(Object a0) {
        record("rmTimerEvent", a0);
        return;
    }

    @Override
    public int gameTicks(int a0) {
        record("gameTicks", a0);
        return 0;
    }

    @Override
    public int hasTrait(int a0, Object a1, int a2) {
        record("hasTrait", a0, a1, a2);
        return 0;
    }

    @Override
    public void destroyObject(Object a0) {
        record("destroyObject", a0);
        return;
    }

    @Override
    public int objCanHearObj(Object a0, Object a1) {
        record("objCanHearObj", a0, a1);
        return 0;
    }

    @Override
    public int gameTimeHour() {
        record("gameTimeHour");
        return 0;
    }

    @Override
    public int fixedParam() {
        record("fixedParam");
        return 0;
    }

    @Override
    public int tileIsVisible(int a0) {
        record("tileIsVisible", a0);
        return 0;
    }

    @Override
    public void dialogueSystemEnter() {
        record("dialogueSystemEnter");
        return;
    }

    @Override
    public int actionBeingUsed() {
        record("actionBeingUsed");
        return 0;
    }

    @Override
    public int critterState(Object a0) {
        record("critterState", a0);
        return 0;
    }

    @Override
    public void gameTimeAdvance(int a0) {
        record("gameTimeAdvance", a0);
        return;
    }

    @Override
    public void radiationInc(Object a0, int a1) {
        record("radiationInc", a0, a1);
        return;
    }

    @Override
    public void radiationDec(Object a0, int a1) {
        record("radiationDec", a0, a1);
        return;
    }

    @Override
    public int critterAttemptPlacement(Object a0, int a1, int a2) {
        record("critterAttemptPlacement", a0, a1, a2);
        return 0;
    }

    @Override
    public int objPid(Object a0) {
        record("objPid", a0);
        return 0;
    }

    @Override
    public int curMapIndex() {
        record("curMapIndex");
        return 0;
    }

    @Override
    public int critterAddTrait(Object a0, int a1, int a2, int a3) {
        record("critterAddTrait", a0, a1, a2, a3);
        return 0;
    }

    @Override
    public int critterRmTrait(Object a0, int a1, int a2, int a3) {
        record("critterRmTrait", a0, a1, a2, a3);
        return 0;
    }

    @Override
    public Program.ProgramValue protoData(int a0, int a1) {
        record("protoData", a0, a1);
        return new Program.ProgramValue(-1);
    }

    @Override
    public String messageStr(int a0, int a1) {
        record("messageStr", a0, a1);
        return "";
    }

    @Override
    public Program.ProgramValue critterInvenObj(Object a0, int a1) {
        record("critterInvenObj", a0, a1);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void objSetLightLevel(Object a0, int a1, int a2) {
        record("objSetLightLevel", a0, a1, a2);
        return;
    }

    @Override
    public Object invenCmds(Object a0, int a1, int a2) {
        record("invenCmds", a0, a1, a2);
        return null;
    }

    @Override
    public void floatMsg(Program.ProgramValue a0, int a1) {
        record("floatMsg", a0, a1);
        return;
    }

    @Override
    public int metarule(int a0, Program.ProgramValue a1) {
        record("metarule", a0, a1);
        return 0;
    }

    @Override
    public void anim(Object a0, int a1, int a2, Program.ProgramValue a3) {
        record("anim", a0, a1, a2, a3);
        return;
    }

    @Override
    public Object objCarryingPidObj(Object a0, int a1) {
        record("objCarryingPidObj", a0, a1);
        return null;
    }

    @Override
    public void regAnimFunc(int a0, Program.ProgramValue a1) {
        record("regAnimFunc", a0, a1);
        return;
    }

    @Override
    public void regAnimAnimate(Object a0, int a1, int a2) {
        record("regAnimAnimate", a0, a1, a2);
        return;
    }

    @Override
    public void regAnimAnimateReverse(Object a0, int a1, int a2) {
        record("regAnimAnimateReverse", a0, a1, a2);
        return;
    }

    @Override
    public void regAnimObjMoveToTile(Object a0, int a1, int a2) {
        record("regAnimObjMoveToTile", a0, a1, a2);
        return;
    }

    @Override
    public void playGmovie(int a0) {
        record("playGmovie", a0);
        return;
    }

    @Override
    public void addMultObjsToInven(Object a0, Object a1, int a2) {
        record("addMultObjsToInven", a0, a1, a2);
        return;
    }

    @Override
    public int rmMultObjsFromInven(Object a0, Object a1, int a2) {
        record("rmMultObjsFromInven", a0, a1, a2);
        return 0;
    }

    @Override
    public int month() {
        record("month");
        return 0;
    }

    @Override
    public int day() {
        record("day");
        return 0;
    }

    @Override
    public void explosion(int a0, int a1, int a2) {
        record("explosion", a0, a1, a2);
        return;
    }

    @Override
    public void opGsayStart() {
        record("opGsayStart");
        return;
    }

    @Override
    public void opGsayEnd() {
        record("opGsayEnd");
        return;
    }

    @Override
    public void gsayReply(int a0, Program.ProgramValue a1) {
        record("gsayReply", a0, a1);
        return;
    }

    @Override
    public void gsayOption(int a0, Program.ProgramValue a1, Program.ProgramValue a2, int a3) {
        record("gsayOption", a0, a1, a2, a3);
        return;
    }

    @Override
    public void gsayMessage(int a0, Program.ProgramValue a1, int a2) {
        record("gsayMessage", a0, a1, a2);
        return;
    }

    @Override
    public void giqOption(int a0, int a1, Program.ProgramValue a2, Program.ProgramValue a3, int a4) {
        record("giqOption", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public void poison(Object a0, int a1) {
        record("poison", a0, a1);
        return;
    }

    @Override
    public int getPoison(Object a0) {
        record("getPoison", a0);
        return 0;
    }

    @Override
    public void partyAdd(Object a0) {
        record("partyAdd", a0);
        return;
    }

    @Override
    public void partyRemove(Object a0) {
        record("partyRemove", a0);
        return;
    }

    @Override
    public void regAnimAnimateForever(Object a0, int a1) {
        record("regAnimAnimateForever", a0, a1);
        return;
    }

    @Override
    public void critterInjure(Object a0, int a1) {
        record("critterInjure", a0, a1);
        return;
    }

    @Override
    public int isInCombat() {
        record("isInCombat");
        return 0;
    }

    @Override
    public void gdialogBarter(int a0) {
        record("gdialogBarter", a0);
        return;
    }

    @Override
    public void invenUnwield() {
        record("invenUnwield");
        return;
    }

    @Override
    public int objIsLocked(Object a0) {
        record("objIsLocked", a0);
        return 0;
    }

    @Override
    public void objLock(Object a0) {
        record("objLock", a0);
        return;
    }

    @Override
    public void objUnlock(Object a0) {
        record("objUnlock", a0);
        return;
    }

    @Override
    public int objIsOpen(Object a0) {
        record("objIsOpen", a0);
        return 0;
    }

    @Override
    public void objOpen(Object a0) {
        record("objOpen", a0);
        return;
    }

    @Override
    public void objClose(Object a0) {
        record("objClose", a0);
        return;
    }

    @Override
    public void gameUiDisable() {
        record("gameUiDisable");
        return;
    }

    @Override
    public void gameUiEnable() {
        record("gameUiEnable");
        return;
    }

    @Override
    public void gfadeOut(int a0) {
        record("gfadeOut", a0);
        return;
    }

    @Override
    public void gfadeIn(int a0) {
        record("gfadeIn", a0);
        return;
    }

    @Override
    public int itemCapsTotal(Object a0) {
        record("itemCapsTotal", a0);
        return 0;
    }

    @Override
    public int itemCapsAdjust(Object a0, int a1) {
        record("itemCapsAdjust", a0, a1);
        return 0;
    }

    @Override
    public void regAnimPlaySfx(Object a0, String a1, int a2) {
        record("regAnimPlaySfx", a0, a1, a2);
        return;
    }

    @Override
    public void critterModSkill(Object a0, int a1, int a2) {
        record("critterModSkill", a0, a1, a2);
        return;
    }

    @Override
    public String sfxBuildCharName(Object a0, int a1, int a2) {
        record("sfxBuildCharName", a0, a1, a2);
        return "";
    }

    @Override
    public String sfxBuildWeaponName(int a0, Object a1, int a2, Object a3) {
        record("sfxBuildWeaponName", a0, a1, a2, a3);
        return "";
    }

    @Override
    public void attackSetup(Object a0, Object a1) {
        record("attackSetup", a0, a1);
        return;
    }

    @Override
    public int destroyMultObjs(Object a0, int a1) {
        record("destroyMultObjs", a0, a1);
        return 0;
    }

    @Override
    public void useObjOnObj(Object a0, Object a1) {
        record("useObjOnObj", a0, a1);
        return;
    }

    @Override
    public void endgameSlideshow() {
        record("endgameSlideshow");
        return;
    }

    @Override
    public void moveObjInvenToObj(Object a0, Object a1) {
        record("moveObjInvenToObj", a0, a1);
        return;
    }

    @Override
    public int objArtFid(Object a0) {
        record("objArtFid", a0);
        return 0;
    }

    @Override
    public int artAnim(int a0) {
        record("artAnim", a0);
        return 0;
    }

    @Override
    public Object partyMemberObj(int a0) {
        record("partyMemberObj", a0);
        return null;
    }

    @Override
    public int rotationToTile(int a0, int a1) {
        record("rotationToTile", a0, a1);
        return 0;
    }

    @Override
    public void jamLock(Object a0) {
        record("jamLock", a0);
        return;
    }

    @Override
    public void gdialogSetBarterMod(int a0) {
        record("gdialogSetBarterMod", a0);
        return;
    }

    @Override
    public int objOnScreen(Object a0) {
        record("objOnScreen", a0);
        return 0;
    }

    @Override
    public int critterIsFleeing(Object a0) {
        record("critterIsFleeing", a0);
        return 0;
    }

    @Override
    public void critterSetFleeState(Object a0, int a1) {
        record("critterSetFleeState", a0, a1);
        return;
    }

    @Override
    public void terminateCombat() {
        record("terminateCombat");
        return;
    }

    @Override
    public void debugMsg(String a0) {
        record("debugMsg", a0);
        return;
    }

    @Override
    public void critterStopAttacking(Object a0) {
        record("critterStopAttacking", a0);
        return;
    }

}
