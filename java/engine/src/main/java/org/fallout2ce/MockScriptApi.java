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
public class MockScriptApi implements ScriptApi {

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
    public void opSayEnd(Program program) {
        record("opSayEnd");
        return;
    }

    @Override
    public void opSayStart(Program program) {
        record("opSayStart");
        return;
    }

    @Override
    public void opSayOption(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayOption", a0, a1);
        return;
    }

    @Override
    public void opSayReply(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayReply", a0, a1);
        return;
    }

    @Override
    public void opSayMessage(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        record("opSayMessage", a0, a1);
        return;
    }

    @Override
    public void opDisplay(Program program, String a0) {
        record("opDisplay", a0);
        return;
    }

    @Override
    public void giveExpPoints(Program program, int a0) {
        record("giveExpPoints", a0);
        return;
    }

    @Override
    public void scrReturn(Program program, int a0) {
        record("scrReturn", a0);
        return;
    }

    @Override
    public void playSfx(Program program, String a0) {
        record("playSfx", a0);
        return;
    }

    @Override
    public String objName(Program program, Object a0) {
        record("objName", a0);
        return "";
    }

    @Override
    public int getPcStat(Program program, int a0) {
        record("getPcStat", a0);
        return 0;
    }

    @Override
    public Object tileContainsPidObj(Program program, int a0, int a1) {
        record("tileContainsPidObj", a0, a1);
        return null;
    }

    @Override
    public void setMapStart(Program program, int a0, int a1, int a2, int a3) {
        record("setMapStart", a0, a1, a2, a3);
        return;
    }

    @Override
    public void overrideMapStart(Program program, int a0, int a1, int a2, int a3) {
        record("overrideMapStart", a0, a1, a2, a3);
        return;
    }

    @Override
    public int hasSkill(Program program, Object a0, int a1) {
        record("hasSkill", a0, a1);
        return 0;
    }

    @Override
    public int usingSkill(Program program, Object a0, int a1) {
        record("usingSkill", a0, a1);
        return 0;
    }

    @Override
    public int rollVsSkill(Program program, Object a0, int a1, int a2) {
        record("rollVsSkill", a0, a1, a2);
        return 0;
    }

    @Override
    public int doCheck(Program program, Object a0, int a1, int a2) {
        record("doCheck", a0, a1, a2);
        return 0;
    }

    @Override
    public int success(Program program, int a0) {
        record("success", a0);
        return 0;
    }

    @Override
    public int critical(Program program, int a0) {
        record("critical", a0);
        return 0;
    }

    @Override
    public void markAreaKnown(Program program, int a0, int a1, int a2) {
        record("markAreaKnown", a0, a1, a2);
        return;
    }

    @Override
    public int random(Program program) {
        record("random");
        return 0;
    }

    @Override
    public int moveTo(Program program, Object a0, int a1, int a2) {
        record("moveTo", a0, a1, a2);
        return 0;
    }

    @Override
    public Object createObject(Program program, int a0, int a1, int a2) {
        record("createObject", a0, a1, a2);
        return null;
    }

    @Override
    public void displayMsg(Program program, String a0) {
        record("displayMsg", a0);
        return;
    }

    @Override
    public void scriptOverrides(Program program) {
        record("scriptOverrides");
        return;
    }

    @Override
    public int objIsCarryingObj(Program program, Object a0) {
        record("objIsCarryingObj", a0);
        return 0;
    }

    @Override
    public int tileContainsObjPid(Program program, int a0, int a1) {
        record("tileContainsObjPid", a0, a1);
        return 0;
    }

    @Override
    public Object selfObj(Program program) {
        record("selfObj");
        return null;
    }

    @Override
    public Object sourceObj(Program program) {
        record("sourceObj");
        return null;
    }

    @Override
    public Object targetObj(Program program) {
        record("targetObj");
        return null;
    }

    @Override
    public Object opGetDude(Program program) {
        record("opGetDude");
        return null;
    }

    @Override
    public Object objBeingUsedWith(Program program) {
        record("objBeingUsedWith");
        return null;
    }

    @Override
    public Program.ProgramValue getLocalVar(Program program, int a0) {
        record("getLocalVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setLocalVar(Program program, int a0, Program.ProgramValue a1) {
        record("setLocalVar", a0, a1);
        return;
    }

    @Override
    public Program.ProgramValue getMapVar(Program program, int a0) {
        record("getMapVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setMapVar(Program program, int a0, Program.ProgramValue a1) {
        record("setMapVar", a0, a1);
        return;
    }

    @Override
    public Program.ProgramValue getGlobalVar(Program program, int a0) {
        record("getGlobalVar", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setGlobalVar(Program program, int a0, Program.ProgramValue a1) {
        record("setGlobalVar", a0, a1);
        return;
    }

    @Override
    public int scriptAction(Program program) {
        record("scriptAction");
        return 0;
    }

    @Override
    public int objType(Program program, Object a0) {
        record("objType", a0);
        return 0;
    }

    @Override
    public int itemSubtype(Program program, Object a0) {
        record("itemSubtype", a0);
        return 0;
    }

    @Override
    public int getCritterStat(Program program, Object a0, int a1) {
        record("getCritterStat", a0, a1);
        return 0;
    }

    @Override
    public int setCritterStat(Program program, Object a0, int a1, int a2) {
        record("setCritterStat", a0, a1, a2);
        return 0;
    }

    @Override
    public void animateStandObj(Program program, Object a0) {
        record("animateStandObj", a0);
        return;
    }

    @Override
    public void animateStandReverseObj(Program program, Object a0) {
        record("animateStandReverseObj", a0);
        return;
    }

    @Override
    public void animateMoveObjToTile(Program program, Object a0, int a1, int a2) {
        record("animateMoveObjToTile", a0, a1, a2);
        return;
    }

    @Override
    public int tileInTileRect(Program program) {
        record("tileInTileRect");
        return 0;
    }

    @Override
    public void attack(Program program) {
        record("attack");
        return;
    }

    @Override
    public int tileDistance(Program program, int a0, int a1) {
        record("tileDistance", a0, a1);
        return 0;
    }

    @Override
    public int tileDistanceObjs(Program program, Object a0, Object a1) {
        record("tileDistanceObjs", a0, a1);
        return 0;
    }

    @Override
    public int tileNum(Program program, Object a0) {
        record("tileNum", a0);
        return 0;
    }

    @Override
    public int tileNumInDirection(Program program, int a0, int a1, int a2) {
        record("tileNumInDirection", a0, a1, a2);
        return 0;
    }

    @Override
    public void dropObj(Program program, Object a0) {
        record("dropObj", a0);
        return;
    }

    @Override
    public void addObjToInven(Program program, Object a0, Object a1) {
        record("addObjToInven", a0, a1);
        return;
    }

    @Override
    public void rmObjFromInven(Program program, Object a0, Object a1) {
        record("rmObjFromInven", a0, a1);
        return;
    }

    @Override
    public void wieldObjCritter(Program program, Object a0, Object a1) {
        record("wieldObjCritter", a0, a1);
        return;
    }

    @Override
    public void useObj(Program program, Object a0) {
        record("useObj", a0);
        return;
    }

    @Override
    public int objCanSeeObj(Program program, Object a0, Object a1) {
        record("objCanSeeObj", a0, a1);
        return 0;
    }

    @Override
    public void startGdialog(Program program, int a0, Object a1, int a2, int a3, int a4) {
        record("startGdialog", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public void endGdialog(Program program) {
        record("endGdialog");
        return;
    }

    @Override
    public Program.ProgramValue metarule3(Program program, int a0, Program.ProgramValue a1, Program.ProgramValue a2, Program.ProgramValue a3) {
        record("metarule3", a0, a1, a2, a3);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void setObjVisibility(Program program, Object a0, int a1) {
        record("setObjVisibility", a0, a1);
        return;
    }

    @Override
    public void loadMap(Program program, Program.ProgramValue a0, int a1) {
        record("loadMap", a0, a1);
        return;
    }

    @Override
    public void wmAreaSetPos(Program program, int a0, int a1, int a2) {
        record("wmAreaSetPos", a0, a1, a2);
        return;
    }

    @Override
    public void setExitGrids(Program program, int a0, int a1, int a2, int a3, int a4) {
        record("setExitGrids", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public int animBusy(Program program, Object a0) {
        record("animBusy", a0);
        return 0;
    }

    @Override
    public int critterHeal(Program program, Object a0, int a1) {
        record("critterHeal", a0, a1);
        return 0;
    }

    @Override
    public void setLightLevel(Program program, int a0) {
        record("setLightLevel", a0);
        return;
    }

    @Override
    public int gameTime(Program program) {
        record("gameTime");
        return 0;
    }

    @Override
    public int elevation(Program program, Object a0) {
        record("elevation", a0);
        return 0;
    }

    @Override
    public void killCritter(Program program, Object a0, int a1) {
        record("killCritter", a0, a1);
        return;
    }

    @Override
    public void killCritterType(Program program, int a0) {
        record("killCritterType", a0);
        return;
    }

    @Override
    public void critterDamage(Program program, Object a0, int a1, int a2) {
        record("critterDamage", a0, a1, a2);
        return;
    }

    @Override
    public void addTimerEvent(Program program, Object a0, int a1, int a2) {
        record("addTimerEvent", a0, a1, a2);
        return;
    }

    @Override
    public void rmTimerEvent(Program program, Object a0) {
        record("rmTimerEvent", a0);
        return;
    }

    @Override
    public int gameTicks(Program program, int a0) {
        record("gameTicks", a0);
        return 0;
    }

    @Override
    public int hasTrait(Program program, int a0, Object a1, int a2) {
        record("hasTrait", a0, a1, a2);
        return 0;
    }

    @Override
    public void destroyObject(Program program, Object a0) {
        record("destroyObject", a0);
        return;
    }

    @Override
    public int objCanHearObj(Program program, Object a0, Object a1) {
        record("objCanHearObj", a0, a1);
        return 0;
    }

    @Override
    public int gameTimeHour(Program program) {
        record("gameTimeHour");
        return 0;
    }

    @Override
    public int fixedParam(Program program) {
        record("fixedParam");
        return 0;
    }

    @Override
    public int tileIsVisible(Program program, int a0) {
        record("tileIsVisible", a0);
        return 0;
    }

    @Override
    public void dialogueSystemEnter(Program program) {
        record("dialogueSystemEnter");
        return;
    }

    @Override
    public int actionBeingUsed(Program program) {
        record("actionBeingUsed");
        return 0;
    }

    @Override
    public int critterState(Program program, Object a0) {
        record("critterState", a0);
        return 0;
    }

    @Override
    public void gameTimeAdvance(Program program, int a0) {
        record("gameTimeAdvance", a0);
        return;
    }

    @Override
    public void radiationInc(Program program, Object a0, int a1) {
        record("radiationInc", a0, a1);
        return;
    }

    @Override
    public void radiationDec(Program program, Object a0, int a1) {
        record("radiationDec", a0, a1);
        return;
    }

    @Override
    public int critterAttemptPlacement(Program program, Object a0, int a1, int a2) {
        record("critterAttemptPlacement", a0, a1, a2);
        return 0;
    }

    @Override
    public int objPid(Program program, Object a0) {
        record("objPid", a0);
        return 0;
    }

    @Override
    public int curMapIndex(Program program) {
        record("curMapIndex");
        return 0;
    }

    @Override
    public int critterAddTrait(Program program, Object a0, int a1, int a2, int a3) {
        record("critterAddTrait", a0, a1, a2, a3);
        return 0;
    }

    @Override
    public int critterRmTrait(Program program, Object a0, int a1, int a2, int a3) {
        record("critterRmTrait", a0, a1, a2, a3);
        return 0;
    }

    @Override
    public Program.ProgramValue protoData(Program program, int a0) {
        record("protoData", a0);
        return new Program.ProgramValue(-1);
    }

    @Override
    public String messageStr(Program program, int a0, int a1) {
        record("messageStr", a0, a1);
        return "";
    }

    @Override
    public Program.ProgramValue critterInvenObj(Program program, Object a0, int a1) {
        record("critterInvenObj", a0, a1);
        return new Program.ProgramValue(-1);
    }

    @Override
    public void objSetLightLevel(Program program, Object a0, int a1, int a2) {
        record("objSetLightLevel", a0, a1, a2);
        return;
    }

    @Override
    public Object invenCmds(Program program, Object a0, int a1, int a2) {
        record("invenCmds", a0, a1, a2);
        return null;
    }

    @Override
    public void floatMsg(Program program, Program.ProgramValue a0, int a1) {
        record("floatMsg", a0, a1);
        return;
    }

    @Override
    public int metarule(Program program, int a0, Program.ProgramValue a1) {
        record("metarule", a0, a1);
        return 0;
    }

    @Override
    public void anim(Program program, Object a0, int a1, int a2, Program.ProgramValue a3) {
        record("anim", a0, a1, a2, a3);
        return;
    }

    @Override
    public Object objCarryingPidObj(Program program, Object a0) {
        record("objCarryingPidObj", a0);
        return null;
    }

    @Override
    public void regAnimFunc(Program program, int a0, Program.ProgramValue a1) {
        record("regAnimFunc", a0, a1);
        return;
    }

    @Override
    public void regAnimAnimate(Program program, Object a0, int a1, int a2) {
        record("regAnimAnimate", a0, a1, a2);
        return;
    }

    @Override
    public void regAnimAnimateReverse(Program program, Object a0, int a1, int a2) {
        record("regAnimAnimateReverse", a0, a1, a2);
        return;
    }

    @Override
    public void regAnimObjMoveToTile(Program program, Object a0, int a1, int a2) {
        record("regAnimObjMoveToTile", a0, a1, a2);
        return;
    }

    @Override
    public void playGmovie(Program program, int a0) {
        record("playGmovie", a0);
        return;
    }

    @Override
    public void addMultObjsToInven(Program program, Object a0, Object a1, int a2) {
        record("addMultObjsToInven", a0, a1, a2);
        return;
    }

    @Override
    public int rmMultObjsFromInven(Program program, Object a0, Object a1, int a2) {
        record("rmMultObjsFromInven", a0, a1, a2);
        return 0;
    }

    @Override
    public int month(Program program) {
        record("month");
        return 0;
    }

    @Override
    public int day(Program program) {
        record("day");
        return 0;
    }

    @Override
    public void explosion(Program program, int a0, int a1, int a2) {
        record("explosion", a0, a1, a2);
        return;
    }

    @Override
    public void opGsayStart(Program program) {
        record("opGsayStart");
        return;
    }

    @Override
    public void opGsayEnd(Program program) {
        record("opGsayEnd");
        return;
    }

    @Override
    public void gsayReply(Program program, int a0, Program.ProgramValue a1) {
        record("gsayReply", a0, a1);
        return;
    }

    @Override
    public void gsayOption(Program program, int a0, Program.ProgramValue a1, Program.ProgramValue a2, int a3) {
        record("gsayOption", a0, a1, a2, a3);
        return;
    }

    @Override
    public void gsayMessage(Program program, int a0, Program.ProgramValue a1, int a2) {
        record("gsayMessage", a0, a1, a2);
        return;
    }

    @Override
    public void giqOption(Program program, int a0, int a1, Program.ProgramValue a2, Program.ProgramValue a3, int a4) {
        record("giqOption", a0, a1, a2, a3, a4);
        return;
    }

    @Override
    public void poison(Program program, Object a0, int a1) {
        record("poison", a0, a1);
        return;
    }

    @Override
    public int getPoison(Program program, Object a0) {
        record("getPoison", a0);
        return 0;
    }

    @Override
    public void partyAdd(Program program, Object a0) {
        record("partyAdd", a0);
        return;
    }

    @Override
    public void partyRemove(Program program, Object a0) {
        record("partyRemove", a0);
        return;
    }

    @Override
    public void regAnimAnimateForever(Program program, Object a0, int a1) {
        record("regAnimAnimateForever", a0, a1);
        return;
    }

    @Override
    public void critterInjure(Program program, Object a0, int a1) {
        record("critterInjure", a0, a1);
        return;
    }

    @Override
    public int isInCombat(Program program) {
        record("isInCombat");
        return 0;
    }

    @Override
    public void gdialogBarter(Program program, int a0) {
        record("gdialogBarter", a0);
        return;
    }

    @Override
    public void invenUnwield(Program program) {
        record("invenUnwield");
        return;
    }

    @Override
    public int objIsLocked(Program program, Object a0) {
        record("objIsLocked", a0);
        return 0;
    }

    @Override
    public void objLock(Program program, Object a0) {
        record("objLock", a0);
        return;
    }

    @Override
    public void objUnlock(Program program, Object a0) {
        record("objUnlock", a0);
        return;
    }

    @Override
    public int objIsOpen(Program program, Object a0) {
        record("objIsOpen", a0);
        return 0;
    }

    @Override
    public void objOpen(Program program, Object a0) {
        record("objOpen", a0);
        return;
    }

    @Override
    public void objClose(Program program, Object a0) {
        record("objClose", a0);
        return;
    }

    @Override
    public void gameUiDisable(Program program) {
        record("gameUiDisable");
        return;
    }

    @Override
    public void gameUiEnable(Program program) {
        record("gameUiEnable");
        return;
    }

    @Override
    public void gfadeOut(Program program, int a0) {
        record("gfadeOut", a0);
        return;
    }

    @Override
    public void gfadeIn(Program program, int a0) {
        record("gfadeIn", a0);
        return;
    }

    @Override
    public int itemCapsTotal(Program program, Object a0) {
        record("itemCapsTotal", a0);
        return 0;
    }

    @Override
    public int itemCapsAdjust(Program program, Object a0, int a1) {
        record("itemCapsAdjust", a0, a1);
        return 0;
    }

    @Override
    public void regAnimPlaySfx(Program program, Object a0, String a1, int a2) {
        record("regAnimPlaySfx", a0, a1, a2);
        return;
    }

    @Override
    public void critterModSkill(Program program, Object a0, int a1, int a2) {
        record("critterModSkill", a0, a1, a2);
        return;
    }

    @Override
    public String sfxBuildCharName(Program program, Object a0, int a1, int a2) {
        record("sfxBuildCharName", a0, a1, a2);
        return "";
    }

    @Override
    public String sfxBuildWeaponName(Program program, int a0, Object a1, int a2, Object a3) {
        record("sfxBuildWeaponName", a0, a1, a2, a3);
        return "";
    }

    @Override
    public void attackSetup(Program program, Object a0, Object a1) {
        record("attackSetup", a0, a1);
        return;
    }

    @Override
    public int destroyMultObjs(Program program, Object a0, int a1) {
        record("destroyMultObjs", a0, a1);
        return 0;
    }

    @Override
    public void useObjOnObj(Program program, Object a0, Object a1) {
        record("useObjOnObj", a0, a1);
        return;
    }

    @Override
    public void endgameSlideshow(Program program) {
        record("endgameSlideshow");
        return;
    }

    @Override
    public void moveObjInvenToObj(Program program, Object a0, Object a1) {
        record("moveObjInvenToObj", a0, a1);
        return;
    }

    @Override
    public int objArtFid(Program program, Object a0) {
        record("objArtFid", a0);
        return 0;
    }

    @Override
    public int artAnim(Program program, int a0) {
        record("artAnim", a0);
        return 0;
    }

    @Override
    public Object partyMemberObj(Program program) {
        record("partyMemberObj");
        return null;
    }

    @Override
    public int rotationToTile(Program program, int a0, int a1) {
        record("rotationToTile", a0, a1);
        return 0;
    }

    @Override
    public void jamLock(Program program, Object a0) {
        record("jamLock", a0);
        return;
    }

    @Override
    public void gdialogSetBarterMod(Program program, int a0) {
        record("gdialogSetBarterMod", a0);
        return;
    }

    @Override
    public int objOnScreen(Program program, Object a0) {
        record("objOnScreen", a0);
        return 0;
    }

    @Override
    public int critterIsFleeing(Program program, Object a0) {
        record("critterIsFleeing", a0);
        return 0;
    }

    @Override
    public void critterSetFleeState(Program program, Object a0, int a1) {
        record("critterSetFleeState", a0, a1);
        return;
    }

    @Override
    public void terminateCombat(Program program) {
        record("terminateCombat");
        return;
    }

    @Override
    public void debugMsg(Program program, String a0) {
        record("debugMsg", a0);
        return;
    }

    @Override
    public void critterStopAttacking(Program program, Object a0) {
        record("critterStopAttacking", a0);
        return;
    }

}
