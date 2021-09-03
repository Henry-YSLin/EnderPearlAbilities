package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;

public class BlockUtils {
    public static ArrayList<Block> getBlocks(Location center, int radius){
        ArrayList<Block> blocks = new ArrayList<>();
        for(double x = center.getX() - radius; x <= center.getX() + radius; x++){
            for(double y = center.getY() - radius; y <= center.getY() + radius; y++){
                for(double z = center.getZ() - radius; z <= center.getZ() + radius; z++){
                    Location loc = new Location(center.getWorld(), x, y, z);
                    blocks.add(loc.getBlock());
                }
            }
        }
        return blocks;
    }
}
