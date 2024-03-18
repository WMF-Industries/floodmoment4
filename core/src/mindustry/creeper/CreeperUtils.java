package mindustry.creeper;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class CreeperUtils{
    public static StringBuilder sb = new StringBuilder();
    public static final float updateInterval = 2/60f; // Base update interval in seconds
    public static final float baseTransferRate = 0.25f; // Base transfer rate NOTE: keep below 0.25f
    public static final float creeperDamage = 0.2f; // Base creeper damage
    // Base damage increase added every second a building is being damaged by flood, resets after 5 seconds without damage or on building death
    public static final float creeperDamageScaling = 0.0025f; // This is really just a hacky way of fighting with mender spam
    public static final float creeperEvaporationUponDamagePercent = 0.98f; // Creeper percentage that will remain upon damaging something
    public static final float creeperUnitDamage = 2f;
    public static final float maxTileCreep = 10.5f;
    public static final float creeperBlockDamageMultiplier = 1f;

    public static final boolean useAiController = false; // Whether to use unit AI controller instead of RTS ai


    //TODO: Better implementation - rely on FloodCompat for looks
    public static BulletType sporeType = UnitTypes.arkyid.weapons.get(UnitTypes.arkyid.weapons.size > 4 ? 6 : 3).bullet; // 6 and 7 when mirrored, 3 before mirroring

    public static float sporeMaxRange = 800f;
    public static float sporeAmount = 20f;
    public static float sporeRadius = 5f;
    public static float sporeSpeedMultiplier = 0.15f;
    public static float sporeHealthMultiplier = 10f;
    public static boolean sporeScaleThreat = true;
    public static double sporeBaseMultifireChance = 0.01d;
    public static float sporeCreepUse = 1.25f;

    public static float unitShieldDamageMultiplier = 1.5f;

    public static float nullifierRange = 16 * tilesize;
    public static float erekirNullifyTime = 45;

    public static float radarBeamDamage = 600f; // damage the radar creeper beam deals to units

    public static float creepTowerDeposit = 3f; // amount of creep deposited by the creep tower after the building is killed
    public static float creepTowerDamage = 50f; // base damage per second, multiplied by target's size
    public static float creepTowerRange = 300f; // just slightly bigger than ripple's range


    public static float suspendDamage = 1500f; // Damage that needs to be applied for the core to be suspended
    public static float suspendTimeout = 180f; // The amount of ticks a core remains suspended (resets upon enough damage applied)

    public static int nullificationPeriod = 5; // How many seconds all cores have to be nullified (suspended) in order for the game to end
    public static float preparationPeriod = 900f; // How many seconds of preparation time pvp should have (core zones active)
    public static final int maxProtectionRadius = 10 * tilesize; // Max core no build zone range

    public static int tutorialID, pvpTutorialID, floodStatID, messageTimer, sporeLauncherCount;
    public static boolean loadedSave, hasLoaded;
    private static float updateTimer;
    private static int nullifiedCount, checkRefresh, pulseOffset;

    public static final Team creeperTeam = Team.blue;

    public static final IntMap<Block> creeperBlocks = new IntMap<>();
    public static final ObjectIntMap<Block> creeperLevels = new ObjectIntMap<>();

    public static Seq<Emitter> creeperEmitters = new Seq<>();
    public static Seq<ChargedEmitter> chargedEmitters = new Seq<>();

    public static Timer.Task fixedRunner, pvpUpdater;

    public static final String[][] tutContinue = {{"[#49e87c]\uE829 Continue[]"}};
    public static final String[][] tutFinal = {{"[#49e87c]\uE829 Finish[]"}};
    public static final String[][] tutStart = {{"[#49e87c]\uE875 Take the tutorial[]"}, {"[#e85e49]⚠ Skip (not recommended)[]"}};
    public static final String[] tutEntries = {
        "[accent]\uE875[] Tutorial 1/6", "In [#e056f0]\uE83B the flood[] there are [scarlet]no units[] to defeat.\nInstead, your goal is to suspend all [accent]emitters[], which are simply [accent]enemy cores, launchpads and accelerators.[]",
        "[accent]\uE875[] Tutorial 2/6", "[scarlet]⚠ Beware! ⚠[]\n[accent]Emitters[] spawn [#e056f0]\uE83B the flood[], which when in proximity to friendly buildings or units, damages them.",
        "[accent]\uE875[] Tutorial 3/6", "[scarlet]⚠ Beware! ⚠[]\n[accent]Charged Emitters[] spawn [#e056f0]\uE83B the flood[] much faster, but they are only active for short periods.\nWhen active or surrounded by lots of flood, they are immune to damage.",
        "[accent]\uE875[] Tutorial 4/6", "You can [accent]suspend emitters[] by constantly damaging them, and destroy [accent]charged emitters[] to remove them.",
        "[accent]\uE875[] Tutorial 5/6", "If [accent]emitters[] are sufficiently suspended, you can [accent]nullify them[] by building and activating an \uF871 [accent]Impact Reactor[] / \uF689 [accent]Lustre[] nearby.",
        "[accent]\uE875[] Tutorial 6/6", "If [accent]emitters[] are surrounded by the maximum creep, they will begin [stat]upgrading[].\nYou can stop the upgrade by damaging them.",
        "[white]\uF872[] Content 1/8", "[scarlet]Spore Launchers[]\n[accent]Thorium Reactors[] fire long distance artillery that releases [accent]a huge amount of flood[] on impact.\nYou can defend against this with \uF80E [accent]Segments[white] & \uF898 []Force Projectors[].",
        "[white]\uF682[] Content 2/8", "[scarlet]Flood Projector[]\n[accent]Shockwave Towers[] rapidly deposit flood at any nearby buildings, forcing a [accent]different approach[] than turret spam.\nRange is slightly larger than Ripples.",
        "[white]\uF6AD[] Content 3/8", "[scarlet]Flood Radar[]\n[accent]Radars[] focus on the closest unit, and after a short time of charging, [accent]shoot[] at that unit, forcing a [accent]different approach[] than unit spam.\nRange is slightly larger than Ripples.",
        "[white]\uF7FA[] Content 4/8", "[scarlet]Flood Creep[]\n[accent]Crawler tree units[] explode when in contact with buildings and release tons of [#e056f0]the flood[].",
        "[white]\uF88B[] Content 5/8", "[scarlet]Flood Mass Drivers[]\n[accent]Mass Drivers[] collect and transport creep over long distances, if not used for regular item transport.",
        "[white]\uF898[] Content 6/8", "[lime]Flood Shields[]\n[accent]Force Projectors[] and [accent]unit shields[] affect [#e056f0]the flood[].\nUnlike unit shields, \uF898 do not absorb the flood, instead they slow down the spreading by a lot!",
        "[white]\uF7F5[] Content 7/8", "[lime]Flood Horizons[]\n[accent]Horizons[] are disarmed and immune to the flood.\nUse them to transport items over the flood.",
        "[white]\uF7FA[] Content 8/8", "[lime]Flood Anticreep[]\n[accent]Crawlers, Scathe Missiles & Shock Mines[] from the player team spread anticreep on contact with the flood!"
    };
    public static final String[] pvpTutEntries = {
        "[accent]\uE875[] Tutorial 1/3", "In [#e056f0]\uE83B flood pvp[], your goal is to defeat your enemy as well as defend yourself from [#e056f0]the flood[].\nSuspending emitters does not end the game.",
        "[accent]\uE875[] Tutorial 2/3", "After the game starts, [accent]Polygonal Core Protection[] is enabled for [accent]15 minutes[]\nUse the given protection period to prepare your defenses!",
        "[accent]\uE875[] Tutorial 3/3", "Content wise, flood pvp is nearly identical to standard flood.\nThe strategies used on a standard flood server might prove to also be effective here.",
        "[white]\uF7F7[] Content 1/1", "[lime]PvP Creep[]\n\uF7F7 & \uF7DE explode when in contact with enemy team's units & buildings and release tons of [#e056f0]the flood[].\nThis ability is disabled once all emitters are nullified.",
    };
    public static final String[] floodStats = {
        "[accent]\uE87C[] Stats [BETA]", "Many blocks on flood were modified to fit in, or be balanced.\nThis part covers global values of modified blocks!",
        "[accent]\uE871[] Global Stats 1/4", "\uF858 [Damage: \uF832 66 -> 10, \uF831 105 -> 20] [Pierce: True -> False]\n\uF85C [Damage: 140 -> 10] [Pierce: True -> False]\n\uF85B [Damage: 20 -> 4]" +
        "\n\uF688 [Damage: 1500 -> 700] [Splash Damage: 160 -> 70]\n[Building Multiplier: -80% -> -70%]\n\uF801 [Damage: 18 -> 360] [Range: 30 -> 28.75]",
        "[accent]\uE86B[] Global Stats 2/4", "\uF8A0\uF8AC\uF8A8 [Solid: True -> False]\n\uF8A6 [Deflect Chance: 10 -> 0]\n\uF8A4, \uF693 [Lightning Chance: 5% -> 0%]\n\uF6EE, \uF6F3, \uF6BB [Insulated & Absorbs Lasers: False -> True]",
        "[purple]\uE833[][accent]\uE86D[] Global Stats 3/4", "\uF7F6 [Health: 70 -> 275] [Range: 13 -> 17.5]\n\uF7F5 [Health: 340 -> 440] [Speed: 12.37 -> 12.75]\n\uF7F4 [Health: 700 -> 1400] [Speed: 12.75 -> 13.5]\n\uF7EB\uF7EA\uF7E9 [Flood Damage Multiplier: 100% (walls only)]" +
        "\n\uF7FA [Health: 200 -> 100] [Speed: 7.5 -> 11.25]\n\uF7F9 [Speed: 4.5 -> 3.75]\n\uF7F8 [Speed: 4.05 -> 3] [Damage: 23 & 18 -> 25 & 20] [Heal On Hit: True -> False]\n\uF7F7 [Speed: 4.5 -> 3.75] [Heal On Hit: True -> False]" +
        "\n\uF7C1 [Damage: 360 -> 240] [Build While Flying/Shooting: True -> False]\n\uF7ED\uF7FE\uF7F7 [Bullets Collide: False -> True]\n\uF7FA\uF7F8\uF7F7 [Target Air: True -> False]\n\uF7FC\uF7EC [Abilities: Shield Regen Ability -> null]\n\uF7C2 [Shield Health: 7000 -> 15000]",
        "[orange]\uE833[][accent]\uE86D[] Global Stats 4/4", "\uF69E [Bullets Collide: False -> True]",
        "[accent]\uE87C[] Stats [BETA]", "You've reached the end of the global part!\nThis next part covers values of modified blocks that only affect the flood team!",
        "[accent]\uE83B[] Flood Stats", "\uF8A0 [Health: 50]\n\uF8AC [Health: 75], \uF8A8 [Health: 100], \uF8A6 [Health: 125]\n\uF8A4 [Health: 150], \uF693 [Health: 175], \uF8AA [Health: 200]\n\uF6EE [Health: 225], \uF6F3 [Health: 250], \uF6BB [Health: 300]" +
        "\n\uF6AD [Damage: 600] [Range: 34]\n\uF682 [Damage: 50 * Target Size] [Range: 37.5]"
    };
    static Seq<String> uuidLog = new Seq<>();
    static ObjectIntMap<String> leaveMap = new ObjectIntMap<>();
    static ObjectFloatMap<String> mapKeeper = new ObjectFloatMap<>();

    public static String getTrafficlightColor(double value){
        return "#" + Integer.toHexString(java.awt.Color.HSBtoRGB((float)value / 3f, 1f, 1f)).substring(2);
    }

    public static float[] targetSpore(float x, float y, float range){
        float[] ret = {0, 0, 0};
        int iterations = 0, maxIterations = Math.max(7500, 20000 - (sporeLauncherCount * 2500));
        if(range <= 0) range = sporeMaxRange;

        while(iterations++ < maxIterations){
            ret[0] = x + Mathf.range(range);
            ret[1] = y + Mathf.range(range);
            Tile retTile = world.tileWorld(ret[0], ret[1]);
            if(retTile != null){
                float dist = retTile.dst(x, y);
                if(retTile.creeperable && dist <= range){
                    ret[2] = dist / ((sporeType.speed * sporeType.lifetime) * sporeSpeedMultiplier); // gets the lifetime multiplier for the spore
                    return ret;
                }
            }
        }

        return new float[] {x, y, 0.1f}; // shoot yourself NOW
    }

    public static void sporeCollision(Bullet bullet, float x, float y){
        Tile tile = world.tileWorld(x, y);
        if(invalidTile(tile)) return;

        depositCreeper(tile, sporeRadius, sporeAmount);
    }

    public static void tryAddEmitter(Building build){
        if(build.team != creeperTeam || world.isGenerating()) return;

        if(Emitter.emitterTypes.containsKey(build.block)){
            creeperEmitters.add(new Emitter(build));
            CreeperUtils.resetDistanceCache();
            verifyUpgrade(build);
        }else if(ChargedEmitter.chargedEmitterTypes.containsKey(build.block)){
            chargedEmitters.add(new ChargedEmitter(build));
            CreeperUtils.resetDistanceCache();
            verifyUpgrade(build);
        }
    }

    public static void init(){
        // for FloodCompat
        netServer.addPacketHandler("flood", (player, version) -> {
            Call.clientPacketReliable(player.con, "flood", "1");
            /* if(Strings.parseFloat(version) < 0.1f) player.sendMessage("[scarlet]Your FloodCompat is outdated.\nConsider updating for the newest features!"); TODO: This is broken and triggers for every floodcompat user smh*/
            if(Strings.parseFloat(version) != 0){
                player.hasCompat = true;
            }
        });

        SaveVersion.addCustomChunk("flood-data", new CreeperSaveIO());
        sporeType.isCreeper = true;

        // walls since conveyors no longer work :{
        creeperBlocks.put(0, Blocks.air);
        creeperBlocks.put(1, Blocks.scrapWall);
        creeperBlocks.put(2, Blocks.titaniumWall);
        creeperBlocks.put(3, Blocks.thoriumWall);
        creeperBlocks.put(4, Blocks.phaseWall);
        creeperBlocks.put(5, Blocks.surgeWall);
        creeperBlocks.put(6, Blocks.reinforcedSurgeWall);
        creeperBlocks.put(7, Blocks.plastaniumWall);
        creeperBlocks.put(8, Blocks.berylliumWall);
        creeperBlocks.put(9, Blocks.tungstenWall);
        creeperBlocks.put(10, Blocks.carbideWall);

        // this is purely for damage multiplication
        creeperBlocks.put(12, Blocks.radar);
        creeperBlocks.put(13, Blocks.shockwaveTower);
        creeperBlocks.put(14, Blocks.massDriver);
        creeperBlocks.put(15, Blocks.thoriumReactor);

        creeperBlocks.put(20, Blocks.coreShard);
        creeperBlocks.put(25, Blocks.coreFoundation);
        creeperBlocks.put(30, Blocks.coreNucleus);

        creeperBlocks.put(31, Blocks.launchPad);
        creeperBlocks.put(32, Blocks.interplanetaryAccelerator);

        creeperBlocks.put(75, Blocks.coreBastion);
        creeperBlocks.put(76, Blocks.coreCitadel);
        creeperBlocks.put(77, Blocks.coreAcropolis);


        for(var set : creeperBlocks.entries()){
            set.value.creeperBlock = true; // used to avoid queuing broken creeper blocks on creeper team
           creeperLevels.put(set.value, set.key);
        }

        Emitter.init();
        ChargedEmitter.init();

        int menuID = 0;
        for(int i = tutEntries.length; --i >= 0;){ // TODO: Clean this nonsense up
            final int j = i;
            int current = menuID;
            menuID = Menus.registerMenu((player, selection) -> {
                if(selection == 1) return;
                if(j == tutEntries.length / 2) return;
                Call.menu(player.con, current, tutEntries[2 * j], tutEntries[2 * j + 1], j == tutEntries.length / 2 - 1 ? tutFinal : tutContinue);
            });
        }

        int pvpMenuID = 0;
        for(int pi = pvpTutEntries.length; --pi >= 0;){
            final int pj = pi;
            int current = pvpMenuID;
            pvpMenuID = Menus.registerMenu((player, selection) -> {
                if(selection == 1) return;
                if(pj == pvpTutEntries.length / 2) return;
                Call.menu(player.con, current, pvpTutEntries[2 * pj], pvpTutEntries[2 * pj + 1], pj == pvpTutEntries.length / 2 - 1 ? tutFinal : tutContinue);
            });
        }

        int statMenuID = 0;
        for(int pi = floodStats.length; --pi >= 0;){
            final int pj = pi;
            int current = statMenuID;
            statMenuID = Menus.registerMenu((player, selection) -> {
                if(selection == 1) return;
                if(pj == floodStats.length / 2) return;
                Call.menu(player.con, current, floodStats[2 * pj], floodStats[2 * pj + 1], pj == floodStats.length / 2 - 1 ? tutFinal : tutContinue);
            });
        }

        tutorialID = menuID;
        pvpTutorialID = pvpMenuID;
        floodStatID = statMenuID;

        Events.on(EventType.PlayerJoin.class, e -> {
            if(leaveMap.get(e.player.uuid()) >= 5){
                Call.menu(e.player.con(), 0, "Connection Issues",
                "We've noticed that you have been disconnecting and rejoining many times in a short time." +
                "\nIf you're experiencing issues with playing on the server, check your internet connection strength," +
                " restart your device or try again later!", tutFinal);
                clearMaps(e.player.uuid());
            }
            if(e.player.getInfo().timesJoined > 1) return;
            Call.menu(e.player.con, state.rules.pvp ? pvpTutorialID : tutorialID, "[accent]Welcome![]", "Looks like it's your first time playing..", tutStart);
        });

        Events.on(EventType.PlayerLeave.class, p -> {
            if(!uuidLog.contains(p.player.uuid())) uuidLog.add(p.player.uuid());
            int tmp = 1 + leaveMap.get(p.player.uuid());
            leaveMap.remove(p.player.uuid());
            leaveMap.put(p.player.uuid(), tmp);
            mapKeeper.remove(p.player.uuid(), 0);
            mapKeeper.put(p.player.uuid(), Time.time);
        });

        Events.on(EventType.CoreChangeEvent.class, e -> {
            if(e.core != null && e.core.team == creeperTeam) verifyUpgrade(e.core);
        });

        Events.on(EventType.WorldLoadBeginEvent.class, e -> {
            pvpUpdater.cancel();
            fixedRunner.cancel();

            hasLoaded = false;

            checkRefresh = sporeLauncherCount = messageTimer = pulseOffset = 0;
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            uuidLog.each(CreeperUtils::clearMaps);

            for(Tile t : world.tiles.array) t.creeperable = false;
            chargedEmitters.clear();
            creeperEmitters.clear();

            for(Tile tile : world.tiles){
                if(!tile.floor().isDeep()
                && tile.floor().placeableOn
                && (tile.breakable() || tile.block().isAir() || tile.block() instanceof TreeBlock)
                && !(tile.block() instanceof StaticWall || tile.block() instanceof Cliff)){
                    tile.creeperable = true;
                }
            }

            if(state.rules.pvp){
                Call.infoToast("Preparation Period Begun!\nProtection Disabling In 15 Minutes.", 10);
                pvpUpdater = Timer.schedule(() -> {
                    state.rules.polygonCoreProtection = false;
                    Call.infoToast("Preparation Period Over!\nPolygonal Core Protection Disabled.", 10);
                    Call.setRules(state.rules);
                }, preparationPeriod);
            }

            loadedSave = state.stats.buildingsBuilt > 0;

            for(Building build : creeperTeam.data().buildings){
                build.heal();

                if(!loadedSave && (build.block != Blocks.coreShard && build.block != Blocks.coreFoundation))
                    build.tile.getLinkedTiles(t -> t.creep = Math.min(creeperLevels.get(build.block, 0), maxTileCreep));

                tryAddEmitter(build);
            }

            Log.info(Structs.count(world.tiles.array, t -> t.creeperable) + " creeperable tiles");
            Log.info(creeperEmitters.size + " emitters");
            Log.info(chargedEmitters.size + " charged emitters");

            emitterDst = new int[world.width()][world.height()];

            fixedRunner = Timer.schedule(CreeperUtils::fixedUpdate, 0, 1);

            hasLoaded = true;
            resetDistanceCache(); // run after loading since it returns if not loaded
        });

        Timer.schedule(() -> {
            if(creeperEmitters.size > 0){
                sb.append(Strings.format(
                        "\uE83B [@] @/@ []emitter@ suspended",
                        getTrafficlightColor(Mathf.clamp((nullifiedCount / Math.max(1.0, creeperEmitters.size)), 0f, 1f)),
                        nullifiedCount, creeperEmitters.size, creeperEmitters.size > 1 ? "s" : ""
                ));
            }
            if(chargedEmitters.size > 0){
                if(sb.length() != 0) sb.append("\n");
                sb.append(Strings.format(
                        "\uE810 [@] @ []charged emitter@ left",
                        getTrafficlightColor(1f - Mathf.clamp(chargedEmitters.size / 10f, 0f, 1f)),
                        chargedEmitters.size, chargedEmitters.size > 1 ? "s" : ""
                ));
            }
            Call.infoPopup(sb.toString(), 2.5f, 20, 50, 20, 500, 0);
            sb.setLength(0);
        }, 0, 2.495f);

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            e.tile.getLinkedTiles(t -> {
                t.creep = t.damageTime = 0;
            }); // clear flood on all the tiles this block spans
        });

        Timer.schedule(() -> {
            uuidLog.each(s -> {
                float tmp = 18000 + mapKeeper.get(s, 0);
                if(tmp > Time.time) return;

                clearMaps(s);
            });
        }, 0 , 15);
    }

    private static void clearMaps(String str){
        leaveMap.remove(str);
        mapKeeper.remove(str, 0);
        uuidLog.remove(str);
    }

    public static void depositCreeper(Tile tile, float radius, float amount){
        Geometry.circle(tile.x, tile.y, (int)radius, (cx, cy) -> {
            Tile ct = world.tile(cx, cy);
            if(invalidTile(ct) || (tile.block() instanceof StaticWall || (tile.floor() != null && !tile.floor().placeableOn || tile.floor().isDeep() || tile.block() instanceof Cliff)))
                return;

            ct.creep = Math.min(ct.creep + amount, ct.creep > maxTileCreep ? Integer.MAX_VALUE : maxTileCreep);
        });
    }

    public static void fixedUpdate(){
        // don't update anything if game is paused
        if(!state.isPlaying() || state.isPaused()) return;

        int newcount = 0;
        for(Emitter emitter : creeperEmitters){
            emitter.fixedUpdate();
            if(emitter.nullified)
                ++newcount;
        }
        nullifiedCount = newcount;
        chargedEmitters.forEach(ChargedEmitter::fixedUpdate);

        if(!state.rules.pvp){
            // check for gameover
            if(nullifiedCount >= creeperEmitters.size && chargedEmitters.size <= 0){
                if(checkRefresh++ >= nullificationPeriod){
                    state.gameOver = true;
                    Events.fire(new EventType.GameOverEvent(state.rules.defaultTeam));
                    return;
                }
            }else checkRefresh = 0;
        }

        // notifies players about FloodCompat / Foos Client every 30 minutes
        if(++messageTimer > 1800){
            messageTimer = 0;
            Groups.player.each(p -> {
                if(!p.hasCompat)
                    p.sendMessage("[lightgray]Hi there, did you know we have a mod to [accent]improve your experience?[]" +
                    "\nThe mod provides [accent]better compatibility & reduces desyncs[] while being fully vanilla compatible!" +
                    "\nDownload [cyan]FloodCompat[] from the in-game mod browser and enjoy your adventures on io flood!" +
                    "\n[accent]Alternatively, you can get Foos Client, which has FloodCompat and other QoL features built-in!");
            });
        }
    }

    public static void updateCreeper(){
        if((updateTimer += Time.delta) < updateInterval) return; // 30 fps flood updating
        updateTimer = 0;

        // update emitters
        var eI = creeperEmitters.iterator();
        while(eI.hasNext()){
            if(!eI.next().update()) eI.remove(); // Removing from a seq while iterating is bad hence the use of an iterator
        }
        var ceI = chargedEmitters.iterator();
        boolean resetCache = false;
        while(ceI.hasNext()){
            if(!ceI.next().update()){
                ceI.remove();
                resetCache = true;
            }
        }
        if(resetCache) resetDistanceCache();

        // no emitters so game over
        if((creeperEmitters.size == 0
        || closestEmitter(world.tile(0, 0)) == null)
        && (chargedEmitters.size == 0
        || closestChargedEmitter(world.tile(0, 0)) == null)) return;


        // update creeper flow
        if(++pulseOffset >= 64) pulseOffset = 0;
        Tile[] arr = world.tiles.array;
        int l = arr.length;
        for(int i = 0; i < l; i++){ // Enhanced for allocates a lot of garbage here
            Tile tile = arr[i];
            if(!tile.creeperable) continue;

            // spread creep and apply damage
            transferCreeper(tile);
            if(tile.creep >= 1) applyDamage(tile);

            if((closestEmitterDist(tile) - pulseOffset) % 64 == 0){
                drawCreeper(tile);
            }
        }
    }

    public static int[][] emitterDst = new int[0][0];

    public static void resetDistanceCache(){
        if(!hasLoaded) return;
        for(int i = 0; i < emitterDst.length; i++){ // Don't use enhanced for as that allocates
            for (int j = 0; j < emitterDst[i].length; j++){
                var tile = world.tile(i, j);
                var dst = -1;
                Emitter ce = closestEmitter(tile);
                ChargedEmitter cce = closestChargedEmitter(tile);
                if(ce == null){
                    if(cce != null) dst = (int)cce.dst(tile);
                }else{
                    dst = (int)ce.dst(tile);
                    if(cce != null) dst = (int)Math.min(ce.dst(tile), cce.dst(tile));
                }
                emitterDst[i][j] = dst;
            }
        }
    }

    public static int closestEmitterDist(Tile tile){
        return emitterDst[tile.x][tile.y];
    }

    public static Emitter closestEmitter(Tile tile){
        return Geometry.findClosest(tile.getX(), tile.getY(), creeperEmitters);
    }

    public static ChargedEmitter closestChargedEmitter(Tile tile){
        return Geometry.findClosest(tile.getX(), tile.getY(), chargedEmitters);
    }

    public static void drawCreeper(Tile tile){
        if(tile.creep < 1f) return;

        int currentLvl = creeperLevels.get(tile.block(), 11);

        if((tile.build == null || tile.block().alwaysReplace || (tile.build.team == creeperTeam && currentLvl <= 10)) && (currentLvl < (int)tile.creep || currentLvl > (int)tile.creep + 0.1f)){
            tile.setNet(creeperBlocks.get(Mathf.clamp((int)tile.creep, 0, 10)), creeperTeam, Mathf.random(0, 3));
        }
    }

    public static void applyDamage(Tile tile){
        if(tile.build != null && tile.build.team != creeperTeam){
            if(tile.block().instakill){
                tile.build.kill(); // for the sake of making transport block spam less effective
                return; // flood doesn't evaporate when dealing with those blocks
            }

            if(Mathf.chance(0.005d)){
                Call.effect(Fx.bubble, tile.build.x, tile.build.y, 0, creeperTeam.color);
            }

            // updates the damage scaling of flood, resets damageTime if there's a 300 tick gap
            tile.getLinkedTiles(t -> t.damageTime = (Time.time - tile.lastDamageTime < 300) ? (t.damageTime + Time.delta) : 0);

            float buildupDamage = (tile.team().data().players.size > 0) ? Mathf.round(
            (creeperDamageScaling * (tile.damageTime / 60)), 0.1f) : 0;
            tile.build.damage(creeperTeam, (creeperDamage + buildupDamage) * tile.creep);
            tile.creep *= creeperEvaporationUponDamagePercent;
            tile.getLinkedTiles(t -> t.lastDamageTime = Time.time);
        }
    }

    public static boolean invalidTile(Tile tile){
        return tile == null;
    }

    public static void transferCreeper(Tile source){
        if(source.build == null || source.creep < 1f) return;

        float transferRate = source.creep > maxTileCreep ? 0.025f : baseTransferRate;

        float creepBefore = source.creep;
        for(int i = 0; i <= 3; i++){
            Tile target = source.nearby(i);
            if(cannotTransfer(source, target)) continue;

            // used for special spreading
            if(target.repelled){
                float offset = target.creep - Mathf.round(target.creep, 1);
                if(offset < 0.001f) offset = 0;
                if(source.creep >= (target.creep + (2 - offset)) || source.creep == maxTileCreep){
                    target.creep += (1 - offset);
                    source.creep -= (1 - offset);
                }
                continue;
            }

            // creeper delta, cannot transfer more than 1/4 source creep or less than 0.001f. Target creep cannot exceed max creep
            float delta = Mathf.clamp((creepBefore - target.creep) * transferRate, 0, Math.min(source.creep * transferRate, maxTileCreep - target.creep));
            if(delta > 0.001f){
                target.creep += delta;
                source.creep -= delta;
            }
        }
    }

    public static boolean cannotTransfer(Tile source, Tile target){
        if(source == null
        || target == null
        || !target.creeperable
        || source.creep <= target.creep
        || target.creep >= maxTileCreep){
            return true;
        }

        return source.build != null && source.build.team != creeperTeam;
    }

    public static void verifyUpgrade(Building build){
        boolean canUpgrade = true;
        Emitter thisEmitter = creeperEmitters.find(em -> em.getX() == build.x && em.getY() == build.y);
        ChargedEmitter thisCharged = chargedEmitters.find(em -> em.getX() == build.x && em.getY() == build.y);
        if(build.block() != Blocks.interplanetaryAccelerator){ // interplanetary accel upgrades into a smaller block, why would we check?
            int tx = build.tileX() - 1, ty = build.tileY() - 1, o = build.block.sizeOffset + 1, max = build.block.size + 2;
            for(int dx = 0; dx < max; dx++){
                for(int dy = 0; dy < max; dy++){
                    Tile other = world.tile(tx + dx + o, ty + dy + o);
                    if(!other.creeperable){
                        canUpgrade = false;
                        break;
                    }
                }
            }
        }
        if(thisEmitter != null) thisEmitter.canUpgrade = canUpgrade;
        if(thisCharged != null) thisCharged.canUpgrade = canUpgrade;
    }
}
