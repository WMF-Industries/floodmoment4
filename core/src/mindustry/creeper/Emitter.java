package mindustry.creeper;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.*;

import static mindustry.creeper.CreeperUtils.*;

public class Emitter implements Position{
    public Building build;
    public EmitterType type;
    public boolean suspended, nullified, canUpgrade;
    protected float counter;

    public static HashMap<Block, EmitterType> emitterTypes = new HashMap<>();

    public Emitter(Building build){
        if (build == null){
            creeperEmitters.remove(this);
            return;
        }
        this.build = build;
        this.type = emitterTypes.get(build.block);
    }

    // updates every interval in CreeperUtils
    public boolean update(){
        if(build == null || !build.isAdded() || build.health <= 1f || !(build instanceof CoreBlock.CoreBuild))return false;

        // i am a fucking moron, reverted back to old
        if(!suspended && (counter += Time.delta) >= type.interval){
            counter = 0;
            if(build.tile.creep >= 10.35f && type.level != 3 && canUpgrade){
                build.tile.creep = Math.min(build.tile.creep + type.amt, (type.upgradeThreshold + maxTileCreep));
            }else build.tile.getLinkedTiles(t -> t.creep = Math.min(t.creep + type.amt, maxTileCreep));
        }

        return true;
    }

    // updates every 1 second
    public void fixedUpdate(){
        if(build == null)return;

        // emitters shouldn't emit when covered in anticreep
        build.tile.getLinkedTiles(tmp -> {
            if(!tmp.creeperable) build.nullifyTimeout = suspendTimeout;
        });

        suspended = build.nullifyTimeout > 0f; // this doesn't have to be updated every tick
        nullified = suspended && build.tile.creep <= maxTileCreep;
        if(nullified){
            Call.label("[red]*[] SUSPENDED [red]*[]", 1f, build.x, build.y);
            Call.effect(Fx.placeBlock, build.x, build.y, build.block.size, Color.yellow);
        }else if(build.tile != null && type.level <= 2 && build.tile.creep > maxTileCreep){
            Call.label(Strings.format("[green]*[white] UPGRADING []@% *[]", (int) ((build.tile.creep - maxTileCreep) * 100 / (type.upgradeThreshold - maxTileCreep))), 1f, build.x, build.y);
            if(build.tile.creep >= type.upgradeThreshold){
                // get next emitter level & upgrade
                EmitterType next = type.getNext();
                creeperEmitters.remove(this);
                build.tile.setNet(next.block, creeperTeam, 0);
                this.build = build.tile.build;
                this.type = next;
                // make tiles creeperable and remove the creep after upgrading
                build.tile.getLinkedTiles(t -> {
                    t.creeperable = true;
                    t.creep = 0;
                });
            }
        }
    }

    @Override
    public float getX(){
        return build.x;
    }

    @Override
    public float getY(){
        return build.y;
    }

    public static void init(){
        emitterTypes.put(Blocks.coreShard, EmitterType.shard);
        emitterTypes.put(Blocks.coreFoundation, EmitterType.foundation);
        emitterTypes.put(Blocks.coreNucleus, EmitterType.nucleus);
    }

    public enum EmitterType{
        shard(3, 30, 1, 750, Blocks.coreShard),
        foundation(5, 20, 2, 4500, Blocks.coreFoundation),
        nucleus(7, 15, 3, 0, Blocks.coreNucleus);

        public final int amt;
        public final int level;
        public final int interval;
        public final int upgradeThreshold;
        public final Block block;

        EmitterType(int amt, int interval, int level, int upgradeThreshold, Block block){
            this.amt = amt;
            this.level = level;
            this.block = block;
            this.interval = interval;
            this.upgradeThreshold = upgradeThreshold;
        }

        public EmitterType getNext(){
            for(EmitterType t : values()){
                if(t.level == (level + 1)) return t;
            }
            return null;
        }
    }
}
