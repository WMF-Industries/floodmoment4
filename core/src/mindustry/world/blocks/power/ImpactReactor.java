package mindustry.world.blocks.power;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.creeper.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.creeper.CreeperUtils.*;

public class ImpactReactor extends PowerGenerator{
    public final int timerUse = timers++;
    public float warmupSpeed = 0.001f;
    public float itemDuration = 60f;

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;
        flags = EnumSet.of(BlockFlag.reactor, BlockFlag.generator);
        lightRadius = 115f;
        emitLight = true;
        envEnabled = Env.any;

        drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawPlasma(), new DrawDefault());

        explosionShake = 6f;
        explosionShakeDuration = 16f;
        explosionDamage = 1900 * 4;
        explosionMinWarmup = 0.3f;
        explodeEffect = Fx.impactReactorExplosion;
        explodeSound = Sounds.explosionbig;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("power", (GeneratorBuild entity) -> new Bar(() ->
        Core.bundle.format("bar.poweroutput",
        Strings.fixed(Math.max(entity.getPowerProduction() - consPower.usage, 0) * 60 * entity.timeScale(), 1)),
        () -> Pal.powerBar,
        () -> entity.productionEfficiency));
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    public class ImpactReactorBuild extends GeneratorBuild{
        public float warmup, totalProgress;
        public int lastFx = 0;
        public int finFx = 0;
        public int smokeFx = 0;
        public Emitter targetEmitter;
        float refresh;
        int timeSuspended;

        @Override
        public void updateTile(){
            refresh += Time.delta;
            if(refresh >= 60) {
                refresh = 0;
                if(!targetEmitter.suspended) {
                    Call.label("[yellow]⚠[red]Emitter Not Suspended[]⚠", 1, this.x, this.y);
                }
                if(targetEmitter == null){
                    Emitter core = CreeperUtils.closestEmitter(tile);
                    if (core != null && within(core, nullifierRange)){
                        targetEmitter = core;
                    }
                }else if(targetEmitter.nullified){
                    ++timeSuspended;
                }else timeSuspended = 0;
            }

            if(++lastFx > (2f - warmup) * 25){
                lastFx = 0;

                if(targetEmitter != null && targetEmitter.build != null && timeSuspended >= 5){
                    Geometry.iterateLine(0f, x, y, targetEmitter.getX(), targetEmitter.getY(), 1f - warmup, (x, y) -> {
                        Timer.schedule(() -> {
                            Call.effect(Fx.missileTrailShort, x, y, warmup * 3f, Pal.accent);
                        }, dst(x, y) / tilesize / 2);
                    });

                    Call.soundAt(Sounds.dullExplosion, x, y, 1, 1);
                    Call.effect(Fx.dynamicSpikes, x, y, warmup * 3f, team.color);
                }
            }

            if(efficiency >= 0.9999f && power.status >= 0.99f){
                boolean prevOut = getPowerProduction() <= consPower.requestedPower(this);

                warmup = Mathf.lerpDelta(warmup, 1f, warmupSpeed * timeScale);
                if(Mathf.equal(warmup, 1f, 0.001f)){
                    warmup = 1f;
                }

                if(targetEmitter != null && timeSuspended >= 5){
                    if(Mathf.equal(warmup, 1f, 0.01f)){
                        Call.effect(Fx.massiveExplosion, x, y, 2f, Pal.accentBack);

                        creeperEmitters.remove(targetEmitter);

                        Call.effect(Fx.shockwave, x, y, 16f, Pal.accent);
                        Call.soundAt(Sounds.corexplode, x, y, 1.2f, 1f);

                        Building build = targetEmitter.build;
                        Block block = build.block;
                        Tile target = build.tile;

                        build.kill();

                        if(state.rules.coreCapture) {
                            target.setNet(block, team(), 0);
                            Call.effect(Fx.placeBlock, target.getX(), target.getY(), block.size, team().color);
                        }

                        targetEmitter = null;
                        Core.app.post(this::kill);
                    }else if(++finFx > (1.1f - warmup) * 50){
                        finFx = 0;
                        if(Mathf.chance(warmup * 0.1f)) {
                            targetEmitter.build.tile.getLinkedTiles(t -> {
                                Call.effect(Fx.mineHuge, t.getX(), t.getY(), warmup, Pal.health);
                            });

                            Call.soundAt(Mathf.chance(0.7f) ? Sounds.flame2 : Sounds.flame, x, y, 0.8f, Mathf.range(0.8f, 1.5f));
                        }
                    }
                }else if(++smokeFx > (1.1f - warmup) * 50){
                    smokeFx = 0;
                    if(targetEmitter != null && Mathf.chance(warmup * 0.3f)) {
                        Call.effect(Fx.smokeCloud, x + Mathf.range(0, 32), y + Mathf.range(0, 32), 1f, Pal.gray);
                    }
                }

                if(!prevOut && (getPowerProduction() > consPower.requestedPower(this))){
                    Events.fire(Trigger.impactPower);
                }

                if(timer(timerUse, itemDuration / timeScale)){
                    consume();
                }
            }else warmup = Mathf.lerpDelta(warmup, 0f, 0.01f);
            totalProgress += warmup * Time.delta;
            productionEfficiency = Mathf.pow(warmup, 5f);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public float ambientVolume(){
            return warmup;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return warmup;
            return super.sense(sensor);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
        }
    }
}
