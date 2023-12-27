package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.creeper.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.creeper.CreeperUtils.*;

/** A turret that fires a continuous beam bullet with no reload or coolant necessary. The bullet only disappears when the turret stops shooting. */
public class ContinuousTurret extends Turret{
    public BulletType shootType = Bullets.placeholder;
    /** Speed at which the turret can change its bullet "aim" distance. This is only used for point laser bullets. */
    public float aimChangeSpeed = Float.POSITIVE_INFINITY;

    public ContinuousTurret(String name){
        super(name);

        coolantMultiplier = 1f;
        envEnabled |= Env.space;
        displayAmmoMultiplier = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
        stats.remove(Stat.reload);
        stats.remove(Stat.inaccuracy);
    }

    //TODO LaserTurret shared code
    public class ContinuousTurretBuild extends TurretBuild{
        public Seq<BulletEntry> bullets = new Seq<>();
        public float lastLength = size * 4f;
        public Emitter targetEmitter;
        int nullifyTime, timeSuspended;
        float refresh;

        @Override
        protected void updateCooling(){
            //TODO how does coolant work here, if at all?
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //TODO update ammo in unit so it corresponds to liquids
            return canConsume();
        }

        @Override
        public boolean shouldConsume(){
            return isShooting();
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            //TODO unclean way of calculating ammo fraction to display
            float ammoFract = efficiency;
            if(findConsumer(f -> f instanceof ConsumeLiquidBase) instanceof ConsumeLiquid cons){
                ammoFract = Math.min(ammoFract, liquids.get(cons.liquid) / liquidCapacity);
            }

            unit.ammo(unit.type().ammoCapacity * ammoFract);

            bullets.removeAll(b -> !b.bullet.isAdded() || b.bullet.type == null || b.bullet.owner != this);

            if(bullets.any()){
                for(var entry : bullets){
                    updateBullet(entry);
                }

                wasShooting = true;
                heat = 1f;
                curRecoil = recoil;
            }

            if(this.team != creeperTeam && block == Blocks.lustre){
                if((refresh += Time.delta) >= 60){
                    refresh = 0;
                    if(targetEmitter == null){
                        Emitter core = CreeperUtils.closestEmitter(tile);
                        if(core != null && within(core, this.range())){
                            targetEmitter = core;
                        }
                    }else{
                        if(!targetEmitter.nullified){
                            Call.label("[yellow]⚠[red]Emitter Not Suspended[]⚠", 1, x, y);
                            timeSuspended = 0;
                        }else{
                            if(++timeSuspended < 5)Call.label("\uE87C[red]Confirming Suspension[]\uE87C", 1, x, y);
                        }

                        if(targetPos.epsilonEquals(targetEmitter.getX(), targetEmitter.getY(), 1f) && Angles.within(rotation, angleTo(targetEmitter), 2.5f) && timeSuspended >= 5 && isShooting() && hasAmmo()){
                            ++nullifyTime;
                            Call.label(Strings.format("[accent]\uE810[@]@%", getTrafficlightColor((double) Mathf.round(nullifyTime / (erekirNullifyTime / 100), 1) / 100), Mathf.round(nullifyTime / (erekirNullifyTime / 100), 1)), 1, x, y);
                            Call.effect(Fx.healBlockFull, targetEmitter.getX(), targetEmitter.getY(), targetEmitter.build.block.size, creeperTeam.color);

                            if(nullifyTime >= erekirNullifyTime){
                                Call.effect(Fx.massiveExplosion, x, y, 2f, Pal.accentBack);

                                creeperEmitters.remove(targetEmitter);

                                Call.effect(Fx.shockwave, x, y, 16f, Pal.accent);
                                Call.soundAt(Sounds.corexplode, x, y, 1.2f, 1f);

                                Building build = targetEmitter.build;
                                Block block = build.block;
                                Tile target = build.tile;

                                build.tile.getLinkedTiles(t -> {
                                    t.creep = 0;
                                });
                                build.kill();

                                if(state.rules.coreCapture){
                                    target.setNet(block, team(), 0);
                                    Call.effect(Fx.placeBlock, target.getX(), target.getY(), block.size, team().color);
                                }
                            }
                        }else nullifyTime = 0;
                    }
                }
            }
        }

        protected void updateBullet(BulletEntry entry){
            float
                bulletX = x + Angles.trnsx(rotation - 90, shootX + entry.x, shootY + entry.y),
                bulletY = y + Angles.trnsy(rotation - 90, shootX + entry.x, shootY + entry.y),
                angle = rotation + entry.rotation;

            entry.bullet.rotation(angle);
            entry.bullet.set(bulletX, bulletY);

            //target length of laser
            float shootLength = Math.min(dst(targetPos), range);
            //current length of laser
            float curLength = dst(entry.bullet.aimX, entry.bullet.aimY);
            //resulting length of the bullet (smoothed)
            float resultLength = Mathf.approachDelta(curLength, shootLength, aimChangeSpeed);
            //actual aim end point based on length
            Tmp.v1.trns(rotation, lastLength = resultLength).add(x, y);

            entry.bullet.aimX = Tmp.v1.x;
            entry.bullet.aimY = Tmp.v1.y;

            if(isShooting() && hasAmmo()){
                entry.bullet.time = entry.bullet.lifetime * entry.bullet.type.optimalLifeFract * shootWarmup;
                entry.bullet.keepAlive = true;
            }
        }

        @Override
        protected void updateReload(){
            //continuous turrets don't have a concept of reload, they are always firing when possible
        }

        @Override
        protected void updateShooting(){
            if(bullets.any()){
                return;
            }

            if(canConsume() && !charging() && shootWarmup >= minWarmup){
                shoot(peekAmmo());
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, efficiency * rotateSpeed * delta());
        }

        @Override
        public void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){
            if(bullet != null){
                bullets.add(new BulletEntry(bullet, offsetX, offsetY, angleOffset, 0f));

                //make sure the length updates to the last set value
                Tmp.v1.trns(rotation, shootY + lastLength).add(x, y);
                bullet.aimX = Tmp.v1.x;
                bullet.aimY = Tmp.v1.y;
            }
        }

        @Override
        public boolean shouldActiveSound(){
            return bullets.any();
        }

        @Override
        public float activeSoundVolume(){
            return 1f;
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(lastLength);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 3){
                lastLength = read.f();
            }
        }
    }
}
