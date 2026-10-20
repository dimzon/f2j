package org.fallout2ce;

/**
 * Which method of the game API a dispatch slot calls, and how many
 * arguments it takes.
 *
 * <p>The compiler needs this to turn a call into a call: the
 * arguments come off the stack the way the VM takes them off, and
 * the method is called with them in the order it declares them.
 *
 * <p>Generated together with {@link GameApi}.
 */
final class GameApiSlots {

    private GameApiSlots() {
    }

    /** The method a slot calls, or null when the slot is the VM's own. */
    static final String[] METHOD = new String[768];

    /** How many arguments the method takes. */
    static final int[] ARGS = new int[768];

    /** What kind of value each argument is, comma separated. */
    static final String[] KINDS = new String[768];
    static {
        METHOD[77] = "opSayEnd";
        ARGS[77] = 0;
        KINDS[77] = "";
    }
    static {
        METHOD[78] = "opSayStart";
        ARGS[78] = 0;
        KINDS[78] = "";
    }
    static {
        METHOD[82] = "opSayOption";
        ARGS[82] = 2;
        KINDS[82] = "value,value";
    }
    static {
        METHOD[83] = "opSayReply";
        ARGS[83] = 2;
        KINDS[83] = "value,value";
    }
    static {
        METHOD[84] = "opSayMessage";
        ARGS[84] = 2;
        KINDS[84] = "value,value";
    }
    static {
        METHOD[107] = "opDisplay";
        ARGS[107] = 1;
        KINDS[107] = "string";
    }
    static {
        METHOD[161] = "giveExpPoints";
        ARGS[161] = 1;
        KINDS[161] = "int";
    }
    static {
        METHOD[162] = "scrReturn";
        ARGS[162] = 1;
        KINDS[162] = "int";
    }
    static {
        METHOD[163] = "playSfx";
        ARGS[163] = 1;
        KINDS[163] = "string";
    }
    static {
        METHOD[164] = "objName";
        ARGS[164] = 1;
        KINDS[164] = "ptr";
    }
    static {
        METHOD[166] = "getPcStat";
        ARGS[166] = 1;
        KINDS[166] = "int";
    }
    static {
        METHOD[167] = "tileContainsPidObj";
        ARGS[167] = 3;
        KINDS[167] = "int,int,int";
    }
    static {
        METHOD[168] = "setMapStart";
        ARGS[168] = 4;
        KINDS[168] = "int,int,int,int";
    }
    static {
        METHOD[169] = "overrideMapStart";
        ARGS[169] = 4;
        KINDS[169] = "int,int,int,int";
    }
    static {
        METHOD[170] = "hasSkill";
        ARGS[170] = 2;
        KINDS[170] = "ptr,int";
    }
    static {
        METHOD[171] = "usingSkill";
        ARGS[171] = 2;
        KINDS[171] = "ptr,int";
    }
    static {
        METHOD[172] = "rollVsSkill";
        ARGS[172] = 3;
        KINDS[172] = "ptr,int,int";
    }
    static {
        METHOD[174] = "doCheck";
        ARGS[174] = 3;
        KINDS[174] = "ptr,int,int";
    }
    static {
        METHOD[175] = "success";
        ARGS[175] = 1;
        KINDS[175] = "int";
    }
    static {
        METHOD[176] = "critical";
        ARGS[176] = 1;
        KINDS[176] = "int";
    }
    static {
        METHOD[178] = "markAreaKnown";
        ARGS[178] = 3;
        KINDS[178] = "int,int,int";
    }
    static {
        METHOD[180] = "random";
        ARGS[180] = 0;
        KINDS[180] = "";
    }
    static {
        METHOD[182] = "moveTo";
        ARGS[182] = 3;
        KINDS[182] = "ptr,int,int";
    }
    static {
        METHOD[183] = "createObject";
        ARGS[183] = 4;
        KINDS[183] = "int,int,int,int";
    }
    static {
        METHOD[184] = "displayMsg";
        ARGS[184] = 1;
        KINDS[184] = "string";
    }
    static {
        METHOD[185] = "scriptOverrides";
        ARGS[185] = 0;
        KINDS[185] = "";
    }
    static {
        METHOD[186] = "objIsCarryingObj";
        ARGS[186] = 2;
        KINDS[186] = "ptr,int";
    }
    static {
        METHOD[187] = "tileContainsObjPid";
        ARGS[187] = 3;
        KINDS[187] = "int,int,int";
    }
    static {
        METHOD[188] = "selfObj";
        ARGS[188] = 0;
        KINDS[188] = "";
    }
    static {
        METHOD[189] = "sourceObj";
        ARGS[189] = 0;
        KINDS[189] = "";
    }
    static {
        METHOD[190] = "targetObj";
        ARGS[190] = 0;
        KINDS[190] = "";
    }
    static {
        METHOD[191] = "opGetDude";
        ARGS[191] = 0;
        KINDS[191] = "";
    }
    static {
        METHOD[192] = "objBeingUsedWith";
        ARGS[192] = 0;
        KINDS[192] = "";
    }
    static {
        METHOD[193] = "getLocalVar";
        ARGS[193] = 1;
        KINDS[193] = "int";
    }
    static {
        METHOD[194] = "setLocalVar";
        ARGS[194] = 2;
        KINDS[194] = "int,value";
    }
    static {
        METHOD[195] = "getMapVar";
        ARGS[195] = 1;
        KINDS[195] = "int";
    }
    static {
        METHOD[196] = "setMapVar";
        ARGS[196] = 2;
        KINDS[196] = "int,value";
    }
    static {
        METHOD[197] = "getGlobalVar";
        ARGS[197] = 1;
        KINDS[197] = "int";
    }
    static {
        METHOD[198] = "setGlobalVar";
        ARGS[198] = 2;
        KINDS[198] = "int,value";
    }
    static {
        METHOD[199] = "scriptAction";
        ARGS[199] = 0;
        KINDS[199] = "";
    }
    static {
        METHOD[200] = "objType";
        ARGS[200] = 1;
        KINDS[200] = "ptr";
    }
    static {
        METHOD[201] = "itemSubtype";
        ARGS[201] = 1;
        KINDS[201] = "ptr";
    }
    static {
        METHOD[202] = "getCritterStat";
        ARGS[202] = 2;
        KINDS[202] = "ptr,int";
    }
    static {
        METHOD[203] = "setCritterStat";
        ARGS[203] = 3;
        KINDS[203] = "ptr,int,int";
    }
    static {
        METHOD[204] = "animateStandObj";
        ARGS[204] = 1;
        KINDS[204] = "ptr";
    }
    static {
        METHOD[205] = "animateStandReverseObj";
        ARGS[205] = 1;
        KINDS[205] = "ptr";
    }
    static {
        METHOD[206] = "animateMoveObjToTile";
        ARGS[206] = 3;
        KINDS[206] = "ptr,int,int";
    }
    static {
        METHOD[207] = "tileInTileRect";
        ARGS[207] = 0;
        KINDS[207] = "";
    }
    static {
        METHOD[208] = "attack";
        ARGS[208] = 0;
        KINDS[208] = "";
    }
    static {
        METHOD[210] = "tileDistance";
        ARGS[210] = 2;
        KINDS[210] = "int,int";
    }
    static {
        METHOD[211] = "tileDistanceObjs";
        ARGS[211] = 2;
        KINDS[211] = "ptr,ptr";
    }
    static {
        METHOD[212] = "tileNum";
        ARGS[212] = 1;
        KINDS[212] = "ptr";
    }
    static {
        METHOD[213] = "tileNumInDirection";
        ARGS[213] = 3;
        KINDS[213] = "int,int,int";
    }
    static {
        METHOD[215] = "dropObj";
        ARGS[215] = 1;
        KINDS[215] = "ptr";
    }
    static {
        METHOD[216] = "addObjToInven";
        ARGS[216] = 2;
        KINDS[216] = "ptr,ptr";
    }
    static {
        METHOD[217] = "rmObjFromInven";
        ARGS[217] = 2;
        KINDS[217] = "ptr,ptr";
    }
    static {
        METHOD[218] = "wieldObjCritter";
        ARGS[218] = 2;
        KINDS[218] = "ptr,ptr";
    }
    static {
        METHOD[219] = "useObj";
        ARGS[219] = 1;
        KINDS[219] = "ptr";
    }
    static {
        METHOD[220] = "objCanSeeObj";
        ARGS[220] = 2;
        KINDS[220] = "ptr,ptr";
    }
    static {
        METHOD[222] = "startGdialog";
        ARGS[222] = 5;
        KINDS[222] = "int,ptr,int,int,int";
    }
    static {
        METHOD[223] = "endGdialog";
        ARGS[223] = 0;
        KINDS[223] = "";
    }
    static {
        METHOD[225] = "metarule3";
        ARGS[225] = 4;
        KINDS[225] = "int,value,value,value";
    }
    static {
        METHOD[227] = "setObjVisibility";
        ARGS[227] = 2;
        KINDS[227] = "ptr,int";
    }
    static {
        METHOD[228] = "loadMap";
        ARGS[228] = 2;
        KINDS[228] = "value,int";
    }
    static {
        METHOD[229] = "wmAreaSetPos";
        ARGS[229] = 3;
        KINDS[229] = "int,int,int";
    }
    static {
        METHOD[230] = "setExitGrids";
        ARGS[230] = 5;
        KINDS[230] = "int,int,int,int,int";
    }
    static {
        METHOD[231] = "animBusy";
        ARGS[231] = 1;
        KINDS[231] = "ptr";
    }
    static {
        METHOD[232] = "critterHeal";
        ARGS[232] = 2;
        KINDS[232] = "ptr,int";
    }
    static {
        METHOD[233] = "setLightLevel";
        ARGS[233] = 1;
        KINDS[233] = "int";
    }
    static {
        METHOD[234] = "gameTime";
        ARGS[234] = 0;
        KINDS[234] = "";
    }
    static {
        METHOD[236] = "elevation";
        ARGS[236] = 1;
        KINDS[236] = "ptr";
    }
    static {
        METHOD[237] = "killCritter";
        ARGS[237] = 2;
        KINDS[237] = "ptr,int";
    }
    static {
        METHOD[238] = "killCritterType";
        ARGS[238] = 2;
        KINDS[238] = "int,int";
    }
    static {
        METHOD[239] = "critterDamage";
        ARGS[239] = 3;
        KINDS[239] = "ptr,int,int";
    }
    static {
        METHOD[240] = "addTimerEvent";
        ARGS[240] = 3;
        KINDS[240] = "ptr,int,int";
    }
    static {
        METHOD[241] = "rmTimerEvent";
        ARGS[241] = 1;
        KINDS[241] = "ptr";
    }
    static {
        METHOD[242] = "gameTicks";
        ARGS[242] = 1;
        KINDS[242] = "int";
    }
    static {
        METHOD[243] = "hasTrait";
        ARGS[243] = 3;
        KINDS[243] = "int,ptr,int";
    }
    static {
        METHOD[244] = "destroyObject";
        ARGS[244] = 1;
        KINDS[244] = "ptr";
    }
    static {
        METHOD[245] = "objCanHearObj";
        ARGS[245] = 2;
        KINDS[245] = "ptr,ptr";
    }
    static {
        METHOD[246] = "gameTimeHour";
        ARGS[246] = 0;
        KINDS[246] = "";
    }
    static {
        METHOD[247] = "fixedParam";
        ARGS[247] = 0;
        KINDS[247] = "";
    }
    static {
        METHOD[248] = "tileIsVisible";
        ARGS[248] = 1;
        KINDS[248] = "int";
    }
    static {
        METHOD[249] = "dialogueSystemEnter";
        ARGS[249] = 0;
        KINDS[249] = "";
    }
    static {
        METHOD[250] = "actionBeingUsed";
        ARGS[250] = 0;
        KINDS[250] = "";
    }
    static {
        METHOD[251] = "critterState";
        ARGS[251] = 1;
        KINDS[251] = "ptr";
    }
    static {
        METHOD[252] = "gameTimeAdvance";
        ARGS[252] = 1;
        KINDS[252] = "int";
    }
    static {
        METHOD[253] = "radiationInc";
        ARGS[253] = 2;
        KINDS[253] = "ptr,int";
    }
    static {
        METHOD[254] = "radiationDec";
        ARGS[254] = 2;
        KINDS[254] = "ptr,int";
    }
    static {
        METHOD[255] = "critterAttemptPlacement";
        ARGS[255] = 3;
        KINDS[255] = "ptr,int,int";
    }
    static {
        METHOD[256] = "objPid";
        ARGS[256] = 1;
        KINDS[256] = "ptr";
    }
    static {
        METHOD[257] = "curMapIndex";
        ARGS[257] = 0;
        KINDS[257] = "";
    }
    static {
        METHOD[258] = "critterAddTrait";
        ARGS[258] = 4;
        KINDS[258] = "ptr,int,int,int";
    }
    static {
        METHOD[259] = "critterRmTrait";
        ARGS[259] = 4;
        KINDS[259] = "ptr,int,int,int";
    }
    static {
        METHOD[260] = "protoData";
        ARGS[260] = 2;
        KINDS[260] = "int,int";
    }
    static {
        METHOD[261] = "messageStr";
        ARGS[261] = 2;
        KINDS[261] = "int,int";
    }
    static {
        METHOD[262] = "critterInvenObj";
        ARGS[262] = 2;
        KINDS[262] = "ptr,int";
    }
    static {
        METHOD[263] = "objSetLightLevel";
        ARGS[263] = 3;
        KINDS[263] = "ptr,int,int";
    }
    static {
        METHOD[265] = "invenCmds";
        ARGS[265] = 3;
        KINDS[265] = "ptr,int,int";
    }
    static {
        METHOD[266] = "floatMsg";
        ARGS[266] = 2;
        KINDS[266] = "value,int";
    }
    static {
        METHOD[267] = "metarule";
        ARGS[267] = 2;
        KINDS[267] = "int,value";
    }
    static {
        METHOD[268] = "anim";
        ARGS[268] = 4;
        KINDS[268] = "ptr,int,int,value";
    }
    static {
        METHOD[269] = "objCarryingPidObj";
        ARGS[269] = 2;
        KINDS[269] = "ptr,int";
    }
    static {
        METHOD[270] = "regAnimFunc";
        ARGS[270] = 2;
        KINDS[270] = "int,value";
    }
    static {
        METHOD[271] = "regAnimAnimate";
        ARGS[271] = 3;
        KINDS[271] = "ptr,int,int";
    }
    static {
        METHOD[272] = "regAnimAnimateReverse";
        ARGS[272] = 3;
        KINDS[272] = "ptr,int,int";
    }
    static {
        METHOD[275] = "regAnimObjMoveToTile";
        ARGS[275] = 3;
        KINDS[275] = "ptr,int,int";
    }
    static {
        METHOD[277] = "playGmovie";
        ARGS[277] = 1;
        KINDS[277] = "int";
    }
    static {
        METHOD[278] = "addMultObjsToInven";
        ARGS[278] = 3;
        KINDS[278] = "ptr,ptr,int";
    }
    static {
        METHOD[279] = "rmMultObjsFromInven";
        ARGS[279] = 3;
        KINDS[279] = "ptr,ptr,int";
    }
    static {
        METHOD[280] = "month";
        ARGS[280] = 0;
        KINDS[280] = "";
    }
    static {
        METHOD[281] = "day";
        ARGS[281] = 0;
        KINDS[281] = "";
    }
    static {
        METHOD[282] = "explosion";
        ARGS[282] = 3;
        KINDS[282] = "int,int,int";
    }
    static {
        METHOD[284] = "opGsayStart";
        ARGS[284] = 0;
        KINDS[284] = "";
    }
    static {
        METHOD[285] = "opGsayEnd";
        ARGS[285] = 0;
        KINDS[285] = "";
    }
    static {
        METHOD[286] = "gsayReply";
        ARGS[286] = 2;
        KINDS[286] = "int,value";
    }
    static {
        METHOD[287] = "gsayOption";
        ARGS[287] = 4;
        KINDS[287] = "int,value,value,int";
    }
    static {
        METHOD[288] = "gsayMessage";
        ARGS[288] = 3;
        KINDS[288] = "int,value,int";
    }
    static {
        METHOD[289] = "giqOption";
        ARGS[289] = 5;
        KINDS[289] = "int,int,value,value,int";
    }
    static {
        METHOD[290] = "poison";
        ARGS[290] = 2;
        KINDS[290] = "ptr,int";
    }
    static {
        METHOD[291] = "getPoison";
        ARGS[291] = 1;
        KINDS[291] = "ptr";
    }
    static {
        METHOD[292] = "partyAdd";
        ARGS[292] = 1;
        KINDS[292] = "ptr";
    }
    static {
        METHOD[293] = "partyRemove";
        ARGS[293] = 1;
        KINDS[293] = "ptr";
    }
    static {
        METHOD[294] = "regAnimAnimateForever";
        ARGS[294] = 2;
        KINDS[294] = "ptr,int";
    }
    static {
        METHOD[295] = "critterInjure";
        ARGS[295] = 2;
        KINDS[295] = "ptr,int";
    }
    static {
        METHOD[296] = "isInCombat";
        ARGS[296] = 0;
        KINDS[296] = "";
    }
    static {
        METHOD[297] = "gdialogBarter";
        ARGS[297] = 1;
        KINDS[297] = "int";
    }
    static {
        METHOD[300] = "invenUnwield";
        ARGS[300] = 0;
        KINDS[300] = "";
    }
    static {
        METHOD[301] = "objIsLocked";
        ARGS[301] = 1;
        KINDS[301] = "ptr";
    }
    static {
        METHOD[302] = "objLock";
        ARGS[302] = 1;
        KINDS[302] = "ptr";
    }
    static {
        METHOD[303] = "objUnlock";
        ARGS[303] = 1;
        KINDS[303] = "ptr";
    }
    static {
        METHOD[304] = "objIsOpen";
        ARGS[304] = 1;
        KINDS[304] = "ptr";
    }
    static {
        METHOD[305] = "objOpen";
        ARGS[305] = 1;
        KINDS[305] = "ptr";
    }
    static {
        METHOD[306] = "objClose";
        ARGS[306] = 1;
        KINDS[306] = "ptr";
    }
    static {
        METHOD[307] = "gameUiDisable";
        ARGS[307] = 0;
        KINDS[307] = "";
    }
    static {
        METHOD[308] = "gameUiEnable";
        ARGS[308] = 0;
        KINDS[308] = "";
    }
    static {
        METHOD[310] = "gfadeOut";
        ARGS[310] = 1;
        KINDS[310] = "int";
    }
    static {
        METHOD[311] = "gfadeIn";
        ARGS[311] = 1;
        KINDS[311] = "int";
    }
    static {
        METHOD[312] = "itemCapsTotal";
        ARGS[312] = 1;
        KINDS[312] = "ptr";
    }
    static {
        METHOD[313] = "itemCapsAdjust";
        ARGS[313] = 2;
        KINDS[313] = "ptr,int";
    }
    static {
        METHOD[315] = "regAnimPlaySfx";
        ARGS[315] = 3;
        KINDS[315] = "ptr,string,int";
    }
    static {
        METHOD[316] = "critterModSkill";
        ARGS[316] = 3;
        KINDS[316] = "ptr,int,int";
    }
    static {
        METHOD[317] = "sfxBuildCharName";
        ARGS[317] = 3;
        KINDS[317] = "ptr,int,int";
    }
    static {
        METHOD[321] = "sfxBuildWeaponName";
        ARGS[321] = 4;
        KINDS[321] = "int,ptr,int,ptr";
    }
    static {
        METHOD[323] = "attackSetup";
        ARGS[323] = 2;
        KINDS[323] = "ptr,ptr";
    }
    static {
        METHOD[324] = "destroyMultObjs";
        ARGS[324] = 2;
        KINDS[324] = "ptr,int";
    }
    static {
        METHOD[325] = "useObjOnObj";
        ARGS[325] = 2;
        KINDS[325] = "ptr,ptr";
    }
    static {
        METHOD[326] = "endgameSlideshow";
        ARGS[326] = 0;
        KINDS[326] = "";
    }
    static {
        METHOD[327] = "moveObjInvenToObj";
        ARGS[327] = 2;
        KINDS[327] = "ptr,ptr";
    }
    static {
        METHOD[329] = "objArtFid";
        ARGS[329] = 1;
        KINDS[329] = "ptr";
    }
    static {
        METHOD[330] = "artAnim";
        ARGS[330] = 1;
        KINDS[330] = "int";
    }
    static {
        METHOD[331] = "partyMemberObj";
        ARGS[331] = 1;
        KINDS[331] = "int";
    }
    static {
        METHOD[332] = "rotationToTile";
        ARGS[332] = 2;
        KINDS[332] = "int,int";
    }
    static {
        METHOD[333] = "jamLock";
        ARGS[333] = 1;
        KINDS[333] = "ptr";
    }
    static {
        METHOD[334] = "gdialogSetBarterMod";
        ARGS[334] = 1;
        KINDS[334] = "int";
    }
    static {
        METHOD[336] = "objOnScreen";
        ARGS[336] = 1;
        KINDS[336] = "ptr";
    }
    static {
        METHOD[337] = "critterIsFleeing";
        ARGS[337] = 1;
        KINDS[337] = "ptr";
    }
    static {
        METHOD[338] = "critterSetFleeState";
        ARGS[338] = 2;
        KINDS[338] = "ptr,int";
    }
    static {
        METHOD[339] = "terminateCombat";
        ARGS[339] = 0;
        KINDS[339] = "";
    }
    static {
        METHOD[340] = "debugMsg";
        ARGS[340] = 1;
        KINDS[340] = "string";
    }
    static {
        METHOD[341] = "critterStopAttacking";
        ARGS[341] = 1;
        KINDS[341] = "ptr";
    }
}
