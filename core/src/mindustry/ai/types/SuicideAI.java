package mindustry.ai.types;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.Fx;
import mindustry.creeper.CreeperUtils;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SuicideAI extends GroundAI{
    static boolean blockedByBlock;

    @Override
    public void updateUnit(){
        if(Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            target = target(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        Building core = unit.closestEnemyCore();

        boolean rotate = false, shoot = false, moveToTarget = false, destroy = false;

        if(target == null){
            target = core;
        }

        if(!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()){
            rotate = true;
            shoot = unit.within(target, unit.type.weapons.first().bullet.range +
                (target instanceof Building b ? b.block.size * Vars.tilesize / 2f : ((Hitboxc)target).hitSize() / 2f));

            destroy = unit.within(target, 30f +
                    (target instanceof Building b ? b.block.size * Vars.tilesize / 2f : 2000f));

            //do not move toward walls or transport blocks
            if(!(target instanceof Building build && !(build.block instanceof CoreBlock) && (
                build.block.group == BlockGroup.walls ||
                build.block.group == BlockGroup.liquids ||
                build.block.group == BlockGroup.transportation
            ))){
                blockedByBlock = false;

                //raycast for target
                boolean blocked = World.raycast(unit.tileX(), unit.tileY(), target.tileX(), target.tileY(), (x, y) -> {
                    for(Point2 p : Geometry.d4c){
                        Tile tile = Vars.world.tile(x + p.x, y + p.y);
                        if(tile != null && tile.build == target) return false;
                        if(tile != null && tile.build != null && tile.build.team != unit.team()){
                            blockedByBlock = true;
                            return true;
                        }else{
                            return tile == null || tile.solid();
                        }
                    }
                    return false;
                });

                //shoot when there's an enemy block in the way
                if(blockedByBlock){
                    shoot = true;
                }

                if(!blocked){
                    moveToTarget = true;
                    //move towards target directly
                    unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
                }
            }
        }

        if(!moveToTarget){
            boolean move = true;

            //stop moving toward the drop zone if applicable
            if(core == null && state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)){
                    move = false;
                }
            }

            if(move){
                if(unit.isFlying() && target != null){
                    moveTo(target, unit.type.range * 0.2f);
                    unit.lookAt(target);
                }else pathfind(Pathfinder.fieldCore);
            }
        }

        if(unit.team == CreeperUtils.creeperTeam && unit.type.creeperDeposit > 0){
            if(destroy) {
                unit.deposit();
            }else{
                if(Mathf.randomBoolean(0.01f))
                    Call.effect(Fx.bubble, unit.x, unit.y, 1, Color.blue);
            }
        }else {
            unit.controlWeapons(rotate, shoot);
        }

        faceTarget();
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground &&
            !(t.block instanceof Conveyor || t.block instanceof Conduit)); //do not target conveyors/conduits
    }
}
