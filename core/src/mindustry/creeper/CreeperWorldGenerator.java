package mindustry.creeper;

import arc.math.*;
import arc.struct.*;
import mindustry.maps.generators.*;
import mindustry.world.*;

import static mindustry.content.Blocks.*;

public abstract class CreeperWorldGenerator extends BasicGenerator{
    /** Blocks that get assigned to terrainSet & oreSet when generating */
    Seq<Block> serpuloTerrain = Seq.with(stone, craters, charr, sand, darksand, dirt, mud, ice, iceSnow, moss, sporeMoss, shale, grass, salt),
    erekirTerrain = Seq.with(dacite, rhyolite, rhyoliteCrater, roughRhyolite, regolith, yellowStone, redIce, redStone, denseRedStone, arkyicStone,
            redmat, bluemat, ferricStone, ferricCraters, carbonStone, beryllicStone, crystallineStone, crystalFloor, yellowStonePlates),
    serpuloOres = Seq.with(sand, oreCopper, oreLead, oreCoal, oreTitanium, oreThorium),
    erekirOres = Seq.with(oreBeryllium, wallOreBeryllium, graphiticWall, oreTungsten, wallOreTungsten, oreCrystalThorium, wallOreThorium),
    /** Blocks used for current world gen */
    terrainSet, oreSet;
    /** Lists used for choosing emitter types based on map size */
    ObjectMap<Block, Integer> emitterMap = ObjectMap.of(
            coreShard, 0,
            coreFoundation, 25000,
            coreNucleus, 62500
    );
    ObjectMap<Block, Integer> chargedMap = ObjectMap.of(
            launchPad, 0,
            interplanetaryAccelerator, 25000,
            coreBastion, 32500,
            coreCitadel, 40000,
            coreAcropolis, 62500
    );
    /** Generation flag */
    boolean isErekir;
    /** Emitters used when generating rooms */
    Block emitter, charged;

    public void generateCreeperWorld(){
        isErekir = Mathf.randomBoolean();
        randTerrain();
        Tiles world = randSize();

        emitterMap.each((type, val) -> {
            if(val <= (world.width * world.height)) emitter = type;
        });
        chargedMap.each((type, val) -> {
            if(val <= (world.width * world.height)) charged = type;
        });

        generate(world);
    }

    @Override
    protected void generate(){
        //TODO
    }

    /** Generates a width & height for the new world */
    Tiles randSize(){
        int var = Mathf.round(Math.max(Mathf.random(600), 150), 25);
        return new Tiles(var, var);
    }

    /** Creates and returns a random terrain set, assigns ores to oreSet */
    void randTerrain(){
        Seq<Block> terrain = new Seq<>(), temp = isErekir ? erekirTerrain : serpuloTerrain;

        while(terrain.size < 5){
            Block get = temp.random();
            if(!terrain.contains(get)){
                terrain.add(get);
            }
        }

        terrainSet.addAll(terrain);
        oreSet.addAll(isErekir ? erekirOres : serpuloOres);
    }
}
