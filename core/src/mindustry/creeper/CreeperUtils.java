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
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class CreeperUtils{
    public static StringBuilder sb = new StringBuilder();
    public static final float updateInterval = 2/60f; // Base update interval in seconds
    public static final float baseTransferRate = 0.25f; // Base transfer rate NOTE: keep below 0.25f
    public static final float creeperDamage = 0.2f; // Base creeper damage
    public static final float creeperEvaporationUponDamagePercent = 0.98f; // Creeper percentage that will remain upon damaging something
    public static final float creeperUnitDamage = 2f;
    public static final float maxTileCreep = 10.5f;
    public static final float creeperBlockDamageMultiplier = 1f;

    public static final boolean useAiController = false; // Whether to use unit AI controller instead of RTS ai


    /*
    public static BulletType sporeType = new ArtilleryBulletType(3f, 20, "shell") {{
        hitEffect = Fx.flakExplosion;
        knockback = 0.8f;
        lifetime = 80f;
        width = height = 11f;
        collidesTiles = false;
        splashDamageRadius = 25f * 0.75f;
        splashDamage = 33f;
    }};
     */

    public static BulletType sporeType = UnitTypes.arkyid.weapons.get(6).bullet;

    public static float sporeMaxRangeMultiplier = 27.5f;
    public static float sporeAmount = 20f;
    public static float sporeRadius = 5f;
    public static float sporeSpeedMultiplier = 0.15f;
    public static float sporeHealthMultiplier = 10f;
    public static float sporeTargetOffset = 256f;
    public static boolean sporeScaleThreat = true;
    public static double sporeBaseMultifireChance = 0.01d;
    public static float sporeCreepUse = 1.25f;

    public static float unitShieldDamageMultiplier = 1.5f;
    public static float buildShieldDamageMultiplier = 0.75f;
    public static float shieldBoostProtectionMultiplier = 0.5f;
    public static float shieldCreeperDropAmount = 7f;
    public static float shieldCreeperDropRadius = 4f;

    public static float nullifierRange = 16 * tilesize;
    public static float erekirNullifyTime = 45;

    public static float radarBeamDamage = 600f; // damage the radar creeper beam deals to units

    public static float creepTowerDeposit = 3f; // amount of creep deposited by the creep tower after the building is killed
    public static float creepTowerRange = 300f; // just slightly bigger than ripple's range


    public static float suspendDamage = 1500f; // Damage that needs to be applied for the core to be suspended
    public static float suspendTimeout = 180f; // The amount of ticks a core remains suspended (resets upon enough damage applied)

    public static float nullificationPeriod = 10f; // How many seconds all cores have to be nullified (suspended) in order for the game to end
    public static float preparationPeriod = 900f; // How many seconds of preparation time pvp should have (core zones active)
    public static int tutorialID, pvpTutorialID;
    public static boolean canGameover, stateUpdate;
    private static final int maxProtectionRadius = 10 * tilesize;
    private static int timePassed, pulseOffset;
    private static int nullifiedCount = pulseOffset = timePassed = 0;

    public static final Team creeperTeam = Team.blue;

    public static final IntMap<Block> creeperBlocks = new IntMap<>();
    public static final ObjectIntMap<Block> creeperLevels = new ObjectIntMap<>();

    public static Seq<Emitter> creeperEmitters = new Seq<>();
    public static Seq<ChargedEmitter> chargedEmitters = new Seq<>();
    public static Seq<ForceProjector.ForceBuild> shields = new Seq<>();

    public static Timer.Task fixedRunner;

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
    "[white]\uF872[]", "[scarlet]Spore Launchers[]\n[accent]Thorium Reactors[] fire long distance artillery that releases [accent]a huge amount of flood[] on impact.\nYou can defend against this with \uF80E [accent]Segments[white] & \uF898 []Force Projectors[].",
    "[white]\uF682[]", "[scarlet]Flood Projector[]\n[accent]Shockwave Towers[] rapidly deposit flood at any nearby buildings, forcing a [accent]different approach[] than turret spam.\nRange is slightly larger than Ripples.",
    "[white]\uF6AD[]", "[scarlet]Flood Radar[]\n[accent]Radars[] focus on the closest unit, and after a short time of charging, [accent]shoot[] at that unit, forcing a [accent]different approach[] than unit spam.\nRange is slightly larger than Ripples.",
    "[white]\uF7FA[]", "[scarlet]Flood Creep[]\n[accent]Crawler tree units[] explode when in contact with buildings and release tons of [#e056f0]the flood[].",
    "[white]\uF898[]", "[lime]Flood Shields[]\n[accent]Force Projectors[] and [accent]unit shields[] absorb [#e056f0]the flood[].\nUnlike unit shields, \uF898 need [accent]coolant and power to regenerate[] & [accent]explode[] if overloaded / destroyed.\n[red]Reclaiming them gives no resources![]",
    "[white]\uF7F5[]", "[lime]Flood Horizons[]\n[accent]Horizons[] are disarmed and immune to the flood, additionally their carrying capacity is set to 20.\nUse them to transport items over flood.",
    };
    public static final String[] pvpTutEntries = {
    "[accent]\uE875[] Tutorial 1/3", "In [#e056f0]\uE83B flood pvp[], your goal is to defeat your enemy as well as defend yourself from [#e056f0]the flood[].\nSuspending emitters does not end the game.",
    "[accent]\uE875[] Tutorial 2/3", "After the game starts, [accent]Polygonal Core Protection[] is enabled for [accent]15 minutes[]\nUse the given protection period to prepare your defenses!",
    "[accent]\uE875[] Tutorial 3/3", "Content wise, flood pvp is nearly identical to standard flood.\nThe strategies used on a standard flood server might prove to also be effective here.",
    "[white]\uF7F7[]", "[lime]PvP Creep[]\n\uF7F7 & \uF7DE explode when in contact with enemy team's units & buildings and release tons of [#e056f0]the flood[].\nThis ability is disabled once all emitters are nullified.",
    };

    private static float updateTimer;

    /** Whether the current map has loaded. We cant use world.isGenerating(); as that's set to false too soon */
    private static boolean hasLoaded;

    public static String getTrafficlightColor(double value){
        return "#" + Integer.toHexString(java.awt.Color.HSBtoRGB((float)value / 3f, 1f, 1f)).substring(2);
    }

    public static float[] targetSpore(){
        float[] ret = {0, 0};
        int iterations = 0;
        Player player;

        while(iterations++ < 10_000 && (player = Groups.player.random()) != null){
            if(player.unit() == null || player.x == 0 && player.y == 0) continue;

            Unit unit = player.unit();
            ret[0] = unit.x + Mathf.range(sporeTargetOffset);
            ret[1] = unit.y + Mathf.range(sporeTargetOffset);
            Tile retTile = world.tileWorld(ret[0], ret[1]);

            if(retTile != null && retTile.creeperable) break;
        }

        return world.tileWorld(ret[0], ret[1]) == null ? new float[]{0, 0} : ret;
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
        }else if(ChargedEmitter.chargedEmitterTypes.containsKey(build.block)){
            chargedEmitters.add(new ChargedEmitter(build));
            CreeperUtils.resetDistanceCache();
        }
    }

    public static void init(){
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
        creeperBlocks.put(12, Blocks.thoriumReactor);

        creeperBlocks.put(20, Blocks.coreShard);
        creeperBlocks.put(25, Blocks.coreFoundation);
        creeperBlocks.put(30, Blocks.coreNucleus);

        creeperBlocks.put(31, Blocks.launchPad);
        creeperBlocks.put(32, Blocks.interplanetaryAccelerator);

        creeperBlocks.put(75, Blocks.coreBastion);
        creeperBlocks.put(76, Blocks.coreCitadel);
        creeperBlocks.put(77, Blocks.coreAcropolis);


        for(var set : creeperBlocks.entries()){
           creeperLevels.put(set.value, set.key);
        }

        Emitter.init();
        ChargedEmitter.init();

        int menuID = 0;
        for(int i = tutEntries.length; --i >= 0; ){
            final int j = i;
            int current = menuID;
            menuID = Menus.registerMenu((player, selection) -> {
                if(selection == 1) return;
                if(j == tutEntries.length / 2) return;
                Call.menu(player.con, current, tutEntries[2 * j], tutEntries[2 * j + 1], j == tutEntries.length / 2 - 1 ? tutFinal : tutContinue);
            });
        }

        int pvpMenuID = 0;
        for(int pi = pvpTutEntries.length; --pi >= 0; ){
            final int pj = pi;
            int current = pvpMenuID;
            pvpMenuID = Menus.registerMenu((player, selection) -> {
                if(selection == 1) return;
                if(pj == tutEntries.length / 2) return;
                Call.menu(player.con, current, pvpTutEntries[2 * pj], pvpTutEntries[2 * pj + 1], pj == pvpTutEntries.length / 2 - 1 ? tutFinal : tutContinue);
            });
        }

        tutorialID = menuID;
        pvpTutorialID = pvpMenuID;
        Events.on(EventType.PlayerJoin.class, e -> {
            if(e.player.getInfo().timesJoined > 1) return;
            Call.menu(e.player.con, state.rules.pvp ? pvpTutorialID : tutorialID, "[accent]Welcome![]", "Looks like it's your first time playing..", tutStart);
        });

        Events.on(EventType.WorldLoadBeginEvent.class, e -> {
            shields.clear();
            stateUpdate = canGameover = true;
            hasLoaded = false;
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            for(Tile t : world.tiles.array) t.creeperable = false;
            chargedEmitters.clear();
            creeperEmitters.clear();

            for(Tile tile : world.tiles){
                if(!tile.floor().isDeep()
                && tile.floor().placeableOn
                && (tile.breakable() || tile.block() == Blocks.air || tile.block() instanceof TreeBlock)
                && !(tile.block() instanceof StaticWall || tile.block() instanceof Cliff)){
                    tile.creeperable = true;
                }
            }

            for(Building build : Groups.build){
                tryAddEmitter(build);
            }

            if(state.rules.pvp) state.rules.polygonCoreProtection = true;

            Log.info(Structs.count(world.tiles.array, t -> t.creeperable) + " creeperable tiles");
            Log.info(creeperEmitters.size + " emitters");
            Log.info(chargedEmitters.size + " charged emitters");

            emitterDst = new int[world.width()][world.height()];

            if(fixedRunner != null) fixedRunner.cancel();
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
            e.tile.getLinkedTiles(t -> t.creep = 0); // clear flood on all the tiles this block spans
        });

        Timer.schedule(() -> {
            if (!state.isGame() || state.rules.pvp) return;
            // check for gameover
            if(nullifiedCount == creeperEmitters.size){
                Timer.schedule(() -> {
                    if(nullifiedCount == creeperEmitters.size && chargedEmitters.size <= 0 && canGameover){
                        // gameover
                        state.gameOver = true;
                        Events.fire(new EventType.GameOverEvent(state.rules.defaultTeam));
                    }
                    // failed to win, core got unsuspended
                }, nullificationPeriod);
            }
        }, 0, 10);
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

        if(stateUpdate && (!state.rules.pvp || ++timePassed >= preparationPeriod)){
            if(state.rules.enemyCoreBuildRadius > maxProtectionRadius)  // flood should allow to nullify emitters
                state.rules.enemyCoreBuildRadius = maxProtectionRadius;
            if(state.rules.pvp){
                Call.infoToast("Preparation Period Over!\nPolygonal Core Protection Disabled.", 10);
            }else{ // do not set for pvp, since it waits 15 minutes
                // set flood banned blocks TODO: small walls are too spammable, should we ban them?
                state.rules.bannedBlocks.addAll(Blocks.lancer, Blocks.arc/*, Blocks.scrapWall, Blocks.copperWall,
                Blocks.titaniumWall, Blocks.thoriumWall, Blocks.plastaniumWall, Blocks.phaseWall, Blocks.surgeWall,
                Blocks.berylliumWall, Blocks.tungstenWall, Blocks.reinforcedSurgeWall, Blocks.carbideWall*/);
                // set flood revealed blocks TODO: include impact / lustre?
                state.rules.revealedBlocks.addAll(Blocks.coreShard, Blocks.scrapWallLarge, Blocks.scrapWallHuge, Blocks.scrapWallGigantic);
                state.rules.hideBannedBlocks = true;
            }

            stateUpdate = state.rules.polygonCoreProtection = false;
        }

        int newcount = 0;
        for(Emitter emitter : creeperEmitters){
            emitter.fixedUpdate();
            if(emitter.nullified)
                newcount++;
        }
        chargedEmitters.forEach(ChargedEmitter::fixedUpdate);

        for(ForceProjector.ForceBuild shield : shields){
            if(shield == null || shield.dead || shield.healthLeft <= 0f){
                shields.remove(shield);
                if(shield == null) continue;
                Core.app.post(shield::kill);

                float percentage = 1f - shield.healthLeft / ((ForceProjector)shield.block).shieldHealth;
                depositCreeper(shield.tile, shieldCreeperDropRadius, shieldCreeperDropAmount * percentage);

                continue;
            }

            double percentage = shield.healthLeft / ((ForceProjector)shield.block).shieldHealth;
            Call.label("[" + getTrafficlightColor(percentage) + "]" + (int)(percentage * 100) + "%" + (shield.phaseHeat > 0.1f ? " [#f4ba6e]\uE86B +" + ((int)((1f - CreeperUtils.shieldBoostProtectionMultiplier) * 100f)) + "%" : ""), 1f, shield.x, shield.y);
        }
        nullifiedCount = newcount;
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

            tile.build.damage(creeperTeam, creeperDamage * tile.creep);
            tile.creep *= creeperEvaporationUponDamagePercent;
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
}
