package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Geometry;
import arc.struct.*;
import arc.util.Timer;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.Fx;
import mindustry.creeper.CreeperUtils;
import mindustry.entities.UnitSorts;
import mindustry.entities.Units;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Radar extends Block{
    public float discoveryTime = 60f * 10f;
    public float rotateSpeed = 2f;

    public @Load("@-base") TextureRegion baseRegion;
    public @Load("@-glow") TextureRegion glowRegion;

    public Color glowColor = Pal.turretHeat;
    public float glowScl = 5f, glowMag = 0.6f;

    public Radar(String name){
        super(name);

        update = solid = true;
        flags = EnumSet.of(BlockFlag.hasFogRadius);
        outlineIcon = true;
        fogRadius = 10;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, fogRadius * tilesize, Pal.accent);
    }

    public class RadarBuild extends Building{
        public float progress;
        public float lastRadius = 0f;
        public float smoothEfficiency = 1f;
        public float totalProgress;

        public float shot_warmup = 0f;
        public float shot_warmup_fx_interval = 35f;
        public boolean on_cd = false;

        public float warmup_iv;

        @Override
        public float fogRadius(){
            return fogRadius * progress * smoothEfficiency;
        }

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.05f);

            if(Math.abs(fogRadius() - lastRadius) >= 0.5f){
                Vars.fogControl.forceUpdate(team, this);
                lastRadius = fogRadius();
            }

            progress += edelta() / discoveryTime;
            progress = Mathf.clamp(progress);

            totalProgress += efficiency * edelta();

            // custom creeper behaviour
            var target = Units.bestEnemy(team, x, y, fogRadius*tilesize, e -> !e.dead(), UnitSorts.strongest);

            if (team() == CreeperUtils.creeperTeam && !on_cd && target != null) {
                shot_warmup = Mathf.lerp(shot_warmup, 1.0f, 0.01f);

                if(++warmup_iv > shot_warmup_fx_interval * (1f - shot_warmup)) {
                    warmup_iv = 0f;

                    Geometry.iterateLine(0f, tile.worldx(), tile.worldy(), target.x(), target.y(), tile.dst(target) * (1f - shot_warmup), (x, y) -> {
                        Call.effect(Fx.shootHealYellow, x, y, angleTo(target), Color.blue);
                    });

                    Call.effect(Fx.placeBlock, target.x(), target.y(), 1, Color.blue);
                    Call.soundAt(Sounds.lasershoot, target.x(), target.y(), 1f, 1);
                }

                if (shot_warmup > 0.9f) {
                    Call.soundAt(Sounds.lasercharge2, target.x(), target.y(), 1.5f, 0.8f);

                    Timer.schedule(() -> {
                        Geometry.iterateLine(0f, tile.worldx(), tile.worldy(), target.x(), target.y(), tile.dst(target) * 0.1f, (x, y) -> {
                            Call.effect(Fx.shootPayloadDriver, x, y, tile.angleTo(target), Color.blue);
                        });

                        Call.soundAt(Sounds.laserblast, target.x(), target.y(), 2f, 1.2f);
                        target.damage(CreeperUtils.radarBeamDamage);

                        on_cd = false;
                    }, 1);

                    shot_warmup = 0f;
                    on_cd = true;
                }

            } else {
                shot_warmup = Mathf.lerp(shot_warmup, 0f, 0.01f);
            }
        }

        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, fogRadius() * tilesize, Pal.accent);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.rect(region, x, y, rotateSpeed * totalProgress);

            Drawf.additive(glowRegion, glowColor, glowColor.a * (1f - glowMag + Mathf.absin(glowScl, glowMag)), x, y, rotateSpeed * totalProgress, Layer.blockAdditive);
        }

        @Override
        public float progress(){
            return progress;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            progress = read.f();
        }
    }
}
