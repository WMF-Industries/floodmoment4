package mindustry.creeper;

import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import java.util.*;

import static mindustry.creeper.CreeperUtils.*;

public class ChargedEmitter implements Position{
    public ChargedEmitterType type;
    public Building build;

    public int counter;
    public int throttle;
    public float buildup;
    public float overflow;
    public boolean emitting;
    boolean immune;
    public StringBuilder sb = new StringBuilder();

    public static HashMap<Block, ChargedEmitterType> chargedEmitterTypes = new HashMap<>();

    public boolean update(){
        if(build == null || build.health <= 1f)return false;

        if(build.health < build.maxHealth && overflow > 0 || emitting || this.build.tile.creep >= 6.5f){
            if(!emitting && this.build.tile.creep < 6.5f){
                overflow--;
                immune = false;
            }else immune = true;
            build.heal(build.maxHealth);
            if(++throttle >= 6 && build.health < build.maxHealth){
                Call.effect(Fx.healBlock, build.x, build.y, build.block.size, creeperTeam.color);
                throttle = 0;
            }
        }else immune = false;

        if(emitting){
            if(++counter >= type.interval){
                counter = 0;
                build.tile.getLinkedTiles(t -> t.creep += type.amt);
            }

            if(--buildup <= 0){
                emitting = false;
                overflow = Math.min(type.chargeCap, overflow + (build.tile.creep / 100));
                build.tile.getLinkedTiles(t -> t.creep = Math.min(t.creep, maxTileCreep));
            }
        }else if((buildup += type.chargePulse) > type.chargeCap){
            emitting = true;
        }
        return true;
    }

    public void fixedUpdate(){
        sb.setLength(0);
        if(immune){
            sb.append(Strings.format("[accent]\uE86B [stat]Immune[] \uE86B[]"));
        }
        if(overflow > 0){
            if(sb.length() != 0) sb.append("\n    ");
            sb.append(Strings.format("[green]@[] - [stat]@%[]", type.upgradable() ? "\ue804" : "\ue813", (int)(overflow * 100 / type.chargeCap)));
        }
        if(emitting){
            Call.effect(Fx.launch, build.x, build.y, build.block.size, creeperTeam.color);
        }else{
            if(sb.length() != 0) sb.append(immune ? "\n    " : "\n");
            sb.append(Strings.format("[red]⚠[] - [stat] @%", (int)(buildup * 100 / type.chargeCap)));
        }
        if(sb.length() != 0){
            Call.label(sb.toString(), 1f, build.x, build.y);
        }
        if(type.upgradable() && type.chargeCap > 0 && build != null && build.tile != null && overflow >= type.chargeCap){
            ChargedEmitterType next = type.getNext();
            if(next != null){
                build.tile.setNet(next.block, creeperTeam, 0);
                chargedEmitters.remove(this);
            }
        }
    }

    public ChargedEmitter(Building build){
        this.build = build;
        this.type = chargedEmitterTypes.get(build.block);
    }

    public static void init(){
        // backwards compatibility
        chargedEmitterTypes.put(Blocks.launchPad, ChargedEmitterType.launchPad);
        chargedEmitterTypes.put(Blocks.interplanetaryAccelerator, ChargedEmitterType.interplanetaryAccelerator);

        chargedEmitterTypes.put(Blocks.coreBastion, ChargedEmitterType.bastion);
        chargedEmitterTypes.put(Blocks.coreCitadel, ChargedEmitterType.citadel);
        chargedEmitterTypes.put(Blocks.coreAcropolis, ChargedEmitterType.acropolis);
    }

    @Override
    public float getX(){
        return build.x;
    }

    @Override
    public float getY(){
        return build.y;
    }

    enum ChargedEmitterType{
        launchPad(5, 5, 1, 0.2f, 600, Blocks.launchPad),
        interplanetaryAccelerator(4, 6, 2, 0.22f, 1200, Blocks.interplanetaryAccelerator),

        bastion(3, 7, 3, 0.25f, 1800, Blocks.coreBastion),
        citadel(2, 8, 4, 0.3f, 2400, Blocks.coreCitadel),
        acropolis(1, 9, 5, 0.35f, 5000, Blocks.coreAcropolis);

        public final int amt;
        public final int level;
        public final int interval;
        public final int chargeCap;
        public final float chargePulse;
        public final Block block;

        ChargedEmitterType(int interval, int amt, int level, float chargePulse, int chargeCap, Block block){
            this.amt = amt;
            this.block = block;
            this.level = level;
            this.interval = interval;
            this.chargeCap = chargeCap;
            this.chargePulse = chargePulse;
        }

        public boolean upgradable(){
            return level < values().length;
        }

        public ChargedEmitterType getNext(){
            for(ChargedEmitterType t : values()){
                if(t.level == (level + 1)){
                    return t;
                }
            }
            return null;
        }
    }
}
