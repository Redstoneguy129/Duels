package me.cameronwhyte.duels;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class FakeBlock extends Entity {
    private final Block block;

    public FakeBlock(Block block, Instance instance, Pos pos) {
        super(EntityType.BLOCK_DISPLAY);
        this.block = block;
        this.setInstance(instance, pos);
        this.hasPhysics = false;
        this.setNoGravity(true);
    }

    @Override
    public void spawn() {
        this.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(block);
        });
    }
}
