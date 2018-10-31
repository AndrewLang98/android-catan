package edu.up.cs.androidcatan;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * @version October 30th, 2018
 * https://github.com/alexweininger/android-catan
 **/

public class City extends Building {

    private final int[] resourceCost = {0, 2, 0, 3, 0}; // Brick, Grain, Lumber, Ore, Wool

    /**
     * @param ownerId id of who owns the building
     */
    public City(int intersectionID, int ownerId) {
        super(ownerId);
        HashMap<String, Integer> checkResources = new HashMap<String, Integer>();
    }

    @Override
    public int getVictoryPoints() {
        return 2;
    }

    @Override
    public int[] getResourceCost() {
        return this.resourceCost;
    }

    @Override
    public String toString() {
        return "City{" +
                "resourceCost=" + Arrays.toString(resourceCost) +
                '}';
    }
}
