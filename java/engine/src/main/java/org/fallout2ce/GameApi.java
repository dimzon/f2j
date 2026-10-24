package org.fallout2ce;

/**
 * The Fallout 2 script API: the builtins a script can call.
 *
 * <p>Each method is one builtin. The game's own opcodes are the intlib's,
 * and they are the surface the scripts are written against - the rest of
 * the engine exists to serve them. Keeping them behind an interface is
 * what makes a script testable: a script can be run against a mock
 * instead of the game.
 *
 * <p>The argument types are what the builtin reads off the script's value
 * stack, in the order the script compiler passed them. The values the VM
 * holds are looser than this - an int slot may hold a float, a pointer
 * slot may hold a string - so the implementation owns the conversions the
 * original makes. The parameter names and the notes come from the C
 * handlers: the value each one pops, and what it does with it.
 */
public interface GameApi {

    /**
     * Whether this API provides the builtin in the given dispatch slot.
     *
     * <p>The original refuses an opcode it has no handler for before it
     * reads the opcode's arguments, and the script stops with a fatal
     * error. An API that does not provide a builtin has to refuse it the
     * same way, so the slot is checked before the arguments are read.
     */
    default boolean supports(int slot) {
        return true;
    }

    /**
     * {@code opSayEnd} (0x804D) - 3 calls in the shipped scripts: end the
     * say-screen dialog.
     */
    void opSayEnd();

    /**
     * {@code opSayStart} (0x804E) - 3 calls in the shipped scripts: start
     * the say-screen dialog.
     */
    void opSayStart();

    /**
     * {@code opSayOption} (0x8052) - 12 calls in the shipped scripts: add
     * an option to the say screen.
     *
     * @param text the text of the option
     * @param procedure the procedure that answers it - a string names it,
     * an integer is its index
     */
    void opSayOption(Program.ProgramValue text, Program.ProgramValue procedure);

    /**
     * {@code opSayReply} (0x8053) - 46 calls in the shipped scripts: set
     * the reply of the say screen.
     *
     * @param text the text of the reply
     * @param title the title the reply goes under
     */
    void opSayReply(Program.ProgramValue text, Program.ProgramValue title);

    /**
     * {@code opSayMessage} (0x8054) - 12 calls in the shipped scripts:
     * show a message of the say screen, for the timeout it was given.
     *
     * @param text the text of the message
     * @param title the title the message goes under
     */
    void opSayMessage(Program.ProgramValue text, Program.ProgramValue title);

    /**
     * {@code opDisplay} (0x806B) - 1 call in the shipped scripts: show a
     * string on the say screen.
     *
     * @param text the text to show
     */
    void opDisplay(String text);

    /**
     * {@code op_give_exp_points} (0x80A1) - 1665 calls in the shipped
     * scripts: give the player experience.
     *
     * @param amount how much experience to give
     */
    void giveExpPoints(int amount);

    /**
     * {@code op_scr_return} (0x80A2) - 6 calls in the shipped scripts:
     * hand a value back to the caller of the script's procedure.
     *
     * @param value the value to return
     */
    void scrReturn(int value);

    /**
     * {@code op_play_sfx} (0x80A3) - 76 calls in the shipped scripts:
     * play a sound effect by name.
     *
     * @param name the name of the sound
     */
    void playSfx(String name);

    /**
     * {@code op_obj_name} (0x80A4) - 2764 calls in the shipped scripts:
     * the name of an object.
     *
     * @param object the object to name
     * @return its name
     */
    String objName(Object object);

    /**
     * {@code op_get_pc_stat} (0x80A6) - 212 calls in the shipped scripts:
     * a stat of the player as a character - the unspent skill points, the
     * level, the experience, the reputation or the karma.
     *
     * @param pcStat the number the game names the stat by
     * @return the value the stat engine keeps for it
     */
    int getPcStat(int pcStat);

    /**
     * {@code op_tile_contains_pid_obj} (0x80A7) - 1221 calls in the
     * shipped scripts: the first object of a proto on a tile.
     *
     * @param tile the tile to look on
     * @param elevation the elevation of the tile
     * @param protoId the proto of the objects to look for
     * @return the first such object, or null when the tile holds none
     */
    Object tileContainsPidObj(int tile, int elevation, int protoId);

    /**
     * {@code op_set_map_start} (0x80A8) - 3 calls in the shipped scripts:
     * set where a map starts.
     *
     * <p>The elevation of the map is set, the view is centered on the
     * tile, and the start is remembered for the load of the map.
     *
     * @param x the x of the start tile
     * @param y the y of the start tile
     * @param elevation the elevation of the start
     * @param rotation the rotation the player faces
     */
    void setMapStart(int x, int y, int elevation, int rotation);

    /**
     * {@code op_override_map_start} (0x80A9) - 56 calls in the shipped
     * scripts: move the player to the start of the map now.
     *
     * @param x the x of the start tile
     * @param y the y of the start tile
     * @param elevation the elevation of the start
     * @param rotation the rotation the player faces
     */
    void overrideMapStart(int x, int y, int elevation, int rotation);

    /**
     * {@code op_has_skill} (0x80AA) - 502 calls in the shipped scripts:
     * the value of a critter's skill.
     *
     * @param critter the critter whose skill is asked about
     * @param skill the number the game names the skill by
     * @return the value of the skill
     */
    int hasSkill(Object critter, int skill);

    /**
     * {@code op_using_skill} (0x80AB) - 103 calls in the shipped scripts:
     * whether a critter is using a skill.
     *
     * @param critter the critter to ask about
     * @param skill the number the game names the skill by
     * @return whether the skill is in use
     */
    int usingSkill(Object critter, int skill);

    /**
     * {@code op_roll_vs_skill} (0x80AC) - 1727 calls in the shipped
     * scripts: roll a critter's skill.
     *
     * @param critter the critter whose skill is rolled
     * @param skill the number the game names the skill by
     * @param modifier the modifier the roll is adjusted by
     * @return the roll, read with {@link #success} and {@link #critical}
     */
    int rollVsSkill(Object critter, int skill, int modifier);

    /**
     * {@code op_do_check} (0x80AE) - 544 calls in the shipped scripts:
     * roll a check of a critter's primary stat.
     *
     * @param critter the critter whose stat is checked
     * @param stat the number the game names the stat by
     * @param modifier the modifier the check is adjusted by
     * @return the roll, read with {@link #success} and {@link #critical}
     */
    int doCheck(Object critter, int stat, int modifier);

    /**
     * {@code op_success} (0x80AF) - 3633 calls in the shipped scripts:
     * read a roll of {@link #rollVsSkill} or {@link #doCheck}.
     *
     * @param roll the roll to read
     * @return whether the roll succeeded
     */
    int success(int roll);

    /**
     * {@code op_critical} (0x80B0) - 4442 calls in the shipped scripts:
     * read a roll of {@link #rollVsSkill} or {@link #doCheck}.
     *
     * @param roll the roll to read
     * @return whether the roll was a critical one
     */
    int critical(int roll);

    /**
     * {@code op_mark_area_known} (0x80B2) - 126 calls in the shipped
     * scripts: mark an area or a map of the worldmap as known.
     *
     * @param isMap whether the id names a map rather than an area
     * @param id the area or the map to mark
     * @param visitedState how much the player knows of it - invisible,
     * unknown, known or visited
     */
    void markAreaKnown(int isMap, int id, int visitedState);

    /**
     * {@code op_random} (0x80B4) - 9127 calls in the shipped scripts: a
     * random number between two bounds.
     *
     * @param min the lower bound, inclusive
     * @param max the upper bound, inclusive
     * @return the random number
     */
    int random(int min, int max);

    /**
     * {@code op_move_to} (0x80B6) - 823 calls in the shipped scripts:
     * move an object to a tile.
     *
     * @param object the object to move
     * @param tile the tile to move it to
     * @param elevation the elevation to move it to
     * @return 0 when it moved
     */
    int moveTo(Object object, int tile, int elevation);

    /**
     * {@code op_create_object} (0x80B7) - 6349 calls in the shipped
     * scripts: create an object of a proto.
     *
     * @param protoId the proto of the object
     * @param tile the tile to create it on
     * @param elevation the elevation to create it on
     * @param sid the script the object runs
     * @return the object, or null when it was not created
     */
    Object createObject(int protoId, int tile, int elevation, int sid);

    /**
     * {@code op_display_msg} (0x80B8) - 17015 calls in the shipped
     * scripts: show a message to the player.
     *
     * @param text the text of the message
     */
    void displayMsg(String text);

    /**
     * {@code op_script_overrides} (0x80B9) - 4196 calls in the shipped
     * scripts: the script overrides the engine's own handling of the
     * event it runs for.
     */
    void scriptOverrides();

    /**
     * {@code op_obj_is_carrying_obj} (0x80BA) - 14686 calls in the
     * shipped scripts: how many items of a proto an object carries.
     *
     * @param owner the object whose inventory is counted
     * @param protoId the proto of the items to count
     * @return how many such items it carries
     */
    int objIsCarryingObj(Object owner, int protoId);

    /**
     * {@code op_tile_contains_obj_pid} (0x80BB) - 293 calls in the
     * shipped scripts: how many objects of a proto a tile holds.
     *
     * @param tile the tile to count on
     * @param elevation the elevation of the tile
     * @param protoId the proto of the objects to count
     * @return how many such objects the tile holds
     */
    int tileContainsObjPid(int tile, int elevation, int protoId);

    /**
     * {@code op_self_obj} (0x80BC) - 97990 calls in the shipped scripts:
     * the object the script runs for.
     *
     * @return the script's owner
     */
    Object selfObj();

    /**
     * {@code op_source_obj} (0x80BD) - 8385 calls in the shipped scripts:
     * the object that caused the script to run.
     *
     * @return the source of the event
     */
    Object sourceObj();

    /**
     * {@code op_target_obj} (0x80BE) - 212 calls in the shipped scripts:
     * the target of the script's combat event.
     *
     * @return the target of the event
     */
    Object targetObj();

    /**
     * {@code opGetDude} (0x80BF) - 24317 calls in the shipped scripts:
     * the player.
     *
     * @return the player's object
     */
    Object opGetDude();

    /**
     * {@code op_obj_being_used_with} (0x80C0) - 662 calls in the shipped
     * scripts: the item the script's owner is being used with.
     *
     * @return the item of the use event
     */
    Object objBeingUsedWith();

    /**
     * {@code op_get_local_var} (0x80C1) - 20115 calls in the shipped
     * scripts: one of the script's own local variables.
     *
     * @param index which local variable to read
     * @return its value
     */
    Program.ProgramValue getLocalVar(int index);

    /**
     * {@code op_set_local_var} (0x80C2) - 16193 calls in the shipped
     * scripts: write one of the script's own local variables.
     *
     * @param index which local variable to write
     * @param value the value to write
     */
    void setLocalVar(int index, Program.ProgramValue value);

    /**
     * {@code op_get_map_var} (0x80C3) - 1955 calls in the shipped
     * scripts: one of the current map's variables.
     *
     * @param index which map variable to read
     * @return its value
     */
    Program.ProgramValue getMapVar(int index);

    /**
     * {@code op_set_map_var} (0x80C4) - 1671 calls in the shipped
     * scripts: write one of the current map's variables.
     *
     * @param index which map variable to write
     * @param value the value to write
     */
    void setMapVar(int index, Program.ProgramValue value);

    /**
     * {@code op_get_global_var} (0x80C5) - 57300 calls in the shipped
     * scripts: one of the game's global variables.
     *
     * @param index which global variable to read
     * @return its value
     */
    Program.ProgramValue getGlobalVar(int index);

    /**
     * {@code op_set_global_var} (0x80C6) - 60443 calls in the shipped
     * scripts: write one of the game's global variables.
     *
     * @param index which global variable to write
     * @param value the value to write
     */
    void setGlobalVar(int index, Program.ProgramValue value);

    /**
     * {@code op_script_action} (0x80C7) - 602 calls in the shipped
     * scripts: the action the script is running for.
     *
     * @return the action of the event
     */
    int scriptAction();

    /**
     * {@code op_obj_type} (0x80C8) - 2081 calls in the shipped scripts:
     * the type of an object - item, critter, scenery and the rest.
     *
     * @param object the object to ask about
     * @return the number of its type
     */
    int objType(Object object);

    /**
     * {@code op_item_subtype} (0x80C9) - 2541 calls in the shipped
     * scripts: the subtype of an item - armor, weapon, ammo and the rest.
     *
     * @param item the item to ask about
     * @return the number of its subtype
     */
    int itemSubtype(Object item);

    /**
     * {@code op_get_critter_stat} (0x80CA) - 5834 calls in the shipped
     * scripts: a stat of a critter.
     *
     * @param critter the critter whose stat is asked about
     * @param stat the number the game names the stat by
     * @return the value of the stat
     */
    int getCritterStat(Object critter, int stat);

    /**
     * {@code op_set_critter_stat} (0x80CB) - 29 calls in the shipped
     * scripts: change a stat of a critter.
     *
     * <p>Only the player's own stats may be changed, and the stat is
     * changed by an amount rather than set to a value.
     *
     * @param critter the critter whose stat is changed
     * @param stat the number the game names the stat by
     * @param amount the amount to change it by
     * @return 0 when it changed
     */
    int setCritterStat(Object critter, int stat, int amount);

    /**
     * {@code op_animate_stand_obj} (0x80CC) - 41 calls in the shipped
     * scripts: play an object's standing animation.
     *
     * @param object the object to stand
     */
    void animateStandObj(Object object);

    /**
     * {@code animate_stand_reverse_obj} (0x80CD) - 16 calls in the
     * shipped scripts: play an object's standing animation backwards.
     *
     * @param object the object to stand
     */
    void animateStandReverseObj(Object object);

    /**
     * {@code animate_move_obj_to_tile} (0x80CE) - 1601 calls in the
     * shipped scripts: walk an object to a tile.
     *
     * @param object the object to walk
     * @param tile the tile to walk it to
     * @param flags the flags of the walk - running and the rest
     */
    void animateMoveObjToTile(Object object, int tile, int flags);

    /**
     * {@code tile_in_tile_rect} (0x80CF) - 1073 calls in the shipped
     * scripts: whether a tile is inside a rectangle of tiles.
     *
     * <p>The bounds of the rectangle are read from the first and the
     * fourth tile; the second and the third are taken and dropped.
     * minX and maxY come from the fourth tile, maxX and minY from the
     * first, so the corners the scripts pass are the top-right and the
     * bottom-left of the rectangle.
     *
     * @param tile1 the corner with the maximum x and the minimum y
     * @param tile2 the second corner, taken and dropped
     * @param tile3 the third corner, taken and dropped
     * @param tile4 the corner with the minimum x and the maximum y
     * @param tile the tile to test
     * @return whether the tile is inside
     */
    int tileInTileRect(int tile1, int tile2, int tile3, int tile4, int tile);

    /**
     * {@code op_attack} (0x80D0) - 1184 calls in the shipped scripts:
     * attack a critter.
     *
     * <p>The original reads only from the hit bonus on: the attack type
     * and the number of attacks go nowhere. The two damage results are
     * forced on the attacker and the target when they are the same.
     *
     * @param target the critter to attack
     * @param attackType dropped by the original
     * @param numAttacks dropped by the original
     * @param hitBonus the accuracy bonus of the attack
     * @param minDamage the least damage of the attack
     * @param maxDamage the most damage of the attack
     * @param attackerResults the damage results forced on the attacker
     * @param targetResults the damage results forced on the target
     */
    void attack(Object target, int attackType, int numAttacks, int hitBonus,
            int minDamage, int maxDamage, int attackerResults, int targetResults);

    /**
     * {@code op_tile_distance} (0x80D2) - 839 calls in the shipped
     * scripts: the distance between two tiles.
     *
     * @param tile1 one tile
     * @param tile2 the other tile
     * @return the distance between them
     */
    int tileDistance(int tile1, int tile2);

    /**
     * {@code op_tile_distance_objs} (0x80D3) - 26018 calls in the shipped
     * scripts: the distance between two objects.
     *
     * @param object1 one object
     * @param object2 the other object
     * @return the distance between their tiles
     */
    int tileDistanceObjs(Object object1, Object object2);

    /**
     * {@code op_tile_num} (0x80D4) - 9044 calls in the shipped scripts:
     * the tile an object is on.
     *
     * @param object the object to locate
     * @return its tile
     */
    int tileNum(Object object);

    /**
     * {@code op_tile_num_in_direction} (0x80D5) - 5860 calls in the
     * shipped scripts: the tile a walk from a tile leads to.
     *
     * @param origin the tile to walk from
     * @param rotation the direction to walk in
     * @param distance how many tiles to walk
     * @return the tile the walk lands on
     */
    int tileNumInDirection(int origin, int rotation, int distance);

    /**
     * {@code op_drop_obj} (0x80D7) - 2 calls in the shipped scripts: drop
     * an item where its owner is.
     *
     * @param item the item to drop
     */
    void dropObj(Object item);

    /**
     * {@code op_add_obj_to_inven} (0x80D8) - 2535 calls in the shipped
     * scripts: put an item into an object's inventory.
     *
     * @param owner the object whose inventory takes the item
     * @param item the item to put in
     */
    void addObjToInven(Object owner, Object item);

    /**
     * {@code op_rm_obj_from_inven} (0x80D9) - 2169 calls in the shipped
     * scripts: take an item out of an object's inventory.
     *
     * @param owner the object whose inventory loses the item
     * @param item the item to take out
     */
    void rmObjFromInven(Object owner, Object item);

    /**
     * {@code op_wield_obj_critter} (0x80DA) - 621 calls in the shipped
     * scripts: make a critter wield an item.
     *
     * @param critter the critter to wield the item
     * @param item the item to wield
     */
    void wieldObjCritter(Object critter, Object item);

    /**
     * {@code op_use_obj} (0x80DB) - 11 calls in the shipped scripts: use
     * an item.
     *
     * @param item the item to use
     */
    void useObj(Object item);

    /**
     * {@code op_obj_can_see_obj} (0x80DC) - 1836 calls in the shipped
     * scripts: whether one object can see another.
     *
     * @param watcher the object that looks
     * @param target the object looked for
     * @return whether the watcher sees the target
     */
    int objCanSeeObj(Object watcher, Object target);

    /**
     * {@code op_start_gdialog} (0x80DE) - 1224 calls in the shipped
     * scripts: start a talking dialog with a critter.
     *
     * @param unused taken and dropped by the original
     * @param who the critter to talk with
     * @param reactionLevel the reaction the dialog starts at
     * @param head the talking head to show, or -1 for none
     * @param background the background to show, or -1 for none
     */
    void startGdialog(int unused, Object who, int reactionLevel, int head,
            int background);

    /**
     * {@code op_end_gdialog} (0x80DF) - 1224 calls in the shipped
     * scripts: end the talking dialog.
     */
    void endGdialog();

    /**
     * {@code op_metarule3} (0x80E1) - 994 calls in the shipped scripts:
     * ask the game a metarule of the three-parameter kind.
     *
     * @param rule the number the game names the rule by
     * @param param1 the first parameter it asks for
     * @param param2 the second parameter it asks for
     * @param param3 the third parameter it asks for
     * @return the answer of the rule
     */
    Program.ProgramValue metarule3(int rule, Program.ProgramValue param1,
            Program.ProgramValue param2, Program.ProgramValue param3);

    /**
     * {@code op_set_obj_visibility} (0x80E3) - 922 calls in the shipped
     * scripts: hide an object or show it again.
     *
     * @param object the object to hide or show
     * @param invisible whether to hide it - 0 shows, the rest hides
     */
    void setObjVisibility(Object object, int invisible);

    /**
     * {@code op_load_map} (0x80E4) - 82 calls in the shipped scripts:
     * load a map.
     *
     * @param map the map to load - its number, or the name it goes by
     * @param elevation the elevation to enter the map on
     */
    void loadMap(Program.ProgramValue map, int elevation);

    /**
     * {@code op_wm_area_set_pos} (0x80E5) - 96 calls in the shipped
     * scripts: move an area on the worldmap.
     *
     * @param city the area to move
     * @param x the world x to move it to
     * @param y the world y to move it to
     */
    void wmAreaSetPos(int city, int x, int y);

    /**
     * {@code op_set_exit_grids} (0x80E6) - 104 calls in the shipped
     * scripts: point every exit grid of an elevation somewhere else.
     *
     * @param elevation the elevation whose exit grids are rewritten
     * @param destinationMap the map they lead to
     * @param destinationElevation the elevation they lead to
     * @param destinationTile the tile they lead to
     * @param destinationRotation the rotation they land facing
     */
    void setExitGrids(int elevation, int destinationMap, int destinationElevation,
            int destinationTile, int destinationRotation);

    /**
     * {@code op_anim_busy} (0x80E7) - 1552 calls in the shipped scripts:
     * whether an object's animation is busy.
     *
     * @param object the object to ask about
     * @return whether an animation is playing
     */
    int animBusy(Object object);

    /**
     * {@code op_critter_heal} (0x80E8) - 166 calls in the shipped
     * scripts: heal a critter.
     *
     * @param critter the critter to heal
     * @param amount how much to heal it by
     * @return 0 when it healed
     */
    int critterHeal(Object critter, int amount);

    /**
     * {@code op_set_light_level} (0x80E9) - 2466 calls in the shipped
     * scripts: set the light of the map.
     *
     * @param level the light level, 0 dark to 100 bright
     */
    void setLightLevel(int level);

    /**
     * {@code op_game_time} (0x80EA) - 1697 calls in the shipped scripts:
     * the clock of the game.
     *
     * @return the game's time in ticks
     */
    int gameTime();

    /**
     * {@code op_elevation} (0x80EC) - 2863 calls in the shipped scripts:
     * the elevation an object is on.
     *
     * @param object the object to locate
     * @return its elevation
     */
    int elevation(Object object);

    /**
     * {@code op_kill_critter} (0x80ED) - 72 calls in the shipped
     * scripts: kill a critter with a death animation.
     *
     * @param critter the critter to kill
     * @param deathFrame the death animation to play, or 0 to destroy it
     */
    void killCritter(Object critter, int deathFrame);

    /**
     * {@code op_kill_critter_type} (0x80EE) - 85 calls in the shipped
     * scripts: kill every critter of a proto on the map.
     *
     * @param protoId the proto of the critters to kill
     * @param deathFrame the death animation to play, or 0 to destroy them
     */
    void killCritterType(int protoId, int deathFrame);

    /**
     * {@code op_critter_damage} (0x80EF) - 428 calls in the shipped
     * scripts: damage a critter.
     *
     * @param critter the critter to damage
     * @param amount how much damage to deal
     * @param damageTypeWithFlags the damage type, with the flags of the
     * knockdown and the rest packed in
     */
    void critterDamage(Object critter, int amount, int damageTypeWithFlags);

    /**
     * {@code op_add_timer_event} (0x80F0) - 2255 calls in the shipped
     * scripts: give an object a timed event.
     *
     * @param object the object the event fires for
     * @param delay how many ticks before it fires
     * @param param the parameter the event carries
     */
    void addTimerEvent(Object object, int delay, int param);

    /**
     * {@code op_rm_timer_event} (0x80F1) - 46 calls in the shipped
     * scripts: take an object's timed events off.
     *
     * @param object the object whose events come off
     */
    void rmTimerEvent(Object object);

    /**
     * {@code op_game_ticks} (0x80F2) - 1646 calls in the shipped
     * scripts: the wall-clock time of a number of game ticks.
     *
     * @param ticks how many ticks
     * @return the milliseconds they take - a tick is a tenth of a second
     */
    int gameTicks(int ticks);

    /**
     * {@code op_has_trait} (0x80F3) - 2754 calls in the shipped scripts:
     * a trait of an object.
     *
     * @param type the kind of trait asked about - object, trait or perk
     * @param critter the critter the trait is asked about
     * @param trait the number the game names the trait by
     * @return the rank or the value of the trait
     */
    int hasTrait(int type, Object critter, int trait);

    /**
     * {@code op_destroy_object} (0x80F4) - 3983 calls in the shipped
     * scripts: destroy an object.
     *
     * @param object the object to destroy
     */
    void destroyObject(Object object);

    /**
     * {@code op_obj_can_hear_obj} (0x80F5) - 7 calls in the shipped
     * scripts: whether one object can hear another.
     *
     * @param listener the object that listens
     * @param target the object listened for
     * @return whether the listener hears the target
     */
    int objCanHearObj(Object listener, Object target);

    /**
     * {@code op_game_time_hour} (0x80F6) - 5467 calls in the shipped
     * scripts: the clock of the game as a time of day.
     *
     * @return the hour as HHMMSS - 120000 is noon
     */
    int gameTimeHour();

    /**
     * {@code op_fixed_param} (0x80F7) - 1094 calls in the shipped
     * scripts: the fixed parameter the event carries.
     *
     * @return the parameter of the event
     */
    int fixedParam();

    /**
     * {@code op_tile_is_visible} (0x80F8) - 1 call in the shipped
     * scripts: whether a tile is in the view.
     *
     * @param tile the tile to test
     * @return whether the view shows it
     */
    int tileIsVisible(int tile);

    /**
     * {@code op_dialogue_system_enter} (0x80F9) - 150 calls in the
     * shipped scripts: start a dialog with the script's owner.
     */
    void dialogueSystemEnter();

    /**
     * {@code op_action_being_used} (0x80FA) - 362 calls in the shipped
     * scripts: the use action the script runs for.
     *
     * @return the action of the use event
     */
    int actionBeingUsed();

    /**
     * {@code critter_state} (0x80FB) - 507 calls in the shipped scripts:
     * the state of a critter.
     *
     * @param critter the critter to ask about
     * @return its state - standing, dead and the rest
     */
    int critterState(Object critter);

    /**
     * {@code op_game_time_advance} (0x80FC) - 370 calls in the shipped
     * scripts: move the clock of the game forward.
     *
     * @param ticks how many ticks to move it by
     */
    void gameTimeAdvance(int ticks);

    /**
     * {@code op_radiation_inc} (0x80FD) - 14 calls in the shipped
     * scripts: give a critter radiation.
     *
     * @param critter the critter to irradiate
     * @param amount how much radiation to give
     */
    void radiationInc(Object critter, int amount);

    /**
     * {@code op_radiation_dec} (0x80FE) - 17 calls in the shipped
     * scripts: take radiation off a critter.
     *
     * @param critter the critter to treat
     * @param amount how much radiation to take off
     */
    void radiationDec(Object critter, int amount);

    /**
     * {@code critter_attempt_placement} (0x80FF) - 471 calls in the
     * shipped scripts: try to place a critter on a tile.
     *
     * @param critter the critter to place
     * @param tile the tile to place it on
     * @param elevation the elevation to place it on
     * @return 0 when it was placed
     */
    int critterAttemptPlacement(Object critter, int tile, int elevation);

    /**
     * {@code op_obj_pid} (0x8100) - 4332 calls in the shipped scripts:
     * the proto of an object.
     *
     * @param object the object to ask about
     * @return its proto id
     */
    int objPid(Object object);

    /**
     * {@code op_cur_map_index} (0x8101) - 1712 calls in the shipped
     * scripts: the current map.
     *
     * @return the index of the map
     */
    int curMapIndex();

    /**
     * {@code op_critter_add_trait} (0x8102) - 1780 calls in the shipped
     * scripts: give a critter a trait.
     *
     * @param critter the critter to give it to
     * @param kind the kind of trait - object, trait or perk
     * @param param the slot the trait goes in
     * @param value the trait to give
     * @return 0 when it was given
     */
    int critterAddTrait(Object critter, int kind, int param, int value);

    /**
     * {@code critter_rm_trait} (0x8103) - 4 calls in the shipped scripts:
     * take a trait off a critter.
     *
     * @param critter the critter to take it off
     * @param kind the kind of trait - object, trait or perk
     * @param param the slot the trait sits in
     * @param value the trait to take off
     * @return 0 when it was taken
     */
    int critterRmTrait(Object critter, int kind, int param, int value);

    /**
     * {@code op_proto_data} (0x8104) - 131 calls in the shipped scripts:
     * a member of a proto.
     *
     * @param protoId the proto to read
     * @param member the number the game names the member by
     * @return the member's value - a number or a string
     */
    Program.ProgramValue protoData(int protoId, int member);

    /**
     * {@code op_message_str} (0x8105) - 32040 calls in the shipped
     * scripts: a message of a message list.
     *
     * @param messageListId the message list to read
     * @param messageIndex which message of the list
     * @return the text of the message
     */
    String messageStr(int messageListId, int messageIndex);

    /**
     * {@code op_critter_inven_obj} (0x8106) - 11327 calls in the shipped
     * scripts: what a critter carries, or how much.
     *
     * @param critter the critter whose inventory is asked about
     * @param slot what to ask for - the armor, a hand, or the count
     * @return the item in the slot, or the number of items
     */
    Program.ProgramValue critterInvenObj(Object critter, int slot);

    /**
     * {@code op_obj_set_light_level} (0x8107) - 4 calls in the shipped
     * scripts: set the light an object gives off.
     *
     * @param object the object to light
     * @param lightIntensity how bright it shines, 0 to 100
     * @param lightDistance how far it shines
     */
    void objSetLightLevel(Object object, int lightIntensity, int lightDistance);

    /**
     * {@code op_inven_cmds} (0x8109) - 4 calls in the shipped scripts:
     * run a command on a critter's inventory.
     *
     * @param object the critter whose inventory is commanded
     * @param command the command to run - 13 reads an item by index
     * @param index the index the command works on
     * @return the item the command answers with
     */
    Object invenCmds(Object object, int command, int index);

    /**
     * {@code op_float_msg} (0x810A) - 3942 calls in the shipped scripts:
     * float a message over an object.
     *
     * <p>The object the text belongs to is taken off the stack by the
     * dispatch, because the floating text needs the render engine.
     *
     * @param text the text to float
     * @param type the color and the flags of the message
     */
    void floatMsg(Program.ProgramValue text, int type);

    /**
     * {@code op_metarule} (0x810B) - 15675 calls in the shipped scripts:
     * ask the game a metarule.
     *
     * @param rule the number the game names the rule by
     * @param param the number or the object it asks for
     * @return the answer of the rule
     */
    int metarule(int rule, Program.ProgramValue param);

    /**
     * {@code op_anim} (0x810C) - 866 calls in the shipped scripts: play
     * an animation of an object.
     *
     * @param object the object to animate
     * @param direction the direction the macros put here; the original
     * drops it
     * @param anim the animation to play - 1000 and 1010 set the frame
     * @param frame the frame to land on, or the object the macro bug
     * passes instead of a rotation
     */
    void anim(Object object, int direction, int anim, Program.ProgramValue frame);

    /**
     * {@code op_obj_carrying_pid_obj} (0x810D) - 5004 calls in the
     * shipped scripts: the first item of a proto an object carries.
     *
     * @param owner the object whose inventory is searched
     * @param protoId the proto of the items to find
     * @return the first such item, or null when it carries none
     */
    Object objCarryingPidObj(Object owner, int protoId);

    /**
     * {@code op_reg_anim_func} (0x810E) - 1344 calls in the shipped
     * scripts: begin or end a batch of registered animations.
     *
     * @param cmd the command - begin, clear or end
     * @param param the parameter the command takes
     */
    void regAnimFunc(int cmd, Program.ProgramValue param);

    /**
     * {@code op_reg_anim_animate} (0x810F) - 223 calls in the shipped
     * scripts: register an animation of an object.
     *
     * @param object the object to animate
     * @param anim the animation to play
     * @param delay how many ticks to wait before it plays
     */
    void regAnimAnimate(Object object, int anim, int delay);

    /**
     * {@code op_reg_anim_animate_reverse} (0x8110) - 2 calls in the
     * shipped scripts: register a reversed animation of an object.
     *
     * @param object the object to animate
     * @param anim the animation to play backwards
     * @param delay how many ticks to wait before it plays
     */
    void regAnimAnimateReverse(Object object, int anim, int delay);

    /**
     * {@code op_reg_anim_obj_move_to_tile} (0x8113) - 5 calls in the
     * shipped scripts: register a walk of an object to a tile.
     *
     * @param object the object to walk
     * @param tile the tile to walk it to
     * @param delay how many ticks to wait before it walks
     */
    void regAnimObjMoveToTile(Object object, int tile, int delay);

    /**
     * {@code op_play_gmovie} (0x8115) - 9 calls in the shipped scripts:
     * play a game movie.
     *
     * @param movie the number of the movie
     */
    void playGmovie(int movie);

    /**
     * {@code op_add_mult_objs_to_inven} (0x8116) - 2482 calls in the
     * shipped scripts: put many copies of an item into an inventory.
     *
     * @param owner the object whose inventory takes them
     * @param item the item to put in
     * @param count how many copies to put in
     */
    void addMultObjsToInven(Object owner, Object item, int count);

    /**
     * {@code rm_mult_objs_from_inven} (0x8117) - 2351 calls in the
     * shipped scripts: take many copies of an item out of an inventory.
     *
     * @param owner the object whose inventory loses them
     * @param item the item to take out
     * @param count how many copies to take out
     * @return how many were taken
     */
    int rmMultObjsFromInven(Object owner, Object item, int count);

    /**
     * {@code op_month} (0x8118) - 897 calls in the shipped scripts: the
     * month of the game's calendar.
     *
     * @return the month, 0 to 11
     */
    int month();

    /**
     * {@code op_day} (0x8119) - 52 calls in the shipped scripts: the day
     * of the game's calendar.
     *
     * @return the day of the month, 1 to 31
     */
    int day();

    /**
     * {@code op_explosion} (0x811A) - 212 calls in the shipped scripts:
     * make an explosion on a tile.
     *
     * @param tile the tile to explode on
     * @param elevation the elevation to explode on
     * @param maxDamage the most damage the explosion deals
     */
    void explosion(int tile, int elevation, int maxDamage);

    /**
     * {@code _op_gsay_start} (0x811C) - 1221 calls in the shipped
     * scripts: start a gsay dialog.
     */
    void opGsayStart();

    /**
     * {@code _op_gsay_end} (0x811D) - 1221 calls in the shipped scripts:
     * end a gsay dialog.
     */
    void opGsayEnd();

    /**
     * {@code op_gsay_reply} (0x811E) - 10354 calls in the shipped
     * scripts: set the reply of a gsay dialog.
     *
     * @param messageListId the message list the reply comes from
     * @param message the message of the reply, or its number in the list
     */
    void gsayReply(int messageListId, Program.ProgramValue message);

    /**
     * {@code op_gsay_option} (0x811F) - 214 calls in the shipped scripts:
     * add an option to a gsay dialog.
     *
     * @param messageListId the message list the option comes from
     * @param message the message of the option, or its number in the list
     * @param procedure the procedure that answers it - a string names it,
     * an integer is its index
     * @param reaction the reaction the option is shown at
     */
    void gsayOption(int messageListId, Program.ProgramValue message,
            Program.ProgramValue procedure, int reaction);

    /**
     * {@code op_gsay_message} (0x8120) - 936 calls in the shipped
     * scripts: show a message of a gsay dialog and end the round.
     *
     * @param messageListId the message list the message comes from
     * @param message the message to show, or its number in the list
     * @param reaction the reaction the message is shown at
     */
    void gsayMessage(int messageListId, Program.ProgramValue message, int reaction);

    /**
     * {@code op_giq_option} (0x8121) - 26082 calls in the shipped
     * scripts: add an option to a gsay dialog, for the smart enough.
     *
     * <p>The option is only added when the player's intelligence - with
     * the Smooth Talker perk - is not below the asked one.
     *
     * @param iq the intelligence the option asks for; negative asks for
     * that exact intelligence or above
     * @param messageListId the message list the option comes from
     * @param message the message of the option, or its number in the list
     * @param procedure the procedure that answers it - a string names it,
     * an integer is its index
     * @param reaction the reaction the option is shown at
     */
    void giqOption(int iq, int messageListId, Program.ProgramValue message,
            Program.ProgramValue procedure, int reaction);

    /**
     * {@code op_poison} (0x8122) - 44 calls in the shipped scripts:
     * poison a critter.
     *
     * @param critter the critter to poison
     * @param amount how much poison to give
     */
    void poison(Object critter, int amount);

    /**
     * {@code op_get_poison} (0x8123) - 126 calls in the shipped scripts:
     * the poison of a critter.
     *
     * @param critter the critter to ask about
     * @return how poisoned it is
     */
    int getPoison(Object critter);

    /**
     * {@code op_party_add} (0x8124) - 43 calls in the shipped scripts:
     * take a critter into the party.
     *
     * @param who the critter to take in
     */
    void partyAdd(Object who);

    /**
     * {@code op_party_remove} (0x8125) - 91 calls in the shipped scripts:
     * take a critter out of the party.
     *
     * @param who the critter to take out
     */
    void partyRemove(Object who);

    /**
     * {@code op_reg_anim_animate_forever} (0x8126) - 2 calls in the
     * shipped scripts: register an animation that never ends.
     *
     * @param object the object to animate
     * @param anim the animation to loop
     */
    void regAnimAnimateForever(Object object, int anim);

    /**
     * {@code op_critter_injure} (0x8127) - 301 calls in the shipped
     * scripts: injure a critter.
     *
     * @param critter the critter to injure
     * @param flags how to injure it - knock down, cripple and the rest
     */
    void critterInjure(Object critter, int flags);

    /**
     * {@code op_is_in_combat} (0x8128) - 322 calls in the shipped
     * scripts: whether the game is in combat.
     *
     * @return whether combat is on
     */
    int isInCombat();

    /**
     * {@code op_gdialog_barter} (0x8129) - 77 calls in the shipped
     * scripts: open the barter screen of the dialog.
     *
     * @param modifier the buy-and-sell modifier the barter starts with
     */
    void gdialogBarter(int modifier);

    /**
     * {@code op_inven_unwield} (0x812C) - 28 calls in the shipped
     * scripts: take the wielded item off the player.
     */
    void invenUnwield();

    /**
     * {@code op_obj_is_locked} (0x812D) - 416 calls in the shipped
     * scripts: whether an object is locked.
     *
     * @param object the object to ask about
     * @return whether it is locked
     */
    int objIsLocked(Object object);

    /**
     * {@code op_obj_lock} (0x812E) - 553 calls in the shipped scripts:
     * lock an object.
     *
     * @param object the object to lock
     */
    void objLock(Object object);

    /**
     * {@code op_obj_unlock} (0x812F) - 682 calls in the shipped scripts:
     * unlock an object.
     *
     * @param object the object to unlock
     */
    void objUnlock(Object object);

    /**
     * {@code op_obj_is_open} (0x8130) - 170 calls in the shipped scripts:
     * whether an object is open.
     *
     * @param object the object to ask about
     * @return whether it is open
     */
    int objIsOpen(Object object);

    /**
     * {@code op_obj_open} (0x8131) - 90 calls in the shipped scripts:
     * open an object.
     *
     * @param object the object to open
     */
    void objOpen(Object object);

    /**
     * {@code op_obj_close} (0x8132) - 235 calls in the shipped scripts:
     * close an object.
     *
     * @param object the object to close
     */
    void objClose(Object object);

    /**
     * {@code op_game_ui_disable} (0x8133) - 87 calls in the shipped
     * scripts: hide the game's interface.
     */
    void gameUiDisable();

    /**
     * {@code op_game_ui_enable} (0x8134) - 89 calls in the shipped
     * scripts: show the game's interface again.
     */
    void gameUiEnable();

    /**
     * {@code op_gfade_out} (0x8136) - 462 calls in the shipped scripts:
     * fade the screen to black.
     *
     * @param duration how fast to fade
     */
    void gfadeOut(int duration);

    /**
     * {@code op_gfade_in} (0x8137) - 460 calls in the shipped scripts:
     * fade the screen back in.
     *
     * @param duration how fast to fade
     */
    void gfadeIn(int duration);

    /**
     * {@code op_item_caps_total} (0x8138) - 632 calls in the shipped
     * scripts: the bottle caps an object holds.
     *
     * @param object the object to count on
     * @return how many caps it holds
     */
    int itemCapsTotal(Object object);

    /**
     * {@code op_item_caps_adjust} (0x8139) - 889 calls in the shipped
     * scripts: change the bottle caps an object holds.
     *
     * @param object the object to adjust
     * @param amount how many caps to add, or take off when negative
     * @return the new total
     */
    int itemCapsAdjust(Object object, int amount);

    /**
     * {@code op_reg_anim_play_sfx} (0x813B) - 28 calls in the shipped
     * scripts: register a sound in a batch of animations.
     *
     * @param object the object the sound is for
     * @param sfxName the name of the sound
     * @param delay how many ticks to wait before it plays
     */
    void regAnimPlaySfx(Object object, String sfxName, int delay);

    /**
     * {@code op_critter_mod_skill} (0x813C) - 60 calls in the shipped
     * scripts: change a skill of the player.
     *
     * @param critter the player - the original refuses anybody else
     * @param skill the number the game names the skill by
     * @param points how many points to move it by; a tagged skill moves
     * half as many
     */
    void critterModSkill(Object critter, int skill, int points);

    /**
     * {@code op_sfx_build_char_name} (0x813D) - 21 calls in the shipped
     * scripts: build the name of a critter's sound.
     *
     * @param object the critter the sound is for
     * @param anim the animation the sound goes with
     * @param soundEffect the kind of sound - a scuff, a die and the rest
     * @return the name of the sound
     */
    String sfxBuildCharName(Object object, int anim, int soundEffect);

    /**
     * {@code op_sfx_build_weapon_name} (0x8141) - 4 calls in the shipped
     * scripts: build the name of a weapon's sound.
     *
     * @param weaponSfxType the kind of sound - swing, hit and the rest
     * @param weapon the weapon the sound is for
     * @param hitMode how the weapon is swung
     * @param target what the weapon hits
     * @return the name of the sound
     */
    String sfxBuildWeaponName(int weaponSfxType, Object weapon, int hitMode,
            Object target);

    /**
     * {@code op_attack_setup} (0x8143) - 206 calls in the shipped
     * scripts: set up a combat between two critters.
     *
     * @param attacker the critter that attacks
     * @param defender the critter that defends
     */
    void attackSetup(Object attacker, Object defender);

    /**
     * {@code op_destroy_mult_objs} (0x8144) - 4 calls in the shipped
     * scripts: destroy many copies of an item.
     *
     * @param object the item to destroy
     * @param quantity how many copies to destroy
     * @return how many were destroyed
     */
    int destroyMultObjs(Object object, int quantity);

    /**
     * {@code op_use_obj_on_obj} (0x8145) - 91 calls in the shipped
     * scripts: use an item on an object.
     *
     * @param item the item to use
     * @param target the object to use it on
     */
    void useObjOnObj(Object item, Object target);

    /**
     * {@code op_endgame_slideshow} (0x8146) - 1 call in the shipped
     * scripts: play the slideshow of the ending.
     */
    void endgameSlideshow();

    /**
     * {@code op_move_obj_inven_to_obj} (0x8147) - 247 calls in the
     * shipped scripts: move everything one object carries to another.
     *
     * @param source the object whose inventory moves
     * @param destination the object whose inventory takes it
     */
    void moveObjInvenToObj(Object source, Object destination);

    /**
     * {@code op_obj_art_fid} (0x8149) - 39 calls in the shipped scripts:
     * the art of an object.
     *
     * @param object the object to ask about
     * @return its frame id
     */
    int objArtFid(Object object);

    /**
     * {@code op_art_anim} (0x814A) - 20 calls in the shipped scripts:
     * the animation built into a frame id.
     *
     * @param fid the frame id to read
     * @return the animation index it carries
     */
    int artAnim(int fid);

    /**
     * {@code op_party_member_obj} (0x814B) - 55310 calls in the shipped
     * scripts: the party member of a proto.
     *
     * @param pid the proto of the member
     * @return the member, or null when it is not in the party
     */
    Object partyMemberObj(int pid);

    /**
     * {@code op_rotation_to_tile} (0x814C) - 2149 calls in the shipped
     * scripts: the rotation one tile is seen from another at.
     *
     * @param fromTile the tile to face from
     * @param toTile the tile to face
     * @return the rotation between them
     */
    int rotationToTile(int fromTile, int toTile);

    /**
     * {@code op_jam_lock} (0x814D) - 67 calls in the shipped scripts:
     * jam an object's lock.
     *
     * @param object the object whose lock is jammed
     */
    void jamLock(Object object);

    /**
     * {@code op_gdialog_set_barter_mod} (0x814E) - 21 calls in the
     * shipped scripts: set the buy-and-sell modifier of the barter.
     *
     * @param modifier the modifier to set
     */
    void gdialogSetBarterMod(int modifier);

    /**
     * {@code op_obj_on_screen} (0x8150) - 49 calls in the shipped
     * scripts: whether an object is in the view.
     *
     * @param object the object to ask about
     * @return whether the view shows it
     */
    int objOnScreen(Object object);

    /**
     * {@code op_critter_is_fleeing} (0x8151) - 340 calls in the shipped
     * scripts: whether a critter is running away.
     *
     * @param critter the critter to ask about
     * @return whether it is fleeing
     */
    int critterIsFleeing(Object critter);

    /**
     * {@code op_critter_set_flee_state} (0x8152) - 30 calls in the
     * shipped scripts: make a critter run away, or stop.
     *
     * @param critter the critter to set
     * @param state whether it flees
     */
    void critterSetFleeState(Object critter, int state);

    /**
     * {@code op_terminate_combat} (0x8153) - 27 calls in the shipped
     * scripts: stop the combat.
     */
    void terminateCombat();

    /**
     * {@code op_debug_msg} (0x8154) - 13552 calls in the shipped scripts:
     * write a debug message.
     *
     * @param text the text to write
     */
    void debugMsg(String text);

    /**
     * {@code op_critter_stop_attacking} (0x8155) - 1 call in the shipped
     * scripts: make a critter stop attacking.
     *
     * @param critter the critter to stop
     */
    void critterStopAttacking(Object critter);

}
