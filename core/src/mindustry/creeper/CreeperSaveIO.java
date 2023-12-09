package mindustry.creeper;

import mindustry.io.SaveFileReader.*;

import java.io.*;

import static mindustry.Vars.*;

public class CreeperSaveIO implements CustomChunk {
    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeByte(0); // version

        for (int i = 0; i < world.width() * world.height(); i++) stream.writeFloat(world.tiles.geti(i).creep);
    }

    @Override
    public void read(DataInput stream) throws IOException {
        var version = stream.readByte();

        for (int i = 0; i < world.width() * world.height(); i++) world.tiles.geti(i).creep = stream.readFloat();
    }

    @Override
    public boolean writeNet() {
        return false;
    }
}
