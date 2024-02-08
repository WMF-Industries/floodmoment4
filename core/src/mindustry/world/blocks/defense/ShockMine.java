package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ShockMine extends Block{
    public final int timerDamage = timers++;

    public float cooldown = 80f;
    public float tileDamage = 5f;
    public float damage = 13;
    public int length = 10;
    public int tendrils = 6;
    public Color lightningColor = Pal.lancerLaser;
    public int shots = 6;
    public float inaccuracy = 0f;
    public @Nullable BulletType bullet;
    public float teamAlpha = 0.3f;
    public @Load("@-team-top") TextureRegion teamRegion;

    public ShockMine(String name){
        super(name);
        update = false;
        destructible = true;
        instakill = true;
        solid = false;
        targetable = false;
    }

    public class ShockMineBuild extends Building{
        @Override
        public void onDestroyed(){
            super.onDestroyed();
            var color = team.color;
            Seq<Tile> tiles = new Seq<>();
            Geometry.circle(tileX(), tileY(), 2, (cx, cy) ->{
                Tile t = world.tile(cx, cy);
                if(t != null && t.creeperable){
                    t.creeperable = false;
                    tiles.add(t);
                }
            });

            var start_time = Time.millis();

            var fxRunner = Timer.schedule(() ->{
                tiles.each(t -> {
                    Timer.schedule(() -> {
                        var size_multiplier = 1 - (Time.millis() - start_time) / 1000f / 3f; // Moves from 1 to 0 with time
                        Call.effect(Fx.lightBlock, t.getX(), t.getY(), Mathf.random(0.01f, 1.5f * size_multiplier), color);
                    }, Mathf.random(0.5f));
                });
            }, 0, 0.5f);

            Timer.schedule(() ->{
                fxRunner.cancel();
                tiles.forEach((t) -> t.creeperable = true);
                tiles.clear();
            }, 3f);
        }

        @Override
        public void drawTeam(){
            //no
        }

        @Override
        public void draw(){
            super.draw();
            Draw.color(team.color, teamAlpha);
            Draw.rect(teamRegion, x, y);
            Draw.color();
        }

        @Override
        public void drawCracks(){
            //no
        }

        @Override
        public void unitOn(Unit unit){
            if(enabled && unit.team != team && timer(timerDamage, cooldown)){
                triggered();
                damage(tileDamage);
            }
        }

        public void triggered(){
            for(int i = 0; i < tendrils; i++){
                Lightning.create(team, lightningColor, damage, x, y, Mathf.random(360f), length);
            }
            if(bullet != null){
                for(int i = 0; i < shots; i++){
                    bullet.create(this, x, y, (360f / shots) * i + Mathf.random(inaccuracy));
                }
            }
        }
    }
}
