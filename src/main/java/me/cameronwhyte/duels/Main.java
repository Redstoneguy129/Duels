package me.cameronwhyte.duels;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    enum Facing {
        NORTH,
        EAST,
        SOUTH,
        WEST;

        public @NotNull Facing getOpposite() {
            switch (this) {
                case NORTH -> {
                    return SOUTH;
                }
                case EAST -> {
                    return WEST;
                }
                case SOUTH -> {
                    return NORTH;
                }
                case WEST -> {
                    return EAST;
                }
            }
            return this;
        }
    }

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        MinecraftServer.setBrandName("Astral Club");
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setChunkSupplier(LightingChunk::new);
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();
            PlayerInventory inventory = player.getInventory();
            inventory.setItemStack(0, ItemStack.of(Material.WOODEN_SWORD, 1));
            inventory.setItemStack(1, ItemStack.of(Material.WOODEN_AXE, 1));
            inventory.setItemStack(2, ItemStack.of(Material.BOW, 1));
            inventory.setItemStack(3, ItemStack.of(Material.ARROW, 1));

            inventory.setItemStack(5, ItemStack.of(Material.OAK_DOOR, 1));
            inventory.setItemStack(6, ItemStack.of(Material.OAK_PLANKS, 1));
            inventory.setItemStack(7, ItemStack.of(Material.OAK_STAIRS, 1));
            inventory.setItemStack(8, ItemStack.of(Material.OAK_SLAB, 1));
        });

        globalEventHandler.addListener(PlayerBlockPlaceEvent.class, event -> {
            event.setCancelled(true);
            Block placedBlock = event.getBlock();
            Facing facing = getFacing(event.getPlayer().getPosition());
            if (placedBlock == Block.OAK_DOOR) {
                placeDoor(instanceContainer, facing, event.getBlockPosition());
            } else if (placedBlock == Block.OAK_PLANKS) {
                placeWall(instanceContainer, facing, event.getBlockPosition());
            } else if (placedBlock == Block.OAK_STAIRS) {
                for (Placement placement : placeStairs(facing, event.getBlockPosition())) {
                    instanceContainer.setBlock(placement.position(), placement.block());
                }
            } else if (placedBlock == Block.OAK_SLAB) {
                for(Placement placement : placeCeiling(event.getBlockPosition())) {
                    instanceContainer.setBlock(placement.position(), placement.block());
                }
            }
        });

        AtomicReference<BlockVec> currentView = new AtomicReference<>();
        List<FakeBlock> fakeBlocks = new ArrayList<>();

        globalEventHandler.addListener(PlayerTickEvent.class, event -> {
            final Player player = event.getPlayer();
            Facing facing = getFacing(player.getPosition());
            try {
                Point point = event.getPlayer().getLineOfSight(7).getFirst();
                BlockVec blockVec = new BlockVec(point.add(0,1,0));
                if(blockVec.equals(currentView.get())) {
                    return;
                }
                for (FakeBlock fakeBlock : fakeBlocks) {
                    fakeBlock.remove();
                }
                fakeBlocks.clear();
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.material() == Material.OAK_STAIRS && instanceContainer.getBlock(blockVec) != Block.OAK_STAIRS) {
                    Placement[] placements = placeStairs(facing, blockVec);
                    for (Placement placement : placements) {
                        FakeBlock fakeBlock = new FakeBlock(placement.block(), instanceContainer, new Pos(placement.position().x(), placement.position().y(), placement.position().z()));
                        fakeBlock.addViewer(player);
                        fakeBlock.spawn();
                        fakeBlocks.add(fakeBlock);
                    }
                    currentView.set(blockVec);
                } else if (itemStack.material() == Material.OAK_DOOR) {
                    //placeDoor(instanceContainer, facing, blockVec);
                } else if (itemStack.material() == Material.OAK_PLANKS) {
                    //placeWall(instanceContainer, facing, blockVec);
                } else if (itemStack.material() == Material.OAK_SLAB && instanceContainer.getBlock(blockVec) != Block.OAK_SLAB) {
                    Placement[] placements = placeCeiling(blockVec);
                    for (Placement placement : placements) {
                        FakeBlock fakeBlock = new FakeBlock(placement.block(), instanceContainer, new Pos(placement.position().x(), placement.position().y(), placement.position().z()));
                        fakeBlock.addViewer(player);
                        fakeBlock.spawn();
                        fakeBlocks.add(fakeBlock);
                    }
                    currentView.set(blockVec);
                }


            } catch (Exception ignored) {
            }
        });

        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565);
    }

    private static @NotNull Facing getFacing(Pos pos) {
        float yaw = pos.yaw();
        Facing facing;
        if (yaw < -135 || yaw > 135) {
            facing = Facing.NORTH;
        } else if (yaw < -45) {
            facing = Facing.EAST;
        } else if (yaw < 45) {
            facing = Facing.SOUTH;
        } else {
            facing = Facing.WEST;
        }
        return facing;
    }

    private static void placeDoor(InstanceContainer instanceContainer, Facing facing, BlockVec position) {
        Block TopDoor = Block.OAK_DOOR.withProperty("half", "upper").withProperty("facing", facing.name().toLowerCase());
        Block BottomDoor = Block.OAK_DOOR.withProperty("half", "lower").withProperty("facing", facing.name().toLowerCase());
        Block[] others = {Block.OAK_PLANKS, Block.OAK_PLANKS, Block.OAK_PLANKS, Block.OAK_PLANKS, Block.OAK_PLANKS, Block.OAK_PLANKS, Block.OAK_PLANKS};
        // Set the block positions based off the facing direction, so they surround the door
        instanceContainer.setBlock(position, BottomDoor);
        instanceContainer.setBlock(position.add(0, 1, 0), TopDoor);
        instanceContainer.setBlock(position.add(0, 2, 0), others[6]);
        switch (facing) {
            case SOUTH, NORTH -> {
                instanceContainer.setBlock(position.add(1, 0, 0), others[0]);
                instanceContainer.setBlock(position.add(-1, 0, 0), others[1]);
                instanceContainer.setBlock(position.add(1, 1, 0), others[2]);
                instanceContainer.setBlock(position.add(-1, 1, 0), others[3]);
                instanceContainer.setBlock(position.add(1, 2, 0), others[4]);
                instanceContainer.setBlock(position.add(-1, 2, 0), others[5]);
            }
            case EAST, WEST -> {
                instanceContainer.setBlock(position.add(0, 0, 1), others[0]);
                instanceContainer.setBlock(position.add(0, 0, -1), others[1]);
                instanceContainer.setBlock(position.add(0, 1, 1), others[2]);
                instanceContainer.setBlock(position.add(0, 1, -1), others[3]);
                instanceContainer.setBlock(position.add(0, 2, 1), others[4]);
                instanceContainer.setBlock(position.add(0, 2, -1), others[5]);
            }
        }
    }

    private static void placeWall(InstanceContainer instanceContainer, Facing facing, BlockVec position) {
        Block[] wall = new Block[9];
        Arrays.fill(wall, Block.OAK_PLANKS);
        instanceContainer.setBlock(position, wall[0]);
        instanceContainer.setBlock(position.add(0, 1, 0), wall[1]);
        instanceContainer.setBlock(position.add(0, 2, 0), wall[2]);
        switch (facing) {
            case SOUTH, NORTH -> {
                instanceContainer.setBlock(position.add(1, 0, 0), wall[3]);
                instanceContainer.setBlock(position.add(-1, 0, 0), wall[4]);
                instanceContainer.setBlock(position.add(1, 1, 0), wall[5]);
                instanceContainer.setBlock(position.add(-1, 1, 0), wall[6]);
                instanceContainer.setBlock(position.add(1, 2, 0), wall[7]);
                instanceContainer.setBlock(position.add(-1, 2, 0), wall[8]);
            }
            case EAST, WEST -> {
                instanceContainer.setBlock(position.add(0, 0, 1), wall[3]);
                instanceContainer.setBlock(position.add(0, 0, -1), wall[4]);
                instanceContainer.setBlock(position.add(0, 1, 1), wall[5]);
                instanceContainer.setBlock(position.add(0, 1, -1), wall[6]);
                instanceContainer.setBlock(position.add(0, 2, 1), wall[7]);
                instanceContainer.setBlock(position.add(0, 2, -1), wall[8]);
            }
        }
    }

    private static Placement[] placeStairs(Facing facing, BlockVec position) {
        Placement[] blocks = new Placement[15];
        Arrays.fill(blocks, new Placement(Block.OAK_STAIRS, new BlockVec(0, 0, 0)));

        blocks[0] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(0, 0, 0));
        blocks[1] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(1, 0, 0));
        blocks[2] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(-1, 0, 0));
        blocks[3] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(0, 0, 1));
        blocks[4] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(1, 0, 1));
        blocks[5] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(-1, 0, 1));

        blocks[6] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(0, 1, 1));
        blocks[7] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(1, 1, 1));
        blocks[8] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(-1, 1, 1));
        blocks[9] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(0, 1, 2));
        blocks[10] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(1, 1, 2));
        blocks[11] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.getOpposite().name().toLowerCase()).withProperty("half", "top"), new BlockVec(-1, 1, 2));
        blocks[12] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(0, 2, 2));
        blocks[13] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(1, 2, 2));
        blocks[14] = new Placement(Block.OAK_STAIRS.withProperty("facing", facing.name().toLowerCase()), new BlockVec(-1, 2, 2));

        for (int i = 0; i < blocks.length; i++) {
            BlockVec pos = position;
            if (facing == Facing.SOUTH) {
                pos = new BlockVec(blocks[i].position().x(), blocks[i].position().y(), blocks[i].position().z());
            } else if (facing == Facing.NORTH) {
                pos = new BlockVec(-blocks[i].position().x(), blocks[i].position().y(), -blocks[i].position().z());
            } else if (facing == Facing.EAST) {
                pos = new BlockVec(blocks[i].position().z(), blocks[i].position().y(), -blocks[i].position().x());
            } else if (facing == Facing.WEST) {
                pos = new BlockVec(-blocks[i].position().z(), blocks[i].position().y(), blocks[i].position().x());
            }
            blocks[i] = new Placement(blocks[i].block(), position.add(pos));
        }
        return blocks;
    }

    private static Placement[] placeCeiling(BlockVec position) {
        Placement[] placements = new Placement[9];
        Block[] ceiling = new Block[9];
        Arrays.fill(ceiling, Block.OAK_SLAB.withProperty("type", "top"));
        for (int i = 0; i < ceiling.length; i++) {
            placements[i] = new Placement(ceiling[i], position.add(new BlockVec(i % 3 - 1, 0, i / 3 - 1)));
        }
        return placements;
    }

    private record Placement(Block block, BlockVec position) {
    }
}