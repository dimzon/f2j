package org.fallout2ce;

/**
 * The script API used when the game is not behind it.
 *
 * <p>Every builtin reports that it is not available and the script stops,
 * the way the original does when a procedure it calls does not exist. The
 * game is a different implementation of {@link GameApi}, and so is
 * {@link MockScriptApi}, which records the calls instead.
 *
 * <p>Generated together with {@link GameApi}.
 */
class UnsupportedScriptApi implements GameApi {

    @Override
    public boolean supports(int slot) {
        return false;
    }

    protected static Program.FatalError unsupported(String name) {
        return new Program.FatalError(
            String.format("\nScript Error: %s: this builtin is not available.", name));
    }

    @Override
    public void opSayEnd() {
        throw unsupported("opSayEnd");
    }

    @Override
    public void opSayStart() {
        throw unsupported("opSayStart");
    }

    @Override
    public void opSayOption(Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayOption");
    }

    @Override
    public void opSayReply(Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayReply");
    }

    @Override
    public void opSayMessage(Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayMessage");
    }

    @Override
    public void opDisplay(String a0) {
        throw unsupported("opDisplay");
    }

    @Override
    public void giveExpPoints(int a0) {
        throw unsupported("op_give_exp_points");
    }

    @Override
    public void scrReturn(int a0) {
        throw unsupported("op_scr_return");
    }

    @Override
    public void playSfx(String a0) {
        throw unsupported("op_play_sfx");
    }

    @Override
    public String objName(Object a0) {
        throw unsupported("op_obj_name");
    }

    @Override
    public int getPcStat(int a0) {
        throw unsupported("op_get_pc_stat");
    }

    @Override
    public Object tileContainsPidObj(int a0, int a1, int a2) {
        throw unsupported("op_tile_contains_pid_obj");
    }

    @Override
    public void setMapStart(int a0, int a1, int a2, int a3) {
        throw unsupported("op_set_map_start");
    }

    @Override
    public void overrideMapStart(int a0, int a1, int a2, int a3) {
        throw unsupported("op_override_map_start");
    }

    @Override
    public int hasSkill(Object a0, int a1) {
        throw unsupported("op_has_skill");
    }

    @Override
    public int usingSkill(Object a0, int a1) {
        throw unsupported("op_using_skill");
    }

    @Override
    public int rollVsSkill(Object a0, int a1, int a2) {
        throw unsupported("op_roll_vs_skill");
    }

    @Override
    public int doCheck(Object a0, int a1, int a2) {
        throw unsupported("op_do_check");
    }

    @Override
    public int success(int a0) {
        throw unsupported("op_success");
    }

    @Override
    public int critical(int a0) {
        throw unsupported("op_critical");
    }

    @Override
    public void markAreaKnown(int a0, int a1, int a2) {
        throw unsupported("op_mark_area_known");
    }

    @Override
    public int random() {
        throw unsupported("op_random");
    }

    @Override
    public int moveTo(Object a0, int a1, int a2) {
        throw unsupported("op_move_to");
    }

    @Override
    public Object createObject(int a0, int a1, int a2, int a3) {
        throw unsupported("op_create_object");
    }

    @Override
    public void displayMsg(String a0) {
        throw unsupported("op_display_msg");
    }

    @Override
    public void scriptOverrides() {
        throw unsupported("op_script_overrides");
    }

    @Override
    public int objIsCarryingObj(Object a0, int a1) {
        throw unsupported("op_obj_is_carrying_obj");
    }

    @Override
    public int tileContainsObjPid(int a0, int a1, int a2) {
        throw unsupported("op_tile_contains_obj_pid");
    }

    @Override
    public Object selfObj() {
        throw unsupported("op_self_obj");
    }

    @Override
    public Object sourceObj() {
        throw unsupported("op_source_obj");
    }

    @Override
    public Object targetObj() {
        throw unsupported("op_target_obj");
    }

    @Override
    public Object opGetDude() {
        throw unsupported("opGetDude");
    }

    @Override
    public Object objBeingUsedWith() {
        throw unsupported("op_obj_being_used_with");
    }

    @Override
    public Program.ProgramValue getLocalVar(int a0) {
        throw unsupported("op_get_local_var");
    }

    @Override
    public void setLocalVar(int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_local_var");
    }

    @Override
    public Program.ProgramValue getMapVar(int a0) {
        throw unsupported("op_get_map_var");
    }

    @Override
    public void setMapVar(int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_map_var");
    }

    @Override
    public Program.ProgramValue getGlobalVar(int a0) {
        throw unsupported("op_get_global_var");
    }

    @Override
    public void setGlobalVar(int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_global_var");
    }

    @Override
    public int scriptAction() {
        throw unsupported("op_script_action");
    }

    @Override
    public int objType(Object a0) {
        throw unsupported("op_obj_type");
    }

    @Override
    public int itemSubtype(Object a0) {
        throw unsupported("op_item_subtype");
    }

    @Override
    public int getCritterStat(Object a0, int a1) {
        throw unsupported("op_get_critter_stat");
    }

    @Override
    public int setCritterStat(Object a0, int a1, int a2) {
        throw unsupported("op_set_critter_stat");
    }

    @Override
    public void animateStandObj(Object a0) {
        throw unsupported("op_animate_stand_obj");
    }

    @Override
    public void animateStandReverseObj(Object a0) {
        throw unsupported("animate_stand_reverse_obj");
    }

    @Override
    public void animateMoveObjToTile(Object a0, int a1, int a2) {
        throw unsupported("animate_move_obj_to_tile");
    }

    @Override
    public int tileInTileRect() {
        throw unsupported("tile_in_tile_rect");
    }

    @Override
    public void attack() {
        throw unsupported("op_attack");
    }

    @Override
    public int tileDistance(int a0, int a1) {
        throw unsupported("op_tile_distance");
    }

    @Override
    public int tileDistanceObjs(Object a0, Object a1) {
        throw unsupported("op_tile_distance_objs");
    }

    @Override
    public int tileNum(Object a0) {
        throw unsupported("op_tile_num");
    }

    @Override
    public int tileNumInDirection(int a0, int a1, int a2) {
        throw unsupported("op_tile_num_in_direction");
    }

    @Override
    public void dropObj(Object a0) {
        throw unsupported("op_drop_obj");
    }

    @Override
    public void addObjToInven(Object a0, Object a1) {
        throw unsupported("op_add_obj_to_inven");
    }

    @Override
    public void rmObjFromInven(Object a0, Object a1) {
        throw unsupported("op_rm_obj_from_inven");
    }

    @Override
    public void wieldObjCritter(Object a0, Object a1) {
        throw unsupported("op_wield_obj_critter");
    }

    @Override
    public void useObj(Object a0) {
        throw unsupported("op_use_obj");
    }

    @Override
    public int objCanSeeObj(Object a0, Object a1) {
        throw unsupported("op_obj_can_see_obj");
    }

    @Override
    public void startGdialog(int a0, Object a1, int a2, int a3, int a4) {
        throw unsupported("op_start_gdialog");
    }

    @Override
    public void endGdialog() {
        throw unsupported("op_end_gdialog");
    }

    @Override
    public Program.ProgramValue metarule3(int a0, Program.ProgramValue a1, Program.ProgramValue a2, Program.ProgramValue a3) {
        throw unsupported("op_metarule3");
    }

    @Override
    public void setObjVisibility(Object a0, int a1) {
        throw unsupported("op_set_obj_visibility");
    }

    @Override
    public void loadMap(Program.ProgramValue a0, int a1) {
        throw unsupported("op_load_map");
    }

    @Override
    public void wmAreaSetPos(int a0, int a1, int a2) {
        throw unsupported("op_wm_area_set_pos");
    }

    @Override
    public void setExitGrids(int a0, int a1, int a2, int a3, int a4) {
        throw unsupported("op_set_exit_grids");
    }

    @Override
    public int animBusy(Object a0) {
        throw unsupported("op_anim_busy");
    }

    @Override
    public int critterHeal(Object a0, int a1) {
        throw unsupported("op_critter_heal");
    }

    @Override
    public void setLightLevel(int a0) {
        throw unsupported("op_set_light_level");
    }

    @Override
    public int gameTime() {
        throw unsupported("op_game_time");
    }

    @Override
    public int elevation(Object a0) {
        throw unsupported("op_elevation");
    }

    @Override
    public void killCritter(Object a0, int a1) {
        throw unsupported("op_kill_critter");
    }

    @Override
    public void killCritterType(int a0, int a1) {
        throw unsupported("op_kill_critter_type");
    }

    @Override
    public void critterDamage(Object a0, int a1, int a2) {
        throw unsupported("op_critter_damage");
    }

    @Override
    public void addTimerEvent(Object a0, int a1, int a2) {
        throw unsupported("op_add_timer_event");
    }

    @Override
    public void rmTimerEvent(Object a0) {
        throw unsupported("op_rm_timer_event");
    }

    @Override
    public int gameTicks(int a0) {
        throw unsupported("op_game_ticks");
    }

    @Override
    public int hasTrait(int a0, Object a1, int a2) {
        throw unsupported("op_has_trait");
    }

    @Override
    public void destroyObject(Object a0) {
        throw unsupported("op_destroy_object");
    }

    @Override
    public int objCanHearObj(Object a0, Object a1) {
        throw unsupported("op_obj_can_hear_obj");
    }

    @Override
    public int gameTimeHour() {
        throw unsupported("op_game_time_hour");
    }

    @Override
    public int fixedParam() {
        throw unsupported("op_fixed_param");
    }

    @Override
    public int tileIsVisible(int a0) {
        throw unsupported("op_tile_is_visible");
    }

    @Override
    public void dialogueSystemEnter() {
        throw unsupported("op_dialogue_system_enter");
    }

    @Override
    public int actionBeingUsed() {
        throw unsupported("op_action_being_used");
    }

    @Override
    public int critterState(Object a0) {
        throw unsupported("critter_state");
    }

    @Override
    public void gameTimeAdvance(int a0) {
        throw unsupported("op_game_time_advance");
    }

    @Override
    public void radiationInc(Object a0, int a1) {
        throw unsupported("op_radiation_inc");
    }

    @Override
    public void radiationDec(Object a0, int a1) {
        throw unsupported("op_radiation_dec");
    }

    @Override
    public int critterAttemptPlacement(Object a0, int a1, int a2) {
        throw unsupported("critter_attempt_placement");
    }

    @Override
    public int objPid(Object a0) {
        throw unsupported("op_obj_pid");
    }

    @Override
    public int curMapIndex() {
        throw unsupported("op_cur_map_index");
    }

    @Override
    public int critterAddTrait(Object a0, int a1, int a2, int a3) {
        throw unsupported("op_critter_add_trait");
    }

    @Override
    public int critterRmTrait(Object a0, int a1, int a2, int a3) {
        throw unsupported("critter_rm_trait");
    }

    @Override
    public Program.ProgramValue protoData(int a0, int a1) {
        throw unsupported("op_proto_data");
    }

    @Override
    public String messageStr(int a0, int a1) {
        throw unsupported("op_message_str");
    }

    @Override
    public Program.ProgramValue critterInvenObj(Object a0, int a1) {
        throw unsupported("op_critter_inven_obj");
    }

    @Override
    public void objSetLightLevel(Object a0, int a1, int a2) {
        throw unsupported("op_obj_set_light_level");
    }

    @Override
    public Object invenCmds(Object a0, int a1, int a2) {
        throw unsupported("op_inven_cmds");
    }

    @Override
    public void floatMsg(Program.ProgramValue a0, int a1) {
        throw unsupported("op_float_msg");
    }

    @Override
    public int metarule(int a0, Program.ProgramValue a1) {
        throw unsupported("op_metarule");
    }

    @Override
    public void anim(Object a0, int a1, int a2, Program.ProgramValue a3) {
        throw unsupported("op_anim");
    }

    @Override
    public Object objCarryingPidObj(Object a0, int a1) {
        throw unsupported("op_obj_carrying_pid_obj");
    }

    @Override
    public void regAnimFunc(int a0, Program.ProgramValue a1) {
        throw unsupported("op_reg_anim_func");
    }

    @Override
    public void regAnimAnimate(Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_animate");
    }

    @Override
    public void regAnimAnimateReverse(Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_animate_reverse");
    }

    @Override
    public void regAnimObjMoveToTile(Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_obj_move_to_tile");
    }

    @Override
    public void playGmovie(int a0) {
        throw unsupported("op_play_gmovie");
    }

    @Override
    public void addMultObjsToInven(Object a0, Object a1, int a2) {
        throw unsupported("op_add_mult_objs_to_inven");
    }

    @Override
    public int rmMultObjsFromInven(Object a0, Object a1, int a2) {
        throw unsupported("rm_mult_objs_from_inven");
    }

    @Override
    public int month() {
        throw unsupported("op_month");
    }

    @Override
    public int day() {
        throw unsupported("op_day");
    }

    @Override
    public void explosion(int a0, int a1, int a2) {
        throw unsupported("op_explosion");
    }

    @Override
    public void opGsayStart() {
        throw unsupported("_op_gsay_start");
    }

    @Override
    public void opGsayEnd() {
        throw unsupported("_op_gsay_end");
    }

    @Override
    public void gsayReply(int a0, Program.ProgramValue a1) {
        throw unsupported("op_gsay_reply");
    }

    @Override
    public void gsayOption(int a0, Program.ProgramValue a1, Program.ProgramValue a2, int a3) {
        throw unsupported("op_gsay_option");
    }

    @Override
    public void gsayMessage(int a0, Program.ProgramValue a1, int a2) {
        throw unsupported("op_gsay_message");
    }

    @Override
    public void giqOption(int a0, int a1, Program.ProgramValue a2, Program.ProgramValue a3, int a4) {
        throw unsupported("op_giq_option");
    }

    @Override
    public void poison(Object a0, int a1) {
        throw unsupported("op_poison");
    }

    @Override
    public int getPoison(Object a0) {
        throw unsupported("op_get_poison");
    }

    @Override
    public void partyAdd(Object a0) {
        throw unsupported("op_party_add");
    }

    @Override
    public void partyRemove(Object a0) {
        throw unsupported("op_party_remove");
    }

    @Override
    public void regAnimAnimateForever(Object a0, int a1) {
        throw unsupported("op_reg_anim_animate_forever");
    }

    @Override
    public void critterInjure(Object a0, int a1) {
        throw unsupported("op_critter_injure");
    }

    @Override
    public int isInCombat() {
        throw unsupported("op_is_in_combat");
    }

    @Override
    public void gdialogBarter(int a0) {
        throw unsupported("op_gdialog_barter");
    }

    @Override
    public void invenUnwield() {
        throw unsupported("op_inven_unwield");
    }

    @Override
    public int objIsLocked(Object a0) {
        throw unsupported("op_obj_is_locked");
    }

    @Override
    public void objLock(Object a0) {
        throw unsupported("op_obj_lock");
    }

    @Override
    public void objUnlock(Object a0) {
        throw unsupported("op_obj_unlock");
    }

    @Override
    public int objIsOpen(Object a0) {
        throw unsupported("op_obj_is_open");
    }

    @Override
    public void objOpen(Object a0) {
        throw unsupported("op_obj_open");
    }

    @Override
    public void objClose(Object a0) {
        throw unsupported("op_obj_close");
    }

    @Override
    public void gameUiDisable() {
        throw unsupported("op_game_ui_disable");
    }

    @Override
    public void gameUiEnable() {
        throw unsupported("op_game_ui_enable");
    }

    @Override
    public void gfadeOut(int a0) {
        throw unsupported("op_gfade_out");
    }

    @Override
    public void gfadeIn(int a0) {
        throw unsupported("op_gfade_in");
    }

    @Override
    public int itemCapsTotal(Object a0) {
        throw unsupported("op_item_caps_total");
    }

    @Override
    public int itemCapsAdjust(Object a0, int a1) {
        throw unsupported("op_item_caps_adjust");
    }

    @Override
    public void regAnimPlaySfx(Object a0, String a1, int a2) {
        throw unsupported("op_reg_anim_play_sfx");
    }

    @Override
    public void critterModSkill(Object a0, int a1, int a2) {
        throw unsupported("op_critter_mod_skill");
    }

    @Override
    public String sfxBuildCharName(Object a0, int a1, int a2) {
        throw unsupported("op_sfx_build_char_name");
    }

    @Override
    public String sfxBuildWeaponName(int a0, Object a1, int a2, Object a3) {
        throw unsupported("op_sfx_build_weapon_name");
    }

    @Override
    public void attackSetup(Object a0, Object a1) {
        throw unsupported("op_attack_setup");
    }

    @Override
    public int destroyMultObjs(Object a0, int a1) {
        throw unsupported("op_destroy_mult_objs");
    }

    @Override
    public void useObjOnObj(Object a0, Object a1) {
        throw unsupported("op_use_obj_on_obj");
    }

    @Override
    public void endgameSlideshow() {
        throw unsupported("op_endgame_slideshow");
    }

    @Override
    public void moveObjInvenToObj(Object a0, Object a1) {
        throw unsupported("op_move_obj_inven_to_obj");
    }

    @Override
    public int objArtFid(Object a0) {
        throw unsupported("op_obj_art_fid");
    }

    @Override
    public int artAnim(int a0) {
        throw unsupported("op_art_anim");
    }

    @Override
    public Object partyMemberObj(int a0) {
        throw unsupported("op_party_member_obj");
    }

    @Override
    public int rotationToTile(int a0, int a1) {
        throw unsupported("op_rotation_to_tile");
    }

    @Override
    public void jamLock(Object a0) {
        throw unsupported("op_jam_lock");
    }

    @Override
    public void gdialogSetBarterMod(int a0) {
        throw unsupported("op_gdialog_set_barter_mod");
    }

    @Override
    public int objOnScreen(Object a0) {
        throw unsupported("op_obj_on_screen");
    }

    @Override
    public int critterIsFleeing(Object a0) {
        throw unsupported("op_critter_is_fleeing");
    }

    @Override
    public void critterSetFleeState(Object a0, int a1) {
        throw unsupported("op_critter_set_flee_state");
    }

    @Override
    public void terminateCombat() {
        throw unsupported("op_terminate_combat");
    }

    @Override
    public void debugMsg(String a0) {
        throw unsupported("op_debug_msg");
    }

    @Override
    public void critterStopAttacking(Object a0) {
        throw unsupported("op_critter_stop_attacking");
    }

}
